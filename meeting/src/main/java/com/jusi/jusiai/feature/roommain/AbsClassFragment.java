package com.jusi.jusiai.feature.roommain;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.jusi.jusiai.core.WhiteBoardService;
import com.jusi.jusiai.framework.UIRoomMgr;
import com.jusi.jusiai.framework.classLarge.IUIEduDef;
import com.jusi.jusiai.framework.classLarge.UIEduRoom;

public abstract class AbsClassFragment extends Fragment {

    private UIEduRoom mUIEduRoom;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUIEduRoom = UIRoomMgr.eduRoom();
    }

    public UIEduRoom getUIRoom() {
        return mUIEduRoom;
    }

    public WhiteBoardService getWhiteBoardService(){
        return mUIEduRoom.getWhiteBoardService();
    }

    public IUIEduDef.IEduDataProvider getDataProvider() {
        return mUIEduRoom.getDataProvider();
    }
}
