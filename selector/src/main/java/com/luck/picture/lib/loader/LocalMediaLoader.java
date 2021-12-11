package com.luck.picture.lib.loader;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.luck.picture.lib.R;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.config.PictureSelectionConfig;
import com.luck.picture.lib.config.SelectMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.entity.LocalMediaFolder;
import com.luck.picture.lib.interfaces.OnQueryAlbumListener;
import com.luck.picture.lib.interfaces.OnQueryAllAlbumListener;
import com.luck.picture.lib.thread.PictureThreadUtils;
import com.luck.picture.lib.utils.MediaUtils;
import com.luck.picture.lib.utils.SdkVersionUtils;
import com.luck.picture.lib.utils.SortUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * @author：luck
 * @data：2016/12/31 19:12
 * @describe: Local media database query class
 */
@Deprecated
public final class LocalMediaLoader extends IBridgeMediaLoader {

    /**
     * Media file database field
     */
    private static final String[] PROJECTION = {
            MediaStore.Files.FileColumns._ID,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.DURATION,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.DATE_ADDED};

    /**
     * Video or Audio mode conditions
     *
     * @param sizeCondition
     * @param queryMimeCondition
     * @return
     */
    private static String getSelectionArgsForVideoOrAudioMediaCondition(String sizeCondition, String queryMimeCondition) {
        return MediaStore.Files.FileColumns.MEDIA_TYPE + "=?" + queryMimeCondition + " AND " + sizeCondition;
    }

