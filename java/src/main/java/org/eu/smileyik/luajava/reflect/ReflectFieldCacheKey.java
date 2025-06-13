package org.eu.smileyik.luajava.reflect;

import java.util.Objects;

public class ReflectFieldCacheKey {
    private final Class<?> clazz;
    private final String name;
    private final boolean ignoreFinal;
    private final boolean ignoreStatic;
    private final boolean ignoreNotStatic;
    private final boolean ignoreNotPublic;


    public ReflectFieldCacheKey(Class<?> clazz, String name,
                                boolean ignoreFinal, boolean ignoreStatic,
                                boolean ignoreNotStatic, boolean ignoreNotPublic) {
        this.clazz = clazz;
        this.name = name;
        this.ignoreFinal = ignoreFinal;
        this.ignoreStatic = ignoreStatic;
        this.ignoreNotStatic = ignoreNotStatic;
        this.ignoreNotPublic = ignoreNotPublic;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        ReflectFieldCacheKey that = (ReflectFieldCacheKey) object;
        return ignoreFinal == that.ignoreFinal &&
                ignoreStatic == that.ignoreStatic &&
                ignoreNotStatic == that.ignoreNotStatic &&
                ignoreNotPublic == that.ignoreNotPublic &&
                Objects.equals(clazz, that.clazz) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clazz, name, ignoreFinal, ignoreStatic, ignoreNotStatic, ignoreNotPublic);
    }
}
