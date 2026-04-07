package org.areslib.math.pathing;

import org.areslib.math.geometry.Translation2d;
import org.areslib.core.localization.AresFollower;
import com.pedropathing.geometry.Pose;
import com.pedropathing.geometry.BezierCurve;
import com.pedropathing.geometry.BezierLine;
import com.pedropathing.paths.PathChain;

import java.util.*;

/**
 * A Championship-Tier utility that dynamically navigates around specified obstacles on the FTC field using an A-Star node graph.
 * <p>
 * This creates optimal {@link PathChain}s containing safe {@link BezierCurve}s around dynamic elements.
 */
public class ObstacleAvoider {

    private static final double FIELD_SIZE = 144.0;
    private static final double RESOLUTION_INCHES = 4.0;
    private static final int GRID_SIZE = (int) (FIELD_SIZE / RESOLUTION_INCHES);

    private final List<Obstacle> obstacles = new ArrayList<>();

    public static class Obstacle {
        public final Translation2d minBounds;
        public final Translation2d maxBounds;

        public Obstacle(Translation2d minBounds, Translation2d maxBounds) {
            this.minBounds = minBounds;
            this.maxBounds = maxBounds;
        }

        public boolean contains(double x, double y) {
            return x >= minBounds.getX() && x <= maxBounds.getX() &&
                   y >= minBounds.getY() && y <= maxBounds.getY();
        }
    }

    private static class Node implements Comparable<Node> {
        final int gridX, gridY;
        double gCost = Double.MAX_VALUE;
        double hCost = 0;
        Node parent;

        Node(int gridX, int gridY) {
            this.gridX = gridX;
            this.gridY = gridY;
        }

        double fCost() {
            return gCost + hCost;
        }

        @Override
        public int compareTo(Node other) {
            return Double.compare(this.fCost(), other.fCost());
        }
    }

    public void addObstacle(Translation2d min, Translation2d max) {
        obstacles.add(new Obstacle(min, max));
    }

    /**
     * Dynamically paints a point on the field as an obstacle.
     * Treats the coordinate as a dense point bounding box so the A-Star grid will path around it.
     */
    public void addDynamicPoint(double x, double y) {
        // Create an obstacle with a 1-inch bounding box centered on point
        obstacles.add(new Obstacle(new Translation2d(x - 0.5, y - 0.5), new Translation2d(x + 0.5, y + 0.5)));
    }

    /**
     * Flushes all obstacles from the internal memory.
     * Useful for clearing temporal LiDAR hits every few seconds.
     */
    public void clearDynamicObstacles() {
        obstacles.clear();
    }

    public PathChain calculatePath(AresFollower follower, Pose start, Pose target) {
        int startX = (int) (start.getX() / RESOLUTION_INCHES);
        int startY = (int) (start.getY() / RESOLUTION_INCHES);
        int endX = (int) (target.getX() / RESOLUTION_INCHES);
        int endY = (int) (target.getY() / RESOLUTION_INCHES);

        // Clamp to grid
        startX = Math.max(0, Math.min(GRID_SIZE - 1, startX));
        startY = Math.max(0, Math.min(GRID_SIZE - 1, startY));
        endX = Math.max(0, Math.min(GRID_SIZE - 1, endX));
        endY = Math.max(0, Math.min(GRID_SIZE - 1, endY));

        Node[][] grid = new Node[GRID_SIZE][GRID_SIZE];
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int y = 0; y < GRID_SIZE; y++) {
                grid[x][y] = new Node(x, y);
            }
        }

        PriorityQueue<Node> openSet = new PriorityQueue<>();
        boolean[][] closedSet = new boolean[GRID_SIZE][GRID_SIZE];

        Node startNode = grid[startX][startY];
        Node endNode = grid[endX][endY];

        startNode.gCost = 0;
        startNode.hCost = getHeuristic(startNode, endNode);
        openSet.add(startNode);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            if (current == endNode) {
                return buildPedroPath(follower, current, start, target);
            }

            closedSet[current.gridX][current.gridY] = true;

            for (Node neighbor : getNeighbors(grid, current)) {
                if (closedSet[neighbor.gridX][neighbor.gridY] || isObstacle(neighbor)) {
                    continue;
                }

                double tentativeGCost = current.gCost + getDistance(current, neighbor);
                if (tentativeGCost < neighbor.gCost) {
                    neighbor.parent = current;
                    neighbor.gCost = tentativeGCost;
                    neighbor.hCost = getHeuristic(neighbor, endNode);
                    if (!openSet.contains(neighbor)) {
                        openSet.add(neighbor);
                    }
                }
            }
        }

        // Fallback
        return follower.getFollower().pathBuilder()
                .addPath(new BezierLine(start, target))
                .setLinearHeadingInterpolation(start.getHeading(), target.getHeading())
                .build();
    }

    private PathChain buildPedroPath(AresFollower follower, Node endNode, Pose startPose, Pose endPose) {
        List<Pose> waypoints = new ArrayList<>();
        Node current = endNode;
        while (current != null) {
            waypoints.add(new Pose(current.gridX * RESOLUTION_INCHES, current.gridY * RESOLUTION_INCHES, 0));
            current = current.parent;
        }
        Collections.reverse(waypoints);
        
        waypoints.set(0, startPose);
        waypoints.set(waypoints.size() - 1, endPose);

        Pose[] poseArray = waypoints.toArray(new Pose[0]);
        return follower.getFollower().pathBuilder()
                .addPath(new BezierCurve(poseArray))
                .setLinearHeadingInterpolation(startPose.getHeading(), endPose.getHeading())
                .build();
    }

    private List<Node> getNeighbors(Node[][] grid, Node node) {
        List<Node> neighbors = new ArrayList<>();
        int[] dx = {-1, 0, 1, -1, 1, -1, 0, 1};
        int[] dy = {-1, -1, -1, 0, 0, 1, 1, 1};

        for (int i = 0; i < 8; i++) {
            int checkX = node.gridX + dx[i];
            int checkY = node.gridY + dy[i];

            if (checkX >= 0 && checkX < GRID_SIZE && checkY >= 0 && checkY < GRID_SIZE) {
                neighbors.add(grid[checkX][checkY]);
            }
        }
        return neighbors;
    }

    private boolean isObstacle(Node node) {
        double realX = node.gridX * RESOLUTION_INCHES;
        double realY = node.gridY * RESOLUTION_INCHES;
        
        for (Obstacle obs : obstacles) {
            if (obs.contains(realX, realY)) {
                return true;
            }
        }
        return false;
    }

    private double getDistance(Node a, Node b) {
        double dx = a.gridX - b.gridX;
        double dy = a.gridY - b.gridY;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private double getHeuristic(Node a, Node b) {
        return getDistance(a, b);
    }
}
