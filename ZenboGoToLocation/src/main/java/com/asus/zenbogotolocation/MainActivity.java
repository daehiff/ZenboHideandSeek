package com.asus.zenbogotolocation;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.asus.robotframework.API.RobotAPI;
import com.asus.robotframework.API.RobotCallback;
import com.asus.robotframework.API.RobotCmdState;
import com.asus.robotframework.API.RobotErrorCode;
import com.asus.robotframework.API.RobotFace;
import com.asus.robotframework.API.results.Location;
import com.asus.robotframework.API.results.RoomInfo;
import com.robot.asus.robotactivity.RobotActivity;

import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class Point {
    double x;
    double y;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

//    @Override
//    public boolean equals (Object o) {
//        Point point = (Point) o;
//        return ((int) this.x == (int) point.x) && ((int) this.y == (int) point.y);
//    }

      public boolean insidePolygon(ArrayList<Point> polygon) {
        // figure out if its inside
        int intersections = 0;
        Point prev = polygon.get(polygon.size() - 1);
        for (Point next : polygon) {
            if ((prev.y <= this.y && this.y < next.y) || (prev.y >= this.y && this.y > next.y)) {
                double dy = next.y - prev.y;
                double dx = next.x - prev.x;
                double x = (this.y - prev.y) / dy * dx + prev.x;
                if (x > this.x) {
                    intersections++;
                }
            }
            prev = next;
        }
        return intersections % 2 == 1;
    }
}

class IntPoint {
    int x;
    int y;

    public IntPoint(int intX, int intY) {
        this.x = intX;
        this.y = intY;
    }

    @Override
    public boolean equals (Object o) {
        IntPoint point = (IntPoint) o;
        return (this.x == point.x) && (this.y == point.y);
    }

    // https://stackoverflow.com/questions/9135759/java-hashcode-for-a-point-class
    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        return result;
    }
}



public class MainActivity extends RobotActivity {

    // request code for READ_CONTACTS. It can be any number > 0.
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;

    // robotAPI flags
    private static boolean isRobotApiInitialed = false;
    public static RobotAPI robotAPI;

    // localization flags
    private static boolean isRobotLocalized = false;

    // 1st roomInfo string
    private String sFirstRoom;

    //Context
    public Context context = this;

    // Navigation
    private static Navigation navigation;
    private static Point currentPoint;
    private static ArrayList<Point> points;
    private static boolean foundPlayer = false;

    // buttons
    private Button mButtonGrantPermission;
    private Button mButtonGetRoomInfo;
    private Button mButtonGoTo;

    // textViews
    private TextView mTextViewPermissionStatus;
    private TextView mTextViewFirstRoomKeyword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        robotAPI = new RobotAPI(context, robotCallback);

        robotAPI.robot.setPressOnHeadAction(false);
        // Hide his face ( -!- important to see the UI )
        robotAPI.robot.setExpression(RobotFace.HIDEFACE);

        // textViews
        mTextViewPermissionStatus = (TextView) findViewById(R.id.textView_permission_status);
        mTextViewFirstRoomKeyword = (TextView) findViewById(R.id.textView_roomInfo_1st_keyword);


