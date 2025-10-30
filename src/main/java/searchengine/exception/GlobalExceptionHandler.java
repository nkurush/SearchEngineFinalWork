package searchengine.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import searchengine.dto.responses.NotOkResponse;
import searchengine.exception.SearchEngineException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Обработка всех кастомных исключений приложения SearchEngineException.
     * Автоматически определяет HTTP статус из типа ошибки.
     * Логирует тип ошибки и сообщение.
     */
    @ExceptionHandler(SearchEngineException.class)
    public ResponseEntity<NotOkResponse> handleSearchEngineException(SearchEngineException ex) {
        log.error("Search engine error [{}]: {}", ex.getErrorType(), ex.getMessage());
        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(new NotOkResponse(ex.getMessage()));
    }

    /**
     * Обработка всех необработанных исключений.
     * Возвращает HTTP 500 Internal Server Error.
     * Логирует полный stack trace для отладки.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<NotOkResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(500)
                .body(new NotOkResponse("Произошла внутренняя ошибка сервера"));
    }
}
