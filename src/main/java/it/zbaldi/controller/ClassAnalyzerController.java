package it.zbaldi.controller;

import it.zbaldi.model.*;
import it.zbaldi.model.extractors.CkManagerExtractor;
import it.zbaldi.model.extractors.GitManagerExtractor;
import it.zbaldi.model.extractors.OtherMetricsExtractor;
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
                datasetEntries = populateCommitMetrics(datasetEntries);
                datasetEntryMap.put(datasetEntries.getFirst().getRelease(), datasetEntries);
            }
            populateOtherMetrics(datasetEntryMap);
            Map<FixedBuggyTicket, Set<String>> linkedTickets = bindCommitsToTickets(getJiraTickets());
            //APPLICARE PROPORTION O NO

        } catch (Exception e) {

            log.error("Error executing extraction process, error message: {}", e.getMessage());
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
     * @return list of dataset entries enriched with Git-based metrics
     */
    private List<DatasetEntry> populateCommitMetrics(List<DatasetEntry> datasetEntries) {

        MetricExtractor<List<DatasetEntry>, List<DatasetEntry>> metricExtractor = new GitManagerExtractor();
        return metricExtractor.startAnalysis(datasetEntries);
    }

    /**
     * Enriches dataset entries with other related metrics.
     * Delegates the analysis to {@link OtherMetricsExtractor} and returns the updated dataset.
     *
     * @param datasetEntries map of dataset entries grouped by key (e.g., commit or version)
     * @return updated map containing computed commit metrics
     */
    private Map<Integer, List<DatasetEntry>> populateOtherMetrics(Map<Integer, List<DatasetEntry>> datasetEntries) {

        MetricExtractor<Map<Integer, List<DatasetEntry>>, Map<Integer, List<DatasetEntry>>> metricExtractor = new OtherMetricsExtractor();
        return metricExtractor.startAnalysis(datasetEntries);
    }

    /**
     * Retrieves Jira tickets and filters out invalid ones (missing fix version or migrated data).
     */
    private List<FixedBuggyTicket> getJiraTickets() {

        List<FixedBuggyTicket> tickets = new TicketSearcher().getJiraFixedBuggyTickets(new ReleaseInfoSearcher().getJiraReleases());
        tickets.removeIf(fix -> fix.getFixVersion().equals("NOT FOUND") || fix.getOpeningVersion().equals("DATA MIGRATED"));
        return tickets;
    }

    private Map<FixedBuggyTicket, Set<String>> bindCommitsToTickets(List<FixedBuggyTicket> tickets) {

        Map<FixedBuggyTicket, Set<String>> linkedCommits = new HashMap<>();

        for (FixedBuggyTicket ticket : tickets) {
            Set<String> classes = new GitWorktreeManager().getClassesTouchedByALinkedCommits(ticket.getKey());

            if(!classes.isEmpty()){
                linkedCommits.put(ticket, classes);;
            }
        }
        return linkedCommits;
    }

    public void prova(){

        getCodeSnapshots(0.05F);
        Map<FixedBuggyTicket, Set<String>> linkedTickets = bindCommitsToTickets(getJiraTickets());
        int a = 0;
    }
}
