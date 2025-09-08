package site.petful.snsservice.instagram.client.dto;

import java.util.ArrayList;
import lombok.AllArgsConstructor;
import lombok.Getter;
import site.petful.snsservice.instagram.media.dto.InstagramMediaDto;

@Getter
@AllArgsConstructor
public class InstagramApiMediaResponseDto {

    private final ArrayList<InstagramMediaDto> data;
    private final Paging paging;

    @Getter
    public static class Cursors {

        public String before;
        public String after;
    }


    @Getter
    public static class Paging {

        public Cursors cursors;
    }
}




