package com.mopick.geometry;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import jakarta.annotation.PreDestroy;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 얼굴 파싱 ONNX 모델로 사진에서 부위 마스크를 만든다.
 *
 * <p>모델 파일이 없으면 {@link #isReady()}가 false가 되고 아무것도 재지 않는다.
 * 그 상태에서도 8필드는 LLM 관찰로 채워지므로 서비스는 정상 동작한다.
 * 모델을 못 구하는 환경에서 기동이 실패하면 안 되므로 생성자에서 예외를 던지지 않는다.
 *
 * <p>측정 실패도 예외가 아니라 빈 값이다. 얼굴이 없는 사진, 너무 어두운 사진에서는
 * 마스크가 비고, 그때는 좌표를 만들지 않는 편이 맞다.
 *
 * <p>좌표는 모델 입력 해상도 기준으로 계산한다. 판정 임계값이 전부 두상 높이 대비 비율이라
 * 원본 크기로 되돌릴 필요가 없다.
 */
public class OnnxHeadSegmenter implements HeadSegmenter {

    private static final Logger log = LoggerFactory.getLogger(OnnxHeadSegmenter.class);

    /** CelebAMask-HQ 계열 얼굴 파싱 모델의 표준 입력 크기. */
    private static final int INPUT_SIZE = 512;
    /** ImageNet 정규화. 이 계열 모델이 공통으로 쓰는 값이다. */
    private static final float[] MEAN = {0.485f, 0.456f, 0.406f};
    private static final float[] STD = {0.229f, 0.224f, 0.225f};

    private final FaceParsingMask maskParser;
    private final OrtEnvironment environment;
    private final OrtSession session;
    private final String inputName;

    public OnnxHeadSegmenter(Path modelPath, FaceParsingMask maskParser) {
        this.maskParser = maskParser;

        OrtEnvironment env = null;
        OrtSession loaded = null;
        String input = null;
        if (modelPath != null && Files.isRegularFile(modelPath)) {
            try {
                env = OrtEnvironment.getEnvironment();
                loaded = env.createSession(modelPath.toString(), new OrtSession.SessionOptions());
                input = loaded.getInputNames().iterator().next();
                log.info("얼굴 파싱 모델 적재: {} (입력 '{}')", modelPath, input);
            } catch (Exception e) {
                log.warn("얼굴 파싱 모델 적재 실패 - 기하 측정 없이 진행한다: {}", e.toString());
                env = null;
                loaded = null;
                input = null;
            }
        } else {
            log.info("얼굴 파싱 모델 파일이 없다({}) - 기하 측정 없이 진행한다", modelPath);
        }
        this.environment = env;
        this.session = loaded;
        this.inputName = input;
    }

    @Override
    public boolean isReady() {
        return session != null;
    }

    @Override
    public Optional<HeadMeasurement> measure(byte[] jpeg) {
        if (!isReady() || jpeg == null) {
            return Optional.empty();
        }
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(jpeg));
            if (image == null) {
                return Optional.empty();
            }
            int[][] mask = runModel(toSquare(image));
            return Optional.of(maskParser.extract(mask));
        } catch (Exception e) {
            // 못 재는 사진은 늘 있다. 그때는 LLM 관찰로 돌아가면 된다.
            log.debug("기하 측정 실패: {}", e.toString());
            return Optional.empty();
        }
    }

    private BufferedImage toSquare(BufferedImage source) {
        BufferedImage out = new BufferedImage(INPUT_SIZE, INPUT_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, INPUT_SIZE, INPUT_SIZE, null);
        g.dispose();
        return out;
    }

    private int[][] runModel(BufferedImage image) throws Exception {
        FloatBuffer buffer = FloatBuffer.allocate(3 * INPUT_SIZE * INPUT_SIZE);
        // NCHW: 채널별로 이어붙인다.
        for (int c = 0; c < 3; c++) {
            for (int y = 0; y < INPUT_SIZE; y++) {
                for (int x = 0; x < INPUT_SIZE; x++) {
                    int rgb = image.getRGB(x, y);
                    int v = switch (c) {
                        case 0 -> (rgb >> 16) & 0xFF;
                        case 1 -> (rgb >> 8) & 0xFF;
                        default -> rgb & 0xFF;
                    };
                    buffer.put((v / 255.0f - MEAN[c]) / STD[c]);
                }
            }
        }
        buffer.rewind();

        long[] shape = {1, 3, INPUT_SIZE, INPUT_SIZE};
        try (OnnxTensor tensor = OnnxTensor.createTensor(environment, buffer, shape);
             OrtSession.Result result = session.run(Map.of(inputName, tensor))) {
            Object value = result.get(0).getValue();
            return argmax((float[][][][]) value);
        }
    }

    /** [1][클래스][H][W] 로짓에서 픽셀마다 가장 큰 클래스를 고른다. */
    private int[][] argmax(float[][][][] logits) {
        float[][][] planes = logits[0];
        int classes = planes.length;
        int height = planes[0].length;
        int width = planes[0][0].length;

        int[][] mask = new int[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int best = 0;
                float bestScore = planes[0][y][x];
                for (int c = 1; c < classes; c++) {
                    if (planes[c][y][x] > bestScore) {
                        bestScore = planes[c][y][x];
                        best = c;
                    }
                }
                mask[y][x] = best;
            }
        }
        return mask;
    }

    @PreDestroy
    public void close() {
        if (session != null) {
            try {
                session.close();
            } catch (Exception e) {
                log.debug("ONNX 세션 종료 실패: {}", e.toString());
            }
        }
    }
}
