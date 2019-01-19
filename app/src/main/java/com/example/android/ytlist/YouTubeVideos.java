package com.example.android.ytlist;

/**
 * Created by My sister is awesome on 1/19/2019.
 */

public class YouTubeVideos {
    String videoUrl;

    public YouTubeVideos() {
    }

    public YouTubeVideos(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
}