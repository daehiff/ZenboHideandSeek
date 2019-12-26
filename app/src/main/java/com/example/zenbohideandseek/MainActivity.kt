package com.example.zenbohideandseek

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.asus.robotframework.API.*
import com.asus.robotframework.API.results.DetectFaceResult
import com.asus.robotframework.API.results.DetectPersonResult
import com.asus.robotframework.API.results.RecognizePersonResult
import org.json.JSONArray
import org.json.JSONObject


class MainActivity : AppCompatActivity() {

    lateinit var robotAPI: RobotAPI
    lateinit var seeking: Seeking
    lateinit var speaking: Speaking
    lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        statusText = findViewById(R.id.status)

        // Initialize the robot
        robotAPI = RobotAPI(applicationContext, robotCallback)
        robotAPI.robot.registerListenCallback(robotListenCallback)

        // Hide his face ( -!- important to see the UI )
        robotAPI.robot.setExpression(RobotFace.HIDEFACE)

        // Intialize behaviours
        seeking = Seeking(robotAPI)
        speaking = Speaking(robotAPI)

        // Turn off his wheels
        robotAPI.wheelLights.turnOff(WheelLights.Lights.SYNC_BOTH, 0xfffffff)

        // Disable some of his behaviours
        robotAPI.robot.setVoiceTrigger(false)
        robotAPI.robot.setPressOnHeadAction(false)

        // Register actions to UI buttons for debugging
        val resetButton = findViewById<Button>(R.id.startdetection)
        resetButton.setOnClickListener {
            seeking.switchToPersonDetection()
            statusText.text = seeking.state.toString()
        }

        val cancelButton = findViewById<Button>(R.id.canceltheshit)
        cancelButton.setOnClickListener {
            seeking.stop()
            statusText.text = "Stopped detections"
        }

        val startButton = findViewById<Button>(R.id.start)
        startButton.setOnClickListener {
            seeking.startLookingForTarget()
            statusText.text = seeking.state.toString()
        }
    }

    override fun onResume() {
        super.onResume()

        statusText.text = "Resumed, ready"
    }

    override fun onPause() {
        super.onPause()

        seeking.stop()
        robotAPI.robot.stopSpeakAndListen()
        statusText.text = "Paused"
    }

    override fun onStop() {
        super.onStop()

        seeking.stop()
        robotAPI.robot.stopSpeakAndListen()
    }

    var robotCallback: RobotCallback = object : RobotCallback() {
        override fun onDetectFaceResult(resultList: MutableList<DetectFaceResult>) {
            resultList.forEach {
                println("detectFace\t\t\ttid %d\t\tuuid %s\t\thpconf %d\t\t%s\t\t%s".format(it.trackID, it.uuid,
                        it.headPoseConfidence, if (it.hasValidDepth()) "valid depth" else "invalid depth",
                        if (it.isCandidateObj) "candidate" else "full"))
            }

        }

        override fun onDetectPersonResult(resultList: MutableList<DetectPersonResult>) {
            resultList.forEach {
                println("detectPerson\t\t\ttid %d\t\ttconf %f\t\t%s".format(it.trackID,
                        it.trackConf, if (it.hasValidDepth()) "valid depth" else "invalid depth"))
            }
            seeking.handlePersonDetection(resultList)
            statusText.text = seeking.state.toString()
        }

//        override fun onRecognizePersonResult(resultList: List<RecognizePersonResult>) {
//            resultList.forEach {
//                println("recognizePerson\t\t\ttid %d\t\tuuid %s\t\t%s\t\thpconf %d".format(it.trackID, it.uuid,
//                        if (it.isCandidateObj) "candidate" else "full", it.headPoseConfidence,
//                        if (it.hasValidDepth()) "valid depth" else "invalid depth"))
//            }
//
//            handlePersonRecognition(robotAPI, resultList)
//            refreshDebugDisplay()
//            statusText.text =
//                    "-last frame-\ntimestamp: " + System.currentTimeMillis().toString() +
//                    "\ndetected: " + resultList.size.toString() + " people"
//        }

    }

    var robotListenCallback: RobotCallback.Listen = object : RobotCallback.Listen {
        override fun onSpeakComplete(p0: String?, p1: String?) {
            seeking.resetState()
        }
        override fun onVoiceDetect(p0: JSONObject?) { }
        override fun onFinishRegister() { }
        override fun onResult(p0: JSONObject?) { }
        override fun onRetry(p0: JSONObject?) { }

        override fun onEventUserUtterance(results: JSONObject?) {
            if (results != null) {
                if (results.has("event_user_utterance")) {
                    val userUtterance = results.getJSONObject("event_user_utterance")
                    println(userUtterance)

                    if (speaking.didUserMentioned(userUtterance, "yes")) {
                        seeking.handleUserAnswer(true)
                    } else if (speaking.didUserMentioned(userUtterance, "no")) {
                        seeking.handleUserAnswer(false)
                    } else if (speaking.didUserMentioned(userUtterance, "sorry")) {
                        seeking.handleApology()
                    }
                }
            }
            statusText.text = seeking.state.toString()
        }
    }
}
