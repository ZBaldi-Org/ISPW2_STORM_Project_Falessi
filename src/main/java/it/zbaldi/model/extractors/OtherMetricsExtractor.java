package it.zbaldi.model.extractors;

import it.zbaldi.model.DatasetEntry;
import it.zbaldi.model.MetricExtractor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.pmd.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class OtherMetricsExtractor implements MetricExtractor<Map<Integer, List<DatasetEntry>>, Void> {

    /** PMD configuration used for loading rule sets. */
    private final PMDConfiguration CONFIGURATION = new PMDConfiguration();

    /** RuleSetLoader created from the PMD configuration. */
    private final RuleSetLoader LOADER = RuleSetLoader.fromPmdConfig(CONFIGURATION);

    /** The primary PMD rule set (quickstart) loaded from resources. */
    private final RuleSet RULESET = LOADER.loadFromResource("rulesets/java/quickstart.xml");

//    private final RuleSet RULESET2 = LOADER.loadFromResource("rulesets/java/design.xml");
//    private final RuleSet RULESET3 = LOADER.loadFromResource("category/java/bestpractices.xml");
//    private final RuleSet RULESET4 = LOADER.loadFromResource("category/java/errorprone.xml");

    /** Wrapper containing the selected rule set(s) for PMD analysis. */
    @SuppressWarnings({"deprecation"})
    private final RuleSets RULESETS = new RuleSets(List.of(RULESET));

    /**
     * Processes dataset releases by computing metrics for each release.
     *
     * @param map a map where the key is the release index and the value is the list of DatasetEntry
     * @return the updated map with computed metrics for all releases
     */
    @Override
    public Void startAnalysis(Map<Integer, List<DatasetEntry>> map) {

        map.keySet().forEach(i -> {

            if (i == 1) {
                setNewClassOtherMetrics(map.get(i));
            }
            else {
                setOtherMetrics(map.get(i - 1), map.get(i));
            }
        });
        log.info("Finished Calculating Other Metrics");
        return null;
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

        log.info("Calculating Other Metrics For: {}", datasetEntry.getClassPath());
        datasetEntry.setCreationAge(1);
        datasetEntry.setLastUpdateAge(0);
        datasetEntry.setReleaseLocTouched(datasetEntry.getTotalLocTouched());
        datasetEntry.setNormalizedReleaseChurn(datasetEntry.getNormalizedTotalChurn());
        datasetEntry.setReleaseNumberOfCommits(datasetEntry.getTotalNumberOfCommits());
        datasetEntry.setReleaseNumberOfAuthors(datasetEntry.getTotalNumberOfAuthors());
        datasetEntry.setReleaseNumberOfFixes(datasetEntry.getTotalNumberOfFixes());
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
                log.info("Calculating Other Metrics For: {}", datasetEntryNew.getClassPath());
                datasetEntryNew.setCreationAge(datasetEntryOld.getCreationAge() + 1);
                datasetEntryNew.setReleaseLocTouched(Math.max(datasetEntryNew.getTotalLocTouched() - datasetEntryOld.getTotalLocTouched(), 0));
                float normalizedReleaseChurn = (float) datasetEntryNew.getReleaseLocTouched() / datasetEntryNew.getLinesOfCode();
                datasetEntryNew.setNormalizedReleaseChurn(normalizedReleaseChurn);
                datasetEntryNew.setReleaseNumberOfCommits(Math.max(datasetEntryNew.getTotalNumberOfCommits() - datasetEntryOld.getTotalNumberOfCommits(), 0));
                List<String> authorsHistoryNew = datasetEntryNew.getAuthorsHistoryList();
                List<String> authorsHistoryOld = datasetEntryOld.getAuthorsHistoryList();
                Set<String> releaseAuthors;

                if(authorsHistoryOld.size() <= authorsHistoryNew.size()){
                    List<String> subAuthorsList = authorsHistoryNew.subList(authorsHistoryOld.size(), authorsHistoryNew.size());
                    releaseAuthors = new HashSet<>(subAuthorsList);
                }
                else{ //IF NEW < OLD COMMIT HISTORY CORRUPTED, SEE ONLY THE DIFFERENCE OF AUTHORS AND ASSIGN THAT
                    Set<String> authorsHistoryNewSet = new HashSet<>(authorsHistoryNew);
                    Set<String> authorsHistoryOldSet = new HashSet<>(authorsHistoryOld);
                    authorsHistoryNewSet.removeAll(authorsHistoryOldSet);
                    releaseAuthors = new HashSet<>(authorsHistoryNewSet);
                }
                datasetEntryNew.setReleaseNumberOfAuthors(releaseAuthors.size());
                datasetEntryNew.setReleaseNumberOfFixes(Math.max(datasetEntryNew.getTotalNumberOfFixes() - datasetEntryOld.getTotalNumberOfFixes(), 0));

                if (datasetEntryNew.getReleaseLocTouched() > 0) {
                    datasetEntryNew.setLastUpdateAge(0);
                }
                else {
                    datasetEntryNew.setLastUpdateAge(datasetEntryOld.getLastUpdateAge() + 1);
                }
                setNumberOfSmells(datasetEntryNew);
            }

        });
    }

    /**
     * Calculates the number of PMD code smells (violations) for a given Java class
     * and stores the result in the provided {@link DatasetEntry}.
     *
     * <p>The method runs a PMD analysis using multiple rule sets (quickstart, design,
     * best practices, and error-prone) and counts all detected violations as "smells".
     *
     * <p>If an error occurs during analysis (e.g., file not found or parsing issue),
     * the number of smells is set to 0 and the error is logged.
     *
     * @param datasetEntry the dataset entry containing the relative path of the class
     *                     to be analyzed and where the computed smell count will be stored
     */
    @SuppressWarnings({"deprecation"})
    private void setNumberOfSmells(DatasetEntry datasetEntry) {

        try {
            Report report = new Report();
            RuleContext context = new RuleContext();
            context.setReport(report);
            SourceCodeProcessor processor = new SourceCodeProcessor(CONFIGURATION);
            Path path = Paths.get(datasetEntry.getRelativeClassPath());
            InputStream is = Files.newInputStream(path);
            processor.processSourceCode(is, RULESETS, context);
            datasetEntry.setNumberOfSmells(report.getViolations().size());

        }catch (Exception e){
            log.error("Error while calculating number of smells of file {} ", datasetEntry.getRelativeClassPath());
            datasetEntry.setNumberOfSmells(0);
        }
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
