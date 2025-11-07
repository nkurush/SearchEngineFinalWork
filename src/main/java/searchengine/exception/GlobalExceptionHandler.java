package searchengine.exception;

import lombok.extern.slf4j.Slf4j;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import searchengine.dto.responses.NotOkResponse;
import searchengine.dto.responses.SearchDataResponse;
import searchengine.dto.responses.SearchResponse;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(SearchEngineException.class)
    public ResponseEntity<NotOkResponse> handleSearchEngineException(SearchEngineException ex) {
        log.error("Search engine error [{}]: {}", ex.getErrorType(), ex.getMessage());
        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(new NotOkResponse(ex.getMessage()));
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<NotOkResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(500)
                .body(new NotOkResponse("Произошла внутренняя ошибка сервера"));
    }
}

