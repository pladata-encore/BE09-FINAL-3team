// ========================================
// 예방접종 관련 상수
// ========================================

// 예방접종 일정용 옵션들 (백엔드 스펙에 맞게 수정)
export const vaccinationSubTypeOptions = [
  "VACCINE", // 접종
  "CHECKUP", // 건강검진
];

export const vaccinationFrequencyOptions = ["매일", "매주", "매월", "당일"];

export const vaccinationFrequencyMapping = {
  매일: "DAILY",
  매주: "WEEKLY",
  매월: "MONTHLY",
  당일: "SINGLE_DAY",

  // 영어 → 한글
  DAILY: "매일",
  WEEKLY: "매주",
  MONTHLY: "매월",
  SINGLE_DAY: "당일",
};
