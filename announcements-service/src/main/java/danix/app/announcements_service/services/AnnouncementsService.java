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

	public List<ResponseDTO> findAll(int page, int count, String currency, List<String> filters, SortDTO sortDTO) {
		User user = getCurrentUser();
		PageRequest pageRequest = getPageRequest(page, count, sortDTO);
		if (filters != null) {
			return announcementMapper.toResponseDTOList(announcementsRepository
					.findAllByCountryAndCityAndTypeIn(user.getCountry(), user.getCity(), pageRequest, filters), currency);
		}
		return announcementMapper.toResponseDTOList(announcementsRepository
				.findAllByCountryAndCity(user.getCountry(), user.getCity(), pageRequest), currency);
	}

	public List<ResponseDTO> findByTitle(int page, int count, String title, String currency, List<String> filters,
			SortDTO sortDTO) {
		User user = getCurrentUser();
		PageRequest pageRequest = getPageRequest(page, count, sortDTO);
		if (filters != null) {
			return announcementMapper.toResponseDTOList(announcementsRepository
					.findAllByTitleContainsIgnoreCaseAndCountryAndCityAndTypeIn(title, user.getCountry(), user.getCity(),
							filters, pageRequest), currency);
		}
		return announcementMapper.toResponseDTOList(announcementsRepository
				.findAllByTitleContainsIgnoreCaseAndCountryAndCity(title, user.getCountry(), user.getCity(),
						pageRequest), currency);
	}

	private PageRequest getPageRequest(int page, int count, SortDTO sort) {
		String property = sort == null ? "id" : switch (sort.getType()) {
			case LIKES -> "likesCount";
			case WATCHES -> "watchesCount";
			default -> sort.getType().toString().toLowerCase();
		};
		Sort.Direction direction = sort == null ? Sort.Direction.DESC : sort.getDirection();
		return PageRequest.of(page, count, Sort.by(direction, property));
	}

	public List<ResponseDTO> findAllByUser(Long id, String currency) {
		return announcementMapper.toResponseDTOList(announcementsRepository.findAllByOwnerId(id), currency);
	}

	public Announcement findById(Long id) {
		return announcementsRepository.findById(id)
			.orElseThrow(() -> new AnnouncementException("Announcement not found"));
	}

	@Transactional
	public DataDTO<Long> save(CreateDTO createDTO, String currency) {
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
		if (announcement.getImages().size() == maxImagesCount) {
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
	public ShowDTO show(Long id, String currency) {
		Announcement announcement = findById(id);
		Long userId = getCurrentUser().getId();
		watchesRepository.findByAnnouncementAndUserId(announcement, userId).orElseGet(() -> {
			announcement.setWatchesCount(announcement.getWatchesCount() + 1);
			return watchesRepository.save(new Watch(announcement, userId));
		});
		ShowDTO showDTO = announcementMapper.toShowDTO(announcement, currency);
		showDTO.setExpiredTime(announcement.getCreatedAt().plusDays(storageDays));
		showDTO.setLiked(likesRepository.findByAnnouncementAndUserId(announcement, userId).isPresent());
		return showDTO;
	}

	@Transactional
	public DataDTO<Long> report(Long id, String cause) {
		Announcement announcement = findById(id);
		Long userId = getCurrentUser().getId();
		reportsRepository.findByAnnouncementAndSenderId(announcement, userId).ifPresent(report -> {
			throw new AnnouncementException("You already send report to this announcement");
		});
		Report report = reportsRepository.save(Report.builder()
			.announcement(announcement)
			.senderId(userId)
			.cause(cause)
			.build());
		return new DataDTO<>(report.getId());
	}

	@Transactional
	public void closeReport(Long id) {
		reportsRepository.findById(id).ifPresentOrElse(reportsRepository::delete, () -> {
			throw new AnnouncementException("Report not found");
		});
	}

	public List<ResponseReportDTO> getReports(int page, int count, Sort.Direction direction, String currency) {
		return reportMapper.toResponseDTOList(reportsRepository
				.findAll(PageRequest.of(page, count, Sort.by(direction, "id"))).getContent(), currency);
	}

	@Transactional
	public void delete(Long id) {
		Announcement announcement = findById(id);
		checkAnnouncementOwner(announcement);
		announcementsRepository.delete(announcement);
		deleteImages(announcement);
	}

	@Transactional
	public void ban(Long id, String cause) {
		Announcement announcement = findById(id);
		announcementsRepository.delete(announcement);
		deleteImages(announcement);
		String email = (String) usersService.getUserEmail(announcement.getOwnerId(), accessKey).get("data");
		assert email != null;
		String message = String.format("Your announcement with title '%s' has been banned due to: %s",
				announcement.getTitle(), cause);
		emailMessageKafkaTemplate.send("message", new EmailMessageDTO(email, message));
	}

	private void deleteImages(Announcement announcement) {
		List<String> images = announcement.getImages().stream()
				.map(Image::getFileName)
				.toList();
		listKafkaTemplate.send("deleted_announcements_images", images);
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
				announcement
					.setPrice(convertPrice(updateDTO.getCurrency(), course -> announcement.getPrice() / course));
			}
			else {
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

	@Transactional
	@KafkaListener(topics = "deleted_user", containerFactory = "containerFactory")
	public void deletedUserListener(Long id) {
		List<Announcement> announcements = announcementsRepository.findAllByOwnerId(id);
		List<String> images = announcements.stream()
			.flatMap(announcement -> announcement.getImages().stream().map(Image::getFileName))
			.toList();
		listKafkaTemplate.send("deleted_announcements_images", images);
		announcementsRepository.deleteAll(announcements);
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
				response = converterAPI.getCourse(currencyLayerKey, currencyCode.toString(),
						"USD", 1);
				Map<String, Object> quotes = (Map<String, Object>) response.get("quotes");
				double course = (double) quotes.get("USD" + currencyCode);
				BigDecimal bigDecimal = BigDecimal.valueOf(operation.apply(course));
				bigDecimal = bigDecimal.setScale(2, RoundingMode.HALF_UP);
				return bigDecimal.doubleValue();
			}
			catch (Exception e) {
                assert response != null;
                Map<String, Object> error = (Map<String, Object>) response.get("error");
				String message = (String) error.get("info");
				log.error("Error convert price message: {}", message);
				throw e;
			}
		}
		return operation.apply(1.0);
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