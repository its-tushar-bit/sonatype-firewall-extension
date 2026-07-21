/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.opensearch;

import java.util.HashMap;
import java.util.Map;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.ItemType;

import org.apache.commons.lang.StringUtils;
import org.opensearch.client.opensearch._types.mapping.Property;

/**
 * Represents the mapping for an index in OpenSearch.
 * <p>
 * This class is used to define the structure of documents within an index.
 * <p>
 * For IQ, we have several document types defined in {@link ItemType} with fields defined in {@link FieldIdentifier}.
 * <p>
 * We will mash all the documents fields together into a single mapping. Keep in mind that each document should have a
 * document type field to distinguish between them.
 * <p>
 * We will also add a field in epoch format to help determine last index time.
 */
public class IndexMapping
{
  public static final String CREATED_AT_EPOCH_MS = "createdAtEpochMs";

  private Map<String, Property> mappings = buildDefaultPropertyMappings();

  public Map<String, Property> getMappings() {
    return mappings;
  }

  public void setMappings(final Map<String, Property> mappings) {
    this.mappings = mappings;
  }

  private static Map<String, Property> buildDefaultPropertyMappings() {
    Map<String, Property> propertyMappings = new HashMap<>();

    propertyMappings.put(FieldIdentifier.ITEM_TYPE.label, createProperty("keyword"));

    propertyMappings.put(FieldIdentifier.ORGANIZATION_ID.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.ORGANIZATION_NAME.label, createProperty("keyword"));

    propertyMappings.put(FieldIdentifier.APPLICATION_ID.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.APPLICATION_NAME.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.APPLICATION_PUBLIC_ID.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.APPLICATION_VERSION.label, createProperty("keyword"));

    propertyMappings.put(FieldIdentifier.POLICY_EVALUATION_STAGE.label, createProperty("keyword"));

    propertyMappings.put(FieldIdentifier.REPORT_ID.label, createProperty("keyword"));

    propertyMappings.put(FieldIdentifier.COMPONENT_HASH.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.COMPONENT_FORMAT.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.COMPONENT_NAME.label, createProperty("keyword"));
    for (String format : ComponentIdentifier.getAllFormats()) {
      for (String coordinateName : ComponentIdentifier.getAllCoordinateNames(format)) {
        propertyMappings.put(FieldIdentifier.COMPONENT_COORDINATE.label + StringUtils.capitalize(coordinateName),
            createProperty("keyword"));
      }
    }

    propertyMappings.put(FieldIdentifier.VULNERABILITY_ID.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.VULNERABILITY_SEVERITY.label, createProperty("float"));
    propertyMappings.put(FieldIdentifier.VULNERABILITY_STATUS.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.VULNERABILITY_DESCRIPTION.label, createProperty("text"));

    propertyMappings.put(FieldIdentifier.APPLICATION_CATEGORY_ID.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.APPLICATION_CATEGORY_NAME.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.APPLICATION_CATEGORY_COLOR.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.APPLICATION_CATEGORY_DESCRIPTION.label, createProperty("text"));

    propertyMappings.put(FieldIdentifier.COMPONENT_LABEL_ID.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.COMPONENT_LABEL_NAME.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.COMPONENT_LABEL_COLOR.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.COMPONENT_LABEL_DESCRIPTION.label, createProperty("text"));

    propertyMappings.put(FieldIdentifier.POLICY_ID.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.POLICY_NAME.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.POLICY_THREAT_CATEGORY.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.POLICY_THREAT_LEVEL.label, createProperty("integer"));

    propertyMappings.put(FieldIdentifier.PARENT_ORGANIZATION_ID.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.PARENT_ORGANIZATION_NAME.label, createProperty("keyword"));

    propertyMappings.put(FieldIdentifier.SBOM_SPECIFICATION.label, createProperty("keyword"));

    propertyMappings.put(FieldIdentifier.POLICY_VIOLATION_ID.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.POLICY_VIOLATION_THREAT_CATEGORY.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL.label, createProperty("integer"));
    propertyMappings.put(FieldIdentifier.POLICY_VIOLATION_POLICY_NAME.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.POLICY_VIOLATION_POLICY_ID.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.POLICY_VIOLATION_WAIVER_STATUS.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.POLICY_VIOLATION_CONSTRAINT_NAME.label, createProperty("keyword"));

    propertyMappings.put(FieldIdentifier.COMPONENT_EFFECTIVE_LICENSE_ID.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.COMPONENT_EFFECTIVE_LICENSE_NAME.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.COMPONENT_LICENSE_THREAT_GROUP_NAME.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.COMPONENT_LICENSE_THREAT_LEVEL.label, createProperty("integer"));

    // TODO(CLM-41642): these explicit policyWaiver* mappings only apply to a freshly created index.
    // On upgrade, incremental waiver enqueue writes docs into the pre-existing index (which lacks
    // these fields) before any full reindex, so OpenSearch dynamic-maps policyWaiver* fields (dates
    // vs keyword, text vs keyword). It self-heals on the next full reindex. The read path must
    // reindex-gate waiver queries or tolerate the dynamically mapped types on a pre-existing index.
    propertyMappings.put(FieldIdentifier.POLICY_WAIVER_ID.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.POLICY_WAIVER_POLICY_NAME.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.POLICY_WAIVER_POLICY_ID.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.POLICY_WAIVER_REASON.label, createProperty("text"));
    propertyMappings.put(FieldIdentifier.POLICY_WAIVER_COMMENT.label, createProperty("text"));
    // ISO-8601 stored as a single keyword token (DocumentBuilder intent); must not date-detect.
    propertyMappings.put(FieldIdentifier.POLICY_WAIVER_CREATED_AT.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.POLICY_WAIVER_EXPIRES_AT.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.POLICY_WAIVER_SCOPE_OWNER_ID.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.POLICY_WAIVER_SCOPE_OWNER_TYPE.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.POLICY_WAIVER_THREAT_LEVEL.label, createProperty("integer"));
    propertyMappings.put(FieldIdentifier.POLICY_WAIVER_WAIVED_BY.label, createProperty("keyword"));

    // Denormalized permission-filter field. Case-sensitive keyword (no normalizer) so opaque
    // context IDs are matched byte-for-byte. Multi-valued (set per document by DocumentBuilder).
    propertyMappings.put(FieldIdentifier.ALLOWED_CONTEXT_IDS.label, createProperty("keyword_case_sensitive"));

    // Case-sensitive so the hash sorts identically to the Lucene doc-values order.
    propertyMappings.put(FieldIdentifier.DOCUMENT_KEY.label, createProperty("keyword_case_sensitive"));

    propertyMappings.put(CREATED_AT_EPOCH_MS, createProperty("long"));

    return propertyMappings;
  }

  private static Property createProperty(String type) {
    return switch (type) {
      case "keyword" -> new Property.Builder().keyword(k -> k.normalizer("lowercase")).build();
      // No normalizer: preserve raw byte-for-byte match on opaque IDs. docValues / index defaults
      // are true for keyword mappings so no explicit setters are needed here.
      case "keyword_case_sensitive" -> new Property.Builder().keyword(k -> k).build();
      case "text" -> new Property.Builder().text(t -> t.analyzer("standard")).build();
      case "integer" -> new Property.Builder().integer(i -> i).build();
      case "float" -> new Property.Builder().float_(i -> i).build();
      case "long" -> new Property.Builder().long_(i -> i).build();
      default -> throw new IllegalArgumentException("Unsupported index property type: " + type);
    };
  }
}
