package kr.ac.jbnu.se.tetris;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class Coins {

    private static final Path FILE = Paths.get("coins.txt");
    private static final int START = 1000;

    public static int get() {
        try {
            return Integer.parseInt(new String(Files.readAllBytes(FILE)).trim());
        } catch (IOException | NumberFormatException e) {
            return START;
        }
    }

    public static void add(int amount) {
        try {
            Files.write(FILE, String.valueOf(get() + amount).getBytes());
        } catch (IOException e) {
            System.out.println("코인 저장 실패: " + e.getMessage());
        }
    }

    public static String settle(int winner, int me, int fee) {
        if (winner == -1) {
            add(fee);
            return "무승부 - 참가비를 돌려받았습니다";
        }
        if (winner == me) {
            add(fee * 2);
            return "승리! 참가비 " + fee + " 코인을 받았습니다";
        }
        return "패배 - 참가비 " + fee + " 코인을 잃었습니다";
    }
}