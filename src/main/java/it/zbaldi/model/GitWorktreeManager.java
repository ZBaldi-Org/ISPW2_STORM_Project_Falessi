package it.zbaldi.model;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
public class GitWorktreeManager {

    /** Git repository URL of Apache Storm. */
    private final String URL = "https://github.com/apache/storm.git";

    /** Local directory name used for cloning the repository. */
    private final String PROJECT_NAME = "storm";

    /**
     * Builds Git worktrees for a list of releases.
     * <p>
     * The method first clones the repository, then iterates over all releases
     * and creates a worktree for each release tag.
     * <p>
     * If a tag is invalid, it retries using the "v" prefix (e.g. v1.0.0).
     *
     * @param releaseInfoList list of release metadata used to create worktrees
     */
    public void buildWorktree(List<ReleaseInfo> releaseInfoList) {

        try {
            int a = 0;
            cloneRepo();
            int i=1;

            for (ReleaseInfo releaseInfo : releaseInfoList) {

                try {
                    createSnapshot(releaseInfo.getReleaseName(), i);

                } catch (GitException e) {
                    log.warn("{} Retrying with v{} tag", e.getMessage(), releaseInfo.getReleaseName());
                    createSnapshot("v" + releaseInfo.getReleaseName(), i);
                }
                i++;
            }

        } catch (Exception e) {
            log.error(e.getMessage());
        }
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

    /**
     * Creates a Git snapshot of the repository using a specific tag via `git worktree`.
     * <p>
     * The snapshot is stored in a separate directory named:
     * PROJECT_NAME_tag.
     * <p>
     * The method executes:
     * git worktree add <targetDir> <tag>
     * <p>
     * If the tag is invalid or does not exist in the local repository,
     * a GitException is thrown. Other Git errors result in a generic Exception.
     *
     * @param tag the Git tag used to create the snapshot
     * @throws GitException if the tag is invalid or not found
     * @throws Exception    for general Git execution errors
     */
    private void createSnapshot(String tag, int order) throws Exception {

        System.out.println(tag);

        Path path = Paths.get(System.getProperty("user.dir"));
        Path targetDir = path.resolve("storm_tags").resolve(order + "_" + PROJECT_NAME + "_" + tag);
        ProcessBuilder pb = new ProcessBuilder("git", "worktree", "add", targetDir.toString(), tag);
        Path base = Paths.get(PROJECT_NAME).toAbsolutePath().normalize();
        pb.directory(base.toFile());
        Process process = pb.start();
        int exitCode = process.waitFor();
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);

        if (exitCode != 0) {

            if (stderr.contains("fatal: invalid reference:")) {
                throw new GitException("Invalid Tag: " + tag);
            } else {
                throw new Exception("Error creating worktree for tag: " + tag);
            }
        }
    }
}
