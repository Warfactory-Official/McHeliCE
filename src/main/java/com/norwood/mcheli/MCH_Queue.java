package com.norwood.mcheli;

import java.util.ArrayList;
import java.util.List;

public class MCH_Queue<T> {

    private int current;
    private final List<T> list;

    public MCH_Queue(int filterLength, T initVal) {
        if (filterLength <= 0) {
            filterLength = 1;
        }

        this.list = new ArrayList<>();

        for (int i = 0; i < filterLength; i++) {
            this.list.add(initVal);
        }

        this.current = 0;
    }

    public void clear(T clearVal) {
        for (int i = 0; i < this.size(); i++) {
            this.list.set(i, clearVal);
        }
    }

    public void put(T t) {
        this.list.set(this.current, t);
        this.current++;
        this.current = this.current % this.size();
    }

    private int getIndex(int offset) {
        offset %= this.size();
        int index = this.current + offset;
        return index < 0 ? index + this.size() : index % this.size();
    }

    public T oldest() {
        return this.list.get(this.getIndex(1));
    }

    public T get(int i) {
        return this.list.get(i);
    }

    public int size() {
        return this.list.size();
    }
}
