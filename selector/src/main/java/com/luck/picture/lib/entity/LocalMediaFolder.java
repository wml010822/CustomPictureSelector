package com.luck.picture.lib.entity;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.luck.picture.lib.config.PictureConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * @author：luck
 * @date：2016-12-31 15:21
 * @describe：MediaFolder Entity
 */

public class LocalMediaFolder implements Parcelable {
    /**
     * folder bucketId
     */
    private long bucketId = PictureConfig.ALL;
    /**
     * folder name
     */
    private String folderName;
    /**
     * folder first path
     */
    private String firstImagePath;

    /**
     * first data mime type
     */
    private String firstMimeType;

    /**
     * folder total media num
     */
    private int folderTotalNum;

    /**
     * There are selected resources in the current directory
     */
    private boolean isSelectTag;

    /**
     * type
     */
    private int ofAllType = -1;

    /**
     * current folder data
     * <p>
     * In isPageStrategy mode, there is no data for the first time
     * </p>
     */
    private List<LocalMedia> data = new ArrayList<>();

    /**
     * # Internal use
     * setCurrentDataPage
     */
    private int currentDataPage = 1;

    /**
     * # Internal use
     * is load more
     */
    private boolean isHasMore;


    public LocalMediaFolder() {
    }

    protected LocalMediaFolder(Parcel in) {
        bucketId = in.readLong();
        folderName = in.readString();
        firstImagePath = in.readString();
        firstMimeType = in.readString();
        folderTotalNum = in.readInt();
        isSelectTag = in.readByte() != 0;
        ofAllType = in.readInt();
        data = in.createTypedArrayList(LocalMedia.CREATOR);
        currentDataPage = in.readInt();
        isHasMore = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(bucketId);
        dest.writeString(folderName);
        dest.writeString(firstImagePath);
        dest.writeString(firstMimeType);
        dest.writeInt(folderTotalNum);
        dest.writeByte((byte) (isSelectTag ? 1 : 0));
        dest.writeInt(ofAllType);
        dest.writeTypedList(data);
        dest.writeInt(currentDataPage);
        dest.writeByte((byte) (isHasMore ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<LocalMediaFolder> CREATOR = new Creator<LocalMediaFolder>() {
        @Override
        public LocalMediaFolder createFromParcel(Parcel in) {
            return new LocalMediaFolder(in);
        }

        @Override
        public LocalMediaFolder[] newArray(int size) {
            return new LocalMediaFolder[size];
        }
    };

    public String getFolderName() {
        return TextUtils.isEmpty(folderName) ? "unknown" : folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public int getFolderTotalNum() {
        return folderTotalNum;
    }

    public void setFolderTotalNum(int folderTotalNum) {
        this.folderTotalNum = folderTotalNum;
    }

    public boolean isSelectTag() {
        return isSelectTag;
    }

    public void setSelectTag(boolean selectTag) {
        isSelectTag = selectTag;
    }

    public long getBucketId() {
        return bucketId;
    }

    public void setBucketId(long bucketId) {
        this.bucketId = bucketId;
    }

    public String getFirstImagePath() {
        return firstImagePath;
    }

    public void setFirstImagePath(String firstImagePath) {
        this.firstImagePath = firstImagePath;
    }


    public int getOfAllType() {
        return ofAllType;
    }

    public void setOfAllType(int ofAllType) {
        this.ofAllType = ofAllType;
    }


    public List<LocalMedia> getData() {
        return data != null ? data : new ArrayList<>();
    }

    public void setData(List<LocalMedia> data) {
        this.data = data;
    }

    public int getCurrentDataPage() {
        return currentDataPage;
    }

    public void setCurrentDataPage(int currentDataPage) {
        this.currentDataPage = currentDataPage;
    }

    public boolean isHasMore() {
        return isHasMore;
    }

    public void setHasMore(boolean hasMore) {
        isHasMore = hasMore;
    }

    public String getFirstMimeType() {
        return firstMimeType;
    }

    public void setFirstMimeType(String firstMimeType) {
        this.firstMimeType = firstMimeType;
    }
}
