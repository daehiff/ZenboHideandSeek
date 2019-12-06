package com.example.zenbohideandseek

import androidx.appcompat.app.AppCompatActivity

import android.os.Bundle
import android.util.Log
import com.asus.robotframework.API.*
import com.asus.robotframework.API.results.Location

import org.json.JSONObject
import java.sql.DriverManager
import java.sql.DriverManager.println

class MainActivity : AppCompatActivity() {

    lateinit var robotAPI: RobotAPI


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        robotAPI = RobotAPI(applicationContext, robotCallback)
        robotAPI.robot.setExpression(RobotFace.WORRIED,
                "I cannot find you after 0 Seconds")

        //robotAPI.motion.moveBody(1f, 1f, 180);
        println("----- Location ----------")
        Log.i("ZENBO_ONC", robotAPI.slam.activeLocalization(10.0).toString())
    }

    companion object {

        var robotCallback: RobotCallback = object : RobotCallback() {
            override fun onResult(cmd: Int, serial: Int, err_code: RobotErrorCode?, result: Bundle?) {
                // TODO i think here comes the command stuff? so maybe we can get our location here?
                super.onResult(cmd, serial, err_code, result)
                val tmp = result?.getBundle("RESULT")?.getParcelable<Location>("LOCATION")
                tmp.let { res ->
                    Log.i("ZENBO_ONRES", res.toString())
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
                println(jsonObject.toString())
            }

            override fun onSpeakComplete(s: String, s1: String) {
                println(s)
            }

            override fun onEventUserUtterance(jsonObject: JSONObject) {

            }

            override fun onResult(jsonObject: JSONObject) {

            }

            override fun onRetry(jsonObject: JSONObject) {

            }
        }
    }
}