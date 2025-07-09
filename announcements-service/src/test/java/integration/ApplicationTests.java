package integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import danix.app.announcements_service.AnnouncementsServiceApplication;
import danix.app.announcements_service.config.SecurityConfig;
import danix.app.announcements_service.dto.CauseDTO;
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
import danix.app.announcements_service.models.Announcement;
import danix.app.announcements_service.models.Image;
import danix.app.announcements_service.models.Like;
import danix.app.announcements_service.models.Report;
import danix.app.announcements_service.models.User;
import danix.app.announcements_service.repositories.AnnouncementsRepository;
import danix.app.announcements_service.repositories.ImagesRepository;
import danix.app.announcements_service.repositories.LikesRepository;
import danix.app.announcements_service.repositories.ReportsRepository;
import danix.app.announcements_service.services.impl.AnnouncementsServiceImpl;
import danix.app.announcements_service.util.SecurityUtil;
import danix.app.announcements_service.util.SortType;
import integration.config.TestSecurityConfig;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.PostgreSQLContainer;
import util.TestUtil;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = AnnouncementsServiceApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import({TestSecurityConfig.class})
@EmbeddedKafka(brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"},
        topics = {"${kafka-topics.email-message}", "${kafka-topics.deleted-user}", "${kafka-topics.deleted-announcement}"})
public class ApplicationTests {

    @MockitoBean
    private SecurityConfig securityConfig;

    @MockitoBean
    private SecurityUtil securityUtil;

    @MockitoBean
    private FilesAPI filesAPI;

    @MockitoBean
    private UsersAPI usersAPI;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AnnouncementsRepository announcementsRepository;

    @Autowired
    private ImagesRepository imagesRepository;

    @Autowired
    private LikesRepository likesRepository;

    @Autowired
    private ReportsRepository reportsRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${spring.kafka.consumer.bootstrap-servers}")
    private String consumerBootstrapServers;

    @Value("${test-kafka-group-id}")
    private String consumerGroupId;

    @Value("${max_storage_days}")
    private int announcementsStorageDays;

    @Value("${access_key}")
    private String accessKey;

    @Value("${kafka-topics.email-message}")
    private String emailMessageTopic;

    @Value("${kafka-topics.deleted-announcement}")
    private String deletedAnnouncementTopic;

    @Value("${kafka-topics.deleted-user}")
    private String deletedUserTopic;

    private final BlockingQueue<EmailMessageDTO> emailMessagesQueue = new LinkedBlockingQueue<>();

    private final BlockingQueue<List<String>> deletedImagesQueue = new LinkedBlockingQueue<>();

    private final List<KafkaMessageListenerContainer> listenerContainers = new ArrayList<>();

    private final TestRepository testRepository = new TestRepository();

    private static final String DEFAULT_PATH = "/announcements";

    private static final int PARTITIONS = 2;

    private static final PostgreSQLContainer<?> POSTGRE_SQL = new PostgreSQLContainer<>("postgres:17-alpine");

    @BeforeAll
    public static void setup() {
        POSTGRE_SQL.start();
    }

    @AfterAll
    public static void cleanUpAll() {
        POSTGRE_SQL.stop();
    }

    @Transactional
    @AfterEach
    public void cleanUp() {
        announcementsRepository.deleteAll();
        listenerContainers.forEach(AbstractMessageListenerContainer::stop);
        listenerContainers.clear();
    }

