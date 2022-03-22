package com.example.karo.ui.invitations;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.karo.HomeActivity;
import com.example.karo.R;
import com.example.karo.RoomActivity;
import com.example.karo.adapter.InvitationsAdapter;
import com.example.karo.adapter.RoomsAdapter;
import com.example.karo.model.Room;
import com.example.karo.model.RoomDetail;
import com.example.karo.model.User;
import com.example.karo.utility.CommonLogic;
import com.example.karo.utility.Const;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;

import java.util.Map;
import java.util.Objects;

public class InvitationsFragment extends Fragment implements RoomsAdapter.ISendStateToRoom {

    private User currentUser;
    private ListenerRegistration listenerRegistration;
    private InvitationsAdapter invitationsAdapter;
    private RecyclerView rcvNotification;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_invitations, container, false);
        loadRooms();
        // get recycler view
        rcvNotification = root.findViewById(R.id.rcvInvitations);
        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        HomeActivity homeActivity = (HomeActivity) getActivity();
        assert homeActivity != null;
        currentUser = homeActivity.getCurrentUser();

        // set up adapter for recycler view
        invitationsAdapter = new InvitationsAdapter(currentUser,this);
        rcvNotification.setAdapter(invitationsAdapter);
        rcvNotification.setLayoutManager(new LinearLayoutManager(homeActivity));
    }

    private void loadRooms() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Query query = db.collection(Const.COLLECTION_ROOMS);
        listenerRegistration = query.addSnapshotListener((value, error) -> {
            if (error != null) {
                CommonLogic.makeToast(getContext(), error.getMessage());
                return;
            }
            // Get room list
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                RoomDetail roomDetail = new RoomDetail();
                DocumentSnapshot document = documentChange.getDocument();
                // get state change data
                int stateChangeData = -2;
                switch (documentChange.getType()) {
                    case ADDED:
                        stateChangeData = Const.ADAPTER_STATE_INSERTED_DATA;
                        break;
                    case MODIFIED:
                        stateChangeData = Const.ADAPTER_STATE_CHANGED_DATA;
                        break;
                    case REMOVED:
                        stateChangeData = Const.ADAPTER_STATE_REMOVED_DATA;
                        break;
                }

                // get room
                Map<String, Object> map = document.getData();
                String playerRoleXEmail = null;
                String playerRoleOEmail = null;
                if (map.get(Const.KEY_PLAYER_ROLE_X_EMAIL) != null) {
                    playerRoleXEmail = map.get(Const.KEY_PLAYER_ROLE_X_EMAIL).toString();
                }
                if (map.get(Const.KEY_PLAYER_ROLE_O_EMAIL) != null) {
                    playerRoleOEmail = map.get(Const.KEY_PLAYER_ROLE_O_EMAIL).toString();
                }
                Room room = new Room(
                        playerRoleXEmail
                        , playerRoleOEmail
                        , Integer.parseInt(Objects.requireNonNull(map.get(Const.KEY_PLAYER_ROLE_X_STATE)).toString())
                        , Integer.parseInt(Objects.requireNonNull(map.get(Const.KEY_PLAYER_ROLE_O_STATE)).toString())
                );
                roomDetail.setRoom(room);
                roomDetail.setRoomDocument(document.getId());
                loadUserRoleXInfo(roomDetail, stateChangeData);
            }
        });
    }

    private void loadUserRoleXInfo(RoomDetail roomDetail, int stateChangeData) {
        Room room = roomDetail.getRoom();
        if (room.getPlayerRoleXState() == Const.PLAYER_STATE_NONE) {
            roomDetail.setUserRoleX(null);
            loadUserRoleOInfo(roomDetail, stateChangeData);
        } else {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection(Const.COLLECTION_USERS)
                    .whereEqualTo(Const.KEY_EMAIL, room.getPlayerRoleXEmail())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            if (querySnapshot != null && querySnapshot.size() > 0) {
                                Map<String, Object> map = querySnapshot.getDocuments().get(0).getData();
                                if (map != null) {
                                    // get data
                                    User user = new User(
                                            Objects.requireNonNull(map.get(Const.KEY_EMAIL)).toString()
                                            , Objects.requireNonNull(map.get(Const.KEY_PASSWORD)).toString()
                                            , Objects.requireNonNull(map.get(Const.KEY_USERNAME)).toString()
                                            , Objects.requireNonNull(map.get(Const.KEY_AVATAR_REF)).toString()
                                            , Integer.parseInt(Objects.requireNonNull(map.get(Const.KEY_SCORE)).toString())
                                    );

                                    // get bitmap avatar
                                    Bitmap bitmap = CommonLogic.loadImageFromInternalStorage(
                                            Const.AVATARS_SOURCE_INTERNAL_PATH + user.getAvatarRef());
                                    user.setAvatarBitmap(bitmap);
                                    roomDetail.setUserRoleX(user);
                                    loadUserRoleOInfo(roomDetail, stateChangeData);
                                }
                            } else {
                                CommonLogic.makeToast(getContext(), "Get user " + room.getPlayerRoleXEmail() + " fail!");
                            }
                        } else {
                            CommonLogic.makeToast(getContext(), "Error: " + task.getException());
                        }
                    });
        }
    }

    private void loadUserRoleOInfo(RoomDetail roomDetail, int stateChangeData) {
        if (roomDetail.getRoom().getPlayerRoleOState() == Const.PLAYER_STATE_NONE
                && (roomDetail.getUserRoleO().getEmail() == currentUser.getEmail()
                || roomDetail.getUserRoleX().getEmail() == currentUser.getEmail())) {
            invitationsAdapter.setData(roomDetail, stateChangeData);
        } else {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection(Const.COLLECTION_USERS)
                    .whereEqualTo(Const.KEY_EMAIL, roomDetail.getRoom().getPlayerRoleOEmail())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            if (querySnapshot != null && querySnapshot.size() > 0) {
                                Map<String, Object> map = querySnapshot.getDocuments().get(0).getData();
                                if (map != null) {
                                    // get data
                                    User user = new User(
                                            Objects.requireNonNull(map.get(Const.KEY_EMAIL)).toString()
                                            , Objects.requireNonNull(map.get(Const.KEY_PASSWORD)).toString()
                                            , Objects.requireNonNull(map.get(Const.KEY_USERNAME)).toString()
                                            , Objects.requireNonNull(map.get(Const.KEY_AVATAR_REF)).toString()
                                            , Integer.parseInt(Objects.requireNonNull(map.get(Const.KEY_SCORE)).toString())
                                    );

                                    // get bitmap avatar
                                    Bitmap bitmap = CommonLogic.loadImageFromInternalStorage(
                                            Const.AVATARS_SOURCE_INTERNAL_PATH + user.getAvatarRef());
                                    user.setAvatarBitmap(bitmap);
                                    roomDetail.setUserRoleO(user);
                                    invitationsAdapter.setData(roomDetail, stateChangeData);
                                }
                            } else {
                                CommonLogic.makeToast(getContext(), "Get user " + roomDetail.getRoom().getPlayerRoleXEmail() + " fail!");
                            }
                        } else {
                            CommonLogic.makeToast(getContext(), "Error: " + task.getException());
                        }
                    });
        }
    }

    @Override
    public void sendStateToRoom(String roomDocument, String role, int state, String email) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference roomRef = db.collection(Const.COLLECTION_ROOMS).document(roomDocument);
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            // send state
            if (role.equals(Const.TOKEN_X)) {
                transaction.update(roomRef, Const.KEY_PLAYER_ROLE_X_EMAIL, email);
                transaction.update(roomRef, Const.KEY_PLAYER_ROLE_X_STATE, state);
            } else {
                transaction.update(roomRef, Const.KEY_PLAYER_ROLE_O_EMAIL, email);
                transaction.update(roomRef, Const.KEY_PLAYER_ROLE_O_STATE, state);
            }
            return null;
        }).addOnSuccessListener(aVoid -> {
            // join room
            if (state == Const.PLAYER_STATE_JOIN_ROOM) {
                Intent intent = new Intent(getContext(), RoomActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString(Const.KEY_ROOM_DOCUMENT, roomDocument);
                intent.putExtras(bundle);
                getContext().startActivity(intent);
            }
        }).addOnFailureListener(e -> CommonLogic.makeToast(getContext(), "Send state failure: " + e.getMessage()));
    }

    @Override
    public void onDetach() {
        listenerRegistration.remove();
        super.onDetach();
    }
}