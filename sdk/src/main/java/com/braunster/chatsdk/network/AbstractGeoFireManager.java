package com.braunster.chatsdk.network;

import com.braunster.chatsdk.interfaces.GeoInterface;

/**
 * Created by Erk on 05.04.2016.
 */
public abstract class AbstractGeoFireManager {

    public abstract void setGeoDelegate(GeoInterface delegate);

    public abstract void start();
}
