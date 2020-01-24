package com.traumtool.models;

import androidx.annotation.NonNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Music implements Serializable {
    @SerializedName("id")
    @Expose
    private Integer id;
    @SerializedName("filename")
    @Expose
    private String filename;
    @SerializedName("category")
    @Expose
    private String category;
    @SerializedName("duration")
    @Expose
    private int duration;
    @SerializedName("file_url")
    @Expose
    private String fileUrl;
    @SerializedName("file_size")
    @Expose
    private long fileSize;

    public Music(Integer id, String filename, String category, int duration, String fileUrl, long fileSize) {
        this.id = id;
        this.filename = filename;
        this.category = category;
        this.duration = duration;
        this.fileUrl = fileUrl;
        this.fileSize = fileSize;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    @NonNull
    @Override
    public String toString() {
        return this.filename + ": " + this.fileUrl;
    }
}

