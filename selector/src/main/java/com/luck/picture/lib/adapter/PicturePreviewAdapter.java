package com.luck.picture.lib.adapter;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.ui.PlayerView;
import com.luck.picture.lib.R;
import com.luck.picture.lib.adapter.holder.BasePreviewHolder;
import com.luck.picture.lib.adapter.holder.PreviewVideoHolder;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;

import java.util.List;

/**
 * @author：luck
 * @date：2021/11/23 1:11 下午
 * @describe：PicturePreviewAdapter2
 */
public class PicturePreviewAdapter extends RecyclerView.Adapter<BasePreviewHolder> {

    private final List<LocalMedia> mData;
    private final BasePreviewHolder.OnPreviewEventListener onPreviewEventListener;
    private boolean isFirstLoaded = true;

    public PicturePreviewAdapter(List<LocalMedia> list, BasePreviewHolder.OnPreviewEventListener listener) {
        this.mData = list;
        this.onPreviewEventListener = listener;
    }

    @NonNull
    @Override
    public BasePreviewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == BasePreviewHolder.ADAPTER_TYPE_VIDEO) {
            return BasePreviewHolder.generate(parent, viewType, R.layout.ps_preview_video);
        } else {
            return BasePreviewHolder.generate(parent, viewType, R.layout.ps_preview_image);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull BasePreviewHolder holder, int position) {
        holder.setOnPreviewEventListener(onPreviewEventListener);
        LocalMedia media = mData.get(position);
        holder.bindData(media, position);
    }


    @Override
    public int getItemViewType(int position) {
        if (PictureMimeType.isHasVideo(mData.get(position).getMimeType())) {
            return BasePreviewHolder.ADAPTER_TYPE_VIDEO;
        } else {
            return BasePreviewHolder.ADAPTER_TYPE_IMAGE;
        }
    }

    @Override
    public int getItemCount() {
        return mData != null ? mData.size() : 0;
    }

    @Override
    public void onViewAttachedToWindow(@NonNull BasePreviewHolder holder) {
        super.onViewAttachedToWindow(holder);
        if (isFirstLoaded) {
            isFirstLoaded = false;
        } else {
            if (holder instanceof PreviewVideoHolder) {
                PreviewVideoHolder videoHolder = (PreviewVideoHolder) holder;
                if (videoHolder.ivPlayButton.getVisibility() == View.GONE) {
                    videoHolder.ivPlayButton.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull BasePreviewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        if (holder instanceof PreviewVideoHolder) {
            PreviewVideoHolder videoHolder = (PreviewVideoHolder) holder;
            videoHolder.releaseVideo();
        }
    }

    /**
     * 释放当前视频相关
     */
    public void destroyVideo(View itemView) {
        PlayerView playerView = itemView.findViewById(R.id.playerView);
        if (playerView != null) {
            if (playerView.getPlayer() != null) {
                playerView.getPlayer().release();
            }
        }
    }
}
