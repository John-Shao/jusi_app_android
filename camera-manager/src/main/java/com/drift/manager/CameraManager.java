package com.drift.manager;

import android.content.Context;

import com.foreamlib.imageloader.ImageLoader;
import com.foreamlib.imageloader.GoproDrawableFileCache;

/**
 * Camera Manager - 管理相机相关的全局实例
 */
public class CameraManager {
    private static CameraManager instance;
    private ImageLoader imageloader;
    private String currentCamIP;

    private CameraManager() {
    }

    public static synchronized CameraManager getInstance() {
        if (instance == null) {
            instance = new CameraManager();
        }
        return instance;
    }

    /**
     * 初始化 CameraManager
     * 应该在 Application.onCreate() 中调用
     */
    public void init(Context context) {
        if (com.foreamlib.log.Log.DEBUG_IMAGE_LOADING) {
            imageloader = new ImageLoader(1);
        } else {
            imageloader = new ImageLoader(3);
        }
        GoproDrawableFileCache.initFileCache(context);
    }

    public ImageLoader getImageLoader() {
        return imageloader;
    }

    public String getCurrentCamIP() {
        return currentCamIP;
    }

    public void setCurrentCamIP(String currentCamIP) {
        this.currentCamIP = currentCamIP;
    }

    public void stopInternetTask() {
        if (imageloader != null) {
            imageloader.cancelImageLoading();
        }
        GoproDrawableFileCache.killAllTask();
    }
}
