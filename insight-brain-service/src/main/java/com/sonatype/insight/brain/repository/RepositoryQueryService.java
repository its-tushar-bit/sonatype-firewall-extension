/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryConnectionStatusResponseDTO;
import com.sonatype.insight.brain.api.v2.service.ApiRepositoryConnectionService;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryConnectionDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.brain.model.repository.RepositoryFormat;
import com.sonatype.insight.brain.repository.client.RepositoryClientFactory;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.telemetry.TelemetryCollector;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class RepositoryQueryService
    implements TelemetryCollector
{
  private static final Logger log = LoggerFactory.getLogger(RepositoryQueryService.class);

  //visible for testing
  public static final TenantReference<Map<String, LongAdder>> REPOSITORY_QUERY_COUNT_PER_FORMAT =
          new TenantReference<>(ConcurrentHashMap::new);

  public static final String INNERSOURCE_REPOSITORY_FORMAT_KEY = "innersource_repository_format";

  public static final String INNERSOURCE_REPOSITORY_QUERY_COUNT_KEY = "innersource_repository_query_count";

  public static final String MAVEN_DEFAULT_EXTENSION = "jar";

  public static final String NEXUS3_QUERY_NAME_KEY = "name";

  public static final String NEXUS3_QUERY_MAVEN_GROUP_KEY = "group";

  public static final String NEXUS3_QUERY_MAVEN_EXTENSION_KEY = "maven.extension";

  public static final String NEXUS3_QUERY_MAVEN_CLASSIFIER_KEY = "maven.classifier";

  public static final String NEXUS3_QUERY_MAVEN_DEFAULT_CLASSIFIER = "";

  private static final String NEXUS3_QUERY_NPM_SCOPE_KEY = "npm.scope";

  private static final Comparator<RepositoryConnection> REPOSITORY_CONNECTION_COMPARATOR = (c1, c2) -> {
    if (c1.getFormat().equals(c2.getFormat())) {
      return 0;
    }
    if (c1.getFormat().equals(RepositoryFormat.GENERIC)) {
      return 1;
    }
    else {
      return -1;
    }
  };

  private final RepositoryClientFactory clientFactory;

  private final PasswordHandler passwordHandler;

  private final RepositoryConnectionDAO repositoryConnectionDAO;

  private final ApiRepositoryConnectionService repositoryConnectionService;

  @Inject
  public RepositoryQueryService(
      final RepositoryClientFactory clientFactory,
      final PasswordHandler passwordHandler,
      final RepositoryConnectionDAO repositoryConnectionDAO,
      final ApiRepositoryConnectionService repositoryConnectionService)
  {
    this.clientFactory = clientFactory;
    this.passwordHandler = passwordHandler;
    this.repositoryConnectionDAO = repositoryConnectionDAO;
    this.repositoryConnectionService = repositoryConnectionService;
  }

  public Pair<RepositoryAllVersionsResponse, RepositorySourceResponseDTO> getAllVersions(
      ComponentIdentifier componentIdentifier,
      Owner owner)
  {
    validateComponentIdentifier(componentIdentifier);
    RepositoryFormat repositoryFormat;
    try {
      repositoryFormat = RepositoryFormat.fromString(componentIdentifier.getFormat());
    }
    catch (Exception e) {
      repositoryFormat = RepositoryFormat.GENERIC;
    }
    RepositoryFormat finalRepositoryFormat = repositoryFormat;

    ApiRepositoryConnectionStatusResponseDTO statusDTO =
        repositoryConnectionService.getOwnerRepositoryConnectionStatus(owner.getType(), owner.getId());
    String effectiveOwnerId = null;

    if (Boolean.TRUE.equals(statusDTO.inheritedFromOrgEnabled)) {
      effectiveOwnerId = statusDTO.inheritedFromOrganizationId;
    }
    else if (statusDTO.allowChange && Boolean.TRUE.equals(statusDTO.enabled)) {
      effectiveOwnerId = owner.getId();
    }

    if (effectiveOwnerId != null) {
      List<RepositoryConnection> repoConnections = repositoryConnectionDAO.getByOwnerId(effectiveOwnerId).stream()
          .filter(repositoryConnection -> finalRepositoryFormat.equals(repositoryConnection.getFormat()) ||
              RepositoryFormat.GENERIC.equals(repositoryConnection.getFormat()))
          .sorted(REPOSITORY_CONNECTION_COMPARATOR)
          .collect(Collectors.toList());
      if (CollectionUtils.isEmpty(repoConnections)) {
        return Pair.of(new RepositoryAllVersionsResponse(Collections.emptyList()), null);
      }

      //for the time being we only support one repository connection - cf. CLM-19789
      RepositoryConnection connection = repoConnections.get(0);
      return searchRepositoryForAllVersions(connection, componentIdentifier);
    }
    else {
      return Pair.of(new RepositoryAllVersionsResponse(Collections.emptyList()), null);
    }
  }

  private void validateComponentIdentifier(ComponentIdentifier componentIdentifier) {
    try {
      componentIdentifier.createAlternativeVersion("*").ensureRequired();
    }
    catch (InvalidComponentIdentifierException e) {
      if (componentIdentifier.isMaven()) {
        if (componentIdentifier.get(ComponentIdentifier.MAVEN_GROUP_ID) == null ||
            componentIdentifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID) == null) {
          throw new BadRequestException(e.getMessage(), e);
        }
      }
      else {
        throw new BadRequestException(e.getMessage(), e);
      }
    }
  }

  private Pair<RepositoryAllVersionsResponse, RepositorySourceResponseDTO> searchRepositoryForAllVersions(
      RepositoryConnection connection,
      ComponentIdentifier componentIdentifier)
  {
    RepositoryAllVersionsResponse response = new RepositoryAllVersionsResponse(Collections.emptyList());
    RepositorySourceResponseDTO sourceResponseDTO = new RepositorySourceResponseDTO();
    sourceResponseDTO.source = connection.getBaseUrl();
    Map<String, String> queryParams = getQueryCriteriaForNexus3(componentIdentifier);
    if (!queryParams.isEmpty()) {
      try {
        RepositoryClient client = clientFactory.create()
            .forNexus3(connection.getBaseUrl(), connection.getUsername(),
                passwordHandler.decryptPassword(connection.getPassword()));
        response = client.getAllVersions(queryParams);
      }
      catch (Exception e) {
        String errorMessage = String.format("unable to retrieve component versions from repository manager: %s",
            connection.getBaseUrl());
        sourceResponseDTO.sourceMessage =
            "Could not retrieve data from InnerSource repository. Check your repository configuration.";
        log.debug(errorMessage, e);
      }
    }
    if (sourceResponseDTO.sourceMessage == null && response.getComponents().isEmpty()) {
      sourceResponseDTO.sourceMessage =
          "No component versions returned from InnerSource repository. This may be due to insufficient privileges.";
    }
    return Pair.of(response, sourceResponseDTO);
  }

  private Map<String, String> getQueryCriteriaForNexus3(ComponentIdentifier componentIdentifier) {
    Map<String, String> queryCriteria = new HashMap<>();
    String format = componentIdentifier.getFormat();
    REPOSITORY_QUERY_COUNT_PER_FORMAT.get().computeIfAbsent(format, key -> new LongAdder()).increment();
    switch (format) {
      case ComponentIdentifier.FORMAT_MAVEN:
        buildMavenQueryCriteria(componentIdentifier, queryCriteria);
        break;
      case ComponentIdentifier.FORMAT_NPM:
        buildNpmQueryCriteria(componentIdentifier, queryCriteria);
        break;
      default:
        //no-op
    }
    return queryCriteria;
  }

  private void buildNpmQueryCriteria(
      final ComponentIdentifier componentIdentifier,
      final Map<String, String> queryCriteria)
  {
    String[] npmPackageId = componentIdentifier.get(ComponentIdentifier.NPM_PACKAGE_ID).split("/");
    String packageName = npmPackageId.length == 1 ? npmPackageId[0] : npmPackageId[1];
    queryCriteria.put(NEXUS3_QUERY_NAME_KEY, packageName);
    if (npmPackageId.length == 2) {
      queryCriteria.put(NEXUS3_QUERY_NPM_SCOPE_KEY, npmPackageId[0]);
    }
  }

  private void buildMavenQueryCriteria(
      final ComponentIdentifier componentIdentifier,
      final Map<String, String> queryCriteria)
  {
    queryCriteria.put(NEXUS3_QUERY_MAVEN_GROUP_KEY, componentIdentifier.get(ComponentIdentifier.MAVEN_GROUP_ID));
    queryCriteria.put(NEXUS3_QUERY_NAME_KEY, componentIdentifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID));
    queryCriteria.put(NEXUS3_QUERY_MAVEN_EXTENSION_KEY,
        getCoordOrDefault(componentIdentifier, ComponentIdentifier.MAVEN_EXTENSION, MAVEN_DEFAULT_EXTENSION));
    queryCriteria.put(NEXUS3_QUERY_MAVEN_CLASSIFIER_KEY, getCoordOrDefault(
        componentIdentifier, ComponentIdentifier.MAVEN_CLASSIFIER, NEXUS3_QUERY_MAVEN_DEFAULT_CLASSIFIER));
  }

  private String getCoordOrDefault(
      ComponentIdentifier componentIdentifier,
      String coordinateName,
      String defaultValue)
  {
    String value = componentIdentifier.get(coordinateName);
    return StringUtils.isNotBlank(value) ? value : defaultValue;
  }

  @Override
  public List<TelemetryData> collectAllData() {
    List<TelemetryData> telemetryData = REPOSITORY_QUERY_COUNT_PER_FORMAT.get().entrySet().stream()
        .map(this::createTelemetryData)
        .collect(Collectors.toList());
    REPOSITORY_QUERY_COUNT_PER_FORMAT.get().clear();
    return telemetryData;
  }

  private TelemetryData createTelemetryData(Entry<String, LongAdder> entry) {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.INNER_SOURCE_REPOSITORY_USAGE);
    telemetryData.put(INNERSOURCE_REPOSITORY_FORMAT_KEY, entry.getKey());
    telemetryData.put(INNERSOURCE_REPOSITORY_QUERY_COUNT_KEY, entry.getValue().intValue());
    return telemetryData;
  }

  @Override
  public boolean isClusterTelemetry() {
    return false;
  }
}
