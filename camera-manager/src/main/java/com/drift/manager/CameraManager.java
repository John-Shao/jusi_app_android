package com.drift.manager;

import android.content.Context;

import com.foreamlib.imageloader.ImageLoader;
import com.foreamlib.imageloader.GoproDrawableFileCache;

import java.util.HashMap;
import java.util.Map;

/**
 * Camera Manager - 管理相机相关的全局实例
 */
public class CameraManager {
    private static CameraManager instance;
    private ImageLoader imageloader;
    private String currentCamIP;
    // 存储每个相机的会议状态 <camIP, isInMeeting>
    private Map<String, Boolean> cameraInMeetingMap;

    private CameraManager() {
        cameraInMeetingMap = new HashMap<>();
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

    /**
     * 设置相机的会议状态
     * @param camIP 相机IP地址
     * @param isInMeeting 是否在会议中
     */
    public void setCameraInMeeting(String camIP, boolean isInMeeting) {
        if (camIP != null) {
            cameraInMeetingMap.put(camIP, isInMeeting);
        }
    }

    /**
     * 获取相机的会议状态
     * @param camIP 相机IP地址
     * @return 是否在会议中，默认false
     */
    public boolean isCameraInMeeting(String camIP) {
        if (camIP != null && cameraInMeetingMap.containsKey(camIP)) {
            return cameraInMeetingMap.get(camIP);
        }
        return false;
    }

    /**
     * 清除指定相机的会议状态
     * @param camIP 相机IP地址
     */
    public void clearCameraInMeeting(String camIP) {
        if (camIP != null) {
            cameraInMeetingMap.remove(camIP);
        }
    }
}
