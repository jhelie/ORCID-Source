/**
 * =============================================================================
 *
 * ORCID (R) Open Source
 * http://orcid.org
 *
 * Copyright (c) 2012-2014 ORCID, Inc.
 * Licensed under an MIT-Style License (MIT)
 * http://orcid.org/open-source-license
 *
 * This copyright and license information (including a link to the full license)
 * shall be included in its entirety in all copies or substantial portion of
 * the software.
 *
 * =============================================================================
 */
package org.orcid.core.analytics;

import javax.ws.rs.core.HttpHeaders;

import org.orcid.core.analytics.client.AnalyticsClient;
import org.orcid.core.manager.ClientDetailsEntityCacheManager;
import org.orcid.core.manager.ProfileEntityCacheManager;
import org.orcid.persistence.jpa.entities.ClientDetailsEntity;
import org.orcid.persistence.jpa.entities.ProfileEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;

public class AnalyticsProcess implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(AnalyticsProcess.class);

    private static final String PUBLIC_API_USER = "Public API user";

    private static final String PUBLIC_API = "Public API";

    private static final String MEMBER_API = "Member API";

    private ContainerRequest request;

    private ContainerResponse response;

    private AnalyticsClient analyticsClient;

    private String clientDetailsId;

    private ClientDetailsEntityCacheManager clientDetailsEntityCacheManager;

    private ProfileEntityCacheManager profileEntityCacheManager;

    public boolean publicApi;

    private String ip;

    @Override
    public void run() {
        AnalyticsData data = getAnalyticsData();
        analyticsClient.sendAnalyticsData(data);
    }

    public void setRequest(ContainerRequest request) {
        this.request = request;
    }

    public void setResponse(ContainerResponse response) {
        this.response = response;
    }

    public void setAnalyticsClient(AnalyticsClient analyticsClient) {
        this.analyticsClient = analyticsClient;
    }

    public void setClientDetailsEntityCacheManager(ClientDetailsEntityCacheManager clientDetailsEntityCacheManager) {
        this.clientDetailsEntityCacheManager = clientDetailsEntityCacheManager;
    }

    public void setClientDetailsId(String clientDetailsId) {
        this.clientDetailsId = clientDetailsId;
    }

    public void setPublicApi(boolean publicApi) {
        this.publicApi = publicApi;
    }

    public void setProfileEntityCacheManager(ProfileEntityCacheManager profileEntityCacheManager) {
        this.profileEntityCacheManager = profileEntityCacheManager;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    private AnalyticsData getAnalyticsData() {
        ip = maskIp(ip);
        APIEndpointParser parser = new APIEndpointParser(request);

        AnalyticsData analyticsData = new AnalyticsData();
        analyticsData.setUrl(getUrlWithHashedOrcidId(parser.getOrcidId(), request.getAbsolutePath().toString()));
        analyticsData.setClientDetailsString(getClientDetailsString());
        analyticsData.setClientId(clientDetailsId != null ? clientDetailsId : ip);
        analyticsData.setContentType(request.getHeaderValue(HttpHeaders.CONTENT_TYPE));
        analyticsData.setUserAgent(request.getHeaderValue(HttpHeaders.USER_AGENT));
        analyticsData.setResponseCode(response.getStatus());
        analyticsData.setIpAddress(ip);
        analyticsData.setCategory(parser.getCategory());
        analyticsData.setApiVersion(getApiString(parser.getApiVersion()));
        analyticsData.setMethod(request.getMethod());
        return analyticsData;
    }

    private String maskIp(String ip) {
        String delimiter = ".";
        int delimiterIndex = ip.lastIndexOf(delimiter);
        if (delimiterIndex == -1) {
            delimiter = ":";
            delimiterIndex = ip.lastIndexOf(":");
        }
        
        if (delimiterIndex != -1) {
            return ip.substring(0, delimiterIndex) + delimiter + "0";
        } else {
            return "";
        }
    }

    private String getUrlWithHashedOrcidId(String orcidId, String url) {
        if (orcidId == null) {
            return url;
        }

        try {
            ProfileEntity profile = profileEntityCacheManager.retrieve(orcidId);
            if (profile.getHashedOrcid() != null) {
                return url.replace(orcidId, profile.getHashedOrcid());
            } else {
                return url;
            }
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid ORCID iD supplied in API call, original URL will be posted to GA");
            return url;
        }
    }

    private String getApiString(String apiVersion) {
        if (publicApi) {
            return PUBLIC_API + " " + apiVersion;
        }
        return MEMBER_API + " " + apiVersion;
    }

    private String getClientDetailsString() {
        if (clientDetailsId != null) {
            ClientDetailsEntity client = clientDetailsEntityCacheManager.retrieve(clientDetailsId);
            StringBuilder clientDetails = new StringBuilder(client.getClientType().value());
            clientDetails.append(" | ");
            clientDetails.append(client.getClientName());
            clientDetails.append(" - ");
            clientDetails.append(clientDetailsId);
            return clientDetails.toString();
        } else {
            return PUBLIC_API_USER;
        }
    }
}
