package it.zbaldi.model;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class TicketSearcher {

    /** Jira project name used to build the API endpoint. */
    private final String PROJECT_NAME = "STORM";

    /** Jira REST API endpoint to retrieve project versions. */
    private final String URL = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22" + PROJECT_NAME + "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR" + "%22status%22=%22resolved%22)AND%22resolution%22=%22fixed%22&fields=key,resolutiondate,versions,fixVersions,created&startAt=";


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
                    String fixVersionString = "NOT FOUND";

                    if (fields.has("fixVersions")) {
                        JSONArray fixVersions = fields.getJSONArray("fixVersions");

                        if(!fixVersions.isEmpty()){
                            JSONObject fixVersion = fixVersions.getJSONObject(0);

                            if(fixVersion.has("name") && !fixVersion.isEmpty()) {
                            fixVersionString = fixVersion.getString("name");
                        }
                        }
                    }
                    String affectedVersionString = "NOT FOUND";

                    if (fields.has("versions")) {
                        JSONArray affectedVersions = fields.getJSONArray("versions");

                        if (!affectedVersions.isEmpty()) {

                            JSONObject affectedVersion = affectedVersions.getJSONObject(0);

                            if (affectedVersion.has("name")) {
                                affectedVersionString = affectedVersion.getString("name");
                            }
                        }
                    }

                    String openingVersionString = "NOT FOUND";

                    if (fields.has("created")) {

                        LocalDate created = LocalDate.parse(fields.getString("created").substring(0, 10));

                        int a = 0;
                        for (int x = 0; x < releaseInfos.size(); x++) {
                            LocalDate releaseDate = releaseInfos.get(x).getDate();

                            if (x + 1 < releaseInfos.size()) {

                                LocalDate nextReleaseDate = releaseInfos.get(x + 1).getDate();

                                if (created.isAfter(releaseDate) && created.isBefore(nextReleaseDate)) {

                                    openingVersionString = releaseInfos.get(x).getReleaseName();
                                    break;
                                }
                            }
                            else{
                                openingVersionString = "DATA MIGRATED";
                            }
                        }
                    }

                    FixedBuggyTicket fixBuggyTicket = new FixedBuggyTicket(key, fixVersionString, affectedVersionString, openingVersionString);
                    fixedBuggyTickets.add(fixBuggyTicket);
                }
                startAt += issues.length();
            } while (startAt < total);
            Set<String> availableVersions = new HashSet<>();

            for(ReleaseInfo releaseInfo : releaseInfos){
                availableVersions.add(releaseInfo.getReleaseName());
            }

            fixedBuggyTickets.removeIf(ticket -> (!"NOT FOUND".equals(ticket.getAffectedVersion())
                            && !availableVersions.contains(ticket.getAffectedVersion()))
                            || (!"NOT FOUND".equals(ticket.getFixVersion())
                            && !availableVersions.contains(ticket.getFixVersion())));
            return fixedBuggyTickets;

        }catch (Exception e){
            log.error("Error in getJiraFixedBuggyTickets, returning empty list");
            return new ArrayList<>();
        }
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
