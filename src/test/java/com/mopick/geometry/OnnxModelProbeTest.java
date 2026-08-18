package com.mopick.geometry;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * 실제 모델 파일이 있을 때만 도는 검증. 모델이 없는 환경(CI 등)에서는 건너뛴다.
 *
 * <p>문서에 적힌 분류 번호를 그대로 믿지 않고, 실제 사진을 넣어 좌표가 상식에 맞는지 본다.
 * 머리카락 번호가 어긋나면 머리 대신 옷을 재게 되는데 그건 조용히 틀린 값으로 나온다.
 */
class OnnxModelProbeTest {

    private static final Path MODEL = Path.of("models/face-parsing.onnx");
    private static final Path PHOTO = Path.of(System.getProperty("user.home"), "Desktop", "1.jpeg");

    static boolean assetsPresent() {
        return Files.isRegularFile(MODEL) && Files.isRegularFile(PHOTO);
    }

    @Test
    @EnabledIf("assetsPresent")
    void 실제_사진에서_상식에_맞는_좌표가_나온다() throws Exception {
        var segmenter = new OnnxHeadSegmenter(MODEL, new FaceParsingMask());
        assertThat(segmenter.isReady()).isTrue();

        Optional<HeadMeasurement> result = segmenter.measure(Files.readAllBytes(PHOTO));
        assertThat(result).isPresent();
        HeadMeasurement m = result.get();

        System.out.println("측정 결과: " + m);

        // 옆모습 사진이므로 각도가 SIDE로 잡혀야 한다.
        assertThat(m.viewAngle()).isEqualTo(HeadMeasurement.ViewAngle.SIDE);

        // 판정에 실제로 쓰는 좌표가 모두 잡혀야 한다.
        assertThat(m.hasEar()).isTrue();
        assertThat(m.chinY()).isNotNull();
        assertThat(m.hairBottomY()).isNotNull();

        // 귀는 턱보다 위, 머리카락은 귀보다 위에서 시작한다.
        assertThat(m.earTopY()).isLessThan(m.earBottomY());
        assertThat(m.earBottomY()).isLessThan(m.chinY());
        assertThat(m.hairTopY()).isLessThan(m.earTopY());

        // 단위(귀 상단~턱)가 양수로 잡혀야 비율 판정이 성립한다.
        assertThat(m.scaleUnit()).isPresent();

        segmenter.close();
    }
}