    @DynamicPropertySource
    public static void configureDbProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRE_SQL::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRE_SQL::getUsername);
        registry.add("spring.datasource.password", POSTGRE_SQL::getPassword);
    }

    @Test
    @WithMockUser
    public void createAnnouncement() throws Exception {
        CreateAnnouncementDTO announcementDTO = CreateAnnouncementDTO.builder()
                .title("test")
                .description("test")
                .type("test")
                .price(100.0)
                .phoneNumber("test")
                .country("test")
                .city("test")
                .build();
        mockCurrentUser();
        String jsonBody = objectMapper.writeValueAsString(announcementDTO);
        MvcResult result = mvc.perform(post(DEFAULT_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andReturn();
        String jsonResponse = result.getResponse().getContentAsString();
        DataDTO<Long> response = objectMapper.readValue(jsonResponse, new TypeReference<>() {
        });
        assertTrue(announcementsRepository.findById(response.getData()).isPresent());
    }

    @Test
    @WithMockUser
    public void findAllAnnouncements() {
        when(securityUtil.isAuthenticated()).thenReturn(true);
        mockCurrentUser();
        List<Announcement> announcements = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Announcement announcement = TestUtil.getTestAnnouncement();
            announcement.setId(null);
            announcements.add(announcement);
        }
        testRepository.saveAllAnnouncements(announcements);
        testRepository.createImages(announcements);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("page", "0");
        params.add("count", "5");
        List<ResponseAnnouncementDTO> response1 = doFindAllRequest(DEFAULT_PATH, params);
        params.remove("page");
        params.add("page", "1");
        List<ResponseAnnouncementDTO> response2 = doFindAllRequest(DEFAULT_PATH, params);
        assertEquals(5, response1.size());
        assertEquals(5, response2.size());
        assertTrue(response1.getFirst().getId() > response2.getFirst().getId());
        response1.forEach(responseAnnouncementDTO -> assertNotNull(responseAnnouncementDTO.getImageId()));
        response2.forEach(responseAnnouncementDTO -> assertNotNull(responseAnnouncementDTO.getImageId()));
    }

    @Test
    @WithMockUser
    public void findAllAnnouncementsWhenCountryAndCityAreDifferent() {
        when(securityUtil.isAuthenticated()).thenReturn(true);
        mockCurrentUser();
        List<Announcement> announcements = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Announcement announcement = TestUtil.getTestAnnouncement();
            announcement.setId(null);
            announcements.add(announcement);
        }
        for (int i = 0; i < 5; i++) {
            Announcement announcement = TestUtil.getTestAnnouncement();
            announcement.setId(null);
            announcement.setCity("other city");
            announcement.setCountry("other country");
            announcements.add(announcement);
        }
        testRepository.saveAllAnnouncements(announcements);
        assertEquals(10, announcementsRepository.findAll().size());
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("page", "0");
        params.add("count", "10");
        List<ResponseAnnouncementDTO> response = doFindAllRequest(DEFAULT_PATH, params);
        User user = TestUtil.getTestUser();
        assertEquals(5, response.size());
        response.forEach(responseAnnouncementDTO -> {
            assertEquals(user.getCountry(), responseAnnouncementDTO.getCountry());
            assertEquals(user.getCity(), responseAnnouncementDTO.getCity());
        });
    }

    @Test
    @WithMockUser
    public void findAllAnnouncementsWithFilters() {
        when(securityUtil.isAuthenticated()).thenReturn(true);
        mockCurrentUser();
        List<String> filters = List.of("filter1", "filter2", "filter3", "filter4", "filter5");
        List<Announcement> announcements = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Announcement announcement = TestUtil.getTestAnnouncement();
            announcement.setId(null);
            announcement.setType(filters.get(i));
            announcements.add(announcement);
        }
        for (int i = 0; i < 5; i++) {
            Announcement announcement = TestUtil.getTestAnnouncement();
            announcement.setId(null);
            announcements.add(announcement);
        }
        testRepository.saveAllAnnouncements(announcements);
        assertEquals(10, announcementsRepository.findAll().size());
        StringBuilder sb = new StringBuilder();
        filters.forEach(filter -> sb.append(filter).append(","));
        sb.deleteCharAt(sb.length() - 1);
        String filtersStr = sb.toString();
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("page", "0");
        params.add("count", "10");
        params.add("filters", filtersStr);
        List<ResponseAnnouncementDTO> response = doFindAllRequest(DEFAULT_PATH, params);
        assertEquals(5, response.size());
        response.forEach(responseAnnouncementDTO -> assertTrue(filters.contains(responseAnnouncementDTO.getType())));
    }

    @Test
    @WithMockUser
    public void findAllAnnouncementsWithSort() {
        when(securityUtil.isAuthenticated()).thenReturn(true);
        mockCurrentUser();
        List<Announcement> announcements = new ArrayList<>();
        int likesCount = 5;
        for (int i = 0; i < 5; i++, likesCount--) {
            Announcement announcement = TestUtil.getTestAnnouncement();
            announcement.setId(null);
            announcement.setLikesCount(likesCount);
            announcements.add(announcement);
        }
        testRepository.saveAllAnnouncements(announcements);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("page", "0");
        params.add("count", "5");
        params.add("sort_type", SortType.LIKES.toString());
        List<ResponseAnnouncementDTO> descSortResponse = doFindAllRequest(DEFAULT_PATH, params);
        checkDescLikesSort(descSortResponse);
        params.add("sort_direction", Sort.Direction.ASC.toString());
        List<ResponseAnnouncementDTO> ascSortResponse = doFindAllRequest(DEFAULT_PATH, params);
        checkAscLikesSort(ascSortResponse);
    }

    @Test
    public void findAllAnnouncementsWhenUserNotAuthenticated() {
        when(securityUtil.isAuthenticated()).thenReturn(false);
        String country = "country";
        String city = "city";
        List<Announcement> announcements = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Announcement announcement = TestUtil.getTestAnnouncement();
            announcement.setId(null);
            announcement.setCountry(country);
            announcement.setCity(city);
            announcements.add(announcement);
        }
        for (int i = 0; i < 5; i++) {
            Announcement announcement = TestUtil.getTestAnnouncement();
            announcement.setId(null);
            announcements.add(announcement);
        }
        testRepository.saveAllAnnouncements(announcements);
        assertEquals(10, announcementsRepository.findAll().size());
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("page", "0");
        params.add("count", "10");
        params.add("country", country);
        params.add("city", city);
        List<ResponseAnnouncementDTO> response = doFindAllRequest(DEFAULT_PATH, params);
        assertEquals(5, response.size());
        response.forEach(responseAnnouncementDTO -> {
            assertEquals(country, responseAnnouncementDTO.getCountry());
            assertEquals(city, responseAnnouncementDTO.getCity());
        });
    }

    @Test
    @WithMockUser
    public void findAllAnnouncementsByTitle() {
        when(securityUtil.isAuthenticated()).thenReturn(true);
        mockCurrentUser();
        String title = "title";
        List<Announcement> announcements = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Announcement announcement = TestUtil.getTestAnnouncement();
            announcement.setId(null);
            announcements.add(announcement);
        }
        for (int i = 0; i < 5; i++) {
            Announcement announcement = TestUtil.getTestAnnouncement();
            announcement.setId(null);
            announcement.setTitle(title);
            announcements.add(announcement);
        }
        testRepository.saveAllAnnouncements(announcements);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("page", "0");
        params.add("count", "10");
        params.add("title", title);
        List<ResponseAnnouncementDTO> response = doFindAllRequest(DEFAULT_PATH + "/find", params);
        assertEquals(5, response.size());
        response.forEach(responseAnnouncementDTO -> assertEquals(title, responseAnnouncementDTO.getTitle()));
    }

    @Test
    @WithMockUser
    public void findAllAnnouncementsByTitleWhenCountryAndCityAreDifferent() {
        when(securityUtil.isAuthenticated()).thenReturn(true);
        mockCurrentUser();
        List<Announcement> announcements = new ArrayList<>();
        String title = "title";
        for (int i = 0; i < 5; i++) {
            Announcement announcement = TestUtil.getTestAnnouncement();
            announcement.setId(null);
            announcements.add(announcement);
        }
        for (int i = 0; i < 5; i++) {
            Announcement announcement = TestUtil.getTestAnnouncement();
            announcement.setId(null);
            announcement.setTitle(title);
            announcements.add(announcement);
        }
        for (int i = 0; i < 5; i++) {
            Announcement announcement = TestUtil.getTestAnnouncement();
            announcement.setId(null);
            announcement.setCity("other city");
            announcement.setCountry("other country");
            announcement.setTitle(title);
            announcements.add(announcement);
        }
        testRepository.saveAllAnnouncements(announcements);
        assertEquals(15, announcementsRepository.findAll().size());
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("page", "0");
        params.add("count", "15");
        params.add("title", title);
        List<ResponseAnnouncementDTO> response = doFindAllRequest(DEFAULT_PATH + "/find", params);
        User user = TestUtil.getTestUser();
        assertEquals(5, response.size());
        response.forEach(responseAnnouncementDTO -> {
            assertEquals(user.getCountry(), responseAnnouncementDTO.getCountry());
            assertEquals(user.getCity(), responseAnnouncementDTO.getCity());
            assertEquals(title, responseAnnouncementDTO.getTitle());
        });
    }

    @Test
    @WithMockUser
    public void findAllAnnouncementsByTitleWithFilters() {
        when(securityUtil.isAuthenticated()).thenReturn(true);
        mockCurrentUser();
        List<String> filters = List.of("filter1", "filter2", "filter3", "filter4", "filter5");
        String title = "title";
        List<Announcement> announcements = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Announcement announcement = TestUtil.getTestAnnouncement();
            announcement.setId(null);
            announcement.setType(filters.get(i));
            announcement.setTitle(title);
            announcements.add(announcement);
        }
        for (int i = 0; i < 5; i++) {
            Announcement announcement = TestUtil.getTestAnnouncement();
            announcement.setId(null);
            announcement.setTitle(title);
            announcements.add(announcement);
        }
        for (int i = 0; i < 5; i++) {
            Announcement announcement = TestUtil.getTestAnnouncement();
            announcement.setId(null);
            announcement.setType(filters.get(i));
            announcements.add(announcement);
        }
        testRepository.saveAllAnnouncements(announcements);
        assertEquals(15, announcementsRepository.findAll().size());
        StringBuilder sb = new StringBuilder();
        filters.forEach(filter -> sb.append(filter).append(","));
        sb.deleteCharAt(sb.length() - 1);
        String filtersStr = sb.toString();
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("page", "0");
        params.add("count", "15");
        params.add("title", title);
        params.add("filters", filtersStr);
        List<ResponseAnnouncementDTO> response = doFindAllRequest(DEFAULT_PATH + "/find", params);
        assertEquals(5, response.size());
        response.forEach(responseAnnouncementDTO -> {
            assertEquals(title, responseAnnouncementDTO.getTitle());
            assertTrue(filters.contains(responseAnnouncementDTO.getType()));
        });
    }

    @Test
    @WithMockUser
    public void findAllAnnouncementsByTitleWithSort() {
        when(securityUtil.isAuthenticated()).thenReturn(true);
        mockCurrentUser();
        List<Announcement> announcements = new ArrayList<>();
        int likesCount = 5;
        String title = "tile";
        for (int i = 0; i < 5; i++, likesCount--) {
            Announcement announcement = TestUtil.getTestAnnouncement();
            announcement.setId(null);
            announcement.setLikesCount(likesCount);
            announcement.setTitle(title);
            announcements.add(announcement);
        }
        likesCount = 5;
        for (int i = 0; i < 5; i++, likesCount--) {
            Announcement announcement = TestUtil.getTestAnnouncement();
            announcement.setId(null);
            announcement.setLikesCount(likesCount);
            announcements.add(announcement);
        }
        testRepository.saveAllAnnouncements(announcements);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("page", "0");
        params.add("count", "10");
        params.add("title", title);
        params.add("sort_type", SortType.LIKES.toString());
        List<ResponseAnnouncementDTO> descSortResponse = doFindAllRequest(DEFAULT_PATH + "/find", params);
        checkDescLikesSort(descSortResponse);
        params.add("sort_direction", Sort.Direction.ASC.toString());
        List<ResponseAnnouncementDTO> ascSortResponse = doFindAllRequest(DEFAULT_PATH, params);
        checkAscLikesSort(ascSortResponse);
    }

    @Test
    public void findAllAnnouncementsByTitleWhenUserNotAuthenticated() {
        when(securityUtil.isAuthenticated()).thenReturn(false);
        String title = "title";
        String country = "country";
        String city = "city";
        List<Announcement> announcements = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Announcement announcement = TestUtil.getTestAnnouncement();
            announcement.setId(null);
            announcement.setCountry(country);
            announcement.setCity(city);
            announcement.setTitle(title);
            announcements.add(announcement);
        }
        for (int i = 0; i < 5; i++) {
            Announcement announcement = TestUtil.getTestAnnouncement();
            announcement.setId(null);
            announcement.setCountry(country);
            announcement.setCity(city);
            announcements.add(announcement);
        }
        for (int i = 0; i < 5; i++) {
            Announcement announcement = TestUtil.getTestAnnouncement();
            announcement.setId(null);
            announcement.setTitle(title);
            announcements.add(announcement);
        }
        testRepository.saveAllAnnouncements(announcements);
        assertEquals(15, announcementsRepository.findAll().size());
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("page", "0");
        params.add("count", "15");
        params.add("title", title);
        params.add("country", country);
        params.add("city", city);
        List<ResponseAnnouncementDTO> response = doFindAllRequest(DEFAULT_PATH + "/find", params);
        assertEquals(5, response.size());
        response.forEach(responseAnnouncementDTO -> {
            assertEquals(title, responseAnnouncementDTO.getTitle());
            assertEquals(country, responseAnnouncementDTO.getCountry());
            assertEquals(city, responseAnnouncementDTO.getCity());
        });
    }

    @Test
    @WithMockUser
    public void findAllUsersAnnouncements() {
        mockCurrentUser();
        List<Announcement> announcements = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Announcement announcement = TestUtil.getTestAnnouncement();
            announcement.setId(null);
            announcements.add(announcement);
        }
        Long id = 2L;
        for (int i = 0; i < 5; i++) {
            Announcement announcement = TestUtil.getTestAnnouncement();
            announcement.setId(null);
            announcement.setOwnerId(id);
            announcements.add(announcement);
        }
        testRepository.saveAllAnnouncements(announcements);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("page", "0");
        params.add("count", "10");
        List<ResponseAnnouncementDTO> response = doFindAllRequest(DEFAULT_PATH + "/user/" + id, params);
        assertEquals(5, response.size());
        response.forEach(responseAnnouncementDTO -> assertEquals(2L, responseAnnouncementDTO.getOwnerId()));
    }

    @Test
    @WithMockUser
    public void showAnnouncement() throws Exception {
        mockCurrentUser();
        Announcement announcement = TestUtil.getTestAnnouncement();
        announcement.setId(null);
        Long id = testRepository.saveAnnouncement(announcement);
        MvcResult result = mvc.perform(get(DEFAULT_PATH + "/" + id))
                .andExpect(status().isOk())
                .andReturn();
        String jsonResponse = result.getResponse().getContentAsString();
        ShowAnnouncementDTO showDTO = objectMapper.readValue(jsonResponse, ShowAnnouncementDTO.class);
        assertEquals(id, showDTO.getId());
    }

    @Test
    @WithMockUser
    public void addLike() throws Exception {
        mockCurrentUser();
        Announcement announcement = TestUtil.getTestAnnouncement();
        announcement.setId(null);
        Long id = testRepository.saveAnnouncement(announcement);
        mvc.perform(post(DEFAULT_PATH + "/" + id + "/like"))
                .andExpect(status().isCreated());
        announcement = announcementsRepository.findById(id).get();
        assertEquals(1, announcement.getLikesCount());
    }

    @Test
    @WithMockUser
    public void deleteLike() throws Exception {
        mockCurrentUser();
        Announcement announcement = TestUtil.getTestAnnouncement();
        announcement.setId(null);
        Long announcementId = testRepository.saveAnnouncement(announcement);
        Long likeId = testRepository.createLike(announcement, TestUtil.getTestUser().getId());
        assertTrue(likesRepository.findById(likeId).isPresent());
        mvc.perform(delete(DEFAULT_PATH + "/" + announcementId + "/like"))
                .andExpect(status().isOk());
        assertTrue(likesRepository.findById(likeId).isEmpty());
    }

    @Test
    @WithMockUser(roles = {"USER", "ADMIN"})
    public void getReports() throws Exception {
        Announcement announcement = TestUtil.getTestAnnouncement();
        announcement.setId(null);
        testRepository.saveAnnouncement(announcement);
        Long reportId = testRepository.createReport(announcement, "test", TestUtil.getTestUser().getId());
        MvcResult result = mvc.perform(get(DEFAULT_PATH + "/reports")
                        .queryParam("page", "0")
                        .queryParam("count", "1"))
                .andExpect(status().isOk())
                .andReturn();
        String jsonResponse = result.getResponse().getContentAsString();
        List<ResponseReportDTO> response = objectMapper.readValue(jsonResponse, new TypeReference<>() {
        });
        assertFalse(response.isEmpty());
        assertEquals(reportId, response.getLast().getId());
    }

    @Test
    @WithMockUser
    public void getReportsWhenUserNotAdmin() throws Exception {
        mvc.perform(get(DEFAULT_PATH + "/reports")
                        .queryParam("page", "0")
                        .queryParam("count", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"USER", "ADMIN"})
    public void showReport() throws Exception {
        Announcement announcement = TestUtil.getTestAnnouncement();
        announcement.setId(null);
        Long announcementId = testRepository.saveAnnouncement(announcement);
        Long reportId = testRepository.createReport(announcement, "test", TestUtil.getTestUser().getId());
        MvcResult result = mvc.perform(get(DEFAULT_PATH + "/report/" + reportId))
                .andExpect(status().isOk())
                .andReturn();
        String jsonResponse = result.getResponse().getContentAsString();
        ShowReportDTO reportDTO = objectMapper.readValue(jsonResponse, ShowReportDTO.class);
        assertEquals(reportId, reportDTO.getId());
        assertEquals(announcementId, reportDTO.getAnnouncement().getId());
    }

    @Test
    @WithMockUser
    public void showReportWhenUserNotAdmin() throws Exception {
        mvc.perform(get(DEFAULT_PATH + "/report/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    public void createReport() throws Exception {
        mockCurrentUser();
        Announcement announcement = TestUtil.getTestAnnouncement();
        announcement.setId(null);
        Long announcementId = testRepository.saveAnnouncement(announcement);
        CauseDTO causeDTO = new CauseDTO("test");
        String jsonBody = objectMapper.writeValueAsString(causeDTO);
        mvc.perform(post(DEFAULT_PATH + "/" + announcementId + "/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated());
        User user = TestUtil.getTestUser();
        Optional<Report> report = reportsRepository.findByAnnouncementAndSenderId(announcement, user.getId());
        assertTrue(report.isPresent());
    }

    @Test
    @WithMockUser(roles = {"USER", "ADMIN"})
    public void closeReport() throws Exception {
        Announcement announcement = TestUtil.getTestAnnouncement();
        announcement.setId(null);
        testRepository.saveAnnouncement(announcement);
        Long reportId = testRepository.createReport(announcement, "test", TestUtil.getTestUser().getId());
        mvc.perform(delete(DEFAULT_PATH + "/report/" + reportId))
                .andExpect(status().isOk());
        assertTrue(reportsRepository.findById(reportId).isEmpty());
    }

    @Test
    @WithMockUser
    public void closeReportWhenUserNotAdmin() throws Exception {
        mvc.perform(delete(DEFAULT_PATH + "/report/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    public void updateAnnouncement() throws Exception {
        mockCurrentUser();
        Announcement announcement = TestUtil.getTestAnnouncement();
        announcement.setId(null);
        Long id = testRepository.saveAnnouncement(announcement);
        UpdateAnnouncementDTO updateAnnouncementDTO = UpdateAnnouncementDTO.builder()
                .title("new_title")
                .description("new_description")
                .type("new_type")
                .build();
        String jsonBody = objectMapper.writeValueAsString(updateAnnouncementDTO);
        mvc.perform(patch(DEFAULT_PATH + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk());
        Announcement updatedAnnouncement = announcementsRepository.findById(id).get();
        assertEquals(updateAnnouncementDTO.getTitle(), updatedAnnouncement.getTitle());
        assertEquals(updateAnnouncementDTO.getDescription(), updatedAnnouncement.getDescription());
        assertEquals(updateAnnouncementDTO.getType(), updatedAnnouncement.getType());
    }

    @Test
    @WithMockUser
    public void addImage() throws Exception {
        mockCurrentUser();
        Announcement announcement = TestUtil.getTestAnnouncement();
        announcement.setId(null);
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                new byte[0]
        );
        Long id = testRepository.saveAnnouncement(announcement);
        MvcResult result = mvc.perform(multipart(DEFAULT_PATH + "/" + id + "/image")
                        .file(file)
                        .content(file.getBytes()))
                .andExpect(status().isCreated())
                .andReturn();
        String jsonResponse = result.getResponse().getContentAsString();
        DataDTO<Long> response = objectMapper.readValue(jsonResponse, new TypeReference<>() {
        });
        Optional<Image> image = imagesRepository.findById(response.getData());
        assertTrue(image.isPresent());
    }

    @Test
    @WithMockUser
    public void getImage() throws Exception {
        mockCurrentUser();
        Announcement announcement = TestUtil.getTestAnnouncement();
        announcement.setId(null);
        testRepository.saveAnnouncement(announcement);
        Long imageId = testRepository.createImage(announcement);
        byte[] imageBody = new byte[0];
        when(filesAPI.downloadImage(any(), any())).thenReturn(imageBody);
        MvcResult result = mvc.perform(get(DEFAULT_PATH + "/image/" + imageId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andReturn();
        byte[] response = result.getResponse().getContentAsByteArray();
        assertNotEquals(imageBody, response);
    }

    @Test
    @WithMockUser
    public void deleteImage() throws Exception {
        mockCurrentUser();
        Announcement announcement = TestUtil.getTestAnnouncement();
        announcement.setId(null);
        testRepository.saveAnnouncement(announcement);
        Long id = testRepository.createImage(announcement);
        assertTrue(imagesRepository.findById(id).isPresent());
        mvc.perform(delete(DEFAULT_PATH + "/image/" + id))
                .andExpect(status().isOk());
        assertTrue(imagesRepository.findById(id).isEmpty());
    }

    @Test
    @WithMockUser(roles = {"USER", "ADMIN"})
    public void banAnnouncement() throws Exception {
        configureEmailMessagesConsumer();
        configureDeletedAnnouncementConsumer();
        mockCurrentUser();
        User user = TestUtil.getTestUser();
        when(usersAPI.getUserEmail(eq(user.getId()), any())).thenReturn(Map.of("data", user.getEmail()));
        Announcement announcement = TestUtil.getTestAnnouncement();
        announcement.setId(null);
        Long id = testRepository.saveAnnouncement(announcement);
        assertTrue(announcementsRepository.findById(id).isPresent());
        IntStream.range(0, 5).forEach(i -> testRepository.createImage(announcement));
        List<Image> images = imagesRepository.findAll();
        assertFalse(images.isEmpty());
        CauseDTO causeDTO = new CauseDTO("test");
        String jsonBody = objectMapper.writeValueAsString(causeDTO);
        mvc.perform(delete(DEFAULT_PATH + "/" + id + "/ban")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk());
        assertTrue(announcementsRepository.findById(id).isEmpty());
        EmailMessageDTO emailMessage = emailMessagesQueue.poll(5, TimeUnit.SECONDS);
        assertNotNull(emailMessage);
        assertNotNull(emailMessage.getEmail());
        assertNotNull(emailMessage.getMessage());
        List<String> deletedImages = deletedImagesQueue.poll(5, TimeUnit.SECONDS);
        assertNotNull(deletedImages);
        assertEquals(images.size(), deletedImages.size());
    }

    @Test
    @WithMockUser
    public void banAnnouncementWhenUserNotAdmin() throws Exception {
        CauseDTO causeDTO = new CauseDTO("test");
        String jsonBody = objectMapper.writeValueAsString(causeDTO);
        mvc.perform(delete(DEFAULT_PATH + "/1/ban")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    public void deleteAnnouncement() throws Exception {
        mockCurrentUser();
        configureDeletedAnnouncementConsumer();
        Announcement announcement = TestUtil.getTestAnnouncement();
        announcement.setId(null);
        Long id = testRepository.saveAnnouncement(announcement);
        assertTrue(announcementsRepository.findById(id).isPresent());
        IntStream.range(0, 5).forEach(i -> testRepository.createImage(announcement));
        List<Image> images = imagesRepository.findAll();
        assertFalse(images.isEmpty());
        mvc.perform(delete(DEFAULT_PATH + "/" + id))
                .andExpect(status().isOk());
        assertTrue(announcementsRepository.findById(id).isEmpty());
        List<String> deletedImages = deletedImagesQueue.poll(5, TimeUnit.SECONDS);
        assertNotNull(deletedImages);
        assertEquals(images.size(), deletedImages.size());
    }

    @Test
    public void deletedUserListener() throws InterruptedException {
        mockCurrentUser();
        User user = TestUtil.getTestUser();
        List<Announcement> usersAnnouncements = new ArrayList<>();
        List<Announcement> announcements = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Announcement announcement = TestUtil.getTestAnnouncement();
            announcement.setId(null);
            usersAnnouncements.add(announcement);
        }
        for (int i = 0; i < 5; i++) {
            Announcement announcement = TestUtil.getTestAnnouncement();
            announcement.setId(null);
            announcement.setOwnerId(user.getId() + 1);
            announcements.add(announcement);
        }
        testRepository.saveAllAnnouncements(announcements);
        testRepository.saveAllAnnouncements(usersAnnouncements);
        assertEquals(5, announcementsRepository.countByOwnerId(user.getId()));
        assertEquals(10, announcementsRepository.findAll().size());
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, consumerBootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        ProducerFactory<String, Long> factory = new DefaultKafkaProducerFactory<>(
                props,
                new StringSerializer(),
                new LongSerializer()
        );
        KafkaTemplate<String, Long> kafkaTemplate = new KafkaTemplate<>(factory);
        kafkaTemplate.send(deletedUserTopic, user.getId());
        AnnouncementsServiceImpl.getDELETED_USER_LATCH().await();
        List<Announcement> savedAnnouncements = announcementsRepository.findAll();
        assertEquals(0, announcementsRepository.countByOwnerId(user.getId()));
        assertEquals(5, savedAnnouncements.size());
        savedAnnouncements.forEach(announcement -> assertNotEquals(user.getId(), announcement.getOwnerId()));
    }

    @Test
    public void deleteExpiredAnnouncements() throws Exception {
        List<Announcement> announcements = new ArrayList<>();
        LocalDateTime time = LocalDateTime.now();
        for (int i = 0; i < 5; i++) {
            Announcement announcement = TestUtil.getTestAnnouncement();
            announcement.setId(null);
            announcement.setCreatedAt(time);
            announcements.add(announcement);
        }
        LocalDateTime expiredTime = LocalDateTime.now().minusDays(announcementsStorageDays + 1);
        for (int i = 0; i < 5; i++) {
            Announcement announcement = TestUtil.getTestAnnouncement();
            announcement.setId(null);
            announcement.setCreatedAt(expiredTime);
            announcements.add(announcement);
        }
        testRepository.saveAllAnnouncements(announcements);
        assertEquals(10, announcementsRepository.findAll().size());
        configureEmailMessagesConsumer();
        User user = TestUtil.getTestUser();
        when(usersAPI.getUserEmail(eq(user.getId()), any())).thenReturn(Map.of("data", user.getEmail()));
        mvc.perform(delete(DEFAULT_PATH + "/expired")
                        .queryParam("access_key", accessKey))
                .andExpect(status().isOk());
        AnnouncementsServiceImpl.getDELETE_EXPIRED_ANNOUNCEMENTS_LATCH().await();
        List<Announcement> remaining = announcementsRepository.findAll();
        assertEquals(5, remaining.size());
        remaining.forEach(announcement -> assertEquals(time.toLocalDate(), announcement.getCreatedAt().toLocalDate()));
        while (!emailMessagesQueue.isEmpty()) {
            EmailMessageDTO emailMessage = emailMessagesQueue.poll(5, TimeUnit.SECONDS);
            assertNotNull(emailMessage);
            assertNotNull(emailMessage.getEmail());
            assertNotNull(emailMessage.getMessage());
        }
    }

    @Test
    public void deleteExpiredAnnouncementsWhenAccessKeyIsInvalid() throws Exception {
        mvc.perform(delete(DEFAULT_PATH + "/expired")
                        .queryParam("access_key", "invalid-key"))
                .andExpect(status().isForbidden());
    }

    @SneakyThrows
    private List<ResponseAnnouncementDTO> doFindAllRequest(String path, MultiValueMap<String, String> params) {
        MvcResult result = mvc.perform(get(path)
                        .queryParams(params))
                .andExpect(status().isOk())
                .andReturn();
        String jsonResponse = result.getResponse().getContentAsString();
        return objectMapper.readValue(jsonResponse, new TypeReference<>() {
        });
    }

    private void checkDescLikesSort(List<ResponseAnnouncementDTO> announcements) {
        boolean isSorted = true;
        for (int i = 0; i < announcements.size() - 1; i++) {
            if (announcements.get(i).getLikesCount() < announcements.get(i + 1).getLikesCount()) {
                isSorted = false;
                break;
            }
        }
        assertTrue(isSorted);
    }

    private void checkAscLikesSort(List<ResponseAnnouncementDTO> announcements) {
        boolean isSorted = true;
        for (int i = 0; i < announcements.size() - 1; i++) {
            if (announcements.get(i).getLikesCount() > announcements.get(i + 1).getLikesCount()) {
                isSorted = false;
                break;
            }
        }
        assertTrue(isSorted);
    }

    private void configureEmailMessagesConsumer() {
        ConsumerFactory<String, EmailMessageDTO> consumerFactory = new DefaultKafkaConsumerFactory<>(
                getConsumerProps(),
                new StringDeserializer(),
                new JsonDeserializer<>(EmailMessageDTO.class)
        );
        ContainerProperties containerProperties = new ContainerProperties(emailMessageTopic);
        KafkaMessageListenerContainer<String, EmailMessageDTO> listenerContainer =
                new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        listenerContainer.setupMessageListener(
                (MessageListener<String, EmailMessageDTO>) data -> emailMessagesQueue.add(data.value()));
        listenerContainer.setBeanName("testEmailMessageListener");
        listenerContainer.start();
        ContainerTestUtils.waitForAssignment(listenerContainer, PARTITIONS);
        listenerContainers.add(listenerContainer);
    }

    private void configureDeletedAnnouncementConsumer() {
        ConsumerFactory<String, List<String>> consumerFactory = new DefaultKafkaConsumerFactory<>(
                getConsumerProps(),
                new StringDeserializer(),
                new JsonDeserializer<>(new TypeReference<>() {
                })
        );
        ContainerProperties containerProperties = new ContainerProperties(deletedAnnouncementTopic);
        KafkaMessageListenerContainer<String, List<String>> listenerContainer =
                new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        listenerContainer.setupMessageListener(
                (MessageListener<String, List<String>>) data -> deletedImagesQueue.add(data.value()));
        listenerContainer.setBeanName("testDeletedAnnouncementListener");
        listenerContainer.start();
        ContainerTestUtils.waitForAssignment(listenerContainer, PARTITIONS);
        listenerContainers.add(listenerContainer);
    }

    private Map<String, Object> getConsumerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, consumerBootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, "false");
        return props;
    }

    private void mockCurrentUser() {
        when(securityUtil.getCurrentUser()).thenReturn(TestUtil.getTestUser());
    }

    @Transactional
    private class TestRepository {

        public Long saveAnnouncement(Announcement announcement) {
            Announcement saved = announcementsRepository.save(announcement);
            return saved.getId();
        }

        public void saveAllAnnouncements(List<Announcement> announcements) {
            announcementsRepository.saveAll(announcements);
        }

        private Long createImage(Announcement announcement) {
            Image image = imagesRepository.save(new Image(UUID.randomUUID() + ".jpg", announcement));
            return image.getId();
        }

        public void createImages(List<Announcement> announcements) {
            List<Image> images = announcements.stream()
                    .map(announcement -> new Image(UUID.randomUUID() + ".jpg", announcement))
                    .toList();
            imagesRepository.saveAll(images);
        }

        public Long createLike(Announcement announcement, Long ownerId) {
            Like like = likesRepository.save(new Like(announcement, ownerId));
            return like.getId();
        }

        public Long createReport(Announcement announcement, String cause, Long senderId) {
            Report report = Report.builder()
                    .announcement(announcement)
                    .cause(cause)
                    .senderId(senderId)
                    .build();
            return reportsRepository.save(report).getId();
        }

    }

}
