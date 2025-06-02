package danix.app.chats_service.controllers;

import danix.app.chats_service.dto.MessageDTO;
import danix.app.chats_service.dto.DataDTO;
import danix.app.chats_service.dto.ResponseMessageDTO;
import danix.app.chats_service.dto.ResponseSupportChatDTO;
import danix.app.chats_service.services.SupportChatsService;
import danix.app.chats_service.util.ChatException;
import danix.app.chats_service.util.ContentType;
import danix.app.chats_service.util.ErrorResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

import static danix.app.chats_service.controllers.UsersChatsController.handleRequestErrors;

@RestController
@RequestMapping("/chats/support")
@RequiredArgsConstructor
public class SupportChatsController {

	private final SupportChatsService chatService;

	@GetMapping("/user")
	public List<ResponseSupportChatDTO> findAllByUser() {
		return chatService.getUserChats();
	}

	@GetMapping
	@PreAuthorize(value = "hasRole('ADMIN')")
	public List<ResponseSupportChatDTO> findAll(@RequestParam(defaultValue = "ASC") Sort.Direction sort,
												@RequestParam int page, @RequestParam int count) {
		return chatService.findAll(page, count, sort);
	}

	@GetMapping("/{id}/messages")
	public List<ResponseMessageDTO> messages(@PathVariable long id, @RequestParam int page, @RequestParam int count) {
		return chatService.getChatMessages(id, page, count);
	}

	@PostMapping
	public ResponseEntity<DataDTO<Long>> create(@RequestBody @Valid MessageDTO messageDTO,
				BindingResult bindingResult) {
		handleRequestErrors(bindingResult);
		return new ResponseEntity<>(chatService.create(messageDTO.getMessage()), HttpStatus.CREATED);
	}

	@PatchMapping("/{id}/close")
	public ResponseEntity<HttpStatus> close(@PathVariable long id) {
		chatService.close(id);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PatchMapping("/{id}/status/wait")
	public ResponseEntity<HttpStatus> setStatusToWait(@PathVariable long id) {
		chatService.setStatusToWait(id);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PatchMapping("/{id}/status/processing")
	@PreAuthorize(value = "hasRole('ADMIN')")
	public ResponseEntity<HttpStatus> setStatusToProcessing(@PathVariable long id) {
		chatService.setStatusToProcessing(id);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<HttpStatus> delete(@PathVariable long id) {
		chatService.delete(id);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/{id}/message")
	public ResponseEntity<DataDTO<Long>> sendMessage(@PathVariable long id, @RequestBody @Valid MessageDTO messageDTO,
													 @RequestHeader("Authorization") String token, BindingResult bindingResult) {
		handleRequestErrors(bindingResult);
		return new ResponseEntity<>(chatService.sendTextMessage(id, messageDTO.getMessage(), token), HttpStatus.CREATED);
	}

	@PatchMapping("/message/{id}")
	public ResponseEntity<HttpStatus> updateMessage(@PathVariable long id, @RequestBody @Valid MessageDTO messageDTO,
			BindingResult bindingResult) {
		handleRequestErrors(bindingResult);
		chatService.updateMessage(id, messageDTO.getMessage());
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/{id}/image")
	public ResponseEntity<DataDTO<Long>> sendImage(@PathVariable long id, @RequestParam MultipartFile image,
												   @RequestHeader("Authorization") String token) {
		return new ResponseEntity<>(chatService.sendFile(id, image, token, ContentType.IMAGE), HttpStatus.CREATED);
	}

	@PostMapping("/{id}/video")
	public ResponseEntity<DataDTO<Long>> sendVideo(@PathVariable long id, @RequestParam MultipartFile video,
												   @RequestHeader("Authorization") String token) {
		return new ResponseEntity<>(chatService.sendFile(id, video, token, ContentType.VIDEO), HttpStatus.OK);
	}

	@GetMapping("/image/{id}")
	public ResponseEntity<?> downloadImage(@PathVariable long id) {
		return chatService.getFile(id, ContentType.IMAGE);
	}

	@GetMapping("/video/{id}")
	public ResponseEntity<?> downloadVideo(@PathVariable long id) {
		return chatService.getFile(id, ContentType.VIDEO);
	}

	@DeleteMapping("/message/{id}")
	public ResponseEntity<HttpStatus> deleteMessage(@PathVariable long id) {
		chatService.deleteMessage(id);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@ExceptionHandler
	public ResponseEntity<ErrorResponse> handleException(ChatException e) {
		return new ResponseEntity<>(new ErrorResponse(e.getMessage(), LocalDateTime.now()), HttpStatus.BAD_REQUEST);
	}

}