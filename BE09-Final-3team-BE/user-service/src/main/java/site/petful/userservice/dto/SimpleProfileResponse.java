package site.petful.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleProfileResponse {
    private Long id;
    private String nickname;
    private String profileImageUrl;
    private String email;  // emil -> email로 수정
    private String phone;
    
}
