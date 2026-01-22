/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.artifactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response.Status.Family;
import jakarta.ws.rs.core.Response.StatusType;

import com.sonatype.insight.brain.artifactory.client.ArtifactoryChecksumSearchError;
import com.sonatype.insight.brain.artifactory.client.ArtifactoryChecksumSearchErrors;
import com.sonatype.insight.brain.artifactory.client.ArtifactoryChecksumSearchResult;
import com.sonatype.insight.brain.artifactory.client.ArtifactoryChecksumSearchResults;
import com.sonatype.insight.brain.artifactory.client.ArtifactoryQueryLanguageUtils;
import com.sonatype.insight.brain.artifactory.client.ChecksumType;
import com.sonatype.insight.brain.report.RepositoryMatcher;
import com.sonatype.insight.client.utils.Authentication;
import com.sonatype.insight.client.utils.HttpClientUtils;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.UrlUtils;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.error.exception.NotAuthenticatedException;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.collect.Iterables;

public class ArtifactoryClient
{
  private static final Logger log = LoggerFactory.getLogger(ArtifactoryClient.class);

  public static final String CHECKSUM_SEARCH_PATH = "/api/search/checksum";

  public static final String AQL_SEARCH_PATH = "/api/search/aql";

  public static final String TEST_SHA256 = "4909fb971d8373b5a1f5998fb788d6708a626c043a94b05378c54ce5760e4000";

  public static final String ARTIFACTORY_ID_HEADER_NAME = "X-Artifactory-Id";

  private final Configuration configuration;

  private final HttpClient httpClient;

  private final HttpClientContext httpClientContext;

