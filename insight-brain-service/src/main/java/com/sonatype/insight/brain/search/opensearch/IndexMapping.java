/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.opensearch;

import java.util.HashMap;
import java.util.Map;

import org.opensearch.client.opensearch._types.mapping.Property;

/**
 * Represents the mapping for an index in OpenSearch.
 * <p>
 * This class is used to define the structure of documents within an index.
 * <p>
 * For IQ, we have seven document types:
 * <ul>
 *   <li>ORGANIZATION</li>
 *   <li>APPLICATION</li>
 *   <li>APPLICATION_CATEGORY</li>
 *   <li>COMPONENT</li>
 *   <li>COMPONENT_LABEL</li>
 *   <li>POLICY</li>
 *   <li>VULNERABILITY</li>
 * </ul>
 * We will mash all the documents fields together into a single mapping. Keep in mind that each document should have a
 * document type field to distinguish between them.
 */
public class IndexMapping
{
  private Map<String, Property> mappings = buildDefaultPropertyMappings();

  public Map<String, Property> getMappings() {
    return mappings;
  }

  public void setMappings(final Map<String, Property> mappings) {
    this.mappings = mappings;
  }

  private static Map<String, Property> buildDefaultPropertyMappings() {
    Map<String, Property> propertyMappings = new HashMap<>();

    // Common mappings for all document types
    propertyMappings.put("documentType", createProperty("keyword"));
    propertyMappings.put("id", createProperty("keyword"));
    propertyMappings.put("name", createProperty("text"));

    // Specific mappings for APPLICATION document type
    propertyMappings.put("applicationPublicId", createProperty("keyword"));

    // Specific mappings for APPLICATION_CATEGORY document type
    propertyMappings.put("applicationCategoryColor", createProperty("keyword"));
    propertyMappings.put("applicationCategoryDescription", createProperty("text"));

    // Specific mappings for COMPONENT document type
    propertyMappings.put("componentHash", createProperty("keyword"));
    propertyMappings.put("componentFormat", createProperty("keyword"));
    propertyMappings.put("componentCoordinateGroupId", createProperty("keyword"));
    propertyMappings.put("componentCoordinateArtifactId", createProperty("keyword"));
    propertyMappings.put("componentCoordinateVersion", createProperty("keyword"));
    propertyMappings.put("componentCoordinateClassifier", createProperty("keyword"));
    propertyMappings.put("componentCoordinateExtension", createProperty("keyword"));
    propertyMappings.put("componentCoordinateName", createProperty("text"));
    propertyMappings.put("componentCoordinateQualifier", createProperty("keyword"));
    propertyMappings.put("componentCoordinatePackageld", createProperty("keyword"));
    propertyMappings.put("componentCoordinateArchitecture", createProperty("keyword"));
    propertyMappings.put("componentCoordinatePlatform", createProperty("keyword"));

    // Specific mappings for COMPONENT_LABEL document type
    propertyMappings.put("componentLabelColor", createProperty("text"));
    propertyMappings.put("componentLabelDescription", createProperty("text"));

    // Specific mappings for POLICY document type
    propertyMappings.put("policyThreatCategory", createProperty("text"));
    propertyMappings.put("policyThreatLevel", createProperty("integer"));

    // Specific mappings for VULNERABILITY document type
    propertyMappings.put("reportId", createProperty("keyword"));
    propertyMappings.put("policyEvaluationStage", createProperty("text"));
    propertyMappings.put("vulnerabilityStatus", createProperty("keyword"));
    propertyMappings.put("vulnerabilitySeverity", createProperty("text"));
    propertyMappings.put("vulnerabilityDescription", createProperty("text"));

    return propertyMappings;
  }

  private static Property createProperty(String type) {
    return switch (type) {
      case "keyword" -> new Property.Builder().keyword(k -> k).build();
      case "text" -> new Property.Builder().text(t -> t).build();
      case "integer" -> new Property.Builder().integer(i -> i).build();
      default -> throw new IllegalArgumentException("Unsupported index property type: " + type);
    };
  }
}
