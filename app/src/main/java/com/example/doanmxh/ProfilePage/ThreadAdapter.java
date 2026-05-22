//package com.example.doanmxh.ProfilePage;
//
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.example.doanmxh.R;
//
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.List;
//import java.util.Locale;
//import java.util.Map;
//
//public class ThreadAdapter extends RecyclerView.Adapter<ThreadAdapter.ThreadViewHolder> {
//
//    private List<Map<String, Object>> threadList;
//
//    public ThreadAdapter(List<Map<String, Object>> threadList) {
//        this.threadList = threadList;
//    }
//
//    @NonNull
//    @Override
//    public ThreadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(parent.getContext())
//                .inflate(R.layout.item_thread, parent, false);
//        return new ThreadViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull ThreadViewHolder holder, int position) {
//        Map<String, Object> thread = threadList.get(position);
//
//        String content = (String) thread.get("noi_dung");
//        holder.txtContent.setText(content != null ? content : "");
//
//        // Hiển thị thời gian nếu có
//        Object timestamp = thread.get("ngay_tao");
//        if (timestamp instanceof com.google.firebase.Timestamp) {
//            Date date = ((com.google.firebase.Timestamp) timestamp).toDate();
//            String formatted = new SimpleDateFormat(
//                    "dd/MM/yyyy HH:mm", Locale.getDefault()
//            ).format(date);
//            holder.txtTime.setText(formatted);
//        } else {
//            holder.txtTime.setText("");
//        }
//    }
//
//    @Override
//    public int getItemCount() {
//        return threadList.size();
//    }
//
//    static class ThreadViewHolder extends RecyclerView.ViewHolder {
//        TextView txtContent, txtTime;
//
//        ThreadViewHolder(@NonNull View itemView) {
//            super(itemView);
//            txtContent = itemView.findViewById(R.id.txtThreadContent);
//            txtTime = itemView.findViewById(R.id.txtThreadTime);
//        }
//    }
//}