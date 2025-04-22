/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.scan.HealthCheckReportSecurityRowDTO;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public class HealthCheckReportSecurityRowDTODeserializer
    extends JsonDeserializer<HealthCheckReportSecurityRowDTO>
{
  @Override
  public HealthCheckReportSecurityRowDTO deserialize(final JsonParser jp, final DeserializationContext ctxt)
      throws IOException
  {
    JsonNode node = jp.getCodec().readTree(jp);
    if (node == null) {
      return null;
    }

    // Extract componentIdentifier and hash from the JSON node
    ComponentIdentifier componentIdentifier = null;
    String hash = null;

    if (node.has("componentIdentifier")) {
      componentIdentifier = jp.getCodec().treeToValue(node.get("componentIdentifier"), ComponentIdentifier.class);
    }

    if (node.has("hash")) {
      hash = node.get("hash").asText();
    }
    HealthCheckReportSecurityRowDTO dto = new HealthCheckReportSecurityRowDTO(componentIdentifier, hash);
    if (node.has("score")) {
      dto.score = node.get("score").floatValue();
    }
    dto.url = getFieldSafely(node, "url");
    dto.reference = getFieldSafely(node, "reference");
    dto.source = getFieldSafely(node, "source");
    dto.summary = getFieldSafely(node, "summary");
    dto.cwe = getFieldSafely(node, "cwe");
    dto.cvssVectorString = getFieldSafely(node, "cvssVectorString");
    dto.cvssVectorSource = getFieldSafely(node, "cvssVectorSource");
    dto.matchState = getFieldSafely(node, "matchState");
    if (node.has("proprietary")) {
      dto.proprietary = node.get("proprietary").asBoolean();
    }
    dto.vulnerabilityCategories = new ArrayList<>();
    dto.aliases = new ArrayList<>();
    dto.vulnIds = new ArrayList<>();
    appendListSafely(node, "vulnerabilityCategories", dto.vulnerabilityCategories);
    appendListSafely(node, "aliases", dto.aliases);
    appendListSafely(node, "vulnIds", dto.vulnIds);
    return dto;
  }

  private static void appendListSafely(
      final JsonNode node,
      final String vulnerabilityCategories,
      final List<String> dto)
  {
    if (node.has(vulnerabilityCategories) && node.get(vulnerabilityCategories).isArray()) {
      node.get(vulnerabilityCategories).forEach(c -> dto.add(c.asText()));
    }
  }

  private String getFieldSafely(final JsonNode node, final String filed) {
    if (node.has(filed)) {
      return node.get(filed).asText();
    }
    return null;
  }
}
