package site.petful.healthservice.activity.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import site.petful.healthservice.activity.dto.ActivityRequest;
import site.petful.healthservice.activity.dto.ActivityResponse;
import site.petful.healthservice.activity.dto.ActivityChartResponse;
import site.petful.healthservice.activity.entity.Activity;
import site.petful.healthservice.activity.entity.ActivityMeal;
import site.petful.healthservice.activity.enums.ActivityLevel;
import site.petful.healthservice.activity.repository.ActivityRepository;
import site.petful.healthservice.common.exception.BusinessException;
import site.petful.healthservice.common.response.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.petful.healthservice.common.client.PetServiceClient;
import site.petful.healthservice.common.dto.PetResponse;
import site.petful.healthservice.common.response.ApiResponse;
import site.petful.healthservice.activity.dto.ActivityUpdateRequest;
import site.petful.healthservice.activity.dto.ActivityUpdateResponse;


import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityService {
    
    private final ActivityRepository activityRepository;
    private final PetServiceClient petServiceClient;
    
    @Transactional
    public Long createActivity(Long userNo, ActivityRequest request) {

        

        
        // 같은 날짜에 이미 활동 데이터가 있는지 확인
        if (activityRepository.existsByPetNoAndActivityDate(request.getPetNo(), request.getActivityDate())) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "해당 날짜에 이미 활동 데이터가 존재합니다. 하루에 하나의 활동 기록만 가능합니다.");
        }
        
        // 칼로리 자동 계산
        int recommendedCaloriesBurned = calculateRecommendedCaloriesBurned(request.getWeightKg(), request.getActivityLevel());
        int caloriesBurned = calculateCaloriesBurned(request.getWalkingDistanceKm(), request.getActivityLevel());
        
        // Activity 엔티티 생성
        Activity activity = Activity.builder()
                .userNo(userNo)
                .petNo(request.getPetNo())
                .activityDate(request.getActivityDate())
                .walkingDistanceKm(request.getWalkingDistanceKm())
                .activityLevel(request.getActivityLevel())
                .caloriesBurned(caloriesBurned)
                .recommendedCaloriesBurned(recommendedCaloriesBurned)
                .weightKg(request.getWeightKg())
                .sleepHours(request.getSleepHours())
                .poopCount(request.getPoopCount())
                .peeCount(request.getPeeCount())
                .memo(request.getMemo())
                .build();
        
        // 식사 정보 추가
        request.getMeals().forEach(mealRequest -> {
            // 섭취 칼로리 자동 계산
            int consumedCalories = calculateConsumedCalories(
                mealRequest.getTotalCalories(),
                mealRequest.getTotalWeightG(),
                mealRequest.getConsumedWeightG()
            );
            
            activity.addMeal(ActivityMeal.builder()
                .totalWeightG(mealRequest.getTotalWeightG())
                .totalCalories(mealRequest.getTotalCalories())
                .consumedWeightG(mealRequest.getConsumedWeightG())
                .consumedCalories(consumedCalories)
                .mealType(mealRequest.getMealType())
                .build());
        });
        
        Activity savedActivity = activityRepository.save(activity);
        
        return savedActivity.getActivityNo();
    }
    
    // 권장 소모 칼로리 = 무게(kg) × 활동계수 × 70
    private int calculateRecommendedCaloriesBurned(Double weightKg, ActivityLevel activityLevel) {
        return (int) Math.round(weightKg * activityLevel.getValue() * 70);
    }
    
    // 소모 칼로리 = 산책 거리(km) × 활동계수 × 5
    private int calculateCaloriesBurned(Double walkingDistanceKm, ActivityLevel activityLevel) {
        return (int) Math.round(walkingDistanceKm * activityLevel.getValue() * 5);
    }
    
    // 섭취 칼로리 = 칼로리/그램 × 섭취량(g)
    private int calculateConsumedCalories(Integer totalCalories, Double totalWeightG, Double consumedWeightG) {
        double caloriesPerGram = (double) totalCalories / totalWeightG;
        return (int) Math.round(caloriesPerGram * consumedWeightG);
    }
    
    // 권장 섭취 칼로리 = 무게(kg) × 활동계수 × 100
    private int calculateRecommendedCaloriesIntake(Double weightKg, ActivityLevel activityLevel) {
        return (int) Math.round(weightKg * activityLevel.getValue() * 100);
    }
    
    /**
     * 특정 날짜의 활동 데이터 조회
     */
    public ActivityResponse getActivity(Long userNo, Long petNo, String activityDateStr) {

        

        
        LocalDate activityDate = LocalDate.parse(activityDateStr);
        
        Activity activity = activityRepository.findByPetNoAndActivityDate(petNo, activityDate)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND, "해당 날짜의 활동 데이터가 존재하지 않습니다."));
        
        // 사용자 권한 확인
        if (!activity.getUserNo().equals(userNo)) {
            throw new BusinessException(ErrorCode.ACTIVITY_FORBIDDEN, "해당 활동 데이터에 대한 접근 권한이 없습니다.");
        }
        
        return convertToResponse(activity);
    }
    
    /**
     * 활동 데이터 부분 수정
     */
    @Transactional
    public ActivityUpdateResponse updateActivity(Long userNo, Long activityNo, ActivityUpdateRequest request) {
        log.info("활동 데이터 부분 수정 시작: userNo={}, activityNo={}", userNo, activityNo);

        // 활동 데이터 조회
        Activity activity = activityRepository.findById(activityNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND, "해당 활동 데이터가 존재하지 않습니다."));

        // 사용자 권한 확인
        if (!activity.getUserNo().equals(userNo)) {
            throw new BusinessException(ErrorCode.ACTIVITY_FORBIDDEN, "해당 활동 데이터에 대한 접근 권한이 없습니다.");
        }


        // 수정 전 데이터 저장
        ActivityUpdateResponse.ActivityData beforeData = convertToActivityData(activity);

        // 기본 필드 부분 수정
        if (request.getActivityDate() != null) {
            activity.setActivityDate(request.getActivityDate());
            log.info("활동 날짜 수정: {}", request.getActivityDate());
        }

        if (request.getWalkingDistanceKm() != null) {
            activity.setWalkingDistanceKm(request.getWalkingDistanceKm());
            log.info("산책 거리 수정: {}", request.getWalkingDistanceKm());
        }

        if (request.getActivityLevel() != null) {
            activity.setActivityLevel(request.getActivityLevel());
            log.info("활동 계수 수정: {}", request.getActivityLevel());
        }

        if (request.getWeightKg() != null) {
            activity.setWeightKg(request.getWeightKg());
            log.info("무게 수정: {}", request.getWeightKg());
        }

        if (request.getSleepHours() != null) {
            activity.setSleepHours(request.getSleepHours());
            log.info("수면 시간 수정: {}", request.getSleepHours());
        }

        if (request.getPoopCount() != null) {
            activity.setPoopCount(request.getPoopCount());
            log.info("대변 횟수 수정: {}", request.getPoopCount());
        }

        if (request.getPeeCount() != null) {
            activity.setPeeCount(request.getPeeCount());
            log.info("소변 횟수 수정: {}", request.getPeeCount());
        }

        if (request.getMemo() != null) {
            activity.setMemo(request.getMemo());
            log.info("메모 수정: {}", request.getMemo());
        }

        // 식사 정보 부분 수정
        if (request.getMeals() != null) {
            updateMeals(activity, request.getMeals());
        }

        // 칼로리 재계산
        recalculateCalories(activity);

        // 활동 데이터 저장
        Activity updatedActivity = activityRepository.save(activity);
        log.info("활동 데이터 부분 수정 완료: activityNo={}", activityNo);

        // 수정 후 데이터 생성
        ActivityUpdateResponse.ActivityData afterData = convertToActivityData(updatedActivity);

        return ActivityUpdateResponse.builder()
                .activityNo(activityNo)
                .userNo(userNo)
                .petNo(activity.getPetNo())
                .before(beforeData)
                .after(afterData)
                .updatedAt(updatedActivity.getUpdatedAt())
                .build();
    }

    /**
     * 식사 정보 부분 수정
     */
    private void updateMeals(Activity activity, List<ActivityUpdateRequest.MealUpdateRequest> mealRequests) {
        log.info("식사 정보 부분 수정 시작: 기존 식사 개수={}, 요청 식사 개수={}",
                activity.getMeals().size(), mealRequests.size());

        // 기존 식사 정보를 Map으로 변환 (mealNo를 키로 사용)
        Map<Long, ActivityMeal> existingMealsMap = activity.getMeals().stream()
                .collect(Collectors.toMap(ActivityMeal::getMealNo, meal -> meal));

        // 새로운 식사 정보 리스트
        List<ActivityMeal> newMeals = new ArrayList<>();

        for (ActivityUpdateRequest.MealUpdateRequest mealRequest : mealRequests) {
            if (mealRequest.getIsDeleted() != null && mealRequest.getIsDeleted()) {
                // 삭제 요청인 경우
                if (mealRequest.getMealNo() != null) {
                    existingMealsMap.remove(mealRequest.getMealNo());
                    log.info("식사 정보 삭제: mealNo={}", mealRequest.getMealNo());
                }
            } else if (mealRequest.getMealNo() != null && existingMealsMap.containsKey(mealRequest.getMealNo())) {
                // 기존 식사 정보 수정
                ActivityMeal existingMeal = existingMealsMap.get(mealRequest.getMealNo());
                updateExistingMeal(existingMeal, mealRequest);
                newMeals.add(existingMeal);
                existingMealsMap.remove(mealRequest.getMealNo());
                log.info("기존 식사 정보 수정: mealNo={}", mealRequest.getMealNo());
            } else {
                // 새로운 식사 정보 추가
                ActivityMeal newMeal = createNewMeal(mealRequest);
                newMeals.add(newMeal);
                log.info("새로운 식사 정보 추가: mealType={}", mealRequest.getMealType());
            }
        }

        // 기존 식사 정보 중 수정되지 않은 것들 추가
        newMeals.addAll(existingMealsMap.values());

        // 활동의 식사 정보 업데이트
        activity.getMeals().clear();
        newMeals.forEach(activity::addMeal);

        log.info("식사 정보 부분 수정 완료: 최종 식사 개수={}", newMeals.size());
    }

    /**
     * 기존 식사 정보 업데이트
     */
    private void updateExistingMeal(ActivityMeal existingMeal, ActivityUpdateRequest.MealUpdateRequest mealRequest) {
        if (mealRequest.getTotalWeightG() != null) {
            existingMeal.setTotalWeightG(mealRequest.getTotalWeightG());
        }
        if (mealRequest.getTotalCalories() != null) {
            existingMeal.setTotalCalories(mealRequest.getTotalCalories());
        }
        if (mealRequest.getConsumedWeightG() != null) {
            existingMeal.setConsumedWeightG(mealRequest.getConsumedWeightG());
        }
        if (mealRequest.getConsumedCalories() != null) {
            existingMeal.setConsumedCalories(mealRequest.getConsumedCalories());
        }
        if (mealRequest.getMealType() != null) {
            existingMeal.setMealType(mealRequest.getMealType());
        }
    }

    /**
     * 새로운 식사 정보 생성
     */
    private ActivityMeal createNewMeal(ActivityUpdateRequest.MealUpdateRequest mealRequest) {
        int consumedCalories = calculateConsumedCalories(
            mealRequest.getTotalCalories(),
            mealRequest.getTotalWeightG(),
            mealRequest.getConsumedWeightG()
        );

        return ActivityMeal.builder()
                .totalWeightG(mealRequest.getTotalWeightG())
                .totalCalories(mealRequest.getTotalCalories())
                .consumedWeightG(mealRequest.getConsumedWeightG())
                .consumedCalories(consumedCalories)
                .mealType(mealRequest.getMealType())
                .build();
    }

    /**
     * 칼로리 재계산
     */
    private void recalculateCalories(Activity activity) {
        // 권장 소모 칼로리 재계산
        int recommendedCaloriesBurned = calculateRecommendedCaloriesBurned(activity.getWeightKg(), activity.getActivityLevel());
        activity.setRecommendedCaloriesBurned(recommendedCaloriesBurned);

        // 실제 소모 칼로리 재계산
        int caloriesBurned = calculateCaloriesBurned(activity.getWalkingDistanceKm(), activity.getActivityLevel());
        activity.setCaloriesBurned(caloriesBurned);

        log.info("칼로리 재계산 완료: 권장={}, 실제={}", recommendedCaloriesBurned, caloriesBurned);
    }

    /**
     * 스케줄에서 기록이 있는 날짜 목록 조회
     */
    public List<String> getActivitySchedule(Long userNo, Long petNo, int year, int month) {


        
        // 해당 월의 시작일과 마지막일 계산
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);
        
        List<Activity> activities = activityRepository.findByPetNoAndDateRange(petNo, startDate, endDate);
        
        // 사용자 권한 확인 및 날짜만 추출
        return activities.stream()
                .filter(activity -> activity.getUserNo().equals(userNo))
                .map(activity -> activity.getActivityDate().toString())
                .sorted()
                .collect(Collectors.toList());
    }
    
    /**
     * 차트 데이터 조회
     * startDate, endDate가 없으면 당일 데이터만 조회
     */
    public ActivityChartResponse getActivityChartData(Long userNo, Long petNo, String startDateStr, String endDateStr) {
        try {
            LocalDate startDate;
            LocalDate endDate;

            // startDate, endDate가 제공되지 않은 경우 당일 데이터만 조회
            if (startDateStr == null || endDateStr == null) {
                LocalDate today = LocalDate.now();
                startDate = today;
                endDate = today;
            } else {
                startDate = LocalDate.parse(startDateStr);
                endDate = LocalDate.parse(endDateStr);
                
                // 날짜 유효성 검증
                if (startDate.isAfter(endDate)) {
                    throw new BusinessException(ErrorCode.DATE_RANGE_INVALID, "시작일은 종료일보다 이전이어야 합니다.");
                }
                
                // 최대 7일 제한
                if (startDate.until(endDate).getDays() > 7) {
                    throw new BusinessException(ErrorCode.DATE_RANGE_INVALID, "최대 7일까지 조회 가능합니다.");
                }
            }
            
            List<Activity> activities = activityRepository.findByPetNoAndDateRange(petNo, startDate, endDate);
            
            // 사용자 권한 확인
            activities = activities.stream()
                    .filter(activity -> activity.getUserNo().equals(userNo))
                    .collect(Collectors.toList());
            
            List<ActivityChartResponse.ChartData> chartDataList = activities.stream()
                    .map(activity -> convertToChartData(activity))
                    .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                    .collect(Collectors.toList());
            
            // 요약 통계 계산
            ActivityChartResponse.SummaryStats summaryStats = calculateSummaryStats(activities);
            
            return ActivityChartResponse.builder()
                    .chartData(chartDataList)
                    .summaryStats(summaryStats)
                    .build();
                    
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("활동 차트 데이터 조회 중 오류 발생: userNo={}, petNo={}, startDate={}, endDate={}", 
                     userNo, petNo, startDateStr, endDateStr, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "활동 차트 데이터 조회 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * Activity 엔티티를 ChartData DTO로 변환
     */
    private ActivityChartResponse.ChartData convertToChartData(Activity activity) {
        String displayDate = getDisplayDate(activity.getActivityDate());
        
        return ActivityChartResponse.ChartData.builder()
                .date(activity.getActivityDate().toString())
                .displayDate(displayDate)
                .recommendedCaloriesBurned(activity.getRecommendedCaloriesBurned())
                .actualCaloriesBurned(activity.getCaloriesBurned())
                .recommendedCaloriesIntake(calculateRecommendedCaloriesIntake(activity.getWeightKg(), activity.getActivityLevel()))
                .actualCaloriesIntake(calculateTotalConsumedCalories(activity))
                .poopCount(activity.getPoopCount())
                .peeCount(activity.getPeeCount())
                .sleepHours(activity.getSleepHours())
                .build();
    }
    
    /**
     * 표시용 날짜 문자열 생성 (요일 표시)
     */
    private String getDisplayDate(LocalDate date) {
        String[] days = {"월", "화", "수", "목", "금", "토", "일"};
        return days[date.getDayOfWeek().getValue() - 1];
    }
    
    /**
     * 요약 통계 계산
     */
    private ActivityChartResponse.SummaryStats calculateSummaryStats(List<Activity> activities) {
        int totalDays = activities.size();
        
        if (totalDays == 0) {
            return ActivityChartResponse.SummaryStats.builder()
                    .totalDays(0)
                    .totalWalkingDistance(0.0)
                    .totalCaloriesBurned(0)
                    .totalCaloriesIntake(0)
                    .averageSleepHours(0.0)
                    .totalPoopCount(0)
                    .totalPeeCount(0)
                    .averageWalkingDistance(0.0)
                    .averageCaloriesBurned(0.0)
                    .averageCaloriesIntake(0.0)
                    .averagePoopCount(0.0)
                    .averagePeeCount(0.0)
                    .build();
        }
        
        // 총합 계산
        double totalWalkingDistance = activities.stream()
                .mapToDouble(activity -> activity.getWalkingDistanceKm() != null ? activity.getWalkingDistanceKm() : 0.0)
                .sum();
        
        int totalCaloriesBurned = activities.stream()
                .mapToInt(activity -> activity.getCaloriesBurned() != null ? activity.getCaloriesBurned() : 0)
                .sum();
        
        int totalCaloriesIntake = activities.stream()
                .mapToInt(this::calculateTotalConsumedCalories)
                .sum();
        
        double totalSleepHours = activities.stream()
                .mapToDouble(activity -> activity.getSleepHours() != null ? activity.getSleepHours() : 0.0)
                .sum();
        
        int totalPoopCount = activities.stream()
                .mapToInt(activity -> activity.getPoopCount() != null ? activity.getPoopCount() : 0)
                .sum();
        
        int totalPeeCount = activities.stream()
                .mapToInt(activity -> activity.getPeeCount() != null ? activity.getPeeCount() : 0)
                .sum();
        
        // 평균 계산
        double averageWalkingDistance = totalWalkingDistance / totalDays;
        double averageCaloriesBurned = (double) totalCaloriesBurned / totalDays;
        double averageCaloriesIntake = (double) totalCaloriesIntake / totalDays;
        double averageSleepHours = totalSleepHours / totalDays;
        double averagePoopCount = (double) totalPoopCount / totalDays;
        double averagePeeCount = (double) totalPeeCount / totalDays;
        
        return ActivityChartResponse.SummaryStats.builder()
                .totalDays(totalDays)
                .totalWalkingDistance(totalWalkingDistance)
                .totalCaloriesBurned(totalCaloriesBurned)
                .totalCaloriesIntake(totalCaloriesIntake)
                .averageSleepHours(averageSleepHours)
                .totalPoopCount(totalPoopCount)
                .totalPeeCount(totalPeeCount)
                .averageWalkingDistance(averageWalkingDistance)
                .averageCaloriesBurned(averageCaloriesBurned)
                .averageCaloriesIntake(averageCaloriesIntake)
                .averagePoopCount(averagePoopCount)
                .averagePeeCount(averagePeeCount)
                .build();
    }
    
    /**
     * 총 섭취 칼로리 계산
     */
    private Integer calculateTotalConsumedCalories(Activity activity) {
        if (activity.getMeals() == null || activity.getMeals().isEmpty()) {
            return 0;
        }
        return activity.getMeals().stream()
                .mapToInt(meal -> meal.getConsumedCalories() != null ? meal.getConsumedCalories() : 0)
                .sum();
    }
    
    /**
     * Activity 엔티티를 ActivityData DTO로 변환 (수정 전후 비교용)
     */
    private ActivityUpdateResponse.ActivityData convertToActivityData(Activity activity) {
        return ActivityUpdateResponse.ActivityData.builder()
                .activityDate(activity.getActivityDate())
                .walkingDistanceKm(activity.getWalkingDistanceKm())
                .activityLevel(activity.getActivityLevel())
                .caloriesBurned(activity.getCaloriesBurned())
                .recommendedCaloriesBurned(activity.getRecommendedCaloriesBurned())
                .recommendedCaloriesIntake(calculateRecommendedCaloriesIntake(activity.getWeightKg(), activity.getActivityLevel()))
                .weightKg(activity.getWeightKg())
                .sleepHours(activity.getSleepHours())
                .poopCount(activity.getPoopCount())
                .peeCount(activity.getPeeCount())
                .memo(activity.getMemo())
                .meals(activity.getMeals().stream()
                        .map(meal -> ActivityUpdateResponse.MealData.builder()
                                .mealNo(meal.getMealNo())
                                .totalWeightG(meal.getTotalWeightG())
                                .totalCalories(meal.getTotalCalories())
                                .consumedWeightG(meal.getConsumedWeightG())
                                .consumedCalories(meal.getConsumedCalories())
                                .mealType(meal.getMealType())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * Activity 엔티티를 Response DTO로 변환
     */
    private ActivityResponse convertToResponse(Activity activity) {
        return ActivityResponse.builder()
                .activityNo(activity.getActivityNo())
                .userNo(activity.getUserNo())
                .petNo(activity.getPetNo())
                .activityDate(activity.getActivityDate())
                .walkingDistanceKm(activity.getWalkingDistanceKm())
                .activityLevel(activity.getActivityLevel())
                .caloriesBurned(activity.getCaloriesBurned())
                .recommendedCaloriesBurned(activity.getRecommendedCaloriesBurned())
                .recommendedCaloriesIntake(calculateRecommendedCaloriesIntake(activity.getWeightKg(), activity.getActivityLevel()))
                .weightKg(activity.getWeightKg())
                .sleepHours(activity.getSleepHours())
                .poopCount(activity.getPoopCount())
                .peeCount(activity.getPeeCount())
                .memo(activity.getMemo())
                .meals(activity.getMeals().stream()
                        .map(meal -> ActivityResponse.MealResponse.builder()
                                .mealNo(meal.getMealNo())
                                .totalWeightG(meal.getTotalWeightG())
                                .totalCalories(meal.getTotalCalories())
                                .consumedWeightG(meal.getConsumedWeightG())
                                .consumedCalories(meal.getConsumedCalories())
                                .mealType(meal.getMealType())
                                .build())
                        .collect(Collectors.toList()))
                .createdAt(activity.getCreatedAt())
                .updatedAt(activity.getUpdatedAt())
                .build();
    }


    /**
     * 사용자별 펫 프로필 목록 조회
     */
    public List<PetResponse> getUserPets(Long userNo) {

        try {
            ApiResponse<List<PetResponse>> petsResponse = petServiceClient.getPets(userNo);
            
            if (petsResponse != null && petsResponse.getData() != null) {
                return petsResponse.getData();
            }
            
            return new ArrayList<>();
            
        } catch (Exception e) {
            log.error("사용자 펫 목록 조회 중 예외 발생: userNo={}", userNo, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "펫 목록 조회 중 오류가 발생했습니다.");
        }
    }


}
