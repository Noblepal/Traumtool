package com.traumtool.activities;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
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
    private DreamAdapter dreamAdapter;
    private static final String TAG = "DreamActivity";
    private boolean isOfflineFromPrefs;
    private String category;
    private ImageButton backButton;
    private LinearLayout llNoOfflineFiles;
    private TextView tvErrorMessage;
    private ImageView topImage;
    private RecyclerView recyclerView;
    private String pdfText = "";
    private ProgressBar progressBar;
    boolean isFoundLocal = false;
    String[] firstLine;
    String title_and_author;
    String author;
    ApiService service;

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
                    hideView(llNoOfflineFiles);
                    populateRecyclerView(offlineDreams);
                } else {
                    showCustomSnackBar("No offline files found", false, null, -2);
                    tvErrorMessage.setText("No offline files");
                    showView(llNoOfflineFiles);
                    hideView(progressBar);
                }
            } else {
                retrieveBooks();
            }
        }, 1000);

    }

    private void retrieveBooks() {
        showView(progressBar);
        hideView(llNoOfflineFiles);
        ApiService service = AppUtils.getApiService();
        service.getDreamFileList(category).enqueue(new Callback<DreamFileResponse>() {
            @Override
            public void onResponse(Call<DreamFileResponse> call, Response<DreamFileResponse> response) {
                if (response.body().getError()) {

                    Toast.makeText(DreamActivity.this, "Error: " + response.body().getMessage(), Toast.LENGTH_SHORT).show();
                }
                hideView(llNoOfflineFiles);
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
                tvErrorMessage.setText("No internet connection");
                showView(llNoOfflineFiles);
                showCustomSnackBar("Failed to get data. Possibly due to network error", true, "Retry", -2);
            }
        });
    }

    private void getAuthorNamesFromURL(ArrayList<Dream> arrayList) {
        for (Dream d : arrayList) {
            extractBookAndAuthorFromFileName(d);
        }
    }

    private void extractBookAndAuthorFromFileName(Dream dream) {
        try { //Small delay to prevent ConcurrentModificationException (Adjust duration in case app is crashing)
            new Handler().postDelayed(() -> {
                String[] bookAndAuthor = AppUtils.stringSplitter(dream.getFileName());
                dream.setFilename(bookAndAuthor[0].trim()); //get book name
                dream.setAuthor(bookAndAuthor[1].trim()); //get author name
                hybridList.remove(dream);
                hybridList.add(dream);
                dreamAdapter.notifyDataSetChanged(); //reload adapter
                hideView(progressBar);
            }, 700);
        } catch (Exception e) {
            Toast.makeText(this, "Some author names could not be identified", Toast.LENGTH_SHORT).show();
        }


        /*service.getThisAuthor(dream.getFileName()).enqueue(new Callback<AuthorResponse>() {
            @Override
            public void onResponse(Call<AuthorResponse> call, Response<AuthorResponse> response) {
                if (!response.body().getAuthor().trim().equals("")) {
                    dream.setAuthor(response.body().getAuthor().trim());
                    hybridList.remove(dream);
                    hybridList.add(dream);
                    Log.d(TAG, "onResponse: dream: " + dream);
                    dreamAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<AuthorResponse> call, Throwable t) {
                Log.i(TAG, "onFailure: Failed to get author(s). Reason: " + t.getLocalizedMessage());
            }
        });*/
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
        new Handler().post(() -> {
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
            runOnUiThread(() -> {
                populateRecyclerView(hybridList);
                getAuthorNamesFromURL(hybridList);
            });
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
        service = AppUtils.getApiService();
        dreamAdapter = new DreamAdapter(this, hybridList);
        llNoOfflineFiles = findViewById(R.id.ll_dreams_no_offline_files);
        tvErrorMessage = findViewById(R.id.no_dream_offline_files);
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
        recyclerView.setAdapter(dreamAdapter);
        dreamAdapter.notifyDataSetChanged();
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
