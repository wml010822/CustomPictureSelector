package com.luck.picture.lib.interfaces;

import androidx.fragment.app.Fragment;

/**
 * @author：luck
 * @date：2021/12/1 8:48 下午
 * @describe：OnPermissionsInterceptListener
 */
public interface OnPermissionsInterceptListener {
    /**
     * Custom Permissions management
     *
     * @param fragment
     * @param permissionArray Permissions array
     * @param call
     */
    void onPermission(Fragment fragment, String[] permissionArray,
                      OnCallbackListener<Boolean> call);
}
