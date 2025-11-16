package com.norwood.mcheli.helper.info;

import com.google.common.collect.Maps;
import com.norwood.mcheli.helper.info.parsers.IParser;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;

public final class ContentParsers {

    private static final Map<String, IParser> PARSERS = Maps.newHashMap();

    private ContentParsers() {}

    public static void register(String extension, IParser parser) {
        if (extension == null || parser == null) {
            throw new IllegalArgumentException("extension and parser must be non-null");
        }
        PARSERS.put(normalize(extension), parser);
    }

    @Nullable
    public static IParser get(String extension) {
        if (extension == null) {
            return null;
        }
        return PARSERS.get(normalize(extension));
    }

    private static String normalize(String extension) {
        String ext = extension.startsWith(".") ? extension.substring(1) : extension;
        return ext.toLowerCase(Locale.ROOT);
    }
}
