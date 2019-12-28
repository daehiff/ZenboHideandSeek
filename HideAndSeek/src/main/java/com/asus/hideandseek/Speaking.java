package com.asus.hideandseek;

import android.util.Log;

import com.asus.robotframework.API.RobotAPI;
import com.asus.robotframework.API.SpeakConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Speaking {
    private SpeakConfig config = new SpeakConfig();

    public Speaking() {
        config.timeout(10f);
        config.listenCurrentDomain(true);
    }

    public void say(String sentence) {
        // TODO: THIS HASN'T BEEN FIXED YET!
        HideAndSeek.robotAPI.robot.stopSpeakAndListen();
        HideAndSeek.robotAPI.robot.speak(sentence);
    }

    public void askForPlay() {
        // TODO: Temporarily disabled until broken askForPlay() shit is fixed
        HideAndSeek.robotAPI.robot.stopSpeakAndListen();
        HideAndSeek.robotAPI.robot.speak("Do you want to play? Press the buttons to answer.");
//        HideAndSeek.robotAPI.robot.speakAndListen("Do you want to play hide and seek with me?",
//                config);
    }

    public void handleUserConversation(Seeking seeking, JSONObject jsonObject) {
        switch (seeking.state) {
            case ASKING_TO_PLAY:
                ArrayList sentences = getUserSentences(jsonObject);

                if (didUserSay(sentences, "yes")) {
                    seeking.handleUserAnswer(true);
                } else if (didUserSay(sentences, "no")) {
                    seeking.handleUserAnswer(false);
                } else if (didUserSay(sentences, "sorry")) {
                    seeking.handleApology();
                } else {
                    say("Sorry, I didn't understand you.");
                    seeking.state = Seeking.SeekingState.SEARCHING_FOR_TARGET;
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

    public boolean didUserSay(ArrayList<String> sentences, String word){
        for (int i=0;i<sentences.size();i++) {
            if (sentences.get(i).contains(word)) {
                return true;
            }
        }
        return false;
    }

    public boolean didUserSay(JSONObject userUtteranceEvent, String word) {
        return didUserSay(getUserSentences(userUtteranceEvent), word);
    }
}
