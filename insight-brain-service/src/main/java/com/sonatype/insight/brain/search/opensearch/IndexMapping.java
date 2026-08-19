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

    // Case-sensitive because faceted on the raw id (opaque UUID, sentinel, or hash)
    propertyMappings.put(FieldIdentifier.ORGANIZATION_ID.label, createProperty("keyword_case_sensitive"));
    propertyMappings.put(FieldIdentifier.ORGANIZATION_NAME.label, createProperty("keyword"));

    // Case-sensitive because faceted on the raw id (opaque UUID)
    propertyMappings.put(FieldIdentifier.APPLICATION_ID.label, createProperty("keyword_case_sensitive"));
    propertyMappings.put(FieldIdentifier.APPLICATION_NAME.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.APPLICATION_PUBLIC_ID.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.APPLICATION_VERSION.label, createProperty("keyword"));

    propertyMappings.put(FieldIdentifier.POLICY_EVALUATION_STAGE.label, createProperty("keyword"));

    propertyMappings.put(FieldIdentifier.REPORT_ID.label, createProperty("keyword"));

    // Case-sensitive because faceted on the raw id (opaque hash)
    propertyMappings.put(FieldIdentifier.COMPONENT_HASH.label, createProperty("keyword_case_sensitive"));
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

    // Case-sensitive because faceted on the raw id (opaque UUID)
    propertyMappings.put(FieldIdentifier.APPLICATION_CATEGORY_ID.label, createProperty("keyword_case_sensitive"));
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

    // Case-sensitive because faceted on the raw id (opaque UUID, hierarchical closure)
    propertyMappings.put(FieldIdentifier.PARENT_ORGANIZATION_ID.label, createProperty("keyword_case_sensitive"));
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

    // These explicit policyWaiver* mappings only apply to a freshly created index. On upgrade,
    // incremental waiver enqueue writes docs into the pre-existing index (which lacks these fields)
    // before any full reindex, so OpenSearch would dynamic-map policyWaiver* fields (text vs keyword
    // for policyWaiverAuto; the epoch-millis long could be mis-typed). date_detection is off at index
    // creation (see OpenSearchSearchIndexClient.createIndex), so date-ish keyword fields are never
    // auto-typed as date, but the keyword-vs-text drift on a pre-existing index remains until reindex.
    // Correct auto/expiry filtering therefore requires these explicit mappings, which on an upgraded
    // deployment only a full reindex installs (populateIndex creates a brand-new index and swaps the
    // alias). That reindex is admin-triggered (IndexService.createIndexAsync, CONFIGURE_SYSTEM) and
    // nothing forces or prompts it on upgrade. The WAIVER read path follows PREVIEW_NEXUS_ONE_UI, which
    // also gates unrelated Nexus One surfaces, so enabling it for those exposes WAIVER search on a
    // not-yet-reindexed index. A full reindex is required after upgrade before WAIVER auto/expiry/status
    // filtering can be relied on.
    propertyMappings.put(FieldIdentifier.POLICY_WAIVER_ID.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.POLICY_WAIVER_POLICY_NAME.label, createProperty("keyword"));
    // Case-sensitive because faceted on the raw id (opaque UUID)
    propertyMappings.put(FieldIdentifier.POLICY_WAIVER_POLICY_ID.label, createProperty("keyword_case_sensitive"));
    propertyMappings.put(FieldIdentifier.POLICY_WAIVER_REASON.label, createProperty("text"));
    propertyMappings.put(FieldIdentifier.POLICY_WAIVER_COMMENT.label, createProperty("text"));
    // ISO-8601 stored as a single keyword token (DocumentBuilder intent); must not date-detect.
    propertyMappings.put(FieldIdentifier.POLICY_WAIVER_CREATED_AT.label, createProperty("keyword"));
    // Sortable epoch-millis twin of POLICY_WAIVER_CREATED_AT backing the WAIVER default created-desc
    // sort; a numeric long, not date-detected. Mirrors POLICY_WAIVER_EXPIRES_AT_EPOCH_MS.
    propertyMappings.put(FieldIdentifier.POLICY_WAIVER_CREATED_AT_EPOCH_MS.label, createProperty("long"));
    propertyMappings.put(FieldIdentifier.POLICY_WAIVER_EXPIRES_AT.label, createProperty("keyword"));
    // Range-queryable epoch-millis twin of POLICY_WAIVER_EXPIRES_AT backing the active-vs-expired
    // filter; must be a numeric long, not date-detected. Mirrors the explicit CREATED_AT_EPOCH_MS long.
    propertyMappings.put(FieldIdentifier.POLICY_WAIVER_EXPIRES_AT_EPOCH_MS.label, createProperty("long"));
    propertyMappings.put(FieldIdentifier.POLICY_WAIVER_SCOPE_OWNER_ID.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.POLICY_WAIVER_SCOPE_OWNER_TYPE.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.POLICY_WAIVER_THREAT_LEVEL.label, createProperty("integer"));
    propertyMappings.put(FieldIdentifier.POLICY_WAIVER_WAIVED_BY.label, createProperty("keyword"));
    // Auto-vs-manual discriminator stored as the keyword string "true"/"false".
    propertyMappings.put(FieldIdentifier.POLICY_WAIVER_AUTO.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.POLICY_WAIVER_IS_AUTO.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.POLICY_WAIVER_EXPIRY_STATUS.label, createProperty("keyword"));
    // Denormalized policy threat category on POLICY_WAIVER + POLICY_WAIVER_REQUEST docs (keyword so the
    // policyType facet/filter matches whole). Same fresh-index-only caveat as the other waiver fields.
    propertyMappings.put(FieldIdentifier.POLICY_WAIVER_POLICY_TYPE.label, createProperty("keyword"));
    // Scope granularity (application/organization/component) keyword; backs the scope facet/filter.
    propertyMappings.put(FieldIdentifier.POLICY_WAIVER_SCOPE.label, createProperty("keyword"));

    // Policy waiver REQUEST fields (ItemType.POLICY_WAIVER_REQUEST). Same fresh-index-only upgrade
    // caveat as the policyWaiver* fields above: correct request-status/policyType filtering requires
    // the explicit mappings, which only a full reindex installs on an upgraded deployment.
    propertyMappings.put(FieldIdentifier.POLICY_WAIVER_REQUEST_STATUS.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.REQUESTER_NAME.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.REVIEWER_NAME.label, createProperty("keyword"));
    // ISO-8601 stored as a single keyword token (display only); must not date-detect.
    propertyMappings.put(FieldIdentifier.REVIEW_TIME.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.REJECTION_REASON.label, createProperty("text"));
    propertyMappings.put(FieldIdentifier.NOTE_TO_REVIEWER.label, createProperty("text"));

    // Application evaluation denormalization. These explicit mappings only apply to a freshly
    // created index; on upgrade an incremental app change writes into the pre-existing index (which
    // lacks these fields) before any full reindex, so OpenSearch dynamic-maps them (long vs
    // date-detect, keyword vs text). Self-heals on the next full reindex, which every new field
    // requires to populate on existing indices.
    propertyMappings.put(FieldIdentifier.APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS.label, createProperty("long"));
    // Epoch-millis of the vulnerability's first IQ detection (earliest triggering violation open time)
    // on SECURITY_VULNERABILITY docs, backing the local "first seen (within ...)" window range filter
    // and display; a numeric long, not date-detected. Absent for non-triggering vulns.
    propertyMappings.put(FieldIdentifier.VULNERABILITY_FIRST_SEEN_EPOCH_MS.label, createProperty("long"));
    // Multi-valued "stage:severity:count" tokens; keyword so each entry is matched/faceted whole.
    propertyMappings.put(FieldIdentifier.APPLICATION_STAGE_SEVERITY_COUNT.label, createProperty("keyword"));
    // Denormalized violation aggregates for the Applications filter/sort rail. Integer max threat level
    // (range filter + desc sort) and the worst state-sort ordinal (asc sort); multi-valued keyword sets
    // of stages / policy types / states (TERMS filters). Same reindex-to-populate caveat as above.
    propertyMappings.put(FieldIdentifier.APPLICATION_MAX_POLICY_THREAT_LEVEL.label, createProperty("integer"));
    propertyMappings.put(FieldIdentifier.APPLICATION_VIOLATION_STAGE.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.APPLICATION_VIOLATION_POLICY_TYPE.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.APPLICATION_VIOLATION_STATE.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.APPLICATION_VIOLATION_STATE_SORT_ORDINAL.label, createProperty("integer"));

    // Denormalized permission-filter field. Case-sensitive keyword (no normalizer) so opaque
    // context IDs are matched byte-for-byte. Multi-valued (set per document by DocumentBuilder).
    propertyMappings.put(FieldIdentifier.ALLOWED_CONTEXT_IDS.label, createProperty("keyword_case_sensitive"));

    // Component violation denormalization on NON_VULNERABLE_COMPONENT docs (Components leg
    // policyTypes/violationStates/policyThreatLevel filters + sort). Explicit mappings apply to a
    // freshly created index; a full reindex populates them on existing indices. Multi-valued keyword
    // for the type/state sets; integer for the range/sort max-threat (float sort of CVSS uses the
    // native float VULNERABILITY_SEVERITY mapping, so no separate twin is needed on OpenSearch).
    propertyMappings.put(FieldIdentifier.COMPONENT_VIOLATION_POLICY_TYPE.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.COMPONENT_VIOLATION_STATE.label, createProperty("keyword"));
    propertyMappings.put(FieldIdentifier.COMPONENT_MAX_POLICY_THREAT_LEVEL.label, createProperty("integer"));

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
