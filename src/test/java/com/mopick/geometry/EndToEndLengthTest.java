package com.mopick.geometry;

import static org.assertj.core.api.Assertions.assertThat;

import com.mopick.stylespec.StyleField;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * 실제 사진 4장 → 모델 → 좌표 → LENGTH. 이 프로젝트에서 세 라운드 동안 못 풀던 문제의 최종 검증.
 */
class EndToEndLengthTest {

    private static final Path MODEL = Path.of("models/face-parsing.onnx");

    static boolean assetsPresent() {
        return Files.isRegularFile(MODEL)
                && Files.isRegularFile(Path.of(System.getProperty("user.home"), "Desktop", "1.jpeg"));
    }

    @Test
    @EnabledIf("assetsPresent")
    void 네_장의_길이가_실제_길이_순서대로_나온다() throws Exception {
        var segmenter = new OnnxHeadSegmenter(MODEL, new FaceParsingMask());
        var deriver = new GeometricFieldDeriver();

        Map<String, String> lengths = new LinkedHashMap<>();
        for (String n : new String[]{"1", "2", "3", "4"}) {
            byte[] jpeg = Files.readAllBytes(
                    Path.of(System.getProperty("user.home"), "Desktop", n + ".jpeg"));
            var m = segmenter.measure(jpeg).orElseThrow();
            var o = deriver.derive(m).get(StyleField.LENGTH);
            lengths.put(n + ".jpeg", o == null ? "판정없음" : o.state() + ":" + o.value());
        }
        System.out.println("기하 LENGTH 판정: " + lengths);

        // 짧은 셋은 귀위, 목덜미까지 오는 3.jpeg만 더 길게 나와야 한다.
        assertThat(lengths.get("1.jpeg")).isEqualTo("OBSERVED:귀위");
        assertThat(lengths.get("2.jpeg")).isEqualTo("OBSERVED:귀위");
        assertThat(lengths.get("4.jpeg")).isEqualTo("OBSERVED:귀위");
        assertThat(lengths.get("3.jpeg")).isEqualTo("OBSERVED:귀덮음");

        segmenter.close();
    }

    @Test
    @EnabledIf("assetsPresent")
    void 같은_사진을_세_번_넣어도_같은_값이_나온다() throws Exception {
        var segmenter = new OnnxHeadSegmenter(MODEL, new FaceParsingMask());
        var deriver = new GeometricFieldDeriver();
        byte[] jpeg = Files.readAllBytes(
                Path.of(System.getProperty("user.home"), "Desktop", "3.jpeg"));

        String first = null;
        for (int i = 0; i < 3; i++) {
            var o = deriver.derive(segmenter.measure(jpeg).orElseThrow()).get(StyleField.LENGTH);
            if (first == null) {
                first = o.value();
            }
            assertThat(o.value()).isEqualTo(first);
        }
        System.out.println("3회 반복 결과: " + first);
        segmenter.close();
    }
}
