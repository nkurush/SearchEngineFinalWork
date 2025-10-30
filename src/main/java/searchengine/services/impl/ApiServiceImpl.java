package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.config.Connection;
import searchengine.exception.SearchEngineException;
import searchengine.model.Page;
import searchengine.model.SitePage;
import searchengine.model.Status;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.ApiService;
import searchengine.services.LemmaService;
import searchengine.services.PageIndexerService;
import searchengine.services.impl.PageFinder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiServiceImpl implements ApiService {
    private final PageIndexerService pageIndexerService;
    private final LemmaService lemmaService;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final SitesList sitesToIndexing;
    //private final Set<SitePage> sitePagesAllFromDB;
    private final Connection connection;


    private final AtomicBoolean indexingProcessing = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void startIndexing() {
        if (indexingProcessing.get()) {
            throw SearchEngineException.indexingAlreadyStarted();
        }

        executor.submit(() -> {
            indexingProcessing.set(true);
            log.info("=== ИНДЕКСАЦИЯ ЗАПУЩЕНА ===");
            try {
                deleteSitePagesAndPagesInDB();
                addSitePagesToDB();
                indexAllSitePages();
                log.info("=== indexAllSitePages() ЗАВЕРШЕН ===");

            } catch (RuntimeException | InterruptedException ex) {
                indexingProcessing.set(false);
                log.error("Error: ", ex);
            }finally {
                indexingProcessing.set(false);
                log.info("=== ИНДЕКСАЦИЯ ПОЛНОСТЬЮ ЗАВЕРШЕНА, ФЛАГ СБРОШЕН ===");

            }
        });
    }

    // Новый метод - логика из контроллера
    @Override
    public void stopIndexing() {
        if (!indexingProcessing.get()) {
            throw SearchEngineException.indexingNotStarted();
        }
        indexingProcessing.set(false);
    }

    @Override
    public void indexPage(String url) throws IOException {
        URL refUrl = new URL(url);
        SitePage sitePage = new SitePage();

        boolean siteFound = sitesToIndexing.getSites().stream()
                .filter(site -> refUrl.getHost().equals(site.getUrl().getHost()))
                .findFirst()
                .map(site -> {
                    sitePage.setName(site.getName());
                    sitePage.setUrl(site.getUrl().toString());
                    return true;
                })
                .orElse(false);

        if (!siteFound) {
            throw SearchEngineException.pageOutOfBounds();
        }

        refreshPage(sitePage, refUrl);
    }

    // СТАРЫЙ МЕТОД - БЕЗ ИЗМЕНЕНИЙ
    @Override
    public void refreshPage(SitePage siteDomain, URL url) {
        SitePage existSitePate = siteRepository.getSitePageByUrl(siteDomain.getUrl());
        siteDomain.setId(existSitePate.getId());
        /*SitePage existSitePate = siteRepository.getSitePageByUrl(siteDomain.getUrl())
                .orElseThrow(() -> SearchEngineException.notFound("Сайт не найден: " + siteDomain.getUrl()));
        siteDomain.setId(existSitePate.getId());
*/
        ConcurrentHashMap<String, Page> resultForkJoinPageIndexer = new ConcurrentHashMap<>();
        try {
            log.info("Запущена переиндексация страницы:" + url.toString());
            PageFinder f = new PageFinder(siteRepository, pageRepository, siteDomain, url.getPath(), resultForkJoinPageIndexer, connection, lemmaService, pageIndexerService, indexingProcessing);
            f.refreshPage();
        } catch (SecurityException ex) {
            SitePage sitePage = siteRepository.findById(siteDomain.getId()).orElseThrow();
            sitePage.setStatus(Status.FAILED);
            sitePage.setLastError(ex.getMessage());
            siteRepository.save(sitePage);
        }
        log.info("Проиндексирован сайт: " + siteDomain.getName());
        SitePage sitePage = siteRepository.findById(siteDomain.getId()).orElseThrow();
        sitePage.setStatus(Status.INDEXED);
        siteRepository.save(sitePage);
    }

    private void deleteSitePagesAndPagesInDB() {
        List<SitePage> sitesFromDB = siteRepository.findAll();

        for (SitePage sitePageDb : sitesFromDB) {
            for (Site siteApp : sitesToIndexing.getSites()) {
                if (sitePageDb.getUrl().equals(siteApp.getUrl().toString())) {
                    siteRepository.deleteById(sitePageDb.getId());
                    break;
                }
            }
        }
    }


    private void addSitePagesToDB() {
        for (Site siteApp : sitesToIndexing.getSites()) {
            SitePage sitePageDAO = new SitePage();
            sitePageDAO.setStatus(Status.INDEXING);
            sitePageDAO.setName(siteApp.getName());
            sitePageDAO.setUrl(siteApp.getUrl().toString());
            siteRepository.save(sitePageDAO);
        }
    }

    private void indexAllSitePages() throws InterruptedException {

        //sitePagesAllFromDB.addAll(siteRepository.findAll());
        List<SitePage> sitePagesAllFromDB = siteRepository.findAll();

        List<String> urlToIndexing = new ArrayList<>();
        for (Site siteApp : sitesToIndexing.getSites()) {
            urlToIndexing.add(siteApp.getUrl().toString());
        }

        sitePagesAllFromDB.removeIf(sitePage -> !urlToIndexing.contains(sitePage.getUrl()));

        List<Thread> indexingThreadList = new ArrayList<>();
        for (SitePage siteDomain : sitePagesAllFromDB) {
            final SitePage siteToIndex = siteDomain;

            log.info("=== Создание потока для сайта: {} ===", siteToIndex.getUrl());

            Runnable indexSite = () -> {
                log.info("=== ПОТОК ЗАПУЩЕН для сайта: {} ===", siteToIndex.getUrl());
                ConcurrentHashMap<String, Page> resultForkJoinPageIndexer = new ConcurrentHashMap<>();

                try {
                    log.info("Запущена индексация " + siteToIndex.getUrl());
                    new ForkJoinPool().invoke(new PageFinder(
                            siteRepository,
                            pageRepository,
                            siteToIndex,
                            "",
                            resultForkJoinPageIndexer,
                            connection,
                            lemmaService,
                            pageIndexerService,
                            indexingProcessing
                    ));
                } catch (SecurityException ex) {
                    SitePage sitePage = siteRepository.findById(siteToIndex.getId()).orElseThrow();
                    sitePage.setStatus(Status.FAILED);
                    sitePage.setLastError(ex.getMessage());
                    siteRepository.save(sitePage);
                }

                if (!indexingProcessing.get()) {
                    log.warn("Indexing stopped by user, site:" + siteToIndex.getUrl());
                    SitePage sitePage = siteRepository.findById(siteToIndex.getId()).orElseThrow();
                    sitePage.setStatus(Status.FAILED);
                    sitePage.setLastError("Indexing stopped by user");
                    siteRepository.save(sitePage);
                } else {
                    log.info("Проиндексирован сайт: " + siteToIndex.getUrl());
                    SitePage sitePage = siteRepository.findById(siteToIndex.getId()).orElseThrow();
                    sitePage.setStatus(Status.INDEXED);
                    siteRepository.save(sitePage);
                }

                log.info("=== ПОТОК ЗАВЕРШЁН для сайта: {} ===", siteToIndex.getUrl());
            };

            Thread thread = new Thread(indexSite);
            thread.setName("Indexing-" + siteToIndex.getName());
            indexingThreadList.add(thread);
            thread.start();
            log.info("=== Поток {} запущен ===", thread.getName());
        }

        for (Thread thread : indexingThreadList) {
            thread.join();
        }
    }
}



