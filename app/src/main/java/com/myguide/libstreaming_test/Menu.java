package com.myguide.libstreaming_test;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.format.Formatter;

import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.audio.AudioQuality;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.rtsp.RtspClient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;

import android.graphics.Bitmap;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;
import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.irozon.justbar.BarItem;
import com.irozon.justbar.JustBar;
import com.irozon.justbar.interfaces.OnBarItemClickListener;

import androidx.core.app.ActivityCompat;

import com.google.zxing.WriterException;

public class Menu extends Activity implements
        RtspClient.Callback,
        Session.Callback,
        SurfaceHolder.Callback,
        OnShowcaseEventListener{

    public final static String TAG = "MainActivity";


    private BarItem mButtonStart;
    private BarItem mButtonStop;

    private SurfaceView mSurfaceView;
    private TextView mTextBitrate;
    private Session mSession;
    private RtspClient mClient;
    private ImageView mimageView;
    private ImageView imageViewBackground;
    private LottieAnimationView stream;
    private LottieAnimationView location;


    RelativeLayout.LayoutParams lps;
    Target[] targets;
    String[][] infos;
    ShowcaseView sv;

    int count_click = 0;
    int count_click_location = 0;

    JustBar justBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_menu);


        //get by id

        mimageView = (ImageView) findViewById(R.id.img_qr);
        imageViewBackground = (ImageView) findViewById(R.id.background);
        mButtonStart = (BarItem) findViewById(R.id.start);
        mButtonStop = (BarItem) findViewById(R.id.stop);

        mSurfaceView = (SurfaceView) findViewById(R.id.surface);
        mTextBitrate = (TextView) findViewById(R.id.bitrate);
        stream = (LottieAnimationView) findViewById(R.id.stream);
        stream.setVisibility(View.INVISIBLE);
        location = (LottieAnimationView) findViewById(R.id.location);
        location.setVisibility(View.INVISIBLE);

        justBar = findViewById(R.id.bottomBar);

        mButtonStop.setEnabled(false);

        //ask for permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                }, 1);
            }
        }

        //SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        // Configures the SessionBuilder
        mSession = SessionBuilder.getInstance()
                .setContext(getApplicationContext())
                .setAudioEncoder(SessionBuilder.AUDIO_AAC)
                .setAudioQuality(new AudioQuality(8000, 16000))
                .setVideoEncoder(SessionBuilder.VIDEO_NONE)
                .setSurfaceView(mSurfaceView)
                .setCallback(this)
                .build();

        // Configures the RTSP client
        mClient = new RtspClient();
        mClient.setSession(mSession);
        mClient.setCallback(this);
        mSurfaceView.getHolder().addCallback(this);


        justBar.setOnBarItemClickListener(new OnBarItemClickListener() {
            @Override
            public void onBarItemClick(BarItem barItem, int position) {
                if (position == 0) {
                    //location tracking enable/disable
                    if (count_click_location == 0) {
                        //start sharing position
                        startService(new Intent(getApplicationContext(), LastLocationService.class));
                        Toast.makeText(Menu.this, "Sharing location started ", Toast.LENGTH_SHORT).show();
                        count_click_location = 1;

                        location.setVisibility(View.VISIBLE);
                    } else {
                        //stop sharing position
                        stopService(new Intent(getApplicationContext(), LastLocationService.class));
                        Toast.makeText(Menu.this, "Sharing location stopped ", Toast.LENGTH_SHORT).show();
                        count_click_location = 0;

                        location.setVisibility(View.INVISIBLE);
                    }
                    mimageView.setVisibility(View.INVISIBLE);
                    imageViewBackground.setVisibility(View.VISIBLE);
                    stream.setVisibility(View.INVISIBLE);
                } else if (position == 1) {
                    // 1 Start streaming
                    startStream();
                    mimageView.setVisibility(View.INVISIBLE);
                    imageViewBackground.setVisibility(View.VISIBLE);
                    location.setVisibility(View.INVISIBLE);
                }else if(position == 2) {
                    // 2 Stop streaming
                    stopStream();
                    mimageView.setVisibility(View.INVISIBLE);
                    imageViewBackground.setVisibility(View.VISIBLE);
                    stream.setVisibility(View.INVISIBLE);
                    location.setVisibility(View.INVISIBLE);
                } else if (position == 3) {
                    //qr code
                    if (mimageView.getDrawable() != null) {
                        mimageView.setVisibility(View.VISIBLE);
                        imageViewBackground.setVisibility(View.INVISIBLE);
                        stream.setVisibility(View.INVISIBLE);
                        location.setVisibility(View.INVISIBLE);
                    } else {
                        Toast.makeText(Menu.this, "Start streaming to view QR", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });


        final String PREFS_NAME = "MyPrefsFile";

        final ViewTarget target1 = new ViewTarget(R.id.swShareLocation, this);
        final ViewTarget target4 = new ViewTarget(R.id.qrCode, this);
        final ViewTarget target2 = new ViewTarget(mButtonStart);
        final ViewTarget target3 = new ViewTarget(mButtonStop);
        targets = new ViewTarget[]{target1, target2, target3, target4};

        lps = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lps.addRule(RelativeLayout.CENTER_VERTICAL);
        lps.addRule(RelativeLayout.CENTER_HORIZONTAL);
        int margin = ((Number) (getResources().getDisplayMetrics().density * 12)).intValue();
        lps.setMargins(margin, margin, margin, margin);

        infos = new String[][]{
                {"Share Location :\n", "Start Streaming :\n", "Stop Streaming :\n", "Qr Code :\n"},
                {
                 "Click here to share your location with the tourists.",
                 "Click here TWICE to start the streaming, you can then talk through your phone and " +
                         "the tourists will hear you through theirs.",
                 "Click here to stop the streaming.",
                 "Click here to show the generated QR code to be scanned by the tourists, " +
                         "so they can start receiving your streaming. \n" + tab +
                         "You can get the QR Code only if you have already Started the streaming."
                }
        };

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        if (settings.getBoolean("first_time", true)) {
            //the app is being launched for first time, do something
            Log.d("Comments", "First time");

            // first time task
            toStep(infos[0][0], infos[1][0], targets[0], 0);

            // record the fact that the app has been started at least once
            settings.edit().putBoolean("first_time", false).commit();
        }
    }

    String tab = "\t\t\t ";

    private void toStep(final String title, final String description,Target target, final int i) {
        final ShowcaseView sv = new ShowcaseView.Builder(this)
                .setTarget(targets[i])
                .setContentTitle(title)
                .setContentText(tab + description)
                .setStyle(R.style.CustomShowcaseTheme)
                .replaceEndButton(R.layout.view_custom_button)
                .withMaterialShowcase()
                .blockAllTouches()
                .setShowcaseEventListener(new OnShowcaseEventListener() {
                    @Override
                    public void onShowcaseViewTouchBlocked(MotionEvent motionEvent) {
                    }

                    @Override
                    public void onShowcaseViewShow(ShowcaseView showcaseView) {
                    }

                    @Override
                    public void onShowcaseViewHide(ShowcaseView showcaseView) {
                        try {
                            toStep(infos[0][i+1], infos[1][i+1], targets[i+1], i+1);
                        } catch(Exception ex) {

                        }
                    }
                    @Override
                    public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
                    }
                })
                .build();
        sv.setButtonPosition(lps);

    }

    // Connects/disconnects to the RTSP server and starts/stops the stream
    public void startStream(){
        if (!mClient.isStreaming()) {
            if (count_click < 1) {
                count_click++;
                Toast.makeText(Menu.this, "Click again to start streaming", Toast.LENGTH_LONG).show();
                // start the RTSP server
                this.startService(new Intent(this, RtspServerService.class));
            } else {
                count_click = 0;
                String ip, port, path;
                // We save the content user inputs in Shared Preferences
                SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(Menu.this);
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putString("uri", "rtsp://127.0.0.1:9999/test");
                editor.putString("password", "");
                editor.putString("username", "");
                editor.commit();
                // We parse the URI written in the Editext
                Pattern uri = Pattern.compile("rtsp://(.+):(\\d*)/(.+)");
                Matcher m = uri.matcher("rtsp://127.0.0.1:9999/test");
                m.find();
                ip = m.group(1);
                port = m.group(2);
                path = m.group(3);

                mClient.setServerAddress(ip, Integer.parseInt(port));
                mClient.setStreamPath("/" + path);
                mClient.startStream();
                //generating qr
                String url = URL();
                putQR(url);
                //Toast.makeText(MainActivity.this,url,Toast.LENGTH_LONG).show();
                stream.setVisibility(View.VISIBLE);
                mButtonStop.setEnabled(true);
            }
        }else stream.setVisibility(View.VISIBLE);
    }

    public void stopStream(){
        if (mClient.isStreaming()) {
            // Stops the stream and disconnects from the RTSP server
            mClient.stopStream();
            // stop the RTSP server
            this.stopService(new Intent(this, RtspServerService.class));
            mimageView.setImageResource(0);
            mButtonStop.setEnabled(false);
            stream.setVisibility(View.INVISIBLE); // Cancels the animation
        }
    }

    private void logError(final String msg) {
        final String error = (msg == null) ? "Error unknown" : msg;
        // Displays a popup to report the eror to the user
        AlertDialog.Builder builder = new AlertDialog.Builder(Menu.this);
        builder.setMessage(msg).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public String getWifiApIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en
                    .hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                if (intf.getName().contains("wl") || intf.getName().contains("ap")) {
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr
                            .hasMoreElements(); ) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress()
                                && (inetAddress.getAddress().length == 4)) {
                            Log.d(TAG, inetAddress.getHostAddress());
                            return inetAddress.getHostAddress();
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e(TAG, ex.toString());
        }
        return null;
    }

    public String URL(){
        //WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        //String ipAddress = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        String URL= "rtsp://"+getWifiApIpAddress()+":9999/test";
        return URL;
    }

    public void putQR(String URL) {
        QRGEncoder qrgEncoder = new QRGEncoder(URL, null, QRGContents.Type.TEXT, 300);
        try {
            // Getting QR-Code as Bitmap
            Bitmap bitmap = qrgEncoder.encodeAsBitmap();
            // Setting Bitmap to ImageView
            mimageView.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mClient.release();
        mSession.release();
        mSurfaceView.getHolder().removeCallback(this);
    }

    @Override
    public void onBitrateUpdate(long bitrate) {
        mTextBitrate.setText("" + bitrate / 1000 + " kbps");
    }

    @Override
    public void onSessionError(int reason, int streamType, Exception e) {
        switch (reason) {
            case Session.ERROR_CAMERA_ALREADY_IN_USE:
                break;
            case Session.ERROR_INVALID_SURFACE:
                break;
            case Session.ERROR_STORAGE_NOT_READY:
                break;
            case Session.ERROR_CONFIGURATION_NOT_SUPPORTED:
                logError("The following settings are not supported on this phone: " +
                        "(" + e.getMessage() + ")");
                e.printStackTrace();
                return;
            case Session.ERROR_OTHER:
                break;
        }

        if (e != null) {
            logError(e.getMessage());
            e.printStackTrace();
        }
    }


    @Override
    public void onPreviewStarted() {

    }

    @Override
    public void onSessionConfigured() {

    }

    @Override
    public void onSessionStarted() {

    }

    @Override
    public void onSessionStopped() {

    }

    @Override
    public void onRtspUpdate(int message, Exception e) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //mSession.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //mClient.stopStream();
    }

    @Override
    public void onShowcaseViewHide(ShowcaseView showcaseView) {

    }

    @Override
    public void onShowcaseViewDidHide(ShowcaseView showcaseView) {

    }

    @Override
    public void onShowcaseViewShow(ShowcaseView showcaseView) {

    }

    @Override
    public void onShowcaseViewTouchBlocked(MotionEvent motionEvent) {

    }
}
