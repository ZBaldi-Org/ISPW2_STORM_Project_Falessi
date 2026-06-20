package it.zbaldi.model;

import it.zbaldi.model.data.FixedBuggyTicket;
import it.zbaldi.model.data.ReleaseInfo;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class TicketSearcher {

    /** Jira project name used to build the API endpoint. */
    private static final String PROJECT_NAME = "STORM";

    /** Jira REST API endpoint to retrieve project versions. */
    private static final String URL = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22" + PROJECT_NAME + "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR" + "%22status%22=%22resolved%22)AND%22resolution%22=%22fixed%22&fields=key,resolutiondate,versions,fixVersions,created&startAt=";

    /** Default ticket version value */
    private static final String DEFAULT_VALUE = "NOT FOUND";

    /**
     * Retrieves Jira issues and maps them to a list of {@link FixedBuggyTicket}.
     * Data is fetched in batches from the Jira REST API and includes key,
     * fix version, affected version, and creation date.
     *
     * @return list of {@link FixedBuggyTicket}, or empty list in case of error
     */
    public List<FixedBuggyTicket> getJiraFixedBuggyTickets(List<ReleaseInfo> releaseInfos) {

        try {
            List<FixedBuggyTicket> fixedBuggyTickets = new ArrayList<>();
            int startAt = 0;
            int maxResults = 1000;
            int total;

            do {
                JSONObject json = readJsonFromUrl(URL + startAt + "&maxResults=" + maxResults);
                JSONArray issues = json.getJSONArray("issues");
                total = json.getInt("total");

                for (int i = 0; i < issues.length(); i++) {
                    JSONObject issue = issues.getJSONObject(i);
                    String key = issue.getString("key");
                    JSONObject fields = issue.getJSONObject("fields");
                    String fixVersionString = DEFAULT_VALUE;
                    fixVersionString = getFixVersion(fixVersionString, fields);
                    String affectedVersionString = DEFAULT_VALUE;
                    affectedVersionString = getAffectedVersion(affectedVersionString, fields);
                    String openingVersionString = DEFAULT_VALUE;
                    openingVersionString = getOpeningVersion(openingVersionString, fields, releaseInfos);
                    FixedBuggyTicket fixBuggyTicket = new FixedBuggyTicket(key, fixVersionString, affectedVersionString, openingVersionString);
                    fixedBuggyTickets.add(fixBuggyTicket);
                }
                startAt += issues.length();

            } while (startAt < total);
            fixedBuggyTickets.removeIf(fix -> fix.getFixVersion().equals(DEFAULT_VALUE) || fix.getOpeningVersion().equals(DEFAULT_VALUE) || fix.getOpeningVersion().equals("DATA MIGRATED"));
            List<String> availableVersions = new ArrayList<>();
            releaseInfos.forEach(releaseInfo -> availableVersions.add(releaseInfo.getReleaseName()));
            fixedBuggyTickets.removeIf(ticket -> (!availableVersions.contains(ticket.getFixVersion())));
            fixedBuggyTickets.forEach(ticket -> {

                if(availableVersions.indexOf(ticket.getAffectedVersion()) > availableVersions.indexOf(ticket.getOpeningVersion())){
                    ticket.setAffectedVersion(DEFAULT_VALUE);
                }

                if(!ticket.getAffectedVersion().equals(DEFAULT_VALUE) && !availableVersions.contains(ticket.getAffectedVersion())){
                    ticket.setAffectedVersion(DEFAULT_VALUE);
                }
            });
            return fixedBuggyTickets;

        }catch (Exception e){
            log.error("Error in getJiraFixedBuggyTickets, returning empty list");
            return new ArrayList<>();
        }
    }

    /**
     * Extracts the first fix version name from the issue fields.
     *
     * @param fixVersionString default fix version value
     * @param fields           JSON object containing issue fields
     * @return the fix version name, or the default value if not available
     */
    private String getFixVersion(String fixVersionString, JSONObject fields) {

        if (fields.has("fixVersions")) {
            JSONArray fixVersions = fields.getJSONArray("fixVersions");

            if (!fixVersions.isEmpty()) {
                JSONObject fixVersion = fixVersions.getJSONObject(0);

                if (fixVersion.has("name") && !fixVersion.isEmpty()) {
                    fixVersionString = fixVersion.getString("name");
                }
            }
        }
        return fixVersionString;
    }

    /**
     * Extracts the first affected version name from the issue fields.
     *
     * @param affectedVersionString default affected version value
     * @param fields                JSON object containing issue fields
     * @return the affected version name, or the default value if not available
     */
    private String getAffectedVersion(String affectedVersionString, JSONObject fields) {

        if (fields.has("versions")) {
            JSONArray affectedVersions = fields.getJSONArray("versions");

            if (!affectedVersions.isEmpty()) {

                JSONObject affectedVersion = affectedVersions.getJSONObject(0);

                if (affectedVersion.has("name")) {
                    affectedVersionString = affectedVersion.getString("name");
                }
            }
        }
        return affectedVersionString;
    }

    /**
     * Determines the opening version based on the issue creation date
     * and the available release information.
     *
     * @param openingVersionString default opening version value
     * @param fields               JSON object containing issue fields
     * @param releaseInfos         list of project releases
     * @return the opening version name, or "DATA MIGRATED" if no match is found
     */
    private String getOpeningVersion(String openingVersionString, JSONObject fields, List<ReleaseInfo> releaseInfos) {

        if (fields.has("created")) {
            LocalDate created = LocalDate.parse(fields.getString("created").substring(0, 10));

            for (int x = 0; x < releaseInfos.size(); x++) {
                LocalDate releaseDate = releaseInfos.get(x).getDate();

                if (x + 1 < releaseInfos.size()) {

                    LocalDate nextReleaseDate = releaseInfos.get(x + 1).getDate();

                    if (created.isAfter(releaseDate) && created.isBefore(nextReleaseDate)) {

                        openingVersionString = releaseInfos.get(x).getReleaseName();
                        break;
                    }
                }
                else {
                    openingVersionString = "DATA MIGRATED";
                }
            }
        }
        return openingVersionString;
    }

    /**
     * Performs the HTTP request to the Jira endpoint and
     * parses the response into a JSONArray.
     *
     * @return JSONArray containing Jira project versions
     * @throws IOException if a network error occurs
     * @throws JSONException if the response is not valid JSON
     */

    private JSONObject readJsonFromUrl(String url) throws IOException, JSONException {

        try (InputStream is = new URL(url).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String jsonText = readAll(rd);
            return new JSONObject(jsonText);
        }
    }

    /**
     * Reads the entire content of a Reader into a String.
     *
     * @param rd the Reader to consume
     * @return full content as a String
     * @throws IOException if an I/O error occurs
     */
    private String readAll(Reader rd) throws IOException {

        StringBuilder sb = new StringBuilder();
        int cp;

        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }
}
