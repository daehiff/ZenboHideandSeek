package com.asus.robotdevsample;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.asus.robotdevsample.R;
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

import static android.content.ContentValues.TAG;
import static java.sql.DriverManager.println;

public class HideAndSeek extends RobotActivity {

    public RobotAPI robotAPI;
    public static Seeking seeking;
    public static Speaking speaking;
    public static TextView statusText;
    public Context context = this;
    public static String playerId = null;

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
        seeking = new Seeking(robotAPI);
        speaking = new Speaking(robotAPI);

        // Turn off his wheels
        robotAPI.wheelLights.turnOff(WheelLights.Lights.SYNC_BOTH, 0xfffffff);

        // Disable some of his behaviours
        robotAPI.robot.setVoiceTrigger(false);
        robotAPI.robot.setPressOnHeadAction(false);
        seeking.stop();


        // Register actions to UI buttons for debugging
        Button resetButton = findViewById(R.id.startdetection);
                resetButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        seeking.state = Seeking.SeekingState.SEARCHING_FOR_TARGET;
                        seeking.switchToFaceDetection();
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

        //ime hide
        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        //robotAPI = new RobotAPI(this, robotCallback);
        //robotAPI.robot.speakAndListen("Do you want to play Hide and Seek ?", new SpeakConfig().timeout(30));
    }





    public static RobotCallback robotCallback = new RobotCallback() {

        @Override
        public void onDetectFaceResult(List<DetectFaceResult> resultList){
            super.onDetectFaceResult(resultList);
            Log.d("onDetectFaceResult", resultList.toString());
            playerId = resultList.get(0).getUuid();
            seeking.handlePersonDetection(resultList, playerId);
            statusText.setText(seeking.state.toString());
        }

        @Override
        public void onDetectPersonResult(List<DetectPersonResult> resultList){
            super.onDetectPersonResult(resultList);
            Log.d("Enter detect person", "Detect Person");
            seeking.handlePersonDetection(resultList);
            statusText.setText(seeking.state.toString());
                //for (int i = 0; i < resultList.size(); i++) {
                    //Log.d("onDetectPersonResult", resultList.get(i).toString());
              //  }

        }

        /*public void onDetectFaceResult(resultList: MutableList<DetectFaceResult>) {
            resultList.forEach {
                println("detectFace\t\t\ttid %d\t\tuuid %s\t\thpconf %d\t\t%s\t\t%s".format(it.trackID, it.uuid,
                        it.headPoseConfidence, if (it.hasValidDepth()) "valid depth" else "invalid depth",
                if (it.isCandidateObj) "candidate" else "full"))
            }

        }*/
        @Override
        public void onResult(int cmd, int serial, RobotErrorCode err_code, Bundle result) {
            super.onResult(cmd, serial, err_code, result);
            Log.d("RobotDevSample", "onResult:"
                    + RobotCommand.getRobotCommand(cmd).name()
                    + ", serial:" + serial + ", err_code:" + err_code
                    + ", result:" + result.getString("RESULT"));
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
        public void onFinishRegister() {

        }

        @Override
        public void onVoiceDetect(JSONObject jsonObject) {
            //Log.d("voice detection", "the json object is " + jsonObject.toString());
        }

        @Override
        public void onSpeakComplete(String s, String s1) {
            //Log.d("RobotDevSample", "speak Complete");
        }

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
