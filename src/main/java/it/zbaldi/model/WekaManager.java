package it.zbaldi.model;

import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import lombok.extern.slf4j.Slf4j;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.BestFirst;
import weka.attributeSelection.CfsSubsetEval;
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
            log.error("Error using ML models on the dataset");
            return Collections.emptyList();
        }
    }
}
