package com.luck.picture.lib.basic;

import com.luck.picture.lib.config.PictureSelectionConfig;
import com.luck.picture.lib.entity.LocalMedia;

import java.util.List;

/**
 * @author：luck
 * @date：2021/11/24 10:11 上午
 * @describe：IPictureSelectorCommonEvent
 */
public interface IPictureSelectorCommonEvent {

    /**
     * View Layout
     *
     * @return resource Id
     */
    int getResourceId();

    /**
     * 权限被拒
     */
    void handlePermissionDenied();

    /**
     * 权限设置结果
     */
    void handlePermissionSettingResult();

    /**
     * 设置app语言
     */
    void initAppLanguage();

    /**
     * 选择拍照或拍视频
     */
    void onSelectedOnlyCamera();

    /**
     * 选择相机类型；拍照、视频、或录音
     */
    void openSelectedCamera();

    /**
     * 拍照
     */
    void openImageCamera();

    /**
     * 拍视频
     */
    void openVideoCamera();

    /**
     * 录音
     */
    void openSoundRecording();

    /**
     * 选择结果
     *
     * @param currentMedia 当前操作对象
     * @param isSelected   选中状态
     * @return 返回当前选择的状态
     */
    int confirmSelect(LocalMedia currentMedia, boolean isSelected);

    /**
     * 验证共选类型模式可选条件
     *
     * @param isSelected      资源是否被选中
     * @param curMimeType     选择的资源类型
     * @param selectVideoSize 已选的视频数量
     * @param duration        视频时长
     * @return
     */
    boolean checkWithMimeTypeValidity(boolean isSelected, String curMimeType, int selectVideoSize, long duration);

    /**
     * 验证单一类型模式可选条件
     *
     * @param isSelected    资源是否被选中
     * @param curMimeType   选择的资源类型
     * @param existMimeType 已选的资源类型
     * @param duration      视频时长
     * @return
     */
    boolean checkOnlyMimeTypeValidity(boolean isSelected, String curMimeType, String existMimeType, long duration);

    /**
     * 选择结果数据发生改变
     *
     * @param isAddRemove  isAddRemove  添加还是移除操作
     * @param currentMedia 当前操作的对象
     */
    void onSelectedChange(boolean isAddRemove, LocalMedia currentMedia);

    /**
     * 上一次单选的数据
     */
    void onLastSingleSelectedChange(LocalMedia oldLocalMedia);

    /**
     * 分发拍照后生成的LocalMedia
     *
     * @param media
     */
    void dispatchCameraMediaResult(LocalMedia media);

    /**
     * {@link PictureSelectionConfig.selectorStyle.getSelectMainStyle().isSelectNumberStyle}
     * <p>
     * isSelectNumberStyle模式下对选择结果编号进行排序
     * </p>
     *
     * @param isRefreshAdapter
     */
    void subSelectPosition(boolean isRefreshAdapter);

    /**
     * 发送选择数据发生变化的通知
     *
     * @param isAddRemove  添加还是移除操作
     * @param currentMedia 当前操作的对象
     */
    void sendSelectedChangeEvent(boolean isAddRemove, LocalMedia currentMedia);

    /**
     * 发送上一次选择数据发生变化的通知
     */
    void sendLastSelectedChangeEvent(LocalMedia oldLocalMedia);


    /**
     * 原图选项发生变化
     */
    void sendSelectedOriginalChangeEvent();

    /**
     * 原图选项发生变化
     */
    void onCheckOriginalChange();

    /**
     * 选择结果回调
     *
     * @param result
     */
    void onResultEvent(List<LocalMedia> result);

    /**
     * show loading
     */
    void showLoading();

    /**
     * dismiss loading
     */
    void dismissLoading();
}
