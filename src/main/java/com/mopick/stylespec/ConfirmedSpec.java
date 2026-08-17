package com.mopick.stylespec;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** 8개 필드 확정 태그 묶음. 누락된 필드는 "모름"으로 채운다. */
public record ConfirmedSpec(List<ConfirmedTag> tags) {

    public static ConfirmedSpec of(List<ConfirmedTag> raw) {
        Map<StyleField, String> map = new EnumMap<>(StyleField.class);
        if (raw != null) {
            for (ConfirmedTag t : raw) {
                if (t != null && t.field() != null) {
                    map.put(t.field(), Vocabulary.normalize(t.field(), t.value()));
                }
            }
        }
        List<ConfirmedTag> full = new ArrayList<>();
        for (StyleField f : StyleField.values()) {
            full.add(new ConfirmedTag(f, map.get(f)));
        }
        return new ConfirmedSpec(full);
    }

    public Map<StyleField, String> valueByField() {
        Map<StyleField, String> map = new EnumMap<>(StyleField.class);
        for (ConfirmedTag t : tags) {
            map.put(t.field(), t.value());
        }
        for (StyleField f : StyleField.values()) {
            map.putIfAbsent(f, null);
        }
        return map;
    }
}
