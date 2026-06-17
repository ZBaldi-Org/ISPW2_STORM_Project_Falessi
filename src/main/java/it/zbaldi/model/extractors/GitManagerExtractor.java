package it.zbaldi.model.extractors;

import it.zbaldi.model.DatasetEntry;
import it.zbaldi.model.GitException;
import it.zbaldi.model.MetricExtractor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class GitManagerExtractor implements MetricExtractor<List<DatasetEntry>, List<DatasetEntry>> {

    /**
     * Performs static analysis on a list of dataset entries by extracting Git-based evolution metrics
     * for each class in the dataset.
     * <p>
     * For each {@link DatasetEntry}, the method:
     * - Retrieves the Git commit history of the corresponding class
     * - Computes change-related metrics (total LOC touched, average commit change density,
     * and normalized total churn)
     * - Computes structural and evolutionary metrics such as:
     * - Total number of commits affecting the class
     * - Total number of distinct authors contributing to the class
     * - Total number of fix-related commits
     * <p>
     * The analysis is performed by opening the Git repository once and reusing it across all entries.
     *
     * @param datasetEntries list of dataset entries representing classes to analyze
     * @return the same list of dataset entries enriched with computed metrics
     */
    @Override
    public List<DatasetEntry> startAnalysis(List<DatasetEntry> datasetEntries) {

        String directory = datasetEntries.getFirst().getRelativeClassPath();
        File repoDir = new File(buildRepositoryPath(directory));

        try (Git git = Git.open(repoDir)) {

            for (DatasetEntry entry : datasetEntries) {
                log.info("Calculating Commit Metrics for; {}", entry.getClassPath());
                List<RevCommit> commits = getClassCommits(git, entry);
                setTotalLocTouchedAndAverageCommitChangeDensityAndNormalizedTotalChurn(entry);
                setTotalNumberOfCommits(entry, commits);
                setTotalNumberOfAuthors(entry, commits);
                setTotalNumberOfFixes(entry, commits);
            }
            log.info("Finished Calculating Commit Metrics");

        } catch (GitException e) {
            log.error(e.getMessage());

        } catch (Exception e) {
            log.error("Error extracting commit metrics");
        }
        return datasetEntries;
    }

    /**
     * Builds a repository path from a directory string by taking the first two path components.
     * This is used to construct the base path for accessing a Git repository.
     *
     * @param directory A path string split by backslashes (e.g., "storm_tags\\1_storm_0.9.0.1\\some\\path")
     * @return A string containing the first two path components joined by a backslash (e.g., "storm_tags\\1_storm_0.9.0.1")
     */
    private String buildRepositoryPath(String directory) {

        String[] parts = directory.split("\\\\");
        return parts[0] + "\\" + parts[1];
    }

    /**
     * Retrieves the commit history for a specific class in the Git repository.
     *
     * @param git          The Git repository object to query
     * @param datasetEntry The dataset entry containing the class path information
     * @return A list of RevCommit objects representing the commit history for the class
     * @throws GitAPIException if an error occurs while accessing the Git repository
     */
    private List<RevCommit> getClassCommits(Git git, DatasetEntry datasetEntry) throws GitAPIException {

        String path = datasetEntry.getClassPath();
        path = path.replace("\\", "/");
        Iterable<RevCommit> commits = git.log().addPath(path).call();
        List<RevCommit> history = new ArrayList<>();

        for (RevCommit commit : commits) {
            history.add(commit);
        }
        return history;
    }

    /**
     * Calculates and sets change-related metrics for a class by analyzing its Git commit history
     * using 'git log --numstat'.
     * <p>
     * The following metrics are computed:
     * - Total lines of code touched (added + deleted) across the entire history of the class
     * - Average change per commit (commit change density), representing how much the class changes on average per commit
     * - Normalized total churn, defined as total lines touched divided by the total lines of code of the class
     * <p>
     * These metrics capture both the absolute and relative evolution of the class over time.
     *
     * @param datasetEntry The dataset entry to update with computed metrics
     * @throws Exception if an error occurs while executing the git command or processing its output
     */
    private void setTotalLocTouchedAndAverageCommitChangeDensityAndNormalizedTotalChurn(DatasetEntry datasetEntry) throws Exception {

        int totalAdded = 0;
        int totalDeleted = 0;
        Path processPath = Paths.get(buildRepositoryPath(datasetEntry.getRelativeClassPath())).toAbsolutePath().normalize();
        String path = datasetEntry.getClassPath();
        path = path.replace("\\", "/");

        ProcessBuilder pb = new ProcessBuilder(
                "git",
                "log",
                "--numstat",
                "--pretty=format:",
                "--",
                path
        );
        pb.directory(processPath.toFile());
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new GitException("Error retrieving class commits for: " + path);
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        int counter = 0;

        while ((line = reader.readLine()) != null) {

            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.trim().split("\\s+");

            if (parts.length == 3) {
                String addedStr = parts[0];
                String deletedStr = parts[1];
                int added = addedStr.equals("-") ? 0 : Integer.parseInt(addedStr);
                int deleted = deletedStr.equals("-") ? 0 : Integer.parseInt(deletedStr);
                totalAdded += added;
                totalDeleted += deleted;
                counter++;
            }
        }
        float average = (float) (totalAdded + totalDeleted) / counter;
        float averageCommitChangeDensity = average / datasetEntry.getLinesOfCode();
        float normalizedTotalChurn = (float) (totalAdded + totalDeleted) / datasetEntry.getLinesOfCode();
        datasetEntry.setAverageCommitChangeDensity(averageCommitChangeDensity);
        datasetEntry.setTotalLocTouched(totalAdded + totalDeleted);
        datasetEntry.setNormalizedTotalChurn(normalizedTotalChurn);
    }

    /**
     * Sets the total number of commits for a class.
     *
     * @param datasetEntry The dataset entry to update
     * @param commits      The list of commits for the class
     */
    private void setTotalNumberOfCommits(DatasetEntry datasetEntry, List<RevCommit> commits) {

        datasetEntry.setTotalNumberOfCommits(commits.size());
    }

    /**
     * Calculates and sets the total number of unique authors who have committed to a class.
     *
     * @param datasetEntry The dataset entry to update
     * @param commits      The list of commits to analyze for authors
     */
    private void setTotalNumberOfAuthors(DatasetEntry datasetEntry, List<RevCommit> commits) {

        Set<String> authors = new HashSet<>();
        List<String> authorsHistory = new ArrayList<>();
        for (RevCommit commit : commits) {
            String author = commit.getAuthorIdent().getName();
            authors.add(author);
            authorsHistory.add(author);
        }
        datasetEntry.setTotalNumberOfAuthors(authors.size());
        datasetEntry.setAuthorsHistoryList(authorsHistory);
    }

    /**
     * Counts and sets the number of commits considered as bug fixes based on commit messages.
     *
     * @param datasetEntry the dataset entry to update
     * @param commits      list of commits to analyze
     */
    private void setTotalNumberOfFixes(DatasetEntry datasetEntry, List<RevCommit> commits) {

        int fixNumber = 0;
        for (RevCommit commit : commits) {
            String commitMessage = commit.getFullMessage();

            if (commitMessage.toLowerCase().contains("fix")) {
                fixNumber += 1;
            }
        }
        datasetEntry.setTotalNumberOfFixes(fixNumber);
    }
}
