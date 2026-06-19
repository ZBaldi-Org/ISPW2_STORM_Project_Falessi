package it.zbaldi.model.daos;

import it.zbaldi.model.DatasetDao;
import it.zbaldi.model.MlDatasetEntry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.util.Map;

@Slf4j
public class CsvWhatIfDao implements DatasetDao<Map<String, int[]>> {

    /**
     * Saves what-if analysis results to a CSV file.
     *
     * <p>The method writes each dataset entry with its corresponding
     * actual and predicted positive values into "what_if_results.csv".
     *
     * @param data map where key is dataset name and value is
     *             [actualPositives, predictedPositives]
     */
    @Override
    public void save(Map<String, int[]> data) {

        log.info("Saving data to file what_if_results.csv");

        try (FileWriter out = new FileWriter("what_if_results.csv", false);
             CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT)) {
            printer.printRecord(
                    "dataset",
                    "actual",
                    "predicted"
            );

            for (Map.Entry<String, int[]> entry : data.entrySet()) {
                printer.printRecord(
                            entry.getKey(),
                            entry.getValue()[0],
                            entry.getValue()[1]
                    );
            }
            log.info("Saved data to file what_if_results.csv");

        }catch (Exception e){
            log.error("Error while saving what_if_results.csv, error message: {}", e.getMessage());
        }
    }
}
