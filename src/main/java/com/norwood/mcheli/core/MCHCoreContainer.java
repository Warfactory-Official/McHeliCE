package com.norwood.mcheli.core;

import java.util.Arrays;

import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.ModMetadata;

import com.google.common.eventbus.EventBus;

public class MCHCoreContainer extends DummyModContainer {

    public MCHCoreContainer() {
        super(new ModMetadata());
        ModMetadata meta = getMetadata();
        meta.modId = "mchcore";
        meta.name = "MCHCore";
        meta.description = "McHeli core mod";
        meta.version = "1.12.2-2.0";
        meta.authorList = Arrays.asList("Movblock", "MrNorwood");
    }

    @Override
    public boolean registerBus(EventBus bus, LoadController controller) {
        bus.register(this);
        return true;
    }
}
