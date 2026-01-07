package com.zhaoyang.boot.nettytest.bittorrent.util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Bencode 编码器
 */
public class SimpleBencode {

    public static byte[] encode(Object obj) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encodeObject(obj, baos);
        return baos.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private static void encodeObject(Object obj, ByteArrayOutputStream baos) {
        if (obj instanceof Integer || obj instanceof Long) {
            encodeInteger(((Number) obj).longValue(), baos);
        } else if (obj instanceof String) {
            encodeString((String) obj, baos);
        } else if (obj instanceof byte[]) {
            encodeBytes((byte[]) obj, baos);
        } else if (obj instanceof List) {
            encodeList((List<Object>) obj, baos);
        } else if (obj instanceof Map) {
            encodeMap((Map<String, Object>) obj, baos);
        } else {
            throw new IllegalArgumentException("Unsupported type: " + obj.getClass());
        }
    }

    private static void encodeInteger(long value, ByteArrayOutputStream baos) {
        baos.write('i');
        writeString(String.valueOf(value), baos);
        baos.write('e');
    }

    private static void encodeString(String value, ByteArrayOutputStream baos) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        encodeBytes(bytes, baos);
    }

    private static void encodeBytes(byte[] bytes, ByteArrayOutputStream baos) {
        writeString(String.valueOf(bytes.length), baos);
        baos.write(':');
        baos.write(bytes, 0, bytes.length);
    }

    private static void encodeList(List<Object> list, ByteArrayOutputStream baos) {
        baos.write('l');
        for (Object item : list) {
            encodeObject(item, baos);
        }
        baos.write('e');
    }

    private static void encodeMap(Map<String, Object> map, ByteArrayOutputStream baos) {
        baos.write('d');

        List<String> sortedKeys = new ArrayList<>(map.keySet());
        Collections.sort(sortedKeys);

        for (String key : sortedKeys) {
            encodeString(key, baos);
            encodeObject(map.get(key), baos);
        }

        baos.write('e');
    }

    private static void writeString(String str, ByteArrayOutputStream baos) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        baos.write(bytes, 0, bytes.length);
    }
}
