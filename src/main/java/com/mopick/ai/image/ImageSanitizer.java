package com.mopick.ai.image;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

/**
 * 분석 요청 전 이미지를 정제한다.
 *
 * <p>JPEG로 재인코딩하므로 EXIF(촬영 위치·기기 등)는 자동으로 제거된다.
 * 원본은 어디에도 저장하지 않고 요청 처리 중 메모리에만 존재한다.
 */
@Component
public class ImageSanitizer {

    /** 긴 변 기준 상한. 헤어 실루엣 판별에는 이 이상 필요 없고 비용·지연만 늘어난다. */
    private static final int MAX_DIMENSION = 1024;
    private static final float JPEG_QUALITY_HINT = 0.85f;

    public record SanitizedImage(byte[] bytes, String sha256, int width, int height) {
    }

    public SanitizedImage sanitize(InputStream in) throws IOException {
        BufferedImage source = ImageIO.read(in);
        if (source == null) {
            throw new IOException("이미지를 읽을 수 없습니다. JPEG 또는 PNG를 올려주세요.");
        }
        BufferedImage resized = resize(source);
        byte[] jpeg = toJpeg(resized);
        return new SanitizedImage(jpeg, sha256(jpeg), resized.getWidth(), resized.getHeight());
    }

    public SanitizedImage sanitize(byte[] raw) throws IOException {
        return sanitize(new ByteArrayInputStream(raw));
    }

    private BufferedImage resize(BufferedImage source) {
        int w = source.getWidth();
        int h = source.getHeight();
        double scale = Math.min(1.0, (double) MAX_DIMENSION / Math.max(w, h));

        int targetW = Math.max(1, (int) Math.round(w * scale));
        int targetH = Math.max(1, (int) Math.round(h * scale));

        // 축소가 필요 없어도 알파 채널을 없애기 위해 항상 RGB로 다시 그린다.
        BufferedImage out = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(source, 0, 0, targetW, targetH, null);
        g.dispose();
        return out;
    }

    private byte[] toJpeg(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        var writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            ImageIO.write(image, "jpeg", out);
            return out.toByteArray();
        }
        var writer = writers.next();
        try (var ios = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(ios);
            var param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(JPEG_QUALITY_HINT);
            }
            writer.write(null, new javax.imageio.IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
