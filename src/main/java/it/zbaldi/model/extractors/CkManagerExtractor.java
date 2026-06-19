package it.zbaldi.model.extractors;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.comments.Comment;
import com.github.mauricioaniche.ck.CK;
import com.github.mauricioaniche.ck.CKClassResult;
import it.zbaldi.model.data.DatasetEntry;
import it.zbaldi.model.interfaces.MetricExtractor;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.reflections.Reflections.log;

public class CkManagerExtractor implements MetricExtractor<String, List<DatasetEntry>> {

    /**
     * Starts the CK analysis on the given path and extracts metrics for all classes.
     *
     * @param pathTag The path to analyze (should point to a directory containing Java source files)
     * @return A list of DatasetEntry objects containing the extracted metrics for each class
     */
    @Override
    public List<DatasetEntry> startAnalysis(String pathTag) {

        Map<String, DatasetEntry> datasetMap = new HashMap<>();
        String marker = pathTag + "\\";

        new CK().calculate(pathTag, result -> {
            String classPath = extractClassPath(result, marker);
            log.info("Setting Ck Metrics For: {}", classPath);
            DatasetEntry datasetEntry = datasetMap.computeIfAbsent(classPath, k -> new DatasetEntry());
            setClassPath(datasetEntry, result, marker);
            setRelease(datasetEntry, pathTag);
            setLinesOfCodeAndCommentDensity(datasetEntry);
            setWeightedMethods(datasetEntry, result);
            setNumberOfAttributes(datasetEntry, result);
            setFanIn(datasetEntry, result);
            setFanOut(datasetEntry, result);
            setCouplingBetweenObjects(datasetEntry, result);
            setNormalizedLackOfCohesion(datasetEntry, result);
            setResponseForClass(datasetEntry, result);
        });
        log.info("Finished Setting Ck Metrics for: {}", pathTag);
        return new ArrayList<>(datasetMap.values());
    }

    /**
     * Sets the class path in the dataset entry by extracting it from the CK result file path.
     *
     * @param datasetEntry The dataset entry to update
     * @param result       The CK class result containing file information
     * @param marker       The path marker used to extract the relative class path
     */
    private void setClassPath(DatasetEntry datasetEntry, CKClassResult result, String marker) {

        String classPath = result.getFile();
        datasetEntry.setFullClassPath(classPath);
        int index = classPath.indexOf(marker);
        classPath = classPath.substring(index + marker.length());
        datasetEntry.setClassPath(classPath);
        datasetEntry.setRelativeClassPath(marker + classPath);
    }

    /**
     * Extracts the relative class path from the full file path using the given marker.
     *
     * @param result CK analysis result containing the full file path
     * @param marker reference string used to split the path
     * @return relative class path after the marker
     */
    private String extractClassPath(CKClassResult result, String marker) {

        String classPath = result.getFile();
        int index = classPath.indexOf(marker);
        return classPath.substring(index + marker.length());
    }

    /**
     * Sets the release number by parsing it from the path.
     * Assumes the path format contains the release version before an underscore.
     *
     * @param datasetEntry The dataset entry to update
     * @param path         The path containing the release information
     */
    private void setRelease(DatasetEntry datasetEntry, String path) {

        String version = path.substring(path.lastIndexOf("\\") + 1);
        version = version.substring(0, version.indexOf("_"));
        datasetEntry.setRelease(Integer.parseInt(version));
    }

