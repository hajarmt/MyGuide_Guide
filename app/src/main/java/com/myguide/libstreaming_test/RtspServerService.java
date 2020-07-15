package com.myguide.libstreaming_test;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import net.majorkernelpanic.streaming.rtsp.RtspServer;

public class RtspServerService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onCreate() {
        super.onCreate();
        //---------------------------------------------server
        // Sets the port of the RTSP server to 1234
        SharedPreferences.Editor editorS = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editorS.putString(RtspServer.KEY_PORT, String.valueOf(9999));
        //editor.putString(RtspServer.USER_SERVICE, "test"); //Why!!
        editorS.commit();
        startService(new Intent(this, RtspServer.class));
        //--------------------------------------------------end>
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, RtspServer.class));
    }
}
