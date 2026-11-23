package kr.ac.jbnu.se.tetris.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * 쌓인 칸들의 판. 칸마다 모양, 색 번호, 나이(기믹 FADE 용)를 함께 갖고 다닌다.
 *
 * 세 값은 늘 같이 움직여야 해서(줄을 당기거나 지울 때) 하나의 객체로 묶고,
 * 바깥에는 칸 단위 조작 메서드만 보여 준다.
 */
public class Playfield {

    private final int width;
    private final int height;
    private final Tetrominoes[][] shapes;
    private final int[][] colors;
    private final int[][] ages;

    public Playfield(int width, int height) {
        this.width = width;
        this.height = height;
        shapes = new Tetrominoes[height][];
        colors = new int[height][];
        ages = new int[height][];
        for (int y = 0; y < height; y++) {
            shapes[y] = emptyShapeRow();
            colors[y] = new int[width];
            ages[y] = new int[width];
        }
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public Tetrominoes shapeAt(int x, int y) {
        return shapes[y][x];
    }

    public int colorAt(int x, int y) {
        return colors[y][x];
    }

    public boolean isEmptyAt(int x, int y) {
        return shapes[y][x] == Tetrominoes.NoShape;
    }

    /** 칸을 채운다. 새로 놓인 칸이므로 나이는 0이다. */
    public void set(int x, int y, Tetrominoes shape, int color) {
        shapes[y][x] = shape;
        colors[y][x] = color;
        ages[y][x] = 0;
    }

    /** 칸 하나를 비운다. 판 밖이면 무시한다. */
    public void erase(int x, int y) {
        if (inBounds(x, y)) {
            set(x, y, Tetrominoes.NoShape, 0);
        }
    }

    public void clear() {
        for (int y = 0; y < height; y++) {
            shapes[y] = emptyShapeRow();
            colors[y] = new int[width];
            ages[y] = new int[width];
        }
    }

    public boolean isRowFull(int y) {
        for (int x = 0; x < width; x++) {
            if (isEmptyAt(x, y)) {
                return false;
            }
        }
        return true;
    }

    public boolean isBoardEmpty() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!isEmptyAt(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** 한 줄을 없애고 위의 줄들을 한 칸씩 내린다. 색과 나이도 칸을 따라 내려온다. */
    public void removeRow(int row) {
        for (int y = row; y < height - 1; y++) {
            shapes[y] = shapes[y + 1];
            colors[y] = colors[y + 1];
            ages[y] = ages[y + 1];
        }
        resetRow(height - 1);
    }

    /** row 에 빈 줄을 끼워 넣고, 그 위의 줄들을 한 칸씩 올린다. */
    public void insertRow(int row) {
        for (int y = height - 1; y > row; y--) {
            shapes[y] = shapes[y - 1];
            colors[y] = colors[y - 1];
            ages[y] = ages[y - 1];
        }
        resetRow(row);
    }

    /** 맨 아래에 구멍 하나(hole)만 뚫린 쓰레기 줄을 끼워 넣는다. */
    public void insertGarbageRow(int hole) {
        insertRow(0);
        for (int x = 0; x < width; x++) {
            shapes[0][x] = (x == hole) ? Tetrominoes.NoShape : Tetrominoes.GarbageShape;
        }
    }

    /**
     * 쌓인 칸의 나이를 1 올리고 life 에 닿은 칸은 지운다. (쓰레기 줄은 제외)
     */
    public void ageAndFade(int life) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Tetrominoes t = shapes[y][x];
                if (t == Tetrominoes.NoShape || t == Tetrominoes.GarbageShape) {
                    continue;
                }
                if (++ages[y][x] >= life) {
                    erase(x, y);
                }
            }
        }
    }

    /** 채워진 칸 중 하나를 무작위로 고른다. {x, y} 를 돌려주고, 하나도 없으면 null. */
    public int[] randomFilledCell(Random random) {
        List<int[]> filled = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!isEmptyAt(x, y)) {
                    filled.add(new int[] { x, y });
                }
            }
        }
        return filled.isEmpty() ? null : filled.get(random.nextInt(filled.size()));
    }

    /** 모양과 색을 (y * width + x) 순서의 한 줄 배열로 편다. 두 배열 길이는 width * height. */
    public void writeTo(Tetrominoes[] outShapes, int[] outColors) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                outShapes[y * width + x] = shapes[y][x];
                outColors[y * width + x] = colors[y][x];
            }
        }
    }

    /** writeTo 의 반대. 받은 칸으로 덮어쓴다. */
    public void readFrom(Tetrominoes[] inShapes, int[] inColors) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                shapes[y][x] = inShapes[y * width + x];
                colors[y][x] = inColors[y * width + x];
            }
        }
    }

    private void resetRow(int y) {
        shapes[y] = emptyShapeRow();
        colors[y] = new int[width];
        ages[y] = new int[width];
    }

    private Tetrominoes[] emptyShapeRow() {
        Tetrominoes[] row = new Tetrominoes[width];
        Arrays.fill(row, Tetrominoes.NoShape);
        return row;
    }
}
