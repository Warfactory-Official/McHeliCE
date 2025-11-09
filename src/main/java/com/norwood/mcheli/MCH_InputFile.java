package com.norwood.mcheli;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class MCH_InputFile {

    public File file = null;
    public BufferedReader br = null;

    public boolean open(String path) {
        this.close();
        this.file = new File(path);
        String filePath = this.file.getAbsolutePath();

        try {
            this.br = new BufferedReader(new FileReader(this.file));
            return true;
        } catch (FileNotFoundException var4) {
            MCH_Lib.DbgLog(true, "FILE open failed MCH_InputFile.open:" + filePath);
            var4.printStackTrace();
            return false;
        }
    }

    public boolean openUTF8(File file) {
        return this.openUTF8(file.getPath());
    }

    public boolean openUTF8(String path) {
        this.close();
        this.file = new File(path);

        try {
            this.br = new BufferedReader(
                    new InputStreamReader(Files.newInputStream(this.file.toPath()), StandardCharsets.UTF_8));
            return true;
        } catch (Exception var3) {
            var3.printStackTrace();
            return false;
        }
    }

    public String readLine() {
        try {
            return this.br != null ? this.br.readLine() : null;
        } catch (IOException var2) {
            return null;
        }
    }

    public void close() {
        try {
            if (this.br != null) {
                this.br.close();
            }
        } catch (IOException var2) {}

        this.br = null;
    }
}
