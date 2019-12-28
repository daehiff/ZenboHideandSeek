package com.asus.hideandseek;

import android.util.Log;
import java.util.List;

import com.asus.robotframework.API.RobotAPI;
import com.asus.robotframework.API.SpeakConfig;
import com.asus.robotframework.API.VisionConfig;
import com.asus.robotframework.API.WheelLights;
import com.asus.robotframework.API.results.DetectFaceResult;
import com.asus.robotframework.API.results.DetectPersonResult;

import static com.asus.hideandseek.Seeking.SeekingState.ASKING_TO_PLAY;
import static java.sql.DriverManager.println;

public class Seeking {
    public String userId = null;

    private int findCount = 0;
    private RobotAPI robotAPI;
    private Speaking speaking;

    public enum SeekingState {
        NOT_STARTED, SEARCHING_FOR_TARGET, ASKING_TO_PLAY, COUNTDOWN, SEEKING
    }

    public SeekingState state = SeekingState.NOT_STARTED;
    public int denialCount = 0;
    public int answerWaitIterations = 0;
    public int countdownSeconds = 5;

    public boolean debugMode = true;
    public VisionConfig.PersonDetectConfig detectPersonConfig = new VisionConfig.PersonDetectConfig();
    public VisionConfig.FaceDetectConfig detectFaceConfig = new VisionConfig.FaceDetectConfig();

    Seeking(RobotAPI robotAPI, Speaking speaking) {
        // Set-up the vision detections parameters
        this.robotAPI = robotAPI;
        this.speaking = speaking;
        detectPersonConfig.interval = 3;
        detectFaceConfig.interval = 3;
        detectPersonConfig.enableDebugPreview = debugMode;
        detectFaceConfig.enableDebugPreview = debugMode;
        detectPersonConfig.enableDetectHead = true;
    }

    public void handlePersonDetection(List<DetectFaceResult> people, String userId) {
        switch (state) {
            case SEARCHING_FOR_TARGET:
                Log.d("Searching for target", "handlePersonDetection: ");
                if (people.size() > 1) {
                    robotAPI.robot.speak("There are too many of you!");
                    blinkAllLights(0x00ff0000, 5, 20);
                } else if (people.size() == 1) {
                    state = ASKING_TO_PLAY;
                    speaking.askForPlay();
                }
                break;

            case ASKING_TO_PLAY:
                Log.d("Asking to play", "handlePersonDetection: ");
                if (people.isEmpty()) {
                    robotAPI.robot.speak("Where did you go?");
                    robotAPI.robot.speak("Don't you know it's a bit rude to walk away from the conversation?");
                    state = ASKING_TO_PLAY;
                }
                answerWaitIterations++;
                break;

            case SEEKING:
                if (findCount == 0){
                    findCount++;
                }
                else {
                    Log.d("SEEKING", "handlePersonDetection: ");
                    if (people.size() == 1 && userId.equals(people.get(0).getUuid())) {
                        robotAPI.robot.speak("Haha, I found you!");
                        blinkAllLights(0x001111ff, 5, 50);
                    }
                    state = SeekingState.NOT_STARTED;
                }
                break;

            case NOT_STARTED:
                break;
            default:
                Log.d("Seeking stuff", "default value");
        }
    }

    public void handlePersonDetection(List<DetectPersonResult> people) {
        switch (state) {
            case SEARCHING_FOR_TARGET:
                Log.d("Searching for target", "handlePersonDetection: ");
                if (people.size() > 1) {
                    robotAPI.robot.speak("There are too many of you!");
                    blinkAllLights(0x00ff0000, 5, 20);
                } else if (people.size() == 1) {
                    state = ASKING_TO_PLAY;
                    speaking.askForPlay();
                }
                break;

            case ASKING_TO_PLAY:
                Log.d("Asking to play", "handlePersonDetection: ");
                if (people.isEmpty()) {
                    robotAPI.robot.speak("Where did you go?");
                    robotAPI.robot.speak("Don't you know it's a bit rude to walk away from the conversation?");
                    state = ASKING_TO_PLAY;
                }
                answerWaitIterations++;
                break;

            case SEEKING:
                if (findCount == 0){
                    findCount++;
                }else {
                    Log.d("SEEKING", "handlePersonDetection: ");
                    if (people.size() == 1 && false) {
                        robotAPI.robot.speak("Haha, I found you!");
                        blinkAllLights(0x001111ff, 5, 50);
                    }
                    state = SeekingState.NOT_STARTED;
                }
                break;

            case NOT_STARTED:
                break;
            default:
                Log.d("Seeking stuff", "default value");
        }
    }

