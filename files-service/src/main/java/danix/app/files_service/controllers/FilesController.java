package danix.app.files_service.controllers;

import danix.app.files_service.dto.ResponseFileDTO;
import danix.app.files_service.services.FilesService;
import danix.app.files_service.util.ErrorResponse;
import danix.app.files_service.util.FileException;
import danix.app.files_service.util.FileType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
public class FilesController {
    private final FilesService filesService;

    @PutMapping("/user/avatar")
    public ResponseEntity<HttpStatus> updateUserAvatar(@RequestParam MultipartFile image, @RequestParam String fileName) {
        filesService.upload(FileType.USER_AVATAR, image, fileName);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/user/avatar")
    public ResponseEntity<ResponseFileDTO> downloadUserAvatar(@RequestParam String fileName) {
        return new ResponseEntity<>(filesService.download(FileType.USER_AVATAR, fileName), HttpStatus.OK);
    }

    @DeleteMapping("/user/avatar")
    public ResponseEntity<HttpStatus> deleteUserAvatar(@RequestParam String fileName) {
        filesService.delete(FileType.USER_AVATAR, fileName);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/announcement/image")
    public ResponseEntity<HttpStatus> addAnnouncementImage(@RequestParam MultipartFile image, @RequestParam String fileName) {
        filesService.upload(FileType.ANNOUNCEMENT_IMAGE, image, fileName);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping("/announcement/image")
    public ResponseEntity<ResponseFileDTO> downloadAnnouncementImage(@RequestParam String fileName) {
        return new ResponseEntity<>(filesService.download(FileType.ANNOUNCEMENT_IMAGE, fileName), HttpStatus.OK);
    }

    @DeleteMapping("/announcement/image")
    public ResponseEntity<HttpStatus> deleteAnnouncementImage(@RequestParam String fileName) {
        filesService.delete(FileType.ANNOUNCEMENT_IMAGE, fileName);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/chat/image")
    public ResponseEntity<ResponseFileDTO> downloadMessageImage(@RequestParam String fileName) {
        return new ResponseEntity<>(filesService.download(FileType.CHAT_IMAGE, fileName), HttpStatus.OK);
    }

    @PostMapping("/chat/image")
    public ResponseEntity<HttpStatus> saveMessageImage(@RequestParam MultipartFile image, @RequestParam String fileName) {
        filesService.upload(FileType.CHAT_IMAGE, image, fileName);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @DeleteMapping("/chat/image")
    public ResponseEntity<HttpStatus> deleteMessageImage(@RequestParam String fileName) {
        filesService.delete(FileType.CHAT_IMAGE, fileName);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleException(FileException e) {
        return new ResponseEntity<>(new ErrorResponse(e.getMessage(), LocalDateTime.now()), HttpStatus.BAD_REQUEST);
    }
}
