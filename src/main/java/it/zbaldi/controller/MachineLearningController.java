package it.zbaldi.controller;

import it.zbaldi.model.DatasetDao;
import it.zbaldi.model.MlDatasetEntry;
import it.zbaldi.model.WekaManager;
import it.zbaldi.model.daos.CsvMlDao;
import it.zbaldi.model.daos.CsvWhatIfDao;
import it.zbaldi.model.enums.MlModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MachineLearningController {

    /**
     * Executes a full analysis process over multiple boolean feature combinations.
     *
     * <p>The method generates all combinations of two boolean parameters, runs the
     * analysis for each combination using {@link WekaManager}, and stores the results
     * indexed by an incremental key. Finally, all results are persisted to CSV.
     */
    public void executeProcess(){

        WekaManager wekaManager = new WekaManager();
        Map<Integer, List<MlDatasetEntry>> results = new HashMap<>();
        List<boolean[]> combinations = new ArrayList<>();
        combinations.add(new boolean[]{false, false});
        combinations.add(new boolean[]{true, false});
        combinations.add(new boolean[]{false, true});
        combinations.add(new boolean[]{true, true});
        int i = 1;

        for(boolean[] bools : combinations){
            List<MlDatasetEntry> list = wekaManager.startAnalysis(bools[0], bools[1]);
            results.put(i, list);
            i++;
        }
        DatasetDao<Map<Integer, List<MlDatasetEntry>>> dao = new CsvMlDao();
        dao.save(results);
    }

    /**
     * Executes a what-if scenario using the selected ML model and persists the results to CSV.
     *
     * @param code integer code mapped to an {@link MlModel} used for the scenario execution
     */
    public void startWhatIfScenario(int code){

        WekaManager wekaManager = new WekaManager();
        Map<String, int[]> results = wekaManager.startWhatIfScenario(MlModel.fromInt(code));
        DatasetDao<Map<String, int[]>> dao = new CsvWhatIfDao();
        dao.save(results);
    }
}
