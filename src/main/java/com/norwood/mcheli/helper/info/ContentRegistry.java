package com.norwood.mcheli.helper.info;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.norwood.mcheli.MCH_BaseInfo;
import com.norwood.mcheli.helper.MCH_Logger;

public class ContentRegistry<T extends MCH_BaseInfo> {

    private final Class<T> contentClass;
    private final String dir;
    private final Map<String, T> registry;

    private ContentRegistry(Class<T> contentClass, String dir, Map<String, T> table) {
        this.contentClass = contentClass;
        this.dir = dir;
        this.registry = Maps.newHashMap(table);
    }

    private static <TYPE extends IContentData> void putTable(Map<String, TYPE> table, TYPE content) {
        table.put(content.getLocation().getPath(), content);
    }

    public static <TYPE extends MCH_BaseInfo> ContentRegistry.Builder<TYPE> builder(Class<TYPE> type, String dir) {
        return new ContentRegistry.Builder<>(type, dir);
    }

    @Nullable
    public T get(@Nullable String key) {
        return key == null ? null : this.registry.get(key);
    }

    @Nullable
    public T findFirst(Predicate<? super T> filter) {
        return this.registry.values().stream().filter(filter).findFirst().orElse(null);
    }

    public boolean reload(String key) {
        T content = this.get(key);
        if (content != null) {
            IContentData newContent = ContentRegistries.reparseContent(content, this.dir);
            if (this.contentClass.isInstance(newContent)) {
                T castedContent = this.contentClass.cast(newContent);

                this.registry.replace(key, castedContent);
                return true;
            }

            MCH_Logger.get().error("Content cast error, old dir:{}, new dir:{}", content.getClass(),
                    newContent.getClass());
        }

        return false;
    }

    public void reloadAll() {
        for (T content : ContentRegistries.reloadAllAddonContents(this)) {
            this.registry.replace(content.getLocation().getPath(), content);
        }
    }

    public List<T> values() {
        return ImmutableList.copyOf(this.registry.values());
    }

    public Set<Entry<String, T>> entries() {
        return this.registry.entrySet();
    }

    public void forEachValueParallel(Consumer<? super T> action) {
        this.registry.values().parallelStream().forEach(action);
    }

    public void forEachValue(Consumer<? super T> action) {
        this.registry.values().forEach(action);
    }

    public boolean contains(String key) {
        return this.registry.containsKey(key);
    }

    public int size() {
        return this.registry.size();
    }

    public Class<T> getType() {
        return this.contentClass;
    }

    public String getDirectoryName() {
        return this.dir;
    }

    public static class Builder<E extends MCH_BaseInfo> {

        private final Class<E> clazz;
        private final String dirName;
        private final Map<String, E> map = Maps.newHashMap();

        Builder(Class<E> clazz, String dir) {
            this.clazz = clazz;
            this.dirName = dir;
        }

        public void put(E content) {
            ContentRegistry.putTable(this.map, content);
        }

        public ContentRegistry<E> build() {
            return new ContentRegistry<>(this.clazz, this.dirName, this.map);
        }
    }
}
