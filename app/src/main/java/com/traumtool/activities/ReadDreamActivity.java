package com.traumtool.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.github.barteksc.pdfviewer.PDFView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.traumtool.R;
import com.traumtool.interfaces.ApiService;
import com.traumtool.models.Dream;
import com.traumtool.utils.AppUtils;

import java.io.File;
import java.io.FileInputStream;
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

    private int pageIndex;
    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page currentPage;
    private ParcelFileDescriptor parcelFileDescriptor;
    private FloatingActionButton prePageButton, nextPageButton;
    PDFView imageViewPdf;
    ProgressBar downloadProgress, renderProgressBar;
    Dream dream;
    File audioFile;
    Boolean isDownloaded = false;
    private static final String TAG = "ReadDreamActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_dream);

        prePageButton = findViewById(R.id.button_pre_doc);
        nextPageButton = findViewById(R.id.button_next_doc);
        imageViewPdf = findViewById(R.id.pdfView);
        downloadProgress = findViewById(R.id.pdf_progress_bar);
        renderProgressBar = findViewById(R.id.render_progress_bar);

        prePageButton.setOnClickListener(v -> onPreviousDocClick());
        nextPageButton.setOnClickListener(v -> onNextDocClick());

        pageIndex = 0;


        getExtras();
    }

    private void getExtras() {
        if (getIntent().hasExtra("item")) {
            dream = (Dream) getIntent().getSerializableExtra("item");
            downloadPDF();
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

    public void onPreviousDocClick() {
        //showPage(currentPage.getIndex() - 1);
    }

    public void onNextDocClick() {
        //..showPage(currentPage.getIndex() + 1);
    }

    private void downloadPDF() {
        showView(downloadProgress);
        String url = "data/" + dream.getCategory() + "/" + dream.getFileName();
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
                                    showCustomSnackBar("File downloaded", false, null);
                                }
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void aVoid) {
                                hideView(downloadProgress);
                                if (isDownloaded)
                                    showCustomSnackBar("Download Complete", false, null);
                                else {
                                    showCustomSnackBar("Something went wrong line 138", true, "Try Again");
                                }

                                try {
                                    renderPDF(audioFile);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }.execute();
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.d(TAG, "onFailure: " + call.toString());
                        hideView(downloadProgress);
                        Toast.makeText(ReadDreamActivity.this, "Failed to get file, reason: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean writeResponseBodyToDisk(ResponseBody body) {
        try {
            File path = ReadDreamActivity.this.getExternalFilesDir("Download/" + dream.getCategory() + "/");

            InputStream inputStream = null;
            OutputStream outputStream = null;
            audioFile = new File(path, dream.getFileName());

            if (audioFile.exists()) {
                isDownloaded = true;
                return true;
            }

            try {
                byte[] fileReader = new byte[4096];

                long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;

                inputStream = body.byteStream();
                outputStream = new FileOutputStream(audioFile);

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


    private void openRenderer(Context context) throws IOException {
        showView(renderProgressBar);
        // In this sample, we read a PDF from the assets directory.
        File path = ReadDreamActivity.this.getExternalFilesDir("Download/" + dream.getCategory());
        Log.d(TAG, "openRenderer: PATH: " + path);
        File file = new File(path, dream.getFileName());
        if (!file.exists()) {
            // Since PdfRenderer cannot handle the compressed asset file directly, we copy it into
            // the cache directory.
            //InputStream asset = context.getExternalFilesDir("text.txt");

            assert path != null;
            FileInputStream fs = new FileInputStream(path);
            File file1 = new File(path, dream.getFileName());
            FileOutputStream output = new FileOutputStream(file1);
            final byte[] buffer = new byte[1024];
            int size;
            while ((size = fs.read(buffer)) != -1) {
                output.write(buffer, 0, size);
            }
            fs.close();
            output.close();
            hideView(renderProgressBar);
        }
        parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        // This is the PdfRenderer we use to render the PDF.
        if (parcelFileDescriptor != null) {
            pdfRenderer = new PdfRenderer(parcelFileDescriptor);
        }
    }

    private void closeRenderer() throws IOException {
        if (null != currentPage) {
            currentPage.close();
        }
        pdfRenderer.close();
        parcelFileDescriptor.close();
    }

    private void showPage(int index) {
        if (pdfRenderer.getPageCount() <= index) {
            return;
        }
        // Make sure to close the current page before opening another one.
        if (null != currentPage) {
            currentPage.close();
        }
        // Use `openPage` to open a specific page in PDF.
        currentPage = pdfRenderer.openPage(index);
        // Important: the destination bitmap must be ARGB (not RGB).
        Bitmap bitmap = Bitmap.createBitmap(currentPage.getWidth(), currentPage.getHeight(),
                Bitmap.Config.ARGB_8888);
        // Here, we render the page onto the Bitmap.
        // To render a portion of the page, use the second and third parameter. Pass nulls to get
        // the default result.
        // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last parameter.
        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        // We are ready to show the Bitmap to user.
        //imageViewPdf.setImageBitmap(bitmap);
        updateUi();
    }

    private void updateUi() {
        int index = currentPage.getIndex();
        int pageCount = pdfRenderer.getPageCount();
        prePageButton.setEnabled(0 != index);
        nextPageButton.setEnabled(index + 1 < pageCount);
    }

    public int getPageCount() {
        return pdfRenderer.getPageCount();
    }

    private void renderPDF(File file) {
        imageViewPdf.fromFile(file)
                .pages(0, 2, 1, 3, 3, 3) // all pages are displayed by default
                .enableSwipe(true) // allows to block changing pages using swipe
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .defaultPage(0)
                .enableAnnotationRendering(false) // render annotations (such as comments, colors or forms)
                .password(null)
                .scrollHandle(null)
                .enableAntialiasing(true) // improve rendering a little bit on low-res screens
                .spacing(0)
                .load();

        hideView(renderProgressBar);
    }

    private void showCustomSnackBar(String message, boolean hasAction, @Nullable String actionText) {
        Snackbar snackbar = Snackbar.make(imageViewPdf, message, Snackbar.LENGTH_LONG);
        if (hasAction) {
            snackbar.setAction(actionText, v -> downloadPDF());
        }
        snackbar.show();
    }
}
