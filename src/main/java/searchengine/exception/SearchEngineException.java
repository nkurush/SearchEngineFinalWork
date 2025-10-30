package searchengine.exception;

import org.springframework.http.HttpStatus;

public class SearchEngineException extends RuntimeException {
    private final HttpStatus httpStatus;
    private final ErrorType errorType;

    private SearchEngineException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
        this.httpStatus = errorType.getHttpStatus();
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    // Статические фабричные методы для удобства
    public static SearchEngineException indexingAlreadyStarted() {
        return new SearchEngineException(
                ErrorType.INDEXING_ALREADY_STARTED,
                "Индексация уже запущена"
        );
    }
    public static SearchEngineException indexingNotStarted() {
        return new SearchEngineException(
                ErrorType.INDEXING_NOT_STARTED,
                "Индексация не запущена"
        );
    }

    public static SearchEngineException pageOutOfBounds() {
        return new SearchEngineException(
                ErrorType.PAGE_OUT_OF_BOUNDS,
                "Данная страница находится за пределами сайтов, указанных в конфигурационном файле"
        );
    }

    public static SearchEngineException emptySearchQuery() {
        return new SearchEngineException(
                ErrorType.EMPTY_SEARCH_QUERY,
                "Задан пустой поисковый запрос"
        );
    }

    // Для кастомных сообщений
    public static SearchEngineException indexingAlreadyStarted(String customMessage) {
        return new SearchEngineException(ErrorType.INDEXING_ALREADY_STARTED, customMessage);
    }
    public static SearchEngineException notFound(String message) {
        return new SearchEngineException(ErrorType.NOT_FOUND, message);
    }
    public static SearchEngineException pageOutOfBounds(String customMessage) {
        return new SearchEngineException(ErrorType.PAGE_OUT_OF_BOUNDS, customMessage);
    }
    public static SearchEngineException indexingNotCompleted() {
        return new SearchEngineException(
                ErrorType.INDEXING_NOT_COMPLETED,
                "Индексация сайта для поиска не закончена"
        );
    }


    public enum ErrorType {
        INDEXING_ALREADY_STARTED(HttpStatus.CONFLICT),
        INDEXING_NOT_STARTED(HttpStatus.METHOD_NOT_ALLOWED),
        PAGE_OUT_OF_BOUNDS(HttpStatus.BAD_REQUEST),
        EMPTY_SEARCH_QUERY(HttpStatus.BAD_REQUEST),
        INDEXING_NOT_COMPLETED(HttpStatus.BAD_REQUEST),
        NOT_FOUND(HttpStatus.NOT_FOUND);


        private final HttpStatus httpStatus;

        ErrorType(HttpStatus httpStatus) {
            this.httpStatus = httpStatus;
        }

        public HttpStatus getHttpStatus() {
            return httpStatus;
        }
    }
}
