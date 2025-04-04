package danix.app.announcements_service.services;

import danix.app.announcements_service.dto.*;
import danix.app.announcements_service.feignClients.CurrencyConverterAPI;
import danix.app.announcements_service.feignClients.FilesService;
import danix.app.announcements_service.models.*;
import danix.app.announcements_service.repositories.*;
import danix.app.announcements_service.security.UserDetailsImpl;
import danix.app.announcements_service.util.AnnouncementException;
import danix.app.announcements_service.util.CurrencyCode;
import danix.app.announcements_service.security.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final ModelMapper mapper;
    private final AnnouncementsRepository announcementsRepository;
    private final AnnouncementsImagesRepository imagesRepository;
    private final WatchesRepository watchesRepository;
    private final LikesRepository likesRepository;
    private final ReportsRepository reportsRepository;
    private final FilesService filesService;
    private final CurrencyConverterAPI converterAPI;
    private final KafkaTemplate<String, List<String>> kafkaTemplate;
    @Value("${currency_converter_api_key}")
    private String currencyLayerKey;
    @Value("${max_images_count}")
    private int maxImagesCount;

    public List<ResponseDTO> findAll(int page, int count, String currency, List<String> filters) {
        User user = getCurrentUser();
        PageRequest pageRequest = PageRequest.of(page, count, Sort.by(Sort.Direction.DESC, "likesCount"));
        if (filters != null) {
            return announcementsRepository.findAllByCountryAndCityAndTypeIn(user.getCountry(), user.getCity(), pageRequest, filters).stream()
                    .map(announcement -> convertToResponseDTO(announcement, currency))
                    .toList();
        }
        return announcementsRepository.findAllByCountryAndCity(user.getCountry(), user.getCity(), pageRequest).stream()
                .map(announcement -> convertToResponseDTO(announcement, currency))
                .toList();
    }

    public List<ResponseDTO> findByTitle(int page, int count, String tile, String currency, List<String> filters) {
        PageRequest pageRequest = PageRequest.of(page, count, Sort.by(Sort.Direction.DESC, "likesCount"));
        User user = getCurrentUser();
        if (filters != null) {
            return announcementsRepository.findAllByTitleContainsIgnoreCaseAndCountryAndCityAndTypeIn(tile,
                            user.getCountry(), user.getCity(), filters, pageRequest).stream()
                    .map(announcement -> convertToResponseDTO(announcement, currency))
                    .toList();
        }
        return announcementsRepository.findAllByTitleContainsIgnoreCaseAndCountryAndCity(tile,
                        user.getCountry(), user.getCity(), pageRequest).stream()
                .map(announcement -> convertToResponseDTO(announcement, currency))
                .toList();
    }

    public List<ResponseDTO> findAllByUser(Long id, String currency) {
        return announcementsRepository.findAllByOwnerId(id).stream()
                .map(announcement -> convertToResponseDTO(announcement, currency))
                .toList();
    }

    public Announcement findById(Long id) {
        return announcementsRepository.findById(id)
                .orElseThrow(() -> new AnnouncementException("Announcement not found"));
    }

    @Transactional
    public void save(CreateDTO createDTO, String currency) {
        Announcement announcement = mapper.map(createDTO, Announcement.class);
        announcement.setCreatedAt(LocalDateTime.now());
        announcement.setOwnerId(getCurrentUser().getId());
        announcement.setPrice(convertPrice(currency, course -> announcement.getPrice() / course));
        announcementsRepository.save(announcement);
    }

    public Map<String, Object> downloadImage(Long id) {
        AnnouncementImage image = imagesRepository.findById(id)
                .orElseThrow(() -> new AnnouncementException("Image not found"));
        return filesService.downloadImage(image.getFileName());
    }

    @Transactional
    public void addImage(MultipartFile image, Long id) {
        Announcement announcement = findById(id);
        checkAnnouncementOwner(announcement);
        if (announcement.getImages().size() == maxImagesCount) {
            throw new AnnouncementException("Images limit acceded");
        }
        String uuid = UUID.randomUUID() + ".jpg";
        filesService.addImage(image, uuid);
        AnnouncementImage announcementImage = new AnnouncementImage();
        announcementImage.setAnnouncement(announcement);
        announcementImage.setFileName(uuid);
        imagesRepository.save(announcementImage);
    }

    @Transactional
    public void deleteImage(Long id) {
        AnnouncementImage image = imagesRepository.findById(id)
                .orElseThrow(() -> new AnnouncementException("Image not found"));
        checkAnnouncementOwner(image.getAnnouncement());
        filesService.deleteImage(image.getFileName());
        imagesRepository.delete(image);
    }

    @Transactional
    public void like(Long id) {
        Announcement announcement = findById(id);
        Long userId = getCurrentUser().getId();
        likesRepository.findByAnnouncementAndUserId(announcement, userId).ifPresent(like -> {
            throw new AnnouncementException("Like already exists");
        });
        likesRepository.save(new AnnouncementLike(announcement, userId));
        announcement.setLikesCount(announcement.getLikesCount() + 1);
    }

    @Transactional
    public void deleteLike(Long id) {
        Announcement announcement = findById(id);
        Long userId = getCurrentUser().getId();
        likesRepository.findByAnnouncementAndUserId(announcement, userId).ifPresentOrElse(likesRepository::delete, () -> {
            throw new AnnouncementException("Like not found");
        });
        announcement.setLikesCount(announcement.getLikesCount() - 1);
    }

    public ShowDTO show(Long id, String currency) {
        Announcement announcement = findById(id);
        ShowDTO showDTO = mapper.map(announcement, ShowDTO.class);
        showDTO.setImagesIds(announcement.getImages().stream()
                .map(AnnouncementImage::getId)
                .toList());
        Long userId = getCurrentUser().getId();
        watchesRepository.findByAnnouncementAndUserId(announcement, userId).or(() -> {
            watchesRepository.save(new AnnouncementWatch(announcement, userId));
            return Optional.empty();
        });
        showDTO.setPrice(convertPrice(currency, course -> announcement.getPrice() * course));
        return showDTO;
    }

    @Transactional
    public void report(Long id, String cause) {
        Announcement announcement = findById(id);
        Long userId = getCurrentUser().getId();
        reportsRepository.findByAnnouncementAndUserId(announcement, userId).ifPresentOrElse(report -> {
            throw new AnnouncementException("You already send report to this announcement");
        }, () -> reportsRepository.save(
                Report.builder()
                        .announcement(announcement)
                        .userId(userId)
                        .cause(cause)
                        .build()
        ));
    }

    @Transactional
    public void closeReport(Long id) {
        reportsRepository.findById(id).ifPresentOrElse(reportsRepository::delete, () -> {
            throw new AnnouncementException("Report not found");
        });
    }

    public List<ResponseReportDTO> getReports(int page, int count, String currency) {
        return reportsRepository.findAll(PageRequest.of(page, count)).stream()
                .map(report ->
                        ResponseReportDTO.builder()
                                .id(report.getId())
                                .announcement(convertToResponseDTO(report.getAnnouncement(), currency))
                                .cause(report.getCause())
                                .senderId(report.getUserId())
                                .build()
                ).toList();
    }

    @Transactional
    public void delete(Long id) {
        Announcement announcement = findById(id);
        checkAnnouncementOwner(announcement);
        List<String> images = announcement.getImages().stream()
                        .map(AnnouncementImage::getFileName)
                        .toList();
        kafkaTemplate.send("deleted_announcements_images", images);
        announcementsRepository.delete(announcement);
    }

    @Transactional
    public void deleteByAdmin(Long id) {
        Announcement announcement = findById(id);
        List<String> images = announcement.getImages().stream()
                .map(AnnouncementImage::getFileName)
                .toList();
        kafkaTemplate.send("deleted_announcements_images", images);
        announcementsRepository.delete(announcement);
    }

    @Transactional
    public void update(Long id, UpdateDTO updateDTO) {
        Announcement announcement = findById(id);
        checkAnnouncementOwner(announcement);
        if (updateDTO.getTitle() != null) {
            if (updateDTO.getTitle().isBlank()) {
                throw new AnnouncementException("Title must not be empty");
            }
            announcement.setTitle(updateDTO.getTitle());
        }
        if (updateDTO.getDescription() != null && !updateDTO.getDescription().isBlank()) {
            announcement.setDescription(updateDTO.getDescription());
        }
        if (updateDTO.getPrice() != null) {
            if (updateDTO.getCurrency() != null) {
                announcement.setPrice(convertPrice(updateDTO.getCurrency(), course -> announcement.getPrice() / course));
            } else {
                announcement.setPrice(updateDTO.getPrice());
            }
        }
        if (updateDTO.getCity() != null) {
            if (updateDTO.getCity().isBlank()) {
                throw new AnnouncementException("City must not be empty");
            }
            announcement.setCity(updateDTO.getCity());
        }
        if (updateDTO.getCountry() != null) {
            if (updateDTO.getCountry().isBlank()) {
                throw new AnnouncementException("Country must not be empty");
            }
            announcement.setCountry(updateDTO.getCountry());
        }
        if (updateDTO.getPhoneNumber() != null) {
            if (updateDTO.getPhoneNumber().isBlank()) {
                throw new AnnouncementException("Phone number must not be empty");
            }
            announcement.setPhoneNumber(updateDTO.getPhoneNumber());
        }
        if (updateDTO.getType() != null) {
            if (updateDTO.getType().isBlank()) {
                throw new AnnouncementException("Type must not be empty");
            }
            announcement.setType(updateDTO.getType());
        }
    }

    @KafkaListener(topics = "deleted_user", containerFactory = "userFactory", groupId = "groupId")
    @Transactional
    public void deletedUserListener(Long id) {
        List<Announcement> announcements = announcementsRepository.findAllByOwnerId(id);
        List<String> images = announcements.stream()
                .flatMap(announcement -> announcement.getImages().stream()
                        .map(AnnouncementImage::getFileName))
                .toList();
        kafkaTemplate.send("deleted_announcements_images", images);
        announcementsRepository.deleteAll(announcements);
    }

    private ResponseDTO convertToResponseDTO(Announcement announcement, String currency) {
        ResponseDTO responseDTO = mapper.map(announcement, ResponseDTO.class);
        if (!announcement.getImages().isEmpty()) {
            responseDTO.setImageId(announcement.getImages().get(0).getId());
        }
        responseDTO.setLikesCount(announcement.getLikesCount());
        responseDTO.setWatchesCount(announcement.getWatches().size());
        responseDTO.setPrice(convertPrice(currency, course -> announcement.getPrice() * course));
        return responseDTO;
    }

    private double convertPrice(String currency, Function<Double, Double> operation) {
        CurrencyCode currencyCode;
        try {
            currencyCode = CurrencyCode.valueOf(currency.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AnnouncementException("Invalid currency");
        }
        if (CurrencyCode.USD != currencyCode) {
            Map<String, Object> response = converterAPI.convertCurrency(currencyLayerKey, currencyCode.toString(),
                    "USD", 1);
            Map<String, Object> quotes = (Map<String, Object>) response.get("quotes");
            double course = (double) quotes.get("USD" + currencyCode);
            BigDecimal bigDecimal = BigDecimal.valueOf(operation.apply(course));
            bigDecimal = bigDecimal.setScale(2, RoundingMode.HALF_UP);
            return bigDecimal.doubleValue();
        }
        return operation.apply(1.0);
    }

    private void checkAnnouncementOwner(Announcement announcement) {
        if (!announcement.getOwnerId().equals(getCurrentUser().getId())) {
            throw new AnnouncementException("You are not owner of this announcement");
        }
    }

    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userDetails.authentication();
    }
}