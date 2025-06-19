package danix.app.authentication_service.config;

import danix.app.authentication_service.dto.ErrorResponseDTO;
import danix.app.authentication_service.dto.RequestErrorResponseDTO;
import danix.app.authentication_service.util.AuthenticationException;
import danix.app.authentication_service.util.ErrorCode;
import danix.app.authentication_service.util.ErrorData;
import danix.app.authentication_service.util.RequestException;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MultipartException;

import java.time.LocalDateTime;

@ControllerAdvice
@Slf4j
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

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ErrorResponseDTO> handleAuthenticationException(AuthenticationException e) {
		return new ResponseEntity<>(getErrorResponseDTO(e), HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(RequestException.class)
	public ResponseEntity<RequestErrorResponseDTO> handleRequestException(RequestException e) {
		return new ResponseEntity<>(new RequestErrorResponseDTO(ErrorCode.REQUEST_ERROR, e.getError()), HttpStatus.BAD_REQUEST);
	}

	private ErrorResponseDTO getErrorResponseDTO(Exception e) {
		ErrorData errorData = new ErrorData(e.getMessage(), LocalDateTime.now());
		return new ErrorResponseDTO(ErrorCode.PROCESSING_ERROR, errorData);
	}

}
