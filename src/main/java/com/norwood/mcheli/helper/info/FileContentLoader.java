package com.norwood.mcheli.helper.info;

import com.google.common.collect.Lists;
import com.norwood.mcheli.helper.MCH_Logger;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileContentLoader extends ContentLoader implements Closeable {

    private ZipFile resourcePackZipFile;

    public FileContentLoader(String domain, File addonDir, String loaderVersion, Predicate<String> fileFilter) {
        super(domain, addonDir, loaderVersion, fileFilter);
    }

    private ZipFile getResourcePackZipFile() throws IOException {
        if (this.resourcePackZipFile == null) {
            this.resourcePackZipFile = new ZipFile(this.dir);
        }
        return this.resourcePackZipFile;
    }

    @Override
    protected List<ContentEntry> getEntries() {
        return this.walkEntries("2".equals(this.loaderVersion));
    }

    private List<ContentEntry> walkEntries(boolean findDeep) {
        List<ContentEntry> list = Lists.newLinkedList();

        try {
            ZipFile zipfile = this.getResourcePackZipFile();
            Iterator<? extends ZipEntry> itr = zipfile.stream().filter(ex -> this.filter(ex, findDeep)).iterator();

            while (itr.hasNext()) {
                String name = itr.next().getName();
                String[] s = name.split("/");
                String typeDirName = s.length >= 3 ? s[2] : null;
                ContentType type = ContentFactories.getType(typeDirName);
                if (type != null) {
                    list.add(this.makeEntry(name, type, false));
                }
            }
        } catch (IOException e) {
            MCH_Logger.get().error("IO Error from file loader!", e);
        }

        return list;
    }

    private boolean filter(ZipEntry zipEntry, boolean deep) {
        String name = zipEntry.getName();
        String[] split = name.split("/");
        String lootDir = split.length >= 2 ? split[0] : "";
        return !zipEntry.isDirectory() && (deep || "assets".equals(lootDir) || split.length <= 2) &&
                this.isReadable(name);
    }

    @Override
    protected InputStream getInputStreamByName(String name) throws IOException {
        ZipFile zipfile = this.getResourcePackZipFile();
        ZipEntry zipentry = zipfile.getEntry(name);
        if (zipentry == null) {
            throw new FileNotFoundException(String.format("'%s' in AddonPack '%s'", this.dir, name));
        }
        return zipfile.getInputStream(zipentry);
    }

    @Override
    protected void finalize() throws Throwable {
        this.close();
        super.finalize();
    }

    @Override
    public void close() throws IOException {
        if (this.resourcePackZipFile != null) {
            this.resourcePackZipFile.close();
            this.resourcePackZipFile = null;
        }
    }
}
