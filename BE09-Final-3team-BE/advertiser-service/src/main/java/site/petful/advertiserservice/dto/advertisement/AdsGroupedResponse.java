package site.petful.advertiserservice.dto.advertisement;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import site.petful.advertiserservice.entity.advertisement.Advertisement;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdsGroupedResponse {

    private List<AdResponse> recruitingAds;
    private List<AdResponse> endedAds;

    public static AdsGroupedResponse from(List<Advertisement> recruiting, List<Advertisement> ended) {
        List<AdResponse> recruitingAds = recruiting.stream()
                .map(AdResponse::from)
                .collect(Collectors.toList());

        List<AdResponse> endedAds = ended.stream()
                .map(AdResponse::from)
                .collect(Collectors.toList());

        return new AdsGroupedResponse(recruitingAds, endedAds);
    }
}
