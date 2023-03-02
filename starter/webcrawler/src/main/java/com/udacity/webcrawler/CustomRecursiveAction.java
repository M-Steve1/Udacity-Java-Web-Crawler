package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

public final class CustomRecursiveAction extends RecursiveAction {
    private String url;
    private Instant deadline;
    private final int maxDepth;
    private Map<String, Integer> counts;
    private Set<String>visitedUrls;
    private final Clock clock;
    private final List<Pattern> ignoredUrls;
    private final PageParserFactory parserFactory;
    private ReentrantLock lock = new ReentrantLock();


    public CustomRecursiveAction(
            String url,
            Instant deadline,
            int maxDepth,
            Map<String, Integer> counts,
            Set<String> visitedUrls,
            Clock clock,
            List<Pattern> ignoredUrls,
            PageParserFactory parserFactory

    ) {
        this.url = url;
        this.deadline = deadline;
        this.maxDepth = maxDepth;
        this.counts = counts;
        this.visitedUrls = visitedUrls;
        this.clock = clock;
        this.ignoredUrls = ignoredUrls;
        this.parserFactory = parserFactory;
    }

    @Override
    protected void compute() {
        if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
            return;
        }
        for (Pattern pattern: ignoredUrls) {
            if (pattern.matcher(url).matches()) {
                return;
            }
        }

        // contains and add are not atomic and thus this will
        // make this list thread-safe.
        lock.lock();
        if (visitedUrls.contains(url)){
            return;
        }
        visitedUrls.add(url);
        lock.unlock();

        PageParser.Result result = parserFactory.get(url).parse();
        popularWordCount(result, counts);
        List<String> subLinks = result.getLinks();
        invokeAll(createSubTask(subLinks));

    }

    private void popularWordCount(PageParser.Result result, Map<String, Integer> counts) {
        for (Map.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
            counts.compute(e.getKey(), (k, v) -> (v == null) ? e.getValue() : e.getValue() + counts.get(e.getKey()));
        }
    }

    private List<CustomRecursiveAction> createSubTask(List<String> subLinks) {
        List<CustomRecursiveAction> subTasks = new ArrayList<>();

        for (String link : subLinks) {
            subTasks.add(new CustomRecursiveAction(link, deadline, maxDepth - 1 , counts, visitedUrls, clock, ignoredUrls, parserFactory));
        }

        return subTasks;
    }
}
