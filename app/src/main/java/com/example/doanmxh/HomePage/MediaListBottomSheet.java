package com.example.doanmxh.HomePage;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doanmxh.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;

public class MediaListBottomSheet {

    public static void show(Context context, List<MediaItem> mediaList) {

        BottomSheetDialog dialog =
                new BottomSheetDialog(context);

        View view = LayoutInflater.from(context)
                .inflate(R.layout.bottom_sheet_media_list,
                        null,
                        false);

        RecyclerView recyclerView =
                view.findViewById(R.id.recyclerMedia);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(context));

        recyclerView.setAdapter(
                new MediaListAdapter(context, mediaList));

        dialog.setContentView(view);
        dialog.show();
    }
}