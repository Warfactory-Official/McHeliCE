package com.norwood.mcheli;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TreeSet;

public class MCH_FileSearch {

    public static final int TYPE_FILE_OR_DIR = 1;
    public static final int TYPE_FILE = 2;
    public static final int TYPE_DIR = 3;
    private final TreeSet<File> set = new TreeSet<>();

    public File[] listFiles(String directoryPath, String fileName) {
        if (fileName != null) {
            fileName = fileName.replace(".", "\\.");
            fileName = fileName.replace("*", ".*");
        }

        return this.listFiles(directoryPath, fileName, 2, true, 0);
    }

    public File[] listFiles(String directoryPath, String fileNamePattern, int type, boolean isRecursive, int period) {
        File dir = new File(directoryPath);
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("引数で指定されたパス[" + dir.getAbsolutePath() + "]はディレクトリではありません。");
        } else {
            File[] files = dir.listFiles();

            for (File file : files) {
                this.addFile(type, fileNamePattern, this.set, file, period);
                if (isRecursive && file.isDirectory()) {
                    this.listFiles(file.getAbsolutePath(), fileNamePattern, type, true, period);
                }
            }

            return this.set.toArray(new File[0]);
        }
    }

    private void addFile(int type, String match, TreeSet<File> set, File file, int period) {
        switch (type) {
            case 2:
                if (!file.isFile()) {
                    return;
                }
                break;
            case 3:
                if (!file.isDirectory()) {
                    return;
                }
        }

        if (match == null || file.getName().matches(match)) {
            if (period != 0) {
                Date lastModifiedDate = new Date(file.lastModified());
                String lastModifiedDateStr = new SimpleDateFormat("yyyyMMdd").format(lastModifiedDate);
                long oneDayTime = 86400000L;
                long periodTime = oneDayTime * Math.abs(period);
                Date designatedDate = new Date(System.currentTimeMillis() - periodTime);
                String designatedDateStr = new SimpleDateFormat("yyyyMMdd").format(designatedDate);
                if (period > 0) {
                    if (lastModifiedDateStr.compareTo(designatedDateStr) >= 0) {}
                } else if (lastModifiedDateStr.compareTo(designatedDateStr) > 0) {
                    return;
                }
            }

            set.add(file);
        }
    }

    public void clear() {
        this.set.clear();
    }
}
