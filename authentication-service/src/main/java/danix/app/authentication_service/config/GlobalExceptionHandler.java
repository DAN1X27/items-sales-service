package danix.app.authentication_service.config;

import danix.app.authentication_service.util.ErrorResponse;
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
	public ResponseEntity<ErrorResponse> handleMultipartException(MultipartException e) {
		return new ResponseEntity<>(new ErrorResponse(e.getMessage(), LocalDateTime.now()), HttpStatus.BAD_REQUEST);
	}

}
