package it.zbaldi.model.util;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Couple<A,B> {

    /** First value of the pair. */
    private A a;

    /** Second value of the pair. */
    private B b;
}
