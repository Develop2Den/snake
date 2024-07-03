package com.codenjoy.dojo.snake.client;


import com.codenjoy.dojo.client.Solver;
import com.codenjoy.dojo.client.WebSocketRunner;
import com.codenjoy.dojo.services.Direction;
import com.codenjoy.dojo.services.Point;
import com.codenjoy.dojo.services.PointImpl;
import java.util.*;

/**
 * User: Denisov Denis
 */

public class YourSolver implements Solver<Board> {

    // Lee

    private static final int MIN_LENGTH_TO_EAT_STONE = 35;
    private static final int MIN_LENGTH_FOR_FORCED_EATING = 12;

    private Direction findPathLee(Board board, Point target) {
        Point head = board.getHead();
        int[][] distances = new int[15][15];
        for (int[] row : distances) {
            Arrays.fill(row, Integer.MAX_VALUE);
        }
        distances[head.getX()][head.getY()] = 0;

        Queue<Point> queue = new LinkedList<>();
        queue.add(head);

        while (!queue.isEmpty()) {
            Point current = queue.poll();
            int currentDistance = distances[current.getX()][current.getY()];

            for (Point neighbor : getValidNeighbors(current, board.getSnake(), board.getStones(), board.getWalls(), board.getSnake().size())) {
                if (distances[neighbor.getX()][neighbor.getY()] == Integer.MAX_VALUE) {
                    distances[neighbor.getX()][neighbor.getY()] = currentDistance + 1;
                    queue.add(neighbor);
                }
            }
        }

        return reconstructPathLee(board, distances, head, target);
    }

    private Direction reconstructPathLee(Board board, int[][] distances, Point start, Point end) {
        List<Point> path = new ArrayList<>();
        Point current = end;

        while (!current.equals(start)) {
            path.add(current);
            List<Point> neighbors = getValidNeighbors(current, board.getSnake(), board.getStones(), board.getWalls(), board.getSnake().size());
            Point next = null;
            for (Point neighbor : neighbors) {
                if (distances[neighbor.getX()][neighbor.getY()] < distances[current.getX()][current.getY()]) {
                    next = neighbor;
                    break;
                }
            }
            if (next == null) {
                break;
            }
            current = next;
        }

        if (!path.isEmpty()) {
            return determineDirection(board, start, path.get(path.size() - 1));
        }

        return board.getSnakeDirection();
    }

    private Direction determineDirection(Board board, Point from, Point to) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();

        if (dx == -1) {
            return Direction.LEFT;
        } else if (dx == 1) {
            return Direction.RIGHT;
        } else if (dy == 1) {
            return Direction.UP;
        } else if (dy == -1) {
            return Direction.DOWN;
        }

        return board.getSnakeDirection();
    }

    private List<Point> getValidNeighbors(Point point, List<Point> snake, List<Point> stones, List<Point> walls, int snakeLength) {
        List<Point> neighbors = new ArrayList<>();
        int x = point.getX();
        int y = point.getY();

        Point[] adjacentPoints = {
                new PointImpl(x, y + 1),
                new PointImpl(x, y - 1),
                new PointImpl(x - 1, y),
                new PointImpl(x + 1, y)
        };

        for (Point neighbor : adjacentPoints) {
            if (!isSnakeContains(snake, neighbor) && isValidMove(neighbor, walls) && (snakeLength >= MIN_LENGTH_TO_EAT_STONE || !isStone(neighbor, stones))) {
                neighbors.add(neighbor);
            }
        }

        if (neighbors.isEmpty() && snakeLength >= MIN_LENGTH_FOR_FORCED_EATING) {
            for (Point neighbor : adjacentPoints) {
                if (!isSnakeContains(snake, neighbor) && isValidMove(neighbor, walls)) {
                    neighbors.add(neighbor);
                }
            }
        }

        return neighbors;
    }

    private boolean isValidMove(Point point, List<Point> walls) {
        int x = point.getX();
        int y = point.getY();
        if (x <= 0 || x >= 14 || y <= 0 || y >= 14) {
            return false;
        }
        for (Point wall : walls) {
            if (wall.getX() == x && wall.getY() == y) {
                return false;
            }
        }
        return true;
    }

    private boolean isSnakeContains(List<Point> snake, Point point) {
        for (Point part : snake) {
            if (part.getX() == point.getX() && part.getY() == point.getY()) {
                return true;
            }
        }
        return false;
    }

    private boolean isStone(Point point, List<Point> stones) {
        for (Point stone : stones) {
            if (stone.getX() == point.getX() && stone.getY() == point.getY()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String get(Board board) {
        if (board.isGameOver()) {
            return "";
        }

        Point apple = board.getApples().getFirst();
        List<Point> snake = board.getSnake();

        if (snake.size() >= MIN_LENGTH_TO_EAT_STONE) {
            Point stone = findStone(board);
            if (stone != null) {
                return findPathLee(board, stone).toString();
            }
        }

        Direction directionToApple = findPathLee(board, apple);
        if (isPathClear(board, directionToApple, apple)) {
            return directionToApple.toString();
        }

        Point emptyCell = findEmptyCell(board, snake);
        if (emptyCell != null) {
            return findPathLee(board, emptyCell).toString();
        }

        return board.getSnakeDirection().toString();
    }

    private boolean isPathClear(Board board, Direction direction, Point target) {
        Point head = board.getHead();
        Point nextPosition = getNextPosition(head, direction);
        return !isSnakeContains(board.getSnake(), nextPosition) && isValidMove(nextPosition, board.getWalls());
    }

    private Point getNextPosition(Point current, Direction direction) {
        int x = current.getX();
        int y = current.getY();

        switch (direction) {
            case LEFT:
                return new PointImpl(x - 1, y);
            case RIGHT:
                return new PointImpl(x + 1, y);
            case UP:
                return new PointImpl(x, y + 1);
            case DOWN:
                return new PointImpl(x, y - 1);
            default:
                return current;
        }
    }

    private Point findEmptyCell(Board board, List<Point> snake) {
        for (int x = 1; x < 14; x++) {
            for (int y = 1; y < 14; y++) {
                Point point = new PointImpl(x, y);
                if (!isSnakeContains(snake, point) && isValidMove(point, board.getWalls())) {
                    return point;
                }
            }
        }
        return null;
    }

    private Point findStone(Board board) {
        List<Point> stones = board.getStones();
        if (!stones.isEmpty()) {
            return stones.get(0);
        }
        return null;
    }

    public static void main(String[] args) {
        String URL = "http://138.197.189.109/codenjoy-contest/board/player/nw132s6xco17y7fhbm9q?code=6394015397278301236";
        WebSocketRunner.runClient(
                URL,
                new YourSolver(),
                new Board());
    }

}
