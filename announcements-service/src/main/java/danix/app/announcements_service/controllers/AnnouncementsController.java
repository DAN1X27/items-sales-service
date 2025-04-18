package danix.app.announcements_service.controllers;

import danix.app.announcements_service.dto.*;
import danix.app.announcements_service.services.AnnouncementsService;
import danix.app.announcements_service.util.AnnouncementException;
import danix.app.announcements_service.util.ErrorResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/announcements")
@RequiredArgsConstructor
public class AnnouncementsController {

	private final AnnouncementsService announcementsService;

	@GetMapping
	public ResponseEntity<List<ResponseDTO>> findAll(@RequestParam int page, @RequestParam int count,
			@RequestParam(defaultValue = "USD") String currency, @RequestParam(required = false) List<String> filters,
			@RequestBody(required = false) @Valid SortDTO sortDTO, BindingResult bindingResult) {
		handleRequestErrors(bindingResult);
		return new ResponseEntity<>(announcementsService.findAll(page, count, currency, filters, sortDTO),
				HttpStatus.OK);
	}

	@GetMapping("/find")
	public ResponseEntity<List<ResponseDTO>> findAnnouncements(@RequestParam String title,
			@RequestParam(defaultValue = "USD") String currency, @RequestParam int page, @RequestParam int count,
			@RequestParam(required = false) List<String> filters, @RequestBody(required = false) @Valid SortDTO sortDTO,
			BindingResult bindingResult) {
		handleRequestErrors(bindingResult);
		return new ResponseEntity<>(announcementsService.findByTitle(page, count, title, currency, filters, sortDTO),
				HttpStatus.OK);
	}

	@GetMapping("/user/{id}")
	public ResponseEntity<List<ResponseDTO>> getAllByUser(@PathVariable Long id,
			@RequestParam(defaultValue = "USD") String currency) {
		return new ResponseEntity<>(announcementsService.findAllByUser(id, currency), HttpStatus.OK);
	}

	@GetMapping("/{id}")
	public ResponseEntity<ShowDTO> show(@PathVariable Long id, @RequestParam(defaultValue = "USD") String currency) {
		return new ResponseEntity<>(announcementsService.show(id, currency), HttpStatus.OK);
	}

	@PostMapping
	public ResponseEntity<DataDTO<Long>> create(@RequestParam(defaultValue = "USD") String currency,
			@RequestBody @Valid CreateDTO createDTO, BindingResult bindingResult) {
		handleRequestErrors(bindingResult);
		return new ResponseEntity<>(announcementsService.save(createDTO, currency), HttpStatus.CREATED);
	}

	@PostMapping("/{id}/like")
	public ResponseEntity<HttpStatus> like(@PathVariable Long id) {
		announcementsService.like(id);
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@DeleteMapping("/{id}/like")
	public ResponseEntity<HttpStatus> deleteLike(@PathVariable Long id) {
		announcementsService.deleteLike(id);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@GetMapping("/reports")
	public ResponseEntity<List<ResponseReportDTO>> getReports(@RequestParam int page, @RequestParam int count,
				@RequestParam(defaultValue = "USD") String currency,
				@RequestParam(value = "sort", defaultValue = "DESC") Sort.Direction sort) {
		return new ResponseEntity<>(announcementsService.getReports(page, count, sort, currency), HttpStatus.OK);
	}

	@DeleteMapping("/report/{id}")
	public ResponseEntity<HttpStatus> closeReport(@PathVariable Long id) {
		announcementsService.closeReport(id);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/{id}/report")
	public ResponseEntity<DataDTO<Long>> report(@PathVariable Long id,
					@RequestBody @Valid CauseDTO causeDTO, BindingResult bindingResult) {
		handleRequestErrors(bindingResult);
		return new ResponseEntity<>(announcementsService.report(id, causeDTO.getCause()), HttpStatus.CREATED);
	}

	@PatchMapping("/{id}")
	public ResponseEntity<HttpStatus> update(@PathVariable Long id, @RequestBody UpdateDTO updateDTO) {
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
	public ResponseEntity<HttpStatus> deleteByAdmin(@PathVariable Long id,
				@RequestBody @Valid CauseDTO causeDTO, BindingResult bindingResult) {
		handleRequestErrors(bindingResult);
		announcementsService.ban(id, causeDTO.getCause());
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@ExceptionHandler
	public ResponseEntity<ErrorResponse> handleException(AnnouncementException e) {
		return new ResponseEntity<>(new ErrorResponse(e.getMessage(), LocalDateTime.now()), HttpStatus.BAD_REQUEST);
	}

	private void handleRequestErrors(BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			StringBuilder message = new StringBuilder();
			bindingResult.getFieldErrors()
					.forEach(error -> message.append(error.getDefaultMessage()).append("; "));
			throw new AnnouncementException(message.toString());
		}
	}

}