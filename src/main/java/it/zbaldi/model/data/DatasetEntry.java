package it.zbaldi.model.data;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class DatasetEntry {

    /** Full qualified path of the class */
    private String fullClassPath;

    /** Relative qualified path of the class */
    private String relativeClassPath;

    /** Path of the class in the project (e.g., core/example/Service.class), The dataset use this type */
    private String classPath;

    /** Identifier or number of the release this measurement belongs to */
    private int release;

    /** Total source lines of code of the class (size measure) */
    private int linesOfCode;

    /** Ratio of comment lines to total lines of code (documentation measure) */
    private float commentDensity;

    /** Weighted number of methods (WMC - Weighted Methods per Class from CK metrics) */
    private int weightedMethods;

    /** Number of attributes (instance fields) declared in the class */
    private int numberOfAttributes;

    /** Number of other classes that call methods of this class (afferent coupling) */
    private int fanIn;

    /** Number of other classes called by this class (efferent coupling) */
    private int fanOut;

    /** Coupling Between Objects (CBO - from CK metrics) */
    private int couplingBetweenObjects;

    /** Normalized Lack of Cohesion of Methods (LCOM) */
    private float normalizedLackOfCohesion;

    /** Response For Class (RFC) */
    private int responseForClass;

    /** Age of the class in number of releases since its initial creation */
    private int creationAge;

    /** Number of releases since the last update/modification of the class */
    private int lastUpdateAge;

    /** Total lines of code touched (added + deleted) throughout the class's history */
    private int totalLocTouched;

    /** Lines of code touched in the current release */
    private int releaseLocTouched;

    /** Normalized total churn (measure of change frequency over time) */
    private float normalizedTotalChurn;

    /** Normalized release churn */
    private float normalizedReleaseChurn;

    /** Total number of commits that have modified this class in its history */
    private int totalNumberOfCommits;

    /** Number of commits that have touched this class in the current release */
    private int releaseNumberOfCommits;

    /** Total number of distinct authors who have contributed to this class */
    private int totalNumberOfAuthors;

    /** This is used to calculate the releaseNumberOfAuthors */
    private List<String> authorsHistoryList;

    /** Number of distinct authors who have contributed in the current release */
    private int releaseNumberOfAuthors;

    /** Total number of fixes (bug-resolving commits) applied to this class */
    private int totalNumberOfFixes;

    /** Number of fixes applied in the current release */
    private int releaseNumberOfFixes;

    /** Average change density per commit (how much changes on average per commit) */
    private float averageCommitChangeDensity;

    /** Number of code smells detected in the class (SonarQube) */
    private int numberOfSmells;

    /** Boolean flag indicating whether the class is buggy or not */
    private boolean buggy;
}