package kr.ac.jbnu.se.tetris.ai;

import kr.ac.jbnu.se.tetris.Resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Java 신경망 계산이 파이썬(PyTorch) 계산과 같은지 확인하는 자가 점검.
 * model/check.txt 의 입력 5개를 넣어서 파이썬이 낸 점수와 비교한다.
 */
public class NeuralNetSelfCheck {

    /** 허용 오차. 파이썬은 float, Java 는 double 로 계산해서 아주 조금 다르다. */
    private static final double TOLERANCE = 1e-4;

    public static void main(String[] args) throws IOException {
        NeuralNet net = new NeuralNet(NeuralNet.WEIGHTS);
        InputStream in = Resources.open("model/check.txt");
        if (in == null) {
            throw new IOException("model/check.txt 가 없습니다");
        }
        int checked = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String header = reader.readLine();   // 첫 줄은 제목
            if (header == null) {
                throw new IOException("model/check.txt 가 비어 있습니다");
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] p = line.trim().split(",");
                double[] input = {
                        Double.parseDouble(p[0]), Double.parseDouble(p[1]),
                        Double.parseDouble(p[2]), Double.parseDouble(p[3]) };
                double expected = Double.parseDouble(p[4]);
                double actual = net.score(input);
                System.out.printf("%s -> Java %.6f / Python %.6f%n", Arrays.toString(input), actual, expected);
                if (Math.abs(actual - expected) > TOLERANCE) {
                    throw new AssertionError("값이 다릅니다: " + line);
                }
                checked++;
            }
        }
        System.out.println("NeuralNetSelfCheck: 통과 (" + checked + "개)");
    }
}