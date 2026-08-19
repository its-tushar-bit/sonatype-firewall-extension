/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.artifactory.client;

import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;

public class ArtifactoryQueryLanguageUtils
{
  public static final String FIELD_REPO = "repo";

  public static final String FIELD_PATH = "path";

  public static final String FIELD_NAME = "name";

  private ArtifactoryQueryLanguageUtils() {
    // Utility class
  }

  public static String createChecksumSearch(ChecksumType checksumType, Set<String> checksums, Set<String> repos) {
    ObjectMapper objectMapper = new ObjectMapper();
    ObjectNode checksumsCriteria = getSearchCriteria(objectMapper, checksums,
        checksumType.name().toLowerCase(Locale.ROOT));
    ObjectNode repositoriesCriteria = getSearchCriteria(objectMapper, repos, FIELD_REPO);
    String checksumTypeNameLowercase = checksumType.name().toLowerCase(Locale.ROOT);
    StringBuilder queryBuilder = new StringBuilder();
    try {
      return queryBuilder.append("items.find(")
          .append(objectMapper.writeValueAsString(checksumsCriteria))
          .append(repositoriesCriteria.get("$or").size() > 0
              ? "," + objectMapper.writeValueAsString(repositoriesCriteria)
              : "")
          .append(").include(\"")
          .append(String.join("\",\"", Arrays.asList(checksumTypeNameLowercase, FIELD_REPO, FIELD_PATH, FIELD_NAME)))
          .append("\")")
          .toString();
    }
    catch (JsonProcessingException e) {
      throw new UncheckedIOException(e.getMessage(), e);
    }
  }

  public static ObjectNode getSearchCriteria(ObjectMapper objectMapper, Set<String> data, String propName) {
    ObjectNode criteria = objectMapper.createObjectNode();
    ArrayNode or = criteria.putArray("$or");
    for (String term : data) {
      if (StringUtils.isNotBlank(term)) {
        or.addObject().put(propName, term);
      }
    }
    return criteria;
  }
}
