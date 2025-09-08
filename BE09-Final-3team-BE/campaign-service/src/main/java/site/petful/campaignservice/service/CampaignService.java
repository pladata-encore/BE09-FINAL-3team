package site.petful.campaignservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.petful.campaignservice.client.AdvertiserFeignClient;
import site.petful.campaignservice.client.PetFeignClient;
import site.petful.campaignservice.client.UserFeignClient;
import site.petful.campaignservice.common.ApiResponse;
import site.petful.campaignservice.common.ErrorCode;
import site.petful.campaignservice.dto.advertisement.AdResponse;
import site.petful.campaignservice.dto.campaign.ApplicantResponse;
import site.petful.campaignservice.dto.campaign.ApplicantRequest;
import site.petful.campaignservice.dto.pet.PetResponse;
import site.petful.campaignservice.dto.campaign.ApplicantsResponse;
import site.petful.campaignservice.entity.Applicant;
import site.petful.campaignservice.entity.ApplicantStatus;
import site.petful.campaignservice.repository.CampaignRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CampaignService {

    public final CampaignRepository campaignRepository;
    private final AdvertiserFeignClient advertiserFeignClient;
    private final PetFeignClient petFeignClient;

    // 1. 체험단 신청
    public ApplicantResponse applyCampaign(Long adNo, Long petNo, ApplicantRequest request) {

        ApiResponse<AdResponse> adResponse = advertiserFeignClient.getAd(adNo);
        if (adResponse == null || adResponse.getData() == null) {
            throw new RuntimeException(ErrorCode.AD_NOT_FOUND.getDefaultMessage());
        }

        ApiResponse<PetResponse> petResponse = petFeignClient.getPet(petNo);
        PetResponse pet = petResponse.getData();



        Applicant applicant = new Applicant();
        applicant.setAdNo(adNo);
        applicant.setPetNo(petNo);
        applicant.setContent(request.getContent());
        applicant.setStatus(applicant.getStatus() == null ? ApplicantStatus.APPLIED : applicant.getStatus());
        applicant.setIsSaved(false);
        applicant.setIsDeleted(false);
        Applicant saved = campaignRepository.save(applicant);

        // advertiser의 applicants 1 증가
        advertiserFeignClient.updateAdByCampaign(adNo, 1);

        return ApplicantResponse.from(saved, pet);
    }

    // 2. 광고별 체험단 전체 조회 - 광고주
    @Transactional(readOnly = true)
    public ApplicantsResponse getApplicants(Long adNo) {
        ApiResponse<AdResponse> adResponse = advertiserFeignClient.getAd(adNo);
        if (adResponse == null || adResponse.getData() == null) {
            throw new RuntimeException(ErrorCode.AD_NOT_FOUND.getDefaultMessage());
        }

        List<Applicant> applicants = campaignRepository.findByAdNo(adNo);

        List<Long> petNos = applicants.stream()
                .map(Applicant::getPetNo)
                .distinct()
                .toList();

        List<PetResponse> pets = petFeignClient.getPetsByPetNos(petNos).getData();

        Map<Long, PetResponse> petMap = pets.stream()
                .collect(Collectors.toMap(PetResponse::getPetNo, pet -> pet));

        List<ApplicantResponse> applicantResponses = applicants.stream()
                .map(applicant -> ApplicantResponse.from(applicant, petMap.get(applicant.getPetNo())))
                .collect(Collectors.toList());

        return ApplicantsResponse.from(applicantResponses);
    }

    // 2-2. petNo로 체험단 신청 내역 조회
    public ApplicantsResponse getApplicantsByPetNo(Long petNo) {
        ApiResponse<PetResponse> petResponse = petFeignClient.getPet(petNo);
        if (petResponse == null || petResponse.getData() == null) {
            throw new RuntimeException(ErrorCode.AD_NOT_FOUND.getDefaultMessage());
        }

        List<Applicant> applicants = campaignRepository.findByPetNo(petNo);

        List<ApplicantResponse> applicantResponses = applicants.stream()
                .map(applicant -> ApplicantResponse.from(applicant, petResponse.getData()))
                .collect(Collectors.toList());

        return ApplicantsResponse.from(applicantResponses);
    }

    // 2-3. 광고 + 사용자별 신청자 조회
    public List<ApplicantResponse> getApplicantsByAd(Long userNo, Long adNo) {
        List<Applicant> applicants = campaignRepository.findByAdNo(adNo);
        List<PetResponse> petResponses = petFeignClient.getPetsExternal(userNo).getData();
        List<Long> petNos = petResponses.stream().map(PetResponse::getPetNo).toList();

        List<Applicant> filteredApplicants = applicants.stream()
                .filter(applicant -> petNos.contains(applicant.getPetNo())).toList();

        return filteredApplicants.stream()
                .map(applicant -> {
                    PetResponse pet = petResponses.stream()
                            .filter(p -> p.getPetNo().equals(applicant.getPetNo()))
                            .findFirst()
                            .orElse(null);
                    return ApplicantResponse.from(applicant, pet);
                })
                .collect(Collectors.toList());
    }

    // 3. 체험단 추가 내용 수정
    public ApplicantResponse updateApplicant(Long applicantNo, ApplicantRequest request) {

        Applicant applicant = campaignRepository.findApplicantByApplicantNo(applicantNo)
                .orElseThrow(() -> new RuntimeException(ErrorCode.APPLICANT_NOT_FOUND.getDefaultMessage()));

        if(request.getContent() != null) {
            applicant.setContent(request.getContent());
        }
        if(request.getIsSaved() != null) {
            applicant.setIsSaved(request.getIsSaved());
        }
        if(request.getStatus() != null) {
            applicant.setStatus(request.getStatus());
        }

        applicant.setContent(request.getContent());
        Applicant saved = campaignRepository.save(applicant);

        ApiResponse<PetResponse> petResponse = petFeignClient.getPet(saved.getPetNo());
        PetResponse pet = petResponse.getData();

        return ApplicantResponse.from(saved, pet);
    }

    // 4. 체험단 신청 취소
    public void cancelApplicant(Long applicantNo) {

        Applicant applicant = campaignRepository.findApplicantByApplicantNo(applicantNo)
                .orElseThrow(() -> new RuntimeException(ErrorCode.APPLICANT_NOT_FOUND.getDefaultMessage()));

        campaignRepository.delete(applicant);

        // advertiser의 applicants 1 감소
        advertiserFeignClient.updateAdByCampaign(applicant.getAdNo(), -1);
    }

    // 4-2. 체험단 소프트 삭제
    public ApplicantResponse deleteApplicant(Long applicantNo, Boolean isDeleted) {

        Applicant applicant = campaignRepository.findApplicantByApplicantNo(applicantNo)
                .orElseThrow(() -> new RuntimeException(ErrorCode.APPLICANT_NOT_FOUND.getDefaultMessage()));

        applicant.setIsDeleted(isDeleted);

        ApiResponse<PetResponse> petResponse = petFeignClient.getPet(applicant.getPetNo());
        PetResponse pet = petResponse.getData();

        return ApplicantResponse.from(applicant, pet);
    }
}
