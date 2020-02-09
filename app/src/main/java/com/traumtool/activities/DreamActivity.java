package com.traumtool.activities;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.snackbar.Snackbar;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import com.traumtool.R;
import com.traumtool.adapters.DreamAdapter;
import com.traumtool.interfaces.ApiService;
import com.traumtool.models.Dream;
import com.traumtool.models.DreamFileResponse;
import com.traumtool.utils.AppUtils;
import com.traumtool.utils.SharedPrefsManager;

import java.io.File;
import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.traumtool.utils.AppUtils.hideView;
import static com.traumtool.utils.AppUtils.showView;

public class DreamActivity extends AppCompatActivity {

    private ArrayList<Dream> dreamArrayList = new ArrayList<>();
    private ArrayList<Dream> offlineDreams = new ArrayList<>();
    private ArrayList<Dream> hybridList = new ArrayList<>();
    private static final String TAG = "DreamActivity";
    private boolean isOfflineFromPrefs;
    private String category;
    private ImageButton backButton;
    private ImageView topImage;
    private RecyclerView recyclerView;
    private String pdfText = "";
    private ProgressBar progressBar;
    boolean isFoundLocal = false;
    String[] firstLine;
    String title_and_author;
    String author;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dream);

        //Get online / offline boolean from shared preferences
        isOfflineFromPrefs = SharedPrefsManager.getInstance(this).getIsOffline();
        category = getIntent().getStringExtra("category");

        initializeStuff();
        showView(progressBar);

        Handler handler = new Handler();
        handler.postDelayed(() -> {
            if (isOfflineFromPrefs) {
                if (getFiles()) {
                    populateRecyclerView(offlineDreams);
                } else {
                    showCustomSnackBar("No offline files found", false, null, -2);
                    hideView(progressBar);
                }
            } else {
                retrieveBooks();
            }
        }, 1000);

    }

    private void retrieveBooks() {
        showView(progressBar);
        ApiService service = AppUtils.getApiService();
        service.getDreamFileList(category).enqueue(new Callback<DreamFileResponse>() {
            @Override
            public void onResponse(Call<DreamFileResponse> call, Response<DreamFileResponse> response) {
                if (response.body().getError()) {
                    hideView(progressBar);
                    Toast.makeText(DreamActivity.this, "Error: " + response.body().getMessage(), Toast.LENGTH_SHORT).show();
                }
                Toast.makeText(DreamActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                try {
                    getFiles();
                    dreamArrayList.addAll(response.body().getDreams());

                    checkIfFilesExist();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<DreamFileResponse> call, Throwable t) {
                hideView(progressBar);
                showCustomSnackBar("Failed to get data. Possibly due to network error", true, "Retry", -2);
            }
        });
    }

    private void showCustomSnackBar(String message, boolean hasAction,
                                    @Nullable String actionText, int LENGTH) {
        Snackbar snackbar = Snackbar.make(progressBar, message, LENGTH);
        if (hasAction) {
            snackbar.setAction(actionText, v -> retrieveBooks());
        }
        snackbar.show();
    }

    private void checkIfFilesExist() {
        Handler handler = new Handler();

        handler.post(() -> {
            for (int t = 0; t < dreamArrayList.size(); t++) {
                for (int f = 0; f < offlineDreams.size(); f++) {
                    if (dreamArrayList.get(t).getFileName().equals(offlineDreams.get(f).getFileName())) {
                        Log.d(TAG, "checkIfFilesExist: Exists: " + offlineDreams.get(f).getFileName());
                        hybridList.add(offlineDreams.get(f));
                        isFoundLocal = true;
                        break;
                    } else {
                        isFoundLocal = false;
                        Log.d(TAG, "checkIfFilesExist: Not exists: " + dreamArrayList.get(f).getFileName());
                    }
                }
                if (!isFoundLocal) {
                    hybridList.add(dreamArrayList.get(t));
                }
            }
            runOnUiThread(() -> populateRecyclerView(hybridList));
        });

    }

    /*Retrieve all offline files*/
    private boolean getFiles() {
        showView(progressBar);
        File path = DreamActivity.this.getExternalFilesDir("Download/" + category + "/");
        Log.d(TAG, "getFiles: " + path);
        String uri = String.valueOf(path);
        File file = new File(uri);
        File[] files = file.listFiles();

        int id = 0;
        for (File f : files) {
            Log.d(TAG, "getFiles: " + f.getName());
            if (f.getName().equals("") || f.getName().isEmpty()) {
                break;
                //return false;
            }

            String words = getWordCount(f);
            String author = getAuthorName(pdfText);

            Dream dream = new Dream(
                    author, words, "", id++, f.getName(), category
            );

            if (dream.getFileName().isEmpty() || dream.getFileName() == null) {
                Log.d(TAG, "getFiles: Is empty");
            } else {
                offlineDreams.add(dream);
            }
        }
        return offlineDreams.size() > 0;
    }


    private String getWordCount(File file) {
        pdfText = "";
        try {
            PdfReader reader = new PdfReader(file.getAbsolutePath());
            int n = reader.getNumberOfPages();
            for (int i = 0; i < n; i++) {
                pdfText = pdfText + PdfTextExtractor.getTextFromPage(reader, i + 1).trim() + "\n"; //Extracting the content from the different pages
            }
            //System.out.println(pdfText);
            reader.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        String[] wc = pdfText.split("\\s+");
        return String.valueOf(wc.length);
    }

    private String getAuthorName(String text) {
        if (text.isEmpty()) return "";
        firstLine = text.split("\n", 2);
        title_and_author = firstLine[0];
        Log.d(TAG, "getAuthorName: " + title_and_author);

        author = title_and_author.substring(title_and_author.indexOf("~") + 2);
        Log.d(TAG, "getAuthorName: author: " + author);

        return author;
    }

    private void initializeStuff() {
        progressBar = findViewById(R.id.dream_progress_bar);
        recyclerView = findViewById(R.id.recyclerViewDream);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(false);
        backButton = findViewById(R.id.imgBackDream);
        backButton.setOnClickListener(v -> onBackPressed());
        topImage = findViewById(R.id.imageView3);

        Glide.with(this).load("https://source.unsplash.com/random/?nature,water")
                .fallback(R.drawable.day)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .transition(DrawableTransitionOptions.withCrossFade(600))
                .placeholder(R.drawable.day)
                .into(topImage);

    }

    private void populateRecyclerView(ArrayList<Dream> dreams) {
        hideView(progressBar);
        recyclerView.setAdapter(new DreamAdapter(this, dreams));
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
