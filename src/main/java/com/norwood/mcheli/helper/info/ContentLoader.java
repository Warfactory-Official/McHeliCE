package com.norwood.mcheli.helper.info;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;
import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.helper.MCH_Logger;
import com.norwood.mcheli.helper.MCH_Utils;
import com.norwood.mcheli.helper.addon.AddonResourceLocation;
import com.norwood.mcheli.helper.info.parsers.IParser;

public abstract class ContentLoader {

    protected final String domain;
    protected final File dir;
    protected final String loaderVersion;
    private final Predicate<String> fileFilter;

    public ContentLoader(String domain, File addonDir, String loaderVersion, Predicate<String> fileFilter) {
        this.domain = domain;
        this.dir = addonDir;
        this.loaderVersion = loaderVersion;
        this.fileFilter = fileFilter;
    }

    public boolean isReadable(String path) {
        return this.fileFilter.test(path);
    }

    public Multimap<ContentType, ContentEntry> load() {
        Multimap<ContentType, ContentEntry> map = LinkedHashMultimap.create();
        for (ContentEntry entry : this.getEntries()) {
            map.put(entry.getType(), entry);
        }
        return map;
    }

    protected abstract List<ContentEntry> getEntries();

    protected abstract InputStream getInputStreamByName(String name) throws IOException;

    public <TYPE extends IContentData> List<TYPE> reloadAndParse(Class<TYPE> clazz, List<TYPE> oldContents,
                                                                 ContentType type) {
        List<TYPE> list = Lists.newLinkedList();
        for (TYPE oldContent : oldContents) {
            try {
                ContentEntry entry = this.makeEntry(oldContent.getContentPath(), type, true);
                IContentData content = entry.parse();
                if (content != null) {
                    content.onPostReload();
                } else {
                    content = oldContent;
                }

                if (clazz.isInstance(content)) {
                    list.add(clazz.cast(content));
                }
            } catch (IOException e) {
                MCH_Logger.get().error("IO Error from file loader!", e);
            }
        }
        return list;
    }

    public IContentData reloadAndParseSingle(IContentData oldData, String dir) {
        IContentData content = oldData;
        try {
            ContentType type = ContentFactories.getType(dir);
            if (type == null) {
                MCH_Logger.get().warn("Unknown content type for directory '{}'", dir);
                return content;
            }
            ContentEntry entry = this.makeEntry(oldData.getContentPath(), type, true);
            content = entry.parse();
            if (content != null) {
                content.onPostReload();
            } else {
                content = oldData;
            }
        } catch (IOException e) {
            MCH_Logger.get().error("IO Error from file loader!", e);
        }
        return content;
    }

    protected ContentEntry makeEntry(String filepath, ContentType type, boolean reload) throws IOException {
        List<String> lines;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(this.getInputStreamByName(filepath), StandardCharsets.UTF_8))) {
            lines = reader.lines().collect(Collectors.toList());
        }
        return new ContentEntry(filepath, this.domain, type, lines, reload);
    }

    public static class ContentEntry {

        private final String filepath;
        private final String domain;
        private final ContentType type;
        private final List<String> lines;
        private final boolean reload;

        private ContentEntry(String filepath, String domain, ContentType type, List<String> lines, boolean reload) {
            this.filepath = filepath;
            this.domain = domain;
            this.type = type;
            this.lines = lines;
            this.reload = reload;
        }

        public ContentType getType() {
            return this.type;
        }

        @Nullable
        public IContentData parse() {
            AddonResourceLocation location = MCH_Utils.addon(this.domain, Files.getNameWithoutExtension(this.filepath));
            if (!this.reload) {
                MCH_MOD.proxy.onParseStartFile(location);
            }

            IContentData content;
            try {
                String extension = Files.getFileExtension(this.filepath);
                IParser parser = ContentParsers.get(extension);
                if (parser == null) {
                    MCH_Logger.get().warn("No parser registered for extension '{}'", extension);
                    return null;
                }

                content = invokeParser(parser, location);
                if (content != null && !content.validate()) {
                    MCH_Logger.get().debug("Invalid content info: {}", this.filepath);
                }
                return content;
            } catch (Exception ex) {
                String msg = "An error occurred while file loading ";
                if (ex instanceof ContentParseException) {
                    msg = msg + "at line:" + ((ContentParseException) ex).getLineNo() + ".";
                }
                MCH_Logger.get().error("{} file:{}, domain:{}", msg, location.getPath(), this.domain, ex);
                return null;
            } finally {
                MCH_MOD.proxy.onParseFinishFile(location);
            }
        }

        @Nullable
        private IContentData invokeParser(IParser parser, AddonResourceLocation location) throws Exception {
            return switch (this.type) {
                case HELICOPTER -> parser.parseHelicopter(location, this.filepath, this.lines, this.reload);
                case PLANE -> parser.parsePlane(location, this.filepath, this.lines, this.reload);
                case SHIP -> parser.parseShip(location, this.filepath, this.lines, this.reload);
                case TANK -> parser.parseTank(location, this.filepath, this.lines, this.reload);
                case VEHICLE -> parser.parseVehicle(location, this.filepath, this.lines, this.reload);
                case WEAPON -> parser.parseWeapon(location, this.filepath, this.lines, this.reload);
                case THROWABLE -> parser.parseThrowable(location, this.filepath, this.lines, this.reload);
                case HUD -> parser.parseHud(location, this.filepath, this.lines, this.reload);
                case ITEM -> parser.parseItem(location, this.filepath, this.lines, this.reload);
            };
        }
    }
}
