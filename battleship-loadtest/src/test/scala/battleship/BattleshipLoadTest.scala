package battleship

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import scala.collection.mutable
import scala.util.Random

class BattleshipLoadTest extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .wsBaseUrl("ws://localhost:8080")

  val scn = scenario("PvE Game - 1000 users")
    // Подключение к WebSocket
    .exec(ws("Connect").connect("/game"))
    .pause(100.milliseconds)

    // Сохранение ID игрока
    .exec(session => session.set("playerId", "Player" + session.userId))

    // Отправка CONNECT
    .exec(ws("CONNECT").sendText("""{"type":"CONNECT","playerName":"TestPlayer","clientVersion":"1.0.0"}"""))
    .pause(100.milliseconds)

    // Поиск PvE игры
    .exec(ws("FIND_GAME").sendText("""{"type":"FIND_GAME","gameMode":"PVE"}"""))
    .pause(200.milliseconds)

    // Стрельба до READY — ожидаем ошибку
    .exec(
      ws("FireTooEarly")
        .sendText("""{"type":"FIRE","x":0,"y":0}""")
        .await(3.seconds)(
          ws.checkTextMessage("FireTooEarlyResp").check(jsonPath("$.type").is("ERROR"))
        )
    )

    // Автоматическая расстановка кораблей (без пересечений)
    .exec { session =>
      val rnd        = new Random(session.userId) // детерминированно на юзера
      val occupied   = mutable.Set[(Int, Int)]()
      val ships = List(
        ("BATTLESHIP", 4),
        ("CRUISER", 3), ("CRUISER", 3),
        ("DESTROYER", 2), ("DESTROYER", 2), ("DESTROYER", 2),
        ("BOAT", 1), ("BOAT", 1), ("BOAT", 1), ("BOAT", 1)
      )

      def tryPlace(size: Int): (Int, Int, String) = {
        var placed = false
        var res: (Int, Int, String) = (0, 0, "HORIZONTAL")
        while (!placed) {
          val horizontal = rnd.nextBoolean()
          val maxX = if (horizontal) 10 - size else 10
          val maxY = if (horizontal) 10 else 10 - size
          val x = rnd.nextInt(maxX)
          val y = rnd.nextInt(maxY)
          val coords = (0 until size).map(i => if (horizontal) (x + i, y) else (x, y + i))
          if (coords.forall(c => !occupied.contains(c))) {
            occupied ++= coords
            res = (x, y, if (horizontal) "HORIZONTAL" else "VERTICAL")
            placed = true
          }
        }
        res
      }

      val messages = ships.map { case (shipType, size) =>
        val (x, y, orient) = tryPlace(size)
        s"""{"type":"PLACE_SHIP","shipType":"$shipType","startX":$x,"startY":$y,"orientation":"$orient"}"""
      }

      session.set("placements", messages)
    }
    .foreach(session => session("placements").as[Seq[String]], "shipMsg") {
      exec(ws("PlaceShip").sendText(session => session("shipMsg").as[String])).pause(50.milliseconds)
    }

    // Отправка READY
    .exec(ws("READY").sendText("""{"type":"READY"}"""))

    // Ожидание игры с AI (AI будет стрелять автоматически)
    .pause(30.seconds)

    .exec(ws("Close").close)

  setUp(
	scn.inject(
		rampConcurrentUsers(0).to(1000).during(90.seconds),
		constantConcurrentUsers(1000).during(3.minutes)     // держим ~1000 активных
	)
  ).protocols(httpProtocol).maxDuration(10.minutes)
}
