package com.zjgsu.moveup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
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

    public static String BASE_URL = "http://10.234.4.72:3500/v1";

    private RecyclerView rvFindClubs;
    private ClubAdapter adapter;
    private List<Club> clubList;
    private Handler mainHandler;

    // 🌟 新增：抽屉布局引用
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_find);

        // 🌟 初始化 DrawerLayout
        drawerLayout = findViewById(R.id.drawerLayout);

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
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String keyword = etSearch.getText().toString().trim();
                fetchClubs(keyword);
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                return true;
            }
            return false;
        });

        // 🌟 绑定左上角菜单按钮点击事件：从左侧划出菜单
        findViewById(R.id.btnMenu).setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        // 🌟 初始化并绑定侧滑菜单内部的跳转点击事件
        setupMenuClicks();

        fetchClubs("");
    }

    /**
     * 🌟 侧滑菜单跳转逻辑配置
     */
    private void setupMenuClicks() {
        TextView menuHome = findViewById(R.id.menu_home);
        TextView menuHistory = findViewById(R.id.menu_history);
        TextView menuPlan = findViewById(R.id.menu_plan);
        TextView menuClub = findViewById(R.id.menu_club);
        TextView menuProfile = findViewById(R.id.menu_profile);

        if (menuHome != null) menuHome.setOnClickListener(v -> {
            startActivity(new Intent(Find.this, Main.class));
            finish();
        });

        if (menuHistory != null) menuHistory.setOnClickListener(v -> {
            startActivity(new Intent(Find.this, History.class));
            finish();
        });

        if (menuPlan != null) menuPlan.setOnClickListener(v -> {
            startActivity(new Intent(Find.this, Plan.class));
            finish();
        });

        // 当前已经在社团查找相关页面，但如果是跳到详情也可以保留
        if (menuClub != null) menuClub.setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.closeDrawers();
            }
        });

        if (menuProfile != null) menuProfile.setOnClickListener(v -> {
            startActivity(new Intent(Find.this, Mine.class));
            finish();
        });
    }

    private void fetchClubs(String keyword) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
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

                                String id = obj.optString("id", "");
                                String name = obj.optString("name");
                                String location = obj.optString("location");
                                String flag = obj.optString("flag", "🇨🇳");
                                String imageUrl = obj.optString("image_url", "");

                                tempNewList.add(new Club(id, name, location, imageUrl, flag));
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