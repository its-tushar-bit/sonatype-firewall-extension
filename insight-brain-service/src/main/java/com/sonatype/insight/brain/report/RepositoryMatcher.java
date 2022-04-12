/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.experimental.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.artifactory.ArtifactoryClientFactory;
import com.sonatype.insight.brain.artifactory.client.ArtifactoryChecksumSearchResult;
import com.sonatype.insight.brain.artifactory.client.ArtifactoryChecksumSearchResults;
import com.sonatype.insight.brain.artifactory.client.ArtifactoryClient;
import com.sonatype.insight.brain.artifactory.client.ChecksumType;
import com.sonatype.insight.brain.dataaccess.artifactory.ArtifactoryConnectionDAO;
import com.sonatype.insight.brain.model.artifactory.ArtifactoryConnection;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.security.PasswordHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;

@Named
public class RepositoryMatcher
{
  private static final Logger log = LoggerFactory.getLogger(RepositoryMatcher.class);

  private static final Set<String> MATCHABLE_STATUSES =
      ImmutableSet.of(MatchState.UNKNOWN.getId(), MatchState.SIMILAR.getId());

  private static final Set<String> MATCHABLE_EXTENSIONS = ImmutableSet.of("jar");

  private static final String FIELD_NAME_SHA256 = "sha256";

  private static final String FIELD_NAME_PROPRIETARY = "proprietary";

  private static final String FIELD_NAME_MATCH_STATE = "matchState";

  private static final String ARTIFACTORY_API_STORAGE_PREFIX = "/artifactory/api/storage/";

  public static final String FIELD_NAME_FILENAMES = "filenames";

  public static final String FIELD_NAMES_PATHNAMES = "pathnames";

  private final ArtifactoryConnectionDAO artifactoryConnectionDao;

  private final ArtifactoryClientFactory artifactoryClientFactory;

  private final PasswordHandler passwordHandler;

  @Inject
  public RepositoryMatcher(
      final ArtifactoryConnectionDAO artifactoryConnectionDao,
      final ArtifactoryClientFactory artifactoryClientFactory,
      final PasswordHandler passwordHandler)
  {
    this.artifactoryConnectionDao = artifactoryConnectionDao;
    this.artifactoryClientFactory = artifactoryClientFactory;
    this.passwordHandler = passwordHandler;
  }

  public void match(final JsonNode bomJson) {
    if (SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.isEnabled()) {
      long start = System.currentTimeMillis();
      Map<ComponentIdentifier, ObjectNode> sha256Matched = identify(bomJson);
      log.debug("performed repository matching in {} seconds with {} identified results",
          (System.currentTimeMillis() - start) / 1000, sha256Matched.size());
      //handle hds matching
    }
  }

  //visible for testing
  Map<ComponentIdentifier, ObjectNode> identify(final JsonNode bomJson) {
    Map<ComponentIdentifier, ObjectNode> identifiedComponents = new HashMap<>();
    Set<ObjectNode> filteredNodes = filterMatchableNodes(bomJson);
    if (CollectionUtils.isNotEmpty(filteredNodes)) {
      List<ArtifactoryConnection> artifactoryConnections = artifactoryConnectionDao.getByOwnerId(ROOT_ORGANIZATION_ID);
      if (CollectionUtils.isNotEmpty(artifactoryConnections)) {
        ArtifactoryConnection rootConnection = artifactoryConnections.get(0);
        ArtifactoryClient artifactoryClient = artifactoryClientFactory.create()
            .forArtifactory(rootConnection.getBaseUrl(), rootConnection.getUsername(),
                passwordHandler.decryptPassword(rootConnection.getPassword()));
        for (ObjectNode node : filteredNodes) {
          if (!matchWithRepository(identifiedComponents, rootConnection, artifactoryClient, node)) {
            break; // avoid checksum search in case of any connection errors to repository
          }
        }
      }
    }
    return identifiedComponents;
  }

