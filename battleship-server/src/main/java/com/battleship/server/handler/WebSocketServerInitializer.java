package com.battleship.server.handler;

import com.battleship.server.ai.AIService;
import com.battleship.server.matchmaking.MatchmakingService;
import com.battleship.server.session.SessionManager;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * Инициализатор pipeline для WebSocket
 */
public class WebSocketServerInitializer extends ChannelInitializer<SocketChannel> {

    private final String websocketPath;
    private final SessionManager sessionManager;
    private final MatchmakingService matchmakingService;
    private final AIService aiService;

    public WebSocketServerInitializer(String websocketPath,
                                     SessionManager sessionManager,
                                     MatchmakingService matchmakingService,
                                     AIService aiService) {
        this.websocketPath = websocketPath;
        this.sessionManager = sessionManager;
        this.matchmakingService = matchmakingService;
        this.aiService = aiService;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(65536));
        pipeline.addLast(new ChunkedWriteHandler());

        pipeline.addLast(new WebSocketServerProtocolHandler(websocketPath, null, true));

        pipeline.addLast(new GameMessageHandler(sessionManager, matchmakingService, aiService));
    }
}
