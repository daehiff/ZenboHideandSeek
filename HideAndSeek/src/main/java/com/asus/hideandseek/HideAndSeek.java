package com.asus.hideandseek;

import android.content.Context;
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
        robotAPI.robot.setExpression(RobotFace.HIDEFACE);

        // Intialize behaviours
        speaking = new Speaking();
        navigation = new Navigation();
        seeking = new Seeking();

        // Turn off his wheels
        robotAPI.wheelLights.turnOff(WheelLights.Lights.SYNC_BOTH, 0xfffffff);

        // Disable some of his behaviours
        robotAPI.robot.setVoiceTrigger(false);
        robotAPI.robot.setPressOnHeadAction(false);
        seeking.stop();


        // Register actions to UI buttons for debugging
        // TODO: Should be removed in the final version
        Button startDetectionButton = findViewById(R.id.startdetection);
            startDetectionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                seeking.switchToPersonDetection();
                statusText.setText(seeking.state.toString());
                }
            });

        Button cancelButton = findViewById(R.id.canceltheshit);
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                seeking.stop();
                statusText.setText("Stopped detections");
                }
            });

        Button startButton = findViewById(R.id.start);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                seeking.startLookingForTarget();
                statusText.setText(seeking.state.toString());
            }
        });

        findViewById(R.id.user_yes).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                seeking.handleUserAnswer(true);
            }
        });

        findViewById(R.id.user_no).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                seeking.handleUserAnswer(false);
            }
        });

        findViewById(R.id.user_sorry).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                seeking.handleApology();
            }
        });
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
//            Log.d("RobotDevSample", "onResult:"
//                    + RobotCommand.getRobotCommand(cmd).name()
//                    + ", serial:" + serial + ", err_code:" + err_code
//                    + ", result:" + result.getString("RESULT"));
        }

        @Override
        public void onStateChange(int cmd, int serial, RobotErrorCode err_code, RobotCmdState state) {
            super.onStateChange(cmd, serial, err_code, state);
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

}