  private static boolean matchWithRepository(
      final Map<ComponentIdentifier, ObjectNode> identifiedComponents,
      final ArtifactoryConnection rootConnection,
      final ArtifactoryClient artifactoryClient,
      final ObjectNode node)
  {
    String sha256 = node.get(FIELD_NAME_SHA256).asText();
    try {
      ArtifactoryChecksumSearchResults artifactoryChecksumSearchResults =
          artifactoryClient.searchByChecksum(ChecksumType.SHA256, sha256);
      if (CollectionUtils.isNotEmpty(artifactoryChecksumSearchResults.results)) {
        ComponentIdentifier resolvedId = resolveComponentIdentifier(artifactoryChecksumSearchResults);
        if (resolvedId != null) {
          identifiedComponents.put(resolvedId, node);
        }
        else {
          log.debug("no recognizable artifact found in repository for sha256={}", sha256);
        }
      }
      return true;
    }
    catch (IOException e) {
      log.error("Checksum search error for repository connection uri {}", rootConnection.getBaseUrl(), e);
      return false;
    }
  }

  private static ComponentIdentifier resolveComponentIdentifier(
      final ArtifactoryChecksumSearchResults artifactoryChecksumSearchResults)
  {
    for (ArtifactoryChecksumSearchResult result : artifactoryChecksumSearchResults.results) {
      ComponentIdentifier resolvedId = resolveComponentIdentifierFromUri(result.uri);
      if (resolvedId != null) {
        return resolvedId;
      }
    }
    return null;
  }

  //visible for testing
  static ComponentIdentifier resolveComponentIdentifierFromUri(final String uriString) {
    if (StringUtils.isBlank(uriString)) {
      return null;
    }

    try {
      URI uri = new URI(uriString);
      String[] pathParts = StringUtils.split(
          StringUtils.removeStart(uri.getPath(), ARTIFACTORY_API_STORAGE_PREFIX), "/");
      if (pathParts.length >= 4) {
        String extension = resolveExtension(pathParts[pathParts.length - 1]);
        pathParts = ArrayUtils.removeAll(pathParts, 0, pathParts.length - 1); // remove repository and filename
        String version = pathParts[pathParts.length - 1];
        String name = pathParts[pathParts.length - 2];
        String namespace = StringUtils.join(
            ArrayUtils.removeAll(pathParts, pathParts.length - 1, pathParts.length - 2), ".");
        return ComponentIdentifier.createMavenCoordinates(namespace, name, version, null, extension);
      }
    }
    catch (URISyntaxException e) {
      log.debug("bad result uri from artifactory {}", uriString, e);
    }
    return null;
  }

  private static String resolveExtension(final String pathPart) {
    if (StringUtils.isBlank(pathPart)) {
      return null;
    }

    return FilenameUtils.getExtension(pathPart);
  }

  private static Set<ObjectNode> filterMatchableNodes(final JsonNode bomJson) {
    Set<ObjectNode> filteredNodes = new HashSet<>();
    JsonNode aaData = bomJson.get("aaData");
    for (JsonNode bomJsonNode : aaData) {
      ObjectNode bomObjectNode = (ObjectNode) bomJsonNode;
      if (hasSha256(bomObjectNode) &&
          hasMatchableStatus(bomObjectNode) &&
          isOfMatchableFileType(bomObjectNode) &&
          notProprietary(bomObjectNode)) {
        filteredNodes.add(bomObjectNode);
      }
    }
    return filteredNodes;
  }

  private static boolean hasSha256(final ObjectNode bomObjectNode) {
    return bomObjectNode.hasNonNull(FIELD_NAME_SHA256);
  }

  private static boolean notProprietary(final ObjectNode bomObjectNode) {
    JsonNode proprietaryNode = bomObjectNode.get(FIELD_NAME_PROPRIETARY);
    return proprietaryNode != null && !proprietaryNode.asBoolean(false);
  }

  private static boolean isOfMatchableFileType(final ObjectNode bomObjectNode) {
    return containsMatchableExtension(bomObjectNode, FIELD_NAME_FILENAMES) ||
        containsMatchableExtension(bomObjectNode, FIELD_NAMES_PATHNAMES);
  }

  private static boolean containsMatchableExtension(ObjectNode bomObjectNode, String arrayFieldName) {
    JsonNode arrayFieldNode = bomObjectNode.get(arrayFieldName);
    if (arrayFieldNode != null) {
      for (JsonNode pathElement : arrayFieldNode) {
        String extension = StringUtils.lowerCase(FilenameUtils.getExtension(pathElement.asText()), Locale.ROOT);
        if (extension != null && MATCHABLE_EXTENSIONS.contains(extension)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean hasMatchableStatus(final ObjectNode bomObjectNode) {
    JsonNode matchStateNode = bomObjectNode.get(FIELD_NAME_MATCH_STATE);
    return matchStateNode != null && MATCHABLE_STATUSES.contains(matchStateNode.asText());
  }
}
