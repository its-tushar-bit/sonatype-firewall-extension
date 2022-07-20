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

public class ArtifactoryQueryLanguageUtils
{
  public static final String FIELD_REPO = "repo";

  public static final String FIELD_PATH = "path";

  public static final String FIELD_NAME = "name";

  private ArtifactoryQueryLanguageUtils() {
    // Utility class
  }

  public static String createChecksumSearch(ChecksumType checksumType, Set<String> checksums) {
    ObjectMapper objectMapper = new ObjectMapper();
    ObjectNode criteria = objectMapper.createObjectNode();
    ArrayNode or = criteria.putArray("$or");
    String checksumTypeNameLowercase = checksumType.name().toLowerCase(Locale.ROOT);
    for (String checksum : checksums) {
      or.addObject().put(checksumTypeNameLowercase, checksum);
    }
    try {
      return "items.find(" + objectMapper.writeValueAsString(criteria) + ").include(\"" +
          String.join("\",\"", Arrays.asList(checksumTypeNameLowercase, FIELD_REPO, FIELD_PATH, FIELD_NAME)) + "\")";
    }
    catch (JsonProcessingException e) {
      throw new UncheckedIOException(e.getMessage(), e);
    }
  }
}
