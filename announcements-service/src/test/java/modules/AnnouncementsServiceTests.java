package modules;

import danix.app.announcements_service.dto.*;
import danix.app.announcements_service.feign.FilesAPI;
import danix.app.announcements_service.feign.UsersAPI;
import danix.app.announcements_service.mapper.AnnouncementMapper;
import danix.app.announcements_service.mapper.ReportMapper;
import danix.app.announcements_service.models.*;
import danix.app.announcements_service.repositories.*;
import danix.app.announcements_service.util.SecurityUtil;
import danix.app.announcements_service.models.User;
import danix.app.announcements_service.services.CurrencyConverterService;
import danix.app.announcements_service.services.impl.AnnouncementsServiceImpl;
import danix.app.announcements_service.util.AnnouncementException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import util.TestUtil;

import static danix.app.announcements_service.util.CurrencyCode.USD;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static util.TestUtil.getTestAnnouncement;

import java.util.*;
import java.util.stream.IntStream;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class AnnouncementsServiceTests {

    @Mock
    private AnnouncementsRepository announcementsRepository;

    @Mock
    private ImagesRepository imagesRepository;

    @Mock
    private WatchesRepository watchesRepository;

    @Mock
    private LikesRepository likesRepository;

    @Mock
    private ReportsRepository reportsRepository;

    @Mock
    private FilesAPI filesAPI;

    @Mock
    private CurrencyConverterService currencyConverterService;

    @Mock
    private UsersAPI usersAPI;

    @Mock
    private KafkaTemplate<String, List<String>> listKafkaTemplate;

    @Mock
    private KafkaTemplate<String, EmailMessageDTO> emailMessageKafkaTemplate;

    @Mock
    private AnnouncementMapper announcementMapper;

    @Mock
    private ReportMapper reportMapper;

    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private AnnouncementsServiceImpl announcementsService;

    private final String deletedAnnouncementTopic = "deleted_announcement";

    private final String emailMessageTopic = "email_message";

    private static final User testUser = TestUtil.getTestUser();

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(announcementsService, "listKafkaTemplate", listKafkaTemplate);
        ReflectionTestUtils.setField(announcementsService, "emailMessageKafkaTemplate", emailMessageKafkaTemplate);
        ReflectionTestUtils.setField(announcementsService, "deletedAnnouncementTopic", deletedAnnouncementTopic);
        ReflectionTestUtils.setField(announcementsService, "emailMessageTopic", emailMessageTopic);
    }

    @Test
    public void save() {
        CreateAnnouncementDTO createDTO = new CreateAnnouncementDTO();
        Announcement announcement = getTestAnnouncement();
        when(announcementMapper.fromCreateDTO(createDTO)).thenReturn(announcement);
        mockCurrentUser();
        announcementsService.save(createDTO, USD);
        verify(announcementsRepository).save(announcement);
    }

    @Test
    public void addImage() {
        Announcement announcement = getTestAnnouncement();
        announcement.setImages(Collections.emptyList());
        when(announcementsRepository.findById(announcement.getId())).thenReturn(Optional.of(announcement));
        mockCurrentUser();
        MultipartFile image = new MockMultipartFile("test", new byte[]{});
        ReflectionTestUtils.setField(announcementsService, "maxImagesCount", 10);
        announcementsService.addImage(image, announcement.getId());
        verify(filesAPI).saveImage(any(), any(), any());
        verify(imagesRepository).save(any());
    }

    @Test
    public void addImageWhenMaxImagesCountAcceded() {
        Announcement announcement = getTestAnnouncement();
        List<Image> images = new ArrayList<>();
        IntStream.range(0, 10).forEach(i -> images.add(new Image("test_name", announcement)));
        announcement.setImages(images);
        when(announcementsRepository.findById(announcement.getId())).thenReturn(Optional.of(announcement));
        mockCurrentUser();
        MultipartFile image = new MockMultipartFile("test", new byte[0]);
        ReflectionTestUtils.setField(announcementsService, "maxImagesCount", 10);
        assertThrows(AnnouncementException.class, () -> announcementsService.addImage(image, announcement.getId()));
    }

    @Test
    public void addImageWhenCurrentUserIsNotOwner() {
        Announcement announcement = getTestAnnouncement();
        announcement.setOwnerId(2L);
        when(announcementsRepository.findById(announcement.getId())).thenReturn(Optional.of(announcement));
        mockCurrentUser();
        MultipartFile image = new MockMultipartFile("test", new byte[0]);
        assertThrows(AnnouncementException.class, () -> announcementsService.addImage(image, announcement.getId()));
    }

    @Test
    public void addImageWhenAnnouncementNotFound() {
        when(announcementsRepository.findById(1L)).thenReturn(Optional.empty());
        MultipartFile image = new MockMultipartFile("test", new byte[0]);
        assertThrows(AnnouncementException.class, () -> announcementsService.addImage(image, 1L));
    }

    @Test
    public void deleteImage() {
        Announcement announcement = getTestAnnouncement();
        Image image = new Image();
        image.setId(1L);
        image.setAnnouncement(announcement);
        image.setFileName("file_name");
        when(imagesRepository.findById(image.getId())).thenReturn(Optional.of(image));
        mockCurrentUser();
        announcementsService.deleteImage(image.getId());
        verify(filesAPI).deleteImage(any(), any());
        verify(imagesRepository).delete(image);
    }

    @Test
    public void deleteImageWhenImageNotFound() {
        when(imagesRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(AnnouncementException.class, () -> announcementsService.deleteImage(1L));
    }

    @Test
    public void deleteImageWhenCurrentUserIsNotOwnerOfAnnouncement() {
        Announcement announcement = getTestAnnouncement();
        announcement.setOwnerId(2L);
        Image image = new Image();
        image.setAnnouncement(announcement);
        when(imagesRepository.findById(1L)).thenReturn(Optional.of(image));
        mockCurrentUser();
        assertThrows(AnnouncementException.class, () -> announcementsService.deleteImage(1L));
    }

    @Test
    public void like() {
        Announcement announcement = getTestAnnouncement();
        mockCurrentUser();
        when(announcementsRepository.findById(announcement.getId())).thenReturn(Optional.of(announcement));
        when(likesRepository.findByAnnouncementAndUserId(announcement, testUser.getId())).thenReturn(Optional.empty());
        announcementsService.addLike(announcement.getId());
        verify(likesRepository).save(any(Like.class));
        assertEquals(1, announcement.getLikesCount());
    }

    @Test
    public void likeWhenAnnouncementNotFound() {
        when(announcementsRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(AnnouncementException.class, () -> announcementsService.addLike(1L));
    }

    @Test
    public void likeWhenLikeAlreadyExists() {
        Announcement announcement = getTestAnnouncement();
        when(announcementsRepository.findById(announcement.getId())).thenReturn(Optional.of(announcement));
        when(likesRepository.findByAnnouncementAndUserId(announcement, testUser.getId())).thenReturn(Optional.of(new Like()));
        mockCurrentUser();
        assertThrows(AnnouncementException.class, () -> announcementsService.addLike(announcement.getId()));
    }

    @Test
    public void deleteLike() {
        Announcement announcement = getTestAnnouncement();
        announcement.setLikesCount(1);
        when(announcementsRepository.findById(announcement.getId())).thenReturn(Optional.of(announcement));
        mockCurrentUser();
        when(likesRepository.findByAnnouncementAndUserId(announcement, testUser.getId())).thenReturn(Optional.of(new Like()));
        announcementsService.deleteLike(announcement.getId());
        verify(likesRepository).delete(any(Like.class));
        assertEquals(0, announcement.getLikesCount());
    }

    @Test
    public void deleteLikeWhenAnnouncementNotFound() {
        when(announcementsRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(AnnouncementException.class, () -> announcementsService.deleteLike(1L));
    }

    @Test
    public void deleteLikeWhenLikeNotFound() {
        Announcement announcement = getTestAnnouncement();
        announcement.setLikesCount(1);
        when(announcementsRepository.findById(announcement.getId())).thenReturn(Optional.of(announcement));
        mockCurrentUser();
        when(likesRepository.findByAnnouncementAndUserId(announcement, testUser.getId())).thenReturn(Optional.empty());
        assertThrows(AnnouncementException.class, () -> announcementsService.deleteLike(announcement.getId()));
    }

    @Test
    public void showWhenAnnouncementNotWatched() {
        Announcement announcement = getTestAnnouncement();
        mockCurrentUser();
        when(announcementsRepository.findById(announcement.getId())).thenReturn(Optional.of(announcement));
        when(watchesRepository.findByAnnouncementAndUserId(announcement, testUser.getId())).thenReturn(Optional.empty());
        when(announcementMapper.toShowDTO(announcement, USD)).thenReturn(new ShowAnnouncementDTO());
        when(likesRepository.findByAnnouncementAndUserId(announcement, testUser.getId())).thenReturn(Optional.empty());
        when(securityUtil.isAuthenticated()).thenReturn(true);
        ReflectionTestUtils.setField(announcementsService, "storageDays", 30);
        ShowAnnouncementDTO showDTO = announcementsService.show(announcement.getId(), USD);
        verify(watchesRepository).save(any(Watch.class));
        assertEquals(1, announcement.getWatchesCount());
        assertEquals(announcement.getCreatedAt().plusDays(30), showDTO.getExpiredDate());
        assertFalse(showDTO.isLiked());
    }

    @Test
    public void showWhenAnnouncementLiked() {
        Announcement announcement = getTestAnnouncement();
        mockCurrentUser();
        when(announcementsRepository.findById(announcement.getId())).thenReturn(Optional.of(announcement));
        when(watchesRepository.findByAnnouncementAndUserId(announcement, testUser.getId())).thenReturn(Optional.of(new Watch()));
        when(announcementMapper.toShowDTO(announcement, USD)).thenReturn(new ShowAnnouncementDTO());
        when(likesRepository.findByAnnouncementAndUserId(announcement, testUser.getId())).thenReturn(Optional.of(new Like()));
        when(securityUtil.isAuthenticated()).thenReturn(true);
        ShowAnnouncementDTO showDTO = announcementsService.show(announcement.getId(), USD);
        assertTrue(showDTO.isLiked());
    }

    @Test
    public void showWhenUserDoesNotAuthenticated() {
        when(securityUtil.isAuthenticated()).thenReturn(false);
        Announcement announcement = getTestAnnouncement();
        when(announcementsRepository.findById(announcement.getId())).thenReturn(Optional.of(announcement));
        when(announcementMapper.toShowDTO(announcement, USD)).thenReturn(new ShowAnnouncementDTO());
        ShowAnnouncementDTO showDTO = announcementsService.show(announcement.getId(), USD);
        verify(watchesRepository, never()).findByAnnouncementAndUserId(any(), any());
        verify(likesRepository, never()).findByAnnouncementAndUserId(any(), any());
        assertFalse(showDTO.isLiked());
    }

    @Test
    public void showWhenAnnouncementNotFound() {
        when(announcementsRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(AnnouncementException.class, () -> announcementsService.show(1L, USD));
    }

    @Test
    public void report() {
        Announcement announcement = getTestAnnouncement();
        mockCurrentUser();
        when(announcementsRepository.findById(announcement.getId())).thenReturn(Optional.of(announcement));
        when(reportsRepository.findByAnnouncementAndSenderId(announcement, testUser.getId())).thenReturn(Optional.empty());
        announcementsService.createReport(announcement.getId(), "Test");
        verify(reportsRepository).save(any(Report.class));
    }

    @Test
    public void reportWhenAnnouncementNotFound() {
        when(announcementsRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(AnnouncementException.class, () -> announcementsService.createReport(1L, "Test"));
    }

    @Test
    public void reportWhenReportAlreadyExists() {
        Announcement announcement = getTestAnnouncement();
        when(announcementsRepository.findById(announcement.getId())).thenReturn(Optional.of(announcement));
        mockCurrentUser();
        when(reportsRepository.findByAnnouncementAndSenderId(announcement, testUser.getId())).thenReturn(Optional.of(new Report()));
        assertThrows(AnnouncementException.class, () -> announcementsService.createReport(announcement.getId(), "Test"));
    }

    @Test
    public void closeReport() {
        Report report = Report.builder().id(1L).build();
        when(reportsRepository.findById(report.getId())).thenReturn(Optional.of(report));
        announcementsService.closeReport(report.getId());
        verify(reportsRepository).delete(report);
    }

    @Test
    public void closeReportWhenReportNotFound() {
        when(reportsRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(AnnouncementException.class, () -> announcementsService.closeReport(1L));
    }

    @Test
    public void getReport() {
        Announcement announcement = getTestAnnouncement();
        Report report = Report.builder().id(1L).announcement(announcement).build();
        when(reportsRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(reportMapper.toShowDTO(report)).thenReturn(new ShowReportDTO());
        ResponseAnnouncementDTO announcementDTO = new ResponseAnnouncementDTO();
        when(announcementMapper.toResponseDTO(announcement, USD)).thenReturn(announcementDTO);
        ShowReportDTO showDTO = announcementsService.showReport(report.getId(), USD);
        assertEquals(announcementDTO, showDTO.getAnnouncement());
    }

    @Test
    public void getReportWhenReportNotFound() {
        when(reportsRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(AnnouncementException.class, () -> announcementsService.showReport(1L, USD));
    }

    @Test
    public void delete() {
        Announcement announcement = getTestAnnouncement();
        List<Image> images = new ArrayList<>();
        IntStream.range(0, 5).forEach(i -> images.add(new Image("test_name", announcement)));
        announcement.setImages(images);
        when(announcementsRepository.findById(announcement.getId())).thenReturn(Optional.of(announcement));
        mockCurrentUser();
        announcementsService.delete(announcement.getId());
        verify(announcementsRepository).deleteById(announcement.getId());
        verify(listKafkaTemplate).send(eq(deletedAnnouncementTopic), any());
    }

    @Test
    public void deleteWhenAnnouncementHasNoImages() {
        Announcement announcement = getTestAnnouncement();
        announcement.setImages(Collections.emptyList());
        when(announcementsRepository.findById(announcement.getId())).thenReturn(Optional.of(announcement));
        mockCurrentUser();
        announcementsService.delete(announcement.getId());
        verify(announcementsRepository).deleteById(announcement.getId());
        verify(listKafkaTemplate, never()).send(eq(deletedAnnouncementTopic), any());
    }

    @Test
    public void deleteWhenAnnouncementNotFound() {
        when(announcementsRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(AnnouncementException.class, () -> announcementsService.delete(1L));
    }

    @Test
    public void deleteWhenCurrentUserIsNotAnnouncementOwner() {
        Announcement announcement = getTestAnnouncement();
        announcement.setOwnerId(2L);
        when(announcementsRepository.findById(announcement.getId())).thenReturn(Optional.of(announcement));
        mockCurrentUser();
        assertThrows(AnnouncementException.class, () -> announcementsService.delete(announcement.getId()));
    }

    @Test
    public void ban() {
        Announcement announcement = getTestAnnouncement();
        List<Image> images = new ArrayList<>();
        IntStream.range(0, 5).forEach(i -> images.add(new Image("test_name", announcement)));
        announcement.setImages(images);
        when(announcementsRepository.findById(announcement.getId())).thenReturn(Optional.of(announcement));
        when(usersAPI.getUserEmail(eq(announcement.getOwnerId()), any())).thenReturn(Map.of("data", "test@gmail.com"));
        announcementsService.ban(announcement.getId(), "test_cause");
        verify(announcementsRepository).deleteById(announcement.getId());
        verify(listKafkaTemplate).send(eq(deletedAnnouncementTopic), any());
        verify(emailMessageKafkaTemplate).send(eq(emailMessageTopic), any());
    }

    @Test
    public void banWhenAnnouncementHasNoImages() {
        Announcement announcement = getTestAnnouncement();
        announcement.setImages(Collections.emptyList());
        when(announcementsRepository.findById(announcement.getId())).thenReturn(Optional.of(announcement));
        when(usersAPI.getUserEmail(eq(announcement.getOwnerId()), any())).thenReturn(Map.of("data", "test@gmail.com"));
        announcementsService.ban(announcement.getId(), "test_cause");
        verify(announcementsRepository).deleteById(announcement.getId());
        verify(listKafkaTemplate, never()).send(eq(deletedAnnouncementTopic), any());
        verify(emailMessageKafkaTemplate).send(eq(emailMessageTopic), any());
    }

    @Test
    public void banWhenAnnouncementNotFound() {
        when(announcementsRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(AnnouncementException.class, () -> announcementsService.ban(1L, "test_cause"));
    }

    @Test
    public void updateWhenAllParamsExists() {
        Announcement announcement = getTestAnnouncement();
        UpdateAnnouncementDTO updateDTO = UpdateAnnouncementDTO.builder()
                .title("new title")
                .description("new description")
                .price(announcement.getPrice() * 2)
                .city("new city")
                .country("new country")
                .phoneNumber("new phone number")
                .type("new type")
                .currency(USD)
                .build();
        when(announcementsRepository.findById(announcement.getId())).thenReturn(Optional.of(announcement));
        when(currencyConverterService.convertPrice(any(), any())).thenReturn(updateDTO.getPrice());
        mockCurrentUser();
        announcementsService.update(announcement.getId(), updateDTO);
        assertEquals(updateDTO.getTitle(), announcement.getTitle());
        assertEquals(updateDTO.getDescription(), announcement.getDescription());
        assertEquals(updateDTO.getPrice(), announcement.getPrice());
        assertEquals(updateDTO.getType(), announcement.getType());
        assertEquals(updateDTO.getCity(), announcement.getCity());
        assertEquals(updateDTO.getCountry(), announcement.getCountry());
    }

    @Test
    public void updateWhenOnlyTitleExists() {
        Announcement announcement = getTestAnnouncement();
        UpdateAnnouncementDTO updateDTO = UpdateAnnouncementDTO.builder().title("new title").build();
        when(announcementsRepository.findById(announcement.getId())).thenReturn(Optional.of(announcement));
        mockCurrentUser();
        announcementsService.update(announcement.getId(), updateDTO);
        assertEquals(updateDTO.getTitle(), announcement.getTitle());
        assertNotNull(announcement.getDescription());
        assertNotNull(announcement.getPrice());
        assertNotNull(announcement.getType());
        assertNotNull(announcement.getCity());
        assertNotNull(announcement.getCountry());
    }

    @Test
    public void updateWhenAnnouncementNotFound() {
        when(announcementsRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(AnnouncementException.class, () -> announcementsService.update(1L, UpdateAnnouncementDTO.builder().build()));
    }

    @Test
    public void updateWhenCurrentUserIsNotAnnouncementOwner() {
        Announcement announcement = getTestAnnouncement();
        announcement.setOwnerId(2L);
        when(announcementsRepository.findById(announcement.getId())).thenReturn(Optional.of(announcement));
        mockCurrentUser();
        assertThrows(AnnouncementException.class, () -> announcementsService.update(announcement.getId(), UpdateAnnouncementDTO.builder().build()));
    }

    private void mockCurrentUser() {
        when(securityUtil.getCurrentUser()).thenReturn(testUser);
    }

}
