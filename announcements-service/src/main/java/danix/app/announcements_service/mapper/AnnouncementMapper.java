package danix.app.announcements_service.mapper;

import danix.app.announcements_service.dto.CreateAnnouncementDTO;
import danix.app.announcements_service.dto.DataDTO;
import danix.app.announcements_service.dto.ResponseAnnouncementDTO;
import danix.app.announcements_service.dto.ShowAnnouncementDTO;
import danix.app.announcements_service.models.Announcement;
import danix.app.announcements_service.repositories.IdProjection;
import danix.app.announcements_service.services.AnnouncementsService;
import danix.app.announcements_service.util.CurrencyCode;
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
    public abstract ResponseAnnouncementDTO toResponseDTO(Announcement announcement, CurrencyCode currency);

    @Mapping(target = "imagesIds", source = "announcement", qualifiedByName = "images")
    @Mapping(target = "price", expression = PRICE_CONVERT_EXPRESSION)
    public abstract ShowAnnouncementDTO toShowDTO(Announcement announcement, CurrencyCode currency);

    public abstract Announcement fromCreateDTO(CreateAnnouncementDTO announcement);

    public List<ResponseAnnouncementDTO> toResponseDTOList(List<Announcement> announcements, CurrencyCode currency) {
        return announcements.stream()
                .map(announcement -> toResponseDTO(announcement, currency))
                .toList();
    }

    public List<Long> toIdsListFromProjectionsList(List<IdProjection> projections) {
        return projections.stream()
                .map(IdProjection::getId)
                .toList();
    }

    @Named("images")
    protected List<DataDTO<Long>> images(Announcement announcement) {
        return announcement.getImages().stream()
                .map(image -> new DataDTO<>(image.getId()))
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
