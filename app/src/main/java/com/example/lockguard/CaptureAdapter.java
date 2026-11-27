package com.example.lockguard;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class CaptureAdapter extends RecyclerView.Adapter<CaptureAdapter.ViewHolder> {

    public interface OnCaptureActionListener {
        void onShare(CaptureItem item);
        void onDelete(CaptureItem item, int position);
    }

    private final List<CaptureItem> items;
    private final OnCaptureActionListener listener;

    public CaptureAdapter(List<CaptureItem> items, OnCaptureActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_capture, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CaptureItem item = items.get(position);
        holder.tvReason.setText(item.title);
        holder.tvDateTime.setText(item.dateTimeFormatted);

        Bitmap bitmap = decodeSampledBitmap(item.file.getAbsolutePath(), 720, 480);
        if (bitmap != null) {
            holder.ivPhoto.setImageBitmap(bitmap);
        } else {
            holder.ivPhoto.setImageResource(android.R.drawable.ic_menu_camera);
        }

        holder.btnShare.setOnClickListener(v -> {
            if (listener != null) {
                listener.onShare(item);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                listener.onDelete(items.get(pos), pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static Bitmap decodeSampledBitmap(String path, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        int inSampleSize = 1;
        if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
            final int halfHeight = options.outHeight / 2;
            final int halfWidth = options.outWidth / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        options.inJustDecodeBounds = false;
        options.inSampleSize = inSampleSize;
        return BitmapFactory.decodeFile(path, options);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivPhoto;
        final TextView tvReason;
        final TextView tvDateTime;
        final MaterialButton btnShare;
        final MaterialButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivCapturePhoto);
            tvReason = itemView.findViewById(R.id.tvCaptureReason);
            tvDateTime = itemView.findViewById(R.id.tvCaptureDateTime);
            btnShare = itemView.findViewById(R.id.btnShareCapture);
            btnDelete = itemView.findViewById(R.id.btnDeleteCapture);
        }
    }
}
