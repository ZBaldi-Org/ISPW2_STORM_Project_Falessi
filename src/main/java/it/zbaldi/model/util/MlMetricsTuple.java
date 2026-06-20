package it.zbaldi.model.util;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MlMetricsTuple {

    /** Accuracy score (correct predictions / total predictions). */
    private double accuracy = 0;

    /** Precision score (true positives / (true positives + false positives)). */
    private double precision = 0;

    /** Recall score (true positives / (true positives + false negatives)). */
    private double recall = 0;

    /** F1 score (harmonic mean of precision and recall). */
    private double f1 = 0;

    /** AUC (Area Under the ROC Curve). */
    private double auc = 0;

    /** Cohen's Kappa coefficient (inter-rater agreement). */
    private double kappa = 0;
}
