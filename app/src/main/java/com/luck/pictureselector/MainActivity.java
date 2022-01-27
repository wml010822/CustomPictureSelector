package com.luck.pictureselector;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.luck.lib.camerax.CameraImageEngine;
import com.luck.lib.camerax.SimpleCameraX;
import com.luck.picture.lib.animators.AnimationType;
import com.luck.picture.lib.app.PictureAppMaster;
import com.luck.picture.lib.basic.IBridgePictureBehavior;
import com.luck.picture.lib.basic.PictureCommonFragment;
import com.luck.picture.lib.basic.PictureSelectionCameraModel;
import com.luck.picture.lib.basic.PictureSelectionModel;
import com.luck.picture.lib.basic.PictureSelectionSystemModel;
import com.luck.picture.lib.basic.PictureSelector;
import com.luck.picture.lib.config.InjectResourceSource;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.config.PictureSelectionConfig;
import com.luck.picture.lib.config.SelectLimitType;
import com.luck.picture.lib.config.SelectMimeType;
import com.luck.picture.lib.config.SelectModeConfig;
import com.luck.picture.lib.decoration.GridSpacingItemDecoration;
import com.luck.picture.lib.engine.CompressEngine;
import com.luck.picture.lib.engine.CropEngine;
import com.luck.picture.lib.engine.ExtendLoaderEngine;
import com.luck.picture.lib.engine.SandboxFileEngine;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.entity.LocalMediaFolder;
import com.luck.picture.lib.entity.MediaExtraInfo;
import com.luck.picture.lib.interfaces.OnCallbackIndexListener;
import com.luck.picture.lib.interfaces.OnCallbackListener;
import com.luck.picture.lib.interfaces.OnCameraInterceptListener;
import com.luck.picture.lib.interfaces.OnExternalPreviewEventListener;
import com.luck.picture.lib.interfaces.OnInjectLayoutResourceListener;
import com.luck.picture.lib.interfaces.OnMediaEditInterceptListener;
import com.luck.picture.lib.interfaces.OnQueryAlbumListener;
import com.luck.picture.lib.interfaces.OnQueryAllAlbumListener;
import com.luck.picture.lib.interfaces.OnQueryDataResultListener;
import com.luck.picture.lib.interfaces.OnResultCallbackListener;
import com.luck.picture.lib.interfaces.OnSelectLimitTipsListener;
import com.luck.picture.lib.language.LanguageConfig;
import com.luck.picture.lib.loader.SandboxFileLoader;
import com.luck.picture.lib.style.BottomNavBarStyle;
import com.luck.picture.lib.style.PictureSelectorStyle;
import com.luck.picture.lib.style.PictureWindowAnimationStyle;
import com.luck.picture.lib.style.SelectMainStyle;
import com.luck.picture.lib.style.TitleBarStyle;
import com.luck.picture.lib.utils.DateUtils;
import com.luck.picture.lib.utils.DensityUtil;
import com.luck.picture.lib.utils.MediaUtils;
import com.luck.picture.lib.utils.SandboxTransformUtils;
import com.luck.picture.lib.utils.SdkVersionUtils;
import com.luck.picture.lib.utils.StyleUtils;
import com.luck.picture.lib.utils.ToastUtils;
import com.luck.picture.lib.utils.ValueOf;
import com.luck.pictureselector.adapter.GridImageAdapter;
import com.luck.pictureselector.listener.DragListener;
import com.luck.pictureselector.listener.OnItemLongClickListener;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropImageEngine;
import com.yalantis.ucrop.model.AspectRatio;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import top.zibin.luban.CompressionPredicate;
import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;
import top.zibin.luban.OnRenameListener;

/**
 * @author：luck
 * @data：2019/12/20 晚上 23:12
 * @描述: Demo
 */

