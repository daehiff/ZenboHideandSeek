package com.robot.asus.robotactivity

import android.app.Activity
import android.os.Bundle
import android.view.View

import com.asus.robotframework.API.RobotAPI
import com.asus.robotframework.API.RobotCallback


open class RobotActivity : Activity() {
    lateinit var robotAPI: RobotAPI
    lateinit var robotCallback:RobotCallback
    lateinit var robotListenCallback:RobotCallback.Listen

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.robotAPI = RobotAPI(applicationContext, robotCallback)
    }

    override fun onPause() {
        super.onPause()
        robotAPI.robot.unregisterListenCallback()
    }

    override fun onResume() {
        super.onResume()
        if (robotListenCallback != null)
            robotAPI.robot.registerListenCallback(robotListenCallback!!)
        val decorView = window.decorView
        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }

    override fun onDestroy() {
        super.onDestroy()
        robotAPI.release()
    }
}
