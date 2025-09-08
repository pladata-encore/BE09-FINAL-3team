package site.petful.petservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class History {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_no")
    private Long historyNo;

    @Column(name = "history_start")
    private LocalDate historyStart;

    @Column(name = "history_end")
    private LocalDate historyEnd;

    @Column(name = "title")
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_urls", columnDefinition = "TEXT")
    private String imageUrls;

    @Column(name = "pet_no")
    private Long petNo;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Pet과의 관계 (N:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_no", insertable = false, updatable = false, 
                foreignKey = @ForeignKey(name = "FK_history_pet_no", 
                foreignKeyDefinition = "FOREIGN KEY (pet_no) REFERENCES pet (pet_no) ON DELETE CASCADE"))
    private Pet pet;

    // HistoryImageFile과의 관계 (1:N)
    @OneToMany(mappedBy = "history", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<HistoryImageFile> imageFiles;
}
