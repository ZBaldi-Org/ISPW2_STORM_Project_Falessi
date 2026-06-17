package it.zbaldi;

import it.zbaldi.controller.ClassAnalyzerController;

import java.io.IOException;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws IOException {
        ClassAnalyzerController controller = new ClassAnalyzerController();
        controller.getCodeSnapshots(0.05F);
        controller.executeExtractionProcess(0.05F);
    }
}
