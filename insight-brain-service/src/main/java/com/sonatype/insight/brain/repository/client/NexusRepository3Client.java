/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.stream.Collectors;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response.StatusType;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.insight.brain.repository.RepositoryAllVersionsResponse;
import com.sonatype.insight.brain.repository.RepositoryClient;
import com.sonatype.insight.brain.repository.ProxyRepositoryComponentResult;
import com.sonatype.insight.client.utils.AbstractClient;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.error.exception.NotAuthenticatedException;
import com.sonatype.insight.json.store.JsonUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP client for accessing Nexus Repository Manager.
 *
 * @since 1.127
 */
public class NexusRepository3Client
    extends AbstractClient
    implements RepositoryClient
{
  private static final Logger log = LoggerFactory.getLogger(NexusRepository3Client.class);

  private static final Comparator<ProxyRepositoryComponentResult> REPOSITORY_COMPONENT_VERSION_COMPARATOR =
      Comparator.comparing(r -> new ComparableVersion(r.getIdentifier().get(ComponentIdentifier.VERSION)));

  public static final String REPO_VERSION = "version";

  public static final String CONTINUATION_TOKEN_PARAM = "continuationToken";

  public static final String REPO_MAVEN_FORMAT = "maven2";

  public static final String REPO_NPM_FORMAT = "npm";

  public static final String NXRM_STATUS_RESOURCE = "/service/rest/v1/status";

  public static final String NXRM_VERSION_HEADER_NAME = "Server";

  NexusRepository3Client(final Configuration configuration) {
    super(configuration);
  }

  /**
   * gets the component identifiers of the repository assets for the given component coordinates in the ascending order
   * of the asset version. This implementation relies on the version ordering provided by NXRM3
   *
   * @param queryParams asset coordinates (e.g. "group", "maven2.extension", etc)
   * @throws IOException
   */
  @Override
  public RepositoryAllVersionsResponse getAllVersions(Map<String, String> queryParams) throws IOException {
    String continuationToken = null;
    List<ProxyRepositoryComponentResult> results = new ArrayList<>();
    do {
      Result result = path("/service/rest/v1/search/assets")
          .queryWithEmptyParams(toQueryParams(queryParams, continuationToken))
          .get();
      if (result.status() != 200) {
        handleError(result);
      }
      NXRM3SearchResponse searchResponse = JsonUtils.parse(result.data(), NXRM3SearchResponse.class);
      mapToAllVersionsResponse(results, searchResponse);
      continuationToken = searchResponse.continuationToken;
    }
    while (continuationToken != null);
    results = results.stream()
        .collect(Collectors.collectingAndThen(
            Collectors.toCollection(() -> new TreeSet<>(REPOSITORY_COMPONENT_VERSION_COMPARATOR)), ArrayList::new));
    return new RepositoryAllVersionsResponse(results);
  }

  /**
   * Gets the server status as an HTTP status code
   *
   * @return the server status
   * @throws IOException
   */
  @Override
  public StatusType getServerStatus() throws IOException {
    Result result = path(NXRM_STATUS_RESOURCE).get();
    return Status.fromStatusCode(result.status());
  }

  private String[] toQueryParams(final Map<String, String> queryParams, final String continuationToken) {
    List<String> params = new ArrayList<>();
    if (queryParams != null && !queryParams.isEmpty()) {
      for (Entry<String, String> entry : queryParams.entrySet()) {
        params.add(entry.getKey());
        params.add(entry.getValue());
      }
    }
    if (continuationToken != null) {
      params.add(CONTINUATION_TOKEN_PARAM);
      params.add(continuationToken);
    }
    return params.toArray(new String[0]);
  }

  private void mapToAllVersionsResponse(
      final List<ProxyRepositoryComponentResult> results,
      final NXRM3SearchResponse result)
  {
    if (result != null) {
      for (NexusItem item : result.items) {
        ComponentIdentifier id = getComponentIdentifier(item);
        if (id != null) {
          try {
            id.ensureComplete();
            results.add(new ProxyRepositoryComponentResult(id, getSha1Safely(item)));
          }
          catch (InvalidComponentIdentifierException e) {
            log.debug("Repository result contained missing/invalid coordinates", e);
          }
        }
      }
    }
  }

  private String getSha1Safely(final NexusItem item) {
    if (item.checksum != null && StringUtils.isNotBlank(item.checksum.sha1)) {
      return item.checksum.sha1;
    }
    return null;
  }

  private ComponentIdentifier getComponentIdentifier(final NexusItem item) {
    String format = item.format;
    if (REPO_MAVEN_FORMAT.equals(format)) {
      return item.maven2 != null
          ? ComponentIdentifier.createMavenCoordinates(
              item.maven2.get("groupId"),
              item.maven2.get("artifactId"),
              item.maven2.get(REPO_VERSION),
              item.maven2.get("classifier"),
              item.maven2.get("extension"))
          : null;
    }
    else if (REPO_NPM_FORMAT.equals(format)) {
      return item.npm != null
          ? ComponentIdentifier.createNpmCoordinates(
              item.npm.get("name"),
              item.npm.get(REPO_VERSION))
          : null;
    }
    return null;
  }

  private void handleError(final Result result) {
    String message = "could not retrieve search component response from repository manager: " + result.message();
    if (result.status() == 401) {
      throw new NotAuthenticatedException(message);
    }
    throw new BadGatewayException(message);
  }

  public static class NXRM3SearchResponse
  {
    public List<NexusItem> items = new ArrayList<>();

    public String continuationToken;
  }

  public static class NexusItem
  {
    public String format;

    public Map<String, String> maven2;

    public Map<String, String> npm;

    public NexusChecksum checksum;
  }

  public static class NexusChecksum
  {
    public String sha1;
  }
}
