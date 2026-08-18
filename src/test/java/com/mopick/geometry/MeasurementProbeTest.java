package com.mopick.geometry;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

/** 실제 사진 4장의 측정값을 찍어 임계값을 실측으로 잡기 위한 프로브. */
class MeasurementProbeTest {

    private static final Path MODEL = Path.of("models/face-parsing.onnx");

    static boolean assetsPresent() {
        return Files.isRegularFile(MODEL);
    }

    @Test
    @EnabledIf("assetsPresent")
    void 네_장의_측정값을_찍는다() throws Exception {
        var segmenter = new OnnxHeadSegmenter(MODEL, new FaceParsingMask());
        System.out.printf("%-9s %-6s %-9s %-9s %-9s %-10s %-9s %s%n",
                "사진", "각도", "머리위", "귀상단", "단위", "귀위머리량", "폭비", "귀아래길이");
        for (String n : new String[]{"1", "2", "3", "4"}) {
            Path photo = Path.of(System.getProperty("user.home"), "Desktop", n + ".jpeg");
            if (!Files.isRegularFile(photo)) continue;
            var m = segmenter.measure(Files.readAllBytes(photo)).orElse(null);
            if (m == null) { System.out.println(n + ".jpeg 측정 실패"); continue; }

            Double unit = (m.chinY() != null && m.earTopY() != null) ? m.chinY() - m.earTopY() : null;
            String below = (m.hairBottomY() != null && m.earBottomY() != null && unit != null)
                    ? String.format("%.3f", (m.hairBottomY() - m.earBottomY()) / unit) : "-";
            String wr = (m.hairWidthPx() != null && m.headWidthPx() != null)
                    ? String.format("%.3f", m.hairWidthPx() / m.headWidthPx()) : "-";
            String above = (m.hairTopY() != null && m.earTopY() != null && unit != null)
                    ? String.format("%.3f", (m.earTopY() - m.hairTopY()) / unit) : "-";
            System.out.printf("%-9s %-6s %-9s %-9s %-9s %-10s %-9s %s%n",
                    n + ".jpeg", m.viewAngle(), m.hairTopY(), m.earTopY(), unit, above, wr, below);
        }
        segmenter.close();
    }
}
