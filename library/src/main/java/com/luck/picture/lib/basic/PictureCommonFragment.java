package com.luck.picture.lib.basic;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.luck.picture.lib.PictureOnlyCameraFragment;
import com.luck.picture.lib.PictureSelectorPreviewFragment;
import com.luck.picture.lib.R;
import com.luck.picture.lib.config.Crop;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.config.PictureSelectionConfig;
import com.luck.picture.lib.config.SelectMimeType;
import com.luck.picture.lib.config.SelectModeConfig;
import com.luck.picture.lib.dialog.PhotoItemSelectedDialog;
import com.luck.picture.lib.dialog.PictureLoadingDialog;
import com.luck.picture.lib.dialog.RemindDialog;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.entity.MediaExtraInfo;
import com.luck.picture.lib.interfaces.OnCallbackIndexListener;
import com.luck.picture.lib.interfaces.OnCallbackListener;
import com.luck.picture.lib.interfaces.OnItemClickListener;
import com.luck.picture.lib.language.LanguageConfig;
import com.luck.picture.lib.language.PictureLanguageUtils;
import com.luck.picture.lib.loader.IBridgeMediaLoader;
import com.luck.picture.lib.manager.SelectedManager;
import com.luck.picture.lib.permissions.PermissionChecker;
import com.luck.picture.lib.permissions.PermissionResultCallback;
import com.luck.picture.lib.style.PictureWindowAnimationStyle;
import com.luck.picture.lib.thread.PictureThreadUtils;
import com.luck.picture.lib.utils.ActivityCompatHelper;
import com.luck.picture.lib.utils.AlbumUtils;
import com.luck.picture.lib.utils.BitmapUtils;
import com.luck.picture.lib.utils.MediaStoreUtils;
import com.luck.picture.lib.utils.MediaUtils;
import com.luck.picture.lib.utils.PictureFileUtils;
import com.luck.picture.lib.utils.SdkVersionUtils;
import com.luck.picture.lib.utils.StringUtils;
import com.luck.picture.lib.utils.ValueOf;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author：luck
 * @date：2021/11/19 10:02 下午
 * @describe：PictureCommonFragment
 */
public abstract class PictureCommonFragment extends Fragment implements IPictureSelectorCommonEvent {

    /**
     * PermissionResultCallback
     */
    private PermissionResultCallback mPermissionResultCallback;

    /**
     * page
     */
    protected int mPage = 1;

    /**
     * Media Loader engine
     */
    protected IBridgeMediaLoader mLoader;

    /**
     * IBridgePictureBehavior
     */
    protected IBridgePictureBehavior iBridgePictureBehavior;

    /**
     * PictureSelector Config
     */
    protected PictureSelectionConfig config;

    /**
     * Loading Dialog
     */
    private PictureLoadingDialog mLoadingDialog;

    /**
     * click sound
     */
    private SoundPool soundPool;

    /**
     * click sound effect id
     */
    private int soundID;


    @Override
    public int getResourceId() {
        return 0;
    }


    @Override
    public void onCheckOriginalChange() {

    }

    @Override
    public void dispatchCameraMediaResult(LocalMedia media) {

    }

    @Override
    public void subSelectPosition(boolean isRefreshAdapter) {

    }

    @Override
    public void onSelectedChange(boolean isAddRemove, LocalMedia currentMedia) {

    }

