package danix.app.users_service.config;

import danix.app.users_service.dto.ErrorResponseDTO;
import danix.app.users_service.dto.RequestErrorResponseDTO;
import danix.app.users_service.util.ErrorCode;
import danix.app.users_service.util.ErrorData;
import danix.app.users_service.util.RequestException;
import danix.app.users_service.util.UserException;
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
	public ResponseEntity<ErrorResponseDTO> handleException(UserException e) {
		return new ResponseEntity<>(getErrorResponseDTO(e), HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler
	public ResponseEntity<RequestErrorResponseDTO> handleException(RequestException e) {
		return new ResponseEntity<>(new RequestErrorResponseDTO(ErrorCode.REQUEST_ERROR, e.getError()), HttpStatus.BAD_REQUEST);
	}

	private ErrorResponseDTO getErrorResponseDTO(Exception e) {
		ErrorData errorData = new ErrorData(e.getMessage(), LocalDateTime.now());
		return new ErrorResponseDTO(ErrorCode.PROCESSING_ERROR, errorData);
	}

}
