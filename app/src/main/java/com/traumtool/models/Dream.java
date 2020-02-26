package com.traumtool.models;

import androidx.annotation.NonNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Dream implements Serializable {
    private String author;
    private String words;
    private String fileUrl;

    @SerializedName("id")
    @Expose
    private Integer id;
    private String originalFileName;
    @SerializedName("filename")
    @Expose
    private String filename;
    @SerializedName("category")
    @Expose
    private String category;

    public Dream(String author, String words, String fileUrl, Integer id, String filename, String category) {
        this.author = author;
        this.words = words;
        this.fileUrl = fileUrl;
        this.id = id;
        this.filename = filename;
        this.originalFileName = filename;
        this.category = category;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getWords() {
        return words;
    }

    public void setWords(String words) {
        this.words = words;
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

    public String getFileName() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
        this.originalFileName = filename;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    @NonNull
    @Override
    public String toString() {
        return this.filename + " author: " + this.author;
    }
}
