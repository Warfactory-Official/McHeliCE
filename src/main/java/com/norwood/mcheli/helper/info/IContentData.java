package com.norwood.mcheli.helper.info;

import com.norwood.mcheli.helper.addon.AddonResourceLocation;

public interface IContentData {

    boolean validate() throws Exception;

    void onPostReload();

    AddonResourceLocation getLocation();

    String getContentPath();
}
