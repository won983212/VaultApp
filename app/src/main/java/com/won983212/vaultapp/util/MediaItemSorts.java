package com.won983212.vaultapp.util;

import com.won983212.vaultapp.MediaItem;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class MediaItemSorts {
    public static final Comparator<MediaItem> FILE_NAME_ASCENDING = (o1, o2) -> compareWithDirectoryFirst(o1, o2, o1.title.compareTo(o2.title));

    public static final Comparator<MediaItem> FILE_NAME_DESCENDING = (o1, o2) -> compareWithDirectoryFirst(o1, o2, o2.title.compareTo(o1.title));

    public static final Comparator<MediaItem> MODIFIED_DATE_ASCENDING = (o1, o2) -> compareWithDirectoryFirst(o1, o2, Long.compare(o1.lastModified, o2.lastModified));

    public static final Comparator<MediaItem> MODIFIED_DATE_DESCENDING = (o1, o2) -> compareWithDirectoryFirst(o1, o2, Long.compare(o2.lastModified, o1.lastModified));

    public static final Comparator<MediaItem> VIEW_COUNT_ASCENDING = (o1, o2) -> compareWithDirectoryFirst(o1, o2, Integer.compare(o1.viewCount, o2.viewCount));

    public static final Comparator<MediaItem> VIEW_COUNT_DESCENDING = (o1, o2) -> compareWithDirectoryFirst(o1, o2, Integer.compare(o2.viewCount, o1.viewCount));

    public static final Comparator<MediaItem> VIDEO_LENGTH_ASCENDING = (o1, o2) -> compareWithDirectoryFirst(o1, o2, Long.compare(o1.videoLengthLong, o2.videoLengthLong));

    public static final Comparator<MediaItem> VIDEO_LENGTH_DESCENDING = (o1, o2) -> compareWithDirectoryFirst(o1, o2, Long.compare(o2.videoLengthLong, o1.videoLengthLong));

    public static final String[] COMPARATOR_LABELS = {
            "파일 이름", "수정 날짜", "클릭 횟수", "영상 길이"
    };

    public static final List<Comparator<MediaItem>> COMPARATOR_OBJECTS = Arrays.asList(
            FILE_NAME_ASCENDING, FILE_NAME_DESCENDING, MODIFIED_DATE_ASCENDING, MODIFIED_DATE_DESCENDING, VIEW_COUNT_ASCENDING, VIEW_COUNT_DESCENDING, VIDEO_LENGTH_ASCENDING, VIDEO_LENGTH_DESCENDING
    );

    private static int compareWithDirectoryFirst(MediaItem o1, MediaItem o2, int otherCompare) {
        if (o1.isDirectory() && !o2.isDirectory())
            return -1;
        else if (o2.isDirectory() && !o1.isDirectory())
            return 1;
        return otherCompare;
    }
}