public class MainActivity extends AppCompatActivity implements IBridgePictureBehavior, View.OnClickListener,
        RadioGroup.OnCheckedChangeListener, CompoundButton.OnCheckedChangeListener {
    private final static String TAG = "PictureSelectorTag";
    private final static int ACTIVITY_RESULT = 1;
    private final static int CALLBACK_RESULT = 2;
    private final static int LAUNCHER_RESULT = 3;
    private GridImageAdapter mAdapter;
    private int maxSelectNum = 9;
    private TextView tv_select_num;
    private TextView tv_original_tips;
    private TextView tvDeleteText;
    private RadioGroup rgb_crop;
    private int aspect_ratio_x, aspect_ratio_y;
    private CheckBox cb_voice, cb_choose_mode, cb_isCamera, cb_isGif,
            cb_preview_img, cb_preview_video, cb_crop, cb_compress,
            cb_mode, cb_hide, cb_crop_circular, cb_crop_use_bitmap, cb_styleCrop, cb_showCropGrid,
            cb_showCropFrame, cb_preview_audio, cb_original, cb_single_back,
            cb_custom_camera, cbPage, cbEnabledMask, cbEditor, cb_custom_sandbox, cb_only_dir,
            cb_preview_full, cb_preview_scale, cb_inject_layout, cb_time_axis, cb_WithImageVideo,
            cb_system_album, cb_fast_select,cb_skip_not_gif,cb_not_gif;
    private int chooseMode = SelectMimeType.ofAll();
    private boolean isHasLiftDelete;
    private boolean needScaleBig = true;
    private boolean needScaleSmall = false;
    private int language = LanguageConfig.UNKNOWN_LANGUAGE;
    private int x = 0, y = 0;
    private int animationMode = AnimationType.DEFAULT_ANIMATION;
    private PictureSelectorStyle selectorStyle;
    private final List<LocalMedia> mData = new ArrayList<>();
    private ActivityResultLauncher<Intent> launcherResult;
    private int resultMode = LAUNCHER_RESULT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        selectorStyle = new PictureSelectorStyle();
        ImageView minus = findViewById(R.id.minus);
        ImageView plus = findViewById(R.id.plus);
        tvDeleteText = findViewById(R.id.tv_delete_text);
        tv_select_num = findViewById(R.id.tv_select_num);
        tv_original_tips = findViewById(R.id.tv_original_tips);
        rgb_crop = findViewById(R.id.rgb_crop);
        RadioGroup rgb_result = findViewById(R.id.rgb_result);
        RadioGroup rgb_style = findViewById(R.id.rgb_style);
        RadioGroup rgb_animation = findViewById(R.id.rgb_animation);
        RadioGroup rgb_list_anim = findViewById(R.id.rgb_list_anim);
        RadioGroup rgb_photo_mode = findViewById(R.id.rgb_photo_mode);
        RadioGroup rgb_language = findViewById(R.id.rgb_language);
        cb_voice = findViewById(R.id.cb_voice);
        cb_choose_mode = findViewById(R.id.cb_choose_mode);
        cb_isCamera = findViewById(R.id.cb_isCamera);
        cb_isGif = findViewById(R.id.cb_isGif);
        cb_WithImageVideo = findViewById(R.id.cbWithImageVideo);
        cb_system_album = findViewById(R.id.cb_system_album);
        cb_fast_select = findViewById(R.id.cb_fast_select);
        cb_preview_full = findViewById(R.id.cb_preview_full);
        cb_preview_scale = findViewById(R.id.cb_preview_scale);
        cb_inject_layout = findViewById(R.id.cb_inject_layout);
        cb_preview_img = findViewById(R.id.cb_preview_img);
        cb_preview_video = findViewById(R.id.cb_preview_video);
        cb_time_axis = findViewById(R.id.cb_time_axis);
        cb_crop = findViewById(R.id.cb_crop);
        cbPage = findViewById(R.id.cbPage);
        cbEditor = findViewById(R.id.cb_editor);
        cbEnabledMask = findViewById(R.id.cbEnabledMask);
        cb_styleCrop = findViewById(R.id.cb_styleCrop);
        cb_compress = findViewById(R.id.cb_compress);
        cb_mode = findViewById(R.id.cb_mode);
        cb_custom_sandbox = findViewById(R.id.cb_custom_sandbox);
        cb_only_dir = findViewById(R.id.cb_only_dir);
        cb_showCropGrid = findViewById(R.id.cb_showCropGrid);
        cb_showCropFrame = findViewById(R.id.cb_showCropFrame);
        cb_preview_audio = findViewById(R.id.cb_preview_audio);
        cb_original = findViewById(R.id.cb_original);
        cb_single_back = findViewById(R.id.cb_single_back);
        cb_custom_camera = findViewById(R.id.cb_custom_camera);
        cb_hide = findViewById(R.id.cb_hide);
        cb_not_gif = findViewById(R.id.cb_not_gif);
        cb_skip_not_gif = findViewById(R.id.cb_skip_not_gif);
        cb_crop_circular = findViewById(R.id.cb_crop_circular);
        cb_crop_use_bitmap = findViewById(R.id.cb_crop_use_bitmap);
        rgb_crop.setOnCheckedChangeListener(this);
        rgb_result.setOnCheckedChangeListener(this);
        rgb_style.setOnCheckedChangeListener(this);
        rgb_animation.setOnCheckedChangeListener(this);
        rgb_list_anim.setOnCheckedChangeListener(this);
        rgb_photo_mode.setOnCheckedChangeListener(this);
        rgb_language.setOnCheckedChangeListener(this);
        RecyclerView mRecyclerView = findViewById(R.id.recycler);
        ImageView left_back = findViewById(R.id.left_back);
        left_back.setOnClickListener(this);
        minus.setOnClickListener(this);
        plus.setOnClickListener(this);
        cb_crop.setOnCheckedChangeListener(this);
        cb_only_dir.setOnCheckedChangeListener(this);
        cb_custom_sandbox.setOnCheckedChangeListener(this);
        cb_crop_circular.setOnCheckedChangeListener(this);
        cb_crop_use_bitmap.setOnCheckedChangeListener(this);
        cb_compress.setOnCheckedChangeListener(this);
        cb_not_gif.setOnCheckedChangeListener(this);
        cb_skip_not_gif.setOnCheckedChangeListener(this);
        tv_select_num.setText(ValueOf.toString(maxSelectNum));

        // 注册需要写在onCreate或Fragment onAttach里，否则会报java.lang.IllegalStateException异常
        launcherResult = createActivityResultLauncher();

//        List<LocalMedia> list = new ArrayList<>();
//        list.add(LocalMedia.generateLocalMedia("https://wx1.sinaimg.cn/mw690/006e0i7xly1gaxqq5m7t8j31311g2ao6.jpg", PictureMimeType.ofJPEG()));
//        list.add(LocalMedia.generateLocalMedia("https://ww1.sinaimg.cn/bmiddle/bcd10523ly1g96mg4sfhag20c806wu0x.gif", PictureMimeType.ofGIF()));
//        mData.addAll(list);

        FullyGridLayoutManager manager = new FullyGridLayoutManager(this,
                4, GridLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(manager);
        RecyclerView.ItemAnimator itemAnimator = mRecyclerView.getItemAnimator();
        if (itemAnimator != null) {
            ((SimpleItemAnimator) itemAnimator).setSupportsChangeAnimations(false);
        }
        mRecyclerView.addItemDecoration(new GridSpacingItemDecoration(4,
                DensityUtil.dip2px(this, 8), false));
        mAdapter = new GridImageAdapter(getContext(), mData);
        mAdapter.setSelectMax(maxSelectNum);
        mRecyclerView.setAdapter(mAdapter);
        if (savedInstanceState != null && savedInstanceState.getParcelableArrayList("selectorList") != null) {
            mData.clear();
            mData.addAll(savedInstanceState.getParcelableArrayList("selectorList"));
        }
        String systemHigh = " (仅支持部分api)";
        String systemTips = "使用系统图库" + systemHigh;
        int startIndex = systemTips.indexOf(systemHigh);
        int endOf = startIndex + systemHigh.length();
        SpannableStringBuilder builder = new SpannableStringBuilder(systemTips);
        builder.setSpan(new AbsoluteSizeSpan(DensityUtil.dip2px(getContext(), 12)), startIndex, endOf, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        builder.setSpan(new ForegroundColorSpan(0xFFCC0000), startIndex, endOf, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        cb_system_album.setText(builder);
        cb_original.setOnCheckedChangeListener((buttonView, isChecked) ->
                tv_original_tips.setVisibility(isChecked ? View.VISIBLE : View.GONE));
        cb_choose_mode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            cb_single_back.setVisibility(isChecked ? View.GONE : View.VISIBLE);
            cb_single_back.setChecked(!isChecked && cb_single_back.isChecked());
        });
        mAdapter.setOnItemClickListener(new GridImageAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                // 预览图片、视频、音频
                PictureSelector.create(MainActivity.this)
                        .openPreview()
                        .setImageEngine(GlideEngine.createGlideEngine())
                        .setSelectorUIStyle(selectorStyle)
                        .setLanguage(language)
                        .isPreviewFullScreenMode(cb_preview_full.isChecked())
                        .setExternalPreviewEventListener(new OnExternalPreviewEventListener() {
                            @Override
                            public void onPreviewDelete(int position) {
                                mAdapter.remove(position);
                                mAdapter.notifyItemRemoved(position);
                            }

                            @Override
                            public boolean onLongPressDownload(LocalMedia media) {
                                return false;
                            }
                        })
                        .startActivityPreview(position, true, mAdapter.getData());
            }

            @Override
            public void openPicture() {
                boolean mode = cb_mode.isChecked();
                if (mode) {
                    // 进入系统相册
                    if (cb_system_album.isChecked()) {
                        PictureSelectionSystemModel systemGalleryMode = PictureSelector.create(getContext())
                                .openSystemGallery(chooseMode)
                                .setSelectionMode(cb_choose_mode.isChecked() ? SelectModeConfig.MULTIPLE : SelectModeConfig.SINGLE)
                                .setCompressEngine(getCompressEngine())
                                .setCropEngine(getCropEngine())
                                .setSkipCropMimeType(getNotSupportCrop())
                                .isOriginalControl(cb_original.isChecked())
                                .setSandboxFileEngine(new MeSandboxFileEngine());
                        forSystemResult(systemGalleryMode);
                    } else {
                        // 进入相册
                        PictureSelectionModel selectionModel = PictureSelector.create(getContext())
                                .openGallery(chooseMode)
                                .setSelectorUIStyle(selectorStyle)
                                .setImageEngine(GlideEngine.createGlideEngine())
                                .setCropEngine(getCropEngine())
                                .setCompressEngine(getCompressEngine())
                                .setSandboxFileEngine(new MeSandboxFileEngine())
                                .setCameraInterceptListener(getCustomCameraEvent())
                                .setSelectLimitTipsListener(new MeOnSelectLimitTipsListener())
                                .setEditMediaInterceptListener(getCustomEditMediaEvent())
                                //.setExtendLoaderEngine(getExtendLoaderEngine())
                                .setInjectLayoutResourceListener(getInjectLayoutResource())
                                .setSelectionMode(cb_choose_mode.isChecked() ? SelectModeConfig.MULTIPLE : SelectModeConfig.SINGLE)
                                .setLanguage(language)
                                .setOutputCameraDir(chooseMode == SelectMimeType.ofAudio()
                                        ? getSandboxAudioOutputPath() : getSandboxCameraOutputPath())
                                .setOutputAudioDir(chooseMode == SelectMimeType.ofAudio()
                                        ? getSandboxAudioOutputPath() : getSandboxCameraOutputPath())
                                .setQuerySandboxDir(chooseMode == SelectMimeType.ofAudio()
                                        ? getSandboxAudioOutputPath() : getSandboxCameraOutputPath())
                                .isDisplayTimeAxis(cb_time_axis.isChecked())
                                .isOnlyObtainSandboxDir(cb_only_dir.isChecked())
                                .isPageStrategy(cbPage.isChecked())
                                .isOriginalControl(cb_original.isChecked())
                                .isDisplayCamera(cb_isCamera.isChecked())
                                .isOpenClickSound(cb_voice.isChecked())
                                .setSkipCropMimeType(getNotSupportCrop())
                                .isFastSlidingSelect(cb_fast_select.isChecked())
                                //.setOutputCameraImageFileName("luck.jpeg")
                                //.setOutputCameraVideoFileName("luck.mp4")
                                .isWithSelectVideoImage(cb_WithImageVideo.isChecked())
                                .isPreviewFullScreenMode(cb_preview_full.isChecked())
                                .isPreviewZoomEffect(cb_preview_scale.isChecked())
                                .isPreviewImage(cb_preview_img.isChecked())
                                .isPreviewVideo(cb_preview_video.isChecked())
                                .isPreviewAudio(cb_preview_audio.isChecked())
                                //.setQueryOnlyMimeType(PictureMimeType.ofGIF())
                                .isMaxSelectEnabledMask(cbEnabledMask.isChecked())
                                .isDirectReturnSingle(cb_single_back.isChecked())
                                .setMaxSelectNum(maxSelectNum)
                                .setRecyclerAnimationMode(animationMode)
                                .isGif(cb_isGif.isChecked())
                                .setSelectedData(mAdapter.getData());
                        forSelectResult(selectionModel);
                    }
                } else {
                    // 单独拍照
                    PictureSelectionCameraModel cameraModel = PictureSelector.create(MainActivity.this)
                            .openCamera(chooseMode)
                            .setCameraInterceptListener(getCustomCameraEvent())
                            .setCropEngine(getCropEngine())
                            .setCompressEngine(getCompressEngine())
                            .setSandboxFileEngine(new MeSandboxFileEngine())
                            .isOriginalControl(cb_original.isChecked())
                            .setOutputAudioDir(getSandboxAudioOutputPath())
                            .setSelectedData(mAdapter.getData());
                    forOnlyCameraResult(cameraModel);
                }
            }
        });

        mAdapter.setItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public void onItemLongClick(RecyclerView.ViewHolder holder, int position, View v) {
                int itemViewType = holder.getItemViewType();
                if (itemViewType != GridImageAdapter.TYPE_CAMERA) {
                    mItemTouchHelper.startDrag(holder);
                }
            }
        });
        // 绑定拖拽事件
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);
        // 清除缓存
