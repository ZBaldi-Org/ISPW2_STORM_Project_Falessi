package it.zbaldi;

import it.zbaldi.controller.ClassAnalyzerController;
import it.zbaldi.model.DatasetEntry;

import java.util.List;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        new ClassAnalyzerController().executeExtractionProcess();
    }
}
