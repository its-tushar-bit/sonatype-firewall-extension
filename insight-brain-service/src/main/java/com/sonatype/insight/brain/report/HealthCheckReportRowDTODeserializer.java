/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.scan.HealthCheckReportRowDTO;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public class HealthCheckReportRowDTODeserializer
    extends JsonDeserializer<HealthCheckReportRowDTO>
{
  @Override
  public HealthCheckReportRowDTO deserialize(final JsonParser jp, final DeserializationContext ctxt)
      throws IOException
  {
    JsonNode node = jp.getCodec().readTree(jp);
    if (node == null) {
      return null;
    }

    // Extract componentIdentifier and hash from the JSON node
    ComponentIdentifier componentIdentifier = null;
    if (node.has("componentIdentifier")) {
      componentIdentifier = jp.getCodec().treeToValue(node.get("componentIdentifier"), ComponentIdentifier.class);
    }

    HealthCheckReportRowDTO dto = new HealthCheckReportRowDTO(componentIdentifier, getFieldSafely(node, "hash"));
    dto.effectiveLicenseThreat = getFieldSafely(node, "effectiveLicenseThreat");
    dto.matchState = getFieldSafely(node, "matchState");
    dto.matchState = getFieldSafely(node, "matchState");
    if (node.has("proprietary")) {
      dto.proprietary = node.get("proprietary").asBoolean();
    }
    dto.declaredLicenses = new HashSet<>();
    dto.observedLicenses = new HashSet<>();
    dto.effectiveLicenses = new HashSet<>();
    appendListSafely(node, "declaredLicenses", dto.declaredLicenses);
    appendListSafely(node, "observedLicenses", dto.observedLicenses);
    appendListSafely(node, "effectiveLicenses", dto.effectiveLicenses);
    return dto;
  }

  private static void appendListSafely(
      final JsonNode node,
      final String vulnerabilityCategories,
      final Set<String> set)
  {
    if (node.has(vulnerabilityCategories) && node.get(vulnerabilityCategories).isArray()) {
      node.get(vulnerabilityCategories).forEach(c -> set.add(c.asText()));
    }
  }

  private String getFieldSafely(final JsonNode node, final String filed) {
    if (node.has(filed)) {
      return node.get(filed).asText();
    }
    return null;
  }
}