//        clearCache();
    }

    private String[] getNotSupportCrop() {
        if (cb_skip_not_gif.isChecked()) {
            return new String[]{PictureMimeType.ofGIF(), PictureMimeType.ofWEBP()};
        }
        return null;
    }

    private final ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
        @Override
        public boolean isLongPressDragEnabled() {
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        }

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            int itemViewType = viewHolder.getItemViewType();
            if (itemViewType != GridImageAdapter.TYPE_CAMERA) {
                viewHolder.itemView.setAlpha(0.7f);
            }
            return makeMovementFlags(ItemTouchHelper.DOWN | ItemTouchHelper.UP
                    | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, 0);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            try {
                //得到item原来的position
                int fromPosition = viewHolder.getAbsoluteAdapterPosition();
                //得到目标position
                int toPosition = target.getAbsoluteAdapterPosition();
                int itemViewType = target.getItemViewType();
                if (itemViewType != GridImageAdapter.TYPE_CAMERA) {
                    if (fromPosition < toPosition) {
                        for (int i = fromPosition; i < toPosition; i++) {
                            Collections.swap(mAdapter.getData(), i, i + 1);
                        }
                    } else {
                        for (int i = fromPosition; i > toPosition; i--) {
                            Collections.swap(mAdapter.getData(), i, i - 1);
                        }
                    }
                    mAdapter.notifyItemMoved(fromPosition, toPosition);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                @NonNull RecyclerView.ViewHolder viewHolder, float dx, float dy, int actionState, boolean isCurrentlyActive) {
            int itemViewType = viewHolder.getItemViewType();
            if (itemViewType != GridImageAdapter.TYPE_CAMERA) {
                if (needScaleBig) {
                    needScaleBig = false;
                    AnimatorSet animatorSet = new AnimatorSet();
                    animatorSet.playTogether(
                            ObjectAnimator.ofFloat(viewHolder.itemView, "scaleX", 1.0F, 1.1F),
                            ObjectAnimator.ofFloat(viewHolder.itemView, "scaleY", 1.0F, 1.1F));
                    animatorSet.setDuration(50);
                    animatorSet.setInterpolator(new LinearInterpolator());
                    animatorSet.start();
                    animatorSet.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            needScaleSmall = true;
                        }
                    });
                }
                int targetDy = tvDeleteText.getTop() - viewHolder.itemView.getBottom();
                if (dy >= targetDy) {
                    //拖到删除处
                    mDragListener.deleteState(true);
                    if (isHasLiftDelete) {
                        //在删除处放手，则删除item
                        viewHolder.itemView.setVisibility(View.INVISIBLE);
                        mAdapter.delete(viewHolder.getAbsoluteAdapterPosition());
                        resetState();
                        return;
                    }
                } else {
                    //没有到删除处
                    if (View.INVISIBLE == viewHolder.itemView.getVisibility()) {
                        //如果viewHolder不可见，则表示用户放手，重置删除区域状态
                        mDragListener.dragState(false);
                    }
                    mDragListener.deleteState(false);
                }
                super.onChildDraw(c, recyclerView, viewHolder, dx, dy, actionState, isCurrentlyActive);
            }
        }

        @Override
        public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
            int itemViewType = viewHolder != null ? viewHolder.getItemViewType() : GridImageAdapter.TYPE_CAMERA;
            if (itemViewType != GridImageAdapter.TYPE_CAMERA) {
                if (ItemTouchHelper.ACTION_STATE_DRAG == actionState) {
                    mDragListener.dragState(true);
                }
                super.onSelectedChanged(viewHolder, actionState);
            }
        }

        @Override
        public long getAnimationDuration(@NonNull RecyclerView recyclerView, int animationType, float animateDx, float animateDy) {
            isHasLiftDelete = true;
            return super.getAnimationDuration(recyclerView, animationType, animateDx, animateDy);
        }

        @Override
        public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            int itemViewType = viewHolder.getItemViewType();
            if (itemViewType != GridImageAdapter.TYPE_CAMERA) {
                viewHolder.itemView.setAlpha(1.0F);
                if (needScaleSmall) {
                    needScaleSmall = false;
                    AnimatorSet animatorSet = new AnimatorSet();
                    animatorSet.playTogether(
                            ObjectAnimator.ofFloat(viewHolder.itemView, "scaleX", 1.1F, 1.0F),
                            ObjectAnimator.ofFloat(viewHolder.itemView, "scaleY", 1.1F, 1.0F));
                    animatorSet.setInterpolator(new LinearInterpolator());
                    animatorSet.setDuration(50);
                    animatorSet.start();
                    animatorSet.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            needScaleBig = true;
                        }
                    });
                }
                super.clearView(recyclerView, viewHolder);
                mAdapter.notifyItemChanged(viewHolder.getAbsoluteAdapterPosition());
                resetState();
            }
        }
    });

    private final DragListener mDragListener = new DragListener() {
        @Override
        public void deleteState(boolean isDelete) {
            if (isDelete) {
                if (!TextUtils.equals(getString(R.string.app_let_go_drag_delete), tvDeleteText.getText())) {
                    tvDeleteText.setText(getString(R.string.app_let_go_drag_delete));
                    tvDeleteText.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_dump_delete, 0, 0);
                }
            } else {
                if (!TextUtils.equals(getString(R.string.app_drag_delete), tvDeleteText.getText())) {
                    tvDeleteText.setText(getString(R.string.app_drag_delete));
                    tvDeleteText.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_normal_delete, 0, 0);
                }
            }

        }

        @Override
        public void dragState(boolean isStart) {
            if (isStart) {
                if (tvDeleteText.getAlpha() == 0F) {
                    ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(tvDeleteText, "alpha", 0F, 1F);
                    alphaAnimator.setInterpolator(new LinearInterpolator());
                    alphaAnimator.setDuration(120);
                    alphaAnimator.start();
                }
            } else {
                if (tvDeleteText.getAlpha() == 1F) {
                    ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(tvDeleteText, "alpha", 1F, 0F);
                    alphaAnimator.setInterpolator(new LinearInterpolator());
                    alphaAnimator.setDuration(120);
                    alphaAnimator.start();
                }
            }
        }
    };

    private void forSystemResult(PictureSelectionSystemModel model) {
        if (resultMode == CALLBACK_RESULT) {
            model.forSystemResult(new MeOnResultCallbackListener());
        } else {
            model.forSystemResult();
        }
    }

    private void forSelectResult(PictureSelectionModel model) {
        switch (resultMode) {
            case ACTIVITY_RESULT:
                model.forResult(PictureConfig.CHOOSE_REQUEST);
                break;
            case CALLBACK_RESULT:
                model.forResult(new MeOnResultCallbackListener());
                break;
            default:
                model.forResult(launcherResult);
                break;
        }
    }

    private void forOnlyCameraResult(PictureSelectionCameraModel model) {
        if (resultMode == CALLBACK_RESULT) {
            model.forResult(new MeOnResultCallbackListener());
        } else {
            model.forResult();
        }
    }

    /**
     * 重置
     */
    private void resetState() {
        isHasLiftDelete = false;
        mDragListener.deleteState(false);
        mDragListener.dragState(false);
    }

    /**
     * 选择结果
     */
    private class MeOnResultCallbackListener implements OnResultCallbackListener<LocalMedia> {
        @Override
        public void onResult(ArrayList<LocalMedia> result) {
            analyticalSelectResults(result);
        }

        @Override
        public void onCancel() {
            Log.i(TAG, "PictureSelector Cancel");
        }
    }

    /**
     * 压缩引擎
     *
     * @return
     */
    private ImageCompressEngine getCompressEngine() {
        return cb_compress.isChecked() ? new ImageCompressEngine() : null;
    }

    /**
     * 裁剪引擎
     *
     * @return
     */
    private ImageCropEngine getCropEngine() {
        return cb_crop.isChecked() ? new ImageCropEngine() : null;
    }

    /**
     * 自定义相机事件
     *
     * @return
     */
    private OnCameraInterceptListener getCustomCameraEvent() {
        return cb_custom_camera.isChecked() ? new MeOnCameraInterceptListener() : null;
    }

    /**
     * 自定义数据加载器
     *
     * @return
     */
    private ExtendLoaderEngine getExtendLoaderEngine() {
        return new MeExtendLoaderEngine();
    }

    /**
     * 自定义编辑事件
     *
     * @return
     */
    private OnMediaEditInterceptListener getCustomEditMediaEvent() {
        return cbEditor.isChecked() ? new MeOnMediaEditInterceptListener() : null;
    }

    /**
     * 注入自定义布局
     *
     * @return
     */
    private OnInjectLayoutResourceListener getInjectLayoutResource() {
        return cb_inject_layout.isChecked() ? new MeOnInjectLayoutResourceListener() : null;
    }

    /**
     * 拦截自定义提示
     */
    private static class MeOnSelectLimitTipsListener implements OnSelectLimitTipsListener {

        @Override
        public boolean onSelectLimitTips(Context context, PictureSelectionConfig config, int limitType) {
            if (limitType == SelectLimitType.SELECT_MAX_VIDEO_SELECT_LIMIT) {
                ToastUtils.showToast(context, context.getString(R.string.ps_message_video_max_num, String.valueOf(config.maxVideoSelectNum)));
                return true;
            }
            return false;
        }
    }

    /**
     * 注入自定义布局UI，前提是布局View id 和 根目录Layout必须一致
     */
    private static class MeOnInjectLayoutResourceListener implements OnInjectLayoutResourceListener {

        @Override
        public int getLayoutResourceId(Context context, int resourceSource) {
            switch (resourceSource) {
                case InjectResourceSource.MAIN_SELECTOR_LAYOUT_RESOURCE:
                    return R.layout.ps_custom_fragment_selector;
                case InjectResourceSource.PREVIEW_LAYOUT_RESOURCE:
                    return R.layout.ps_custom_fragment_preview;
                case InjectResourceSource.MAIN_ITEM_IMAGE_LAYOUT_RESOURCE:
                    return R.layout.ps_custom_item_grid_image;
                case InjectResourceSource.MAIN_ITEM_VIDEO_LAYOUT_RESOURCE:
                    return R.layout.ps_custom_item_grid_video;
                case InjectResourceSource.MAIN_ITEM_AUDIO_LAYOUT_RESOURCE:
                    return R.layout.ps_custom_item_grid_audio;
                case InjectResourceSource.ALBUM_ITEM_LAYOUT_RESOURCE:
                    return R.layout.ps_custom_album_folder_item;
                case InjectResourceSource.PREVIEW_ITEM_IMAGE_LAYOUT_RESOURCE:
                    return R.layout.ps_custom_preview_image;
                case InjectResourceSource.PREVIEW_ITEM_VIDEO_LAYOUT_RESOURCE:
                    return R.layout.ps_custom_preview_video;
                case InjectResourceSource.PREVIEW_GALLERY_ITEM_LAYOUT_RESOURCE:
                    return R.layout.ps_custom_preview_gallery_item;
                default:
                    return 0;
            }
        }
    }

    /**
     * 自定义数据加载器
     */
    private class MeExtendLoaderEngine implements ExtendLoaderEngine {

        @Override
        public void loadAllAlbumData(Context context,
                                     OnQueryAllAlbumListener<LocalMediaFolder> query) {
            LocalMediaFolder folder = SandboxFileLoader
                    .loadInAppSandboxFolderFile(context, getSandboxPath());
            List<LocalMediaFolder> folders = new ArrayList<>();
            folders.add(folder);
            query.onComplete(folders);
        }

        @Override
        public void loadOnlyInAppDirAllMediaData(Context context,
                                                 OnQueryAlbumListener<LocalMediaFolder> query) {
            LocalMediaFolder folder = SandboxFileLoader
                    .loadInAppSandboxFolderFile(context, getSandboxPath());
            query.onComplete(folder);
        }

        @Override
        public void loadFirstPageMediaData(Context context, long bucketId, int page, int pageSize, OnQueryDataResultListener<LocalMedia> query) {
            LocalMediaFolder folder = SandboxFileLoader
                    .loadInAppSandboxFolderFile(context, getSandboxPath());
            query.onComplete(folder.getData(), false);
        }

        @Override
        public void loadMoreMediaData(Context context, long bucketId, int page, int limit, int pageSize, OnQueryDataResultListener<LocalMedia> query) {

        }
    }

    /**
     * 自定义编辑
     */
    private class MeOnMediaEditInterceptListener implements OnMediaEditInterceptListener {

        @Override
        public void onStartMediaEdit(Fragment fragment, LocalMedia currentLocalMedia, int requestCode) {
            String currentEditPath = currentLocalMedia.getAvailablePath();
            Uri inputUri = PictureMimeType.isContent(currentEditPath)
                    ? Uri.parse(currentEditPath) : Uri.fromFile(new File(currentEditPath));
            Uri destinationUri = Uri.fromFile(
                    new File(getSandboxPath(), DateUtils.getCreateFileName("CROP_") + ".jpeg"));
            UCrop uCrop = UCrop.of(inputUri, destinationUri);
            UCrop.Options options = buildOptions();
            options.setHideBottomControls(false);
            uCrop.withOptions(options);
            uCrop.startEdit(fragment.getActivity(), fragment, requestCode);
        }
    }

    /**
     * 自定义拍照
     */
    private class MeOnCameraInterceptListener implements OnCameraInterceptListener {

        @Override
        public void openCamera(Fragment fragment, int cameraMode, int requestCode) {
            if (cameraMode == SelectMimeType.ofAudio()) {
                ToastUtils.showToast(fragment.getContext(), "自定义录音功能，请自行扩展");
            } else {
                SimpleCameraX camera = SimpleCameraX.of();
                camera.setCameraMode(cameraMode);
                camera.setVideoFrameRate(25);
                camera.setVideoBitRate(3 * 1024 * 1024);
                camera.isDisplayRecordChangeTime(true);
                camera.setOutputPathDir(getSandboxCameraOutputPath());
                camera.setImageEngine(new CameraImageEngine() {
                    @Override
                    public void loadImage(Context context, String url, ImageView imageView) {
                        Glide.with(context).load(url).into(imageView);
                    }
                });
                camera.start(fragment.getActivity(), fragment, requestCode);
            }
        }
    }

    /**
     * 自定义沙盒文件处理
     */
    private static class MeSandboxFileEngine implements SandboxFileEngine {

        @Override
        public void onStartSandboxFileTransform(Context context, boolean isOriginalImage,
                                                int index, LocalMedia media,
                                                OnCallbackIndexListener<LocalMedia> listener) {
            if (PictureMimeType.isContent(media.getAvailablePath())) {
                String sandboxPath = SandboxTransformUtils.copyPathToSandbox(context, media.getPath(),
                        media.getMimeType());
                media.setSandboxPath(sandboxPath);
            }
            if (isOriginalImage) {
                String originalPath = SandboxTransformUtils.copyPathToSandbox(context, media.getPath(),
                        media.getMimeType());
                media.setOriginalPath(originalPath);
                media.setOriginal(!TextUtils.isEmpty(originalPath));
            }
            listener.onCall(media, index);
        }
    }

    /**
     * 自定义裁剪
     */
    private class ImageCropEngine implements CropEngine {

        @Override
        public void onStartCrop(Fragment fragment, LocalMedia currentLocalMedia,
                                ArrayList<LocalMedia> dataSource, int requestCode) {
            String currentCropPath = currentLocalMedia.getAvailablePath();
            Uri inputUri;
            if (PictureMimeType.isContent(currentCropPath) || PictureMimeType.isHasHttp(currentCropPath)) {
                inputUri = Uri.parse(currentCropPath);
            } else {
                inputUri = Uri.fromFile(new File(currentCropPath));
            }
            String fileName = DateUtils.getCreateFileName("CROP_") + ".jpg";
            Uri destinationUri = Uri.fromFile(new File(getSandboxPath(), fileName));
            UCrop.Options options = buildOptions();
            ArrayList<String> dataCropSource = new ArrayList<>();
            for (int i = 0; i < dataSource.size(); i++) {
                LocalMedia media = dataSource.get(i);
                dataCropSource.add(media.getAvailablePath());
            }
            UCrop uCrop = UCrop.of(inputUri, destinationUri, dataCropSource);
            //options.setMultipleCropAspectRatio(buildAspectRatios(dataSource.size()));
            uCrop.withOptions(options);
            uCrop.setImageEngine(new UCropImageEngine() {
                @Override
                public void loadImage(Context context, String url, ImageView imageView) {
                    if (!ImageLoaderUtils.assertValidRequest(context)) {
                        return;
                    }
                    Glide.with(context).load(url).override(180, 180).into(imageView);
                }

                @Override
                public void loadImage(Context context, Uri url, int maxWidth, int maxHeight, OnCallbackListener<Bitmap> call) {
                    if (!ImageLoaderUtils.assertValidRequest(context)) {
                        return;
                    }
                    Glide.with(context).asBitmap().override(maxWidth, maxHeight).load(url).into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            if (call != null) {
                                call.onCall(resource);
                            }
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            if (call != null) {
                                call.onCall(null);
                            }
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                        }
                    });
                }
            });
            uCrop.start(fragment.getActivity(), fragment, requestCode);
        }
    }

    /**
     * 多图裁剪时每张对应的裁剪比例
     *
     * @param dataSourceCount
     * @return
     */
    private AspectRatio[] buildAspectRatios(int dataSourceCount) {
        AspectRatio[] aspectRatios = new AspectRatio[dataSourceCount];
        for (int i = 0; i < dataSourceCount; i++) {
            if (i == 0) {
                aspectRatios[i] = new AspectRatio("16:9", 16, 9);
            } else if (i == 1) {
                aspectRatios[i] = new AspectRatio("3:2", 3, 2);
            } else {
                aspectRatios[i] = new AspectRatio("原始比例", 0, 0);
            }
        }
        return aspectRatios;
    }

    /**
     * 配制UCrop，可根据需求自我扩展
     *
     * @return
     */
    private UCrop.Options buildOptions() {
        UCrop.Options options = new UCrop.Options();
        options.setHideBottomControls(!cb_hide.isChecked());
        options.setFreeStyleCropEnabled(cb_styleCrop.isChecked());
        options.setShowCropFrame(cb_showCropFrame.isChecked());
        options.setShowCropGrid(cb_showCropGrid.isChecked());
        options.setCircleDimmedLayer(cb_crop_circular.isChecked());
        options.withAspectRatio(aspect_ratio_x, aspect_ratio_y);
        options.setCropOutputPathDir(getSandboxPath());
        options.isCropDragSmoothToCenter(false);
        options.isUseCustomLoaderBitmap(cb_crop_use_bitmap.isChecked());
        options.setSkipCropMimeType(getNotSupportCrop());
        options.isForbidCropGifWebp(cb_not_gif.isChecked());
        options.isForbidSkipMultipleCrop(false);
        if (selectorStyle != null && selectorStyle.getSelectMainStyle().getStatusBarColor() != 0) {
            SelectMainStyle mainStyle = selectorStyle.getSelectMainStyle();
            boolean isDarkStatusBarBlack = mainStyle.isDarkStatusBarBlack();
            int statusBarColor = mainStyle.getStatusBarColor();
            options.isDarkStatusBarBlack(isDarkStatusBarBlack);
            if (StyleUtils.checkStyleValidity(statusBarColor)) {
                options.setStatusBarColor(statusBarColor);
                options.setToolbarColor(statusBarColor);
            } else {
                options.setStatusBarColor(ContextCompat.getColor(getContext(), R.color.ps_color_grey));
                options.setToolbarColor(ContextCompat.getColor(getContext(), R.color.ps_color_grey));
            }
            TitleBarStyle titleBarStyle = selectorStyle.getTitleBarStyle();
            if (StyleUtils.checkStyleValidity(titleBarStyle.getTitleTextColor())) {
                options.setToolbarWidgetColor(titleBarStyle.getTitleTextColor());
            } else {
                options.setToolbarWidgetColor(ContextCompat.getColor(getContext(), R.color.ps_color_white));
            }
        } else {
            options.setStatusBarColor(ContextCompat.getColor(getContext(), R.color.ps_color_grey));
            options.setToolbarColor(ContextCompat.getColor(getContext(), R.color.ps_color_grey));
            options.setToolbarWidgetColor(ContextCompat.getColor(getContext(), R.color.ps_color_white));
        }
        return options;
    }

    /**
     * 自定义压缩
     */
    private static class ImageCompressEngine implements CompressEngine {

        @Override
        public void onStartCompress(Context context, ArrayList<LocalMedia> list,
                                    OnCallbackListener<ArrayList<LocalMedia>> listener) {
            // 自定义压缩
            List<Uri> compress = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                LocalMedia media = list.get(i);
                String availablePath = media.getAvailablePath();
                Uri uri = PictureMimeType.isContent(availablePath) || PictureMimeType.isHasHttp(availablePath)
                        ? Uri.parse(availablePath)
                        : Uri.fromFile(new File(availablePath));
                compress.add(uri);
            }
            if (compress.size() == 0) {
                listener.onCall(list);
                return;
            }
            Luban.with(context)
                    .load(compress)
                    .ignoreBy(100)
                    .filter(new CompressionPredicate() {
                        @Override
                        public boolean apply(String path) {
                            return PictureMimeType.isUrlHasImage(path) && !PictureMimeType.isHasHttp(path);

                        }
                    })
                    .setRenameListener(new OnRenameListener() {
                        @Override
                        public String rename(String filePath) {
                            int indexOf = filePath.lastIndexOf(".");
                            String postfix = indexOf != -1 ? filePath.substring(indexOf) : ".jpg";
                            return DateUtils.getCreateFileName("CMP_") + postfix;
                        }
                    })
                    .setCompressListener(new OnCompressListener() {
                        @Override
                        public void onStart() {
                        }

                        @Override
                        public void onSuccess(int index, File compressFile) {
                            LocalMedia media = list.get(index);
                            if (compressFile.exists() && !TextUtils.isEmpty(compressFile.getAbsolutePath())) {
                                media.setCompressed(true);
                                media.setCompressPath(compressFile.getAbsolutePath());
                                media.setSandboxPath(SdkVersionUtils.isQ() ? media.getCompressPath() : null);
                            }
                            if (index == list.size() - 1) {
                                listener.onCall(list);
                            }
                        }

                        @Override
                        public void onError(int index, Throwable e) {
                            if (index != -1) {
                                LocalMedia media = list.get(index);
                                media.setCompressed(false);
                                media.setCompressPath(null);
                                media.setSandboxPath(null);
                                if (index == list.size() - 1) {
                                    listener.onCall(list);
                                }
                            }
                        }
                    }).launch();
        }
    }

    /**
     * 创建相机自定义输出目录
     *
     * @return
     */
    private String getSandboxCameraOutputPath() {
        if (cb_custom_sandbox.isChecked()) {
            File externalFilesDir = getContext().getExternalFilesDir("");
            File customFile = new File(externalFilesDir.getAbsolutePath(), "Sandbox");
            if (!customFile.exists()) {
                customFile.mkdirs();
            }
            return customFile.getAbsolutePath() + File.separator;
        } else {
            return "";
        }
    }

    /**
     * 创建音频自定义输出目录
     *
     * @return
     */
    private String getSandboxAudioOutputPath() {
        if (cb_custom_sandbox.isChecked()) {
            File externalFilesDir = getContext().getExternalFilesDir("");
            File customFile = new File(externalFilesDir.getAbsolutePath(), "Sound");
            if (!customFile.exists()) {
                customFile.mkdirs();
            }
            return customFile.getAbsolutePath() + File.separator;
        } else {
            return "";
        }
    }

    /**
     * 创建自定义输出目录
     *
     * @return
     */
    private String getSandboxPath() {
        File externalFilesDir = getContext().getExternalFilesDir("");
        File customFile = new File(externalFilesDir.getAbsolutePath(), "Sandbox");
        if (!customFile.exists()) {
            customFile.mkdirs();
        }
        return customFile.getAbsolutePath() + File.separator;
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.left_back:
                finish();
                break;
            case R.id.minus:
                if (maxSelectNum > 1) {
                    maxSelectNum--;
                }
                tv_select_num.setText(maxSelectNum + "");
                mAdapter.setSelectMax(maxSelectNum);
                break;
            case R.id.plus:
                maxSelectNum++;
                tv_select_num.setText(maxSelectNum + "");
                mAdapter.setSelectMax(maxSelectNum);
                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
        switch (checkedId) {
            case R.id.rb_all:
                chooseMode = SelectMimeType.ofAll();
                cb_preview_img.setChecked(true);
                cb_preview_video.setChecked(true);
                cb_isGif.setChecked(false);
                cb_preview_video.setChecked(true);
                cb_preview_img.setChecked(true);
                cb_preview_video.setVisibility(View.VISIBLE);
                cb_preview_img.setVisibility(View.VISIBLE);
                cb_compress.setVisibility(View.VISIBLE);
                cb_crop.setVisibility(View.VISIBLE);
                cb_isGif.setVisibility(View.VISIBLE);
                cb_preview_audio.setVisibility(View.GONE);
                break;
            case R.id.rb_image:
                chooseMode = SelectMimeType.ofImage();
                cb_preview_img.setChecked(true);
                cb_preview_video.setChecked(false);
                cb_isGif.setChecked(false);
                cb_preview_video.setChecked(false);
                cb_preview_video.setVisibility(View.GONE);
                cb_preview_img.setChecked(true);
                cb_preview_audio.setVisibility(View.GONE);
                cb_preview_img.setVisibility(View.VISIBLE);
                cb_compress.setVisibility(View.VISIBLE);
                cb_crop.setVisibility(View.VISIBLE);
                cb_isGif.setVisibility(View.VISIBLE);
                break;
            case R.id.rb_video:
                chooseMode = SelectMimeType.ofVideo();
                cb_preview_img.setChecked(false);
                cb_preview_video.setChecked(true);
                cb_isGif.setChecked(false);
                cb_isGif.setVisibility(View.GONE);
                cb_preview_video.setChecked(true);
                cb_preview_video.setVisibility(View.VISIBLE);
                cb_preview_img.setVisibility(View.GONE);
                cb_preview_img.setChecked(false);
                cb_compress.setVisibility(View.GONE);
                cb_preview_audio.setVisibility(View.GONE);
                cb_crop.setVisibility(View.GONE);
                break;
            case R.id.rb_audio:
                chooseMode = SelectMimeType.ofAudio();
                cb_preview_audio.setVisibility(View.VISIBLE);
                break;
            case R.id.rb_system:
                language = LanguageConfig.SYSTEM_LANGUAGE;
                break;
            case R.id.rb_jpan:
                language = LanguageConfig.JAPAN;
                break;
            case R.id.rb_tw:
                language = LanguageConfig.TRADITIONAL_CHINESE;
                break;
            case R.id.rb_us:
                language = LanguageConfig.ENGLISH;
                break;
            case R.id.rb_ka:
                language = LanguageConfig.KOREA;
                break;
            case R.id.rb_de:
                language = LanguageConfig.GERMANY;
                break;
            case R.id.rb_fr:
                language = LanguageConfig.FRANCE;
                break;
            case R.id.rb_spanish:
                language = LanguageConfig.SPANISH;
                break;
            case R.id.rb_portugal:
                language = LanguageConfig.PORTUGAL;
                break;
            case R.id.rb_ar:
                language = LanguageConfig.AR;
                break;
            case R.id.rb_crop_default:
                aspect_ratio_x = 0;
                aspect_ratio_y = 0;
                break;
            case R.id.rb_crop_1to1:
                aspect_ratio_x = 1;
                aspect_ratio_y = 1;
                break;
            case R.id.rb_crop_3to4:
                aspect_ratio_x = 3;
                aspect_ratio_y = 4;
                break;
            case R.id.rb_crop_3to2:
                aspect_ratio_x = 3;
                aspect_ratio_y = 2;
                break;
            case R.id.rb_crop_16to9:
                aspect_ratio_x = 16;
                aspect_ratio_y = 9;
                break;
            case R.id.rb_launcher_result:
                resultMode = 0;
                break;
            case R.id.rb_activity_result:
                resultMode = 1;
                break;
            case R.id.rb_callback_result:
                resultMode = 2;
                break;
            case R.id.rb_photo_default_animation:
                PictureWindowAnimationStyle defaultAnimationStyle = new PictureWindowAnimationStyle();
                defaultAnimationStyle.setActivityEnterAnimation(R.anim.ps_anim_enter);
                defaultAnimationStyle.setActivityExitAnimation(R.anim.ps_anim_exit);
                selectorStyle.setWindowAnimationStyle(defaultAnimationStyle);
                break;
            case R.id.rb_photo_up_animation:
                PictureWindowAnimationStyle animationStyle = new PictureWindowAnimationStyle();
                animationStyle.setActivityEnterAnimation(R.anim.ps_anim_up_in);
                animationStyle.setActivityExitAnimation(R.anim.ps_anim_down_out);
                selectorStyle.setWindowAnimationStyle(animationStyle);
                break;
            case R.id.rb_default_style:
                selectorStyle = new PictureSelectorStyle();

                break;
            case R.id.rb_white_style:
                TitleBarStyle whiteTitleBarStyle = new TitleBarStyle();
                whiteTitleBarStyle.setTitleBackgroundColor(ContextCompat.getColor(getContext(), R.color.ps_color_white));
                whiteTitleBarStyle.setTitleDrawableRightResource(R.drawable.ic_orange_arrow_down);
                whiteTitleBarStyle.setTitleLeftBackResource(R.drawable.ps_ic_black_back);
                whiteTitleBarStyle.setTitleTextColor(ContextCompat.getColor(getContext(), R.color.ps_color_black));
                whiteTitleBarStyle.setTitleCancelTextColor(ContextCompat.getColor(getContext(), R.color.ps_color_53575e));
                whiteTitleBarStyle.setDisplayTitleBarLine(true);

                BottomNavBarStyle whiteBottomNavBarStyle = new BottomNavBarStyle();
                whiteBottomNavBarStyle.setBottomNarBarBackgroundColor(Color.parseColor("#EEEEEE"));
                whiteBottomNavBarStyle.setBottomPreviewSelectTextColor(ContextCompat.getColor(getContext(), R.color.ps_color_53575e));

                whiteBottomNavBarStyle.setBottomPreviewNormalTextColor(ContextCompat.getColor(getContext(), R.color.ps_color_9b));
                whiteBottomNavBarStyle.setBottomPreviewSelectTextColor(ContextCompat.getColor(getContext(), R.color.ps_color_fa632d));
                whiteBottomNavBarStyle.setCompleteCountTips(false);
                whiteBottomNavBarStyle.setBottomEditorTextColor(ContextCompat.getColor(getContext(), R.color.ps_color_53575e));
                whiteBottomNavBarStyle.setBottomOriginalTextColor(ContextCompat.getColor(getContext(), R.color.ps_color_53575e));

                SelectMainStyle selectMainStyle = new SelectMainStyle();
                selectMainStyle.setStatusBarColor(ContextCompat.getColor(getContext(), R.color.ps_color_white));
                selectMainStyle.setDarkStatusBarBlack(true);
                selectMainStyle.setSelectNormalTextColor(ContextCompat.getColor(getContext(), R.color.ps_color_9b));
                selectMainStyle.setSelectTextColor(ContextCompat.getColor(getContext(), R.color.ps_color_fa632d));
                selectMainStyle.setPreviewSelectBackground(R.drawable.ps_demo_white_preview_selector);
                selectMainStyle.setSelectBackground(R.drawable.ps_checkbox_selector);
                selectMainStyle.setSelectText(getString(R.string.ps_done_front_num));
                selectMainStyle.setMainListBackgroundColor(ContextCompat.getColor(getContext(), R.color.ps_color_white));

                selectorStyle.setTitleBarStyle(whiteTitleBarStyle);
                selectorStyle.setBottomBarStyle(whiteBottomNavBarStyle);
                selectorStyle.setSelectMainStyle(selectMainStyle);
                break;
            case R.id.rb_num_style:
                TitleBarStyle blueTitleBarStyle = new TitleBarStyle();
                blueTitleBarStyle.setTitleBackgroundColor(ContextCompat.getColor(getContext(), R.color.ps_color_blue));

                BottomNavBarStyle numberBlueBottomNavBarStyle = new BottomNavBarStyle();
                numberBlueBottomNavBarStyle.setBottomPreviewNormalTextColor(ContextCompat.getColor(getContext(), R.color.ps_color_9b));
                numberBlueBottomNavBarStyle.setBottomPreviewSelectTextColor(ContextCompat.getColor(getContext(), R.color.ps_color_blue));
                numberBlueBottomNavBarStyle.setBottomNarBarBackgroundColor(ContextCompat.getColor(getContext(), R.color.ps_color_white));
                numberBlueBottomNavBarStyle.setBottomSelectNumResources(R.drawable.ps_demo_blue_num_selected);
                numberBlueBottomNavBarStyle.setBottomEditorTextColor(ContextCompat.getColor(getContext(), R.color.ps_color_53575e));
                numberBlueBottomNavBarStyle.setBottomOriginalTextColor(ContextCompat.getColor(getContext(), R.color.ps_color_53575e));


                SelectMainStyle numberBlueSelectMainStyle = new SelectMainStyle();
                numberBlueSelectMainStyle.setStatusBarColor(ContextCompat.getColor(getContext(), R.color.ps_color_blue));
                numberBlueSelectMainStyle.setSelectNumberStyle(true);
                numberBlueSelectMainStyle.setPreviewSelectNumberStyle(true);
                numberBlueSelectMainStyle.setSelectBackground(R.drawable.ps_demo_blue_num_selector);
                numberBlueSelectMainStyle.setMainListBackgroundColor(ContextCompat.getColor(getContext(), R.color.ps_color_white));
                numberBlueSelectMainStyle.setPreviewSelectBackground(R.drawable.ps_demo_preview_blue_num_selector);

                numberBlueSelectMainStyle.setSelectNormalTextColor(ContextCompat.getColor(getContext(), R.color.ps_color_9b));
                numberBlueSelectMainStyle.setSelectTextColor(ContextCompat.getColor(getContext(), R.color.ps_color_blue));
                numberBlueSelectMainStyle.setSelectText(getString(R.string.ps_completed));

                selectorStyle.setTitleBarStyle(blueTitleBarStyle);
                selectorStyle.setBottomBarStyle(numberBlueBottomNavBarStyle);
                selectorStyle.setSelectMainStyle(numberBlueSelectMainStyle);
                break;
            case R.id.rb_we_chat_style:
                // 主体风格
                SelectMainStyle numberSelectMainStyle = new SelectMainStyle();
                numberSelectMainStyle.setSelectNumberStyle(true);
                numberSelectMainStyle.setPreviewSelectNumberStyle(false);
                numberSelectMainStyle.setPreviewDisplaySelectGallery(true);
                numberSelectMainStyle.setSelectBackground(R.drawable.ps_default_num_selector);
                numberSelectMainStyle.setPreviewSelectBackground(R.drawable.ps_preview_checkbox_selector);
                numberSelectMainStyle.setSelectNormalBackgroundResources(R.drawable.ps_select_complete_normal_bg);
                numberSelectMainStyle.setSelectNormalTextColor(ContextCompat.getColor(getContext(), R.color.ps_color_53575e));
                numberSelectMainStyle.setSelectNormalText(getString(R.string.ps_send));
                numberSelectMainStyle.setAdapterPreviewGalleryBackgroundResource(R.drawable.ps_preview_gallery_bg);
                numberSelectMainStyle.setAdapterPreviewGalleryItemSize(DensityUtil.dip2px(getContext(), 52));
                numberSelectMainStyle.setPreviewSelectText(getString(R.string.ps_select));
                numberSelectMainStyle.setPreviewSelectTextSize(14);
                numberSelectMainStyle.setPreviewSelectTextColor(ContextCompat.getColor(getContext(), R.color.ps_color_white));
                numberSelectMainStyle.setPreviewSelectMarginRight(DensityUtil.dip2px(getContext(), 6));
                numberSelectMainStyle.setSelectBackgroundResources(R.drawable.ps_select_complete_bg);
                numberSelectMainStyle.setSelectText(getString(R.string.ps_send_num));
                numberSelectMainStyle.setSelectTextColor(ContextCompat.getColor(getContext(), R.color.ps_color_white));
                numberSelectMainStyle.setMainListBackgroundColor(ContextCompat.getColor(getContext(), R.color.ps_color_black));
                numberSelectMainStyle.setCompleteSelectRelativeTop(true);
                numberSelectMainStyle.setPreviewSelectRelativeBottom(true);
                numberSelectMainStyle.setAdapterItemIncludeEdge(false);

                // 头部TitleBar 风格
                TitleBarStyle numberTitleBarStyle = new TitleBarStyle();
                numberTitleBarStyle.setHideCancelButton(true);
                numberTitleBarStyle.setAlbumTitleRelativeLeft(true);
                numberTitleBarStyle.setTitleAlbumBackgroundResource(R.drawable.ps_album_bg);
                numberTitleBarStyle.setTitleDrawableRightResource(R.drawable.ps_ic_grey_arrow);
                numberTitleBarStyle.setPreviewTitleLeftBackResource(R.drawable.ps_ic_normal_back);

                // 底部NavBar 风格
                BottomNavBarStyle numberBottomNavBarStyle = new BottomNavBarStyle();
                numberBottomNavBarStyle.setBottomPreviewNarBarBackgroundColor(ContextCompat.getColor(getContext(), R.color.ps_color_half_grey));
                numberBottomNavBarStyle.setBottomPreviewNormalText(getString(R.string.ps_preview));
                numberBottomNavBarStyle.setBottomPreviewNormalTextColor(ContextCompat.getColor(getContext(), R.color.ps_color_9b));
                numberBottomNavBarStyle.setBottomPreviewNormalTextSize(16);
                numberBottomNavBarStyle.setCompleteCountTips(false);
                numberBottomNavBarStyle.setBottomPreviewSelectText(getString(R.string.ps_preview_num));
                numberBottomNavBarStyle.setBottomPreviewSelectTextColor(ContextCompat.getColor(getContext(), R.color.ps_color_white));


                selectorStyle.setTitleBarStyle(numberTitleBarStyle);
                selectorStyle.setBottomBarStyle(numberBottomNavBarStyle);
                selectorStyle.setSelectMainStyle(numberSelectMainStyle);

                break;
            case R.id.rb_default:
                animationMode = AnimationType.DEFAULT_ANIMATION;
                break;
            case R.id.rb_alpha:
                animationMode = AnimationType.ALPHA_IN_ANIMATION;
                break;
            case R.id.rb_slide_in:
                animationMode = AnimationType.SLIDE_IN_BOTTOM_ANIMATION;
                break;
        }
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.cb_crop:
                rgb_crop.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                cb_hide.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                cb_crop_circular.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                cb_crop_use_bitmap.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                cb_styleCrop.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                cb_showCropFrame.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                cb_showCropGrid.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                cb_skip_not_gif.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                cb_not_gif.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                break;
            case R.id.cb_custom_sandbox:
                cb_only_dir.setChecked(isChecked);
                break;
            case R.id.cb_only_dir:
                cb_custom_sandbox.setChecked(isChecked);
                break;
            case R.id.cb_skip_not_gif:
                cb_not_gif.setChecked(false);
                cb_skip_not_gif.setChecked(isChecked);
                break;
            case R.id.cb_not_gif:
                cb_skip_not_gif.setChecked(false);
                cb_not_gif.setChecked(isChecked);
                break;
            case R.id.cb_crop_circular:
                if (isChecked) {
                    x = aspect_ratio_x;
                    y = aspect_ratio_y;
                    aspect_ratio_x = 1;
                    aspect_ratio_y = 1;
                } else {
                    aspect_ratio_x = x;
                    aspect_ratio_y = y;
                }
                rgb_crop.setVisibility(isChecked ? View.GONE : View.VISIBLE);
                if (isChecked) {
                    cb_showCropFrame.setChecked(false);
                    cb_showCropGrid.setChecked(false);
                } else {
                    cb_showCropFrame.setChecked(true);
                    cb_showCropGrid.setChecked(true);
                }
                break;
        }
    }


    @Override
    public void onSelectFinish(@Nullable PictureCommonFragment.SelectorResult result) {
        if (result == null) {
            return;
        }
        if (result.mResultCode == RESULT_OK) {
            ArrayList<LocalMedia> selectorResult = PictureSelector.obtainSelectorList(result.mResultData);
            analyticalSelectResults(selectorResult);
        } else if (result.mResultCode == RESULT_CANCELED) {
            Log.i(TAG, "onSelectFinish PictureSelector Cancel");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == PictureConfig.CHOOSE_REQUEST) {
                ArrayList<LocalMedia> result = PictureSelector.obtainSelectorList(data);
                analyticalSelectResults(result);
            }
        } else if (resultCode == RESULT_CANCELED) {
            Log.i(TAG, "onActivityResult PictureSelector Cancel");
        }
    }

    /**
     * 创建一个ActivityResultLauncher
     *
     * @return
     */
    private ActivityResultLauncher<Intent> createActivityResultLauncher() {
        return registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        int resultCode = result.getResultCode();
                        if (resultCode == RESULT_OK) {
                            ArrayList<LocalMedia> selectList = PictureSelector.obtainSelectorList(result.getData());
                            analyticalSelectResults(selectList);
                        } else if (resultCode == RESULT_CANCELED) {
                            Log.i(TAG, "onActivityResult PictureSelector Cancel");
                        }
                    }
                });
    }


    /**
     * 处理选择结果
     *
     * @param result
     */
    private void analyticalSelectResults(ArrayList<LocalMedia> result) {
        for (LocalMedia media : result) {
            if (media.getWidth() == 0 || media.getHeight() == 0) {
                if (PictureMimeType.isHasImage(media.getMimeType())) {
                    MediaExtraInfo imageExtraInfo = MediaUtils.getImageSize(media.getPath());
                    media.setWidth(imageExtraInfo.getWidth());
                    media.setHeight(imageExtraInfo.getHeight());
                } else if (PictureMimeType.isHasVideo(media.getMimeType())) {
                    MediaExtraInfo videoExtraInfo = MediaUtils.getVideoSize(PictureAppMaster.getInstance().getAppContext(), media.getPath());
                    media.setWidth(videoExtraInfo.getWidth());
                    media.setHeight(videoExtraInfo.getHeight());
                }
            }
            Log.i(TAG, "文件名: " + media.getFileName());
            Log.i(TAG, "是否压缩:" + media.isCompressed());
            Log.i(TAG, "压缩:" + media.getCompressPath());
            Log.i(TAG, "原图:" + media.getPath());
            Log.i(TAG, "绝对路径:" + media.getRealPath());
            Log.i(TAG, "是否裁剪:" + media.isCut());
            Log.i(TAG, "裁剪:" + media.getCutPath());
            Log.i(TAG, "是否开启原图:" + media.isOriginal());
            Log.i(TAG, "原图路径:" + media.getOriginalPath());
            Log.i(TAG, "沙盒路径:" + media.getSandboxPath());
            Log.i(TAG, "原始宽高: " + media.getWidth() + "x" + media.getHeight());
            Log.i(TAG, "裁剪宽高: " + media.getCropImageWidth() + "x" + media.getCropImageHeight());
            Log.i(TAG, "文件大小: " + media.getSize());
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean isMaxSize = result.size() == mAdapter.getSelectMax();
                int oldSize = mAdapter.getData().size();
                mAdapter.notifyItemRangeRemoved(0, isMaxSize ? oldSize + 1 : oldSize);
                mAdapter.getData().clear();

                mAdapter.getData().addAll(result);
                mAdapter.notifyItemRangeInserted(0, result.size());
            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAdapter != null && mAdapter.getData() != null && mAdapter.getData().size() > 0) {
            outState.putParcelableArrayList("selectorList",
                    mAdapter.getData());
        }
    }

    public Context getContext() {
        return this;
    }
}
