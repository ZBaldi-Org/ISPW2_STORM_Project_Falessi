package it.zbaldi.controller;

import it.zbaldi.model.DatasetEntry;
import it.zbaldi.model.GitWorktreeManager;
import it.zbaldi.model.ReleaseInfo;
import it.zbaldi.model.ReleaseInfoSearcher;
import it.zbaldi.model.extractors.CkManagerExtractor;
import it.zbaldi.model.extractors.GitManagerExtractor;

import java.util.List;

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

    public void prova() {
        new GitManagerExtractor().startAnalysis(new CkManagerExtractor().startAnalysis("storm_tags\\1_storm_0.9.0.1"));
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

        return new CkManagerExtractor().startAnalysis(releasePath);
    }

    /**
     * Populates Git-based commit metrics for each dataset entry by delegating the analysis
     * to {@link GitManagerExtractor}.
     *
     * @param datasetEntries list of dataset entries to enrich with commit metrics
     * @return list of dataset entries enriched with Git-based metrics
     */
    private List<DatasetEntry> populateCommitMetrics(List<DatasetEntry> datasetEntries) {

        return new GitManagerExtractor().startAnalysis(datasetEntries);
    }
}
