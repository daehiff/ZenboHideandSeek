package com.example.zenbohideandseek

import android.os.Handler
import com.asus.robotframework.API.RobotAPI
import com.asus.robotframework.API.SpeakConfig
import com.asus.robotframework.API.VisionConfig
import com.asus.robotframework.API.WheelLights
import com.asus.robotframework.API.results.DetectPersonResult

class Seeking(val robotAPI: RobotAPI) {
    val DOMAIN = "85848A87233A4268AE9E6DBE15A73273"

    enum class SeekingState {
        NOT_STARTED, SEARCHING_FOR_TARGET, ASKING_TO_PLAY, COUNTDOWN, SEEKING
    }
    var state: SeekingState = SeekingState.NOT_STARTED
    var denialCount = 0
    var answerWaitIterations = 0
    var countdownSeconds = 5

    val debugMode = true
    val detectPersonConfig = VisionConfig.PersonDetectConfig()
    val detectFaceConfig = VisionConfig.FaceDetectConfig()

    init {
        // Set-up the vision detections parameters
        detectPersonConfig.interval = 3
        detectFaceConfig.interval = 3
        detectPersonConfig.enableDebugPreview = debugMode
        detectFaceConfig.enableDebugPreview = debugMode
        detectPersonConfig.enableDetectHead = true
    }

    fun handlePersonDetection(people: List<DetectPersonResult>) {
        when (state) {
            SeekingState.SEARCHING_FOR_TARGET -> {
                if (people.size > 1) {
                    robotAPI.robot.speak("There are too many of you!")
                    blinkAllLights(0x00ff0000, 5, 20)
                } else if (people.size == 1) {

                    val speakingConfig = SpeakConfig()
                    speakingConfig.domain(DOMAIN)
                    speakingConfig.timeout(10f)
                    speakingConfig.listenCurrentDomain(true)

                    state = SeekingState.ASKING_TO_PLAY
                    robotAPI.robot.stopSpeakAndListen()
                    robotAPI.robot.speakAndListen("Do you want to play hide and seek with me?",
                            speakingConfig)
                }
            }
            SeekingState.ASKING_TO_PLAY -> {
                if (people.isEmpty()) {
                    robotAPI.robot.speak("Where did you go?")
                    robotAPI.robot.speak("Don't you know it's a bit rude to walk away from the conversation?")
                    state = SeekingState.ASKING_TO_PLAY
                }
                answerWaitIterations++
//                if (answerWaitIterations > 20) {
//                    answerWaitIterations = 0
//
//                    robotAPI.robot.speak("Hey, I asked you something!")
//                }
            }
            SeekingState.SEEKING -> {
                if (people.size == 1) {
                    robotAPI.robot.speak("Haha, I found you!")
                    blinkAllLights(0x001111ff, 5, 50)
                }
                state = SeekingState.NOT_STARTED
            }
        }
    }

    fun resetState() {
        state = SeekingState.NOT_STARTED
    }

    fun startLookingForTarget() {
        println("Zenbo will now look for a person and ask him.")
//        robotAPI.robot.setExpression(RobotFace.EXPECTING)
        state = SeekingState.SEARCHING_FOR_TARGET
    }

    fun handleUserAnswer(decision: Boolean) {
        if (decision) {
            blinkAllLights(0x0011ff00, 2, 10)
            if (denialCount < 2) {
//                robotAPI.utility.playEmotionalAction(RobotFace.ACTIVE, Utility.PlayAction.Head_up_1)
                robotAPI.robot.speak("Okay! Hide now, I'm counting until five.")
                startCountdown(5)
            } else {
//                robotAPI.utility.playEmotionalAction(RobotFace.SERIOUS, Utility.PlayAction.Head_up_1)
                robotAPI.robot.speak("Well good for you, but I don't want to play with you anymore now!")
                state = SeekingState.NOT_STARTED
            }
        } else {
//            robotAPI.utility.playEmotionalAction(RobotFace.INNOCENT, Utility.PlayAction.Head_down_1)
            robotAPI.robot.speak(if (denialCount == 0) "Huh. Ok then." else "Again? Ok then.")
            blinkAllLights(0x00ff8800, 2, 10)
            denialCount++
        }
    }

    fun handleApology() {
//        robotAPI.robot.setExpression(RobotFace.ACTIVE)
        robotAPI.robot.speak("It's okay!")

        blinkAllLights(0x000055ff, 3, 50)

        denialCount = 0
    }

    fun startCountdown(seconds: Int) {
        state = SeekingState.COUNTDOWN
        countdownSeconds = seconds
        announceCountdown()
    }

    private fun announceCountdown() {
        robotAPI.robot.speak(countdownSeconds.toString())
        blinkAllLights(0x00005500, 1, 50)

        if (countdownSeconds > 0) {
            Handler().postDelayed({ announceCountdown() }, 2000)
        } else {
            robotAPI.robot.speak("I'm coming!")
            state = SeekingState.SEEKING
        }

        countdownSeconds--
    }

    private fun blinkAllLights(color: Int, cycles: Int, speed: Int) {
        robotAPI.wheelLights.setColor(WheelLights.Lights.SYNC_BOTH, 0xfffffff, color)
        robotAPI.wheelLights.startBlinking(WheelLights.Lights.SYNC_BOTH, 0xfffffff, speed, speed, cycles)
    }

    fun switchToPersonDetection() {
        robotAPI.vision.cancelDetectFace()
        robotAPI.vision.requestDetectPerson(detectPersonConfig)
    }

    fun switchToFaceDetection() {
        robotAPI.vision.cancelDetectPerson()
        robotAPI.vision.requestDetectFace(detectFaceConfig)
    }

    fun stop() {
        robotAPI.vision.cancelDetectPerson()
        robotAPI.vision.cancelDetectFace()
        state = SeekingState.NOT_STARTED
    }
}