package com.traumtool.models;

import androidx.annotation.NonNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Question implements Serializable {
    @SerializedName("file_url")
    @Expose
    private String fileUrl;
    @SerializedName("id")
    @Expose
    private Integer id;
    @SerializedName("filename")
    @Expose
    private String filename;
    @SerializedName("category")
    @Expose
    private String category;

    public Question(String fileUrl, Integer id, String filename, String category) {
        this.fileUrl = fileUrl;
        this.id = id;
        this.filename = filename;
        this.category = category;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
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

    @NonNull
    @Override
    public String toString() {
        return this.filename + " -> " + this.category + " -> " + this.fileUrl;
    }
}
