package com.battleship.client.network;

import com.battleship.common.protocol.Messages;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * WebSocket клиент для игры
 */
public class GameClient {
    private static final Logger logger = LoggerFactory.getLogger(GameClient.class);

    private final String host;
    private final int port;
    private final String path;
    private final ObjectMapper objectMapper;

    private Channel channel;
    private EventLoopGroup group;
    private Consumer<Messages.Message> messageHandler;
    private String playerId;

    public GameClient(String host, int port, String path) {
        this.host = host;
        this.port = port;
        this.path = path;
        this.objectMapper = new ObjectMapper();
    }

    public CompletableFuture<Void> connect() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        try {
            URI uri = new URI("ws://" + host + ":" + port + path);

            group = new NioEventLoopGroup();

            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();

                            pipeline.addLast(new HttpClientCodec());
                            pipeline.addLast(new HttpObjectAggregator(8192));
                            pipeline.addLast(WebSocketClientCompressionHandler.INSTANCE);

                            WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory
                                    .newHandshaker(uri, WebSocketVersion.V13, null, true,
                                                  new DefaultHttpHeaders());

                            pipeline.addLast(new WebSocketClientHandler(handshaker, GameClient.this));
                        }
                    });

            channel = bootstrap.connect(host, port).sync().channel();
            logger.info("Подключение к {}:{}{}", host, port, path);

            future.complete(null);
        } catch (Exception e) {
            logger.error("Ошибка подключения: {}", e.getMessage());
            future.completeExceptionally(e);
        }

        return future;
    }

    public void disconnect() {
        if (channel != null) {
            channel.close();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
        logger.info("Отключение от сервера");
    }

    public void sendMessage(Messages.Message message) {
        if (channel == null || !channel.isActive()) {
            logger.error("Канал не активен");
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(message);
            channel.writeAndFlush(new TextWebSocketFrame(json));
            logger.debug("Отправлено: {}", message.getType());
        } catch (Exception e) {
            logger.error("Ошибка отправки сообщения: {}", e.getMessage());
        }
    }

    void handleMessage(String json) {
        try {
            Messages.Message message = objectMapper.readValue(json, Messages.Message.class);
            logger.debug("Получено: {}", message.getType());

            // Сохраняем playerId при подключении
            if (message instanceof Messages.Connected connected) {
                this.playerId = connected.getPlayerId();
            }

            if (messageHandler != null) {
                messageHandler.accept(message);
            }
        } catch (Exception e) {
        }
    }

    public void setMessageHandler(Consumer<Messages.Message> handler) {
        this.messageHandler = handler;
    }

    public String getPlayerId() {
        return playerId;
    }

    public boolean isConnected() {
        return channel != null && channel.isActive();
    }

    private static class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {
        private final WebSocketClientHandshaker handshaker;
        private final GameClient client;
        private ChannelPromise handshakeFuture;

        public WebSocketClientHandler(WebSocketClientHandshaker handshaker, GameClient client) {
            this.handshaker = handshaker;
            this.client = client;
        }

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            handshakeFuture = ctx.newPromise();
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            handshaker.handshake(ctx.channel());
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
            Channel ch = ctx.channel();

            if (!handshaker.isHandshakeComplete()) {
                handshaker.finishHandshake(ch, (io.netty.handler.codec.http.FullHttpResponse) msg);
                handshakeFuture.setSuccess();
                return;
            }

            if (msg instanceof TextWebSocketFrame textFrame) {
                client.handleMessage(textFrame.text());
            } else if (msg instanceof CloseWebSocketFrame) {
                ch.close();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("Ошибка в клиенте: {}", cause.getMessage());
            if (!handshakeFuture.isDone()) {
                handshakeFuture.setFailure(cause);
            }
            ctx.close();
        }
    }
}
