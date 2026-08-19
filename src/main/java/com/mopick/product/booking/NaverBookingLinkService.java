package com.mopick.product.booking;

import com.mopick.api.ApiException;
import com.mopick.product.config.NaverProperties;
import com.mopick.product.stylist.StylistDetailService;
import org.springframework.stereotype.Service;

@Service
public class NaverBookingLinkService {

    private final NaverProperties naverProperties;
    private final StylistDetailService stylistDetailService;

    public NaverBookingLinkService(NaverProperties naverProperties, StylistDetailService stylistDetailService) {
        this.naverProperties = naverProperties;
        this.stylistDetailService = stylistDetailService;
    }

    public BookingLinkResponse createLink(String stylistId, String portfolioId) {
        var stylist = stylistDetailService.findByStylistId(stylistId)
                .orElseThrow(() -> new ApiException("STYLIST_NOT_FOUND", "디자이너를 찾을 수 없습니다."));
        String placeId = stylist.naverPlaceId() != null ? stylist.naverPlaceId() : "unknown";
        String url = naverProperties.bookingBaseUrl()
                + "?placeId=" + placeId
                + "&stylistId=" + stylistId
                + (portfolioId != null ? "&portfolioId=" + portfolioId : "");
        return new BookingLinkResponse(url, stylist.salonName(), stylist.stylistName(), portfolioId);
    }

    public record BookingLinkResponse(String url, String salonName, String stylistName, String portfolioId) {
    }
}
