package com.traumtool.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.traumtool.R;
import com.traumtool.adapters.DreamAdapter;
import com.traumtool.interfaces.ApiService;
import com.traumtool.models.Dream;
import com.traumtool.models.DreamFileResponse;
import com.traumtool.utils.AppUtils;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DreamActivity extends AppCompatActivity {

    private ArrayList<Dream> dreamArrayList = new ArrayList<>();
    private static final String TAG = "DreamActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dream);

        retrieveBooks();
    }

    private void retrieveBooks() {
        ApiService service = AppUtils.getApiService();
        service.getDreamFileList("dreamtravel").enqueue(new Callback<DreamFileResponse>() {
            @Override
            public void onResponse(Call<DreamFileResponse> call, Response<DreamFileResponse> response) {
                if (response.body().getError()) {
                    Toast.makeText(DreamActivity.this, "Error: " + response.body().getMessage(), Toast.LENGTH_SHORT).show();
                }
                Toast.makeText(DreamActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                try {
                    dreamArrayList.addAll(response.body().getDreams());

                    initializeStuff();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<DreamFileResponse> call, Throwable t) {
                Log.d(TAG, "onFailure: " + t.getMessage());
            }
        });
    }

    private void initializeStuff() {
        RecyclerView recyclerView = findViewById(R.id.recyclerViewDream);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(false);
        recyclerView.setAdapter(new DreamAdapter(this, dreamArrayList));

    }
}
