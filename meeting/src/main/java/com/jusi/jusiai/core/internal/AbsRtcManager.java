package com.jusi.jusiai.core.internal;

import androidx.annotation.NonNull;

import com.jusi.jusiai.core.impl.RtcDataProviderImpl;

public abstract class AbsRtcManager {

    protected RtcDataProviderImpl mDataProvider;

    public AbsRtcManager(@NonNull RtcDataProviderImpl dataProvider) {
        mDataProvider = dataProvider;
    }

    public abstract void dispose();
}