    @Override
    public void onLastSingleSelectedChange(LocalMedia oldLocalMedia) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (mPermissionResultCallback != null) {
            PermissionChecker.getInstance().onRequestPermissionsResult(grantResults, mPermissionResultCallback);
            mPermissionResultCallback = null;
        }
    }


    /**
     * Set PermissionResultCallback
     *
     * @param callback
     */
    public void setPermissionsResultAction(PermissionResultCallback callback) {
        mPermissionResultCallback = callback;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getResourceId() != 0) {
            return inflater.inflate(getResourceId(), container, false);
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mLoadingDialog = new PictureLoadingDialog(getContext());
        if (savedInstanceState != null) {
            config = savedInstanceState.getParcelable(PictureConfig.EXTRA_PICTURE_SELECTOR_CONFIG);
        }
        if (config == null) {
            config = PictureSelectionConfig.getInstance();
        }
        if (config.isOpenClickSound && !config.isOnlyCamera) {
            soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
            soundID = soundPool.load(getContext(), R.raw.ps_click_music, 1);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        initAppLanguage();
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (config != null) {
            outState.putParcelable(PictureConfig.EXTRA_PICTURE_SELECTOR_CONFIG, config);
        }
    }

    @Nullable
    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        PictureWindowAnimationStyle windowAnimationStyle = PictureSelectionConfig.selectorStyle.getWindowAnimationStyle();
        if (enter) {
            return AnimationUtils.loadAnimation(getActivity(), windowAnimationStyle.activityEnterAnimation);
        } else {
            return AnimationUtils.loadAnimation(getActivity(), windowAnimationStyle.activityExitAnimation);
        }
    }


    @Override
    public int confirmSelect(LocalMedia currentMedia, boolean isSelected) {
        String curMimeType = currentMedia.getMimeType();
        long curDuration = currentMedia.getDuration();
        List<LocalMedia> selectedResult = SelectedManager.getSelectedResult();
        if (config.selectionMode == SelectModeConfig.MULTIPLE) {
            if (config.isWithVideoImage) {
                // 共选型模式
                int selectVideoSize = 0;
                for (int i = 0; i < selectedResult.size(); i++) {
                    String mimeType = selectedResult.get(i).getMimeType();
                    if (PictureMimeType.isHasVideo(mimeType)) {
                        selectVideoSize++;
                    }
                }
                if (checkWithMimeTypeValidity(isSelected, curMimeType, selectVideoSize, curDuration)) {
                    return SelectedManager.INVALID;
                }
            } else {
                // 单一型模式
                if (checkOnlyMimeTypeValidity(isSelected, curMimeType, SelectedManager.getTopResultMimeType(), curDuration)) {
                    return SelectedManager.INVALID;
                }
            }
        }
        int resultCode;
        if (isSelected) {
            selectedResult.remove(currentMedia);
            resultCode = SelectedManager.REMOVE;
        } else {
            if (config.selectionMode == SelectModeConfig.SINGLE) {
                if (selectedResult.size() > 0) {
                    sendLastSelectedChangeEvent(selectedResult.get(0));
                    selectedResult.clear();
                }
            }
            selectedResult.add(currentMedia);
            currentMedia.setNum(selectedResult.size());
            resultCode = SelectedManager.ADD_SUCCESS;
            playClickEffect();
        }
        sendSelectedChangeEvent(resultCode == SelectedManager.ADD_SUCCESS, currentMedia);
        return resultCode;
    }

    @SuppressLint({"StringFormatInvalid", "StringFormatMatches"})
    @Override
    public boolean checkWithMimeTypeValidity(boolean isSelected, String curMimeType, int selectVideoSize, long duration) {
        if (PictureMimeType.isHasVideo(curMimeType)) {
            if (config.maxVideoSelectNum <= 0) {
                // 如果视频可选数量是0
                RemindDialog.showTipsDialog(getContext(), getString(R.string.ps_rule));
                return true;
            }

            if (!isSelected && SelectedManager.getSelectedResult().size() >= config.maxSelectNum) {
                RemindDialog.showTipsDialog(getContext(), getString(R.string.ps_message_max_num, config.maxSelectNum));
                return true;
            }

            if (!isSelected && selectVideoSize >= config.maxVideoSelectNum) {
                // 如果选择的是视频
                RemindDialog.showTipsDialog(getContext(), StringUtils.getMsg(getContext(), curMimeType, config.maxVideoSelectNum));
                return true;
            }

            if (!isSelected && config.videoMinSecond > 0 && duration < config.videoMinSecond) {
                // 视频小于最低指定的长度
                RemindDialog.showTipsDialog(getContext(), getString(R.string.ps_choose_min_seconds, config.videoMinSecond / 1000));
                return true;
            }

            if (!isSelected && config.videoMaxSecond > 0 && duration > config.videoMaxSecond) {
                // 视频时长超过了指定的长度
                RemindDialog.showTipsDialog(getContext(), getString(R.string.ps_choose_max_seconds, config.videoMaxSecond / 1000));
                return true;
            }
        } else {
            if (!isSelected && SelectedManager.getSelectedResult().size() >= config.maxSelectNum) {
                RemindDialog.showTipsDialog(getContext(), getString(R.string.ps_message_max_num, config.maxSelectNum));
                return true;
            }
        }
        return false;
    }


    @SuppressLint("StringFormatInvalid")
    @Override
    public boolean checkOnlyMimeTypeValidity(boolean isSelected, String curMimeType, String existMimeType, long duration) {
        boolean isSameMimeType = PictureMimeType.isMimeTypeSame(existMimeType, curMimeType);
        if (!isSameMimeType) {
            RemindDialog.showTipsDialog(getContext(), getString(R.string.ps_rule));
            return true;
        }
        if (PictureMimeType.isHasVideo(existMimeType) && config.maxVideoSelectNum > 0) {
            if (!isSelected && SelectedManager.getSelectedResult().size() >= config.maxVideoSelectNum) {
                // 如果先选择的是视频
                RemindDialog.showTipsDialog(getContext(), StringUtils.getMsg(getContext(), existMimeType, config.maxVideoSelectNum));
                return true;
            }
            if (!isSelected && config.videoMinSecond > 0 && duration < config.videoMinSecond) {
                // 视频小于最低指定的长度
                RemindDialog.showTipsDialog(getContext(), getString(R.string.ps_choose_min_seconds, config.videoMinSecond / 1000));
                return true;
            }

            if (!isSelected && config.videoMaxSecond > 0 && duration > config.videoMaxSecond) {
                // 视频时长超过了指定的长度
                RemindDialog.showTipsDialog(getContext(), getString(R.string.ps_choose_max_seconds, config.videoMaxSecond / 1000));
                return true;
            }
        } else {
            if (!isSelected && SelectedManager.getSelectedResult().size() >= config.maxSelectNum) {
                RemindDialog.showTipsDialog(getContext(), StringUtils.getMsg(getContext(), existMimeType, config.maxSelectNum));
                return true;
            }
            if (PictureMimeType.isHasVideo(curMimeType)) {
                if (!isSelected && config.videoMinSecond > 0 && duration < config.videoMinSecond) {
                    // 视频小于最低指定的长度
                    RemindDialog.showTipsDialog(getContext(), getString(R.string.ps_choose_min_seconds, config.videoMinSecond / 1000));
                    return true;
                }
                if (!isSelected && config.videoMaxSecond > 0 && duration > config.videoMaxSecond) {
                    // 视频时长超过了指定的长度
                    RemindDialog.showTipsDialog(getContext(), getString(R.string.ps_choose_max_seconds, config.videoMaxSecond / 1000));
                    return true;
                }
            }
        }
        return false;
    }


    @Override
    public void sendSelectedChangeEvent(boolean isAddRemove, LocalMedia currentMedia) {
        if (!ActivityCompatHelper.isDestroy(getActivity())) {
            List<Fragment> fragments = getActivity().getSupportFragmentManager().getFragments();
            for (int i = 0; i < fragments.size(); i++) {
                Fragment fragment = fragments.get(i);
                if (fragment instanceof PictureCommonFragment) {
                    ((PictureCommonFragment) fragment).onSelectedChange(isAddRemove, currentMedia);
                }
            }
        }
    }

    @Override
    public void sendLastSelectedChangeEvent(LocalMedia oldLocalMedia) {
        if (!ActivityCompatHelper.isDestroy(getActivity())) {
            List<Fragment> fragments = getActivity().getSupportFragmentManager().getFragments();
            for (int i = 0; i < fragments.size(); i++) {
                Fragment fragment = fragments.get(i);
                if (fragment instanceof PictureCommonFragment) {
                    ((PictureCommonFragment) fragment).onLastSingleSelectedChange(oldLocalMedia);
                }
            }
        }
    }

    @Override
    public void sendSelectedOriginalChangeEvent() {
        if (!ActivityCompatHelper.isDestroy(getActivity())) {
            List<Fragment> fragments = getActivity().getSupportFragmentManager().getFragments();
            for (int i = 0; i < fragments.size(); i++) {
                Fragment fragment = fragments.get(i);
                if (fragment instanceof PictureCommonFragment) {
                    ((PictureCommonFragment) fragment).onCheckOriginalChange();
                }
            }
        }
    }

    @Override
    public void openSelectedCamera() {
        switch (config.chooseMode) {
            case SelectMimeType.TYPE_ALL:
                if (config.ofAllCameraType == SelectMimeType.ofImage()) {
                    openImageCamera();
                } else if (config.ofAllCameraType == SelectMimeType.ofVideo()) {
                    openVideoCamera();
                } else {
                    onSelectedOnlyCamera();
                }
                break;
            case SelectMimeType.TYPE_IMAGE:
                openImageCamera();
                break;
            case SelectMimeType.TYPE_VIDEO:
                openVideoCamera();
                break;
            case SelectMimeType.TYPE_AUDIO:
                openSoundRecording();
                break;
            default:
                break;
        }
    }


    @Override
    public void onSelectedOnlyCamera() {
        PhotoItemSelectedDialog selectedDialog = PhotoItemSelectedDialog.newInstance();
        selectedDialog.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                switch (position) {
                    case PhotoItemSelectedDialog.IMAGE_CAMERA:
                        if (PictureSelectionConfig.interceptCameraListener != null) {
                            interceptCameraEvent(SelectMimeType.TYPE_IMAGE);
                        } else {
                            openImageCamera();
                        }
                        break;
                    case PhotoItemSelectedDialog.VIDEO_CAMERA:
                        if (PictureSelectionConfig.interceptCameraListener != null) {
                            interceptCameraEvent(SelectMimeType.TYPE_VIDEO);
                        } else {
                            openVideoCamera();
                        }
                        break;
                    default:
                        break;
                }
            }
        });
        selectedDialog.show(getChildFragmentManager(), "PhotoItemSelectedDialog");
    }

    @Override
    public void openImageCamera() {
        PermissionChecker.getInstance().requestPermissions(this,
                new String[]{Manifest.permission.CAMERA}, new PermissionResultCallback() {
                    @Override
                    public void onGranted() {
                        if (!ActivityCompatHelper.isDestroy(getActivity())) {
                            if (PictureSelectionConfig.interceptCameraListener != null) {
                                interceptCameraEvent(SelectMimeType.TYPE_IMAGE);
                            } else {
                                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                if (cameraIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                                    Uri imageUri = MediaStoreUtils.createCameraOutImageUri(getContext(), config);
                                    if (imageUri != null) {
                                        if (config.isCameraAroundState) {
                                            cameraIntent.putExtra(PictureConfig.CAMERA_FACING, PictureConfig.CAMERA_BEFORE);
                                        }
                                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                                        startActivityForResult(cameraIntent, PictureConfig.REQUEST_CAMERA);
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void onDenied() {

                    }
                });
    }


    @Override
    public void openVideoCamera() {
        PermissionChecker.getInstance().requestPermissions(this,
                new String[]{Manifest.permission.CAMERA}, new PermissionResultCallback() {
                    @Override
                    public void onGranted() {
                        if (!ActivityCompatHelper.isDestroy(getActivity())) {
                            if (PictureSelectionConfig.interceptCameraListener != null) {
                                interceptCameraEvent(SelectMimeType.TYPE_VIDEO);
                            } else {
                                Intent cameraIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                                if (cameraIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                                    Uri videoUri = MediaStoreUtils.createCameraOutVideoUri(getContext(), config);
                                    if (videoUri != null) {
                                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);
                                        if (config.isCameraAroundState) {
                                            cameraIntent.putExtra(PictureConfig.CAMERA_FACING, PictureConfig.CAMERA_BEFORE);
                                        }
                                        cameraIntent.putExtra(PictureConfig.EXTRA_QUICK_CAPTURE, config.isQuickCapture);
                                        cameraIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, config.recordVideoMaxSecond);
                                        cameraIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, config.videoQuality);
                                        startActivityForResult(cameraIntent, PictureConfig.REQUEST_CAMERA);
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void onDenied() {

                    }
                });
    }


    @Override
    public void openSoundRecording() {
        PermissionChecker.getInstance().requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO}, new PermissionResultCallback() {
                    @Override
                    public void onGranted() {
                        if (!ActivityCompatHelper.isDestroy(getActivity())) {
                            if (PictureSelectionConfig.interceptCameraListener != null) {
                                interceptCameraEvent(SelectMimeType.TYPE_AUDIO);
                            } else {
                                Intent cameraIntent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
                                if (cameraIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                                    Uri audioUri = MediaStoreUtils.createCameraOutAudioUri(getContext(), config);
                                    if (audioUri != null) {
                                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, audioUri);
                                        startActivityForResult(cameraIntent, PictureConfig.REQUEST_CAMERA);
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void onDenied() {

                    }
                });
    }


    /**
     * 拦截相机事件并处理返回结果
     */
    private void interceptCameraEvent(int cameraType) {
        config.cameraPath = PictureSelectionConfig.interceptCameraListener.openCamera(getContext(), cameraType);
    }

    /**
     * 点击选择的音效
     */
    private void playClickEffect() {
        if (soundPool != null && config.isOpenClickSound) {
            soundPool.play(soundID, 0.1f, 0.5f, 0, 1, 1);
        }
    }

    /**
     * 释放音效资源
     */
    private void releaseSoundPool() {
        try {
            if (soundPool != null) {
                soundPool.release();
                soundPool = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PictureConfig.REQUEST_CAMERA) {
                dispatchHandleCamera(data);
            } else if (requestCode == Crop.REQUEST_CROP) {
                Uri resultUri = Crop.getOutput(data);
                if (resultUri != null) {
                    int cropCount = Crop.getCropCount(data);
                    if (cropCount == SelectedManager.getCount()) {
                        List<LocalMedia> selectedResult = SelectedManager.getSelectedResult();
                        for (int i = 0; i < SelectedManager.getCount(); i++) {
                            if (TextUtils.isEmpty(resultUri.getPath())) {
                                continue;
                            }
                            LocalMedia media = selectedResult.get(i);
                            media.setCut(true);
                            media.setCutPath(resultUri.getPath());
                            media.setCropImageWidth(Crop.getOutputImageWidth(data));
                            media.setCropImageHeight(Crop.getOutputImageHeight(data));
                            media.setCropResultAspectRatio(Crop.getOutputCropAspectRatio(data));
                            media.setCropOffsetX(Crop.getOutputImageOffsetX(data));
                            media.setCropOffsetY(Crop.getOutputImageOffsetY(data));
                            media.setSandboxPath(media.getCutPath());
                        }
                        List<LocalMedia> result = new ArrayList<>(selectedResult);
                        onResultEvent(result);
                    }
                } else {
                    Toast.makeText(getContext(), "image crop error，data is null", Toast.LENGTH_LONG).show();
                }
            }
        } else if (resultCode == Crop.RESULT_CROP_ERROR) {
            Throwable throwable = data != null ? Crop.getError(data) : new Throwable("image crop error");
            if (throwable != null) {
                Toast.makeText(getContext(), throwable.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            if (requestCode == PictureConfig.REQUEST_CAMERA) {
                MediaUtils.deleteUri(getContext(), config.cameraPath);
            }
        }
    }

    /**
     * 相机事件回调处理
     */
    private void dispatchHandleCamera(Intent intent) {
        PictureThreadUtils.executeByIo(new PictureThreadUtils.SimpleTask<LocalMedia>() {

            @Override
            public LocalMedia doInBackground() {
                if (config.chooseMode == SelectMimeType.ofAudio()) {
                    handleAudioEvent(intent);
                }
                if (TextUtils.isEmpty(config.cameraPath)) {
                    return null;
                }
                return buildLocalMedia();
            }

            @Override
            public void onSuccess(LocalMedia result) {
                PictureThreadUtils.cancel(this);
                if (result != null) {
                    dispatchCameraMediaResult(result);
                    onScannerScanFile(result);
                }
            }
        });
    }

    /**
     * 刷新相册
     *
     * @param localMedia 要刷新的对象
     */
    private void onScannerScanFile(LocalMedia localMedia) {
        if (ActivityCompatHelper.isDestroy(getActivity())) {
            return;
        }
        if (SdkVersionUtils.isQ()) {
            if (PictureMimeType.isHasVideo(localMedia.getMimeType()) && PictureMimeType.isContent(config.cameraPath)) {
                new PictureMediaScannerConnection(getActivity(), localMedia.getRealPath());
            }
        } else {
            new PictureMediaScannerConnection(getActivity(), config.cameraPath);
            if (PictureMimeType.isHasImage(localMedia.getMimeType())) {
                int lastImageId = MediaUtils.getDCIMLastImageId(getContext());
                if (lastImageId != -1) {
                    MediaUtils.removeMedia(getContext(), lastImageId);
                }
            }
        }
    }

    /**
     * buildLocalMedia
     */
    private LocalMedia buildLocalMedia() {
        if (ActivityCompatHelper.isDestroy(getActivity())) {
            return null;
        }
        long id, bucketId;
        File cameraFile;
        Uri mimeTypeUri;
        if (PictureMimeType.isContent(config.cameraPath)) {
            Uri cameraUri = Uri.parse(config.cameraPath);
            mimeTypeUri = cameraUri;
            String path = PictureFileUtils.getPath(getActivity(), cameraUri);
            cameraFile = new File(path);
            int lastIndexOf = config.cameraPath.lastIndexOf("/") + 1;
            id = lastIndexOf > 0 ? ValueOf.toLong(config.cameraPath.substring(lastIndexOf)) : System.currentTimeMillis();
            bucketId = AlbumUtils.generateCameraBucketId(getContext(), cameraFile, "");
        } else {
            cameraFile = new File(config.cameraPath);
            mimeTypeUri = Uri.fromFile(cameraFile);
            if (config.isCameraRotateImage) {
                BitmapUtils.rotateImage(getContext(), config.cameraPath);
            }
            id = System.currentTimeMillis();
            bucketId = AlbumUtils.generateCameraBucketId(getContext(), cameraFile, config.outPutCameraPath);
        }
        String mimeType = PictureMimeType.getMimeTypeFromMediaContentUri(getActivity(), mimeTypeUri);
        MediaExtraInfo mediaExtraInfo;
        if (PictureMimeType.isHasVideo(mimeType)) {
            mediaExtraInfo = MediaUtils.getVideoSize(getContext(), config.cameraPath);
        } else if (PictureMimeType.isHasAudio(mimeType)) {
            mediaExtraInfo = MediaUtils.getAudioSize(getContext(), config.cameraPath);
        } else {
            mediaExtraInfo = MediaUtils.getImageSize(getContext(), config.cameraPath);
        }
        String folderName = AlbumUtils.generateCameraFolderName(config.cameraPath, mimeType, config.outPutCameraPath);
        LocalMedia media = LocalMedia.parseLocalMedia(id, config.cameraPath, cameraFile.getAbsolutePath(),
                cameraFile.getName(), folderName, mediaExtraInfo.getDuration(), config.chooseMode,
                mimeType, mediaExtraInfo.getWidth(), mediaExtraInfo.getHeight(), cameraFile.length(), bucketId,
                cameraFile.lastModified() / 1000);
        if (SdkVersionUtils.isQ() && !PictureMimeType.isContent(config.cameraPath)) {
            media.setSandboxPath(config.cameraPath);
        }
        return media;
    }

    /**
     * 针对音频的处理逻辑
     *
     * @param intent
     */
    private void handleAudioEvent(Intent intent) {
        if (intent == null) {
            return;
        }
        if (ActivityCompatHelper.isDestroy(getActivity())) {
            return;
        }
        if (SdkVersionUtils.isR()) {
            config.cameraPath = PictureMimeType.isContent(intent.getData().toString()) ? intent.getData().toString() : intent.getData().getPath();
            Uri audioOutUri = MediaStoreUtils.createAudioUri(getActivity(), config.cameraAudioFormatForQ);
            if (audioOutUri != null) {
                InputStream inputStream = PictureContentResolver.getContentResolverOpenInputStream(getActivity(), Uri.parse(config.cameraPath));
                OutputStream outputStream = PictureContentResolver.getContentResolverOpenOutputStream(getActivity(), audioOutUri);
                PictureFileUtils.writeFileFromIS(inputStream, outputStream);
                config.cameraPath = audioOutUri.toString();
            }
        }
    }

    /**
     * 分发处理结果，比如压缩、裁剪、沙盒路径转换
     */
    protected void dispatchTransformResult() {
        List<LocalMedia> selectedResult = SelectedManager.getSelectedResult();
        List<LocalMedia> result = new ArrayList<>(selectedResult);
        if (checkCropValidity()) {
            if (result.size() == 1) {
                PictureSelectionConfig.cropEngine.onStartSingleCrop(getActivity(), this, result.get(0));
            } else {
                LocalMedia firstImageMedia = null;
                for (int i = 0; i < result.size(); i++) {
                    if (PictureMimeType.isHasImage(result.get(i).getMimeType())) {
                        firstImageMedia = result.get(i);
                        break;
                    }
                }
                PictureSelectionConfig.cropEngine.onStartMultipleCrop(getContext(), this, firstImageMedia, result);
            }
        } else if (checkCompressValidity()) {
            showLoading();
            PictureSelectionConfig.compressEngine.onStartCompress(getContext(), result,
                    new OnCallbackListener<List<LocalMedia>>() {
                        @Override
                        public void onCall(List<LocalMedia> result) {
                            onResultEvent(result);
                        }
                    });

        } else if (PictureSelectionConfig.sandboxFileEngine != null && result.size() > 0) {
            showLoading();
            for (int i = 0; i < result.size(); i++) {
                LocalMedia media = result.get(i);
                PictureSelectionConfig.sandboxFileEngine.onStartSandboxFileTransform(getContext(), i,
                        media, new OnCallbackIndexListener<LocalMedia>() {
                            @Override
                            public void onCall(LocalMedia data, int index) {
                                if (result.size() > index) {
                                    LocalMedia media = result.get(index);
                                    media.setSandboxPath(data.getSandboxPath());
                                }
                                if (index == result.size() - 1) {
                                    onResultEvent(result);
                                }
                            }
                        });
            }

        } else {
            onResultEvent(result);
        }
    }

    /**
     * 验证裁剪的可行性
     *
     * @return
     */
    private boolean checkCropValidity() {
        if (PictureSelectionConfig.cropEngine != null) {
            if (SelectedManager.getCount() == 1) {
                return PictureMimeType.isHasImage(SelectedManager.getTopResultMimeType());
            } else {
                for (int i = 0; i < SelectedManager.getCount(); i++) {
                    LocalMedia media = SelectedManager.getSelectedResult().get(i);
                    if (PictureMimeType.isHasImage(media.getMimeType())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 验证压缩的可行性
     *
     * @return
     */
    private boolean checkCompressValidity() {
        if (PictureSelectionConfig.compressEngine != null) {
            for (int i = 0; i < SelectedManager.getCount(); i++) {
                LocalMedia media = SelectedManager.getSelectedResult().get(i);
                if (PictureMimeType.isHasImage(media.getMimeType())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 返回处理完成后的选择结果
     */
    @Override
    public void onResultEvent(List<LocalMedia> result) {
        dismissLoading();
        if (PictureSelectionConfig.resultCallListener != null) {
            PictureSelectionConfig.resultCallListener.onResult(result);
        }
        if (config.isOnlyCamera) {
            if (!ActivityCompatHelper.isDestroy(getActivity())) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        } else {
            if (this instanceof PictureSelectorPreviewFragment) {
                iBridgePictureBehavior.onImmediateFinish();
            } else {
                iBridgePictureBehavior.onFinish();
            }
        }
    }

    /**
     * set app language
     */
    @Override
    public void initAppLanguage() {
        PictureSelectionConfig config = PictureSelectionConfig.getInstance();
        if (config.language != LanguageConfig.UNKNOWN_LANGUAGE && !config.isOnlyCamera) {
            PictureLanguageUtils.setAppLanguage(getActivity(), config.language);
        }
    }


    @Override
    public void showLoading() {
        try {
            if (ActivityCompatHelper.isDestroy(getActivity())) {
                return;
            }
            if (mLoadingDialog.isShowing()) {
                mLoadingDialog.dismiss();
            }
            mLoadingDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void dismissLoading() {
        try {
            if (ActivityCompatHelper.isDestroy(getActivity())) {
                return;
            }
            if (mLoadingDialog.isShowing()) {
                mLoadingDialog.dismiss();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        releaseSoundPool();
        super.onDestroy();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        initAppLanguage();
        super.onAttach(context);
        if (getParentFragment() instanceof IBridgePictureBehavior) {
            iBridgePictureBehavior = (IBridgePictureBehavior) getParentFragment();
        } else if (context instanceof IBridgePictureBehavior) {
            iBridgePictureBehavior = (IBridgePictureBehavior) context;
        } else {
            if (this instanceof PictureOnlyCameraFragment || this instanceof PictureSelectorPreviewFragment) {
                /**
                 * {@link PictureSelector.openCamera or startPreview}
                 * <p>
                 *     不需要使用到IBridgePictureBehavior，可以忽略
                 * </p>
                 */
            } else {
                throw new IllegalArgumentException(context.toString()
                        + " please must implement IBridgePictureBehavior");
            }
        }
    }
}
