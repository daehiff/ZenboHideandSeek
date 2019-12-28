package com.asus.hideandseek;

import android.util.Log;

public class Navigation {

    public Navigation() {
    }

    public void startSearchingRoom () {
        // TODO: Julia
        HideAndSeek.robotAPI.robot.speak("I'm looking for you now!");
        Log.d("HideAndSeek", "Room search should commence now.");
    }

    public void stopAllMovement () {
        // TODO: Julia
        Log.d("HideAndSeek", "Robot should stop now.");
    }
}
