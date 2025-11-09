package com.norwood.mcheli.helper.io;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.annotation.Nullable;

import org.jline.utils.OSUtils;

import com.google.common.base.CharMatcher;
import com.google.common.collect.Lists;

public abstract class ResourceLoader implements Closeable {

    protected final File dir;

    ResourceLoader(File file) {
        this.dir = file;
    }

    public static ResourceLoader create(File file) {
        return file.isDirectory() ? new DirectoryLoader(file) : new ZipJarFileLoader(file);
    }

    public List<ResourceLoader.ResourceEntry> loadAll() throws IOException {
        return this.loadAll(null);
    }

    public abstract List<ResourceLoader.ResourceEntry> loadAll(@Nullable Predicate<? super ResourceLoader.ResourceEntry> var1) throws IOException;

    public Optional<ResourceLoader.ResourceEntry> loadFirst() throws IOException {
        return this.loadAll(null).stream().findFirst();
    }

    public abstract ResourceLoader.ResourceEntry load(String var1) throws IOException;

    public abstract InputStream getInputStreamFromEntry(ResourceLoader.ResourceEntry var1) throws IOException;

    public InputStream getInputStream(String relativePath) throws IOException {
        return this.getInputStreamFromEntry(this.load(relativePath));
    }

    @Override
    protected void finalize() throws Throwable {
        this.close();
        super.finalize();
    }

    static class DirectoryLoader extends ResourceLoader {

        private static final boolean ON_WINDOWS = OSUtils.IS_WINDOWS;
        private static final CharMatcher BACKSLASH_MATCHER = CharMatcher.is('\\');

        DirectoryLoader(File file) {
            super(file);
        }

        @Override
        public List<ResourceLoader.ResourceEntry> loadAll(@Nullable Predicate<? super ResourceLoader.ResourceEntry> filePathFilter) throws IOException {
            List<ResourceLoader.ResourceEntry> list = Lists.newLinkedList();
            filePathFilter = filePathFilter == null ? path -> true : filePathFilter;
            this.loadFiles(this.dir, list, filePathFilter);
            return list;
        }

        private void loadFiles(File dir, List<ResourceLoader.ResourceEntry> list,
                               Predicate<? super ResourceLoader.ResourceEntry> filePathFilter) {
            if (dir.exists()) {
                if (dir.isDirectory()) {
                    for (File file : dir.listFiles()) {
                        this.loadFiles(file, list, filePathFilter);
                    }
                } else {
                    Path file = dir.toPath();
                    Path root = this.dir.toPath();
                    String s = root.relativize(file).toString();
                    if (ON_WINDOWS) {
                        s = BACKSLASH_MATCHER.replaceFrom(s, '/');
                    }

                    ResourceLoader.ResourceEntry resourceFile = new ResourceLoader.ResourceEntry(s, dir.isDirectory());
                    if (filePathFilter.test(resourceFile)) {
                        list.add(resourceFile);
                    }
                }
            }
        }

        @Override
        public ResourceLoader.ResourceEntry load(String relativePath) throws IOException {
            File file = this.getFile(relativePath);
            if (file != null && file.exists()) {
                return new ResourceLoader.ResourceEntry(relativePath, file.isDirectory());
            } else {
                throw new FileNotFoundException(relativePath);
            }
        }

        @Override
        public InputStream getInputStreamFromEntry(ResourceLoader.ResourceEntry resource) throws IOException {
            File file1 = this.getFile(resource.getPath());
            if (file1 == null) {
                throw new FileNotFoundException(
                        String.format("'%s' in ResourcePack '%s'", this.dir, resource.getPath()));
            } else {
                return new BufferedInputStream(Files.newInputStream(file1.toPath()));
            }
        }

        @Nullable
        private File getFile(String filepath) {
            try {
                File file1 = new File(this.dir, filepath);
                if (file1.isFile() && this.validatePath(file1, filepath)) {
                    return file1;
                }
            } catch (IOException var3) {}

            return null;
        }

        private boolean validatePath(File file, String filepath) throws IOException {
            String s = file.getCanonicalPath();
            if (ON_WINDOWS) {
                s = BACKSLASH_MATCHER.replaceFrom(s, '/');
            }

            return s.endsWith(filepath);
        }

        @Override
        public void close() {}
    }

    public static class ResourceEntry {

        private final String path;
        private final boolean isDirectory;

        public ResourceEntry(String path, boolean isDirectory) {
            this.path = path;
            this.isDirectory = isDirectory;
        }

        public boolean isDirectory() {
            return this.isDirectory;
        }

        public String getPath() {
            return this.path;
        }
    }

    static class ZipJarFileLoader extends ResourceLoader {

        private ZipFile resourcePackZipFile;

        ZipJarFileLoader(File file) {
            super(file);
        }

        @Override
        public List<ResourceLoader.ResourceEntry> loadAll(@Nullable Predicate<? super ResourceLoader.ResourceEntry> filePathFilter) throws IOException {
            ZipFile zipfile = this.getResourcePackZipFile();
            filePathFilter = filePathFilter == null ? path -> true : filePathFilter;
            return zipfile.stream()
                    .map(enrty -> new ResourceLoader.ResourceEntry(enrty.getName(), enrty.isDirectory()))
                    .filter(filePathFilter)
                    .collect(Collectors.toList());
        }

        @Override
        public ResourceLoader.ResourceEntry load(String relativePath) throws IOException {
            ZipFile zipfile = this.getResourcePackZipFile();
            ZipEntry zipEntry = zipfile.getEntry(relativePath);
            if (zipEntry != null) {
                return new ResourceLoader.ResourceEntry(zipEntry.getName(), zipEntry.isDirectory());
            } else {
                throw new FileNotFoundException(relativePath);
            }
        }

        @Override
        public InputStream getInputStreamFromEntry(ResourceLoader.ResourceEntry resource) throws IOException {
            ZipFile zipfile = this.getResourcePackZipFile();
            ZipEntry zipentry = zipfile.getEntry(resource.getPath());
            if (zipentry == null) {
                throw new FileNotFoundException(
                        String.format("'%s' in ResourcePack '%s'", this.dir, resource.getPath()));
            } else {
                return zipfile.getInputStream(zipentry);
            }
        }

        private ZipFile getResourcePackZipFile() throws IOException {
            if (this.resourcePackZipFile == null) {
                this.resourcePackZipFile = new ZipFile(this.dir);
            }

            return this.resourcePackZipFile;
        }

        @Override
        public void close() throws IOException {
            if (this.resourcePackZipFile != null) {
                this.resourcePackZipFile.close();
                this.resourcePackZipFile = null;
            }
        }
    }
}