    /**
     * Query conditions in all modes
     *
     * @param timeCondition
     * @param sizeCondition
     * @param queryMimeCondition
     * @return
     */
    private static String getSelectionArgsForAllMediaCondition(String timeCondition, String sizeCondition, String queryMimeCondition) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("(").append(MediaStore.Files.FileColumns.MEDIA_TYPE).append("=?").append(queryMimeCondition).append(" OR ")
                .append(MediaStore.Files.FileColumns.MEDIA_TYPE).append("=? AND ").append(timeCondition).append(") AND ").append(sizeCondition).toString();
        return stringBuilder.toString();
    }

    /**
     * Query conditions in image modes
     *
     * @param sizeCondition
     * @param queryMimeCondition
     * @return
     */
    private static String getSelectionArgsForImageMediaCondition(String sizeCondition, String queryMimeCondition) {
        return MediaStore.Files.FileColumns.MEDIA_TYPE + "=?" + queryMimeCondition + " AND " + sizeCondition;
    }

    /**
     * Gets a file of the specified type
     *
     * @return
     */
    private static String[] getSelectionArgsForAllMediaType() {
        return new String[]{String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE), String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)};
    }

    /**
     * Gets a file of the specified type
     *
     * @param mediaType
     * @return
     */
    private static String[] getSelectionArgsForSingleMediaType(int mediaType) {
        return new String[]{String.valueOf(mediaType)};
    }


    public LocalMediaLoader(Context context, PictureSelectionConfig config) {
        this.mContext = context;
        this.config = config;
    }

    @Override
    public void loadOnlyInAppDirAllMedia(OnQueryAlbumListener<LocalMediaFolder> listener) {
        PictureThreadUtils.executeByIo(new PictureThreadUtils.SimpleTask<LocalMediaFolder>() {

            @Override
            public LocalMediaFolder doInBackground() {
                return SandboxFileLoader.loadInAppSandboxFolderFile(mContext, config.sandboxDir);
            }

            @Override
            public void onSuccess(LocalMediaFolder result) {
                PictureThreadUtils.cancel(this);
                if (listener != null) {
                    listener.onComplete(result);
                }
            }
        });
    }

    @Override
    public void loadAllMedia(OnQueryAllAlbumListener<LocalMediaFolder> listener) {
        PictureThreadUtils.executeByIo(new PictureThreadUtils.SimpleTask<List<LocalMediaFolder>>() {

            @Override
            public List<LocalMediaFolder> doInBackground() {
                List<LocalMediaFolder> imageFolders = new ArrayList<>();
                Cursor data = mContext.getContentResolver().query(QUERY_URI, PROJECTION,
                        getSelection(), getSelectionArgs(), ORDER_BY);
                try {
                    if (data != null) {
                        LocalMediaFolder allImageFolder = new LocalMediaFolder();
                        ArrayList<LocalMedia> latelyImages = new ArrayList<>();
                        int count = data.getCount();
                        if (count > 0) {
                            int idColumn = data.getColumnIndexOrThrow(PROJECTION[0]);
                            int dataColumn = data.getColumnIndexOrThrow(PROJECTION[1]);
                            int mimeTypeColumn = data.getColumnIndexOrThrow(PROJECTION[2]);
                            int widthColumn = data.getColumnIndexOrThrow(PROJECTION[3]);
                            int heightColumn = data.getColumnIndexOrThrow(PROJECTION[4]);
                            int durationColumn = data.getColumnIndexOrThrow(PROJECTION[5]);
                            int sizeColumn = data.getColumnIndexOrThrow(PROJECTION[6]);
                            int folderNameColumn = data.getColumnIndexOrThrow(PROJECTION[7]);
                            int fileNameColumn = data.getColumnIndexOrThrow(PROJECTION[8]);
                            int bucketIdColumn = data.getColumnIndexOrThrow(PROJECTION[9]);
                            int dateAddedColumn = data.getColumnIndexOrThrow(PROJECTION[10]);

                            data.moveToFirst();
                            do {
                                long id = data.getLong(idColumn);
                                String mimeType = data.getString(mimeTypeColumn);
                                mimeType = TextUtils.isEmpty(mimeType) ? PictureMimeType.ofJPEG() : mimeType;
                                String absolutePath = data.getString(dataColumn);
                                String url = SdkVersionUtils.isQ() ? MediaUtils.getRealPathUri(id, mimeType) : absolutePath;
                                // Here, it is solved that some models obtain mimeType and return the format of image / *,
                                // which makes it impossible to distinguish the specific type, such as mi 8,9,10 and other models
                                if (mimeType.endsWith("image/*")) {
                                    if (PictureMimeType.isContent(url)) {
                                        mimeType = PictureMimeType.getImageMimeType(absolutePath);
                                    } else {
                                        mimeType = PictureMimeType.getImageMimeType(url);
                                    }
                                    if (!config.isGif) {
                                        if (PictureMimeType.isGif(mimeType)) {
                                            continue;
                                        }
                                    }
                                }
                                if (!config.isWebp) {
                                    if (mimeType.startsWith(PictureMimeType.ofWEBP())) {
                                        continue;
                                    }
                                }
                                if (!config.isBmp) {
                                    if (mimeType.startsWith(PictureMimeType.ofBMP())) {
                                        continue;
                                    }
                                }

                                int width = data.getInt(widthColumn);
                                int height = data.getInt(heightColumn);
                                long duration = data.getLong(durationColumn);
                                long size = data.getLong(sizeColumn);
                                String folderName = data.getString(folderNameColumn);
                                String fileName = data.getString(fileNameColumn);
                                long bucketId = data.getLong(bucketIdColumn);

                                if (PictureMimeType.isHasVideo(mimeType)) {
                                    if (config.videoMinSecond > 0 && duration < config.videoMinSecond) {
                                        // If you set the minimum number of seconds of video to display
                                        continue;
                                    }
                                    if (config.videoMaxSecond > 0 && duration > config.videoMaxSecond) {
                                        // If you set the maximum number of seconds of video to display
                                        continue;
                                    }
                                    if (duration == 0) {
                                        //If the length is 0, the corrupted video is processed and filtered out
                                        continue;
                                    }
                                    if (size <= 0) {
                                        // The video size is 0 to filter out
                                        continue;
                                    }
                                }
                                LocalMedia image = LocalMedia.parseLocalMedia(id, url, absolutePath, fileName, folderName, duration, config.chooseMode, mimeType, width, height, size, bucketId, data.getLong(dateAddedColumn));
                                LocalMediaFolder folder = getImageFolder(url, mimeType, folderName, imageFolders);
                                folder.setBucketId(image.getBucketId());
                                List<LocalMedia> images = folder.getData();
                                images.add(image);
                                folder.setFolderTotalNum(folder.getFolderTotalNum() + 1);
                                folder.setBucketId(image.getBucketId());
                                latelyImages.add(image);
                                int imageNum = allImageFolder.getFolderTotalNum();
                                allImageFolder.setFolderTotalNum(imageNum + 1);

                            } while (data.moveToNext());

                            LocalMediaFolder selfFolder = SandboxFileLoader
                                    .loadInAppSandboxFolderFile(mContext, config.sandboxDir);
                            if (selfFolder != null) {
                                imageFolders.add(selfFolder);
                                allImageFolder.setFolderTotalNum(allImageFolder.getFolderTotalNum() + selfFolder.getFolderTotalNum());
                                allImageFolder.setData(selfFolder.getData());
                                latelyImages.addAll(0, selfFolder.getData());
                                if (MAX_SORT_SIZE > selfFolder.getFolderTotalNum()) {
                                    if (latelyImages.size() > MAX_SORT_SIZE) {
                                        SortUtils.sortLocalMediaAddedTime(latelyImages.subList(0, MAX_SORT_SIZE));
                                    } else {
                                        SortUtils.sortLocalMediaAddedTime(latelyImages);
                                    }
                                }
                            }

                            if (latelyImages.size() > 0) {
                                SortUtils.sortFolder(imageFolders);
                                imageFolders.add(0, allImageFolder);
                                allImageFolder.setFirstImagePath
                                        (latelyImages.get(0).getPath());
                                allImageFolder.setFirstMimeType(latelyImages.get(0).getMimeType());
                                String title = config.chooseMode == SelectMimeType.ofAudio() ?
                                        mContext.getString(R.string.ps_all_audio)
                                        : mContext.getString(R.string.ps_camera_roll);
                                allImageFolder.setFolderName(title);
                                allImageFolder.setBucketId(PictureConfig.ALL);
                                allImageFolder.setData(latelyImages);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (data != null && !data.isClosed()) {
                        data.close();
                    }
                }
                return imageFolders;
            }

            @Override
            public void onSuccess(List<LocalMediaFolder> result) {
                PictureThreadUtils.cancel(this);
                if (listener != null) {
                    listener.onComplete(result);
                }
            }
        });
    }

    private String getSelection() {
        String durationCondition = getDurationCondition();
        String fileSizeCondition = getFileSizeCondition();
        String queryMimeCondition = getQueryMimeCondition();
        switch (config.chooseMode) {
            case SelectMimeType.TYPE_ALL:
                // Get all, not including audio
                return getSelectionArgsForAllMediaCondition(durationCondition, fileSizeCondition, queryMimeCondition);
            case SelectMimeType.TYPE_IMAGE:
                // Gets the image
                return getSelectionArgsForImageMediaCondition(fileSizeCondition, queryMimeCondition);
            case SelectMimeType.TYPE_VIDEO:
                // Access to video
                return getSelectionArgsForVideoOrAudioMediaCondition(fileSizeCondition, queryMimeCondition);
            case SelectMimeType.TYPE_AUDIO:
                // Access to the audio
                return getSelectionArgsForVideoOrAudioMediaCondition(durationCondition, queryMimeCondition);
        }
        return null;
    }

    private String[] getSelectionArgs() {
        switch (config.chooseMode) {
            case SelectMimeType.TYPE_ALL:
                // Get All
                return getSelectionArgsForAllMediaType();
            case SelectMimeType.TYPE_IMAGE:
                // Get Image
                return getSelectionArgsForSingleMediaType(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE);
            case SelectMimeType.TYPE_VIDEO:
                // Get Video
                return getSelectionArgsForSingleMediaType(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO);
            case SelectMimeType.TYPE_AUDIO:
                // Get Audio
                return getSelectionArgsForSingleMediaType(MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO);
        }
        return null;
    }


    /**
     * Create folder
     *
     * @param firstPath
     * @param firstMimeType
     * @param imageFolders
     * @param folderName
     * @return
     */
    private LocalMediaFolder getImageFolder(String firstPath, String firstMimeType, String folderName, List<LocalMediaFolder> imageFolders) {
        for (LocalMediaFolder folder : imageFolders) {
            // Under the same folder, return yourself, otherwise create a new folder
            String name = folder.getFolderName();
            if (TextUtils.isEmpty(name)) {
                continue;
            }
            if (TextUtils.equals(name, folderName)) {
                return folder;
            }
        }
        LocalMediaFolder newFolder = new LocalMediaFolder();
        newFolder.setFolderName(folderName);
        newFolder.setFirstImagePath(firstPath);
        newFolder.setFirstMimeType(firstMimeType);
        imageFolders.add(newFolder);
        return newFolder;
    }

    /**
     * Get video (maximum or minimum time)
     *
     * @return
     */
    private String getDurationCondition() {
        long maxS = config.videoMaxSecond == 0 ? Long.MAX_VALUE : config.videoMaxSecond;
        return String.format(Locale.CHINA, "%d <%s " + MediaStore.MediaColumns.DURATION + " and " + MediaStore.MediaColumns.DURATION + " <= %d",
                Math.max((long) 0, config.videoMinSecond),
                Math.max((long) 0, config.videoMinSecond) == 0 ? "" : "=",
                maxS);
    }

    /**
     * Get media size (maxFileSize or miniFileSize)
     *
     * @return
     */
    private String getFileSizeCondition() {
        long maxS = config.filterMaxFileSize == 0 ? Long.MAX_VALUE : config.filterMaxFileSize;
        return String.format(Locale.CHINA, "%d <%s " + MediaStore.MediaColumns.SIZE + " and " + MediaStore.MediaColumns.SIZE + " <= %d",
                Math.max(0, config.filterMinFileSize),
                Math.max(0, config.filterMinFileSize) == 0 ? "" : "=",
                maxS);
    }

    private String getQueryMimeCondition() {
        List<String> filters = config.queryOnlyList;
        HashSet<String> filterSet = new HashSet<>(filters);
        Iterator<String> iterator = filterSet.iterator();
        StringBuilder stringBuilder = new StringBuilder();
        int index = -1;
        while (iterator.hasNext()) {
            String value = iterator.next();
            if (TextUtils.isEmpty(value)) {
                continue;
            }
            if (config.chooseMode == SelectMimeType.ofVideo()) {
                if (value.startsWith(PictureMimeType.MIME_TYPE_PREFIX_IMAGE) || value.startsWith(PictureMimeType.MIME_TYPE_PREFIX_AUDIO)) {
                    continue;
                }
            } else if (config.chooseMode == SelectMimeType.ofImage()) {
                if (value.startsWith(PictureMimeType.MIME_TYPE_PREFIX_AUDIO) || value.startsWith(PictureMimeType.MIME_TYPE_PREFIX_VIDEO)) {
                    continue;
                }
            } else if (config.chooseMode == SelectMimeType.ofAudio()) {
                if (value.startsWith(PictureMimeType.MIME_TYPE_PREFIX_VIDEO) || value.startsWith(PictureMimeType.MIME_TYPE_PREFIX_IMAGE)) {
                    continue;
                }
            }
            index++;
            stringBuilder.append(index == 0 ? " AND " : " OR ").append(MediaStore.MediaColumns.MIME_TYPE).append("='").append(value).append("'");
        }
        if (config.chooseMode != SelectMimeType.ofVideo()) {
            if (!config.isGif && !filterSet.contains(PictureMimeType.ofGIF())) {
                stringBuilder.append(NOT_GIF);
            }
        }
        return stringBuilder.toString();
    }

}
