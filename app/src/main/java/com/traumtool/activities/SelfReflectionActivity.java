package com.traumtool.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.snackbar.Snackbar;
import com.traumtool.R;
import com.traumtool.interfaces.ApiService;
import com.traumtool.models.Question;
import com.traumtool.models.QuestionFileResponse;
import com.traumtool.utils.AppUtils;
import com.traumtool.utils.SharedPrefsManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.traumtool.utils.AppUtils.RANDOM_PIC_URL;
import static com.traumtool.utils.AppUtils.hideView;
import static com.traumtool.utils.AppUtils.showView;
import static com.traumtool.utils.AppUtils.tempHideView;

public class SelfReflectionActivity extends AppCompatActivity {

    private ArrayList<Question> questionArrayList = new ArrayList<>();
    private ArrayList<Question> offlineFiles = new ArrayList<>();
    private static final String TAG = "SelfReflectionActivity";
    TextView tvQuestion, skipQuestion, titleQuestion;
    RelativeLayout buttonNextQuestion;
    ImageButton backButton;
    ProgressBar downloadTextFileProgress;
    String category;
    ImageView backGround;
    Question question;
    File questionFile;
    int lastid = 1;
    String read_files = " ";
    List<Integer> alreadyViewedQuestions = new ArrayList<>();
    boolean isDownloaded = false, isOfflineFromPrefs = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_self_reflection);
        category = getIntent().getStringExtra("category");
        initializeStuff();
    }

    private void initializeStuff() {
        backGround = findViewById(R.id.imageViewReflection);
        backButton = findViewById(R.id.imgBackSelfReflection);
        backButton.setOnClickListener(v -> onBackPressed());
        titleQuestion = findViewById(R.id.textViewQuestionTitle);
        skipQuestion = findViewById(R.id.tv_skip_question);
        skipQuestion.setOnClickListener(v -> displayNextQuestion(getNextFile()));

        Glide.with(this).load(RANDOM_PIC_URL)
                .fallback(R.drawable.relaxation)
                .placeholder(R.drawable.relaxation)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .transition(DrawableTransitionOptions.withCrossFade(700))
                .into(backGround);

        tvQuestion = findViewById(R.id.textViewQuestion);
        buttonNextQuestion = findViewById(R.id.materialButtonReflection);
        downloadTextFileProgress = findViewById(R.id.downloadTextFileProgress);

        //Get online/offline boolean from shared preferences
        isOfflineFromPrefs = SharedPrefsManager.getInstance(this).getIsOffline();

        if (isOfflineFromPrefs) {
            //Retrieve locally downloaded files
            getFiles();
        } else {
            //Retrieve files from server
            retrieveQuestionList();
        }
        buttonNextQuestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tvQuestion.getText().toString().isEmpty()) {

                } else {
                    showCongratulationsMessage();
                    //new Handler().postDelayed(() -> displayNextQuestion(getNextFile()), 1000);
                }
            }
        });

    }

    /*
     * Display next random question from text files
     */
    private void displayNextQuestion(File question) {
        showView(downloadTextFileProgress);
        tempHideView(titleQuestion);
        if (question == null) {
            tvQuestion.setText("");
            return;
        }
        Log.d(TAG, "displayNextQuestion: " + question.getName());
        StringBuilder text = new StringBuilder();
        try {
            question = new File(question.getPath());
            BufferedReader br = new BufferedReader(new FileReader(question));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "displayNextQuestion: " + text.toString());
        tvQuestion.setText(text.toString());
        hideView(downloadTextFileProgress);
        showView(titleQuestion);
    }

    private void getFiles() {
        showView(downloadTextFileProgress);
        File path = SelfReflectionActivity.this.getExternalFilesDir("Download/" + category);
        String uri = String.valueOf(path);
        File file = new File(uri);
        File[] files = file.listFiles();
        Log.i(TAG, "getFiles: Looking in this PATH -> " + uri);
        Log.e(TAG, "getFiles: offline file size::::" + files.length);

        int id = 0;
        for (File f : files) {
            Log.e(TAG, "getFiles: " + f.getName());
            if (f.getName() == "" || f.getName().isEmpty()) {
                return;
            }
            Question question = new Question(
                    uri + "/" + f.getName(),
                    id++,
                    f.getName(),
                    category
            );

            if (question.getFilename().isEmpty() || question.getFilename() == null) {
                Log.d(TAG, "getFiles: Is empty");
            } else {
                offlineFiles.add(question);
            }
        }
        lastid = id;
        if (files.length > 0) {
            hideView(downloadTextFileProgress);
            displayNextQuestion(getNextFile());
        } else {
            showCustomSnackBar("No offline questions", false, null, -2);
            //Toast.makeText(this, "No offline questions ", Toast.LENGTH_SHORT).show();
            tvQuestion.setText("No offline questions");
            hideView(downloadTextFileProgress);
            buttonNextQuestion.setClickable(false);
            Log.d(TAG, "getFiles: Is empty");

        }
    }

    private File getNextFile() {
        Log.e(TAG, "getNextFile: SIZEEE::" + alreadyViewedQuestions.size());
        if (alreadyViewedQuestions.size() > 0) {
            //showCongratulationsMessage();
        }
        hideView(tvQuestion);
        File path = SelfReflectionActivity.this.getExternalFilesDir("Download/" + category);
        String uri = String.valueOf(path);
        File file = null;
        Random r = new Random();

        Log.d(TAG, "getNextFile: offline -> " + offlineFiles.size());
        int i1 = r.nextInt(offlineFiles.size());
        String[] miami = read_files.trim().split(" ");
        int miami1 = 1;
        while (read_files.contains(Integer.toString(i1)) && miami.length != miami1) {
            r = new Random();
            i1 = r.nextInt(offlineFiles.size());
            miami1++;
        }
        Log.e(TAG, "getNextFile: rand no : " + i1);
        alreadyViewedQuestions.add(i1);
        read_files = read_files + " " + i1;
        int cc = 0, ff = 0;
        for (Question s : offlineFiles) {
            if (s.getId() == i1) {
                uri = s.getFileUrl();
                Log.e(TAG, "getNextFile: URI: " + uri);
                file = new File(uri);
            } else {
                Log.e(TAG, "getNextFile: not me: " + uri);
                cc++;
            }
            ff++;
        }
        showView(tvQuestion);
        return file;

    }

    private void showCongratulationsMessage() {
        startActivity(new Intent(SelfReflectionActivity.this, CongratulationsActivity.class));
        finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    private boolean isAvailableOffline(Question file) {
        File path = SelfReflectionActivity.this.getExternalFilesDir("Download/" + file.getCategory() + "/");
        File mFile = new File(path, file.getFilename());
        //if (mFile.exists())
        //file.setFileUrl(String.valueOf(mFile));
        Log.d(TAG, "isAvailableOffline: Offline file found: " + mFile.exists());
        return mFile.exists();
    }

    private void retrieveQuestionList() {
        showView(downloadTextFileProgress);
        ApiService service = AppUtils.getApiService();
        service.getQuestionFileList("self_reflection").enqueue(new Callback<QuestionFileResponse>() {
            @Override
            public void onResponse(Call<QuestionFileResponse> call, Response<QuestionFileResponse> response) {
                hideView(downloadTextFileProgress);
                if (response.body().getError()) {
                    Toast.makeText(SelfReflectionActivity.this, "Error: " + response.body().getMessage(), Toast.LENGTH_SHORT).show();
                }
                //Toast.makeText(SelfReflectionActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                try {
                    Log.e(TAG, "onResponse: " + response.body().getQuestions());
                    questionArrayList.addAll(response.body().getQuestions());
                    Log.d(TAG, "onResponse: SIZE:: " + questionArrayList.size());
                    for (int C = 0; C < questionArrayList.size(); C++) {
                        if (!isAvailableOffline(questionArrayList.get(C))) {
                            downloadQuestion(questionArrayList.get(C));
                            Log.e(TAG, "onResponse: online");
                        } else {
                            Log.e(TAG, "onResponse: offline");
                        }
                    }
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            getFiles();
                        }
                    }, 1900);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<QuestionFileResponse> call, Throwable t) {
                hideView(downloadTextFileProgress);
                showCustomSnackBar("Failed to get data. Possibly due to network error", true, "Retry", -2);
                Log.d(TAG, "onFailure: " + t.getMessage());
            }
        });
    }

    private void downloadQuestion(Question q) {
        showView(downloadTextFileProgress);
        question = q;
        Log.d(TAG, "downloadQuestion: ::" + question.toString());
        showView(downloadTextFileProgress);
        String url = "data/" + q.getCategory() + "/" + q.getFilename();
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
                                    isDownloaded = writeResponseBodyToDisk(response.body(), q.getFilename());//Write downloaded file to disk
                                    Log.d(TAG, "doInBackground: 142 " + isDownloaded);

                                } else {
                                    Log.d(TAG, "doInBackground: Downloaded!");
                                }
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void aVoid) {
                                hideView(downloadTextFileProgress);
                                if (isDownloaded)
                                    Log.d(TAG, "onPostExecute: Downloaded: " + q.getFilename());
                                else {
                                    Log.d(TAG, "onPostExecute: Error: line 170");
                                }
                            }
                        }.execute();
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.d(TAG, "onFailure: " + call.toString());
                        hideView(downloadTextFileProgress);
                        Toast.makeText(SelfReflectionActivity.this, "Failed to get file, reason: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean writeResponseBodyToDisk(ResponseBody body, String aa) {

        try {
            File path = SelfReflectionActivity.this.getExternalFilesDir("Download/" + category + "/");

            InputStream inputStream = null;
            OutputStream outputStream = null;
            questionFile = new File(path, aa);

            if (questionFile.exists()) {
                isDownloaded = true;
                return true;
            }

            try {
                byte[] fileReader = new byte[4096];

                long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;

                inputStream = body.byteStream();
                outputStream = new FileOutputStream(questionFile);

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
            Log.d(TAG, "writeResponseBodyToDisk: 248 " + e.getLocalizedMessage());
            e.printStackTrace();

            return false;
        }
    }

    private void showCustomSnackBar(String message, boolean hasAction,
                                    @Nullable String actionText, int LENGTH) {
        Snackbar snackbar = Snackbar.make(tvQuestion, message, LENGTH);
        if (hasAction) {
            snackbar.setAction(actionText, v -> retrieveQuestionList());
        }
        snackbar.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            alreadyViewedQuestions.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
