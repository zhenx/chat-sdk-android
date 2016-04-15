package com.braunster.chatsdk.interfaces;

import com.braunster.chatsdk.dao.BUser;
import com.firebase.geofire.GeoLocation;

/**
 * Created by Erk on 05.04.2016.
 */
public interface GeoInterface {

    boolean userAdded(BUser user, GeoLocation location);
    boolean userMoved(BUser user, GeoLocation location);
    boolean userRemoved(BUser user);
    boolean setCurrentUserGeoLocation(GeoLocation location);
    boolean setState(int stringResId);
}
