package com.asus.hideandseek;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.asus.hideandseek.R;
import com.asus.robotframework.API.RobotAPI;
import com.asus.robotframework.API.RobotCallback;
import com.asus.robotframework.API.RobotCmdState;
import com.asus.robotframework.API.RobotCommand;
import com.asus.robotframework.API.RobotErrorCode;
import com.asus.robotframework.API.RobotFace;
import com.asus.robotframework.API.SpeakConfig;
import com.asus.robotframework.API.WheelLights;
import com.robot.asus.robotactivity.RobotActivity;
import com.asus.robotframework.API.results.DetectFaceResult;
import com.asus.robotframework.API.results.DetectPersonResult;

import org.json.JSONObject;

import java.util.List;

public class HideAndSeek extends RobotActivity {
    public static RobotAPI robotAPI;
    public static Seeking seeking;
    public static Speaking speaking;
    public static Navigation navigation;
    public static TextView statusText;
    public Context context = this;

    private static int errorCount = 0;

    // request code for READ_CONTACTS. It can be any number > 0.
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    private static final int ERROR_THRESHOLD = 5;

    public HideAndSeek(RobotCallback robotCallback, RobotCallback.Listen robotListenCallback) {
        super(robotCallback, robotListenCallback);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        statusText = findViewById(R.id.status);

        // Initialize the robot
        robotAPI = new RobotAPI(context, robotCallback);
        robotAPI.robot.registerListenCallback(robotListenCallback);

        // Hide his face ( -!- important to see the UI )
        robotAPI.robot.setExpression(RobotFace.ACTIVE);

        // Intialize behaviours
        speaking = new Speaking();
        navigation = new Navigation();
        seeking = new Seeking();
        seeking.stop();

        // Turn off his wheels
        robotAPI.wheelLights.turnOff(WheelLights.Lights.SYNC_BOTH, 0xff);

        // Disable some of his behaviours
        robotAPI.robot.setVoiceTrigger(false);
        robotAPI.robot.setPressOnHeadAction(false);
        robotAPI.robot.speakAndListen("I am waiting for your command master", SpeakConfig.MODE_FOREVER);
        // Ask for contacts permission
        requestPermission();

        // Register actions to UI buttons for debugging
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        seeking.stop();
        robotAPI.robot.stopSpeakAndListen();
    }

    public static RobotCallback robotCallback = new RobotCallback() {

        @Override
        public void onDetectFaceResult(List<DetectFaceResult> resultList) {
            super.onDetectFaceResult(resultList);
            seeking.handleFaceDetection(resultList);
            statusText.setText(seeking.state.toString());
        }

        @Override
        public void onDetectPersonResult(List<DetectPersonResult> resultList) {
            super.onDetectPersonResult(resultList);
            seeking.handlePersonDetection(resultList);
            statusText.setText(seeking.state.toString());
        }

        @Override
        public void onResult(int cmd, int serial, RobotErrorCode err_code, Bundle result) {
            super.onResult(cmd, serial, err_code, result);
        }

        @Override
        public void onStateChange(int cmd, int serial, RobotErrorCode err_code, RobotCmdState state) {
            super.onStateChange(cmd, serial, err_code, state);
            statusText.setText(seeking.state.toString());

            if (cmd == RobotCommand.MOTION_GO_FROM_A_TO_B.getValue() && state == RobotCmdState.SUCCEED) {
                // keep going
                navigation.continueSearch();
            } else if (state == RobotCmdState.ACTIVE) {
                // do nothing
            } else if (state == RobotCmdState.FAILED) {
                errorCount++;
                if (errorCount > ERROR_THRESHOLD) {
                    robotAPI.robot.speak("Oops I'm stuck.");
                    navigation.stopAllMovement();
                } else {
                    navigation.continueSearch();
                }
            }
        }

        @Override
        public void initComplete() {
            super.initComplete();

        }
    };

    public static RobotCallback.Listen robotListenCallback = new RobotCallback.Listen() {
        @Override
        public void onFinishRegister() { }

        @Override
        public void onVoiceDetect(JSONObject jsonObject) { }

        @Override
        public void onSpeakComplete(String s, String s1) { }

        @Override
        public void onEventUserUtterance(JSONObject jsonObject) {
            Log.d("Event utterance", "the json object is " + jsonObject.toString());
            speaking.handleUserConversation(seeking, jsonObject);
        }

        @Override
        public void onResult(JSONObject jsonObject) {
            Log.d("Listening complete", "the json object is " + jsonObject.toString());
        }

        @Override
        public void onRetry(JSONObject jsonObject) {

        }
    };

    public HideAndSeek() {
        super(robotCallback, robotListenCallback);
    }

    private void requestPermission() {
        // Check the SDK version and whether the permission is already granted or not.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                this.checkSelfPermission(Manifest.permission.READ_CONTACTS) ==
                        PackageManager.PERMISSION_GRANTED) {
            // Android version is lesser than 6.0 or the permission is already granted.
            Log.d("ZenboGoToLocation", "permission is already granted");
            return;
        }

        if (shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},
                    PERMISSIONS_REQUEST_READ_CONTACTS);
        } else {
            //showMessageOKCancel("You need to allow access to Contacts",
            //        new DialogInterface.OnClickListener() {
            //            @Override
            //            public void onClick(DialogInterface dialog, int which) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},
                    PERMISSIONS_REQUEST_READ_CONTACTS);
            //            }
            //        });
        }
    }
}
