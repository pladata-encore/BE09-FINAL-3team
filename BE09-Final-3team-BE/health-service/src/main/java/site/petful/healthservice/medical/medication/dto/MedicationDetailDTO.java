package site.petful.healthservice.medical.medication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicationDetailDTO {
    private Long scheduleNo;
    private String title;           // 약명 + 용량 표기
    private String mainType;        // MEDICATION
    private String subType;         // PILL|SUPPLEMENT
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalTime time;         // 대표 시간(startDate 기준)
    private List<LocalTime> times;  // 하루 N회일 때 전개 시간 목록
    private String frequency;       // 라벨 저장값
    private Boolean alarmEnabled;
    private Integer reminderDaysBefore;
    private Integer lastReminderDaysBefore;  // 마지막으로 설정했던 알림 시기

    // 상세(Detail)
    private String medicationName;
    private String dosage;
    private Integer durationDays;
    private Boolean isPrescription;  // true: 처방전으로 등록, false: 일반 등록
}


