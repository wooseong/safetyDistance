package threetoone.sleepinessdistance;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.multidex.MultiDex;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import threetoone.sleepinessdistance.camera.CameraSourcePreview;
import threetoone.sleepinessdistance.camera.GraphicOverlay;


public class MainFragment extends AppCompatActivity {
    private static final String TAG = "FaceTracker";
    private static final int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 1;
    private CameraSource mCameraSource = null;
    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;
    private MediaPlayer mp;
    private AudioManager audioManager;
    private Button connectButton;

    private static final int RC_HANDLE_GMS = 9001;
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    public int detectionStart = 0;
    private long filterOnTime = 0, filterOffTime = 0;
    private boolean detectionCheck = false, filterCheck = false;

    private int c;

    private TimerTask eyeAvgTimer, filterTimer1, filterTimer2;
    private Timer timer;

    public boolean value, soundOnOff=false;

    public static boolean bFilter = false, bSound = false;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.fragment_main);

        Singleton.getInstance().setSleepTextView((TextView) findViewById(R.id.sleepTextview));
        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);
        connectButton = (Button) findViewById(R.id.connectButton);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        c = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mPreview.setVisibility(View.INVISIBLE);

        action();
        connectButton.setOnClickListener(
                new Button.OnClickListener() {
                    public void onClick(View v) {
                        buttonEvent();
                    }
                }
        );
    }

    public void buttonEvent() {
        value = Singleton.getInstance().getSwitchValue();

        if (value) {
            connectButton.setBackgroundResource(R.drawable.connectbutton2);
            value = false;
            Singleton.getInstance().setSwitchValue(value);
            bFilter = false;
            bSound = false;
//            Intent intent = new Intent(MainFragment.this, BackgroundService.class);
//            stopService(intent);
        } else {
            connectButton.setBackgroundResource(R.drawable.disconnectbutton2);
            value = true;
            Singleton.getInstance().setSwitchValue(value);
            bFilter = true;
            bSound = true;
        }
    }

    public void action() {

        // Check for the camera permission before accessing the camera. If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            requestCameraPermission();
        }

        if (c == 0) {
            Toast.makeText(getApplicationContext(), "Volume is MUTE", Toast.LENGTH_LONG).show();
        }

        eyeAvgTimer = new TimerTask() {
            @Override
            public void run() {
                detectionStart++;
            }
        };

        timer = new Timer();
        timer.schedule(eyeAvgTimer, 0, 30000); //30초
    }

    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();

    }

    private void createCameraSource() {

        Context context = getApplicationContext();
        FaceDetector detector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();

        detector.setProcessor(
                new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory())
                        .build());

        if (!detector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Toast.makeText(getApplicationContext(), "Dependencies are not yet available. ", Toast.LENGTH_LONG).show();
            Log.w(TAG, "Face detector dependencies are not yet available.");
        }

        mCameraSource = new CameraSource.Builder(context, detector)
                .setRequestedPreviewSize(640, 480)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedFps(45.0f)
                .build();

    }

    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // we have permission, so create the camerasource
            createCameraSource();
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("ALERT")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }

    private void startCameraSource() {

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    public TimerTask filterTimer1TaskMaker() {
        TimerTask filterTimer1 = new TimerTask() {
            @Override
            public void run() {
                Intent intent = new Intent(MainFragment.this, BlueLightFilterService.class);
                startService(intent);
                filterCheck = true;
            }
        };
        return filterTimer1;
    }

    public TimerTask filterTimer2TaskMaker() {
        TimerTask filterTimer2 = new TimerTask() {
            @Override
            public void run() {
                Intent intent = new Intent(MainFragment.this, BlueLightFilterService.class);
                stopService(intent);
                filterCheck = false;
            }
        };
        return filterTimer2;
    }

    public void filterOn(int i) {
        TextView text = (TextView) findViewById(R.id.sleepTextview);
        switch (i) {
            case 1:
                Singleton.getInstance().getSleepTextView().setText("MISSING");
                text.setText("Face Missing");
                break;
            case 2:
            case 3:
                Singleton.getInstance().getSleepTextView().setText("WARNING");
                Singleton.getInstance().getSleepTextView().setTextColor(Color.RED);
                text.setText("WARNING");
                break;
        }
        if(bSound)
        {
            play_media();
            soundOnOff=true;
        }
        if (bFilter) {
            if (filterOnTime == 0) {
                filterOnTime = System.currentTimeMillis();
                filterTimer1 = filterTimer1TaskMaker();
                filterTimer2 = filterTimer2TaskMaker();
                timer.schedule(filterTimer1, 0, 300); //0.3초
                timer.schedule(filterTimer2, 150, 300); //0.3초
            }
        }
    }

    public void filterOff() {
        if(soundOnOff){
            stop_playing();
            soundOnOff=false;
        }
        if (filterOnTime != 0) {
            filterOffTime = System.currentTimeMillis();
            if (filterOffTime - filterOnTime >= 750) {
                filterTimer1.cancel();
                filterTimer2.cancel();
                filterOnTime = 0;
                if (filterCheck) {
                    Intent intent = new Intent(MainFragment.this, BlueLightFilterService.class);
                    stopService(intent);
                }
            }
        }
    }
    public void play_media()
    {
        stop_playing();
        mp = MediaPlayer.create(this, R.raw.alarm);
        mp.start();
    }
    public void stop_playing()
    {
        if (mp != null) {
            mp.stop();
            mp.release();
            mp = null;
        }
    }

    // Graphic Face Tracker

    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new GraphicFaceTracker(mGraphicOverlay);
        }
    }

    private class GraphicFaceTracker extends Tracker<Face> {
        private GraphicOverlay mOverlay;
        private FaceGraphic mFaceGraphic;

        GraphicFaceTracker(GraphicOverlay overlay) {
            mOverlay = overlay;
            mFaceGraphic = new FaceGraphic(overlay);
        }

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        @Override
        public void onNewItem(int faceId, Face item) {
            mFaceGraphic.setId(faceId);
        }

        int state_i, state_f = -1,
                eyeCloseCNT = 0, eyeCloseCNT1, eyeCloseCNT2,
                errorCnt1 = 0, errorCnt2 = 0,
                CTCTime = 30000;

        long start, end = System.currentTimeMillis(), begin, stop, detectStart, detectEnd,
                eyeCloseTime, eyeCloseTimeTotal = 0;

        double min_time = 70, max_time = 400,
                realMinTime = 1000, realMaxTime = 0,
                eyeCloseTimeAvg = 0, eyeCloseToCloseAvg = 0;

        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
            mOverlay.add(mFaceGraphic);
            mFaceGraphic.updateFace(face);
            if (detectionStart == 2 && !detectionCheck) {
                eyeAvgTimer.cancel();
                detectionCheck = true;

                eyeCloseTimeAvg = eyeCloseTimeTotal / eyeCloseCNT;// 눈깜밖임 평균 시간
                eyeCloseToCloseAvg = (CTCTime / eyeCloseCNT) - eyeCloseTimeAvg;//눈 깜밖임부터 그 다음 깜밖임까지의 시간(눈 감는 시간 제외)
                min_time = (int) eyeCloseTimeAvg / 2;
                if (min_time < 60) min_time = 60;
                max_time = (int) eyeCloseTimeAvg * 2;
                if (max_time > 400) max_time = 400;

                eyeCloseCNT1 = eyeCloseCNT2 = eyeCloseCNT;
                detectStart = System.currentTimeMillis();
            }
            filterOff();
            eye_tracking(face);
        }

        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            mOverlay.remove(mFaceGraphic);
            filterOn(1);
        }

        @Override
        public void onDone() {
            mOverlay.remove(mFaceGraphic);
        }

        private void eye_tracking(Face face) {
            float l = face.getIsLeftEyeOpenProbability();
            float r = face.getIsRightEyeOpenProbability();
            if (l < 0.35 && r < 0.35)//눈 감음
            {
                state_i = 0;
            } else//눈 뜸
            {
                state_i = 1;
            }
            if (state_i != state_f) {//처음, 눈을 감거나 떠서 변화가 생기는 경우
                start = System.currentTimeMillis();
                if (state_f == 0) { //눈을 감았다가 뜬 시점
                    eyeCloseCNT++;
                    if (!detectionCheck) {//처음 30초
                        eyeCloseTimeTotal += eyeCloseTime;
                        if (eyeCloseTime < realMinTime) realMinTime = eyeCloseTime;
                        if (eyeCloseTime > realMaxTime) realMaxTime = eyeCloseTime;
                    } else {
                        detectEnd = System.currentTimeMillis();
                        if ((eyeCloseToCloseAvg * 0.8 <= detectEnd - eyeCloseTime - detectStart) &&
                                (detectEnd - eyeCloseTime - detectStart <= eyeCloseToCloseAvg * 1.2)) {
                            CTCTime += detectEnd - detectStart;
                            eyeCloseCNT1++;
                            eyeCloseToCloseAvg = (CTCTime / eyeCloseCNT1) - eyeCloseTimeAvg;
                            errorCnt1 = 0;
                        } else {
                            errorCnt1++;
                        }
                        detectStart = detectEnd;
                        if (eyeCloseTimeAvg * 0.8 <= eyeCloseTime && eyeCloseTime <= eyeCloseTimeAvg * 1.2) {
                            eyeCloseCNT2++;
                            eyeCloseTimeTotal += eyeCloseTime;
                            eyeCloseTimeAvg = eyeCloseTimeTotal / eyeCloseCNT2;// 눈깜밖임 평균 시간 재설정
                            min_time = (int) eyeCloseTimeAvg / 2;
                            if (min_time < 60) min_time = 60;
                            max_time = (int) eyeCloseTimeAvg * 2;
                            if (max_time > 400) max_time = 400;
                            errorCnt2 = 0;
                        } else {
                            errorCnt2++;
                        }
                        if (errorCnt1 > 3 || errorCnt2 > 3) {
                            errorCnt1 = 0;
                            errorCnt2 = 0;
                            filterOn(2);
                        }
                    }
                }
                end = start;
                stop = System.currentTimeMillis();
            } else if (state_i == 0 && state_f == 0) {
                begin = System.currentTimeMillis();
                eyeCloseTime = begin - stop;
                if (detectionCheck) {
                    if (min_time > eyeCloseTime) {
                    } else if (eyeCloseTime > max_time) {

                        filterOn(3);
                    }
                }
                begin = stop;
            }
            state_f = state_i;

            filterOff();
        }
    }

    public void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {   // 마시멜로우 이상일 경우
            if (!Settings.canDrawOverlays(this)) {              // 체크
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
            } else {
                startService(new Intent(MainFragment.this, MyService.class));
            }
        } else {
            startService(new Intent(MainFragment.this, MyService.class));
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE) {
            if (!Settings.canDrawOverlays(this)) {
                // TODO 동의를 얻지 못했을 경우의 처리

            } else {
                startService(new Intent(MainFragment.this, MyService.class));
            }
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        MultiDex.install(this);
    }

    @Override
    public void onBackPressed() {
        checkPermission();
    }
}
//                intent.putExtra(startTime, DateFormat.getDateTimeInstance().format(new Date()));
