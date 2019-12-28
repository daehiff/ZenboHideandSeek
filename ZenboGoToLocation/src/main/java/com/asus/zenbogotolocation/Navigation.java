package com.asus.zenbogotolocation;

import android.os.CountDownTimer;
import android.util.Log;

import com.asus.robotframework.API.RobotAPI;
import com.asus.robotframework.API.RobotCallback;
import com.asus.robotframework.API.results.Location;
import com.robot.asus.robotactivity.RobotActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Navigation extends RobotActivity {
    private RobotAPI robotAPI;

    public Navigation(RobotCallback robotCallback, RobotCallback.Listen robotListenCallback, RobotAPI robotAPI) {
        super(robotCallback, robotListenCallback);
        this.robotAPI = robotAPI;
    }

    public static ArrayList<Point> lineStringToPoints(String lineString) {
        //https://stackoverflow.com/questions/24256478/pattern-to-extract-text-between-parenthesis/24256532
        String pointsString = lineString.substring(lineString.indexOf("(")+1,lineString.indexOf(")"));

        ArrayList<String> pointsStrings = new ArrayList(Arrays.asList(pointsString.split(",")));

        ArrayList<Point> points = new ArrayList<Point>();
        for (int i = 0; i < pointsStrings.size(); i++) {
            String[] singlePoint = pointsStrings.get(i).trim().split(" ");
            points.add(new Point(Double.parseDouble(singlePoint[0]), Double.parseDouble(singlePoint[1])));
        }

        return points;
    }

    public static ArrayList<IntPoint> getAdjacentPoints(ArrayList<Point> map, IntPoint point) {
        ArrayList<IntPoint> adjacentPoints = new ArrayList<IntPoint>();
        Point up = new Point(point.x, point.y + 1.0);
        Point right = new Point(point.x + 1.0, point.y);
        Point down = new Point(point.x, point.y - 1.0);
        Point left = new Point(point.x - 1.0, point.y);

        if (up.insidePolygon(map)) {
            Log.d("ZenboGoToLocation", "up in polygon");
            IntPoint upInt = new IntPoint((int) up.x, (int) up.y);
            adjacentPoints.add(upInt);
        }
        if (right.insidePolygon(map)) {
            Log.d("ZenboGoToLocation", "right in polygon");
            IntPoint rightInt = new IntPoint((int) right.x, (int) right.y);
            adjacentPoints.add(rightInt);
        }
        if (down.insidePolygon(map)) {
            Log.d("ZenboGoToLocation", "down in polygon");
            IntPoint downInt = new IntPoint((int) down.x, (int) down.y);
            adjacentPoints.add(downInt);
        }
        if (left.insidePolygon(map)) {
            Log.d("ZenboGoToLocation", "left in polygon");
            IntPoint leftInt = new IntPoint((int) left.x, (int) left.y);
            adjacentPoints.add(leftInt);
        }

        return adjacentPoints;
    }

    public void goToLocation(IntPoint point) {
        Location location = new Location();
        location.coordinate.x = (float) point.x;
        location.coordinate.y = (float) point.y;

        robotAPI.motion.goTo(location, false);
        Log.d("ZenboGoToLocation", "going to location" + point.x + " " + point.y);
    }

    //pause 3 seconds
    public void pause() {
        new CountDownTimer(3000, 1000) {

            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
            }
        }.start();
    }

    // BFS @TODO CHANGE TO DFS
    public boolean conductSearch(ArrayList<Point> map, Point startPt) {
        Log.d("ZenboGoToLocation", "starting to search");

        ArrayList<IntPoint> queue = new ArrayList<IntPoint>();
        Map<IntPoint, Boolean> visited = new HashMap<IntPoint, Boolean>();

        visited.put(new IntPoint((int) startPt.x, (int) startPt.y), Boolean.TRUE);

        queue.add(new IntPoint((int)startPt.x, (int)startPt.y));
        Log.d("ZenboGoToLocation", "Start Pt" + startPt.x + " " + startPt.y);

        while (queue.size() > 0) {
            IntPoint currPt = queue.remove(0); //pop

            // @TODO implement logic for finding the person
            goToLocation(currPt);
            //pause();
            if (false) { // @TODO DID WE FIND THEM
                return true;
            }

            ArrayList<IntPoint> adjacentPoints = getAdjacentPoints(map, currPt);
            for (int i = 0; i < adjacentPoints.size(); i++) {
                IntPoint adjacentPoint = adjacentPoints.get(i);
                Log.d("ZenboGoToLocation", "adjacentPoint" + adjacentPoint.x + " " + adjacentPoint.y);

                // since we haven't initialized visited yet, we need to initialize it here
                if (visited.get(adjacentPoint) == null) {
                    visited.put(adjacentPoint, Boolean.FALSE);
                }
                if (visited.get(adjacentPoint) == Boolean.FALSE) {
                    visited.put(adjacentPoint, Boolean.TRUE);
                    queue.add(adjacentPoint);
                }
            }
            Log.d("ZenboGoToLocation", "queue size" + queue.size());
            if (queue.size() > 10) {
                return true;
            }
        }

        return false;
    }

    public static int indexOfInt(ArrayList<Point> points, Point point) {
        for (int i = 0; i < points.size(); i++) {
            Point currPt = points.get(i);
            if (((int) currPt.x == (int) point.x) && ((int) currPt.y == (int) point.y)) {
                return i;
            }
        }
        return -1;
    }
}
