package it.zbaldi.model.daos;

import it.zbaldi.model.data.DatasetEntry;
import it.zbaldi.model.interfaces.DatasetDao;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.util.List;
import java.util.Map;

@Slf4j
public class CsvFileDao implements DatasetDao<Map<Integer, List<DatasetEntry>>> {

    /**
     * Saves dataset entries into a CSV file.
     * <p>
     * Iterates over all {@link DatasetEntry} objects in the provided map
     * and writes them as rows in "dataset.csv".
     *
     * @param data map of dataset entries grouped by an integer key
     */
    @Override
    public void save(Map<Integer, List<DatasetEntry>> data) {

        log.info("Saving data to file dataset.csv");

        try (FileWriter out = new FileWriter("dataset.csv", false);
             CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT)) {
            printer.printRecord(
                    "class",
                    "release",
                    "loc",
                    "comment density",
                    "wcm",
                    "number of attributes",
                    "fan-in",
                    "fan-out",
                    "cbo",
                    "normalized lcom",
                    "rfc",
                    "creation age",
                    "last update age",
                    "total loc touched",
                    "release loc touched",
                    "normalized total churn",
                    "normalized release churn",
                    "total number of commits",
                    "release number of commits",
                    "total number of authors",
                    "release number of authors",
                    "total number of fixes",
                    "release number of fixes",
                    "average commit change density",
                    "number of smells",
                    "buggy"

            );

            for (List<DatasetEntry> datasetEntryList : data.values()) {

                for (DatasetEntry entry : datasetEntryList) {
                    printer.printRecord(
                            entry.getClassPath(),
                            entry.getRelease(),
                            entry.getLinesOfCode(),
                            entry.getCommentDensity(),
                            entry.getWeightedMethods(),
                            entry.getNumberOfAttributes(),
                            entry.getFanIn(),
                            entry.getFanOut(),
                            entry.getCouplingBetweenObjects(),
                            entry.getNormalizedLackOfCohesion(),
                            entry.getResponseForClass(),
                            entry.getCreationAge(),
                            entry.getLastUpdateAge(),
                            entry.getTotalLocTouched(),
                            entry.getReleaseLocTouched(),
                            entry.getNormalizedTotalChurn(),
                            entry.getNormalizedReleaseChurn(),
                            entry.getTotalNumberOfCommits(),
                            entry.getReleaseNumberOfCommits(),
                            entry.getTotalNumberOfAuthors(),
                            entry.getReleaseNumberOfAuthors(),
                            entry.getTotalNumberOfFixes(),
                            entry.getReleaseNumberOfFixes(),
                            entry.getAverageCommitChangeDensity(),
                            entry.getNumberOfSmells(),
                            entry.isBuggy()
                    );
                }
            }
            log.info("Saved data to file dataset.csv");

        }catch (Exception e){
            log.error("Error while saving dataset.csv, error message: {}", e.getMessage());
        }
    }
}
