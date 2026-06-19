package it.zbaldi.model.enums;

import lombok.Getter;

@Getter
public enum MlModel {

    /** Random Forest model. */
    RANDOM_FOREST(0),
    /** Naive Bayes model. */
    NAIVEBAYES(1),
    /** IBK model. */
    IBK(2);

    /** Model identifier. */
    private final int id;

    /**
     * Constructs an MlModel with the given identifier.
     * @param id the model identifier
     */
    MlModel(int id) {

        this.id = id;
    }

    /**
     * Returns the MlModel corresponding to the given identifier.
     * @param id the model identifier
     * @return the MlModel with the given identifier
     * @throws IllegalArgumentException if no model matches the identifier
     */
    public static MlModel fromInt(int id) {

        for (MlModel m : values()) {

            if (m.id == id) {
                return m;
            }
        }
        throw new IllegalArgumentException("Invalid id: " + id);
    }
}
