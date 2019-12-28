package com.asus.robotdevsample;

import android.util.Log;

import com.asus.robotframework.API.RobotAPI;
import com.asus.robotframework.API.RobotCallback;
import com.asus.robotframework.API.SpeakConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import kotlin.jvm.internal.markers.KMutableList;

import static com.asus.robotdevsample.Seeking.SeekingState.ASKING_TO_PLAY;
import static com.asus.robotdevsample.Seeking.SeekingState.*;

public class Speaking {
    private RobotAPI robotAPI;
    public final String DOMAIN = "85848A87233A4268AE9E6DBE15A73273";

    public Speaking(RobotAPI robotAPI) {
        this.robotAPI = robotAPI;
        robotAPI.robot.jumpToPlan(DOMAIN, "lanuchHelloWolrd_Plan");
    }

    public void handleUserConversation(Seeking seeking, JSONObject jsonObject){
        try {
            switch (seeking.state) {
                case ASKING_TO_PLAY:
                    String currentUserUtterance = jsonObject.getJSONObject("event_user_utterance").getString("user_utterance");
                    if (currentUserUtterance.contains("yes")) {
                        Log.d("userUtteranceContainYes", "It contains Yes");
                        this.robotAPI.robot.stopSpeakAndListen();
                        seeking.startLookingForTarget();
                    } else {
                        Log.d("userUtteranceContains", "It doesn't contain yes");
                        break;
                    }
                    break;

                    default:
                        Log.d("Not asking to play", "Not asking to play");
                        break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private ArrayList getUserSentences(JSONObject userUtteranceEvent)  {
        String utterancesString = null;
        try {
            utterancesString = userUtteranceEvent.getString("user_utterance");
            JSONArray utterances = new JSONArray(utterancesString);
            int count = utterances.length();
            ArrayList sentences = new ArrayList<String>();

            for(int i=0;i<count-1;i++){
                JSONObject utterance = utterances.getJSONObject(i);
                String csrType = utterance.getString("CsrType");

                if(csrType == "vocon"){
                    String result = utterance.getString("result");
                    sentences.add(result);
                }

            }
            return sentences;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean didUserMentioned(ArrayList<String> sentences, String word){
        for (int i=0;i<sentences.size();i++) {
            if (sentences.get(i).contains(word)) {
                return true;
            }
        }
        return false;
    }

    public boolean didUserMentioned(JSONObject userUtteranceEvent, String word) {
        return didUserMentioned(getUserSentences(userUtteranceEvent), word);
    }


}
