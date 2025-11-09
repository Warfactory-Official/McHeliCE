package com.norwood.mcheli.eval.eval.var;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class MapVariable implements Variable {

    protected Map<Object, Object> map;

    public MapVariable() {
        this(new HashMap<>());
    }

    public MapVariable(Map<Object, Object> varMap) {
        this.map = varMap;
    }

    public Map<Object, Object> getMap() {
        return this.map;
    }

    public void setMap(Map<Object, Object> varMap) {
        this.map = varMap;
    }

    @Override
    public void setValue(Object name, Object obj) {
        this.map.put(name, obj);
    }

    @Override
    public Object getObject(Object name) {
        return this.map.get(name);
    }

    @Override
    public long evalLong(Object val) {
        return ((Number) val).longValue();
    }

    @Override
    public double evalDouble(Object val) {
        return ((Number) val).doubleValue();
    }

    @Override
    public Object getObject(Object array, int index) {
        return Array.get(array, index);
    }

    @Override
    public void setValue(Object array, int index, Object val) {
        Array.set(array, index, val);
    }

    @Override
    public Object getObject(Object obj, String field) {
        try {
            Class<?> c = obj.getClass();
            Field f = c.getField(field);
            return f.get(obj);
        } catch (RuntimeException var5) {
            throw var5;
        } catch (Exception var6) {
            throw new RuntimeException(var6);
        }
    }

    @Override
    public void setValue(Object obj, String field, Object val) {
        try {
            Class<?> c = obj.getClass();
            Field f = c.getField(field);
            f.set(obj, val);
        } catch (RuntimeException var6) {
            throw var6;
        } catch (Exception var7) {
            throw new RuntimeException(var7);
        }
    }
}
