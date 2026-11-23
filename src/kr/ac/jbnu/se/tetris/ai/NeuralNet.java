package kr.ac.jbnu.se.tetris.ai;

import kr.ac.jbnu.se.tetris.Resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 학습한 신경망(4 -> 64 -> 64 -> 1)으로 판의 점수를 계산한다.
 *
 * 숫자는 파이썬에서 꺼낸 model/weights.txt 를 읽는다 (export_weights.py 와 같은 순서).
 * 계산은 곱하기, 더하기, ReLU 뿐이라 외부 라이브러리 없이 직접 한다.
 */
public class NeuralNet {

    /** 가중치 파일 위치 (Resources 기준). */
    public static final String WEIGHTS = "model/weights.txt";

    private static final int INPUT = 4;
    private static final int HIDDEN = 64;

    /** 숫자 개수: 4x64 + 64 + 64x64 + 64 + 64 + 1 = 4,545 */
    private static final int TOTAL = INPUT * HIDDEN + HIDDEN + HIDDEN * HIDDEN + HIDDEN + HIDDEN + 1;

    private final double[][] w1 = new double[HIDDEN][INPUT];
    private final double[] b1 = new double[HIDDEN];
    private final double[][] w2 = new double[HIDDEN][HIDDEN];
    private final double[] b2 = new double[HIDDEN];
    private final double[] w3 = new double[HIDDEN];
    private final double b3;

    /** 가중치 파일을 읽는다. 파일이 없거나 숫자 개수가 다르면 IOException. */
    public NeuralNet(String path) throws IOException {
        double[] v = readNumbers(path);
        int i = 0;
        for (int o = 0; o < HIDDEN; o++) {
            for (int k = 0; k < INPUT; k++) {
                w1[o][k] = v[i++];
            }
        }
        for (int o = 0; o < HIDDEN; o++) {
            b1[o] = v[i++];
        }
        for (int o = 0; o < HIDDEN; o++) {
            for (int k = 0; k < HIDDEN; k++) {
                w2[o][k] = v[i++];
            }
        }
        for (int o = 0; o < HIDDEN; o++) {
            b2[o] = v[i++];
        }
        for (int k = 0; k < HIDDEN; k++) {
            w3[k] = v[i++];
        }
        b3 = v[i];
    }

    /** 입력 [지운 줄, 구멍, 울퉁불퉁함, 높이 합] -> 판의 점수. */
    public double score(double[] input) {
        double[] h1 = layer(w1, b1, input);
        double[] h2 = layer(w2, b2, h1);
        double out = b3;
        for (int k = 0; k < HIDDEN; k++) {
            out += w3[k] * h2[k];
        }
        return out;   // 마지막 층은 ReLU 없음
    }

    /** 곱하고 더한 뒤 ReLU (음수는 0으로). */
    private static double[] layer(double[][] w, double[] b, double[] x) {
        double[] out = new double[b.length];
        for (int o = 0; o < b.length; o++) {
            double sum = b[o];
            for (int k = 0; k < x.length; k++) {
                sum += w[o][k] * x[k];
            }
            out[o] = Math.max(0, sum);
        }
        return out;
    }

    private static double[] readNumbers(String path) throws IOException {
        InputStream in = Resources.open(path);
        if (in == null) {
            throw new IOException("가중치 파일이 없습니다: " + path);
        }
        double[] values = new double[TOTAL];
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String text = line.trim();
                if (text.isEmpty()) {
                    continue;
                }
                if (count == TOTAL) {
                    throw new IOException("숫자가 " + TOTAL + "개보다 많습니다");
                }
                values[count++] = Double.parseDouble(text);
            }
        }
        if (count != TOTAL) {
            throw new IOException("숫자가 " + count + "개입니다 (" + TOTAL + "개여야 함)");
        }
        return values;
    }
}