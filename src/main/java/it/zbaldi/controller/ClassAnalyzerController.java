package it.zbaldi.controller;

import it.zbaldi.model.GitWorktreeManager;
import it.zbaldi.model.LocalCache;
import it.zbaldi.model.ReleaseInfoSearcher;
import it.zbaldi.model.TicketSearcher;
import it.zbaldi.model.daos.CsvFileDao;
import it.zbaldi.model.data.DatasetEntry;
import it.zbaldi.model.data.FixedBuggyTicket;
import it.zbaldi.model.data.ReleaseInfo;
import it.zbaldi.model.extractors.CkManagerExtractor;
import it.zbaldi.model.extractors.GitManagerExtractor;
import it.zbaldi.model.extractors.OtherMetricsExtractor;
import it.zbaldi.model.interfaces.DatasetDao;
import it.zbaldi.model.interfaces.MetricExtractor;
import it.zbaldi.model.util.Couple;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Slf4j
public class ClassAnalyzerController {

    /**
     * Creates code snapshots from a subset of Jira releases.
     * The subset size is determined by the given percentage.
     *
     * @param keepPercentage fraction of releases to keep (0–1)
     *                       used to build Git worktrees
     */
    public void getCodeSnapshots(float keepPercentage) {

        List<ReleaseInfo> releases = new ReleaseInfoSearcher().getJiraReleases();
        int sizeToKeep = (int) (releases.size() * keepPercentage);

        if (sizeToKeep > 0 && sizeToKeep < releases.size()) {
            new GitWorktreeManager().buildWorktree(releases.subList(0, sizeToKeep));
        }
    }

    /**
     * Executes the full extraction process by scanning dataset directories under the "storm_tags/" root,
     * building dataset entries with CK and commit metrics, and aggregating them by release.
     * Finally, it enriches the collected data with additional metrics.
     * <p>
     * Errors during the process are caught and logged without interrupting execution.
     */
    public void executeExtractionProcess() {

        Map<Integer, List<DatasetEntry>> datasetEntryMap = new TreeMap<>();
        Path root = Path.of("storm_tags/");

        try (var stream = Files.list(root)) {

            for (Path directory : (Iterable<Path>) stream::iterator) {

                if (!Files.isDirectory(directory)) {
                    continue;
                }
                List<DatasetEntry> datasetEntries = populateCkMetrics(directory.toString());
                populateCommitMetrics(datasetEntries);
                datasetEntryMap.put(datasetEntries.getFirst().getRelease(), datasetEntries);
            }
            populateOtherMetrics(datasetEntryMap);
            Map<FixedBuggyTicket, Set<String>> linkedTickets = bindCommitsToTickets(getJiraTickets());
            startLabeling(linkedTickets, datasetEntryMap, calculateProportion(getJiraTickets()));
            DatasetDao<Map<Integer, List<DatasetEntry>>> dao = new CsvFileDao();
            dao.save(datasetEntryMap);

        } catch (Exception e) {
            log.error("Error executing extraction process\n Error message: {}\n Stacktrace: ", e.getMessage(), e);
        }
    }

    /**
     * Populates CK metrics for a given release path.
     * Note: The path should be in a format compatible with Windows file paths,
     * where the CkManagerExtractor expects to append a "\\" to create a path marker.
     * Example format: "storm_tags\\1_storm_0.9.0.1"
     *
     * @param releasePath The path to the release directory to analyze
     * @return A list of DatasetEntry objects containing CK metrics for each class
     */
    private List<DatasetEntry> populateCkMetrics(String releasePath) {

        MetricExtractor<String, List<DatasetEntry>> metricExtractor = new CkManagerExtractor();
        return metricExtractor.startAnalysis(releasePath);
    }

    /**
     * Populates Git-based commit metrics for each dataset entry by delegating the analysis
     * to {@link GitManagerExtractor}.
     *
     * @param datasetEntries list of dataset entries to enrich with commit metrics
     */
    private void populateCommitMetrics(List<DatasetEntry> datasetEntries) {

        MetricExtractor<List<DatasetEntry>, Void> metricExtractor = new GitManagerExtractor();
        metricExtractor.startAnalysis(datasetEntries);
    }

    /**
     * Enriches dataset entries with other related metrics.
     * Delegates the analysis to {@link OtherMetricsExtractor} and returns the updated dataset.
     *
     * @param datasetEntries map of dataset entries grouped by key (e.g., commit or version)
     */
    private void populateOtherMetrics(Map<Integer, List<DatasetEntry>> datasetEntries) {

        MetricExtractor<Map<Integer, List<DatasetEntry>>, Void> metricExtractor = new OtherMetricsExtractor();
        metricExtractor.startAnalysis(datasetEntries);
    }

    /**
     * Retrieves Jira tickets and saves in cache for proportion.
     */
    private List<FixedBuggyTicket> getJiraTickets() {

        List<ReleaseInfo> releases = new ReleaseInfoSearcher().getJiraReleases();

        for(int i = 0; i < releases.size(); i++) {
            LocalCache.addTotalRelease(releases.get(i).getReleaseName(), i);
        }
        return new TicketSearcher().getJiraFixedBuggyTickets(releases);
    }

