package com.luck.picture.lib.basic;

import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.luck.picture.lib.PictureSelectorFragment;
import com.luck.picture.lib.PictureSelectorPreviewFragment;
import com.luck.picture.lib.R;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureSelectionConfig;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.immersive.ImmersiveManage;
import com.luck.picture.lib.language.LanguageConfig;
import com.luck.picture.lib.language.PictureLanguageUtils;
import com.luck.picture.lib.manager.SelectedManager;
import com.luck.picture.lib.style.PictureWindowAnimationStyle;
import com.luck.picture.lib.style.TitleBarStyle;
import com.luck.picture.lib.utils.ActivityCompatHelper;
import com.luck.picture.lib.utils.SdkVersionUtils;
import com.luck.picture.lib.utils.StyleUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author：luck
 * @date：2021/11/17 9:59 上午
 * @describe：PictureSelectorSupporterActivity
 */
public class PictureSelectorSupporterActivity extends AppCompatActivity implements IBridgePictureBehavior {


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initAppLanguage();
        immersive();
        setContentView(R.layout.ps_activity_container);
        setupFragment();
    }

    private void immersive() {
        TitleBarStyle titleBarStyle = PictureSelectionConfig.selectorStyle.getTitleBarStyle();
        int statusBarColor = titleBarStyle.getStatusBarColor();
        int navigationBarColor = titleBarStyle.getNavigationBarColor();
        boolean isDarkStatusBarBlack = titleBarStyle.isDarkStatusBarBlack();
        if (!StyleUtils.checkStyleValidity(statusBarColor)) {
            statusBarColor = ContextCompat.getColor(this, R.color.ps_color_grey);
        }
        if (!StyleUtils.checkStyleValidity(navigationBarColor)) {
            navigationBarColor = ContextCompat.getColor(this, R.color.ps_color_grey);
        }
        ImmersiveManage.immersiveAboveAPI23(this, statusBarColor, navigationBarColor, isDarkStatusBarBlack);
    }

    private void setupFragment() {
        if (getIntent().hasExtra(PictureConfig.EXTRA_EXTERNAL_PREVIEW)
                && getIntent().getBooleanExtra(PictureConfig.EXTRA_EXTERNAL_PREVIEW, false)) {
            int position = getIntent().getIntExtra(PictureConfig.EXTRA_EXTERNAL_PREVIEW_CURRENT_POSITION, 0);
            PictureSelectorPreviewFragment fragment = PictureSelectorPreviewFragment.newInstance();
            List<LocalMedia> previewResult = SelectedManager.getSelectedPreviewResult();
            List<LocalMedia> previewData = new ArrayList<>(previewResult);
            PictureSelectionConfig.selectorStyle.getSelectMainStyle().setPreviewDisplaySelectGallery(false);
            boolean isDisplayDelete = getIntent()
                    .getBooleanExtra(PictureConfig.EXTRA_EXTERNAL_PREVIEW_DISPLAY_DELETE, false);
            fragment.setExternalPreviewData(position, previewData.size(), previewData, isDisplayDelete);
            SelectedManager.clearExternalPreviewData();
            if (ActivityCompatHelper.checkFragmentNonExits(this, PictureSelectorPreviewFragment.TAG)) {
                injectFragmentFromScreen(PictureSelectorFragment.TAG, fragment);
            }
        } else {
            if (ActivityCompatHelper.checkFragmentNonExits(this, PictureSelectorFragment.TAG)) {
                injectFragmentFromScreen(PictureSelectorFragment.TAG, PictureSelectorFragment.newInstance());
            }
        }
    }

    /**
     * set app language
     */
    public void initAppLanguage() {
        PictureSelectionConfig config = PictureSelectionConfig.getInstance();
        if (config.language != LanguageConfig.UNKNOWN_LANGUAGE && !config.isOnlyCamera) {
            PictureLanguageUtils.setAppLanguage(this, config.language);
        }
    }

    @Override
    public void injectFragmentFromScreen(String tag, Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, fragment, tag)
                .addToBackStack(tag)
                .commitAllowingStateLoss();
    }

    @Override
    public void onFinish() {
        onBackPressed();
    }

    @Override
    public void onImmediateFinish() {
        exit();
    }

    @Override
    public void onBackPressed() {
        if (ActivityCompatHelper.checkRootFragment(this)) {
            exit();
        } else {
            getSupportFragmentManager().popBackStack();
        }
    }

    private void exit() {
        if (SdkVersionUtils.isQ()) {
            finishAfterTransition();
        } else {
            super.onBackPressed();
        }
        finish();
        PictureWindowAnimationStyle windowAnimationStyle = PictureSelectionConfig.selectorStyle.getWindowAnimationStyle();
        overridePendingTransition(0, windowAnimationStyle.activityExitAnimation);
        PictureSelectionConfig.destroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            if (ActivityCompatHelper.checkRootFragment(this)) {
                if (PictureSelectionConfig.resultCallListener != null) {
                    PictureSelectionConfig.resultCallListener.onCancel();
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(PictureContextWrapper.wrap(newBase,
                PictureSelectionConfig.getInstance().language));
    }
}
