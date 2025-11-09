package com.norwood.mcheli.eval.util;

public class CharUtil {

    public static String escapeString(String str) {
        return escapeString(str, 0, str.length());
    }

    public static String escapeString(String input, int offset, int length) {
        StringBuilder escaped = new StringBuilder(length);
        int end = offset + length;
        int[] advance = new int[1];

        while (offset < end) {
            char c = escapeChar(input, offset, end, advance);
            if (advance[0] <= 0) break;
            escaped.append(c);
            offset += advance[0];
        }

        return escaped.toString();
    }

    public static char escapeChar(String str, int pos, int end, int[] ret) {
        if (pos >= end) {
            ret[0] = 0;
            return '\u0000';
        }

        char c = str.charAt(pos);
        if (c != '\\') {
            ret[0] = 1;
            return c;
        }

        pos++;
        if (pos >= end) {
            ret[0] = 1;
            return '\\';
        }

        c = str.charAt(pos);
        ret[0] = 2;

        return switch (c) {
            case 'b' -> '\b';
            case 'f' -> '\f';
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            case 'u' -> {
                long code = 0;
                for (int i = 0; i < 4 && ++pos < end; i++, ret[0]++) {
                    char hex = str.charAt(pos);
                    int digit = Character.digit(hex, 16);
                    if (digit == -1) break;
                    code = (code << 4) + digit;
                }

                yield (char) code;
            }
            case '0', '1', '2', '3', '4', '5', '6', '7' -> {
                long code = c - '0';
                for (int i = 1; i < 3 && pos + 1 < end; i++) {
                    char next = str.charAt(pos + 1);
                    if (next < '0' || next > '7') break;
                    code = (code << 3) + (next - '0');
                    pos++;
                    ret[0]++;
                }
                yield (char) code;
            }
            default -> c;
        };
    }
}
