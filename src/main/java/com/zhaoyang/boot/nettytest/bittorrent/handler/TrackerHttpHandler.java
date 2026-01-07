package com.zhaoyang.boot.nettytest.bittorrent.handler;

import com.zhaoyang.boot.nettytest.bittorrent.model.Peer;
import com.zhaoyang.boot.nettytest.bittorrent.model.TorrentManager;
import com.zhaoyang.boot.nettytest.bittorrent.util.BencodeUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * Tracker HTTP 请求处理器
 */
public class TrackerHttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger logger = LoggerFactory.getLogger(TrackerHttpHandler.class);

    private static final int DEFAULT_INTERVAL = 1800;
    private static final int DEFAULT_NUM_WANT = 50;

    private final TorrentManager torrentManager;

    public TrackerHttpHandler(TorrentManager torrentManager) {
        this.torrentManager = torrentManager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        logger.debug("Received HTTP request: {} {}", request.method(), request.uri());

        if (!request.uri().startsWith("/announce")) {
            if (request.uri().equals("/") || request.uri().equals("/stats")) {
                handleStatsRequest(ctx, request);
            } else {
                sendErrorResponse(ctx, request, "Invalid tracker endpoint");
            }
            return;
        }

        try {
            Map<String, String> params = parseQueryParams(request.uri());

            String infoHash = BencodeUtil.urlDecodeInfoHash(params.get("info_hash"));
            String peerId = BencodeUtil.urlDecodePeerId(params.get("peer_id"));
            String port = params.get("port");

            if (infoHash == null || peerId == null || port == null) {
                sendErrorResponse(ctx, request, "Missing required parameters");
                return;
            }

            long uploaded = parseLong(params.get("uploaded"), 0);
            long downloaded = parseLong(params.get("downloaded"), 0);
            long left = parseLong(params.get("left"), 0);
            int numWant = (int) parseLong(params.get("numwant"), DEFAULT_NUM_WANT);
            String event = params.get("event");

            String rawIp = getClientIp(ctx);
            String paramIp = params.get("ip");
            String ip = paramIp != null ? paramIp : rawIp;

            logger.debug("IP resolution: rawIp={}, paramIp={}, final={}", rawIp, paramIp, ip);

            int peerPort = Integer.parseInt(port);

            handleAnnounce(ctx, request, infoHash, peerId, ip, peerPort,
                    uploaded, downloaded, left, numWant, event);

        } catch (Exception e) {
            logger.error("Error handling announce request", e);
            sendErrorResponse(ctx, request, "Internal tracker error");
        }
    }

    private void handleAnnounce(ChannelHandlerContext ctx, FullHttpRequest request,
                                String infoHash, String peerId, String ip, int port,
                                long uploaded, long downloaded, long left,
                                int numWant, String event) {
        try {
            if ("stopped".equals(event)) {
                torrentManager.removePeer(infoHash, peerId);
                sendAnnounceResponse(ctx, request, infoHash, List.of());
                return;
            }

            torrentManager.announcePeer(infoHash, peerId, ip, port, uploaded, downloaded, left);

            List<Peer> peers = torrentManager.getPeers(infoHash, numWant);

            sendAnnounceResponse(ctx, request, infoHash, peers);

            logger.info("Announce from peer {} for torrent {}: up={}, down={}, left={}, ip={}, port={}",
                    peerId, infoHash, uploaded, downloaded, left, ip, port);

        } catch (Exception e) {
            logger.error("Error in announce handler", e);
            sendErrorResponse(ctx, request, "Failed to process announce");
        }
    }

    private void sendAnnounceResponse(ChannelHandlerContext ctx, FullHttpRequest request,
                                       String infoHash, List<Peer> peers) {
        try {
            int complete = torrentManager.getCompleteCount(infoHash);
            int incomplete = torrentManager.getIncompleteCount(infoHash);

            byte[] responseBytes = BencodeUtil.encodeTrackerResponse(
                    DEFAULT_INTERVAL, complete, incomplete, peers);

            FullHttpResponse response = new DefaultFullHttpResponse(
                    HTTP_1_1, OK, Unpooled.wrappedBuffer(responseBytes));

            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, responseBytes.length);

            if (HttpUtil.isKeepAlive(request)) {
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                ctx.writeAndFlush(response);
            } else {
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            }

        } catch (Exception e) {
            logger.error("Error encoding tracker response", e);
            sendErrorResponse(ctx, request, "Failed to encode response");
        }
    }

    private void handleStatsRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        Map<String, Integer> stats = torrentManager.getStats();

        StringBuilder html = new StringBuilder();
        html.append("<html><head><title>Tracker Stats</title></head><body>");
        html.append("<h1>BitTorrent Tracker Statistics</h1>");
        html.append("<table border='1'>");
        html.append("<tr><th>Metric</th><th>Value</th></tr>");
        html.append("<tr><td>Active Torrents</td><td>").append(stats.get("torrents")).append("</td></tr>");
        html.append("<tr><td>Total Peers</td><td>").append(stats.get("peers")).append("</td></tr>");
        html.append("<tr><td>Seeders</td><td>").append(stats.get("seeders")).append("</td></tr>");
        html.append("<tr><td>Leechers</td><td>").append(stats.get("leechers")).append("</td></tr>");
        html.append("</table></body></html>");

        byte[] content = html.toString().getBytes();

        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, OK, Unpooled.wrappedBuffer(content));

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.length);

        if (HttpUtil.isKeepAlive(request)) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            ctx.writeAndFlush(response);
        } else {
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }

    private void sendErrorResponse(ChannelHandlerContext ctx, FullHttpRequest request, String error) {
        try {
            byte[] errorBytes = BencodeUtil.encodeErrorResponse(error);

            FullHttpResponse response = new DefaultFullHttpResponse(
                    HTTP_1_1, OK, Unpooled.wrappedBuffer(errorBytes));

            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, errorBytes.length);

            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);

        } catch (Exception e) {
            logger.error("Error sending error response", e);
            ctx.close();
        }
    }

    private Map<String, String> parseQueryParams(String uri) {
        Map<String, String> params = new HashMap<>();

        int queryStart = uri.indexOf('?');
        if (queryStart < 0) return params;

        String query = uri.substring(queryStart + 1);
        for (String param : query.split("&")) {
            int eqIdx = param.indexOf('=');
            if (eqIdx > 0) {
                String key = param.substring(0, eqIdx);
                String value = param.substring(eqIdx + 1);
                params.put(key, value);
            }
        }

        return params;
    }

    private String getClientIp(ChannelHandlerContext ctx) {
        return ctx.channel().remoteAddress().toString().split(":")[0].replace("/", "");
    }

    private long parseLong(String value, long defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Exception in HTTP handler", cause);
        ctx.close();
    }
}
