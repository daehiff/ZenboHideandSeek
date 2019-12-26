package com.example.zenbohideandseek

import com.asus.robotframework.API.RobotAPI
import com.asus.robotframework.API.RobotCallback
import com.asus.robotframework.API.SpeakConfig
import org.json.JSONArray
import org.json.JSONObject

class Speaking(val robotAPI: RobotAPI) {
    val DOMAIN = "85848A87233A4268AE9E6DBE15A73273"

    init {
        robotAPI.robot.jumpToPlan(DOMAIN, "lanuchHelloWolrd_Plan")
    }


    private fun getUserSentences(userUtteranceEvent: JSONObject): MutableList<String> {
        var utterancesString = userUtteranceEvent.getString("user_utterance")
        val utterances = JSONArray(utterancesString)
        var count = utterances.length()
        var sentences: MutableList<String> = mutableListOf()

        for (idx in 0..(count - 1)) {
            val utterance = utterances.getJSONObject(idx)
            val csrType = utterance.getString("CsrType")

            if (csrType == "vocon") {
                val result = utterance.getString("result")
                sentences.add(result)
            }
        }

        return sentences
    }

    fun didUserMentioned(sentences: MutableList<String>, word: String): Boolean {
        for (sentence in sentences) {
            if (sentence.contains(word)) {
                return true
            }
        }
        return false

    }

    fun didUserMentioned(userUtteranceEvent: JSONObject, word: String): Boolean {
        return didUserMentioned(getUserSentences(userUtteranceEvent), word)
    }
}