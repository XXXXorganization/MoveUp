package com.zjgsu.moveup;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class Find extends AppCompatActivity {

    // 🌟 核心修改点：暴露 BASE_URL，方便测试框架拦截和动态修改
    public static String BASE_URL = "http://10.0.2.2:3000/v1";

    private RecyclerView rvFindClubs;
    private ClubAdapter adapter;
    private List<Club> clubList;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_find);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mainHandler = new Handler(Looper.getMainLooper());
        clubList = new ArrayList<>();

        rvFindClubs = findViewById(R.id.rvFindClubs);
        rvFindClubs.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ClubAdapter(clubList, R.layout.item_find_club);
        rvFindClubs.setAdapter(adapter);

        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    String keyword = etSearch.getText().toString().trim();
                    fetchClubs(keyword);
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        fetchClubs("");
    }

    private void fetchClubs(String keyword) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                // 🌟 修改点：使用 BASE_URL 动态拼接地址
                String urlString = BASE_URL + "/clubs";
                if (!keyword.isEmpty()) {
                    urlString += "?q=" + URLEncoder.encode(keyword, "UTF-8");
                }

                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);

                if (connection.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    JSONObject resp = new JSONObject(sb.toString());
                    if (resp.optInt("code") == 200) {
                        JSONObject data = resp.getJSONObject("data");
                        JSONArray listArray = data.optJSONArray("list");

                        List<Club> tempNewList = new ArrayList<>();
                        if (listArray != null) {
                            for (int i = 0; i < listArray.length(); i++) {
                                JSONObject obj = listArray.getJSONObject(i);

                                // 提取后端的 ID
                                String id = obj.optString("id", "");
                                String name = obj.optString("name");
                                String location = obj.optString("location");
                                String flag = obj.optString("flag", "🇨🇳");

                                // 传入构造函数中
                                tempNewList.add(new Club(id, name, location, R.drawable.moveup, flag));
                            }
                        }

                        mainHandler.post(() -> {
                            clubList.clear();
                            clubList.addAll(tempNewList);
                            adapter.notifyDataSetChanged();
                            if (clubList.isEmpty()) Toast.makeText(Find.this, "未找到相关的跑团", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            } catch (Exception e) {
                mainHandler.post(() -> Toast.makeText(Find.this, "网络异常", Toast.LENGTH_SHORT).show());
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }
}