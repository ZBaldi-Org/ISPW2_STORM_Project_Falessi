package it.zbaldi.model.daos;

import it.zbaldi.model.DatasetDao;
import it.zbaldi.model.MlDatasetEntry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.util.List;
import java.util.Map;

@Slf4j
public class CsvMlDao implements DatasetDao<Map<Integer, List<MlDatasetEntry>>> {

    /**
     * Saves dataset entries into a CSV file.
     * <p>
     * Iterates over all {@link MlDatasetEntry} objects in the provided map
     * and writes them as rows in "ml_results.csv".
     *
     * @param data map of ml dataset entries grouped by an integer key
     */
    @Override
    public void save(Map<Integer, List<MlDatasetEntry>> data) {

        log.info("Saving data to file ml_results.csv");

        try (FileWriter out = new FileWriter("ml_results.csv", false);
             CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT)) {
            printer.printRecord(
                    "model",
                    "feature selection",
                    "balancing",
                    "accuracy",
                    "precision",
                    "recall",
                    "f1",
                    "auc",
                    "kappa"
            );

            for (List<MlDatasetEntry> mlDatasetEntries : data.values()) {

                for (MlDatasetEntry entry : mlDatasetEntries) {
                    printer.printRecord(
                            entry.getModelName(),
                            entry.isFeatureSelection(),
                            entry.isBalancing(),
                            entry.getAccuracy(),
                            entry.getPrecision(),
                            entry.getRecall(),
                            entry.getF1(),
                            entry.getAuc(),
                            entry.getKappa()
                    );
                }
            }
            log.info("Saved data to file ml_results.csv");

        }catch (Exception e){
            log.error("Error while saving ml_results.csv, error message: {}", e.getMessage());
        }
    }
}
