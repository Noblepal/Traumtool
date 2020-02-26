package com.traumtool.activities;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.pdfview.PDFView;
import com.traumtool.R;
import com.traumtool.interfaces.ApiService;
import com.traumtool.models.Dream;
import com.traumtool.utils.AppUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.traumtool.utils.AppUtils.hideView;
import static com.traumtool.utils.AppUtils.showView;

public class ReadDreamActivity extends AppCompatActivity {

    PDFView imageViewPdf;
    ProgressBar renderProgressBar;
    Dream dream;
    TextView tvTitle;
    File pdfFile;
    ImageButton backButton;
    Boolean isDownloaded = false;
    private static final String TAG = "ReadDreamActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.FullscreenTheme);
        setContentView(R.layout.activity_read_dream);

        imageViewPdf = findViewById(R.id.pdfView);
        tvTitle = findViewById(R.id.tvAppTitle);
        renderProgressBar = findViewById(R.id.render_progress_bar);
        backButton = findViewById(R.id.imgBackReadDream);
        backButton.setOnClickListener(v -> onBackPressed());

        getExtras();
    }

    private void getExtras() {
        if (getIntent().hasExtra("item")) {
            dream = (Dream) getIntent().getSerializableExtra("item");
            assert dream != null;
            tvTitle.setText(dream.getFileName());
            if (dream.getAuthor() != null && dream.getWords() != null) {
                loadLocalFile();
            } else {
                downloadPDF();
            }
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        try {
            //openRenderer(getApplicationContext());
            //showPage(pageIndex);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStop() {
        try {
            //closeRenderer();
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onStop();
    }

    private void downloadPDF() {
        showView(renderProgressBar);
        String url = "data/" + dream.getCategory() + "/" + dream.getOriginalFileName();
        ApiService service = AppUtils.getApiService();
        service.downloadFile(url).
                enqueue(new Callback<ResponseBody>() {
                    @SuppressLint("StaticFieldLeak")
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        //TODO: In case it stops working (again) Delete this AsyncTask and remove the @Streaming annotation from the API
                        new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected Void doInBackground(Void... voids) {
                                if (response.body() != null) {
                                    isDownloaded = writeResponseBodyToDisk(response.body());//Write downloaded file to disk
                                    Log.d(TAG, "doInBackground: 124 " + isDownloaded);

                                } else {
                                    showCustomSnackBar("File downloaded", false, null, 0);
                                }
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void aVoid) {
                                hideView(renderProgressBar);
                                if (isDownloaded) {
                                    //showCustomSnackBar("Download Complete", false, null);
                                } else {
                                    showCustomSnackBar("Something went wrong", true, "Try Again", -2);
                                }

                                try {
                                    renderPDF(pdfFile);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }.execute();
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.d(TAG, "onFailure: " + call.toString());
                        hideView(renderProgressBar);
                        Toast.makeText(ReadDreamActivity.this, "Failed to get file, reason: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadLocalFile() {
        File path = ReadDreamActivity.this.getExternalFilesDir("Download/" + dream.getCategory() + "/");
        showCustomSnackBar("Viewing local file", false, null, -1);
        renderPDF(new File(path, dream.getFileName()));
    }

    private boolean writeResponseBodyToDisk(ResponseBody body) {
        try {
            File path = ReadDreamActivity.this.getExternalFilesDir("Download/" + dream.getCategory() + "/");

            InputStream inputStream = null;
            OutputStream outputStream = null;
            pdfFile = new File(path, dream.getFileName());

            if (pdfFile.exists()) {
                isDownloaded = true;
                return true;
            }

            try {
                byte[] fileReader = new byte[4096];

                long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;

                inputStream = body.byteStream();
                outputStream = new FileOutputStream(pdfFile);

                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }

                    outputStream.write(fileReader, 0, read);

                    fileSizeDownloaded += read;

                    Log.d(TAG, "file download: " + fileSizeDownloaded + " of " + fileSize);
                }

                outputStream.flush();

                return true;
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "writeResponseBodyToDisk: 190 " + e.getLocalizedMessage());
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            Log.d(TAG, "writeResponseBodyToDisk: 202 " + e.getLocalizedMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void renderPDF(File file) {
        imageViewPdf.fromFile(file).show();
        hideView(renderProgressBar);
    }

    private void showCustomSnackBar(String message, boolean hasAction, @Nullable String actionText, int length) {
        Snackbar snackbar = Snackbar.make(imageViewPdf, message, length);
        if (hasAction) {
            snackbar.setAction(actionText, v -> downloadPDF());
        }
        snackbar.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

}
