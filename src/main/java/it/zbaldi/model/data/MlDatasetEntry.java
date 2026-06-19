package it.zbaldi.model.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MlDatasetEntry {

    /** Name of the model */
    private String modelName;

    /** Whether feature selection is applied */
    private boolean featureSelection;

    /** Whether class balancing is applied */
    private boolean balancing;

    /** Accuracy metric */
    private double accuracy;

    /** Precision metric */
    private double precision;

    /** Recall metric */
    private double recall;

    /** F1-score metric */
    private double f1;

    /** AUC metric */
    private double auc;

    /** Cohen's kappa metric */
    private double kappa;
}