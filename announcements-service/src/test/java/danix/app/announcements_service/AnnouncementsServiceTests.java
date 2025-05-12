package danix.app.announcements_service;

import danix.app.announcements_service.dto.*;
import danix.app.announcements_service.feign.CurrencyConverterAPI;
import danix.app.announcements_service.feign.FilesService;
import danix.app.announcements_service.feign.UsersService;
import danix.app.announcements_service.mapper.AnnouncementMapper;
import danix.app.announcements_service.mapper.ReportMapper;
import danix.app.announcements_service.models.*;
import danix.app.announcements_service.repositories.*;
import danix.app.announcements_service.security.User;
import danix.app.announcements_service.security.UserDetailsImpl;
import danix.app.announcements_service.services.AnnouncementsService;
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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import static danix.app.announcements_service.util.CurrencyCode.USD;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.IntStream;

@Slf4j
@ExtendWith(MockitoExtension.class)
class AnnouncementsServiceTests {

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
    private FilesService filesService;

    @Mock
    private CurrencyConverterAPI converterAPI;

    @Mock
    private UsersService usersService;

    @Mock
    private KafkaTemplate<String, List<String>> listKafkaTemplate;

    @Mock
    private KafkaTemplate<String, EmailMessageDTO> emailMessageKafkaTemplate;

    @Mock
    private AnnouncementMapper announcementMapper;

    @Mock
    private ReportMapper reportMapper;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private AnonymousAuthenticationToken anonymousToken;

    @InjectMocks
    private AnnouncementsService announcementsService;

    private static final User testUser = User.builder().id(1L).build();


    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(announcementsService, "listKafkaTemplate", listKafkaTemplate);
        ReflectionTestUtils.setField(announcementsService, "emailMessageKafkaTemplate", emailMessageKafkaTemplate);
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
        verify(filesService).saveImage(any(), any(), any());
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
        verify(filesService).deleteImage(any(), any());
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
        announcementsService.like(announcement.getId());
        verify(likesRepository).save(any(Like.class));
        assertEquals(1, announcement.getLikesCount());
    }

    @Test
    public void likeWhenAnnouncementNotFound() {
        when(announcementsRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(AnnouncementException.class, () -> announcementsService.like(1L));
    }

    @Test
    public void likeWhenLikeAlreadyExists() {
        Announcement announcement = getTestAnnouncement();
        when(announcementsRepository.findById(announcement.getId())).thenReturn(Optional.of(announcement));
        when(likesRepository.findByAnnouncementAndUserId(announcement, testUser.getId())).thenReturn(Optional.of(new Like()));
        mockCurrentUser();
        assertThrows(AnnouncementException.class, () -> announcementsService.like(announcement.getId()));
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
        ShowAnnouncementDTO showDTO = announcementsService.show(announcement.getId(), USD);
        assertTrue(showDTO.isLiked());
    }

    @Test
    public void showWhenUserDoesNotAuthenticated() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(anonymousToken);
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
        announcementsService.report(announcement.getId(), "Test");
        verify(reportsRepository).save(any(Report.class));
    }

    @Test
    public void reportWhenAnnouncementNotFound() {
        when(announcementsRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(AnnouncementException.class, () -> announcementsService.report(1L, "Test"));
    }

    @Test
    public void reportWhenReportAlreadyExists() {
        Announcement announcement = getTestAnnouncement();
        when(announcementsRepository.findById(announcement.getId())).thenReturn(Optional.of(announcement));
        mockCurrentUser();
        when(reportsRepository.findByAnnouncementAndSenderId(announcement, testUser.getId())).thenReturn(Optional.of(new Report()));
        assertThrows(AnnouncementException.class, () -> announcementsService.report(announcement.getId(), "Test"));
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
        ShowReportDTO showDTO = announcementsService.getReport(report.getId(), USD);
        assertEquals(announcementDTO, showDTO.getAnnouncement());
    }

    @Test
    public void getReportWhenReportNotFound() {
        when(reportsRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(AnnouncementException.class, () -> announcementsService.getReport(1L, USD));
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
        verify(listKafkaTemplate).send(eq("deleted_announcements_images"), any());
    }

    @Test
    public void deleteWhenAnnouncementHasNoImages() {
        Announcement announcement = getTestAnnouncement();
        announcement.setImages(Collections.emptyList());
        when(announcementsRepository.findById(announcement.getId())).thenReturn(Optional.of(announcement));
        mockCurrentUser();
        announcementsService.delete(announcement.getId());
        verify(announcementsRepository).deleteById(announcement.getId());
        verify(listKafkaTemplate, never()).send(eq("deleted_announcements_images"), any());
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
        when(usersService.getUserEmail(eq(announcement.getOwnerId()), any())).thenReturn(Map.of("data", "test@gmail.com"));
        announcementsService.ban(announcement.getId(), "test_cause");
        verify(announcementsRepository).deleteById(announcement.getId());
        verify(listKafkaTemplate).send(eq("deleted_announcements_images"), any());
        verify(emailMessageKafkaTemplate).send(eq("message"), any());
    }

    @Test
    public void banWhenAnnouncementHasNoImages() {
        Announcement announcement = getTestAnnouncement();
        announcement.setImages(Collections.emptyList());
        when(announcementsRepository.findById(announcement.getId())).thenReturn(Optional.of(announcement));
        when(usersService.getUserEmail(eq(announcement.getOwnerId()), any())).thenReturn(Map.of("data", "test@gmail.com"));
        announcementsService.ban(announcement.getId(), "test_cause");
        verify(announcementsRepository).deleteById(announcement.getId());
        verify(listKafkaTemplate, never()).send(eq("deleted_announcements_images"), any());
        verify(emailMessageKafkaTemplate).send(eq("message"), any());
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

    private Announcement getTestAnnouncement() {
        return Announcement.builder()
                .id(1L)
                .ownerId(testUser.getId())
                .title("test_title")
                .description("test_description")
                .country("test_country")
                .city("test_city")
                .price(100.0)
                .type("test_type")
                .createdAt(LocalDateTime.now())
                .build();
    }

    private void mockCurrentUser() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetailsImpl(testUser));
    }

}
