package com.zjgsu.moveup;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

public class clubterm extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_clubterm);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        RecyclerView recycler = findViewById(R.id.recyclerPosts);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(new ClubTermPostAdapter(buildSamplePosts()));
    }

    @NonNull
    private List<ClubTermPost> buildSamplePosts() {
        List<ClubTermPost> list = new ArrayList<>();

        list.add(new ClubTermPost(
                "Anonim",
                "Today at 23:43 AM",
                "Late Post",
                R.drawable.fengmian,
                "3",
                "Morning Walk",
                "6.00 Km • 30m00s",
                "27 grove likes",
                R.drawable.ic_avatar_placeholder));

        list.add(new ClubTermPost(
                "Anonim",
                "Today at 23:43 AM",
                "Late Post",
                R.drawable.fengmian,
                "3",
                "Morning Walk",
                "5.00 Km • 5:00 Km • 30m00s",
                "27 grove likes",
                R.drawable.ic_avatar_placeholder));

        return list;
    }
}