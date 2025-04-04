package danix.app.chats_service.controllers;

import danix.app.chats_service.dto.ResponseChatDTO;
import danix.app.chats_service.dto.ResponseMessageDTO;
import danix.app.chats_service.services.ChatService;
import danix.app.chats_service.util.ChatException;
import danix.app.chats_service.util.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chats")
@RequiredArgsConstructor
public class ChatsController {
    private final ChatService chatService;

    @GetMapping
    public ResponseEntity<List<ResponseChatDTO>> getUserChats() {
        return new ResponseEntity<>(chatService.getUserChats(), HttpStatus.OK);
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<ResponseMessageDTO>> getChatMessages(@PathVariable long id, @RequestParam int page,
                                                                    @RequestParam int count) {
        return new ResponseEntity<>(chatService.getChatMessages(id, page, count), HttpStatus.OK);
    }

    @PostMapping("/{id}")
    public ResponseEntity<HttpStatus> create(@PathVariable long id, @RequestHeader("Authorization") String token) {
        chatService.create(id, token);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> delete(@PathVariable long id) {
        chatService.delete(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/{id}/message")
    public ResponseEntity<HttpStatus> sendMessage(@PathVariable long id, @RequestBody Map<String, Object> messageData,
                                                 @RequestHeader("Authorization") String token) {
        String message = (String) messageData.get("message");
        if (message == null || message.isBlank()) {
            throw new ChatException("Message must not be empty");
        }
        chatService.sendTextMessage(id, message, token);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/{id}/image")
    public ResponseEntity<HttpStatus> sendImage(@PathVariable long id, @RequestParam MultipartFile image,
                                                @RequestHeader("Authorization") String token) {
        chatService.sendImage(id, image, token);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping("/image/{id}")
    public ResponseEntity<?> downloadImage(@PathVariable long id) {
        Map<String, Object> imageData = chatService.getImage(id);
        byte[] body = Base64.getDecoder().decode((String) imageData.get("data"));
        MediaType mediaType = MediaType.parseMediaType((String) imageData.get("mediaType"));
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(mediaType)
                .body(body);
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