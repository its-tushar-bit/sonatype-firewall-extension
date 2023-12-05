/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.sonatype.clm.dto.model.sourcecontrol.ApiSourceControlRepositoryUserDTO;
import com.sonatype.insight.client.utils.AbstractClient;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;
import com.sonatype.nexus.git.utils.api.GitException;
import com.sonatype.nexus.git.utils.repository.JGitRecentCommitterFinder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to access SourceControl API
 *
 * @since 1.72
 */
public class SourceControlClient
    extends AbstractClient
{
  private static final Logger log = LoggerFactory.getLogger(SourceControlClient.class);

  public static final ObjectMapper mapper = defaultMapper();

  public SourceControlClient(Configuration config) {
    super(config);
  }

  public int addOrUpdateSourceControlRecord(String publicId, String repositoryUrl) throws IOException {
    return addOrUpdateSourceControlRecord(publicId, repositoryUrl, null);
  }

  public int addOrUpdateSourceControlRecord(String publicId, String repositoryUrl, String repositoryPath)
      throws IOException
  {
    Result result = path("api", "v2", "sourceControl")
        .query("publicId", publicId, "repositoryUrl", repositoryUrl)
        .post(getApiSourceControlRepositoryUserDTOEntity(repositoryPath));
    verifyStatusCode(result);
    return result.status();
  }

  private ByteArrayEntity getApiSourceControlRepositoryUserDTOEntity(
      String repositoryPath)
      throws IOException
  {
    Map<String, Collection<Instant>> recentCommitters = Collections.emptyMap();
    try {
      recentCommitters = getRecentCommitters(repositoryPath, 90);
    }
    catch (GitException e) {
      log.warn("Cannot get map of recent committers: {}", e.getMessage());
    }
    if (!recentCommitters.isEmpty()) {
      ApiSourceControlRepositoryUserDTO apiSourceControlRepoUserDTO =
          new ApiSourceControlRepositoryUserDTO(recentCommitters);
      return new ByteArrayEntity(mapper.writeValueAsBytes(apiSourceControlRepoUserDTO), ContentType.APPLICATION_JSON);
    }
    return null;
  }

  Map<String, Collection<Instant>> getRecentCommitters(String repositoryPath, int daysPeriod) throws GitException {
    return new JGitRecentCommitterFinder(repositoryPath, daysPeriod).tryGetRecentCommitters();
  }

  private static ObjectMapper defaultMapper() {
    return JsonMapper.builder()
        .addModule(new JavaTimeModule())
        .build();
  }
}
