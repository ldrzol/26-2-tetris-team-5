package kr.ac.jbnu.se.tetris;

public class Coins {

    public static int get() {
        return Wallet.coins();
    }

    public static void add(int amount) {
        Wallet.addCoins(amount);
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