        // buttons
        mButtonGrantPermission = (Button) findViewById(R.id.button_requestPermission);
        mButtonGrantPermission.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {

                // request READ_CONTACTS permission
                requestPermission();

            }
        });

        mButtonGetRoomInfo = (Button) findViewById(R.id.button_getRoomInfo);
        mButtonGetRoomInfo.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {

                    //3. use robotAPI to get all room info:
                    ArrayList<RoomInfo> arrayListRooms = robotAPI.contacts.room.getAllRoomInfo();

                    sFirstRoom = arrayListRooms.get(0).keyword;
                    String sLineString = arrayListRooms.get(0).wkt;
                    points =  Navigation.lineStringToPoints(sLineString);
                    Log.d("ZenboGoToLocation", "list of points = " + sLineString);

                    Point point1 = new Point(9, 9);
                    Point point2 = new Point(11, 9);
                    Point point3 = new Point(7, 6);

                    boolean inside1 = point1.insidePolygon(points);
                    boolean inside2 = point2.insidePolygon(points);
                    boolean inside3 = point3.insidePolygon(points);

                    Log.d("ZenboGoToLocation", "inside? = " + inside1 + inside2 + inside3);

                    mTextViewFirstRoomKeyword.setText(sFirstRoom);

                    //robotAPI.slam.activeLocalization();
                    Log.d("ZenboGoToLocation", "just send the command to get location");

                    navigation = new Navigation(robotCallback, robotListenCallback, robotAPI, points, foundPlayer);
                    Point startPt = new Point(11, 11);
                    //navigation.conductSearch(points, startPt);
                    navigation.startSearch(startPt);

                    //mButtonGoTo.setEnabled(true);
                    //mButtonGetRoomInfo.setEnabled(false);

                }
                catch (Exception e){
                    Log.d("ZenboGoToLocation", "get room info result exception = "+ e);
                }

            }
        });

        mButtonGoTo = (Button) findViewById(R.id.button_goTo);
        mButtonGoTo.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!sFirstRoom.equals("")) {

                    if(isRobotApiInitialed) {
                        // use robotAPI to go to the position "keyword":
                        robotAPI.motion.goTo(sFirstRoom);
                    }

                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // check permission READ_CONTACTS is granted or not
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted by user yet
            Log.d("ZenboGoToLocation", "READ_CONTACTS permission is not granted by user yet");
            mTextViewPermissionStatus.setText(getString(R.string.permission_not_granted));
            mButtonGrantPermission.setEnabled(true);
            mButtonGetRoomInfo.setEnabled(false);
        }
        else{
            // permission is granted by user
            Log.d("ZenboGoToLocation", "READ_CONTACTS permission is granted");
            mTextViewPermissionStatus.setText(getString(R.string.permission_granted));
            mButtonGrantPermission.setEnabled(false);
            mButtonGetRoomInfo.setEnabled(true);
        }

        // initial params
        mTextViewFirstRoomKeyword.setText(getString(R.string.first_room_info));
        mButtonGoTo.setEnabled(false);
        sFirstRoom="";

    }

    public static RobotCallback robotCallback = new RobotCallback() {
        @Override
        public void initComplete() {
            super.initComplete();

            Log.d("ZenboGoToLocation", "initComplete()");
            isRobotApiInitialed = true;
        }

        @Override
        public void onResult(int cmd, int serial, RobotErrorCode err_code, Bundle result) {
            Log.d("ZenboGoToLocation", "onResult");
            super.onResult(cmd, serial, err_code, result);
            Bundle bundle = result.getBundle("RESULT");
            Location currentLocation = bundle.getParcelable("LOCATION");
            Log.d("ZenboGoToLocation", "currentLocation" + currentLocation + currentLocation.coordinate);
        }

        @Override
        public void onStateChange(int cmd, int serial, RobotErrorCode err_code, RobotCmdState state) {
            super.onStateChange(cmd, serial, err_code, state);
            // GAME OVER
            if (foundPlayer) {
                return;
            }

            // 41 = a to b
            if (cmd == 41 && state == RobotCmdState.SUCCEED) {
                // keep going
                navigation.continueSearch();
            } else if (state == RobotCmdState.ACTIVE) {
                // do nothing
            } else if (state == RobotCmdState.FAILED) {
                // do some error handling
            }
        }
    };


    public static RobotCallback.Listen robotListenCallback = new RobotCallback.Listen() {
        @Override
        public void onFinishRegister() {

        }

        @Override
        public void onVoiceDetect(JSONObject jsonObject) {

        }

        @Override
        public void onSpeakComplete(String s, String s1) {

        }

        @Override
        public void onEventUserUtterance(JSONObject jsonObject) {

        }

        @Override
        public void onResult(JSONObject jsonObject) {

        }

        @Override
        public void onRetry(JSONObject jsonObject) {

        }
    };


    public MainActivity() {
        super(robotCallback, robotListenCallback);
    }


    private void requestPermission() {
        // Check the SDK version and whether the permission is already granted or not.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                this.checkSelfPermission(Manifest.permission.READ_CONTACTS) ==
                        PackageManager.PERMISSION_GRANTED) {
            // Android version is lesser than 6.0 or the permission is already granted.
            Log.d("ZenboGoToLocation", "permission is already granted");
            return;
        }

        if (shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},
                    PERMISSIONS_REQUEST_READ_CONTACTS);
        } else {
            //showMessageOKCancel("You need to allow access to Contacts",
            //        new DialogInterface.OnClickListener() {
            //            @Override
            //            public void onClick(DialogInterface dialog, int which) {
                            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},
                                    PERMISSIONS_REQUEST_READ_CONTACTS);
            //            }
            //        });
        }
    }

    /*
    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new android.support.v7.app.AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }
    */
}
