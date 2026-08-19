package com.mopick.product.stylist;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StylistProfileFixture(
        String stylistId,
        String salonName,
        String title,
        String introduction,
        String naverPlaceId
) {
}

record StylistProfileCatalog(List<StylistProfileFixture> profiles) {
}
