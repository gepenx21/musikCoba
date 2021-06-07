package com.appodeals.musikcoba.Model;


public class Song {

    private String artist;
    private String title;
    private int duration;
    private String streamUrl;

    public Song(String artist, String title, int duration, String streamUrl) {
        this.artist = artist;
        this.title = title;
        this.duration = duration;
        this.streamUrl = streamUrl;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    public String getArtist() {
        return artist;
    }

    public String getTitle() {
        return title;
    }

    public int getDuration() {
        return duration;
    }

    public String getStreamUrl() {
        return streamUrl;
    }
}
