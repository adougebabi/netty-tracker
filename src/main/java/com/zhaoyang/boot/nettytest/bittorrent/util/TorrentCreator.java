package com.zhaoyang.boot.nettytest.bittorrent.util;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;

/**
 * Torrent 文件生成器
 */
public class TorrentCreator {

    private static final int PIECE_LENGTH = 262144;

    public static void createTorrent(File file, String announceUrl, File outputTorrent) throws Exception {
        Map<String, Object> torrent = new HashMap<>();

        torrent.put("announce", announceUrl);
        torrent.put("creation date", System.currentTimeMillis() / 1000);
        torrent.put("created by", "Netty Tracker/1.0");

        Map<String, Object> info = new HashMap<>();
        info.put("piece length", PIECE_LENGTH);
        info.put("name", file.getName());

        if (file.isFile()) {
            info.put("length", file.length());

            byte[] pieces = calculatePieces(file);
            info.put("pieces", pieces);

        } else {
            throw new IllegalArgumentException("Directory mode not implemented yet");
        }

        torrent.put("info", info);

        byte[] torrentBytes = SimpleBencode.encode(torrent);
        try (FileOutputStream fos = new FileOutputStream(outputTorrent)) {
            fos.write(torrentBytes);
        }

        byte[] infoHash = calculateInfoHash(info);
        System.out.println("Torrent created: " + outputTorrent.getAbsolutePath());
        System.out.println("Info Hash: " + BencodeUtil.bytesToHex(infoHash));
    }

    private static byte[] calculatePieces(File file) throws Exception {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        ByteArrayOutputStream pieces = new ByteArrayOutputStream();

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[PIECE_LENGTH];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) > 0) {
                sha1.update(buffer, 0, bytesRead);
                byte[] pieceHash = sha1.digest();
                pieces.write(pieceHash);
            }
        }

        return pieces.toByteArray();
    }

    private static byte[] calculateInfoHash(Map<String, Object> info) throws Exception {
        byte[] encodedInfo = SimpleBencode.encode(info);
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        return sha1.digest(encodedInfo);
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java TorrentCreator <file> <announce_url> [output.torrent]");
            System.out.println("Example: java TorrentCreator test.txt http://localhost:6969/announce");
            System.exit(1);
        }

        String filePath = args[0];
        String announceUrl = args[1];
        String outputPath = args.length > 2 ? args[2] : filePath + ".torrent";

        try {
            File file = new File(filePath);
            if (!file.exists()) {
                System.err.println("File not found: " + filePath);
                System.exit(1);
            }

            File outputTorrent = new File(outputPath);
            createTorrent(file, announceUrl, outputTorrent);

        } catch (Exception e) {
            System.err.println("Error creating torrent: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
