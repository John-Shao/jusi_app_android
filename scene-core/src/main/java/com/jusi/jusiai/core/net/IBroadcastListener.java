package com.jusi.jusiai.core.net;

public interface IBroadcastListener<T> {

    String getEvent();

    Class<T> getDataClass();

    void onListener(T t);
}
