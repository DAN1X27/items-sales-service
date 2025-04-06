package danix.app.chats_service.controllers;

import danix.app.chats_service.dto.ResponseMessageDTO;
import danix.app.chats_service.dto.ResponseSupportChatDTO;
import danix.app.chats_service.services.SupportChatService;
import danix.app.chats_service.util.ChatException;
import danix.app.chats_service.util.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chats/support")
@RequiredArgsConstructor
public class SupportChatsController {
    private final SupportChatService chatService;

    @GetMapping("/user")
    public List<ResponseSupportChatDTO> findAllByUser() {
        return chatService.findAllByUser();
    }

    @GetMapping
    @PreAuthorize(value = "hasRole('ADMIN')")
    public List<ResponseSupportChatDTO> findAll(@RequestParam(defaultValue = "ASC") String sort,
                                                @RequestParam int page, @RequestParam int count) {
        return chatService.findAll(page, count, sort);
    }

    @GetMapping("/{id}/messages")
    public List<ResponseMessageDTO> messages(@PathVariable long id, @RequestParam int page,
                                             @RequestParam int count) {
        return chatService.getChatMessages(id, page, count);
    }

    @PostMapping
    public ResponseEntity<HttpStatus> create() {
        chatService.create();
        return new ResponseEntity<>(HttpStatus.CREATED);
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

    @PatchMapping("/{id}/take")
    @PreAuthorize(value = "hasRole('ADMIN')")
    public ResponseEntity<HttpStatus> setStatusToProcessing(@PathVariable long id) {
        chatService.take(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> delete(@PathVariable long id) {
        chatService.delete(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/{id}/message")
    public ResponseEntity<HttpStatus> sendMessage(@PathVariable long id,
                                                  @RequestBody Map<String, Object> messageData) {
        String message = (String) messageData.get("message");
        if (message == null || message.isBlank()) {
            throw new ChatException("Message must not be empty");
        }
        chatService.sendTextMessage(message, id);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/{id}/message/image")
    public ResponseEntity<HttpStatus> sendImage(@PathVariable long id, @RequestParam MultipartFile image) {
        chatService.sendImage(image, id);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping("/message/image/{id}")
    public ResponseEntity<?> downloadImage(@PathVariable long id) {
        return ChatsController.convertImageToResponseEntity(chatService.getImage(id));
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