package site.petful.healthservice.medical.care.dto;

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
public class CareResponseDTO {
    private Long scheduleNo;
    private String title;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String mainType;
    private String subType;
    private String frequency; // 라벨 저장값
    private Boolean alarmEnabled;
    private Integer reminderDaysBefore; // 대표값(첫 번째)
    private Integer lastReminderDaysBefore;  // 마지막으로 설정했던 알림 시기
    private List<LocalTime> times; // 일정 시간들
}


