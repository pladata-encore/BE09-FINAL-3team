package site.petful.advertiserservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.petful.advertiserservice.common.ApiResponse;
import site.petful.advertiserservice.common.ApiResponseGenerator;
import site.petful.advertiserservice.dto.campaign.*;
import site.petful.advertiserservice.service.PetService;

import java.util.List;

@RestController
@RequestMapping("/pet")
public class PetController {

    private final PetService petService;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    // 1. 펫스타 전체 목록 조회
    @GetMapping("/petstars")
    public ResponseEntity<ApiResponse<List<PetResponse>>> getAllPetstars() {
        List<PetResponse> response = petService.geAllPetstars();
        return ResponseEntity.ok(ApiResponseGenerator.success(response));
    }

    // 1-2. 펫스타별 인스타 media 목록 조회
    @GetMapping("/petstars/media")
    public ResponseEntity<ApiResponse<List<InstagramMediaDto>>> getAllPetstarsMedia() {
        List<InstagramMediaDto> response = petService.geAllPetstarsMedia();
        return ResponseEntity.ok(ApiResponseGenerator.success(response));
    }

    // 2. 반려동물 상세 조회
    @GetMapping("/{petNo}")
    public ResponseEntity<ApiResponse<PetResponse>> getPet(@PathVariable Long petNo) {
        PetResponse response = petService.getPet(petNo);
        return ResponseEntity.ok(ApiResponseGenerator.success(response));
    }

    // 3. 포트폴리오 조회
    @GetMapping("/portfolio/{petNo}")
    public ResponseEntity<ApiResponse<PortfolioResponse>> getPortfolioExternal(@PathVariable Long petNo) {
        PortfolioResponse response = petService.getPortfolio(petNo);
        return ResponseEntity.ok(ApiResponseGenerator.success(response));
    }

    // 4. 활동 이력 조회
    @GetMapping("/history/{petNo}")
    public ResponseEntity<ApiResponse<List<HistoryResponse>>> getHistoryExternal(@PathVariable Long petNo) {
        List<HistoryResponse> response = petService.getHistory(petNo);
        return ResponseEntity.ok(ApiResponseGenerator.success(response));
    }

}