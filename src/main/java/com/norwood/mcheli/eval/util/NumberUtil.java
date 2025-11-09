package com.norwood.mcheli.eval.util;

public class NumberUtil {

    public static long parseLong(String str) {
        if (str == null) {
            return 0L;
        }

        str = str.trim();
        int len = str.length();
        if (len == 0) {
            return 0L;
        }

        // Strip suffix
        char lastChar = str.charAt(len - 1);
        if (lastChar == '.' || lastChar == 'L' || lastChar == 'l') {
            len--;
            str = str.substring(0, len);
        }

        // Parse base prefix
        if (len >= 3 && str.charAt(0) == '0') {
            return switch (str.charAt(1)) {
                case 'B', 'b' -> parseLongBin(str, 2, len - 2);
                case 'O', 'o' -> parseLongOct(str, 2, len - 2);
                case 'X', 'x' -> parseLongHex(str, 2, len - 2);
                default -> parseLongDec(str, 0, len);
            };
        }

        return parseLongDec(str, 0, len);
    }

    public static long parseLongBin(String str) {
        return str == null ? 0L : parseLongBin(str, 0, str.length());
    }

    public static long parseLongBin(String str, int pos, int len) {
        long result = 0L;
        int end = pos + len;

        for (; pos < end; pos++) {
            result *= 2L;
            char c = str.charAt(pos);
            result += switch (c) {
                case '0' -> 0;
                case '1' -> 1;
                default -> throw new NumberFormatException(str.substring(pos, end));
            };
        }

        return result;
    }

    public static long parseLongOct(String str) {
        return str == null ? 0L : parseLongOct(str, 0, str.length());
    }

    public static long parseLongOct(String str, int pos, int len) {
        long result = 0L;
        int end = pos + len;

        for (; pos < end; pos++) {
            result *= 8L;
            char c = str.charAt(pos);
            result += switch (c) {
                case '0' -> 0;
                case '1', '2', '3', '4', '5', '6', '7' -> c - '0';
                default -> throw new NumberFormatException(str.substring(pos, end));
            };
        }

        return result;
    }

    public static long parseLongDec(String str) {
        return str == null ? 0L : parseLongDec(str, 0, str.length());
    }

    public static long parseLongDec(String str, int pos, int len) {
        long ret = 0L;

        for (int i = 0; i < len; i++) {
            ret *= 10L;
            char c = str.charAt(pos++);
            switch (c) {
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    ret += c - '0';
                    break;
                case '0':
                default:
                    throw new NumberFormatException(str.substring(pos, len));
            }
        }

        return ret;
    }

    public static long parseLongHex(String str) {
        return str == null ? 0L : parseLongHex(str, 0, str.length());
    }

    public static long parseLongHex(String str, int pos, int len) {
        long result = 0L;
        int end = pos + len;

        for (; pos < end; pos++) {
            result *= 16L;
            char c = str.charAt(pos);
            result += switch (c) {
                case '0' -> 0;
                case '1', '2', '3', '4', '5', '6', '7', '8', '9' -> c - '0';
                case 'A', 'B', 'C', 'D', 'E', 'F' -> c - 'A' + 10;
                case 'a', 'b', 'c', 'd', 'e', 'f' -> c - 'a' + 10;
                default -> throw new NumberFormatException(str.substring(pos, end));
            };
        }

        return result;
    }
}
