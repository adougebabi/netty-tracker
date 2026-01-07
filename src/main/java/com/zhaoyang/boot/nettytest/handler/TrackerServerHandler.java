package com.zhaoyang.boot.nettytest.handler;

import com.zhaoyang.boot.nettytest.manager.ClientManager;
import com.zhaoyang.boot.nettytest.protocol.TrackerMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 消息处理器
 */
public class TrackerServerHandler extends SimpleChannelInboundHandler<String> {
    private static final Logger logger = LoggerFactory.getLogger(TrackerServerHandler.class);

    private final ClientManager clientManager;
    private String clientId;

    public TrackerServerHandler(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("New connection from {}", ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        logger.debug("Received: {}", msg);

        TrackerMessage message = TrackerMessage.parse(msg.trim());
        if (message == null) {
            logger.warn("Invalid message format: {}", msg);
            sendError(ctx.channel(), "Invalid message format");
            return;
        }

        switch (message.getType()) {
            case REGISTER:
                handleRegister(ctx.channel(), message);
                break;

            case HEARTBEAT:
                handleHeartbeat(ctx.channel(), message);
                break;

            case MESSAGE:
                handleMessage(ctx.channel(), message);
                break;

            default:
                logger.warn("Unknown message type: {}", message.getType());
                sendError(ctx.channel(), "Unknown message type");
        }
    }

    private void handleRegister(Channel channel, TrackerMessage message) {
        this.clientId = message.getClientId();
        clientManager.register(clientId, channel);

        TrackerMessage ack = new TrackerMessage(
                TrackerMessage.MessageType.ACK,
                "SERVER",
                "Registration successful"
        );
        channel.writeAndFlush(ack.encode());
        logger.info("Client {} registered successfully", clientId);
    }

    private void handleHeartbeat(Channel channel, TrackerMessage message) {
        TrackerMessage ack = new TrackerMessage(
                TrackerMessage.MessageType.ACK,
                "SERVER",
                "Heartbeat received"
        );
        channel.writeAndFlush(ack.encode());
        logger.debug("Heartbeat from client {}", message.getClientId());
    }

    private void handleMessage(Channel channel, TrackerMessage message) {
        logger.info("Message from {}: {}", message.getClientId(), message.getPayload());

        TrackerMessage ack = new TrackerMessage(
                TrackerMessage.MessageType.ACK,
                "SERVER",
                "Message received"
        );
        channel.writeAndFlush(ack.encode());
    }

    private void sendError(Channel channel, String error) {
        TrackerMessage errorMsg = new TrackerMessage(
                TrackerMessage.MessageType.ERROR,
                "SERVER",
                error
        );
        channel.writeAndFlush(errorMsg.encode());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (clientId != null) {
            clientManager.unregister(clientId);
            logger.info("Client {} disconnected", clientId);
        } else {
            clientManager.unregister(ctx.channel());
            logger.info("Unknown client disconnected from {}", ctx.channel().remoteAddress());
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Exception in channel {}", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }
}
