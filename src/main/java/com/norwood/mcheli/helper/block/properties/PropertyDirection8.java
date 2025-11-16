package com.norwood.mcheli.helper.block.properties;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.norwood.mcheli.helper.block.EnumDirection8;
import net.minecraft.block.properties.PropertyEnum;

import java.util.Collection;

public class PropertyDirection8 extends PropertyEnum<EnumDirection8> {

    protected PropertyDirection8(String name, Collection<EnumDirection8> allowedValues) {
        super(name, EnumDirection8.class, allowedValues);
    }

    public static PropertyDirection8 create(String name) {
        return create(name, Predicates.alwaysTrue());
    }

    public static PropertyDirection8 create(String name, Predicate<EnumDirection8> filter) {
        return create(name, Collections2.filter(Lists.newArrayList(EnumDirection8.values()), filter));
    }

    public static PropertyDirection8 create(String name, Collection<EnumDirection8> allowedValues) {
        return new PropertyDirection8(name, allowedValues);
    }
}
