package danix.app.announcements_service.services;

import danix.app.announcements_service.dto.*;
import danix.app.announcements_service.feign.CurrencyConverterAPI;
import danix.app.announcements_service.feign.FilesService;
import danix.app.announcements_service.feign.UsersService;
import danix.app.announcements_service.mapper.AnnouncementMapper;
import danix.app.announcements_service.mapper.ReportMapper;
import danix.app.announcements_service.models.*;
import danix.app.announcements_service.repositories.*;
import danix.app.announcements_service.security.UserDetailsImpl;
import danix.app.announcements_service.util.AnnouncementException;
import danix.app.announcements_service.util.CurrencyCode;
import danix.app.announcements_service.security.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnouncementsService {

	private final AnnouncementsRepository announcementsRepository;

	private final ImagesRepository imagesRepository;

	private final WatchesRepository watchesRepository;

	private final LikesRepository likesRepository;

	private final ReportsRepository reportsRepository;

	private final FilesService filesService;

	private final CurrencyConverterAPI converterAPI;

	private final UsersService usersService;

	private final KafkaTemplate<String, List<String>> listKafkaTemplate;

	private final KafkaTemplate<String, EmailMessageDTO> emailMessageKafkaTemplate;

	private final AnnouncementMapper announcementMapper;

	private final ReportMapper reportMapper;

	@Value("${currency_converter_api_key}")
	private String currencyLayerKey;

	@Value("${max_images_count}")
	private int maxImagesCount;

	@Value("${max_storage_days}")
	private int storageDays;

	@Value("${access_key}")
	private String accessKey;

	public List<ResponseAnnouncementDTO> findAll(int page, int count, String currency, List<String> filters, SortDTO sortDTO) {
		User user = getCurrentUser();
		PageRequest pageRequest = getPageRequest(page, count, sortDTO);
		if (filters != null) {
			List<Long> ids = announcementMapper.toIdsListFromProjectionsList(announcementsRepository
					.findAllByCountryAndCityAndTypeIn(user.getCountry(), user.getCity(), pageRequest, filters));
			return announcementMapper.toResponseDTOList(announcementsRepository
					.findAllByIdIn(ids, pageRequest.getSort()), currency);
		}
		List<Long> ids = announcementMapper.toIdsListFromProjectionsList(announcementsRepository
				.findAllByCountryAndCity(user.getCountry(), user.getCity(), pageRequest));
		return announcementMapper.toResponseDTOList(announcementsRepository
				.findAllByIdIn(ids, pageRequest.getSort()), currency);
	}

	public List<ResponseAnnouncementDTO> findByTitle(int page, int count, String title, String currency, List<String> filters,
			SortDTO sortDTO) {
		User user = getCurrentUser();
		PageRequest pageRequest = getPageRequest(page, count, sortDTO);
		if (filters != null) {
			List<Long> ids = announcementMapper.toIdsListFromProjectionsList(announcementsRepository
					.findAllByTitleContainsIgnoreCaseAndCountryAndCityAndTypeIn(title, user.getCountry(), user.getCity(),
							filters, pageRequest));
			return announcementMapper.toResponseDTOList(announcementsRepository
					.findAllByIdIn(ids, pageRequest.getSort()), currency);
		}
		List<Long> ids = announcementMapper.toIdsListFromProjectionsList(announcementsRepository
				.findAllByTitleContainsIgnoreCaseAndCountryAndCity(title, user.getCountry(), user.getCity(), pageRequest));
		return announcementMapper
				.toResponseDTOList(announcementsRepository.findAllByIdIn(ids, pageRequest.getSort()), currency);
	}

	private PageRequest getPageRequest(int page, int count, SortDTO sort) {
		boolean isNull = sort == null;
		String property = isNull ? "id" : switch (sort.getType()) {
			case LIKES -> "likesCount";
			case WATCHES -> "watchesCount";
			default -> sort.getType().toString().toLowerCase();
		};
		Sort.Direction direction = isNull ? Sort.Direction.DESC : sort.getDirection();
		return PageRequest.of(page, count, Sort.by(direction, property));
	}

	public List<ResponseAnnouncementDTO> findAllByUser(Long id, String currency, int page, int count) {
		List<Long> ids = announcementMapper.toIdsListFromProjectionsList(announcementsRepository
				.findAllByOwnerId(id, PageRequest.of(page, count, getIdSort())));
		return announcementMapper.toResponseDTOList(announcementsRepository.findAllByIdIn(ids, getIdSort()), currency);
	}

	public Announcement findById(Long id) {
		return announcementsRepository.findById(id)
			.orElseThrow(() -> new AnnouncementException("Announcement not found"));
	}

	@Transactional
	public DataDTO<Long> save(CreateAnnouncementDTO createDTO, String currency) {
		Announcement announcement = announcementMapper.fromCreateDTO(createDTO);
		announcement.setCreatedAt(LocalDateTime.now());
		announcement.setOwnerId(getCurrentUser().getId());
		announcement.setPrice(convertPrice(currency, course -> announcement.getPrice() / course));
		announcementsRepository.save(announcement);
		return new DataDTO<>(announcement.getId());
	}

	public byte[] downloadImage(Long id) {
		Image image = imagesRepository.findById(id)
				.orElseThrow(() -> new AnnouncementException("Image not found"));
		return filesService.downloadImage(image.getFileName(), accessKey);
	}

	@Transactional
	public void addImage(MultipartFile image, Long id) {
		Announcement announcement = findById(id);
		checkAnnouncementOwner(announcement);
		if (announcement.getImages().size() >= maxImagesCount) {
			throw new AnnouncementException("Images limit acceded");
		}
		String uuid = UUID.randomUUID() + ".jpg";
		filesService.saveImage(image, uuid, accessKey);
		imagesRepository.save(new Image(uuid, announcement));
	}

	@Transactional
	public void deleteImage(Long id) {
		Image image = imagesRepository.findById(id)
				.orElseThrow(() -> new AnnouncementException("Image not found"));
		checkAnnouncementOwner(image.getAnnouncement());
		filesService.deleteImage(image.getFileName(), accessKey);
		imagesRepository.delete(image);
	}

	@Transactional
	public void like(Long id) {
		Announcement announcement = findById(id);
		Long userId = getCurrentUser().getId();
		likesRepository.findByAnnouncementAndUserId(announcement, userId).ifPresent(like -> {
			throw new AnnouncementException("Like already exists");
		});
		likesRepository.save(new Like(announcement, userId));
		announcement.setLikesCount(announcement.getLikesCount() + 1);
	}

	@Transactional
	public void deleteLike(Long id) {
		Announcement announcement = findById(id);
		Long userId = getCurrentUser().getId();
		likesRepository.findByAnnouncementAndUserId(announcement, userId)
			.ifPresentOrElse(likesRepository::delete, () -> {
				throw new AnnouncementException("Like not found");
			});
		announcement.setLikesCount(announcement.getLikesCount() - 1);
	}

	@Transactional
	public ShowAnnouncementDTO show(Long id, String currency) {
		Announcement announcement = findById(id);
		Long userId = getCurrentUser().getId();
		watchesRepository.findByAnnouncementAndUserId(announcement, userId).orElseGet(() -> {
			announcement.setWatchesCount(announcement.getWatchesCount() + 1);
			return watchesRepository.save(new Watch(announcement, userId));
		});
		ShowAnnouncementDTO showDTO = announcementMapper.toShowDTO(announcement, currency);
		showDTO.setExpiredDate(announcement.getCreatedAt().plusDays(storageDays));
		showDTO.setLiked(likesRepository.findByAnnouncementAndUserId(announcement, userId).isPresent());
		return showDTO;
	}

	@Transactional
	public void report(Long id, String cause) {
		Announcement announcement = findById(id);
		Long userId = getCurrentUser().getId();
		reportsRepository.findByAnnouncementAndSenderId(announcement, userId).ifPresent(report -> {
			throw new AnnouncementException("You already send report to this announcement");
		});
		reportsRepository.save(Report.builder()
			.announcement(announcement)
			.senderId(userId)
			.cause(cause)
			.build());
	}

	@Transactional
	public void closeReport(Long id) {
		reportsRepository.findById(id).ifPresentOrElse(reportsRepository::delete, () -> {
			throw new AnnouncementException("Report not found");
		});
	}

	public List<ResponseReportDTO> getReports(int page, int count, Sort.Direction direction) {
		return reportMapper.toResponseDTOList(reportsRepository
				.findAll(PageRequest.of(page, count, Sort.by(direction, "id"))).getContent());
	}

	public ShowReportDTO getReport(long id, String currency) {
		Report report = reportsRepository.findById(id).orElseThrow(() -> new AnnouncementException("Report not found"));
		ShowReportDTO showDTO = reportMapper.toShowDTO(report);
		showDTO.setAnnouncement(announcementMapper.toResponseDTO(report.getAnnouncement(), currency));
		return showDTO;
	}

	@Transactional
	public void delete(Long id) {
		Announcement announcement = findById(id);
		checkAnnouncementOwner(announcement);
		deleteImages(announcement);
		announcementsRepository.deleteById(announcement.getId());
	}

	@Transactional
	public void ban(Long id, String cause) {
		Announcement announcement = findById(id);
		deleteImages(announcement);
		String email = getUserEmail(announcement.getOwnerId());
		String message = String.format("Your announcement with title '%s' has been banned due to: %s",
				announcement.getTitle(), cause);
		emailMessageKafkaTemplate.send("message", new EmailMessageDTO(email, message));
		announcementsRepository.deleteById(announcement.getId());
	}

	@Transactional
	public void update(Long id, UpdateDTO updateDTO) {
		Announcement announcement = findById(id);
		checkAnnouncementOwner(announcement);
		if (updateDTO.getTitle() != null) {
			announcement.setTitle(updateDTO.getTitle());
		}
		if (updateDTO.getDescription() != null) {
			announcement.setDescription(updateDTO.getDescription());
		}
		if (updateDTO.getPrice() != null) {
			if (updateDTO.getCurrency() != null) {
				announcement
					.setPrice(convertPrice(updateDTO.getCurrency(), course -> updateDTO.getPrice() / course));
			}
			else {
				announcement.setPrice(updateDTO.getPrice());
			}
		}
		if (updateDTO.getCity() != null) {
			announcement.setCity(updateDTO.getCity());
		}
		if (updateDTO.getCountry() != null) {
			announcement.setCountry(updateDTO.getCountry());
		}
		if (updateDTO.getPhoneNumber() != null) {
			announcement.setPhoneNumber(updateDTO.getPhoneNumber());
		}
		if (updateDTO.getType() != null) {
			announcement.setType(updateDTO.getType());
		}
	}

	public double convertPrice(String currency, Function<Double, Double> operation) {
		CurrencyCode currencyCode;
		try {
			currencyCode = CurrencyCode.valueOf(currency.toUpperCase());
		}
		catch (IllegalArgumentException e) {
			throw new AnnouncementException("Invalid currency");
		}
		if (CurrencyCode.USD != currencyCode) {
			Map<String, Object> response = null;
			try {
				response = converterAPI.getCourse(currencyLayerKey, currencyCode.toString(), "USD", 1);
				Map<String, Object> quotes = (Map<String, Object>) response.get("quotes");
				double course = (double) quotes.get("USD" + currencyCode);
				BigDecimal bigDecimal = BigDecimal.valueOf(operation.apply(course)).setScale(2, RoundingMode.HALF_UP);
				return bigDecimal.doubleValue();
			}
			catch (Exception e) {
                assert response != null;
                Map<String, Object> error = (Map<String, Object>) response.get("error");
				String message = (String) error.get("info");
				log.error("Error convert price: {}", message);
				throw e;
			}
		}
		return operation.apply(1.0);
	}

	@Transactional
	@Async("virtualExecutor")
	public void deleteExpiredAnnouncements() {
		int page = 0;
		while (true) {
			List<Long> ids = announcementMapper.toIdsListFromProjectionsList(announcementsRepository
					.findAllByCreatedAtBefore(LocalDateTime.now().minusDays(storageDays),
							PageRequest.of(page, 50)));
			if (ids.isEmpty()) {
				break;
			}
			List<Announcement> announcements = announcementsRepository.findAllByIdIn(ids, getIdSort());
			deleteImages(announcements);
			announcements.forEach(announcement -> {
				String email = getUserEmail(announcement.getOwnerId());
				String message = "Your announcement has been removed due to expiration";
				emailMessageKafkaTemplate.send("message", new EmailMessageDTO(email, message));
			});
			announcementsRepository.deleteAllByIdIn(ids);
			page++;
		}
	}

	@Transactional
	@KafkaListener(topics = "deleted_user", containerFactory = "containerFactory")
	public void deletedUserListener(Long id) {
		int page = 0;
		 while (true) {
			List<Long> ids = announcementMapper.toIdsListFromProjectionsList(announcementsRepository
					.findAllByOwnerId(id, PageRequest.of(page, 50, getIdSort())));
			if (ids.isEmpty()) {
				break;
			}
			List<Announcement> announcements = announcementsRepository.findAllByIdIn(ids, getIdSort());
			deleteImages(announcements);
			announcementsRepository.deleteAllByIdIn(ids);
			page++;
		}
	}

	private void deleteImages(Announcement announcement) {
		if (!announcement.getImages().isEmpty()) {
			List<String> images = announcement.getImages().stream()
					.map(Image::getFileName)
					.toList();
			listKafkaTemplate.send("deleted_announcements_images", images);
		}
	}

	private void deleteImages(List<Announcement> announcements) {
		List<String> images = announcements.stream()
				.flatMap(announcement -> announcement.getImages().stream().map(Image::getFileName))
				.toList();
		if (!images.isEmpty()) {
			listKafkaTemplate.send("deleted_announcements_images", images);
		}
	}

	private String getUserEmail(Long id) {
		return (String) usersService.getUserEmail(id, accessKey).get("data");
	}

	private Sort getIdSort() {
		return Sort.by(Sort.Direction.DESC, "id");
	}

	private void checkAnnouncementOwner(Announcement announcement) {
		if (!announcement.getOwnerId().equals(getCurrentUser().getId())) {
			throw new AnnouncementException("You are not owner of this announcement");
		}
	}

	private User getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		return userDetails.authentication();
	}

}