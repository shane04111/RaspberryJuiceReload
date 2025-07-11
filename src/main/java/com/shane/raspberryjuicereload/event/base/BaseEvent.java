package com.shane.raspberryjuicereload.event.base;

import com.shane.raspberryjuicereload.manager.LocationManager;

public abstract class BaseEvent {
    protected final LocationManager locationManager;

    public BaseEvent(LocationManager locationManager) {
        this.locationManager = locationManager;
    }
}
