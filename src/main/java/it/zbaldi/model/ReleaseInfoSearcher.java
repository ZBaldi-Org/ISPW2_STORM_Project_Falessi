package it.zbaldi.model;

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
import java.util.Comparator;
import java.util.List;

@Slf4j
public class ReleaseInfoSearcher {

    /** Jira project name used to build the API endpoint. */
    private final String PROJECT_NAME = "STORM";

    /** Jira REST API endpoint to retrieve project versions. */
    private final String URL = "https://issues.apache.org/jira/rest/api/2/project/" + PROJECT_NAME + "/versions";

    /**
     * Retrieves Jira releases (versions), converts them into ReleaseInfo objects,
     * and sorts them by release date.
     *
     * @return a list of ReleaseInfo sorted by ascending release date
     */
    public List<ReleaseInfo> getJiraReleases(){

        try{
            JSONArray versions = readJsonFromUrl();
            List<ReleaseInfo> releaseInfos = new ArrayList<>();

            for (int i = 0; i < versions.length(); i++) {
                JSONObject version = versions.getJSONObject(i);

                if (version.has("releaseDate") && version.has("name") && version.has("id")) {
                    releaseInfos.add(new ReleaseInfo(version.get("id").toString(),
                            version.get("name").toString(),
                            LocalDate.parse(version.get("releaseDate").toString())));
                }
            }
            releaseInfos.sort(Comparator.comparing(ReleaseInfo::getDate));
            return releaseInfos;

        }catch (Exception e){
            log.error("Error in getJiraReleases, returning empty list");
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
    private JSONArray readJsonFromUrl() throws IOException, JSONException {

        try (InputStream is = new URL(URL).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String jsonText = readAll(rd);
            return new JSONArray(jsonText);
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