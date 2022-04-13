/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.artifactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import javax.ws.rs.core.Response.Status;

import com.sonatype.insight.brain.artifactory.client.ArtifactoryChecksumSearchError;
import com.sonatype.insight.brain.artifactory.client.ArtifactoryChecksumSearchErrors;
import com.sonatype.insight.brain.artifactory.client.ArtifactoryChecksumSearchResults;
import com.sonatype.insight.brain.artifactory.client.ArtifactoryClient;
import com.sonatype.insight.brain.artifactory.client.ChecksumType;
import com.sonatype.insight.client.utils.Authentication;
import com.sonatype.insight.client.utils.HttpClientUtils;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.UrlUtils;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.error.exception.NotAuthenticatedException;
import com.sonatype.insight.json.store.JsonUtils;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultArtifactoryClient
    implements ArtifactoryClient
{
  private static final Logger log = LoggerFactory.getLogger(DefaultArtifactoryClient.class);

  public static final String CHECKSUM_SEARCH_PATH = "/api/search/checksum";

  public static final String TEST_SHA256 = "4909fb971d8373b5a1f5998fb788d6708a626c043a94b05378c54ce5760e4000";

  private final Configuration configuration;

  private final HttpClient httpClient;

  private final HttpClientContext httpClientContext;

  public DefaultArtifactoryClient(Configuration configuration) {
    this.configuration = configuration;
    httpClient = HttpClientUtils.create(this.configuration).build();
    httpClientContext = createHttpClientContext(this.configuration);
  }

  private static HttpClientContext createHttpClientContext(Configuration configuration) {
    Authentication authentication = configuration.getServerAuth();
    if (authentication == null) {
      return null;
    }
    URI uri;
    try {
      uri = new URI(configuration.getServerUrl());
    }
    catch (URISyntaxException e) {
      log.error("Invalid Artifactory server url {}.", configuration.getServerUrl(), e);
      return null;
    }
    HttpHost httpHost = HttpHost.create(uri.getScheme() + "://" + uri.getAuthority());
    BasicCredentialsProvider basicCredentialsProvider = new BasicCredentialsProvider();
    basicCredentialsProvider.setCredentials(AuthScope.ANY,
        new UsernamePasswordCredentials(authentication.getUsername(), String.valueOf(authentication.getPassword())));
    BasicAuthCache basicAuthCache = new BasicAuthCache();
    basicAuthCache.put(httpHost, new BasicScheme());
    HttpClientContext httpClientContext = HttpClientContext.create();
    httpClientContext.setCredentialsProvider(basicCredentialsProvider);
    httpClientContext.setAuthCache(basicAuthCache);
    return httpClientContext;
  }

  @Override
  public ArtifactoryChecksumSearchResults searchByChecksum(ChecksumType checksumType, String checksum)
      throws IOException
  {
    HttpGet request = new HttpGet(this.configuration.getServerUrl() + CHECKSUM_SEARCH_PATH +
        UrlUtils.appendQueryParams(checksumType.name().toLowerCase(Locale.ROOT), checksum));
    HttpResponse response = httpClient.execute(request, httpClientContext);
    if (response.getStatusLine().getStatusCode() != 200) {
      handleError(response);
    }
    return JsonUtils.parse(response.getEntity().getContent(), ArtifactoryChecksumSearchResults.class);
  }

  private void handleError(HttpResponse response) throws IOException {
    ArtifactoryChecksumSearchErrors errors =
        JsonUtils.parse(response.getEntity().getContent(), ArtifactoryChecksumSearchErrors.class);
    ArtifactoryChecksumSearchError error = errors.errors.get(0);
    if (error.status == 401) {
      throw new NotAuthenticatedException(error.message);
    }
    throw new BadGatewayException(error.message);
  }

  @Override
  public Status getServerStatus() throws IOException {
    HttpGet request = new HttpGet(this.configuration.getServerUrl() + CHECKSUM_SEARCH_PATH +
        UrlUtils.appendQueryParams(ChecksumType.SHA256.name().toLowerCase(Locale.ROOT), TEST_SHA256));
    HttpResponse response = httpClient.execute(request, httpClientContext);
    return Status.fromStatusCode(response.getStatusLine().getStatusCode());
  }
}
