package com.asus.hideandseek;

import java.util.ArrayList;

class Point {
    double x;
    double y;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

	// citation: https://stackoverflow.com/questions/38675611/determine-if-point-is-in-set-of-coordinates-in-java
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