  public ArtifactoryClient(Configuration configuration) {
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
      log.debug("Artifactory URL: {}", configuration.getServerUrl());
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

  public ArtifactoryChecksumSearchResults searchByChecksum(
      ChecksumType checksumType,
      String checksum,
      Set<String> repositories) throws IOException
  {
    HttpGet request = new HttpGet(path(CHECKSUM_SEARCH_PATH) +
        UrlUtils.appendQueryParams(checksumType.name().toLowerCase(Locale.ROOT), checksum, "repos",
            StringUtils.join(repositories, ",")));
    log.debug("Artifactory checksums search request: {}", request.getURI());
    HttpResponse response = httpClient.execute(request, httpClientContext);
    log.debug("Artifactory checksum search response status: {}", response.getStatusLine());
    if (response.getStatusLine().getStatusCode() != 200) {
      handleError(response);
    }
    String responseContent = EntityUtils.toString(response.getEntity());
    log.debug("Artifactory checksums search response: {}", responseContent);
    return JsonUtils.parse(responseContent, ArtifactoryChecksumSearchResults.class);
  }

  public Map<String, ArtifactoryChecksumSearchResults> searchByChecksumsUsingAQL(
      ChecksumType checksumType,
      Set<String> checksums,
      Set<String> repositories,
      Integer aqlBatchSize) throws IOException
  {
    if (CollectionUtils.isEmpty(checksums)) {
      log.debug("No checksums provided for AQL call, returning empty result.");
      return Collections.emptyMap();
    }

    Map<String, ArtifactoryChecksumSearchResults> allResults = new HashMap<>();

    for (List<String> checksumsBatch : Iterables.partition(checksums, aqlBatchSize)) {
      String aqlQuery =
          ArtifactoryQueryLanguageUtils.createChecksumSearch(checksumType, new HashSet<>(checksumsBatch), repositories);
      log.debug("Artifactory AQL query checksum search: {} with batch size: {}", aqlQuery, checksumsBatch.size());
      HttpPost request = new HttpPost(path(AQL_SEARCH_PATH));
      request.setEntity(new StringEntity(aqlQuery));
      HttpResponse response = httpClient.execute(request, httpClientContext);
      log.debug("Artifactory AQL checksums search response status: {}", response.getStatusLine());
      int status = response.getStatusLine().getStatusCode();
      if (status != 200) {
        handleError(status, EntityUtils.toString(response.getEntity()));
      }

      String responseContent = EntityUtils.toString(response.getEntity());
      log.debug("Artifactory AQL checksums search response: {}", responseContent);
      allResults.putAll(convertAQLResultsToArtifactoryChecksumSearchResults(checksumType, checksums,
          new ObjectMapper().readTree(responseContent)));
    }
    return allResults;
  }

  private Map<String, ArtifactoryChecksumSearchResults> convertAQLResultsToArtifactoryChecksumSearchResults(
      ChecksumType checksumType,
      Set<String> checksums,
      JsonNode responseJson)
  {
    Map<String, ArtifactoryChecksumSearchResults> results = new HashMap<>();
    for (JsonNode result : responseJson.path("results")) {
      String sha256 = result.path(checksumType.name().toLowerCase(Locale.ROOT)).asText();
      if (!checksums.contains(sha256)) {
        continue;
      }
      String repo = result.path(ArtifactoryQueryLanguageUtils.FIELD_REPO).asText();
      String path = result.path(ArtifactoryQueryLanguageUtils.FIELD_PATH).asText();
      String name = result.path(ArtifactoryQueryLanguageUtils.FIELD_NAME).asText();
      if (StringUtils.isAnyBlank(sha256, repo, path, name)) {
        continue;
      }
      ArtifactoryChecksumSearchResults artifactoryChecksumSearchResults =
          results.computeIfAbsent(sha256, key -> new ArtifactoryChecksumSearchResults());
      ArtifactoryChecksumSearchResult artifactoryChecksumSearchResult = new ArtifactoryChecksumSearchResult();
      artifactoryChecksumSearchResult.uri = path(RepositoryMatcher.API_STORAGE_PREFIX, repo, path, name);
      artifactoryChecksumSearchResults.results.add(artifactoryChecksumSearchResult);
    }
    return results;
  }

  private void handleError(HttpResponse response) throws IOException {
    String errorContent = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
    log.error("Artifactory error raw response: {}", errorContent);
    ArtifactoryChecksumSearchErrors errors = JsonUtils.parse(errorContent, ArtifactoryChecksumSearchErrors.class);
    ArtifactoryChecksumSearchError error = errors.errors.get(0);
    handleError(error.status, error.message);
  }

  private void handleError(int status, String content) {
    if (status == 401) {
      throw new NotAuthenticatedException(content);
    }
    throw new BadGatewayException(content);
  }

  public StatusType getServerStatusViaQueryParam() throws IOException {
    HttpGet request = new HttpGet(path(CHECKSUM_SEARCH_PATH) +
        UrlUtils.appendQueryParams(ChecksumType.SHA256.name().toLowerCase(Locale.ROOT), TEST_SHA256));
    return getStatusType(httpClient.execute(request, httpClientContext));
  }

  public StatusType getServerStatusViaAQL() throws IOException {
    HttpPost request = new HttpPost(path(AQL_SEARCH_PATH));
    request.setEntity(new StringEntity(ArtifactoryQueryLanguageUtils
        .createChecksumSearch(ChecksumType.SHA256, Collections.singleton(TEST_SHA256), Collections.emptySet())));
    return getStatusType(httpClient.execute(request, httpClientContext));
  }

  // Visible for testing
  String path(String... paths) {
    return UrlUtils.appendUrlPaths(configuration.getServerUrl(), paths);
  }

  private StatusType getStatusType(final HttpResponse response) {
    Header serverHeader = response.getFirstHeader(ARTIFACTORY_ID_HEADER_NAME);
    String server = serverHeader == null ? null : serverHeader.getValue();
    log.debug("Artifactory server header {}, status {}", server, response.getStatusLine());
    if (StringUtils.isBlank(server)) {
      return new StatusType() {
        @Override
        public int getStatusCode() {
          return Status.BAD_REQUEST.getStatusCode();
        }

        @Override
        public Family getFamily() {
          return Status.BAD_REQUEST.getFamily();
        }

        @Override
        public String getReasonPhrase() {
          return Status.BAD_REQUEST.getReasonPhrase() + ". Not a valid Artifactory server.";
        }
      };
    }
    return Status.fromStatusCode(response.getStatusLine().getStatusCode());
  }
}
