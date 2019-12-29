package com.asus.hideandseek;

import android.util.Log;

import com.asus.robotframework.API.RobotAPI;
import com.asus.robotframework.API.SpeakConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.asus.hideandseek.HideAndSeek.robotAPI;
import static com.asus.hideandseek.HideAndSeek.seeking;
import static com.asus.robotframework.API.SpeakConfig.MODE_DEFAULT;
import static com.asus.robotframework.API.SpeakConfig.MODE_FOREVER;

public class Speaking {
    private SpeakConfig config = new SpeakConfig();

    public Speaking() {
        config.timeout(10f);
        config.listenCurrentDomain(true);
    }

    public void say(String sentence) {
        // TODO: THIS HASN'T BEEN FIXED YET!
        robotAPI.robot.stopSpeakAndListen();
        robotAPI.robot.speak(sentence);
    }

    public void askForPlay() {
        robotAPI.robot.stopSpeakAndListen();
        robotAPI.robot.speakAndListen("Do you want to play hide and seek with me?", MODE_FOREVER);
        seeking.state = Seeking.SeekingState.ASKING_TO_PLAY;
    }

    public void handleUserConversation(Seeking seeking, JSONObject jsonObject) {
        String sentences = null;
        try {
            sentences = jsonObject.getJSONObject("event_user_utterance").getString("user_utterance");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        switch (seeking.state) {
            case ASKING_TO_PLAY:
                if (didUserSay(sentences, Arrays.asList("yes", "sure", "i do"))) {
                    seeking.handleUserAnswer(true);
                } else if (didUserSay(sentences, Arrays.asList("no", "i do not", "i don't"))) {
                    seeking.handleUserAnswer(false);
                } else if (didUserSay(sentences, "sorry")) {
                    seeking.handleApology();
                } else if (sentences.equals(null) || sentences.contains("ERROR")){
                    robotAPI.robot.speakAndListen("", MODE_DEFAULT);
                    break;
                } else {
                    robotAPI.robot.speakAndListen("Sorry, I didn't understand you, say yes or no", MODE_FOREVER);
                }
                break;

            default:
                Log.d("HideAndSeek", "Not asking to play");
            break;
        }
    }

    private ArrayList<String> getUserSentences(JSONObject userUtteranceObject)  {
        try {
            JSONObject userUtteranceEvent = userUtteranceObject.getJSONObject("user_utterance_event");
            String utterancesString = userUtteranceEvent.getString("user_utterance");
            JSONArray utterances = new JSONArray(utterancesString);
            int count = utterances.length();
            ArrayList sentences = new ArrayList<String>();

            for(int i = 0; i < count - 1; i++){
                JSONObject utterance = utterances.getJSONObject(i);
                String csrType = utterance.getString("CsrType");

                if(csrType == "vocon"){
                    String result = utterance.getString("result");
                    sentences.add(result);
                }

            }
            return sentences;
        } catch (JSONException e) {
            return new ArrayList<String>();
        }
    }

    public boolean didUserSay(String sentences, String word){
        if (sentences.toLowerCase().contains(word)) {
            return true;
        }
        return false;
    }


    public boolean didUserSay(String sentences, List<String> words){
        for (String word : words) {
            if (sentences.toLowerCase().contains(word)) {
                return true;
            }
        }
        return false;
    }
}
