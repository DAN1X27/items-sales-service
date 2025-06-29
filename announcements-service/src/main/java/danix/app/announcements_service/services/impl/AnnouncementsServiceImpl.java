package danix.app.announcements_service.services.impl;

import danix.app.announcements_service.dto.CreateAnnouncementDTO;
import danix.app.announcements_service.dto.DataDTO;
import danix.app.announcements_service.dto.EmailMessageDTO;
import danix.app.announcements_service.dto.ResponseAnnouncementDTO;
import danix.app.announcements_service.dto.ResponseReportDTO;
import danix.app.announcements_service.dto.ShowAnnouncementDTO;
import danix.app.announcements_service.dto.ShowReportDTO;
import danix.app.announcements_service.dto.UpdateAnnouncementDTO;
import danix.app.announcements_service.feign.FilesAPI;
import danix.app.announcements_service.feign.UsersAPI;
import danix.app.announcements_service.mapper.AnnouncementMapper;
import danix.app.announcements_service.mapper.ReportMapper;
import danix.app.announcements_service.models.*;
import danix.app.announcements_service.repositories.*;
import danix.app.announcements_service.util.SecurityUtil;
import danix.app.announcements_service.services.AnnouncementsService;
import danix.app.announcements_service.services.CurrencyConverterService;
import danix.app.announcements_service.util.AnnouncementException;
import danix.app.announcements_service.util.CurrencyCode;
import danix.app.announcements_service.util.SortData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnouncementsServiceImpl implements AnnouncementsService {

	private final AnnouncementsRepository announcementsRepository;

	private final ImagesRepository imagesRepository;

	private final WatchesRepository watchesRepository;

	private final LikesRepository likesRepository;

	private final ReportsRepository reportsRepository;

	private final CurrencyConverterService currencyConverterService;

	private final FilesAPI filesAPI;

	private final UsersAPI usersAPI;

	private final KafkaTemplate<String, List<String>> listKafkaTemplate;

	private final KafkaTemplate<String, EmailMessageDTO> emailMessageKafkaTemplate;

	private final AnnouncementMapper announcementMapper;

	private final ReportMapper reportMapper;

	private final SecurityUtil securityUtil;

	private static final Sort idSort = Sort.by(Sort.Direction.DESC, "id");

	@Value("${max_images_count}")
	private int maxImagesCount;

	@Value("${max_storage_days}")
	private int storageDays;

	@Value("${access_key}")
	private String accessKey;

	@Override
	public List<ResponseAnnouncementDTO> findAll(int page, int count, CurrencyCode currency, List<String> filters,
												 SortData sortData, String country, String city) {
		PageRequest pageRequest = getPageRequest(page, count, sortData);
		if (filters != null) {
			List<Long> ids = announcementMapper.toIdsListFromProjectionsList(announcementsRepository
					.findAllByCountryAndCityAndTypeIn(country, city, pageRequest, filters));
			return announcementMapper.toResponseDTOList(announcementsRepository
					.findAllByIdIn(ids, pageRequest.getSort()), currency);
		}
		List<Long> ids = announcementMapper.toIdsListFromProjectionsList(announcementsRepository
				.findAllByCountryAndCity(country, city, pageRequest));
		return announcementMapper.toResponseDTOList(announcementsRepository
				.findAllByIdIn(ids, pageRequest.getSort()), currency);
	}

	@Override
	public List<ResponseAnnouncementDTO> findByTitle(int page, int count, String title, CurrencyCode currency, List<String> filters,
													SortData sortData, String country, String city) {
		PageRequest pageRequest = getPageRequest(page, count, sortData);
		if (filters != null) {
			List<Long> ids = announcementMapper.toIdsListFromProjectionsList(announcementsRepository
					.findAllByTitleContainsIgnoreCaseAndCountryAndCityAndTypeIn(title, country, city, filters, pageRequest));
			return announcementMapper.toResponseDTOList(announcementsRepository
					.findAllByIdIn(ids, pageRequest.getSort()), currency);
		}
		List<Long> ids = announcementMapper.toIdsListFromProjectionsList(announcementsRepository
				.findAllByTitleContainsIgnoreCaseAndCountryAndCity(title, country, city, pageRequest));
		return announcementMapper
				.toResponseDTOList(announcementsRepository.findAllByIdIn(ids, pageRequest.getSort()), currency);
	}

	private PageRequest getPageRequest(int page, int count, SortData sortData) {
		String property = switch (sortData.type()) {
			case LIKES -> "likesCount";
			case WATCHES -> "watchesCount";
			default -> sortData.type().toString().toLowerCase();
		};
		Sort.Direction direction = sortData.direction();
		return PageRequest.of(page, count, Sort.by(direction, property));
	}

	@Override
	public List<ResponseAnnouncementDTO> findAllByUser(Long id, CurrencyCode currency, int page, int count) {
		List<Long> ids = announcementMapper.toIdsListFromProjectionsList(announcementsRepository
				.findAllByOwnerId(id, PageRequest.of(page, count, idSort)));
		return announcementMapper.toResponseDTOList(announcementsRepository.findAllByIdIn(ids, idSort), currency);
	}

	@Override
	@Transactional
	public DataDTO<Long> save(CreateAnnouncementDTO createDTO, CurrencyCode currency) {
		Announcement announcement = announcementMapper.fromCreateDTO(createDTO);
		announcement.setCreatedAt(LocalDateTime.now());
		announcement.setOwnerId(securityUtil.getCurrentUser().getId());
		announcement.setPrice(currencyConverterService.convertPrice(currency, course -> announcement.getPrice() / course));
		announcementsRepository.save(announcement);
		return new DataDTO<>(announcement.getId());
	}

	@Override
	public byte[] downloadImage(Long id) {
		Image image = imagesRepository.findById(id)
				.orElseThrow(() -> new AnnouncementException("Image not found"));
		return filesAPI.downloadImage(image.getFileName(), accessKey);
	}

	@Override
	@Transactional
	public void addImage(MultipartFile image, Long id) {
		Announcement announcement = findById(id);
		checkAnnouncementOwner(announcement);
		if (announcement.getImages().size() >= maxImagesCount) {
			throw new AnnouncementException("Images limit acceded");
		}
		String uuid = UUID.randomUUID() + ".jpg";
		filesAPI.saveImage(image, uuid, accessKey);
		imagesRepository.save(new Image(uuid, announcement));
	}

	@Override
	@Transactional
	public void deleteImage(Long id) {
		Image image = imagesRepository.findById(id)
				.orElseThrow(() -> new AnnouncementException("Image not found"));
		checkAnnouncementOwner(image.getAnnouncement());
		filesAPI.deleteImage(image.getFileName(), accessKey);
		imagesRepository.delete(image);
	}

	@Override
	@Transactional
	public void addLike(Long id) {
		Announcement announcement = findById(id);
		Long userId = securityUtil.getCurrentUser().getId();
		likesRepository.findByAnnouncementAndUserId(announcement, userId).ifPresent(like -> {
			throw new AnnouncementException("Like already exists");
		});
		likesRepository.save(new Like(announcement, userId));
		announcement.setLikesCount(announcement.getLikesCount() + 1);
	}

	@Override
	@Transactional
	public void deleteLike(Long id) {
		Announcement announcement = findById(id);
		Long userId = securityUtil.getCurrentUser().getId();
		likesRepository.findByAnnouncementAndUserId(announcement, userId)
			.ifPresentOrElse(likesRepository::delete, () -> {
				throw new AnnouncementException("Like not found");
			});
		announcement.setLikesCount(announcement.getLikesCount() - 1);
	}

	@Override
	@Transactional
	public ShowAnnouncementDTO show(Long id, CurrencyCode currency) {
		Announcement announcement = findById(id);
		ShowAnnouncementDTO showDTO = announcementMapper.toShowDTO(announcement, currency);
		showDTO.setExpiredDate(announcement.getCreatedAt().plusDays(storageDays));
		if (securityUtil.isAuthenticated()) {
			Long userId = securityUtil.getCurrentUser().getId();
			watchesRepository.findByAnnouncementAndUserId(announcement, userId).orElseGet(() -> {
				announcement.setWatchesCount(announcement.getWatchesCount() + 1);
				return watchesRepository.save(new Watch(announcement, userId));
			});
			showDTO.setLiked(likesRepository.findByAnnouncementAndUserId(announcement, userId).isPresent());
		}
		return showDTO;
	}

	@Override
	@Transactional
	public void createReport(Long id, String cause) {
		Announcement announcement = findById(id);
		Long userId = securityUtil.getCurrentUser().getId();
		reportsRepository.findByAnnouncementAndSenderId(announcement, userId).ifPresent(report -> {
			throw new AnnouncementException("You already send report to this announcement");
		});
		reportsRepository.save(Report.builder()
			.announcement(announcement)
			.senderId(userId)
			.cause(cause)
			.build());
	}

	@Override
	@Transactional
	public void closeReport(Long id) {
		reportsRepository.findById(id).ifPresentOrElse(reportsRepository::delete, () -> {
			throw new AnnouncementException("Report not found");
		});
	}

	@Override
	public List<ResponseReportDTO> getAllReports(int page, int count, Sort.Direction direction) {
		return reportMapper.toResponseDTOList(reportsRepository
				.findAll(PageRequest.of(page, count, Sort.by(direction, "id"))).getContent());
	}

	@Override
	public ShowReportDTO showReport(long id, CurrencyCode currency) {
		Report report = reportsRepository.findById(id).orElseThrow(() -> new AnnouncementException("Report not found"));
		ShowReportDTO showDTO = reportMapper.toShowDTO(report);
		showDTO.setAnnouncement(announcementMapper.toResponseDTO(report.getAnnouncement(), currency));
		return showDTO;
	}

	@Override
	@Transactional
	public void delete(Long id) {
		Announcement announcement = findById(id);
		checkAnnouncementOwner(announcement);
		deleteImages(announcement);
		announcementsRepository.deleteById(announcement.getId());
	}

	@Override
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

	@Override
	@Transactional
	public void update(Long id, UpdateAnnouncementDTO updateDTO) {
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
				announcement.setPrice(currencyConverterService
						.convertPrice(updateDTO.getCurrency(), course -> updateDTO.getPrice() / course));
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

	@Override
	@Transactional
	@Async("virtualExecutor")
	public void deleteExpired() {
		int page = 0;
		while (true) {
			List<Long> ids = announcementMapper.toIdsListFromProjectionsList(announcementsRepository
					.findAllByCreatedAtBefore(LocalDateTime.now().minusDays(storageDays),
							PageRequest.of(page, 50)));
			if (ids.isEmpty()) {
				break;
			}
			List<Announcement> announcements = announcementsRepository.findAllByIdIn(ids, idSort);
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
					.findAllByOwnerId(id, PageRequest.of(page, 50, idSort)));
			if (ids.isEmpty()) {
				break;
			}
			List<Announcement> announcements = announcementsRepository.findAllByIdIn(ids, idSort);
			deleteImages(announcements);
			announcementsRepository.deleteAllByIdIn(ids);
			page++;
		}
	}

	private Announcement findById(Long id) {
		return announcementsRepository.findById(id)
				.orElseThrow(() -> new AnnouncementException("Announcement not found"));
	}

	private void deleteImages(Announcement announcement) {
		if (!announcement.getImages().isEmpty()) {
			List<String> images = announcement.getImages().stream()
					.map(Image::getFileName)
					.toList();
			listKafkaTemplate.send("deleted_announcement", images);
		}
	}

	private void deleteImages(List<Announcement> announcements) {
		List<String> images = announcements.stream()
				.flatMap(announcement -> announcement.getImages().stream().map(Image::getFileName))
				.toList();
		if (!images.isEmpty()) {
			listKafkaTemplate.send("deleted_announcement", images);
		}
	}

	private String getUserEmail(Long id) {
		return (String) usersAPI.getUserEmail(id, accessKey).get("data");
	}

	private void checkAnnouncementOwner(Announcement announcement) {
		if (!announcement.getOwnerId().equals(securityUtil.getCurrentUser().getId())) {
			throw new AnnouncementException("You are not owner of this announcement");
		}
	}

}