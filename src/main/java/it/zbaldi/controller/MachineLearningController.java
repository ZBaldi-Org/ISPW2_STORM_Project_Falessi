package it.zbaldi.controller;

import it.zbaldi.model.DatasetDao;
import it.zbaldi.model.MlDatasetEntry;
import it.zbaldi.model.WekaManager;
import it.zbaldi.model.daos.CsvMlDao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MachineLearningController {

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
}
