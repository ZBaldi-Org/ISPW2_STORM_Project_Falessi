package it.zbaldi.model;

import it.zbaldi.model.data.ReleaseInfo;
import it.zbaldi.model.exceptions.GenericException;
import it.zbaldi.model.exceptions.GitException;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class GitWorktreeManager {

    /** Git repository URL of Apache Storm. */
    private static final String URL = "https://github.com/apache/storm.git";

    /** Local directory name used for cloning the repository. */
    private static final String PROJECT_NAME = "storm";

    /** Local directory name where are saved worktrees. */
    private static final String PATH = "storm_tags";

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
            cloneRepo();
            int i=1;

            for (ReleaseInfo releaseInfo : releaseInfoList) {
                LocalCache.addRelease(releaseInfo.getReleaseName(), i);
                createSnapshot(releaseInfo, i);
            }

        } catch (GenericException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Creates a snapshot for the specified release.
     * If the release tag is not found, retries using the same tag prefixed with {@code v}.
     *
     * @param releaseInfo release information containing the release name
     * @param i           snapshot index
     * @throws GenericException if snapshot creation fails
     */
    private void createSnapshot(ReleaseInfo releaseInfo, int i) throws GenericException {

        try {
            createSnapshot(releaseInfo.getReleaseName(), i);

        } catch (GitException e) {
            log.warn("{} Retrying with v{} tag", e.getMessage(), releaseInfo.getReleaseName());
            createSnapshot("v" + releaseInfo.getReleaseName(), i);
        }
    }

    /**
     * Clones a Git repository from the given URL using a system git command.
     *
     * @throws GenericException if the clone operation fails
     */
    private void cloneRepo() throws GenericException {

        try {
            ProcessBuilder pb = new ProcessBuilder("git", "clone", URL);
            pb.inheritIO();
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new GitException("Error Cloning Repository: " + URL);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GenericException("Error Cloning Repository: " + URL + " Message: " + e.getMessage());

        } catch (Exception e) {
            throw new GenericException("Error Cloning Repository: " + URL + " Message: " + e.getMessage());
        }
    }

    /**
     * Creates a Git worktree snapshot for the given tag.
     *
     * @param tag   the Git tag
     * @param order snapshot order used in directory naming
     * @throws GenericException if the tag is invalid or not found
     */
    private void createSnapshot(String tag, int order) throws GenericException{

        try {
            Path path = Paths.get(System.getProperty("user.dir"));
            Path targetDir = path.resolve(PATH).resolve(order + "_" + PROJECT_NAME + "_" + tag);
            ProcessBuilder pb = new ProcessBuilder("git", "worktree", "add", targetDir.toString(), tag);
            Path base = Paths.get(PROJECT_NAME).toAbsolutePath().normalize();
            pb.directory(base.toFile());
            Process process = pb.start();
            int exitCode = process.waitFor();
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);

            if (exitCode != 0 && stderr.contains("fatal: invalid reference:")) {
                throw new GitException("Invalid Tag: " + tag);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GenericException("Error Creating Worktree For Tag: " + tag + " Message: " + e.getMessage());

        } catch (Exception e) {
            throw new GenericException("Error Creating Worktree For Tag: " + tag + " Message: " + e.getMessage());
        }
    }

    /**
     * Retrieves all Java classes affected by commits linked to a specific identifier.
     * <p>
     * This method resolves the target repository directory based on the current release
     * and project configuration, then delegates the retrieval of touched classes to
     * {@code getAllClasses}.
     *
     * @param id identifier used to filter linked Git commits
     * @return set of Java class paths touched by matching commits, or an empty set on error
     */
    public Set<String> getClassesTouchedByALinkedCommits(String id) {

        log.info("Getting Classes Touched By A Linked Commit {}", id);
        int release = LocalCache.getReleaseSize();
        Path path = Paths.get(System.getProperty("user.dir"));
        Path targetDir = path.resolve(PATH).resolve(release + "_" + PROJECT_NAME + "_" + LocalCache.getReleaseKey(release));

        if (!Files.exists(targetDir)) {
            targetDir = path.resolve(PATH).resolve(release + "_" + PROJECT_NAME + "_v" + LocalCache.getReleaseKey(release));
        }

        try {
            return getAllClasses(id, targetDir.toString());

        } catch (GenericException e) {
            log.error(e.getMessage());
            return  Collections.emptySet();
        }
    }

    /**
     * Returns all Java files modified in commits matching a given Git grep tag.
     *
     * @param id        commit message keyword/tag used for filtering
     * @param targetDir repository directory
     * @return set of touched .java file paths
     * @throws GenericException if the tag is invalid or not found
     */
    private Set<String> getAllClasses(String id, String targetDir) throws GenericException {

        try {
            Path processPath = Paths.get(targetDir).toAbsolutePath().normalize();

            ProcessBuilder pb = new ProcessBuilder(
                    "git", "log",
                    "--all",
                    "-i",
                    "--grep=" + id,
                    "--name-only",
                    "--pretty=format:"
            );
            pb.directory(processPath.toFile());
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new GitException("Error Retrieving Classes Touched By Commits With Tag: " + id);
            }
            Set<String> classes = new HashSet<>();

            for (String line : output.split("\n")) {
                line = line.trim();

                if (line.isBlank()) {
                    continue;
                }

                if (line.endsWith(".java")) {
                    classes.add(line.replace('/', '\\'));
                }
            }
            log.info("Got {} Classes Touched By Commits With Tag {}", classes.size(), id);
            return classes;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GenericException("Error Retrieving Classes Touched By Commits With Tag: " + id + " Message: " + e.getMessage());

        } catch (Exception e) {
            throw new GenericException("Error Retrieving Classes Touched By Commits With Tag: "+ id + " Message: "+ e.getMessage());
        }
    }
}
