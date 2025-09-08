package site.petful.snsservice.instagram.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "instagram_token")
@RequiredArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Setter
@Getter
public class InstagramTokenEntity {

    @Id
    private Long userNo;
    @Column(nullable = false, length = 512)
    private String token;
    @Column(nullable = false)
    private LocalDateTime expireAt;
    @CreatedDate
    private LocalDateTime createdAt;


    public InstagramTokenEntity(Long userNo, String token, Long expiresIn) {
        this.userNo = userNo;
        this.token = token;
        if (expiresIn == null) {
            expiresIn = 60 * 24 * 60 * 60L;
        }
        this.expireAt = LocalDateTime.now().plusSeconds(expiresIn);
    }

}
