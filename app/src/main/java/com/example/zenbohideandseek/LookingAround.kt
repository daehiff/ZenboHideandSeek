package com.example.zenbohideandseek

import com.asus.robotframework.API.RobotAPI
import com.asus.robotframework.API.VisionConfig
import com.asus.robotframework.API.results.RecognizePersonResult

var seenPeople: List<String> = emptyList()
var seekingSeenPeople: List<String> = emptyList()
var lastSeen: List<String> = emptyList()
var currentTarget: String? = null
var seeking: Boolean = false

/**
 * Starts the detection of people
 */
fun startLookingForPeople (robotAPI: RobotAPI) {
    val config = VisionConfig.PersonRecognizeConfig()
    config.interval = 2
    config.enableDebugPreview = true
    config.enableDetectHead = true

    robotAPI.vision.requestRecognizePerson(config)
}

/**
 * Finds a target to seek for
 */
fun findSeekTarget (robotAPI: RobotAPI) {
    seekingSeenPeople = emptyList()

    if (lastSeen.isEmpty()) {
        robotAPI.robot.speak("I can't see anyone.")
        currentTarget = null
        return
    }
    if (lastSeen.size > 1) {
        robotAPI.robot.speak("There is too many of you.")
        currentTarget = null
        return
    }

    currentTarget = lastSeen[0]
}

/**
 * Starts the seeking
 */
fun startSeeking (robotAPI: RobotAPI) {
    robotAPI.robot.speak("I'm looking for you now!")
    seeking = true
}

fun resetLists () {
    lastSeen = emptyList()
    seenPeople = emptyList()
    seekingSeenPeople = emptyList()
    currentTarget = null
    seeking = false
}

/**
 * PersonRecognition callback handler
 */
fun handlePersonRecognition (robotAPI: RobotAPI, people: List<RecognizePersonResult>) {
    people.forEach {
        println(seenPeople.toString())
        if (!seenPeople.contains(it.uuid)) {
            robotAPI.robot.speak("Yo")
            println("_NEW " + it.uuid)
            seenPeople = seenPeople.plusElement(it.uuid)
        }

        if (seeking) {
            if (it.uuid == currentTarget) {
                robotAPI.robot.speak("There! I found you!")
                seeking = false
            } else if (!seekingSeenPeople.contains(it.uuid)) {
                robotAPI.robot.speak("Nope")
                seekingSeenPeople = seekingSeenPeople.plusElement(it.uuid)
            }
        }

        println("SEEN " + it.uuid)
    }

    lastSeen = people.map { it.uuid }
}

fun stopLookingForPeople (robotAPI: RobotAPI) {
    robotAPI.vision.cancelDetectPerson()
    println("Stopped looking")
}