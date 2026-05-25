package com.example.doanmxh.HomePage;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doanmxh.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Bottom Sheet hiển thị danh sách ảnh dạng dọc (1 ảnh/dòng).
 * Click vào ảnh → FullScreenImageActivity.
 *
 * Mở bằng:
 *   ImageListBottomSheet.show(context, imageUrls);
 */
public class ImageListBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_IMAGES = "arg_images";

    public static void show(Context ctx, List<String> images) {
        if (!(ctx instanceof androidx.fragment.app.FragmentActivity)) return;

        ImageListBottomSheet sheet = new ImageListBottomSheet();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_IMAGES, new ArrayList<>(images));
        sheet.setArguments(args);
        sheet.show(
                ((androidx.fragment.app.FragmentActivity) ctx).getSupportFragmentManager(),
                "ImageListBottomSheet"
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_image_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        List<String> images = getArguments() != null
                ? getArguments().getStringArrayList(ARG_IMAGES)
                : new ArrayList<>();

        RecyclerView rv = view.findViewById(R.id.rvImageList);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(new ImageListAdapter(images, (url, index) -> {
            dismiss();
            FullScreenImageActivity.open(requireContext(), images, index);
        }));
    }

    // ── Inner Adapter ──────────────────────────────────────────
    static class ImageListAdapter extends RecyclerView.Adapter<ImageListAdapter.VH> {

        interface OnImageClick {
            void onClick(String url, int index);
        }

        private final List<String> urls;
        private final OnImageClick listener;

        ImageListAdapter(List<String> urls, OnImageClick listener) {
            this.urls     = urls;
            this.listener = listener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_image_list, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Glide.with(holder.imageView.getContext())
                    .load(urls.get(position))
                    .centerCrop()
                    .into(holder.imageView);

            holder.imageView.setOnClickListener(v ->
                    listener.onClick(urls.get(position), position));
        }

        @Override
        public int getItemCount() { return urls != null ? urls.size() : 0; }

        static class VH extends RecyclerView.ViewHolder {
            ImageView imageView;
            VH(@NonNull View v) {
                super(v);
                imageView = v.findViewById(R.id.ivListImage);
            }
        }
    }
}