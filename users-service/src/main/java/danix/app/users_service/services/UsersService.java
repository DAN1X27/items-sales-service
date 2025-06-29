package danix.app.users_service.services;

import danix.app.users_service.dto.*;
import danix.app.users_service.models.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface UsersService {

    User getById(Long id);

    ResponseUserDTO show(Long id);

    UserInfoDTO getInfo();

    List<ResponseCommentDTO> getUserComments(long id, int page, int count);

    void temporalRegistration(RegistrationDTO registrationDTO);

    DataDTO<Long> registrationConfirm(String email);

    void updateInfo(UpdateInfoDTO updateInfoDTO);

    void updateEmail(String email);

    void deleteTempUser(String email);

    void updateAvatar(MultipartFile image);

    ResponseEntity<?> getAvatar(Long id);

    void deleteAvatar();

    void addComment(Long id, String text);

    void deleteComment(Long id);

    void addGrade(Long id, int stars);

    List<ResponseReportDTO> getReports(int page, int count);

    void createReport(Long id, String cause);

    void deleteReport(Integer id);

    List<ResponseBannedUserDTO> getBannedUsers(int page, int count);

    void banUser(Long id, String cause);

    Map<String, Object> isBanned(String username);

    void unbanUser(Long id);

    void delete();

    void delete(Long id);

    void blockUser(Long id);

    void unblockUser(Long id);

    List<DataDTO<Long>> getBlockedUsers(int page, int count);

    DataDTO<Boolean> isBlockedByUser(Long id);

}
