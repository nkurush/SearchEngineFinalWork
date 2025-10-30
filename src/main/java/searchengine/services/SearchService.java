package searchengine.services;

import java.io.IOException;

public interface SearchService {
    Object search(String query, String site, Integer offset, Integer limit) throws IOException;
}
