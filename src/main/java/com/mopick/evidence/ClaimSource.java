package com.mopick.evidence;

import com.fasterxml.jackson.annotation.JsonValue;

/** claim의 근거 출처. 우선순위는 confirmed_tag > stylist_metadata > pairwise_ai. */
public enum ClaimSource {
    CONFIRMED_TAG("confirmed_tag"),
    STYLIST_METADATA("stylist_metadata"),
    PAIRWISE_AI("pairwise_ai");

    private final String json;

    ClaimSource(String json) {
        this.json = json;
    }

    @JsonValue
    public String json() {
        return json;
    }
}
