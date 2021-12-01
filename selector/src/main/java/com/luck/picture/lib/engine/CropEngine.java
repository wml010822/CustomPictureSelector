package com.luck.picture.lib.engine;


import androidx.fragment.app.Fragment;

import com.luck.picture.lib.config.Crop;
import com.luck.picture.lib.config.CustomField;
import com.luck.picture.lib.entity.LocalMedia;

import java.util.ArrayList;
import java.util.List;

/**
 * @author：luck
 * @date：2021/11/23 8:13 下午
 * @describe：CropEngine
 */
public interface CropEngine {

    /**
     * Custom crop image engine
     * <p>
     * Users can implement this interface, and then access their own crop framework to plug
     * the crop path into the {@link LocalMedia} object;
     * <p>
     * 1、If Activity start crop use context;
     * activity.startActivityForResult({@link Crop.REQUEST_CROP})
     * <p>
     * 2、If Fragment start crop use fragment;
     * fragment.startActivityForResult({@link Crop.REQUEST_CROP})
     * <p>
     * 3、If you implement your own clipping function, you need to assign the following values in
     * Intent.putExtra {@link CustomField}
     *
     * </p>
     *
     * @param fragment          Fragment
     * @param currentLocalMedia current crop data
     * @param dataSource        crop data
     * @param requestCode       Activity result code or fragment result code
     */
    void onStartCrop(Fragment fragment, LocalMedia currentLocalMedia, List<LocalMedia> dataSource, int requestCode);

}