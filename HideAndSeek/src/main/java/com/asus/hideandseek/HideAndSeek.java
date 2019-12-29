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
import com.asus.robotframework.API.results.RecognizePersonResult;
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
    private static int bodyMoved = 0;
    private static boolean moveHeadLeft = true;
    public enum NavigationState {
        NOT_STARTED, A_TO_B, MOVE_HEAD_LEFT, MOVE_HEAD_RIGHT, MOVE_HEAD_CENTER, MOVE_BODY
    }
    public static NavigationState navigationState = NavigationState.NOT_STARTED;

    // request code for READ_CONTACTS. It can be any number > 0.
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    private static final int ERROR_THRESHOLD = 5;

    public HideAndSeek(RobotCallback robotCallback, RobotCallback.Listen robotListenCallback) {
        super(robotCallback, robotListenCallback);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
        statusText = findViewById(R.id.status);

        // Initialize the robot
        robotAPI = new RobotAPI(context, robotCallback);
        robotAPI.robot.registerListenCallback(robotListenCallback);

        // Hide his face ( -!- important to see the UI )
        robotAPI.robot.setExpression(RobotFace.HIDEFACE);

        // Intialize behaviours
        speaking = new Speaking();
        navigation = new Navigation();
        seeking = new Seeking();

        // Turn off his wheels
        robotAPI.wheelLights.turnOff(WheelLights.Lights.SYNC_BOTH, 0xff);

        // Disable some of his behaviours
        robotAPI.robot.setVoiceTrigger(false);
        robotAPI.robot.setPressOnHeadAction(false);

        // Ask for contacts permission
        requestPermission();

        super.onCreate(savedInstanceState);
    }


    @Override
    protected void onResume() {
        super.onResume();

        // Start looking for the user
        seeking.startLookingForTarget();
    }


    @Override
    protected void onDestroy() {
        seeking.stop();
        robotAPI.robot.stopSpeakAndListen();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
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
        public void onRecognizePersonResult(List<RecognizePersonResult> resultList) {
            super.onRecognizePersonResult(resultList);
            seeking.handlePersonDetection(resultList);
            statusText.setText(seeking.state.toString());
        }

        @Override
        public void onResult(int cmd, int serial, RobotErrorCode err_code, Bundle result) {
            super.onResult(cmd, serial, err_code, result);
            if (cmd == RobotCommand.MOTION_MOVE_BODY.getValue()) {
                navigation.continueSearch();
            }
        }

        @Override
        public void onStateChange(int cmd, int serial, RobotErrorCode err_code, RobotCmdState state) {
            super.onStateChange(cmd, serial, err_code, state);
            statusText.setText(seeking.state.toString());

            if (cmd == RobotCommand.MOTION_GO_FROM_A_TO_B.getValue() && state == RobotCmdState.FAILED) {
                errorCount++;
                if (errorCount > ERROR_THRESHOLD) {
                    robotAPI.robot.speak("Oops I'm stuck.");
                    errorCount = 0;
                    navigation.stopAllMovement();
                }
            }

            if (state == RobotCmdState.SUCCEED) {
                if (cmd == RobotCommand.MOTION_GO_FROM_A_TO_B.getValue() && navigationState == NavigationState.A_TO_B) {
                    Log.d("Movement", "MOVE HEAD LEFT");
                    navigationState = NavigationState.MOVE_HEAD_LEFT;
                    navigation.moveHeadLeft();
                } else if (cmd == RobotCommand.MOVE_HEAD.getValue() && navigationState == NavigationState.MOVE_HEAD_LEFT) {
                    Log.d("Movement", "MOVE HEAD RIGHT");
                    navigationState = NavigationState.MOVE_HEAD_RIGHT;
                    navigation.moveHeadRight();
                } else if (cmd == RobotCommand.MOVE_HEAD.getValue() && navigationState == NavigationState.MOVE_HEAD_RIGHT) {
                    Log.d("Movement", "MOVE HEAD CENTER");
                    navigationState = NavigationState.MOVE_HEAD_CENTER;
                    navigation.moveHeadCenter();
                } else if (cmd == RobotCommand.MOVE_HEAD.getValue() && navigationState == NavigationState.MOVE_HEAD_CENTER) {
                    if (bodyMoved == 0) {
                        Log.d("Movement", "MOVE BODY FOR THE 0TH TIME");
                        bodyMoved = 1;
                        navigationState = NavigationState.MOVE_BODY;
                        navigation.moveBody();
                    } else {
                        Log.d("Movement", "MOVE BODY FOR THE 1ST TIME");
                        bodyMoved = 0;
                        navigation.continueSearch();
                    }
                } else if (cmd == RobotCommand.MOTION_MOVE_BODY.getValue() && navigationState == NavigationState.MOVE_BODY) {
                    navigation.moveHeadLeft();
                    navigationState = NavigationState.MOVE_HEAD_LEFT;
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