    public void resetState() {
        state = SeekingState.NOT_STARTED;
    }

    public void startLookingForTarget() {
        println("Zenbo will now look for a person and ask him.");
//        robotAPI.robot.setExpression(RobotFace.EXPECTING)
        state = SeekingState.SEARCHING_FOR_TARGET;
        robotAPI.robot.speak("Ok ! Let's start the countdown", SpeakConfig.MODE_DEFAULT);
        startCountdown(10);
        LookingMovement();

    }

    private void LookingMovement() {
        //TODO
        //Move around and stop at some places JULIA

        switchToPersonDetection();
    }

    public void handleUserAnswer(Boolean decision) {
        if (decision) {
            blinkAllLights(0x0011ff00, 2, 10);
            if (denialCount < 2) {
//                robotAPI.utility.playEmotionalAction(RobotFace.ACTIVE, Utility.PlayAction.Head_up_1);
                robotAPI.robot.speak("Okay! Hide now, I'm counting until five.");
                startCountdown(5);
            } else {
//                robotAPI.utility.playEmotionalAction(RobotFace.SERIOUS, Utility.PlayAction.Head_up_1);
                robotAPI.robot.speak("Well good for you, but I don't want to play with you anymore now!");
                state = SeekingState.NOT_STARTED;
            }
        } else {
//            robotAPI.utility.playEmotionalAction(RobotFace.INNOCENT, Utility.PlayAction.Head_down_1);
            if (denialCount == 0) {
                robotAPI.robot.speak("Huh. Ok then.");
            }
            else {
                robotAPI.robot.speak("Again ? Ok then.");
            }
            blinkAllLights(0x00ff8800, 2, 10);
            denialCount++;
        }
    }

    public void handleApology() {
//        robotAPI.robot.setExpression(RobotFace.ACTIVE)
        robotAPI.robot.speak("It's okay!");

        blinkAllLights(0x000055ff, 3, 50);

        denialCount = 0;
    }

    public void startCountdown(int seconds) {
        stop();
        state = SeekingState.COUNTDOWN;
        countdownSeconds = seconds;
        announceCountdown();
    }

    private void announceCountdown() {

        for(int i = countdownSeconds; i>0; i--){
            robotAPI.robot.speak(Integer.toString(i));
            blinkAllLights(0x00005500, 1, 50);
            try {
                Log.d("countdown", "announceCountdown: " + i);
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.d("Countdown bug", e.toString());
            }
        }
        robotAPI.robot.speak("I'm coming!");
        state = SeekingState.SEEKING;
        stop();
        switchToFaceDetection();
    }

    private void blinkAllLights(int color, int cycles, int speed) {
        robotAPI.wheelLights.setColor(WheelLights.Lights.SYNC_BOTH, 0xfffffff, color);
        robotAPI.wheelLights.startBlinking(WheelLights.Lights.SYNC_BOTH, 0xfffffff, speed, speed, cycles);
    }

    public void switchToPersonDetection() {
        robotAPI.vision.cancelDetectFace();
        robotAPI.vision.requestDetectPerson(detectPersonConfig);
    }

    public void switchToFaceDetection() {
        robotAPI.vision.cancelDetectPerson();
        robotAPI.vision.requestDetectFace(detectFaceConfig);
    }

    public void stop() {
        robotAPI.vision.cancelDetectPerson();
        robotAPI.vision.cancelDetectFace();
        //state = SeekingState.NOT_STARTED;
    }
}
