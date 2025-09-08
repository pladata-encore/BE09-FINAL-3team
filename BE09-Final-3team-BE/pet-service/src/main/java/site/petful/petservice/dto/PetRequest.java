package site.petful.petservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetRequest {

    private String name;
    private String type;
    private Long age;
    private String gender;
    private Float weight;
    private String imageUrl;
    private Long snsId;
    private Long snsProfileNo;
    private String snsUsername;
}
