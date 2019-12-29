package com.asus.hideandseek;

import android.util.Log;

import com.asus.robotframework.API.RobotAPI;
import com.asus.robotframework.API.SpeakConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import static com.asus.hideandseek.HideAndSeek.robotAPI;
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
        // TODO: Temporarily disabled until broken askForPlay() shit is fixed
        robotAPI.robot.stopSpeakAndListen();
        robotAPI.robot.speak("Do you want to play? Press the buttons to answer.");
//        HideAndSeek.robotAPI.robot.speakAndListen("Do you want to play hide and seek with me?",
//                config);
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
                if (didUserSay(sentences, "yes")) {
                    seeking.handleUserAnswer(true);
                } else if (didUserSay(sentences, "no")) {
                    seeking.handleUserAnswer(false);
                } else if (didUserSay(sentences, "sorry")) {
                    seeking.handleApology();
                }else if (sentences.equals(null) || sentences.contains("ERROR")){
                    robotAPI.robot.speakAndListen("", MODE_DEFAULT);
                    break;
                }/* else {
                    seeking.state = Seeking.SeekingState.ASKING_TO_PLAY;
                    robotAPI.robot.speakAndListen("Sorry, I didn't understand you, say yes or no", MODE_DEFAULT);
                }*/
                break;

            case NOT_STARTED:
                if (didUserSay(sentences, "play")) {
                    robotAPI.robot.stopSpeakAndListen();
                    robotAPI.robot.speakAndListen("Do you want to play hide and seek ?", SpeakConfig.MODE_TIMEOUT);
                    seeking.state = Seeking.SeekingState.ASKING_TO_PLAY;
                }else{
                    robotAPI.robot.speakAndListen("I do not know this command", MODE_DEFAULT);
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
            if (sentences.contains(word)) {
                return true;
            }
        return false;
    }
}
