package com.zjgsu.moveup;

import android.content.Intent;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;

public class History extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        RecyclerView list = findViewById(R.id.recyclerHistory);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(
                new HistoryAdapter(
                        buildSampleRuns(),
                        (run, position) -> shareRun(run)));

        findViewById(R.id.btnMenu)
                .setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    @NonNull
    private List<HistoryRun> buildSampleRuns() {
        String pace = getString(R.string.activity_pace_value);
        String dist = getString(R.string.activity_distance);
        return Arrays.asList(
                new HistoryRun(
                        getString(R.string.activity_date_1),
                        getString(R.string.activity_title_1),
                        getString(R.string.activity_time_value),
                        pace,
                        dist),
                new HistoryRun(
                        "14/7/2024",
                        "Evening Jog",
                        "22.30",
                        pace,
                        "5,20 Km"),
                new HistoryRun(
                        "10/7/2024",
                        "Park Loop",
                        "18.00",
                        "5\'15\"",
                        "2,50 Km"));
    }

    private void shareRun(@NonNull HistoryRun run) {
        String text =
                run.title
                        + " — "
                        + run.distanceValue
                        + ", "
                        + getString(R.string.label_time)
                        + " "
                        + run.timeValue;
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(send, getString(R.string.content_desc_share)));
    }
}
