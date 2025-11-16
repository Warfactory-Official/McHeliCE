package com.norwood.mcheli.helper.info;

import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

public class FolderContentLoader extends ContentLoader {

    private final Path root;

    public FolderContentLoader(String domain, File addonDir, String loaderVersion, Predicate<String> fileFilter) {
        super(domain, addonDir, loaderVersion, fileFilter);
        Path root;
        try {
            root = addonDir.toPath().toRealPath();
        } catch (IOException e) {
            root = addonDir.toPath().toAbsolutePath().normalize();
        }
        this.root = root;
    }

    private static String normalizeName(String name) {
        String s = name.replace('\\', '/');
        while (s.startsWith("/")) {
            s = s.substring(1);
        }
        return s;
    }

    @Override
    protected List<ContentEntry> getEntries() {
        boolean loadDeep = "2".equals(this.loaderVersion);
        return walkDir(this.dir, null, loadDeep, 0);
    }

    private List<ContentEntry> walkDir(File node, @Nullable ContentType type, boolean allowDeepFromHere, int depth) {
        List<ContentEntry> list = Lists.newArrayList();

        if (node == null || !node.exists()) {
            return list;
        }

        if (node.isDirectory()) {
            if (allowDeepFromHere || depth <= 1) {
                File[] children = node.listFiles();
                if (children != null) {
                    for (File child : children) {
                        ContentType nextType = type;
                        if (nextType == null) {
                            nextType = ContentFactories.getType(child.getName());
                        }
                        boolean nextAllowDeep = allowDeepFromHere || (depth == 0 && "assets".equals(child.getName()));
                        list.addAll(walkDir(child, nextType, nextAllowDeep, depth + 1));
                    }
                }
            }
            return list;
        }

        try {
            String rel = relativeForwardSlash(node);
            if (this.isReadable(rel) && type != null) {
                list.add(this.makeEntry(rel, type, false));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    private String relativeForwardSlash(File file) throws IOException {
        Path fileReal;
        try {
            fileReal = file.toPath().toRealPath();
        } catch (IOException e) {
            throw new FileNotFoundException(file.getAbsolutePath());
        }

        Path rel = root.relativize(fileReal).normalize();
        if (rel.startsWith("..")) {
            throw new SecurityException("Resolved file escapes addon root: " + fileReal);
        }
        String s = rel.toString();
        s = s.replace('\\', '/');
        return s;
    }

    @Override
    protected InputStream getInputStreamByName(String name) throws IOException {
        File file1 = getFile(name);
        if (file1 == null) throw new FileNotFoundException(String.format("'%s' in AddonPack '%s'", name, this.dir));
        return new BufferedInputStream(Files.newInputStream(file1.toPath()));
    }

    @Nullable
    private File getFile(String name) {
        String normalized = normalizeName(name);
        Path candidate;
        try {
            candidate = root.resolve(normalized).normalize();
        } catch (InvalidPathException ipe) {
            return null;
        }
        if (!candidate.startsWith(root)) return null;
        if (Files.isRegularFile(candidate)) return candidate.toFile();
        return null;
    }
}
