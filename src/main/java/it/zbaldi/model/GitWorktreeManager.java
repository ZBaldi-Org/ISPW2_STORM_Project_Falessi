package it.zbaldi.model;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

@Slf4j
public class GitWorktreeManager {

    private final String URL = "https://github.com/apache/storm.git";
    private final String PROJECT_NAME = "storm";

    public void buildWorktree(List<ReleaseInfo> releaseInfoList) {

        try {
            cloneRepo();

        } catch (Exception e) {
            log.error(e.getMessage());
        }

        releaseInfoList.forEach(releaseInfo -> {
            String tag = releaseInfo.getReleaseName();

            try {
                ProcessBuilder pb = new ProcessBuilder(
                        "git",
                        "worktree",
                        "add",
                        PROJECT_NAME + "_" + tag,
                        tag
                );

                // 🔥 IMPORTANTISSIMO: esegui il comando dentro la repo git
                pb.directory(repoDir);

                pb.inheritIO();

                Process process = pb.start();

                int exitCode = process.waitFor();

                if (exitCode != 0) {
                    throw new RuntimeException("Error creating worktree for " + tag);
                }

            } catch (Exception e) {
                throw new RuntimeException("Error on tag " + tag, e);
            }
        });
    }

    /**
     * Clones a Git repository from the given URL using a system git command.
     * Throws an exception if the clone operation fails.
     *
     * @throws Exception if the git clone process fails
     */
    private void cloneRepo() throws Exception {

        ProcessBuilder pb = new ProcessBuilder("git", "clone", URL);
        pb.inheritIO();
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new Exception("Error cloning repository: " + URL);
        }
    }
}
