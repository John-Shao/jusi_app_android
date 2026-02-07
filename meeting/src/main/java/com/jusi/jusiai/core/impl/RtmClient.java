package com.jusi.jusiai.core.impl;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;
import com.ss.bytertc.engine.RTCVideo;
import com.jusi.jusiai.common.AbsBroadcast;
import com.jusi.jusiai.core.net.rtm.RTMBaseClient;
import com.jusi.jusiai.core.net.rtm.RTMBizInform;
import com.jusi.jusiai.core.net.rtm.RtmInfo;

public class RtmClient extends RTMBaseClient {

    public RtmClient(@NonNull RTCVideo RTCEngine, @NonNull RtmInfo rtmInfo) {
        super(RTCEngine, rtmInfo);
    }

    @Override
    protected JsonObject getCommonParams(String cmd) {
        throw new IllegalStateException("unexpected call");
    }

    @Override
    protected void initEventListener() {
    }

    public void putEventListener(AbsBroadcast<? extends RTMBizInform> absBroadcast) {
        mEventListeners.put(absBroadcast.getEvent(), absBroadcast);
    }

    public void removeEventListener() {
        mEventListeners.clear();
    }
}
