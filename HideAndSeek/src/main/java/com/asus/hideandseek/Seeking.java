package com.asus.hideandseek;

import android.os.Handler;

import java.util.List;

import com.asus.robotframework.API.RobotFace;
import com.asus.robotframework.API.Utility;
import com.asus.robotframework.API.VisionConfig;
import com.asus.robotframework.API.WheelLights;
import com.asus.robotframework.API.results.DetectFaceResult;
import com.asus.robotframework.API.results.DetectPersonResult;

import static com.asus.hideandseek.HideAndSeek.robotAPI;
import static com.asus.hideandseek.HideAndSeek.seeking;
import static com.asus.hideandseek.HideAndSeek.statusText;
import static com.asus.hideandseek.Seeking.SeekingState.ASKING_TO_PLAY;

public class Seeking {
    public static int DEFAULT_COUNTDOWN_TIME = 5;
    public static int DEFAULT_COUNTDOWN_INTERVAL_MS = 2000;
    public enum SeekingState {
        NOT_STARTED, SEARCHING_FOR_TARGET, ASKING_TO_PLAY, COUNTDOWN, POSITIONING, SEEKING
    }

    public SeekingState state = SeekingState.NOT_STARTED;
    private int denialCount = 0;

    private boolean debugMode = true;
    private int countdownSeconds = 0;
    private VisionConfig.PersonDetectConfig detectPersonConfig = new VisionConfig.PersonDetectConfig();
    private VisionConfig.FaceDetectConfig detectFaceConfig = new VisionConfig.FaceDetectConfig();
    Handler delayHandler = new Handler();
    Runnable repeatCountdown = new Runnable() {
        @Override
        public void run() {
            countdown();
        }
    };

    Seeking() {
        // Set-up the vision detections parameters
        detectPersonConfig.interval = 3;
        detectFaceConfig.interval = 3;
        detectPersonConfig.enableDebugPreview = debugMode;
        detectFaceConfig.enableDebugPreview = debugMode;
        detectPersonConfig.enableDetectHead = true;
    }

    public void handlePersonDetection(List<DetectPersonResult> people) {
        switch (state) {
            case SEARCHING_FOR_TARGET:
                if (people.size() > 1) {
                    HideAndSeek.speaking.say("There are too many of you!");
                    robotAPI.robot.setExpression(RobotFace.SHOCKED);
                    blinkAllLights(0x00ff0000, 5, 20);
                } else if (people.size() == 1) {
                    state = ASKING_TO_PLAY;
                    robotAPI.robot.setExpression(RobotFace.CONFIDENT);
                    HideAndSeek.speaking.askForPlay();
                }
                break;

            case SEEKING:
                if (people.size() == 1) {
                    if (people.get(0).getTrackConf() >= 1.0f) {
                        HideAndSeek.robotAPI.robot.speak("Haha, I found you!");
                        robotAPI.robot.setExpression(RobotFace.HAPPY);
                        blinkAllLights(0x001111ff, 5, 50);
                        stop();
                    }
                }
                state = SeekingState.NOT_STARTED;
                break;

            default:
                break;
        }
    }

    public void handleFaceDetection(List<DetectFaceResult> faces) {

    }

    public void startLookingForTarget() {
    //      robotAPI.robot.setExpression(RobotFace.EXPECTING)
        state = SeekingState.SEARCHING_FOR_TARGET;
    }

    public void handleUserAnswer(Boolean decision) {
        if (decision) {
            blinkAllLights(0x0011ff00, 2, 10);
            if (denialCount < 2) {
//                HideAndSeek.robotAPI.utility.playEmotionalAction(RobotFace.ACTIVE, Utility.PlayAction.Head_up_1);
                HideAndSeek.robotAPI.robot.speak("Okay! Hide now, I'm counting until five.");
                robotAPI.robot.setExpression(RobotFace.TIRED);
                seeking.switchToPersonDetection();
                statusText.setText(seeking.state.toString());
                startCountdown(DEFAULT_COUNTDOWN_TIME);
            } else {
//                robotAPI.utility.playEmotionalAction(RobotFace.SERIOUS, Utility.PlayAction.Head_up_1);
                HideAndSeek.robotAPI.robot.speak("Well good for you, but I don't want to play with you anymore now!");
                robotAPI.robot.setExpression(RobotFace.PROUD);
                state = SeekingState.NOT_STARTED;
            }
        } else {
//            robotAPI.utility.playEmotionalAction(RobotFace.INNOCENT, Utility.PlayAction.Head_down_1);
            if (denialCount == 0) {
                HideAndSeek.robotAPI.robot.speak("Huh. Ok then.");
            }
            else {
                HideAndSeek.robotAPI.robot.speak("Again ? Ok then.");
            }
            blinkAllLights(0x00ff8800, 2, 10);
            denialCount++;
            state = SeekingState.NOT_STARTED;
        }
    }

    public void handleApology() {
//        robotAPI.robot.setExpression(RobotFace.ACTIVE)
        HideAndSeek.robotAPI.robot.speak("It's okay!");
        state = SeekingState.NOT_STARTED;
        blinkAllLights(0x000055ff, 3, 50);

        denialCount = 0;
    }

    public void startCountdown(int seconds) {
        state = SeekingState.COUNTDOWN;
        countdownSeconds = 0;
        delayHandler.postDelayed(repeatCountdown, 2000);
    }

    private void countdown() {
        countdownSeconds++;
        HideAndSeek.robotAPI.robot.speak(Integer.toString(countdownSeconds));

        if (countdownSeconds >= DEFAULT_COUNTDOWN_TIME) {
            state = SeekingState.POSITIONING;
            countdownSeconds = 0;
            Point startPoint = new Point(11, 11);
            HideAndSeek.navigation.startSearchingRoom(startPoint);
        } else {
            delayHandler.postDelayed(repeatCountdown, 2000);
        }
    }

    private void blinkAllLights(int color, int cycles, int speed) {
        HideAndSeek.robotAPI.wheelLights.setColor(WheelLights.Lights.SYNC_BOTH, 0xff, color);
        HideAndSeek.robotAPI.wheelLights.startBlinking(WheelLights.Lights.SYNC_BOTH, 0xff, speed, speed, cycles);
    }

    public void switchToPersonDetection() {
        HideAndSeek.robotAPI.vision.cancelDetectFace();
        HideAndSeek.robotAPI.vision.requestDetectPerson(detectPersonConfig);
    }

    public void switchToFaceDetection() {
        HideAndSeek.robotAPI.vision.cancelDetectPerson();
        HideAndSeek.robotAPI.vision.requestDetectFace(detectFaceConfig);
    }

    public void stop() {
        HideAndSeek.robotAPI.vision.cancelDetectPerson();
        HideAndSeek.robotAPI.vision.cancelDetectFace();
        HideAndSeek.navigation.stopAllMovement();
        state = SeekingState.NOT_STARTED;
    }
}
