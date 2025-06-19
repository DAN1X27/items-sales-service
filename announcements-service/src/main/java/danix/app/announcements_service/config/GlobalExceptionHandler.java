package danix.app.announcements_service.config;

import danix.app.announcements_service.dto.ErrorResponseDTO;
import danix.app.announcements_service.dto.RequestErrorResponseDTO;
import danix.app.announcements_service.util.AnnouncementException;
import danix.app.announcements_service.util.ErrorCode;
import danix.app.announcements_service.util.ErrorData;
import danix.app.announcements_service.util.RequestException;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MultipartException;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(FeignException.Unauthorized.class)
	public ResponseEntity<HttpStatus> handleFeignUnauthorizedException() {
		return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
	}

	@ExceptionHandler(FeignException.Forbidden.class)
	public ResponseEntity<HttpStatus> handleFeignForbiddenException() {
		return new ResponseEntity<>(HttpStatus.FORBIDDEN);
	}

	@ExceptionHandler(MultipartException.class)
	public ResponseEntity<ErrorResponseDTO> handleMultipartException(MultipartException e) {
		return new ResponseEntity<>(getErrorResponseDTO(e), HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler
	public ResponseEntity<ErrorResponseDTO> handleAnnouncementException(AnnouncementException e) {
		return new ResponseEntity<>(getErrorResponseDTO(e), HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler
	public ResponseEntity<RequestErrorResponseDTO> handleRequestException(RequestException e) {
		return new ResponseEntity<>(new RequestErrorResponseDTO(ErrorCode.REQUEST_ERROR, e.getError()), HttpStatus.BAD_REQUEST);
	}

	private ErrorResponseDTO getErrorResponseDTO(Exception e) {
		ErrorData errorData = new ErrorData(e.getMessage(), LocalDateTime.now());
		return new ErrorResponseDTO(ErrorCode.PROCESSING_ERROR, errorData);
	}

}
