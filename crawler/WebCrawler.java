package info.kgeorgiy.ja.Laskin_Pavel.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.*;

public class WebCrawler implements NewCrawler {
    final private Downloader downloader;
    final private ExecutorService downloaders;
    final private ExecutorService extractors;
    final private int perHost;

    private static int checkArg(String arg){
        int intArg = Integer.parseInt(arg);
        if (intArg > 0){
            return intArg;
        }
        throw new IllegalArgumentException("Arguments should be more than 0");
    }


    /**
     * Main entry point for the WebCrawler application.
     *
     * @param args Command line arguments:
     *             1. Starting URL (String)
     *             2. Download directory path (String)
     *             3. Extraction directory path (String)
     *             4. Maximum recursion depth (String)
     *             5. Comma-separated exclude patterns (String)
     * @throws IllegalArgumentException If arguments count is not 5
     * @throws IOException If I/O error occurs during crawling
     **/
    public static void main(String[] args) throws IOException {
        if (args.length != 5){
            throw new IllegalArgumentException("Should be 5 arguments");
        }
        try (WebCrawler crawler = new WebCrawler(new CachingDownloader(1), checkArg(args[1]), checkArg(args[2]), checkArg(args[2]))) {
            crawler.download(args[0], checkArg(args[3]), new ArrayList<>());    // MY_NOTE: я не понял, мы теперь прнимаем еще и List<String> excludes в main???
        } catch (IOException e){
            throw new IOException(e);
        }

    }

    /**
     * Creates WebCrawler with specified configuration.
     *
     * @param downloader Downloader implementation to use
     * @param downloaders Number of downloader threads
     * @param extractors Number of extractor threads
     * @param perHost Max concurrent requests per host
     */
    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost){
        this.downloader = downloader;
        this.downloaders = Executors.newFixedThreadPool(downloaders);
        this.extractors = Executors.newFixedThreadPool(extractors);
        this.perHost = perHost;
    }

    @Override
    public Result download(String url, int depth, List<String> excludes) {
        Map<String, IOException> exceptionMap = new ConcurrentHashMap<>();
        Set<String> visitedUrls = ConcurrentHashMap.newKeySet();
        Set<String> successUrls = ConcurrentHashMap.newKeySet();
        Phaser phaser = new Phaser(1);

        bfs(url, depth, phaser, visitedUrls, successUrls, exceptionMap, excludes);

        phaser.arriveAndAwaitAdvance();
        return new Result(new ArrayList<>(successUrls), exceptionMap);
    }

    @Override
    public Result download(String url, int depth) {
        return download(url, depth, new ArrayList<>());
    }

    private boolean checkHost(String url, List<String> excludes, Map<String, IOException> exceptionMap){
        boolean contain = false;
        String urlHost = "";
        try {
            urlHost = URLUtils.getHost(url);
        } catch (IOException e){
            exceptionMap.put(url, e);
        }
        for (String exUrls : excludes){;
            if (urlHost.contains(exUrls)) {
                contain = true;
                break;
            }
        }
        return !contain;
    }



    private void bfs(String url, int depth, Phaser phaser, Set<String> visitedUrls,
                     Set<String> successUrls, Map<String, IOException> exceptionMap,
                     List<String> excludes) {
        BlockingDeque<MyPair> currentLevel = new LinkedBlockingDeque<>();
        BlockingDeque<MyPair> nextLevel = new LinkedBlockingDeque<>();

        try {
            if (checkHost(url, excludes, exceptionMap) && visitedUrls.add(url)) {
                Document doc = downloader.download(url);
                successUrls.add(url);
                if (depth > 1) {
                    List<String> links = doc.extractLinks();
                    for (String link : links) {
                        if (checkHost(link, excludes, exceptionMap) && visitedUrls.add(link)) {
                            nextLevel.add(new MyPair(link, depth - 1));
                        }
                    }
                }
            }
        } catch (IOException e) {
            exceptionMap.put(url, e);
        }

        while (!nextLevel.isEmpty()) {
            BlockingDeque<MyPair> temp = currentLevel;
            currentLevel = nextLevel;
            nextLevel = temp;

            while (!currentLevel.isEmpty()) {
                MyPair curUrl = currentLevel.poll();
                if (curUrl != null && curUrl.depth > 0) {
                    phaser.register();
                    BlockingDeque<MyPair> finalNextLevel = nextLevel;
                    downloaders.submit(() -> {
                        try {
                            Document curDoc = downloader.download(curUrl.url);
                            successUrls.add(curUrl.url);
                            if (curUrl.depth > 1) {
                                phaser.register();
                                extractors.submit(() -> {
                                    try {
                                        List<String> curAllUrls = curDoc.extractLinks();
                                        for (String curEl : curAllUrls) {
                                            if (checkHost(curEl, excludes, exceptionMap) && visitedUrls.add(curEl)) {
                                                finalNextLevel.add(new MyPair(curEl, curUrl.depth - 1));
                                            }
                                        }
                                    } catch (IOException e) {
                                        exceptionMap.put(curUrl.url, e);
                                    } finally {
                                        phaser.arriveAndDeregister();
                                    }
                                });
                            }
                        } catch (IOException e) {
                            exceptionMap.put(curUrl.url, e);
                        } finally {
                            phaser.arriveAndDeregister();
                        }
                    });
                }
            }

            phaser.arriveAndAwaitAdvance();
        }
    }

    @Override
    public void close() {
        extractors.shutdown();
        downloaders.shutdown();
    }
}