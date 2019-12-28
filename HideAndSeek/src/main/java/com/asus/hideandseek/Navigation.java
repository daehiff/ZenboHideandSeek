package com.asus.hideandseek;

import android.util.Log;

import com.asus.robotframework.API.results.Location;
import com.asus.robotframework.API.results.RoomInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Navigation {
    private static ArrayList<IntPoint> stack = new ArrayList<IntPoint>();
    private static Map<IntPoint, Boolean> visited = new HashMap<IntPoint, Boolean>();
    private static ArrayList<Point> map;
    private static boolean seekingActive = false;

    public Navigation() {
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

        HideAndSeek.robotAPI.motion.goTo(location, false);
        Log.d("ZenboGoToLocation", "going to location" + point.x + " " + point.y);
    }

    public void startSearchingRoom(Point startPt) {
        seekingActive = true;

        ArrayList<RoomInfo> arrayListRooms = HideAndSeek.robotAPI.contacts.room.getAllRoomInfo();

        String lineString = arrayListRooms.get(0).wkt;
        map =  Navigation.lineStringToPoints(lineString);
        Log.d("ZenboGoToLocation", "list of points = " + lineString);

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

            visited.put(currPt, Boolean.TRUE);

            ArrayList<IntPoint> adjacentPoints = getAdjacentPoints(map, currPt);
            for (int i = 0; i < adjacentPoints.size(); i++) {
                IntPoint adjacentPoint = adjacentPoints.get(i);
                stack.add(adjacentPoint);
                Log.d("ZenboGoToLocation", "adjacentPoint" + adjacentPoint.x + " " + adjacentPoint.y);
            }
        }
    }

    public void stopAllMovement() {
        seekingActive = false;
    }

    public static void continueSearch() {
        if (!seekingActive) {
            return;
        }
        HideAndSeek.seeking.state = Seeking.SeekingState.SEEKING;
        IntPoint currPt = stack.remove(stack.size() - 1); //pop
        while (visited.get(currPt) == Boolean.TRUE) {
            if (stack.size() > 0) {
                currPt = stack.remove(stack.size() - 1);
            } else { // no more points left
                return;
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
    }
}
