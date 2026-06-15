package it.zbaldi.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
@AllArgsConstructor
public class ReleaseInfo {

    /** A unique identifier for the release (e.g., a version number or build ID). */
    private String releaseID;

    /** The name of the software release (e.g., "Version 1.0", "Spring Update"). */
    private String releaseName;

    /** The date when the release was made. */
    private LocalDate date;
}
