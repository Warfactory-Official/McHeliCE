package com.norwood.mcheli;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MCH_OStream extends ByteArrayOutputStream {

    public static final int SIZE = 30720;
    public int index = 0;

    public void write(DataOutputStream dos) {
        try {
            int datasize;
            if (this.index + SIZE <= this.size()) {
                datasize = SIZE;
            } else {
                datasize = this.size() - this.index;
            }

            dos.writeInt(this.index);
            dos.writeInt(datasize);
            dos.writeInt(this.size());
            dos.write(this.buf, this.index, datasize);
            this.index += datasize;
        } catch (IOException var3) {
            var3.printStackTrace();
        }
    }

    public boolean isDataEnd() {
        return this.index >= this.size();
    }
}
