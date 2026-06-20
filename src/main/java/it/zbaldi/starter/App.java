package it.zbaldi.starter;

import it.zbaldi.view.ConsoleView;
import it.zbaldi.view.GenericView;

public class App {

    public static void main( String[] args ) {

        GenericView view = new ConsoleView();
        view.start();
    }
}
