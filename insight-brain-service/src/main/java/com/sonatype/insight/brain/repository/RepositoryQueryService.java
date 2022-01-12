/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryConnectionDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.brain.model.repository.RepositoryFormat;
import com.sonatype.insight.brain.repository.client.RepositoryClientFactory;
import com.sonatype.insight.brain.security.PasswordHandler;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class RepositoryQueryService
{
  private static final Logger log = LoggerFactory.getLogger(RepositoryQueryService.class);

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

  private final OwnerDAO ownerDAO;

  @Inject
  public RepositoryQueryService(
      final RepositoryClientFactory clientFactory,
      final PasswordHandler passwordHandler,
      final RepositoryConnectionDAO repositoryConnectionDAO,
      final OwnerDAO ownerDAO)
  {
    this.clientFactory = clientFactory;
    this.passwordHandler = passwordHandler;
    this.repositoryConnectionDAO = repositoryConnectionDAO;
    this.ownerDAO = ownerDAO;
  }

  public Pair<RepositoryAllVersionsResponse, String> getAllVersions(
      ComponentIdentifier componentIdentifier,
      String ownerId)
  {
    Objects.requireNonNull(ownerId);
    RepositoryFormat repositoryFormat;
    try {
      repositoryFormat = RepositoryFormat.fromString(componentIdentifier.getFormat());
    }
    catch (Exception e) {
      repositoryFormat = RepositoryFormat.GENERIC;
    }
    RepositoryFormat finalRepositoryFormat = repositoryFormat;
    List<RepositoryConnection> repoConnections = null;
    if (isRepositoryConnectionAllowedForOwner(ownerId)) {
      repoConnections = repositoryConnectionDAO.getByOwnerIdWithHierarchy(ownerId).stream()
          .filter(repositoryConnection -> finalRepositoryFormat.equals(repositoryConnection.getFormat()) ||
              RepositoryFormat.GENERIC.equals(repositoryConnection.getFormat()))
          .sorted(REPOSITORY_CONNECTION_COMPARATOR)
          .collect(Collectors.toList());
    }
    if (CollectionUtils.isEmpty(repoConnections)) {
      return Pair.of(new RepositoryAllVersionsResponse(Collections.emptyList()), null);
    }

    //for the time being we only support one repository connection - cf. CLM-19789
    RepositoryConnection connection = repoConnections.get(0);
    return Pair.of(searchRepositoryForAllVersions(connection, componentIdentifier), connection.getBaseUrl());
  }

  @VisibleForTesting
  boolean isRepositoryConnectionAllowedForOwner(String ownerId) {
    Boolean isEnabled = null;
    for (Owner owner : ownerDAO.walkHierarchy(ownerId)) {
      switch (owner.getType()) {
        case APPLICATION: {
          Application application = (Application) owner;
          isEnabled = application.isRepositoryConnectionEnabled();
          break;
        }
        case ORGANIZATION: {
          Organization organization = (Organization) owner;
          if (isEnabled != null && organization.isAllowRepositoryConnectionOverride()) {
            return isEnabled;
          }
          isEnabled = organization.isRepositoryConnectionEnabled();
          break;
        }
        default: {
          throw new IllegalStateException("Unknown owner type: " + owner.getType());
        }
      }
    }
    if (isEnabled == null) {
      isEnabled = false;
    }
    return isEnabled;
  }

  private RepositoryAllVersionsResponse searchRepositoryForAllVersions(
      RepositoryConnection connection,
      ComponentIdentifier componentIdentifier)
  {
    RepositoryAllVersionsResponse response = new RepositoryAllVersionsResponse(Collections.emptyList());
    Map<String, String> queryParams = getQueryCriteriaForNexus3(componentIdentifier);
    if (!queryParams.isEmpty()) {
      try {
        RepositoryClient client = clientFactory.create()
            .forNexus3(connection.getBaseUrl(), connection.getUsername(),
                passwordHandler.decryptPassword(connection.getPassword()));
        response = client.getAllVersions(queryParams);
      }
      catch (IOException e) {
        log.debug(String.format("unable to retrieve component versions from repository manager: %s",
            connection.getBaseUrl()), e);
      }
    }
    return response;
  }

  private Map<String, String> getQueryCriteriaForNexus3(ComponentIdentifier componentIdentifier) {
    Map<String, String> queryCriteria = new HashMap<>();
    String format = componentIdentifier.getFormat();
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
}
