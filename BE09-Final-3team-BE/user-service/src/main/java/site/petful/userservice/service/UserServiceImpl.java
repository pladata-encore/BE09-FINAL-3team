package site.petful.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import site.petful.userservice.common.ErrorCode;
import site.petful.userservice.entity.Role;
import site.petful.userservice.entity.User;
import site.petful.userservice.entity.UserProfile;
import site.petful.userservice.dto.PasswordChangeRequest;
import site.petful.userservice.dto.PasswordResetRequest;
import site.petful.userservice.dto.PasswordResetResponse;
import site.petful.userservice.dto.VerificationConfirmRequest;
import site.petful.userservice.dto.VerificationConfirmResponse;
import site.petful.userservice.dto.FileUploadResponse;
import site.petful.userservice.dto.ProfileResponse;
import site.petful.userservice.dto.ProfileUpdateRequest;
import site.petful.userservice.dto.SimpleProfileResponse;
import site.petful.userservice.dto.SignupRequest;
import site.petful.userservice.dto.SignupResponse;
import site.petful.userservice.dto.WithdrawRequest;
import site.petful.userservice.dto.WithdrawResponse;
import site.petful.userservice.dto.ReportRequest;
import site.petful.userservice.repository.UserProfileRepository;
import site.petful.userservice.repository.UserRepository;
import site.petful.userservice.common.ftp.FtpService;
import site.petful.userservice.admin.entity.ReportLog;
import site.petful.userservice.admin.entity.ActorRef;
import site.petful.userservice.admin.entity.ActorType;
import site.petful.userservice.admin.entity.ReportStatus;
import site.petful.userservice.admin.repository.ReportLogRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final RedisService redisService;
    private final FtpService ftpService;
    private final ReportLogRepository reportLogRepository;

    @Override
    public SignupResponse signup(SignupRequest request) {
        // 입력 검증
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException(ErrorCode.INVALID_REQUEST.getDefaultMessage() + " - 이메일은 필수 입력 항목입니다.");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException(ErrorCode.INVALID_REQUEST.getDefaultMessage() + " - 비밀번호는 필수 입력 항목입니다.");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException(ErrorCode.INVALID_REQUEST.getDefaultMessage() + " - 이름은 필수 입력 항목입니다.");
        }
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            throw new IllegalArgumentException(ErrorCode.INVALID_REQUEST.getDefaultMessage() + " - 전화번호는 필수 입력 항목입니다.");
        }

        // 이메일 중복
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException(ErrorCode.DUPLICATE_EMAIL.getDefaultMessage());
        }

        // 사용자 저장
        User saved = userRepository.save(User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .nickname(request.getNickname())
                .phone(request.getPhone())
                .userType(request.getUserType() != null ? request.getUserType() : Role.USER) // 기본값 설정
                .birthDate(request.getBirthDate())
                .description(request.getDescription())
                .roadAddress(request.getRoadAddress())
                .detailAddress(request.getDetailAddress())
                .birthYear(request.getBirthYear())
                .birthMonth(request.getBirthMonth())
                .birthDay(request.getBirthDay())
                .emailVerified(false)
                .isActive(true)
                .build());

        return SignupResponse.builder()
                .userNo(saved.getUserNo())
                .email(saved.getEmail())
                .name(saved.getName())
                .message("회원가입이 성공적으로 완료되었습니다. 로그인 후 이용하세요.")
                .build();
    }

    @Override
    public void markEmailVerified(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(ErrorCode.USER_NOT_FOUND.getDefaultMessage()));
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(ErrorCode.USER_NOT_FOUND.getDefaultMessage()));
    }
    
    @Override
    public User findByUserNo(Long userNo) {
        return userRepository.findById(userNo)
                .orElseThrow(() -> new RuntimeException(ErrorCode.USER_NOT_FOUND.getDefaultMessage()));
    }
    
    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userNo) {
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new RuntimeException(ErrorCode.USER_NOT_FOUND.getDefaultMessage()));
        
        UserProfile profile = userProfileRepository.findByUser_UserNo(userNo)
                .orElse(null);
        
        // 프로필이 없으면 기본 사용자 정보에서 생년월일 가져오기
        LocalDate birthDate = null;
        if (profile != null && profile.getBirthDate() != null) {
            birthDate = profile.getBirthDate();
        } else if (user.getBirthDate() != null) {
            birthDate = user.getBirthDate();
        }
        
        return ProfileResponse.builder()
                .userNo(user.getUserNo())
                .email(user.getEmail())
                .name(user.getName())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .role(user.getUserType().name())
                .profileImageUrl(profile != null ? profile.getProfileImageUrl() : null)
                .selfIntroduction(profile != null ? profile.getSelfIntroduction() : null)
                .birthDate(birthDate)
                // 프로필에 주소가 있으면 프로필 주소, 없으면 회원가입 시 입력한 기본 주소 사용
                .roadAddress(profile != null && profile.getRoadAddress() != null ? 
                           profile.getRoadAddress() : user.getRoadAddress())
                .detailAddress(profile != null && profile.getDetailAddress() != null ? 
                             profile.getDetailAddress() : user.getDetailAddress())
                .instagramAccount(profile != null ? profile.getInstagramAccount() : null)
                .build();
    }
    
    @Override
    @Transactional
    public ProfileResponse updateProfile(Long userNo, ProfileUpdateRequest request) {
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new RuntimeException(ErrorCode.USER_NOT_FOUND.getDefaultMessage()));
        
        UserProfile profile = userProfileRepository.findByUser_UserNo(userNo)
                .orElse(UserProfile.builder()
                        .user(user)
                        .build());
        
        // 프로필 정보 업데이트
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
            userRepository.save(user);
        }
        if (request.getProfileImageUrl() != null) {
            profile.setProfileImageUrl(request.getProfileImageUrl());
        }
        if (request.getSelfIntroduction() != null) {
            profile.setSelfIntroduction(request.getSelfIntroduction());
        }
        if (request.getBirthDate() != null) {
            profile.setBirthDate(request.getBirthDate());
        } else if (profile.getBirthDate() == null && user.getBirthDate() != null) {
            // 프로필에 생년월일이 없고 사용자 기본 정보에 있으면 가져오기
            profile.setBirthDate(user.getBirthDate());
        }
        if (request.getRoadAddress() != null) {
            profile.setRoadAddress(request.getRoadAddress());
        }
        if (request.getDetailAddress() != null) {
            profile.setDetailAddress(request.getDetailAddress());
        }
        if (request.getInstagramAccount() != null) {
            profile.setInstagramAccount(request.getInstagramAccount());
        }
        
        UserProfile savedProfile = userProfileRepository.save(profile);
        
        return ProfileResponse.builder()
                .userNo(user.getUserNo())
                .email(user.getEmail())
                .name(user.getName())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .role(user.getUserType().name())
                .profileImageUrl(savedProfile.getProfileImageUrl())
                .selfIntroduction(savedProfile.getSelfIntroduction())
                .birthDate(savedProfile.getBirthDate())
                .roadAddress(savedProfile.getRoadAddress())
                .detailAddress(savedProfile.getDetailAddress())
                .instagramAccount(savedProfile.getInstagramAccount())
                .profileUpdatedAt(savedProfile.getUpdatedAt())
                .build();
    }
    
    @Override
    @Transactional
    public PasswordResetResponse requestPasswordReset(PasswordResetRequest request) {
        // 사용자 존재 여부 확인
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException(ErrorCode.USER_NOT_FOUND.getDefaultMessage()));
        
        // 비밀번호 재설정용 인증 코드 생성 (6자리 숫자)
        String verificationCode = generateVerificationCode();
        
        // Redis 연결 테스트
        boolean redisConnected = redisService.testConnection();
        System.out.println("Redis 연결 상태: " + redisConnected);
        
        // Redis에 인증 코드 저장 (10분 유효)
        String redisKey = "password_reset:" + request.getEmail();
        redisService.setValue(redisKey, verificationCode, 600); // 10분 = 600초
        
        // 디버깅을 위한 로그 추가
        System.out.println("=== 비밀번호 재설정 디버깅 ===");
        System.out.println("이메일: " + request.getEmail());
        System.out.println("생성된 인증 코드: " + verificationCode);
        System.out.println("Redis 키: " + redisKey);
        
        // Redis에 저장된 값 확인
        String savedCode = redisService.getValue(redisKey);
        System.out.println("Redis에서 조회한 코드: " + savedCode);
        System.out.println("저장 성공 여부: " + (savedCode != null && savedCode.equals(verificationCode)));
        System.out.println("================================");
        
        // 이메일 발송
        String subject = "[Petful] 비밀번호 재설정 인증 코드";
        String content = String.format(
            "안녕하세요, %s님!\n\n" +
            "비밀번호 재설정을 요청하셨습니다.\n\n" +
            "인증 코드: %s\n\n" +
            "이 인증 코드는 10분간 유효합니다.\n" +
            "본인이 요청하지 않았다면 이 이메일을 무시하세요.\n\n" +
            "감사합니다.\n" +
            "Petful 팀",
            user.getName(),
            verificationCode
        );
        
        emailService.sendEmail(request.getEmail(), subject, content);
        
        return PasswordResetResponse.builder()
                .message("비밀번호 재설정 인증 코드가 이메일로 발송되었습니다.")
                .email(request.getEmail())
                .build();
    }
    
    @Override
    @Transactional(readOnly = true)
    public SimpleProfileResponse getSimpleProfile(Long userNo) {
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new RuntimeException(ErrorCode.USER_NOT_FOUND.getDefaultMessage()));
        
        UserProfile profile = userProfileRepository.findByUser_UserNo(userNo)
                .orElse(null);
        
        return SimpleProfileResponse.builder()
                .id(user.getUserNo())
                .nickname(user.getNickname())
                .profileImageUrl(profile != null ? profile.getProfileImageUrl() : null)
                .email(user.getEmail())
                .phone(user.getPhone())
                .build();
    }

    @Override
    public List<SimpleProfileResponse> getSimpleProfilesBatch(List<Long> userNos) {
        if (userNos == null || userNos.isEmpty()) {
            return new ArrayList<>();
        }

        List<SimpleProfileResponse> profiles = new ArrayList<>();

        for (Long userNo : userNos) {
            try {
                SimpleProfileResponse profile = getSimpleProfile(userNo);
                if (profile != null) {
                    profiles.add(profile);
                }
            } catch (Exception e) {
                // 개별 사용자 조회 실패 시 로그만 남기고 계속 진행
                log.warn("Failed to get profile for user {}: {}", userNo, e.getMessage());
            }
        }
        return profiles;
    }

    @Override
    @Transactional(readOnly = true)
    public VerificationConfirmResponse verifyPasswordResetCode(VerificationConfirmRequest request) {
        // 인증 코드 검증
        String redisKey = "password_reset:" + request.getEmail();
        String storedCode = redisService.getValue(redisKey);
        
        // 디버깅을 위한 로그 추가
        System.out.println("=== 인증 코드 확인 디버깅 ===");
        System.out.println("요청 이메일: " + request.getEmail());
        System.out.println("요청 인증 코드: " + request.getCode());
        System.out.println("Redis 키: " + redisKey);
        System.out.println("Redis에서 조회한 코드: " + storedCode);
        System.out.println("코드 일치 여부: " + (storedCode != null && storedCode.equals(request.getCode())));
        System.out.println("=====================================");
        
        if (storedCode == null) {
            return VerificationConfirmResponse.builder()
                    .message("인증 코드가 만료되었습니다.")
                    .email(request.getEmail())
                    .verified(false)
                    .build();
        }
        
        if (!storedCode.equals(request.getCode())) {
            return VerificationConfirmResponse.builder()
                    .message("유효하지 않은 인증 코드입니다.")
                    .email(request.getEmail())
                    .verified(false)
                    .build();
        }
        
        return VerificationConfirmResponse.builder()
                .message("인증이 성공적으로 완료되었습니다.")
                .email(request.getEmail())
                .verified(true)
                .build();
    }
    
    @Override
    @Transactional
    public void changePassword(PasswordChangeRequest request) {
        // 비밀번호 확인
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("새 비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }
        
        // 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException(ErrorCode.USER_NOT_FOUND.getDefaultMessage()));
        
        // 비밀번호 변경
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        // Redis에서 인증 코드 삭제 (선택사항 - 보안을 위해 삭제)
        String redisKey = "password_reset:" + request.getEmail();
        redisService.deleteValue(redisKey);
    }
    
    @Override
    @Transactional
    public WithdrawResponse withdraw(Long userNo, WithdrawRequest request) {
        // 사용자 조회
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new RuntimeException(ErrorCode.USER_NOT_FOUND.getDefaultMessage()));
        
        // 비밀번호 확인
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        
        // 이미 탈퇴한 사용자인지 확인
        if (!user.getIsActive()) {
            throw new IllegalStateException("이미 탈퇴한 계정입니다.");
        }
        
        // 탈퇴 정보 저장 (로그 목적)
        String withdrawInfo = "탈퇴 사유: " + request.getReason() + " (탈퇴일: " + LocalDateTime.now() + ")";
        log.info("회원탈퇴 - 사용자: {}, 사유: {}", user.getEmail(), request.getReason());
        
        // 관련 데이터 정리
        // - Redis에서 사용자 관련 데이터 삭제
        String redisKey = "user:" + user.getEmail();
        redisService.deleteValue(redisKey);
        
        // - 프로필 이미지가 있다면 삭제 (선택사항)
        if (user .getImageNo() != null) {
            // FTP 서버에서 이미지 삭제 로직 추가 가능
            log.info("프로필 이미지 삭제 - imageNo: {}", user.getImageNo());
        }
        
        // UserProfile도 함께 삭제 (CASCADE 설정이 있다면 자동 삭제)
        Optional<UserProfile> userProfileOpt = userProfileRepository.findByUser_UserNo(userNo);
        if (userProfileOpt.isPresent()) {
            userProfileRepository.delete(userProfileOpt.get());
        }
        
        // 실제 DB에서 사용자 데이터 삭제 (Hard Delete)
        userRepository.delete(user);
        
        return WithdrawResponse.builder()
                .userNo(user.getUserNo())
                .email(user.getEmail())
                .message("회원탈퇴가 성공적으로 처리되었습니다.")
                .withdrawnAt(LocalDateTime.now())
                .build();
    }
    
    @Override
    @Transactional
    public FileUploadResponse uploadProfileImage(MultipartFile file, Long userNo) {
        try {
            // userNo null 체크
            if (userNo == null) {
                return FileUploadResponse.builder()
                        .success(false)
                        .message("사용자 번호가 유효하지 않습니다.")
                        .build();
            }
            
            // 파일 검증
            if (file.isEmpty()) {
                return FileUploadResponse.builder()
                        .success(false)
                        .message("업로드할 파일이 없습니다.")
                        .build();
            }
            
            // 파일 크기 검증 (10MB 제한)
            if (file.getSize() > 10 * 1024 * 1024) {
                return FileUploadResponse.builder()
                        .success(false)
                        .message("파일 크기는 10MB를 초과할 수 없습니다.")
                        .build();
            }
            
            // 파일 타입 검증
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return FileUploadResponse.builder()
                        .success(false)
                        .message("이미지 파일만 업로드 가능합니다.")
                        .build();
            }
            
            String uploadedFilename = ftpService.upload(file);
            
            if (uploadedFilename != null) {
                // FtpService에서 반환된 파일명 사용
                String fileUrl = ftpService.getFileUrl(uploadedFilename);
                
                // 사용자의 프로필 이미지 URL 업데이트
                updateUserProfileImageUrl(userNo, fileUrl);
                
                // 업데이트된 프로필 정보 조회
                ProfileResponse updatedProfile = getProfile(userNo);
                
                return FileUploadResponse.builder()
                        .fileName(uploadedFilename)
                        .fileUrl(fileUrl)
                        .message("프로필 이미지 업로드가 성공했습니다.")
                        .success(true)
                        .profileResponse(updatedProfile)  // 업데이트된 프로필 정보 포함
                        .build();
            } else {
                return FileUploadResponse.builder()
                        .success(false)
                        .message("파일 업로드에 실패했습니다.")
                        .build();
            }
            
        } catch (Exception e) {
            return FileUploadResponse.builder()
                    .success(false) 
                    .message("파일 업로드 중 오류가 발생했습니다: " + e.getMessage())
                    .build();
        }
    }
    
    /**
     * 6자리 숫자 인증 코드 생성
     */
    private String generateVerificationCode() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }
    
    /**
     * 사용자의 프로필 이미지 URL 업데이트
     */
    private void updateUserProfileImageUrl(Long userNo, String imageUrl) {
        try {
            // userNo null 체크
            if (userNo == null) {
                throw new IllegalArgumentException("사용자 번호가 null입니다.");
            }
            
            // imageUrl null 체크
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                throw new IllegalArgumentException("이미지 URL이 유효하지 않습니다.");
            }
            
            // 사용자 조회
            User user = userRepository.findById(userNo)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userNo));
            
            // UserProfile 조회 또는 생성
            UserProfile profile = userProfileRepository.findByUser_UserNo(userNo)
                    .orElse(UserProfile.builder()
                            .user(user)
                            .build());
            
            // 프로필 이미지 URL 업데이트
            profile.setProfileImageUrl(imageUrl);
            
            // 저장
            userProfileRepository.save(profile);
            
        } catch (Exception e) {
            throw new RuntimeException("프로필 이미지 URL 업데이트 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public void reportUser(Long reporterUserNo, String reporterName, ReportRequest request) {
        try {
            log.info("신고 요청 시작 - 신고자: {}, 대상: {}, 사유: {}", 
                    reporterName, request.getTargetName(), request.getReason());
            
            // 신고자 정보 조회
            User reporter = userRepository.findById(reporterUserNo)
                    .orElseThrow(() -> new RuntimeException("신고자를 찾을 수 없습니다: " + reporterUserNo));
            
            log.info("신고자 조회 성공: {}", reporter.getEmail());
            
            // 신고 대상 사용자 조회 (targetName으로 검색)
            User targetUser = userRepository.findByNickname(request.getTargetName())
                    .orElse(userRepository.findByName(request.getTargetName())
                            .orElse(null));
            
            if (targetUser == null) {
                log.error("신고 대상을 찾을 수 없습니다: {}", request.getTargetName());
                throw new RuntimeException("신고 대상을 찾을 수 없습니다: " + request.getTargetName());
            }
            
            log.info("신고 대상 조회 성공: {}", targetUser.getEmail());
            
            // 자기 자신을 신고하는 경우 방지
            if (reporterUserNo.equals(targetUser.getUserNo())) {
                throw new RuntimeException("자기 자신을 신고할 수 없습니다.");
            }
            
            // ReportLog 생성
            ReportLog reportLog = new ReportLog();
            reportLog.setReason(request.getReason());
            reportLog.setReporter(new ActorRef(ActorType.USER, reporterUserNo));
            reportLog.setTarget(new ActorRef(ActorType.USER, targetUser.getUserNo()));
            reportLog.setReportStatus(ReportStatus.BEFORE);
            reportLog.setCreatedAt(LocalDateTime.now());
            
            log.info("ReportLog 객체 생성 완료");
            
            // 저장
            reportLogRepository.save(reportLog);
            
            log.info("신고가 접수되었습니다. 신고자: {}, 대상: {}, 사유: {}", 
                    reporterName, request.getTargetName(), request.getReason());
            
        } catch (Exception e) {
            log.error("신고 처리 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("신고 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void reportUserByAdvertiser(Long advertiserNo, ReportRequest request) {
        try {
            log.info("신고 요청 시작 - 신고 대상: {}, 사유: {}",
                    request.getTargetName(), request.getReason());

            // 신고 대상 사용자 조회 (targetName으로 검색)
            User targetUser = userRepository.findByNickname(request.getTargetName())
                    .orElse(userRepository.findByName(request.getTargetName())
                            .orElse(null));

            if (targetUser == null) {
                log.error("신고 대상을 찾을 수 없습니다: {}", request.getTargetName());
                throw new RuntimeException("신고 대상을 찾을 수 없습니다: " + request.getTargetName());
            }

            log.info("신고 대상 조회 성공: {}", targetUser.getEmail());

            // ReportLog 생성
            ReportLog reportLog = new ReportLog();
            reportLog.setReason(request.getReason());
            reportLog.setReporter(new ActorRef(ActorType.ADVERTISER, advertiserNo));
            reportLog.setTarget(new ActorRef(ActorType.USER, targetUser.getUserNo()));
            reportLog.setReportStatus(ReportStatus.BEFORE);
            reportLog.setCreatedAt(LocalDateTime.now());

            log.info("ReportLog 객체 생성 완료");

            // 저장
            reportLogRepository.save(reportLog);

            log.info("신고가 접수되었습니다. 신고 대상: {}, 사유: {}",
                    request.getTargetName(), request.getReason());

        } catch (Exception e) {
            log.error("신고 처리 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("신고 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}