    /**
     * Links each ticket to the set of Java classes modified by commits associated with its key.
     * Only tickets with at least one related class change are included in the result.
     *
     * @param tickets list of fixed/buggy tickets to process
     * @return map of tickets to the set of touched classes for matching commits
     */
    private Map<FixedBuggyTicket, Set<String>> bindCommitsToTickets(List<FixedBuggyTicket> tickets) {

        Map<FixedBuggyTicket, Set<String>> linkedCommits = new HashMap<>();

        for (FixedBuggyTicket ticket : tickets) {
            Set<String> classes = new GitWorktreeManager().getClassesTouchedByALinkedCommits(ticket.getKey());

            if(!classes.isEmpty()){
                linkedCommits.put(ticket, classes);
            }
        }
        return linkedCommits;
    }

    /**
     * Calculates the average proportion of affected versions across a set of
     * fixed buggy tickets using the proportion method.
     * <p>
     * Only tickets with a known affected version are considered. Tickets for
     * which the fix and opening versions coincide are ignored to avoid division
     * by zero.
     *
     * @param tickets the list of fixed buggy tickets
     * @return the average proportion value computed from the valid tickets
     */
    private float calculateProportion(List<FixedBuggyTicket> tickets) {

        int count = 0;
        float sum = 0;
        float p;

        for(FixedBuggyTicket ticket : tickets){

            if(!ticket.getAffectedVersion().equals("NOT FOUND")){
                int fix = LocalCache.getTotalReleaseValue(ticket.getFixVersion());
                int affected = LocalCache.getTotalReleaseValue(ticket.getAffectedVersion());
                int opening = LocalCache.getTotalReleaseValue(ticket.getOpeningVersion());

                if(fix-opening == 0){
                    continue;
                }
                p = (float) (fix - affected) /(fix-opening);
                sum += p;
                count++;
            }
        }
        sum /= count;
        return sum ;
    }

    /**
     * Labels dataset entries as buggy based on the affected and fix versions of each ticket.
     * For each ticket, all dataset entries in the version range [affected, fix) are marked as buggy.
     *
     * @param linkedClassesMap map of tickets associated with their linked classes
     * @param datasetEntryMap  map of release versions to dataset entries
     */
    private void startLabeling(Map<FixedBuggyTicket, Set<String>> linkedClassesMap, Map<Integer, List<DatasetEntry>> datasetEntryMap, float proportion){

        linkedClassesMap.forEach((key, value) -> {

            Couple<Integer, Integer> couple = new Couple<>();
            calculateBoundaries(key, couple, datasetEntryMap, proportion);

            if(couple.getA() != null){
                startLabeling(datasetEntryMap, value, couple.getA(), couple.getB());
            }
        });
    }

    /**
     * Calculates the start and end boundary indices for a buggy ticket.
     * The boundaries are derived either from affected/fix versions or, if missing,
     * from opening/fix versions using a proportional estimation.
     * <p>
     * The computed values are stored in the provided Couple object.
     *
     * @param key             ticket containing version information
     * @param couple          output object where start (A) and end (B) boundaries are stored
     * @param datasetEntryMap map used to determine dataset size fallback
     * @param proportion      factor used to estimate start position when version data is incomplete
     */
    private void calculateBoundaries(FixedBuggyTicket key, Couple<Integer, Integer> couple, Map<Integer, List<DatasetEntry>> datasetEntryMap,  float proportion) {

        Integer start = null;
        Integer end = null;

        if (!key.getAffectedVersion().equals("NOT FOUND")) {
            Integer affected = LocalCache.getReleaseValue(key.getAffectedVersion());

            if (affected != null) {  //I'm not over x %
                start = affected;
                Integer fix = LocalCache.getReleaseValue(key.getFixVersion());
                end = Objects.requireNonNullElseGet(fix, datasetEntryMap::size);
            }
        }
        else {

            Integer fix = LocalCache.getTotalReleaseValue(key.getFixVersion());
            Integer opening = LocalCache.getTotalReleaseValue(key.getOpeningVersion());

            if (fix != null && opening != null) {
                start = (int) (fix - (fix - opening) * proportion);

                if (start > datasetEntryMap.size()) {
                    start = null;
                }
                else if (start <= 0) {
                    start = 1;
                }
                end = Objects.requireNonNullElseGet(LocalCache.getReleaseValue(key.getFixVersion()), datasetEntryMap::size);
            }
        }
        couple.setA(start);
        couple.setB(end);
    }

    /**
     * Labels dataset entries as buggy within a given range of keys.
     * If the range includes only one key, processes that single entry list.
     *
     * @param datasetEntryMap map containing dataset entries grouped by integer keys
     * @param value           set of class paths used to determine buggy entries
     * @param start           starting key (inclusive)
     * @param end             ending key (exclusive)
     */
    private void startLabeling(Map<Integer, List<DatasetEntry>> datasetEntryMap, Set<String> value, Integer start, Integer end){

        if (start.intValue() == end.intValue()) {
            List<DatasetEntry> datasetEntries = datasetEntryMap.get(start);
            datasetEntries.forEach(e -> {

                if (value.contains(e.getClassPath())) {
                    e.setBuggy(true);
                }
            });
        }
        else {

            for (int i = start; i < end; i++) {
                List<DatasetEntry> datasetEntries = datasetEntryMap.get(i);
                datasetEntries.forEach(e -> {

                    if (value.contains(e.getClassPath())) {
                        e.setBuggy(true);
                    }
                });
            }
        }
    }
}
