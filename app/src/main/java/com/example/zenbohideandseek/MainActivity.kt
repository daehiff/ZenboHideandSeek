package com.example.zenbohideandseek

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity

import android.os.Bundle
import android.os.Handler
import android.widget.Button
import android.widget.TextView
import com.asus.robotframework.API.*
import com.asus.robotframework.API.results.RecognizePersonResult


class MainActivity : AppCompatActivity() {

    lateinit var robotAPI: RobotAPI
    lateinit var statusText: TextView
    lateinit var lastseenText: TextView

    /**
     * Just updates the texts on the debug view
     */
    fun refreshDebugDisplay () {
        lastseenText.text =
                "Seen in last frame: " + lastSeen.toString() +
                        "\nCurrent seek target: " + currentTarget.toString() +
                        "\nSeen in seeking session: " + seekingSeenPeople.toString() +
                        "\nSeen all time: " + seenPeople.toString()
        lastseenText.setTextColor(if (seeking) Color.GREEN else Color.BLACK)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        robotAPI = RobotAPI(applicationContext, robotCallback)

        statusText = findViewById(R.id.status)
        lastseenText = findViewById(R.id.lastseen)
        val cancelButton = findViewById<Button>(R.id.canceltheshit)
        cancelButton.setOnClickListener {
            robotAPI.robot.speak("Fine")
            stopLookingForPeople(robotAPI)
        }

        val resetButton = findViewById<Button>(R.id.reset)
        resetButton.setOnClickListener {
            robotAPI.robot.speak("uh")
            resetLists()
            refreshDebugDisplay()
        }

        val startButton = findViewById<Button>(R.id.start)
        startButton.setOnClickListener {
            findSeekTarget(robotAPI)

            if (currentTarget !== null) {
                robotAPI.robot.speak("Ok! Hide! Quickly! I'm counting until five!")

                Handler().postDelayed({
                    startSeeking(robotAPI)
                    refreshDebugDisplay()
                }, 10000 // value in milliseconds
                )
            }
        }

        startLookingForPeople(robotAPI)
    }

    var robotCallback: RobotCallback = object : RobotCallback() {

        override fun onResult(cmd: Int, serial: Int, err_code: RobotErrorCode?, result: Bundle?) {
            super.onResult(cmd, serial, err_code, result)
        }

        override fun onStateChange(cmd: Int, serial: Int, err_code: RobotErrorCode?, state: RobotCmdState?) {
            super.onStateChange(cmd, serial, err_code, state)
        }

        override fun initComplete() {
            super.initComplete()
        }

        override fun onRecognizePersonResult(resultList: List<RecognizePersonResult>) {
            handlePersonRecognition(robotAPI, resultList)
            refreshDebugDisplay()
            statusText.text =
                    "-last frame-\ntimestamp: " + System.currentTimeMillis().toString() +
                    "\ndetected: " + resultList.size.toString() + " people"
        }
    }
}
