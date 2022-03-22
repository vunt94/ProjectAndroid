package com.example.karo.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.karo.R;
import com.example.karo.model.Room;
import com.example.karo.model.RoomDetail;
import com.example.karo.model.User;
import com.example.karo.ui.invitations.InvitationsFragment;
import com.example.karo.utility.Const;

import java.util.ArrayList;

public class InvitationsAdapter extends RecyclerView.Adapter<InvitationsAdapter.ViewHolder> {

    private ArrayList<RoomDetail> rooms;
    private User currentUser;
    private RoomsAdapter.ISendStateToRoom iSendStateToRoom;
    private boolean isX = false;

    public InvitationsAdapter(User currentUser, InvitationsFragment invitationFragment) {
        this.rooms = new ArrayList<>();
        this.currentUser = currentUser;
        this.iSendStateToRoom = invitationFragment;
    }

    public void addData(RoomDetail roomDetail) {
        this.rooms.add(roomDetail);
        notifyItemInserted(rooms.size() - 1);
    }

    public void removeData(int position) {
        this.rooms.remove(position);
        notifyItemRemoved(position);
    }

    public void changeData(RoomDetail roomDetail, int position) {
        Room oldRoom = this.rooms.get(position).getRoom();
        Room newRoom = roomDetail.getRoom();
        if (oldRoom.getPlayerRoleXState() != newRoom.getPlayerRoleXState()
                && (oldRoom.getPlayerRoleXState() == Const.PLAYER_STATE_NONE
                || newRoom.getPlayerRoleXState() == Const.PLAYER_STATE_NONE)) {
            this.rooms.get(position).setRoomDocument(roomDetail.getRoomDocument());
            this.rooms.get(position).setRoom(roomDetail.getRoom());
            this.rooms.get(position).setUserRoleX(roomDetail.getUserRoleX());
            this.rooms.get(position).setUserRoleO(roomDetail.getUserRoleO());
            notifyItemChanged(position);
        } else if (oldRoom.getPlayerRoleOState() != newRoom.getPlayerRoleOState()
                && (oldRoom.getPlayerRoleOState() == Const.PLAYER_STATE_NONE
                || newRoom.getPlayerRoleOState() == Const.PLAYER_STATE_NONE)) {
            this.rooms.get(position).setRoomDocument(roomDetail.getRoomDocument());
            this.rooms.get(position).setRoom(roomDetail.getRoom());
            this.rooms.get(position).setUserRoleX(roomDetail.getUserRoleX());
            this.rooms.get(position).setUserRoleO(roomDetail.getUserRoleO());
            notifyItemChanged(position);
        }
    }

    public void setData(RoomDetail roomDetail, int state) {
        if (state == Const.ADAPTER_STATE_REMOVED_DATA) {
            int position = findPosition(roomDetail.getRoomDocument());
            removeData(position);
        } else if (state == Const.ADAPTER_STATE_INSERTED_DATA) {
            addData(roomDetail);
        } else if (state == Const.ADAPTER_STATE_CHANGED_DATA) {
            int position = findPosition(roomDetail.getRoomDocument());
            changeData(roomDetail, position);
        }
    }

    private int findPosition(String roomDocument) {
        int index = -1;
        for (RoomDetail roomDetail : rooms) {
            index++;
            if (roomDetail.getRoomDocument().equals(roomDocument)) {
                return index;
            }
        }
        return -1;
    }

    @NonNull
    @Override
    public InvitationsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_invitation, parent, false);
        return new InvitationsAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InvitationsAdapter.ViewHolder holder, int position) {
        RoomDetail roomDetail = rooms.get(position);
        Room room = roomDetail.getRoom();
        User userRoleX = roomDetail.getUserRoleX();
        User userRoleO = roomDetail.getUserRoleO();
        holder.roomDocument = roomDetail.getRoomDocument();
        if (userRoleX.getEmail() != null) {
            if (room.getPlayerRoleXState() == Const.PLAYER_STATE_NONE) {
                holder.imgPlayerRoleAvatar.setVisibility(View.GONE);
                holder.txtPlayerRoleName.setVisibility(View.GONE);
            } else {
                holder.imgPlayerRoleAvatar.setVisibility(View.VISIBLE);
                holder.txtPlayerRoleName.setVisibility(View.VISIBLE);
                if (userRoleX != null) {
                    holder.txtPlayerRoleName.setText(userRoleX.getUsername());
                    holder.imgPlayerRoleAvatar.setImageBitmap(userRoleX.getAvatarBitmap());
                }
            }
        }
        else {
            isX = true;
            if (room.getPlayerRoleOState() == Const.PLAYER_STATE_NONE) {
                holder.imgPlayerRoleAvatar.setVisibility(View.GONE);
                holder.txtPlayerRoleName.setVisibility(View.GONE);
            } else {
                holder.imgPlayerRoleAvatar.setVisibility(View.VISIBLE);
                holder.txtPlayerRoleName.setVisibility(View.VISIBLE);
                if (userRoleO != null) {
                    holder.txtPlayerRoleName.setText(userRoleO.getUsername());
                    holder.imgPlayerRoleAvatar.setImageBitmap(userRoleO.getAvatarBitmap());
                }
            }
        }
    }


    @Override
    public int getItemCount() {
        return rooms.size();
    }

    public interface ISendStateToRoom {
        void sendStateToRoom(String roomDocument, String role, int state, String email);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPlayerRoleAvatar;
        TextView txtPlayerRoleName;
        Button btnJoin;
        Button btnDecline;
        String roomDocument;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            imgPlayerRoleAvatar = itemView.findViewById(R.id.imgPlayerRoleAvatar);
            txtPlayerRoleName = itemView.findViewById(R.id.txtPlayerRoleName);
            btnJoin = itemView.findViewById(R.id.btnJoin);
            btnDecline = itemView.findViewById(R.id.btnDecline);

            btnJoin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isX) {
                        iSendStateToRoom.sendStateToRoom(roomDocument, Const.TOKEN_X,
                                Const.PLAYER_STATE_JOIN_ROOM, currentUser.getEmail());
                    }
                    else {
                        iSendStateToRoom.sendStateToRoom(roomDocument, Const.TOKEN_O,
                                Const.PLAYER_STATE_JOIN_ROOM, currentUser.getEmail());
                    }
                }
            });

            btnDecline.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
        }
    }
}
