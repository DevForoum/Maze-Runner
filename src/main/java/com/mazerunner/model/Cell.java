package com.mazerunner.model;

/**
 * Encapsulates matrix position, wall barriers, structural integrity, and search state.
 */
public class Cell {
    private final int row;
    private final int col;

    // Walls array mapping: 0 = Top, 1 = Right, 2 = Bottom, 3 = Left
    private final boolean[] walls = {true, true, true, true};
    // Cracked flags tracking structural vulnerability per wall
    private final boolean[] cracked = {false, false, false, false};
    private boolean visited = false;
    private boolean path = false; // Flagged when part of solved path solution

    public Cell(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }
    public boolean[] getWalls() { return walls; }
    public boolean[] getCracked() { return cracked; }

    public boolean hasWall(Direction dir) {
        return walls[dir.getWallIndex()];
    }

    public boolean hasWall(int wallIndex) {
        return walls[wallIndex];
    }

    public void removeWall(int wallIndex) {
        if (wallIndex >= 0 && wallIndex < 4) {
            this.walls[wallIndex] = false;
            this.cracked[wallIndex] = false;
        }
    }

    public void removeWall(Direction dir) {
        removeWall(dir.getWallIndex());
    }

    public void setWall(int wallIndex, boolean hasWall) {
        if (wallIndex >= 0 && wallIndex < 4) {
            this.walls[wallIndex] = hasWall;
            if (!hasWall) {
                this.cracked[wallIndex] = false;
            }
        }
    }

    public void setWall(Direction dir, boolean hasWall) {
        setWall(dir.getWallIndex(), hasWall);
    }

    public boolean isCracked(int wallIndex) {
        return wallIndex >= 0 && wallIndex < 4 && this.cracked[wallIndex];
    }

    public boolean isCracked(Direction dir) {
        return isCracked(dir.getWallIndex());
    }

    public void setCracked(int wallIndex, boolean cracked) {
        if (wallIndex >= 0 && wallIndex < 4) {
            this.cracked[wallIndex] = cracked;
            if (cracked) {
                this.walls[wallIndex] = true;
            }
        }
    }

    public void setCracked(Direction dir, boolean cracked) {
        setCracked(dir.getWallIndex(), cracked);
    }

    public void breakWall(int wallIndex) {
        removeWall(wallIndex);
    }

    public void breakWall(Direction dir) {
        breakWall(dir.getWallIndex());
    }

    public boolean isVisited() { return visited; }
    public void setVisited(boolean visited) { this.visited = visited; }

    public boolean isPath() { return path; }
    public void setPath(boolean path) { this.path = path; }

    // Energy Gems
    private boolean hasGem = false;

    public boolean hasGem() { return hasGem; }
    public void setGem(boolean hasGem) { this.hasGem = hasGem; }

    // Wormholes / Portals
    private boolean portal = false;
    private int portalId = 0; // 1 = Cyan Portal, 2 = Violet Portal
    private java.awt.Point portalTarget = null;

    public boolean isPortal() { return portal; }
    public int getPortalId() { return portalId; }
    public java.awt.Point getPortalTarget() { return portalTarget; }

    public void setPortal(int portalId, java.awt.Point target) {
        this.portal = true;
        this.portalId = portalId;
        this.portalTarget = target;
    }

    public void clearPortal() {
        this.portal = false;
        this.portalId = 0;
        this.portalTarget = null;
    }

    public void reset() {
        this.visited = false;
        this.path = false;
        this.hasGem = false;
        this.portal = false;
        this.portalId = 0;
        this.portalTarget = null;
        for (int i = 0; i < 4; i++) {
            this.walls[i] = true;
            this.cracked[i] = false;
        }
    }
}