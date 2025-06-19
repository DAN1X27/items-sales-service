package danix.app.announcements_service.controllers;

import danix.app.announcements_service.dto.*;
import danix.app.announcements_service.security.User;
import danix.app.announcements_service.services.AnnouncementsService;
import danix.app.announcements_service.util.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static danix.app.announcements_service.security.UserDetailsServiceImpl.getCurrentUser;
import static danix.app.announcements_service.security.UserDetailsServiceImpl.isAuthenticated;

@Slf4j
@RestController
@Tag(name = "Announcements API")
@RequestMapping("/announcements")
@RequiredArgsConstructor
public class AnnouncementsController {

    private final AnnouncementsService announcementsService;

    @GetMapping
    public ResponseEntity<List<ResponseAnnouncementDTO>> findAll(@RequestParam int page, @RequestParam int count,
               @RequestParam(defaultValue = "USD") CurrencyCode currency, @RequestParam(required = false) String city,
               @RequestParam(required = false) String country, @RequestParam(required = false) List<String> filters,
               @RequestParam(defaultValue = "ID", name = "sort_type") SortType sortType,
               @RequestParam(defaultValue = "DESC", name = "sort_direction") Sort.Direction sortDirection) {
        SortData sortData = new SortData(sortType, sortDirection);
        if (isAuthenticated()) {
            User user = getCurrentUser();
            return new ResponseEntity<>(announcementsService.findAll(page, count, currency, filters, sortData,
                    user.getCountry(), user.getCity()), HttpStatus.OK);
        } else if (city == null) {
            throw new AnnouncementException("City is required");
        } else if (country == null) {
            throw new AnnouncementException("Country is required");
        } else {
            return new ResponseEntity<>(announcementsService.findAll(page, count, currency, filters, sortData,
                    country, city), HttpStatus.OK);
        }
    }

    @GetMapping("/find")
    public ResponseEntity<List<ResponseAnnouncementDTO>> findByTitle(@RequestParam String title,
                @RequestParam(defaultValue = "USD") CurrencyCode currency, @RequestParam int page, @RequestParam int count,
                @RequestParam(required = false) String city, @RequestParam(required = false) String country,
                @RequestParam(required = false) List<String> filters,
                @RequestParam(defaultValue = "ID", name = "sort_type") SortType sortType,
                @RequestParam(defaultValue = "DESC", value = "sort_direction") Sort.Direction sortDirection) {
        SortData sortData = new SortData(sortType, sortDirection);
        if (isAuthenticated()) {
            User user = getCurrentUser();
            return new ResponseEntity<>(announcementsService.findByTitle(page, count, title, currency, filters, sortData,
                    user.getCountry(), user.getCity()), HttpStatus.OK);
        } else if (city == null) {
            throw new AnnouncementException("City is required");
        } else if (country == null) {
            throw new AnnouncementException("Country is required");
        } else {
            return new ResponseEntity<>(announcementsService.findByTitle(page, count, title, currency, filters, sortData,
                    country, city), HttpStatus.OK);
        }
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<List<ResponseAnnouncementDTO>> getAllByUser(@PathVariable Long id,
               @RequestParam(defaultValue = "USD") CurrencyCode currency, @RequestParam int page, @RequestParam int count) {
        return new ResponseEntity<>(announcementsService.findAllByUser(id, currency, page, count), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShowAnnouncementDTO> show(@PathVariable Long id, @RequestParam(defaultValue = "USD") CurrencyCode currency) {
        return new ResponseEntity<>(announcementsService.show(id, currency), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<DataDTO<Long>> create(@RequestParam(defaultValue = "USD") CurrencyCode currency,
                @RequestBody @Valid CreateAnnouncementDTO createDTO, BindingResult bindingResult) {
        handleRequestErrors(bindingResult);
        return new ResponseEntity<>(announcementsService.save(createDTO, currency), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<HttpStatus> like(@PathVariable Long id) {
        announcementsService.addLike(id);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}/like")
    public ResponseEntity<HttpStatus> deleteLike(@PathVariable Long id) {
        announcementsService.deleteLike(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/reports")
    public ResponseEntity<List<ResponseReportDTO>> getReports(@RequestParam int page, @RequestParam int count,
                @RequestParam(value = "sort", defaultValue = "DESC") Sort.Direction sort) {
        return new ResponseEntity<>(announcementsService.getAllReports(page, count, sort), HttpStatus.OK);
    }

    @GetMapping("/report/{id}")
    public ResponseEntity<ShowReportDTO> getReport(@PathVariable long id,
                @RequestParam(defaultValue = "USD") CurrencyCode currency) {
        return new ResponseEntity<>(announcementsService.showReport(id, currency), HttpStatus.OK);
    }

    @DeleteMapping("/report/{id}")
    public ResponseEntity<HttpStatus> closeReport(@PathVariable Long id) {
        announcementsService.closeReport(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/{id}/report")
    public ResponseEntity<HttpStatus> report(@PathVariable Long id,
                @RequestBody @Valid CauseDTO causeDTO, BindingResult bindingResult) {
        handleRequestErrors(bindingResult);
        announcementsService.createReport(id, causeDTO.getCause());
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<HttpStatus> update(@PathVariable Long id, @RequestBody @Valid UpdateAnnouncementDTO updateDTO,
                BindingResult bindingResult) {
        handleRequestErrors(bindingResult);
        announcementsService.update(id, updateDTO);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/image/{id}")
    public ResponseEntity<?> downloadImage(@PathVariable Long id) {
        byte[] data = announcementsService.downloadImage(id);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.IMAGE_JPEG)
                .body(data);
    }

    @PostMapping("/{id}/image")
    public ResponseEntity<HttpStatus> addImage(@PathVariable Long id, @RequestParam MultipartFile image) {
        announcementsService.addImage(image, id);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @DeleteMapping("/image/{id}")
    public ResponseEntity<HttpStatus> deleteImage(@PathVariable Long id) {
        announcementsService.deleteImage(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> delete(@PathVariable Long id) {
        announcementsService.delete(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/{id}/ban")
    public ResponseEntity<HttpStatus> ban(@PathVariable Long id,
                @RequestBody @Valid CauseDTO causeDTO, BindingResult bindingResult) {
        handleRequestErrors(bindingResult);
        announcementsService.ban(id, causeDTO.getCause());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/expired")
    public ResponseEntity<HttpStatus> deleteExpiredAnnouncements() {
        announcementsService.deleteExpired();
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private void handleRequestErrors(BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            LocalDateTime timestamp = LocalDateTime.now();
            Map<String, ErrorData> error = bindingResult.getFieldErrors().stream()
                    .collect(Collectors.toMap(FieldError::getField, fieldError -> new ErrorData(
                            fieldError.getDefaultMessage(), timestamp)));
            throw new RequestException(error);
        }
    }

}