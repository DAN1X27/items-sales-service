package danix.app.files_service.services;

import danix.app.files_service.util.FileType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface FilesService {

    void upload(FileType type, MultipartFile file, String fileName);

    ResponseEntity<?> download(FileType type, String fileName);

    void delete(FileType type, String fileName);

}
