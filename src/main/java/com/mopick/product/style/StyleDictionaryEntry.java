package com.mopick.product.style;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mopick.stylespec.ConfirmedTag;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StyleDictionaryEntry(
        String id,
        String name,
        List<String> aliases,
        StyleCategory category,
        String styleFamily,
        String description,
        List<String> features,
        int popularRank,
        List<ConfirmedTag> defaultTags
) {
}
