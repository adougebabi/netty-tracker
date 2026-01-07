package com.zhaoyang.boot.nettytest.bittorrent.util;

import com.zhaoyang.boot.nettytest.bittorrent.model.Peer;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Bencode 工具类
 */
public class BencodeUtil {

    public static byte[] encodeTrackerResponse(int interval, int complete, int incomplete,
                                                 List<Peer> peers) {
        Map<String, Object> response = new HashMap<>();
        response.put("interval", interval);
        response.put("complete", complete);
        response.put("incomplete", incomplete);

        if (!peers.isEmpty()) {
            response.put("peers", buildCompactPeerList(peers));
        } else {
            response.put("peers", new byte[0]);
        }

        return SimpleBencode.encode(response);
    }

    public static byte[] encodeErrorResponse(String errorMessage) {
        Map<String, Object> response = new HashMap<>();
        response.put("failure reason", errorMessage);
        return SimpleBencode.encode(response);
    }

    private static byte[] buildCompactPeerList(List<Peer> peers) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for (Peer peer : peers) {
            try {
                String[] ipParts = peer.getIp().split("\\.");
                if (ipParts.length == 4) {
                    for (String part : ipParts) {
                        baos.write(Integer.parseInt(part));
                    }

                    int port = peer.getPort();
                    baos.write((port >> 8) & 0xFF);
                    baos.write(port & 0xFF);
                }
            } catch (Exception e) {
            }
        }

        return baos.toByteArray();
    }

    public static String urlDecodeInfoHash(String urlEncoded) {
        if (urlEncoded == null) return null;

        try {
            byte[] bytes = java.net.URLDecoder.decode(urlEncoded, StandardCharsets.ISO_8859_1)
                    .getBytes(StandardCharsets.ISO_8859_1);
            return bytesToHex(bytes);
        } catch (Exception e) {
            return null;
        }
    }

    public static String urlDecodePeerId(String urlEncoded) {
        if (urlEncoded == null) return null;

        try {
            return java.net.URLDecoder.decode(urlEncoded, StandardCharsets.ISO_8859_1);
        } catch (Exception e) {
            return urlEncoded;
        }
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
