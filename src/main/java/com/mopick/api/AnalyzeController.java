package com.mopick.api;

import com.mopick.ai.AiAvailability;
import com.mopick.ai.StyleSpecAnalyzer;
import com.mopick.ai.image.ImageSanitizer;
import com.mopick.api.dto.StyleSpecResponse;
import com.mopick.stylespec.StyleSpec;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 사진 → StyleSpec 두 엔드포인트.
 *
 * <p>업로드된 원본은 저장하지 않는다. 정제된 바이트가 분석 요청에만 쓰이고 응답 후 사라진다.
 */
@RestController
public class AnalyzeController {

    private static final Logger log = LoggerFactory.getLogger(AnalyzeController.class);

    private final StyleSpecAnalyzer analyzer;
    private final ImageSanitizer imageSanitizer;
    private final AiAvailability availability;

    public AnalyzeController(StyleSpecAnalyzer analyzer, ImageSanitizer imageSanitizer,
                             AiAvailability availability) {
        this.analyzer = analyzer;
        this.imageSanitizer = imageSanitizer;
        this.availability = availability;
    }

    @PostMapping(value = "/api/analyze-goal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StyleSpecResponse analyzeGoal(@RequestParam(value = "image", required = false) MultipartFile image) throws IOException {
        return StyleSpecResponse.from(analyze(image, true), availability.isEnabled());
    }

    @PostMapping(value = "/api/analyze-portfolio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StyleSpecResponse analyzePortfolio(@RequestParam(value = "image", required = false) MultipartFile image) throws IOException {
        return StyleSpecResponse.from(analyze(image, false), availability.isEnabled());
    }

    private StyleSpec analyze(MultipartFile image, boolean goal) throws IOException {
        if (image == null || image.isEmpty()) {
            throw new ApiException("IMAGE_REQUIRED", "사진을 첨부해 주세요.");
        }
        ImageSanitizer.SanitizedImage sanitized;
        try (InputStream in = image.getInputStream()) {
            sanitized = imageSanitizer.sanitize(in);
        } catch (IOException e) {
            throw new ApiException("IMAGE_UNREADABLE", "이미지를 읽을 수 없습니다. JPEG 또는 PNG를 올려주세요.");
        }
        log.debug("분석 요청 goal={} size={}x{} hash={}", goal,
                sanitized.width(), sanitized.height(), sanitized.sha256());

        return goal ? analyzer.analyzeGoal(sanitized.bytes()) : analyzer.analyzePortfolio(sanitized.bytes());
    }
}
