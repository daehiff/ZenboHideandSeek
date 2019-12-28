package com.asus.zenbogotolocation;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;

import com.asus.robotframework.API.RobotAPI;
import com.asus.robotframework.API.RobotCallback;
import com.asus.robotframework.API.RobotCmdState;
import com.asus.robotframework.API.RobotErrorCode;
import com.asus.robotframework.API.results.Location;
import com.robot.asus.robotactivity.RobotActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Navigation extends RobotActivity {
    private static RobotAPI robotAPI;
    private static ArrayList<IntPoint> stack = new ArrayList<IntPoint>();
    private static Map<IntPoint, Boolean> visited = new HashMap<IntPoint, Boolean>();
    private static ArrayList<Point> map;
    private static boolean foundPlayer = false;

    public Navigation(RobotCallback robotCallback, RobotCallback.Listen robotListenCallback, RobotAPI robotAPI, ArrayList<Point> points, boolean foundPlayer) {
        super(robotCallback, robotListenCallback);
        this.robotAPI = robotAPI;
        this.map = points;
        this.foundPlayer = foundPlayer;
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
        Log.d("ZenboGoToLocation", "get adjacentpoints");
        ArrayList<Point> points= new ArrayList<Point>();

        Point up = new Point(point.x, point.y + 1.0);
        points.add(up);
        Point right = new Point(point.x + 1.0, point.y);
        points.add(right);
        Point down = new Point(point.x, point.y - 1.0);
        points.add(down);
        Point left = new Point(point.x - 1.0, point.y);
        points.add(left);

        ArrayList<IntPoint> adjacentPoints = new ArrayList<IntPoint>();

        for (Point pt : points) {
            if (pt.insidePolygon(map)) {
                IntPoint intPt = new IntPoint((int) pt.x, (int) pt.y);
                adjacentPoints.add(intPt);
            }
        }

        return adjacentPoints;
    }

    public static void goToLocation(IntPoint point) {
        Location location = new Location();
        location.coordinate.x = (float) point.x;
        location.coordinate.y = (float) point.y;

        robotAPI.motion.goTo(location, false);
        Log.d("ZenboGoToLocation", "going to location" + point.x + " " + point.y);
    }

    public boolean startSearch(Point startPt) {
        stack.add(new IntPoint((int)startPt.x, (int)startPt.y));
        Log.d("ZenboGoToLocation", "Start Point" + startPt.x + " " + startPt.y);

        IntPoint currPt = stack.remove(stack.size() - 1); //pop

        if (visited.get(currPt) == null) {
            visited.put(currPt, Boolean.FALSE);
        }

        // if the point is not discovered, then check for the person, then add its neighbors
        if (visited.get(currPt) == Boolean.FALSE) {
            // @TODO implement logic for finding the person
            goToLocation(currPt);

            if (false) { // @TODO DID WE FIND THEM
                foundPlayer = true;
                return true;
            }

            visited.put(currPt, Boolean.TRUE);

            ArrayList<IntPoint> adjacentPoints = getAdjacentPoints(map, currPt);
            for (int i = 0; i < adjacentPoints.size(); i++) {
                IntPoint adjacentPoint = adjacentPoints.get(i);
                stack.add(adjacentPoint);
                Log.d("ZenboGoToLocation", "adjacentPoint" + adjacentPoint.x + " " + adjacentPoint.y);
            }
        }

        return false;
    }

    public static boolean continueSearch() {
        IntPoint currPt = stack.remove(stack.size() - 1); //pop
        while (visited.get(currPt) == Boolean.TRUE) {
            if (stack.size() > 0) {
                currPt = stack.remove(stack.size() - 1);
            } else { // no more points left
                return true;
            }
        }

        // since we haven't initialized visited yet, we need to initialize it here
        if (visited.get(currPt) == null) {
            visited.put(currPt, Boolean.FALSE);
        }

        if (stack.size() > 0) {
            // if the point is not discovered, then check for the person, then add its neighbors
            if (visited.get(currPt) == Boolean.FALSE) {
                // @TODO implement logic for finding the person
                goToLocation(currPt);

                if (false) { // @TODO DID WE FIND THEM
                    foundPlayer = true;
                    return true;
                }

                Log.d("ZenboGoToLocation", "Marking point as visited: " + currPt.x + " " + currPt.y);
                visited.put(currPt, Boolean.TRUE);

                ArrayList<IntPoint> adjacentPoints = getAdjacentPoints(map, currPt);
                for (int i = 0; i < adjacentPoints.size(); i++) {
                    IntPoint adjacentPoint = adjacentPoints.get(i);
                    stack.add(adjacentPoint);
                    Log.d("ZenboGoToLocation", "adjacentPoint" + adjacentPoint.x + " " + adjacentPoint.y);
                }
            }
        }
        return false;
    }
}
