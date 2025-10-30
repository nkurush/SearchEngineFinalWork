package searchengine.services;

import searchengine.model.SitePage;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

public interface ApiService {
        void startIndexing();
        void stopIndexing();
        void indexPage(String url) throws IOException;
        void refreshPage(SitePage siteDomain, URL url);
}
