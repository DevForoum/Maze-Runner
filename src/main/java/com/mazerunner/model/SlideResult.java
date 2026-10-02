package com.mazerunner.model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the complete trajectory and impact consequences of a gravity slide.
 */
public class SlideResult {
    private final List<Point> pathCoordinates = new ArrayList<>();
    private final Direction slideDirection;
    private boolean wallBroken = false;
    private Point impactCell = null;
    private Direction impactWallDirection = null;

    public SlideResult(Direction slideDirection) {
        this.slideDirection = slideDirection;
    }

    public void addStep(int row, int col) {
        pathCoordinates.add(new Point(col, row)); // x=col, y=row
    }

    public List<Point> getPathCoordinates() {
        return pathCoordinates;
    }

    public int getDistanceTraversed() {
        return Math.max(0, pathCoordinates.size() - 1);
    }

    public Direction getSlideDirection() {
        return slideDirection;
    }

    public boolean isWallBroken() {
        return wallBroken;
    }

    public void setWallBroken(boolean wallBroken) {
        this.wallBroken = wallBroken;
    }

    public Point getImpactCell() {
        return impactCell;
    }

    public void setImpact(int row, int col, Direction wallDir, boolean broken) {
        this.impactCell = new Point(col, row);
        this.impactWallDirection = wallDir;
        this.wallBroken = broken;
    }

    public Direction getImpactWallDirection() {
        return impactWallDirection;
    }

    public boolean hadHighMomentum() {
        return getDistanceTraversed() >= 2;
    }
}
