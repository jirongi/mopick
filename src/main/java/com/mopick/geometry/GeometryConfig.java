package com.mopick.geometry;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 기하 측정기 배선.
 *
 * <p>실제 측정기(얼굴 파싱 모델 기반)를 빈으로 등록하면 그쪽이 쓰이고, 없으면 아무것도
 * 재지 않는 기본 구현이 들어간다. 그 상태에서도 8필드는 LLM 관찰로 전부 채워지므로
 * 서비스 동작에는 문제가 없다.
 *
 * <p>모델 경로는 {@code mopick.geometry.model-path}로 지정한다(기본 {@code models/face-parsing.onnx}).
 */
@Configuration
public class GeometryConfig {

    private static final Logger log = LoggerFactory.getLogger(GeometryConfig.class);

    /**
     * 모델 파일이 있으면 ONNX 측정기를, 없으면 아무것도 재지 않는 측정기를 쓴다.
     * 모델이 없다고 기동이 실패하면 안 된다 — 그 상태에서도 서비스는 LLM 관찰만으로 돌아간다.
     */
    @Bean
    public HeadSegmenter headSegmenter(
            @Value("${mopick.geometry.model-path:models/face-parsing.onnx}") String modelPath,
            FaceParsingMask maskParser) {
        Path path = Path.of(modelPath);
        if (!Files.isRegularFile(path)) {
            log.info("기하 측정기 미탑재 (모델 없음: {}) - 8필드를 모두 LLM 관찰로 채운다", path.toAbsolutePath());
            return disabled();
        }
        OnnxHeadSegmenter segmenter = new OnnxHeadSegmenter(path, maskParser);
        return segmenter.isReady() ? segmenter : disabled();
    }

    private HeadSegmenter disabled() {
        return new HeadSegmenter() {
            @Override
            public Optional<HeadMeasurement> measure(byte[] jpeg) {
                return Optional.empty();
            }

            @Override
            public boolean isReady() {
                return false;
            }
        };
    }
}
