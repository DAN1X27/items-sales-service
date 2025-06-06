package danix.app.chats_service.controllers;

import danix.app.chats_service.dto.MessageDTO;
import danix.app.chats_service.dto.DataDTO;
import danix.app.chats_service.dto.ResponseUsersChatDTO;
import danix.app.chats_service.dto.ResponseMessageDTO;
import danix.app.chats_service.services.UsersChatsService;
import danix.app.chats_service.util.ChatException;
import danix.app.chats_service.util.ContentType;
import danix.app.chats_service.util.ErrorResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@Tag(name = "Users chats API")
@RequestMapping("/chats")
@RequiredArgsConstructor
public class UsersChatsController {

	private final UsersChatsService chatsService;

	@GetMapping
	public ResponseEntity<List<ResponseUsersChatDTO>> getUserChats() {
		return new ResponseEntity<>(chatsService.getUserChats(), HttpStatus.OK);
	}

	@GetMapping("/{id}/messages")
	public ResponseEntity<List<ResponseMessageDTO>> getChatMessages(@PathVariable long id, @RequestParam int page,
			@RequestParam int count) {
		return new ResponseEntity<>(chatsService.getChatMessages(id, page, count), HttpStatus.OK);
	}

	@PostMapping("/{id}")
	public ResponseEntity<DataDTO<Long>> create(@PathVariable long id, @RequestHeader("Authorization") String token) {

		return new ResponseEntity<>(chatsService.create(id, token), HttpStatus.CREATED);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<HttpStatus> delete(@PathVariable long id) {
		chatsService.delete(id);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/{id}/message")
	public ResponseEntity<DataDTO<Long>> sendMessage(@PathVariable long id,
			@RequestHeader("Authorization") String token, @RequestBody @Valid MessageDTO messageDTO,
			BindingResult bindingResult) {
		handleRequestErrors(bindingResult);
		return new ResponseEntity<>(chatsService.sendTextMessage(id, messageDTO.getMessage(), token),
				HttpStatus.CREATED);
	}

	@PatchMapping("/message/{id}")
	public ResponseEntity<HttpStatus> updateMessage(@PathVariable long id, @RequestBody @Valid MessageDTO messageDTO,
			BindingResult bindingResult) {
		handleRequestErrors(bindingResult);
		chatsService.updateMessage(id, messageDTO.getMessage());
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/{id}/image")
	public ResponseEntity<DataDTO<Long>> sendImage(@PathVariable long id, @RequestParam MultipartFile image,
			@RequestHeader("Authorization") String token) {

		return new ResponseEntity<>(chatsService.sendFile(id, image, token, ContentType.IMAGE), HttpStatus.CREATED);
	}

	@PostMapping("/{id}/video")
	public ResponseEntity<DataDTO<Long>> sendVideo(@PathVariable long id, @RequestParam MultipartFile video,
			@RequestHeader("Authorization") String token) {
		return new ResponseEntity<>(chatsService.sendFile(id, video, token, ContentType.VIDEO), HttpStatus.OK);
	}

	@GetMapping("/video/{id}")
	public ResponseEntity<?> downloadVideo(@PathVariable long id) {
		return chatsService.getFile(id, ContentType.VIDEO);
	}

	@GetMapping("/image/{id}")
	public ResponseEntity<?> downloadImage(@PathVariable long id) {
		return chatsService.getFile(id, ContentType.IMAGE);
	}

	@DeleteMapping("/message/{id}")
	public ResponseEntity<HttpStatus> deleteMessage(@PathVariable long id) {
		chatsService.deleteMessage(id);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@ExceptionHandler
	public ResponseEntity<ErrorResponse> handleException(ChatException e) {
		return new ResponseEntity<>(new ErrorResponse(e.getMessage(), LocalDateTime.now()), HttpStatus.BAD_REQUEST);
	}

	static void handleRequestErrors(BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			StringBuilder message = new StringBuilder();
			bindingResult.getFieldErrors().forEach(error -> message.append(error.getDefaultMessage()).append("; "));
			throw new ChatException(message.toString());
		}
	}

}