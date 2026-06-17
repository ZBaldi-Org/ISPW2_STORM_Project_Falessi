package it.zbaldi.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class FixedBuggyTicket {

    /** Unique identifier of the entity (e.g., issue, class, or record). */
    private String key;

    /** Version in which the fix was introduced or released. */
    private String fixVersion;

    /** Version(s) in which the issue or change affects the system. */
    private String affectedVersion;

    /** Version in which the issue was discovered (opening version). */
    private String openingVersion;
}
