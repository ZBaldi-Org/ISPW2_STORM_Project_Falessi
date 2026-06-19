package it.zbaldi.model;

import it.zbaldi.model.enums.MlModel;
import lombok.extern.slf4j.Slf4j;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.BestFirst;
import weka.attributeSelection.CfsSubsetEval;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.RandomForest;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SpreadSubsample;
import weka.filters.unsupervised.attribute.Normalize;

import java.util.*;

@Slf4j
public class WekaManager {

    public List<MlDatasetEntry> startAnalysis(boolean featureSelectionFlag, boolean balancingFlag) {

        try{
            DataSource source = new DataSource("dataset.csv");
            Instances data = source.getDataSet();
            data.deleteAttributeAt(1);
            data.deleteAttributeAt(0);
            data.setClassIndex(data.numAttributes() - 1);
            RandomForest rf = new RandomForest();
            NaiveBayes nb = new NaiveBayes();
            IBk ibk = new IBk();
            Classifier[] models = {rf, nb, ibk};
            List<MlDatasetEntry> results = new ArrayList<>();

            for (Classifier model : models) {

                double averageAccuracy, averagePrecision,  averageRecall, averageF1, averageAuc, averageKappa;
                double sumAccuracy = 0, sumPrecision = 0, sumRecall = 0, sumF1 = 0, sumAuc = 0, sumKappa = 0;

                for (int i = 0; i < 10; i++) {
                    Instances runData = new Instances(data);
                    runData.randomize(new java.util.Random(i));

                    if (runData.classAttribute().isNominal()) {
                        runData.stratify(10);
                    }

                    for (int j = 0; j < 10; j++) {
                        Instances train = runData.trainCV(10, j);
                        Instances test = runData.testCV(10, j);

                        if (featureSelectionFlag) {
                            AttributeSelection selector = new AttributeSelection();
                            CfsSubsetEval eval = new CfsSubsetEval();
                            BestFirst search = new BestFirst();
                            search.setDirection(new weka.core.SelectedTag(0, BestFirst.TAGS_SELECTION)); //BACKWARD GREEDY
                            selector.setEvaluator(eval);
                            selector.setSearch(search);
                            selector.SelectAttributes(train);
                            train = selector.reduceDimensionality(train);
                            test = selector.reduceDimensionality(test);
                        }

                        Normalize normalize = new Normalize();
                        normalize.setInputFormat(train);
                        train = Filter.useFilter(train, normalize);
                        test = Filter.useFilter(test, normalize);

                        if (balancingFlag) {  //HYBRID APPROACH
                            SpreadSubsample filter = new SpreadSubsample();  //UNDERSAMPLING MAJORITY IS 2X MINORITY
                            filter.setOptions(new String[]{"-M", "2.0"});
                            filter.setInputFormat(train);
                            train = Filter.useFilter(train, filter);
                            Resample resample = new Resample();
                            resample.setOptions(new String[]{"-B", "1.0", "-Z", "200"}); //OVERSAMPLING MINORITY
                            resample.setInputFormat(train);
                            train = Filter.useFilter(train, resample);
                        }

                        model.buildClassifier(train);
                        Evaluation eval = new Evaluation(train);
                        eval.evaluateModel(model, test);
                        sumAccuracy += eval.pctCorrect();
                        sumPrecision += eval.precision(1);
                        sumRecall += eval.recall(1);
                        sumF1 += eval.fMeasure(1);
                        sumAuc += eval.areaUnderROC(1);
                        sumKappa += eval.kappa();
                    }
                }

                averageAccuracy = sumAccuracy / 100;
                averagePrecision = sumPrecision / 100;
                averageRecall = sumRecall / 100;
                averageF1 = sumF1 / 100;
                averageAuc = sumAuc / 100;
                averageKappa = sumKappa / 100;
                results.add(new MlDatasetEntry(model.getClass().getSimpleName(), featureSelectionFlag, balancingFlag, averageAccuracy, averagePrecision, averageRecall, averageF1, averageAuc, averageKappa));
            }
            return results;

        }catch (Exception e){
            log.error("Error Using ML Models On The Dataset");
            return Collections.emptyList();
        }
    }

    /**
     * Performs a what-if analysis using a RandomForest classifier.
     * Evaluates different dataset scenarios and returns actual vs predicted positives.
     *
     * @return map of dataset names to [actualPositives, predictedPositives]
     */
    public Map<String, int[]> startWhatIfScenario(MlModel mlModel){

        try{
            DataSource source = new DataSource("dataset.csv");
            Instances a = source.getDataSet();
            a.deleteAttributeAt(1);
            a.deleteAttributeAt(0);
            a.setClassIndex(a.numAttributes() - 1);
            Instances bPlus = new Instances(a, 0);
            Instances b = new Instances(a, 0);
            Instances c = new Instances(a, 0);
            int smellsIndex = a.numAttributes() - 2;

            for (int i = 0; i < a.numInstances(); i++) {
                double smells = a.instance(i).value(smellsIndex);

                if (smells > 0) {
                    bPlus.add(a.instance(i));
                    Instance entry = (Instance) a.instance(i).copy();
                    entry.setDataset(a);
                    entry.setValue(smellsIndex, 0);
                    b.add(entry);
                }
                else if(smells == 0){
                    c.add(a.instance(i));
                }
            }

            Classifier bestClassifier;

            switch(mlModel){
                case RANDOM_FOREST -> bestClassifier = new RandomForest();
                case NAIVEBAYES -> bestClassifier = new NaiveBayes();
                case IBK -> bestClassifier = new IBk();
                default -> throw new Exception("Unsupported ML Model");
            }
            bestClassifier.buildClassifier(a);
            Map<String, Instances> map = Map.of("a", a, "bPlus", bPlus, "b", b, "c", c);
            Map<String, int[]> results = new HashMap<>();
            Evaluation eval = new Evaluation(a);

            for(Map.Entry<String, Instances> entry : map.entrySet()) {
                eval.evaluateModel(bestClassifier, entry.getValue());
                double[][] cm = eval.confusionMatrix();
                double tn = cm[0][0];
                double fp = cm[0][1];
                double fn = cm[1][0];
                double tp = cm[1][1];
                int actualPositives = (int) (fn + tp);
                int predictedPositives = (int) (fp + tp);
                results.put(entry.getKey(), new int[]{actualPositives, predictedPositives});
            }
            return results;

        }catch (Exception e){
            log.error("Error Doing WhatIfScenario, message: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}
