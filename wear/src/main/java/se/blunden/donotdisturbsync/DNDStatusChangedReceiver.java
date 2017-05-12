package se.blunden.donotdisturbsync;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class DNDStatusChangedReceiver extends BroadcastReceiver {
    private static final String TAG = "DndStatusReceiver";

    // Toggle between sending the ringer mode or the interrupt filter mode
    private static final boolean SEND_RINGER_MODE = true;

    private Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;

        if (intent.getAction().equals(AudioManager.RINGER_MODE_CHANGED_ACTION)) {
            Log.d(TAG, "Received a RINGER_MODE_CHANGED");

            if (SEND_RINGER_MODE) {
                AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

                int deviceRingerMode = audioManager.getRingerMode();

                Log.d(TAG, "Current ringer mode is: " + deviceRingerMode);

                if (deviceRingerMode != AudioManager.RINGER_MODE_SILENT) {
                    // Save the "normal" non-silent mode
                    saveNormalRingerMode(deviceRingerMode);
                }
                sendRingerMode(deviceRingerMode);
            } else {
                // Read and send the Interruption filter status instead (which controls the DND mode)
                // Syncing the actual ringer mode has other unintended consequences and doesn't match
                // what AW 1.5 synced.
                NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                int deviceDndMode = mNotificationManager.getCurrentInterruptionFilter();

                Log.d(TAG, "Current DND mode is: " + deviceDndMode);
                sendDndMode(deviceDndMode);
            }
        }

        if (intent.getAction().equals(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)) {
            Log.d(TAG, "Received an INTERRUPTION_FILTER_CHANGED");

            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            int deviceDndMode = mNotificationManager.getCurrentInterruptionFilter();

            Log.d(TAG, "Current DND mode is: " + deviceDndMode);
            sendDndMode(deviceDndMode);
        }
    }

    private void sendRingerMode(int ringerMode) {
        // Build the Intent needed to start the WearMessageSenderService
        Intent wearMessageSenderServiceIntent = new Intent(mContext, WearMessageSenderService.class);
        wearMessageSenderServiceIntent.setAction(WearMessageSenderService.ACTION_SEND_MESSAGE);
        wearMessageSenderServiceIntent.putExtra(WearMessageSenderService.EXTRA_RINGER_MODE, String.valueOf(ringerMode));

        mContext.startService(wearMessageSenderServiceIntent);
    }

    private void sendDndMode(int dndMode) {
        // Build the Intent needed to start the WearMessageSenderService
        Intent wearMessageSenderServiceIntent = new Intent(mContext, WearMessageSenderService.class);
        wearMessageSenderServiceIntent.setAction(WearMessageSenderService.ACTION_SEND_MESSAGE);
        wearMessageSenderServiceIntent.putExtra(WearMessageSenderService.EXTRA_DND_MODE, String.valueOf(dndMode));

        mContext.startService(wearMessageSenderServiceIntent);
    }

    private void saveNormalRingerMode(int ringerMode) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        sharedPreferences.edit().putInt("normalMode", ringerMode).apply();
    }
}
