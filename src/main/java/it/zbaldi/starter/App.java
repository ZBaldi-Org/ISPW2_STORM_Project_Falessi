package it.zbaldi.starter;

import it.zbaldi.view.ConsoleView;
import it.zbaldi.view.GenericView;

import java.io.IOException;

public class App {

    public static void main( String[] args ) throws IOException {

        GenericView view = new ConsoleView();
        view.start();
    }
}
