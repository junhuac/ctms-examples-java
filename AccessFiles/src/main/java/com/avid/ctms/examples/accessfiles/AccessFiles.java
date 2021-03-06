package com.avid.ctms.examples.accessfiles;

import com.avid.ctms.examples.tools.common.*;
import com.avid.ctms.examples.tools.unirest.PlatformToolsUnirest;
import com.damnhandy.uri.template.*;
import com.mashape.unirest.http.*;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.json.*;

import java.net.*;
import java.util.*;
import java.util.Formatter;
import java.util.logging.*;

/**
 * Copyright 2013-2017 by Avid Technology, Inc.
 * User: nludwig
 * Date: 2017-8-2
 * Time: 7:58
 * Project: CTMS
 */

/**
 * This example shows, how to retrieve information about the files (a.k.a. essences) of an asset.
 */
public class AccessFiles {
    private static final Logger LOG = Logger.getLogger(AccessFiles.class.getName());

    private AccessFiles() {
    }

    private static void getFileInformation(String urlAccessFileByUsageAndProtocol) throws Exception {

        try {
            URL simpleSearchResultPageURL = new URL(urlAccessFileByUsageAndProtocol);
            final HttpURLConnection simpleSearchResultPageConnection = (HttpURLConnection) simpleSearchResultPageURL.openConnection();
            simpleSearchResultPageConnection.setConnectTimeout(PlatformTools.getDefaultConnectionTimeoutms());
            simpleSearchResultPageConnection.setReadTimeout(PlatformTools.getDefaultReadTimeoutms());
            simpleSearchResultPageConnection.setRequestProperty("Accept", "application/hal+json");

            final int simpleSearchStatus = simpleSearchResultPageConnection.getResponseCode();
            if (HttpURLConnection.HTTP_OK == simpleSearchStatus) {
                final String rawSimpleSearchPageResult = PlatformTools.getContent(simpleSearchResultPageConnection);

                final JSONObject searchesResult = JSONObject.fromObject(rawSimpleSearchPageResult);
                final JSONArray files = searchesResult.getJSONObject("_embedded").getJSONArray("aa:access-file");
                final StringBuilder sb = new StringBuilder(String.format("Requested: <%s>%n", urlAccessFileByUsageAndProtocol));
                try (final Formatter formatter = new Formatter(sb)) {
                    for (int i = 0; i < files.size(); ++i) {
                        final JSONObject file = files.getJSONObject(i);
                        formatter.format("\t%s;%s%n", file.get("protocol"), file.getJSONObject("_links").getJSONObject("ma:essence").get("href"));
                    }
                }

                LOG.log(Level.INFO, sb::toString);
            } else {
                LOG.log(Level.INFO, "Error getting resource <{0}>. -> {1}", new Object[] {urlAccessFileByUsageAndProtocol, simpleSearchResultPageConnection.getResponseMessage()});
            }
        } catch (final Exception exception) {
                LOG.log(Level.SEVERE, "failure", exception);
            } finally {
            }
    }

    public static void main(String[] args) throws Exception {
        if (7 != args.length) {
            LOG.log(Level.INFO, "Usage: {0} <apidomain> <servicetype> <serviceversion> <realm> <username> <password> <assetid>", AccessFiles.class.getSimpleName());
        } else {
            final String apiDomain = args[0];
            final String serviceType = args[1];
            final String serviceVersion = args[2];
            final String realm = args[3];
            final String username = args[4];
            final String password = args[5];
            // deutsch.mp3: 2016051315594660101291561460050569B02260000003692B00000D0D000003
            final String assetID = args[6];

            final boolean successfullyAuthorized = PlatformTools.authorize(apiDomain, username, password);
            if(successfullyAuthorized) {
                try {
                    /// Query CTMS Registry:
                    final String registryServiceVersion = "0";
                    final String defaultAssetByIDUriTemplate = String.format("https://%s/apis/%s;version=%s;realm=%s/assets/{id}", apiDomain, serviceType, serviceVersion, realm);
                    final List<String> assetByIDUriTemplates = PlatformTools.findInRegistry(apiDomain, Collections.singletonList(serviceType), registryServiceVersion, "aa:asset-by-id", defaultAssetByIDUriTemplate);

                    /// Prepare simple search request:
                    final UriTemplate searchURITemplate = UriTemplate.fromTemplate(assetByIDUriTemplates.get(0));
                    URL simpleSearchResultPageURL = new URL(searchURITemplate.set("id", assetID).expand());

                    final HttpURLConnection simpleSearchResultPageConnection = (HttpURLConnection) simpleSearchResultPageURL.openConnection();
                    simpleSearchResultPageConnection.setConnectTimeout(PlatformTools.getDefaultConnectionTimeoutms());
                    simpleSearchResultPageConnection.setReadTimeout(PlatformTools.getDefaultReadTimeoutms());
                    simpleSearchResultPageConnection.setRequestProperty("Accept", "application/hal+json");

                    final int simpleSearchStatus = simpleSearchResultPageConnection.getResponseCode();
                    if (HttpURLConnection.HTTP_OK == simpleSearchStatus) {
                        final String rawSimpleSearchPageResult = PlatformTools.getContent(simpleSearchResultPageConnection);

                        final JSONObject searchesResult = JSONObject.fromObject(rawSimpleSearchPageResult);
                        final JSONObject advancedSearchLinkObject = searchesResult.getJSONObject("_links").getJSONObject("aa:access-file-by-type-usage-and-protocol");
                        // Is advanced search supported?
                        if (null != advancedSearchLinkObject) {
                            /// Doing the advanced search and write the results to stdout:
                            String urlAccessFileByUsageAndProtocol = advancedSearchLinkObject.getString("href");
                            urlAccessFileByUsageAndProtocol = urlAccessFileByUsageAndProtocol.substring(0, urlAccessFileByUsageAndProtocol.lastIndexOf('{'));

                            // Get all files:
                            getFileInformation(urlAccessFileByUsageAndProtocol);
                            // Get files with usage=BROWSE:
                            getFileInformation(urlAccessFileByUsageAndProtocol + "?usage=BROWSE");
                            // Get files with filetype=VIDEO and usage=BROWSE:
                            getFileInformation(urlAccessFileByUsageAndProtocol + "?filetype=VIDEO&usage=BROWSE");
                            // Get files with filetype=FSET:
                            getFileInformation(urlAccessFileByUsageAndProtocol + "?filetype=FSET");
                        }
                    } else {
                            LOG.log(Level.INFO, "Error getting resource <{0}>. -> {1}", new Object[]{simpleSearchResultPageURL, simpleSearchResultPageConnection.getResponseMessage()});
                    }
                } catch (final Exception exception) {
                    LOG.log(Level.SEVERE, "failure", exception);
                } finally {
                    PlatformTools.logout(apiDomain);
                }
            } else {
                LOG.log(Level.INFO, "Authorization failed.");
            }
        }

        LOG.log(Level.INFO, "End");
    }
}
