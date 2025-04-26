package danix.app.announcements_service.mapper;

import danix.app.announcements_service.dto.CreateDTO;
import danix.app.announcements_service.dto.ResponseDTO;
import danix.app.announcements_service.dto.ShowDTO;
import danix.app.announcements_service.models.Announcement;
import danix.app.announcements_service.models.Image;
import danix.app.announcements_service.services.AnnouncementsService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public abstract class AnnouncementMapper {

    @Autowired
    @Lazy
    protected AnnouncementsService announcementsService;

    protected final String PRICE_CONVERT_EXPRESSION =
            "java(announcementsService.convertPrice(currency, course -> announcement.getPrice() * course))";

    @Mapping(target = "imageId", source = "announcement", qualifiedByName = "imageId")
    @Mapping(target = "price", expression = PRICE_CONVERT_EXPRESSION)
    public abstract ResponseDTO toResponseDTO(Announcement announcement, String currency);

    @Mapping(target = "imagesIds", source = "announcement", qualifiedByName = "images")
    @Mapping(target = "price", expression = PRICE_CONVERT_EXPRESSION)
    public abstract ShowDTO toShowDTO(Announcement announcement, String currency);

    public abstract Announcement fromCreateDTO(CreateDTO announcement);

    public List<ResponseDTO> toResponseDTOList(List<Announcement> announcements, String currency) {
        return announcements.stream()
                .map(announcement -> toResponseDTO(announcement, currency))
                .toList();
    }

    @Named("images")
    protected List<Long> images(Announcement announcement) {
        return announcement.getImages().stream()
                .map(Image::getId)
                .toList();
    }

    @Named("imageId")
    protected Long imageId(Announcement announcement) {
        if (!announcement.getImages().isEmpty()) {
            return announcement.getImages().getFirst().getId();
        }
        return null;
    }
}
