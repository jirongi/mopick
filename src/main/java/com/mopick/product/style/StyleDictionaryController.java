package com.mopick.product.style;

import com.mopick.api.ApiException;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product/styles")
public class StyleDictionaryController {

    private final StyleDictionaryService styleDictionaryService;

    public StyleDictionaryController(StyleDictionaryService styleDictionaryService) {
        this.styleDictionaryService = styleDictionaryService;
    }

    @GetMapping
    public List<StyleDictionaryEntry> list(@RequestParam(required = false) StyleCategory category) {
        return styleDictionaryService.findAll(category == null ? StyleCategory.ALL : category);
    }

    @GetMapping("/search")
    public List<StyleDictionaryEntry> search(@RequestParam String q,
                                             @RequestParam(required = false) StyleCategory category) {
        return styleDictionaryService.search(q, category);
    }

    @GetMapping("/popular")
    public List<String> popular(@RequestParam(defaultValue = "5") int limit) {
        return styleDictionaryService.popularQueries(limit);
    }

    @GetMapping("/recommended")
    public List<StyleDictionaryEntry> recommended(@RequestParam(defaultValue = "4") int limit) {
        return styleDictionaryService.recommended(limit);
    }

    @GetMapping("/{id}")
    public StyleDictionaryEntry detail(@PathVariable String id) {
        return styleDictionaryService.findById(id)
                .orElseThrow(() -> new ApiException("STYLE_NOT_FOUND", "스타일을 찾을 수 없습니다."));
    }
}
