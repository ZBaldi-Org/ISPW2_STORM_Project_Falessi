package it.zbaldi.model.extractors;

import com.github.javaparser.JavaParser;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.comments.Comment;
import it.zbaldi.model.DatasetEntry;
import it.zbaldi.model.MetricExtractor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class OtherMetricsExtractor implements MetricExtractor<Map<Integer, List<DatasetEntry>>, Map<Integer, List<DatasetEntry>>> {

    /**
     * Processes dataset releases by computing metrics for each release.
     *
     * @param map a map where the key is the release index and the value is the list of DatasetEntry
     * @return the updated map with computed metrics for all releases
     */
    @Override
    public Map<Integer, List<DatasetEntry>> startAnalysis(Map<Integer, List<DatasetEntry>> map) {

        map.keySet().forEach(i -> {

            if (i == 1) {
                setNewClassOtherMetrics(map.get(i));
            }
            else {
                setOtherMetrics(map.get(i - 1), map.get(i));
            }
        });
        return map;
    }

    /**
     * Initializes metrics for all entries representing new classes in a release.
     *
     * @param datasetEntries list of DatasetEntry objects to initialize
     */
    private void setNewClassOtherMetrics(List<DatasetEntry> datasetEntries) {

        datasetEntries.forEach(this::setNewClassOtherMetrics);
    }

    /**
     * Initializes metrics for a single DatasetEntry representing a new class.
     *
     * @param datasetEntry the DatasetEntry to initialize
     */
    private void setNewClassOtherMetrics(DatasetEntry datasetEntry) {

        datasetEntry.setCreationAge(1);
        datasetEntry.setLastUpdateAge(0);
        datasetEntry.setReleaseLocTouched(datasetEntry.getTotalLocTouched());
        datasetEntry.setNormalizedReleaseChurn(datasetEntry.getNormalizedTotalChurn());
        datasetEntry.setReleaseNumberOfCommits(datasetEntry.getTotalNumberOfCommits());
        datasetEntry.setReleaseNumberOfAuthors(datasetEntry.getTotalNumberOfAuthors());
        datasetEntry.setReleaseNumberOfFixes(datasetEntry.getTotalNumberOfFixes());
        setCommentDensity(datasetEntry);
        setNumberOfSmells(datasetEntry);
    }

    /**
     * Computes and updates release metrics for each DatasetEntry in the new release,
     * comparing them with the corresponding entry from the previous release.
     * <p>
     * If no matching entry is found, the entry is treated as a new class and initialized
     * using default values.
     *
     * @param datasetEntriesOld list of DatasetEntry objects from the previous release
     * @param datasetEntriesNew list of DatasetEntry objects from the current release
     */
    private void setOtherMetrics(List<DatasetEntry> datasetEntriesOld, List<DatasetEntry> datasetEntriesNew) {

        datasetEntriesNew.forEach(datasetEntryNew -> {
            DatasetEntry datasetEntryOld = searchOldDatasetEntry(datasetEntryNew.getClassPath(), datasetEntriesOld);

            if (datasetEntryOld == null) {
                setNewClassOtherMetrics(datasetEntryNew);
            }
            else {
                datasetEntryNew.setCreationAge(datasetEntryOld.getCreationAge() + 1);
                datasetEntryNew.setReleaseLocTouched(datasetEntryNew.getTotalLocTouched() - datasetEntryOld.getTotalLocTouched());
                float normalizedReleaseChurn = (float) datasetEntryNew.getReleaseLocTouched() / datasetEntryNew.getLinesOfCode();
                datasetEntryNew.setNormalizedReleaseChurn(normalizedReleaseChurn);
                datasetEntryNew.setReleaseNumberOfCommits(datasetEntryNew.getTotalNumberOfCommits() - datasetEntryOld.getTotalNumberOfCommits());
                List<String> authorsHistoryNew = datasetEntryNew.getAuthorsHistoryList();
                List<String> authorsHistoryOld = datasetEntryOld.getAuthorsHistoryList();
                List<String> subAuthorsList = authorsHistoryNew.subList(authorsHistoryOld.size(), authorsHistoryNew.size());
                Set<String> releaseAuthors = new HashSet<>(subAuthorsList);
                datasetEntryNew.setReleaseNumberOfAuthors(releaseAuthors.size());
                datasetEntryNew.setReleaseNumberOfFixes(datasetEntryNew.getTotalNumberOfFixes() - datasetEntryOld.getTotalNumberOfFixes());

                if (datasetEntryNew.getReleaseLocTouched() > 0) {
                    datasetEntryNew.setLastUpdateAge(0);
                }
                else {
                    datasetEntryNew.setLastUpdateAge(datasetEntryOld.getLastUpdateAge() + 1);
                }
                setCommentDensity(datasetEntryNew);
                setNumberOfSmells(datasetEntryNew);
            }

        });
    }

    /**
     * Computes the comment density of a Java file and stores it in the dataset entry.
     * Density is defined as comment lines divided by total lines of code.
     *
     * @param datasetEntry dataset entry containing file path and metrics
     */
    private void setCommentDensity(DatasetEntry datasetEntry) {

        try {
            File file = new File(datasetEntry.getRelativeClassPath());
            CompilationUnit compilationUnit = StaticJavaParser.parse(file);
            var comments = compilationUnit.getAllContainedComments();
            int totalLines = 0;
            int lines;
            long totalLocs = Files.lines(Path.of(datasetEntry.getRelativeClassPath())).filter(line -> !line.trim().isEmpty()).filter(line -> !line.trim().startsWith("//")).filter(line -> !line.trim().startsWith("/*")).count();

            for (Comment c : comments) {
                lines = c.getBegin().flatMap(begin -> c.getEnd().map(end -> end.line - begin.line + 1)).orElse(0);
                totalLines += lines;
            }
            float density = (float) totalLines / totalLocs;
            datasetEntry.setCommentDensity(density);

        } catch (Exception e) {
            log.error("Error while calculating comment density of file {} ", datasetEntry.getRelativeClassPath());
            datasetEntry.setCommentDensity(0);
        }
    }

    private void setNumberOfSmells(DatasetEntry datasetEntry){


    }

    /**
     * Finds the previous DatasetEntry matching the given class path.
     *
     * @param classPath         the class path to search for
     * @param datasetEntriesOld list of previous DatasetEntry objects
     * @return the matching DatasetEntry, or null if not found
     */
    private DatasetEntry searchOldDatasetEntry(String classPath, List<DatasetEntry> datasetEntriesOld) {

        for (DatasetEntry datasetEntryOld : datasetEntriesOld) {

            String classPathOld = datasetEntryOld.getClassPath();

            if (classPath.equals(classPathOld)) {
                return datasetEntryOld;
            }
        }
        return null;
    }
}
