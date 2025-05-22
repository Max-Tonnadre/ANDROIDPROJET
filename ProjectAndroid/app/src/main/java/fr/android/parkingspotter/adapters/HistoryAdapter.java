package fr.android.parkingspotter.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.gms.maps.model.LatLng;
import fr.android.parkingspotter.R;
import fr.android.parkingspotter.models.ParkingLocation;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {
    public interface OnHistoryActionListener {
        void onShowOnMap(ParkingLocation location);
        void onShowDirections(ParkingLocation location);
        void onDelete(ParkingLocation location);
    }
    private final List<ParkingLocation> locations;
    private final OnHistoryActionListener actionListener;
    private OnShowOnMapRequestListener showOnMapRequestListener;
    public void setOnShowOnMapRequestListener(OnShowOnMapRequestListener listener) {
        this.showOnMapRequestListener = listener;
    }
    public HistoryAdapter(List<ParkingLocation> locations, OnHistoryActionListener actionListener) {
        this.locations = locations;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        ParkingLocation location = locations.get(position);
        // Format date et heure
        String formattedDateTime = "";
        try {
            // Suppose que location.getDatetime() est au format ISO 8601 ou yyyy-MM-dd HH:mm:ss
            String raw = location.getDatetime();
            Date date = null;
            if (raw != null && !raw.isEmpty()) {
                // Essaye plusieurs formats courants
                try {
                    date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(raw);
                } catch (ParseException e1) {
                    try { date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(raw); } catch (ParseException e2) {}
                }
            }
            if (date != null) {
                formattedDateTime = new SimpleDateFormat("dd/MM/yy HH:mm").format(date);
            } else {
                formattedDateTime = raw != null ? raw : "";
            }
        } catch (Exception e) {
            formattedDateTime = location.getDatetime();
        }
        holder.textDatetime.setText(formattedDateTime);
        holder.textAddress.setText(location.getAddress());
        // Affiche la miniature si photo (local uniquement)
        if (location.getPhotoPath() != null && !location.getPhotoPath().isEmpty()) {
            File imgFile = new File(location.getPhotoPath());
            if (imgFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                holder.imagePhoto.setImageBitmap(bitmap);
            } else {
                holder.imagePhoto.setImageResource(android.R.drawable.ic_menu_camera);
            }
        } else {
            holder.imagePhoto.setImageResource(android.R.drawable.ic_menu_camera);
        }
        // Bouton d'actions (menu)
        holder.btnActions.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.getMenu().add(v.getContext().getString(R.string.history_show_on_map));
            popup.getMenu().add(v.getContext().getString(R.string.history_show_directions));
            popup.getMenu().add(v.getContext().getString(R.string.history_delete_point));
            popup.setOnMenuItemClickListener(item -> {
                String title = item.getTitle().toString();
                if (title.equals(v.getContext().getString(R.string.history_show_on_map))) {
                    if (showOnMapRequestListener != null) showOnMapRequestListener.onShowOnMapRequest(location);
                    actionListener.onShowOnMap(location);
                } else if (title.equals(v.getContext().getString(R.string.history_show_directions))) {
                    actionListener.onShowDirections(location);
                } else if (title.equals(v.getContext().getString(R.string.history_delete_point))) {
                    actionListener.onDelete(location);
                }
                return true;
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return locations.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        ImageView imagePhoto;
        TextView textAddress, textDatetime;
        ImageButton btnActions;
        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            imagePhoto = itemView.findViewById(R.id.image_photo);
            textAddress = itemView.findViewById(R.id.text_address);
            textDatetime = itemView.findViewById(R.id.text_datetime);
            btnActions = itemView.findViewById(R.id.btn_history_actions);
        }
    }
}
