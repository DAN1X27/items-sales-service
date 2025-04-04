package danix.app.announcements_service.controllers;

import danix.app.announcements_service.dto.*;
import danix.app.announcements_service.services.AnnouncementService;
import danix.app.announcements_service.util.AnnouncementException;
import danix.app.announcements_service.util.ErrorResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/announcements")
@RequiredArgsConstructor
@Slf4j
public class AnnouncementsController {
    private final AnnouncementService announcementService;

    @GetMapping
    public ResponseEntity<List<ResponseDTO>> findAll(@RequestParam int page, @RequestParam int count,
                                                     @RequestParam(defaultValue = "USD") String currency,
                                                     @RequestParam(required = false) List<String> filters) {
        return new ResponseEntity<>(announcementService.findAll(page, count, currency, filters), HttpStatus.OK);
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<List<ResponseDTO>> getAllByUser(@PathVariable Long id,
                                                          @RequestParam(defaultValue = "USD") String currency) {
        return new ResponseEntity<>(announcementService.findAllByUser(id, currency), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShowDTO> show(@PathVariable Long id, @RequestParam(defaultValue = "USD") String currency) {
        return new ResponseEntity<>(announcementService.show(id, currency), HttpStatus.OK);
    }

    @GetMapping("/find")
    public ResponseEntity<List<ResponseDTO>> findAnnouncements(@RequestParam String title,
                                                               @RequestParam(defaultValue = "USD") String currency,
                                                               @RequestParam(required = false) List<String> filters,
                                                               @RequestParam int page, @RequestParam int count) {
        return new ResponseEntity<>(announcementService.findByTitle(page, count ,title, currency, filters), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<HttpStatus> create(@RequestParam(defaultValue = "USD") String currency,
                                             @RequestBody @Valid CreateDTO createDTO,
                                             BindingResult bindingResult) {
        handleRequestExceptions(bindingResult);
        announcementService.save(createDTO, currency);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<HttpStatus> like(@PathVariable Long id) {
        announcementService.like(id);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}/like")
    public ResponseEntity<HttpStatus> deleteLike(@PathVariable Long id) {
        announcementService.deleteLike(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/reports")
    public ResponseEntity<List<ResponseReportDTO>> getReports(@RequestParam int page, @RequestParam int count,
                                                              @RequestParam(defaultValue = "USD") String currency) {
        return new ResponseEntity<>(announcementService.getReports(page, count, currency), HttpStatus.OK);
    }

    @DeleteMapping("/report/{id}")
    public ResponseEntity<HttpStatus> closeReport(@PathVariable Long id) {
        announcementService.closeReport(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/{id}/admin")
    public ResponseEntity<HttpStatus> deleteByAdmin(@PathVariable Long id) {
        announcementService.deleteByAdmin(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/{id}/report")
    public ResponseEntity<HttpStatus> report(@PathVariable Long id, @RequestParam String cause) {
        announcementService.report(id, cause);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<HttpStatus> update(@PathVariable Long id, @RequestBody UpdateDTO updateDTO) {
        announcementService.update(id, updateDTO);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/image/{id}")
    public ResponseEntity<?> downloadImage(@PathVariable Long id) {
        Map<String, Object> data = announcementService.downloadImage(id);
        byte[] bytes = Base64.getDecoder().decode((String) data.get("data"));
        MediaType mediaType = MediaType.parseMediaType((String) data.get("mediaType"));
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(mediaType)
                .body(bytes);
    }

    @PostMapping("/{id}/image")
    public ResponseEntity<HttpStatus> addImage(@PathVariable Long id, @RequestParam MultipartFile image) {
        announcementService.addImage(image, id);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @DeleteMapping("/image/{id}")
    public ResponseEntity<HttpStatus> deleteImage(@PathVariable Long id) {
        announcementService.deleteImage(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleException(AnnouncementException e) {
        return new ResponseEntity<>(new ErrorResponse(e.getMessage(), LocalDateTime.now()), HttpStatus.BAD_REQUEST);
    }

    private void handleRequestExceptions(BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            StringBuilder message = new StringBuilder();
            bindingResult.getFieldErrors().forEach(error ->
                    message.append(error.getDefaultMessage()).append("; "));
            throw new AnnouncementException(message.toString());
        }
    }
}