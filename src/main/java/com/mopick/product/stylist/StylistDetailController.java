package com.mopick.product.stylist;

import com.mopick.api.ApiException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product/stylists")
public class StylistDetailController {

    private final StylistDetailService stylistDetailService;

    public StylistDetailController(StylistDetailService stylistDetailService) {
        this.stylistDetailService = stylistDetailService;
    }

    @GetMapping("/{stylistId}")
    public StylistDetailService.StylistDetailResponse detail(@PathVariable String stylistId) {
        return stylistDetailService.findByStylistId(stylistId)
                .orElseThrow(() -> new ApiException("STYLIST_NOT_FOUND", "디자이너를 찾을 수 없습니다."));
    }

    @GetMapping("/{stylistId}/portfolios/{portfolioId}")
    public StylistDetailService.PortfolioDetailResponse portfolio(@PathVariable String stylistId,
                                                                  @PathVariable String portfolioId) {
        return stylistDetailService.findPortfolio(stylistId, portfolioId)
                .orElseThrow(() -> new ApiException("PORTFOLIO_NOT_FOUND", "포트폴리오를 찾을 수 없습니다."));
    }
}
