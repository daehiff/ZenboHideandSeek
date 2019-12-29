package com.asus.hideandseek;

import android.os.Handler;

import java.util.List;

import com.asus.robotframework.API.RobotFace;
import com.asus.robotframework.API.SpeakConfig;
import com.asus.robotframework.API.Utility;
import com.asus.robotframework.API.VisionConfig;
import com.asus.robotframework.API.WheelLights;
import com.asus.robotframework.API.results.DetectFaceResult;
import com.asus.robotframework.API.results.DetectPersonResult;
import com.asus.robotframework.API.results.RecognizePersonResult;

import static com.asus.hideandseek.HideAndSeek.robotAPI;
import static com.asus.hideandseek.HideAndSeek.seeking;
import static com.asus.hideandseek.HideAndSeek.speaking;
import static com.asus.hideandseek.HideAndSeek.statusText;
import static com.asus.hideandseek.Seeking.SeekingState.ASKING_TO_PLAY;
import static com.asus.hideandseek.Seeking.SeekingState.SEEKING;
import static com.asus.robotframework.API.SpeakConfig.MODE_DEFAULT;

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
    private VisionConfig.PersonRecognizeConfig personRecognizeConfig = new VisionConfig.PersonRecognizeConfig();
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
        personRecognizeConfig.interval = 2;
        personRecognizeConfig.enableCandidateObj = false;
        personRecognizeConfig.enableDebugPreview = debugMode;
        personRecognizeConfig.enableDetectHead = true;
    }

    public void handlePersonDetection(List<RecognizePersonResult> people) {
        switch (state) {
            case SEARCHING_FOR_TARGET:
                if (people.size() > 1) {
                    HideAndSeek.speaking.say("There are too many of you!");
                    robotAPI.robot.setExpression(RobotFace.SHOCKED);
                    blinkAllLights(0x00ff0000, 5, 20);
                } else if (people.size() == 1) {
                    robotAPI.robot.setExpression(RobotFace.CONFIDENT);
                    HideAndSeek.speaking.askForPlay();
                }
                break;

            case SEEKING:
                if (people.size() == 1) {
                    String uuid = people.get(0).getUuid();
                    String name = robotAPI.contacts.family.getName(uuid);
                    boolean isValidName = name != null && uuid.charAt(0) != '-';
                    String message = (isValidName ? "Hey " + name + "!" : "Hey you there!") + " I found you!";
                    HideAndSeek.robotAPI.robot.speak(message);
                    robotAPI.robot.setExpression(RobotFace.HAPPY);
                    blinkAllLights(0x001111ff, 5, 50);
                    stop();
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
        switchToPersonDetection();
        state = SeekingState.SEARCHING_FOR_TARGET;
    }

    public void handleUserAnswer(Boolean decision) {
        if (decision) {
            blinkAllLights(0x0011ff00, 2, 10);
            if (denialCount < 2) {
//                HideAndSeek.robotAPI.utility.playEmotionalAction(RobotFace.ACTIVE, Utility.PlayAction.Head_up_1);
                HideAndSeek.robotAPI.robot.speak("I'm counting until five.");
                robotAPI.robot.setExpression(RobotFace.TIRED);
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
//            state = SEEKING;
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
        HideAndSeek.robotAPI.vision.requestRecognizePerson(personRecognizeConfig);
    }

    public void switchToFaceDetection() {
        HideAndSeek.robotAPI.vision.cancelRecognizePerson();
        HideAndSeek.robotAPI.vision.requestDetectFace(detectFaceConfig);
    }

    public void stop() {
        HideAndSeek.robotAPI.vision.cancelRecognizePerson();
        HideAndSeek.robotAPI.vision.cancelDetectFace();
        HideAndSeek.navigation.stopAllMovement();
        state = SeekingState.NOT_STARTED;
    }
}
