package it.zbaldi.controller;

import it.zbaldi.model.GitWorktreeManager;
import it.zbaldi.model.ReleaseInfo;
import it.zbaldi.model.ReleaseInfoSearcher;

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
}
