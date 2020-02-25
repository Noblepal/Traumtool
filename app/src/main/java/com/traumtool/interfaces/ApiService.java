package com.traumtool.interfaces;


import com.traumtool.models.AuthorResponse;
import com.traumtool.models.DreamFileResponse;
import com.traumtool.models.FileResponse;
import com.traumtool.models.QuestionFileResponse;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

public interface ApiService {
    @GET("data/")
    Call<FileResponse> getFileList(
            @Query("category") String category
    );

    @GET("data/")
    Call<DreamFileResponse> getDreamFileList(
            @Query("category") String category
    );

    @GET("data/")
    Call<QuestionFileResponse> getQuestionFileList(
            @Query("category") String category
    );

    @Streaming
    @GET()
    Call<ResponseBody> downloadFile(@Url String url);

    @POST("data/dreamtravel/getPdfMiscellaneous.php")
    Call<AuthorResponse> getThisAuthor(
            @Query("filename") String filename
    );
}
