package com.example.zenbohideandseek

import androidx.appcompat.app.AppCompatActivity

import android.os.Bundle
import android.util.Log
import com.asus.robotframework.API.*

import org.json.JSONObject
import java.sql.DriverManager
import java.sql.DriverManager.println

class MainActivity : AppCompatActivity() {

    lateinit var robotAPI: RobotAPI


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        robotAPI = RobotAPI(applicationContext, robotCallback)
        robotAPI.robot.speakAndListen("Do you want to play Hide and Seek ?", SpeakConfig())
    }

    override fun onPause() {
        super.onPause()
        robotAPI.robot.stopSpeakAndListen()
    }

    override fun onResume() {
        super.onResume()

        // close faical
        robotAPI.robot.setExpression(RobotFace.HIDEFACE)

        // listen user utterance
        robotAPI.robot.speakAndListen("Do you want to play Hide and Seek ?", SpeakConfig().timeout(10f))

    }

        var robotCallback: RobotCallback = object : RobotCallback() {
            override fun onResult(cmd: Int, serial: Int, err_code: RobotErrorCode?, result: Bundle?) {
                // TODO i think here comes the command stuff? so maybe we can get our location here?
                super.onResult(cmd, serial, err_code, result)
                result?.let { res ->
                    Log.i("ZENBO", res.toString())
                } ?: run {
                    Log.i("ZENBO", "BUNDLE IS NULL!")
                }
            }

            override fun onStateChange(cmd: Int, serial: Int, err_code: RobotErrorCode?, state: RobotCmdState?) {
                super.onStateChange(cmd, serial, err_code, state)
            }

            override fun initComplete() {
                super.initComplete()

            }
        }

        var robotListenCallback: RobotCallback.Listen = object : RobotCallback.Listen {
            override fun onFinishRegister() {

            }

            override fun onVoiceDetect(jsonObject: JSONObject) {
            }

            override fun onSpeakComplete(s: String, s1: String) {
                println(s)
            }

            override fun onEventUserUtterance(jsonObject: JSONObject) {
                val text: String
                text = "onEventUserUtterance: $jsonObject"
                Log.d("User Utterance", text)
            }

            override fun onResult(jsonObject: JSONObject) {
                val text = "onResult: $jsonObject"
                Log.d("Listening text :", text)


                val sIntentionID = RobotUtil.queryListenResultJson(jsonObject, "IntentionId")
                Log.d("Listening Intention", "Intention Id = $sIntentionID")

                if (sIntentionID == "helloWorld") {
                    val sSluResultCity = RobotUtil.queryListenResultJson(jsonObject, "myCity1", null)
                    Log.d("Listening city", "Result City = " + sSluResultCity!!)
                }

            }

            override fun onRetry(jsonObject: JSONObject) {

            }
        }
}
