package com.asus.hideandseek;

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