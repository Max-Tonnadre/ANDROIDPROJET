package fr.android.parkingspotter.sync;

import android.content.Context;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import fr.android.parkingspotter.database.ParkingDatabaseHelper;
import fr.android.parkingspotter.firebase.SupabaseDatabaseManager;
import fr.android.parkingspotter.models.ParkingLocation;

public class ParkingSyncManager {
    public enum SyncDirection { LOCAL_TO_REMOTE, REMOTE_TO_LOCAL }

    public static void synchronize(Context context, SupabaseDatabaseManager remoteDB, ParkingDatabaseHelper localDB, SyncDirection direction, Runnable onComplete) {
        if (direction == SyncDirection.LOCAL_TO_REMOTE) {
            remoteDB.getAllParkingLocationsAsync(remoteList -> {
                List<ParkingLocation> localList = localDB.getAllParkingLocations();
                Map<String, ParkingLocation> remoteByUuid = new HashMap<>();
                for (ParkingLocation r : remoteList) {
                    if (r.getRemoteId() != null) remoteByUuid.put(r.getRemoteId(), r);
                }
                for (ParkingLocation local : localList) {
                    if (local.getRemoteId() == null || !remoteByUuid.containsKey(local.getRemoteId())) {
                        remoteDB.addParkingLocation(local, local.getPhotoPath());
                    }
                }
                if (onComplete != null) onComplete.run();
            });
        } else if (direction == SyncDirection.REMOTE_TO_LOCAL) {
            remoteDB.getAllParkingLocationsAsync(remoteList -> {
                localDB.clearAll();
                for (ParkingLocation remote : remoteList) {
                    localDB.addParkingLocationWithRemoteId(remote, remote.getRemoteId());
                }
                if (onComplete != null) onComplete.run();
            });
        }
    }
}
