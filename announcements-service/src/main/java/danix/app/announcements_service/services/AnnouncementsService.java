package danix.app.announcements_service.services;

import danix.app.announcements_service.dto.*;
import danix.app.announcements_service.util.CurrencyCode;
import danix.app.announcements_service.util.SortData;
import org.springframework.data.domain.Sort;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AnnouncementsService {

    List<ResponseAnnouncementDTO> findAll(int page, int count, CurrencyCode currency, List<String> filters, SortData sortData,
                                          String country, String city);

    List<ResponseAnnouncementDTO> findByTitle(int page, int count, String title, CurrencyCode currency, List<String> filters,
                                              SortData sortData, String country, String city);

    List<ResponseAnnouncementDTO> findAllByUser(Long id, CurrencyCode currency, int page, int count);

    DataDTO<Long> save(CreateAnnouncementDTO createDTO, CurrencyCode currency);

    byte[] downloadImage(Long id);

    void addImage(MultipartFile image, Long id);

    void deleteImage(Long id);

    void addLike(Long id);

    void deleteLike(Long id);

    ShowAnnouncementDTO show(Long id, CurrencyCode currency);

    void createReport(Long id, String cause);

    void closeReport(Long id);

    List<ResponseReportDTO> getAllReports(int page, int count, Sort.Direction direction);

    ShowReportDTO showReport(long id, CurrencyCode currency);

    void delete(Long id);

    void ban(Long id, String cause);

    void update(Long id, UpdateAnnouncementDTO updateDTO);

    void deleteExpired();
}
