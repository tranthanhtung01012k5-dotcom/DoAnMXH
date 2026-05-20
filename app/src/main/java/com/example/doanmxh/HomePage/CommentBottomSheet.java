//package com.example.doanmxh.HomePage;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.EditText;
//import android.widget.ImageButton;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.example.doanmxh.ProfilePage.UserProfileActivity;
//import com.example.doanmxh.R;
//import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.firestore.FieldValue;
//import com.google.firebase.firestore.FirebaseFirestore;
//import com.google.firebase.firestore.Query;
//
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class CommentBottomSheet extends BottomSheetDialogFragment {
//
//    private static final String ARG_POST_ID = "post_id";
//
//    private String postId;
//
//    private RecyclerView rvComments;
//    private EditText edtComment;
//    private ImageButton btnSend;
//
//    private CommentAdapter adapter;
//
//    private final List<CommentModel> commentList = new ArrayList<>();
//
//    private FirebaseFirestore db;
//
//    // ── replyToCommentId để hỗ trợ reply ──
//    private String replyToCommentId = null;
//
//    public static CommentBottomSheet newInstance(String postId) {
//        CommentBottomSheet sheet = new CommentBottomSheet();
//        Bundle args = new Bundle();
//        args.putString(ARG_POST_ID, postId);
//        sheet.setArguments(args);
//        return sheet;
//    }
//
//    @Override
//    public void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        if (getArguments() != null) {
//            postId = getArguments().getString(ARG_POST_ID);
//        }
//        db = FirebaseFirestore.getInstance();
//    }
//
//    @Nullable
//    @Override
//    public View onCreateView(
//            @NonNull LayoutInflater inflater,
//            @Nullable ViewGroup container,
//            @Nullable Bundle savedInstanceState
//    ) {
//        View view = inflater.inflate(R.layout.bottom_sheet_comment, container, false);
//
//        rvComments = view.findViewById(R.id.rvComments);
//        edtComment = view.findViewById(R.id.edtComment);
//        btnSend = view.findViewById(R.id.btnSend);
//
//        // ── Adapter ──
//        adapter = new CommentAdapter(
//                commentList,
//                postId,
//                new CommentAdapter.OnCommentActionListener() {
//
//                    @Override
//                    public void onReplyClick(CommentModel comment, int position) {
//                        edtComment.setText("@" + comment.getHoVaTen() + " ");
//                        edtComment.setSelection(edtComment.getText().length());
//                        edtComment.requestFocus();
//                        replyToCommentId = comment.getDocumentId();
//                    }
//
//                    @Override
//                    public void onLikeClick(CommentModel comment, int position) {
//                        Toast.makeText(getContext(), "❤️", Toast.LENGTH_SHORT).show();
//                    }
//
//                    @Override
//                    public void onAvatarClick(CommentModel comment, int position) {
//                        if (getContext() == null) return;
//                        Intent intent = new Intent(getContext(), UserProfileActivity.class);
//                        intent.putExtra("user_id", comment.getNguoiDungId());
//                        startActivity(intent);
//                    }
//
//                    @Override
//                    public void onAddFriendClick(CommentModel comment, int position) {
//                        if (getContext() == null) return;
//
//                        String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
//                                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
//
//                        if (currentUid == null) {
//                            Toast.makeText(getContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
//                            return;
//                        }
//
//                        String authorUid = comment.getNguoiDungId();
//                        if (authorUid == null || authorUid.equals(currentUid)) return;
//
//                        String docId = currentUid + "_" + authorUid;
//
//                        Map<String, Object> followData = new HashMap<>();
//                        followData.put("nguoi_theo_doi_id", currentUid);
//                        followData.put("nguoi_duoc_theo_doi_id", authorUid);
//                        followData.put("ngay_theo_doi", new Date());
//
//                        db.collection("theo_doi").document(docId)
//                                .set(followData)
//                                .addOnSuccessListener(unused -> {
//                                    if (getContext() == null) return;
//
//                                    // Tăng số người theo dõi của tác giả
//                                    db.collection("nguoi_dung")
//                                            .document(authorUid)
//                                            .update("so_nguoi_theo_doi", FieldValue.increment(1));
//
//                                    Map<String, Object> followerEntry = new HashMap<>();
//                                    followerEntry.put("nguoi_dung_id", currentUid);
//                                    followerEntry.put("ngay_theo_doi", new Date());
//                                    db.collection("nguoi_dung")
//                                            .document(authorUid)
//                                            .collection("nguoi_theo_doi")
//                                            .document(currentUid)
//                                            .set(followerEntry);
//
//                                    // Tăng số người đang theo dõi của người dùng hiện tại
//                                    db.collection("nguoi_dung")
//                                            .document(currentUid)
//                                            .update("so_nguoi_dang_theo_doi", FieldValue.increment(1));
//
//                                    Map<String, Object> followingEntry = new HashMap<>();
//                                    followingEntry.put("nguoi_dung_id", authorUid);
//                                    followingEntry.put("ngay_theo_doi", new Date());
//                                    db.collection("nguoi_dung")
//                                            .document(currentUid)
//                                            .collection("nguoi_dang_theo_doi")
//                                            .document(authorUid)
//                                            .set(followingEntry);
//
//                                    comment.setFollowing(true);
//                                    adapter.notifyItemChanged(position);
//                                    Toast.makeText(getContext(),
//                                            "Đã theo dõi @" + comment.getHoVaTen(),
//                                            Toast.LENGTH_SHORT).show();
//                                })
//                                .addOnFailureListener(e -> {
//                                    if (getContext() == null) return;
//                                    Toast.makeText(getContext(),
//                                            "Lỗi: " + e.getMessage(),
//                                            Toast.LENGTH_SHORT).show();
//                                });
//                    }
//                }
//        );
//
//        rvComments.setLayoutManager(new LinearLayoutManager(getContext()));
//        rvComments.setAdapter(adapter);
//
//        loadComments();
//
//        btnSend.setOnClickListener(v -> sendComment());
//
//        return view;
//    }
//
//    private void loadComments() {
//        String myUid = FirebaseAuth.getInstance().getCurrentUser() != null
//                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
//
//        db.collection("bai_viet")
//                .document(postId)
//                .collection("binh_luan")
//                .orderBy("ngay_tao", Query.Direction.ASCENDING)
//                .addSnapshotListener((snapshots, error) -> {
//                    if (error != null || snapshots == null) return;
//
//                    List<CommentModel> tempList = new ArrayList<>();
//
//                    for (com.google.firebase.firestore.DocumentSnapshot doc
//                            : snapshots.getDocuments()) {
//
//                        CommentModel comment = new CommentModel();
//                        comment.setDocumentId(doc.getId());
//                        comment.setNoiDung(doc.getString("noi_dung"));
//                        comment.setNguoiDungId(doc.getString("nguoi_dung_id"));
//                        comment.setHoVaTen(doc.getString("ho_va_ten"));
//                        comment.setAnhDaiDien(doc.getString("anh_dai_dien"));
//                        comment.setBinhLuanChaId(doc.getString("binh_luan_cha_id"));
//
//                        Boolean verified = doc.getBoolean("verified");
//                        comment.setVerified(verified != null && verified);
//
//                        Long soLikeLong = doc.getLong("so_like");
//                        comment.setSoLike(soLikeLong != null ? soLikeLong.intValue() : 0);
//
//                        if (doc.getTimestamp("ngay_tao") != null) {
//                            comment.setNgayTao(doc.getTimestamp("ngay_tao"));
//                        }
//
//                        comment.setLikedByMe(false);
//                        comment.setFollowing(false);
//
//                        tempList.add(comment);
//                    }
//
//                    // Nếu chưa đăng nhập, hiển thị luôn không cần check like/follow
//                    if (myUid == null) {
//                        commentList.clear();
//                        commentList.addAll(tempList);
//                        adapter.notifyDataSetChanged();
//                        return;
//                    }
//
//                    // Đã đăng nhập: check like + follow cho từng comment
//                    for (CommentModel comment : tempList) {
//                        db.collection("bai_viet")
//                                .document(postId)
//                                .collection("binh_luan")
//                                .document(comment.getDocumentId())
//                                .collection("luot_thich")
//                                .document(myUid)
//                                .get()
//                                .addOnSuccessListener(likeDoc -> {
//                                    comment.setLikedByMe(likeDoc.exists());
//
//                                    String followDocId = myUid + "_" + comment.getNguoiDungId();
//                                    db.collection("theo_doi")
//                                            .document(followDocId)
//                                            .get()
//                                            .addOnSuccessListener(followDoc -> {
//                                                comment.setFollowing(followDoc.exists());
//
//                                                // Thêm vào list nếu chưa có
//                                                boolean exists = false;
//                                                for (CommentModel c : commentList) {
//                                                    if (c.getDocumentId().equals(comment.getDocumentId())) {
//                                                        exists = true;
//                                                        break;
//                                                    }
//                                                }
//                                                if (!exists) {
//                                                    commentList.add(comment);
//                                                }
//
//                                                // Sắp xếp theo thời gian
//                                                commentList.sort((c1, c2) -> {
//                                                    if (c1.getNgayTao() == null || c2.getNgayTao() == null) return 0;
//                                                    return c1.getNgayTao().compareTo(c2.getNgayTao());
//                                                });
//
//                                                adapter.notifyDataSetChanged();
//                                            });
//                                });
//                    }
//                });
//    }
//
//    private void sendComment() {
//        String noiDung = edtComment.getText().toString().trim();
//        if (noiDung.isEmpty()) return;
//
//        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
//                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
//
//        if (uid == null) {
//            Toast.makeText(getContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        db.collection("nguoi_dung").document(uid).get()
//                .addOnSuccessListener(userDoc -> {
//                    String hoVaTen = userDoc.exists() ? userDoc.getString("ho_va_ten") : "Ẩn danh";
//                    String anhDaiDien = userDoc.exists() ? userDoc.getString("anh_dai_dien") : "";
//                    Boolean verified = userDoc.getBoolean("verified");
//
//                    Map<String, Object> comment = new HashMap<>();
//                    comment.put("nguoi_dung_id", uid);
//                    comment.put("ho_va_ten", hoVaTen);
//                    comment.put("anh_dai_dien", anhDaiDien);
//                    comment.put("verified", verified != null && verified);
//                    comment.put("noi_dung", noiDung);
//                    comment.put("ngay_tao", new Date());
//                    comment.put("binh_luan_cha_id",
//                            replyToCommentId != null ? replyToCommentId : "");
//                    comment.put("so_like", 0);
//
//                    db.collection("bai_viet")
//                            .document(postId)
//                            .collection("binh_luan")
//                            .add(comment)
//                            .addOnSuccessListener(ref -> {
//                                edtComment.setText("");
//                                replyToCommentId = null;
//
//                                db.collection("bai_viet").document(postId)
//                                        .update("so_binh_luan", FieldValue.increment(1));
//                            });
//                });
//    }
//}