    /**
     * Computes the comment density of a Java file and stores it in the dataset entry.
     * Density is defined as comment lines divided by total lines of code.
     *
     * @param datasetEntry dataset entry containing file path and metrics
     */
    private void setLinesOfCodeAndCommentDensity(DatasetEntry datasetEntry) {

        try {
            File file = new File(datasetEntry.getRelativeClassPath());
            CompilationUnit compilationUnit = StaticJavaParser.parse(file);
            var comments = compilationUnit.getAllContainedComments();
            int totalLines = 0;
            int lines;
            long totalLocs = Files.lines(Path.of(datasetEntry.getRelativeClassPath())).filter(line -> !line.trim().isEmpty()).filter(line -> !line.trim().startsWith("//")).filter(line -> !line.trim().startsWith("/*")).count();

            for (Comment c : comments) {
                lines = c.getBegin().flatMap(begin -> c.getEnd().map(end -> end.line - begin.line + 1)).orElse(0);
                totalLines += lines;
            }
            float density = (float) totalLines / totalLocs;
            datasetEntry.setLinesOfCode((int) totalLocs);
            datasetEntry.setCommentDensity(density);

        } catch (Exception e) {
            log.error("Error while calculating lines of code of file {} ", datasetEntry.getRelativeClassPath());
        }
    }

    /**
     * Sets the lines of code metric from the CK result.
     *
     * @param datasetEntry The dataset entry to update
     * @param result       The CK class result containing LOC information
     */
    private void setLinesOfCode(DatasetEntry datasetEntry, CKClassResult result) {

        datasetEntry.setLinesOfCode(result.getLoc());
    }

    /**
     * Sets the weighted methods per class (WMC) metric from the CK result.
     *
     * @param datasetEntry The dataset entry to update
     * @param result       The CK class result containing WMC information
     */
    private void setWeightedMethods(DatasetEntry datasetEntry, CKClassResult result) {

        datasetEntry.setWeightedMethods(result.getWmc());
    }

    /**
     * Sets the number of attributes/fields metric from the CK result.
     *
     * @param datasetEntry The dataset entry to update
     * @param result       The CK class result containing field count information
     */
    private void setNumberOfAttributes(DatasetEntry datasetEntry, CKClassResult result) {

        datasetEntry.setNumberOfAttributes(result.getNumberOfFields());
    }

    /**
     * Sets the fan-in metric from the CK result.
     * Fan-in measures the number of classes that call this class's methods.
     *
     * @param datasetEntry The dataset entry to update
     * @param result       The CK class result containing fan-in information
     */
    private void setFanIn(DatasetEntry datasetEntry, CKClassResult result) {

        datasetEntry.setFanIn(result.getFanin());
    }

    /**
     * Sets the fan-out metric from the CK result.
     * Fan-out measures the number of other classes that this class calls.
     *
     * @param datasetEntry The dataset entry to update
     * @param result       The CK class result containing fan-out information
     */
    private void setFanOut(DatasetEntry datasetEntry, CKClassResult result) {

        datasetEntry.setFanOut(result.getFanout());
    }

    /**
     * Sets the coupling between objects (CBO) metric from the CK result.
     * CBO measures the number of classes to which this class is coupled.
     *
     * @param datasetEntry The dataset entry to update
     * @param result       The CK class result containing CBO information
     */
    private void setCouplingBetweenObjects(DatasetEntry datasetEntry, CKClassResult result) {

        datasetEntry.setCouplingBetweenObjects(result.getCbo());
    }

    /**
     * Sets the normalized lack of cohesion (LCOM) metric from the CK result.
     * LCOM measures the dissimilarity among methods in a class based on shared instance variables.
     *
     * @param datasetEntry The dataset entry to update
     * @param result       The CK class result containing LCOM information
     */
    private void setNormalizedLackOfCohesion(DatasetEntry datasetEntry, CKClassResult result) {

        datasetEntry.setNormalizedLackOfCohesion(result.getLcomNormalized());
    }

    /**
     * Sets the response for class (RFC) metric from the CK result.
     * RFC measures the number of methods that can be invoked in response to a message received by an object.
     *
     * @param datasetEntry The dataset entry to update
     * @param result       The CK class result containing RFC information
     */
    private void setResponseForClass(DatasetEntry datasetEntry, CKClassResult result) {

        datasetEntry.setResponseForClass(result.getRfc());
    }
}
