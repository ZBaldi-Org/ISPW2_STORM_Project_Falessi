package it.zbaldi.view;

import it.zbaldi.controller.ClassAnalyzerController;
import it.zbaldi.controller.MachineLearningController;

import java.util.Scanner;

public class ConsoleView implements GenericView {

    /** Controller for analyzing classes. */
    private final ClassAnalyzerController classAnalyzerController = new ClassAnalyzerController();

    /** Controller for ML analysis. */
    private final MachineLearningController machineLearningController = new MachineLearningController();

    /**
     * Starts the console view application loop.
     * Continues showing menu and processing user input until exit is selected.
     */
    @Override
    public void start() {

        boolean exit = false;

        while(!exit) {
            showMenu();
            exit = checkOption();
        }
    }

    /**
     * Displays the main menu options to the user.
     */
    private void showMenu(){

        System.out.println("--------STORM PROJECT ANALYZER--------");
        System.out.println("Select one option:");
        System.out.println("1) Start Project Analysis");
        System.out.println("2) Start ML Analysis");
        System.out.println("3) Start What-If Scenario");
        System.out.println("4) Exit");
    }

    /**
     * Handles user input menu selection.
     * @return true if the user selected to exit, false otherwise
     */
    private boolean checkOption(){

        try {
            Scanner scanner = new Scanner(System.in);
            int input = scanner.nextInt();

            switch (input) {
                case 1:
                    classAnalyzerController.getCodeSnapshots(0.3F);
                    classAnalyzerController.executeExtractionProcess();
                    return false;
                case 2:
                    machineLearningController.executeProcess();
                    return false;
                case 3:
                    System.out.println("Choose:\n0 -> Random Forest\n1 -> Naive Bayes\n2 -> IBk");
                    machineLearningController.startWhatIfScenario(scanner.nextInt());
                    return false;
                case 4:
                    return true;
                default:
                    System.out.println("Invalid option");
                    return false;
            }
        }catch (Exception e){
            System.out.println("Invalid option");
            return false;
        }
    }
}
