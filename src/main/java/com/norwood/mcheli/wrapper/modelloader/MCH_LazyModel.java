package com.norwood.mcheli.wrapper.modelloader;

import com.norwood.mcheli.MCH_OnDemandModels;
import com.norwood.mcheli.aircraft.MCH_AircraftInfo;
import com.norwood.mcheli.helper.client.IModelCustom;


public class MCH_LazyModel implements IModelCustom {

    private final MCH_AircraftInfo info;

    public MCH_LazyModel(MCH_AircraftInfo info) {
        this.info = info;
    }

    private void request() {
        MCH_OnDemandModels.notifyRendered(this.info);
    }

    @Override
    public String getType() {
        return "lazy";
    }

    @Override
    public void renderAll() {
        request();
    }

    @Override
    public void renderOnly(String... groupNames) {
        request();
    }

    @Override
    public void renderPart(String partName) {
        request();
    }

    @Override
    public void renderAllExcept(String... excludedGroupNames) {
        request();
    }

    @Override
    public IModelCustom toVBO() {
        return this;
    }
}
