package com.example.cdac.servicetouploaddata;

/**
 * Created by cdac on 23/6/15.
 */
public interface DataChangeListener {
    public void DataChangedEvent(int newCount, float newDistance, float newAngle, String newTime);

}
