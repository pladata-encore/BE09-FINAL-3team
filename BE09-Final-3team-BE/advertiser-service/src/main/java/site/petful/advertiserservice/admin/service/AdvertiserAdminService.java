package site.petful.advertiserservice.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.petful.advertiserservice.dto.advertisement.AdAdminResponse;
import site.petful.advertiserservice.dto.advertisement.AdResponse;
import site.petful.advertiserservice.dto.advertiser.AdvertiserAdminResponse;
import site.petful.advertiserservice.admin.dto.AdvertiserWithFilesResponse;
import site.petful.advertiserservice.entity.advertiser.Advertiser;
import org.springframework.web.server.ResponseStatusException;
import site.petful.advertiserservice.entity.advertisement.AdStatus;
import site.petful.advertiserservice.entity.advertisement.Advertisement;
import site.petful.advertiserservice.repository.AdRepository;
import site.petful.advertiserservice.repository.AdvertiserRepository;
import site.petful.advertiserservice.repository.FileRepository;
import site.petful.advertiserservice.entity.advertiser.AdvertiserFiles;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdvertiserAdminService {
    private final AdvertiserRepository advertiserRepository;
    private final AdRepository adRepository;
    private final FileRepository fileRepository;
    // 광고주 제한
    public void restrictAdvertiser(Long id) {
        Advertiser restrictAdvertiser = advertiserRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "해당 광고주를 찾을 수 없습니다."));
        restrictAdvertiser.suspend();
    }
    // 광고주 제한 거절
    @Transactional
    public void rejectAdvertiser(Long id,String reason) {
        Advertiser rejectAdvertiser = advertiserRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "해당 광고주를 찾을 수 없습니다."));
        rejectAdvertiser.setReason(reason);
        advertiserRepository.save(rejectAdvertiser);
    }
    @Transactional
    public void approveAdvertiser(Long id) {
        Advertiser approveAdvertiser = advertiserRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "해당 광고주를 찾을 수 없습니다."));
        approveAdvertiser.setIsApproved(true);
        advertiserRepository.save(approveAdvertiser);
    }

    public Page<AdvertiserWithFilesResponse> getAllAdvertiser(Pageable pageable) {
        log.info("🔍 [AdvertiserAdminService] 미승인 광고주 목록 조회 시작");
        
        Page<Advertiser> advertisers = advertiserRepository.findByIsApprovedFalseAndReasonIsNull(pageable);
        log.info("📋 [AdvertiserAdminService] 조회된 광고주 수: {}", advertisers.getTotalElements());
        
        return advertisers.map(advertiser -> {
            log.info("🔍 [AdvertiserAdminService] 광고주 {} 파일 조회 시작", advertiser.getAdvertiserNo());
            
            // 각 광고주의 파일 조회
            List<AdvertiserFiles> files = fileRepository.findByAdvertiser_AdvertiserNo(advertiser.getAdvertiserNo())
                    .orElse(new ArrayList<>());
            log.info("📁 [AdvertiserAdminService] 광고주 {} 파일 수: {}", advertiser.getAdvertiserNo(), files.size());
            
            return AdvertiserWithFilesResponse.from(advertiser, files);
        });
    }

    public Page<AdAdminResponse> getAllCampaign(Pageable pageable) {
        return adRepository.findByAdStatus(AdStatus.TRIAL, pageable).map(AdAdminResponse::from);
    }

    @Transactional
    public void deleteCampaign(Long adId) {
        Advertisement ad = adRepository.findById(adId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "해당 광고를 찾을 수 없습니다."
                ));
        if (ad.getAdStatus() == AdStatus.REJECTED) {
            return; // 이미 삭제 상태면 아무 것도 안 함
        }
        ad.setAdStatus(AdStatus.REJECTED);
    }

    public Page<AdAdminResponse> getPendingAds(Pageable pageable) {
       return adRepository.findByAdStatus(AdStatus.PENDING, pageable).map(AdAdminResponse::from);
    }

    public void approve(Long adId) {
        Advertisement ad = adRepository.findByAdNo(adId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "해당 광고를 찾을 수 없습니다."
                ));

        if (ad.getAdStatus() == AdStatus.APPROVED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "이미 승인된 광고입니다."
            );
        }

        ad.setAdStatus(AdStatus.APPROVED);
    }

    public void reject(Long adId, String reason) {
        Advertisement ad = adRepository.findByAdNo(adId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "해당 광고를 찾을 수 없습니다."
                ));

        if (ad.getAdStatus() == AdStatus.REJECTED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "이미 거절된 광고입니다."
            );
        }

        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "거절 사유를 입력해야 합니다."
            );
        }

        ad.setAdStatus(AdStatus.REJECTED);
        ad.setReason(reason);
    }

}
