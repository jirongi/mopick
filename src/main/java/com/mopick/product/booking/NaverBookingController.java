package com.mopick.product.booking;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product/booking")
public class NaverBookingController {

    private final NaverBookingLinkService naverBookingLinkService;

    public NaverBookingController(NaverBookingLinkService naverBookingLinkService) {
        this.naverBookingLinkService = naverBookingLinkService;
    }

    @GetMapping("/naver-link")
    public NaverBookingLinkService.BookingLinkResponse naverLink(@RequestParam String stylistId,
                                                                  @RequestParam(required = false) String portfolioId) {
        return naverBookingLinkService.createLink(stylistId, portfolioId);
    }
}
