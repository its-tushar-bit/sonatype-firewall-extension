/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global.fieldmap;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.sonatype.insight.brain.search.index.ItemType;

import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_CATEGORY_COLOR;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_CATEGORY_DESCRIPTION;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_CATEGORY_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_CATEGORY_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_PUBLIC_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_VERSION;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_COORDINATE_ARCHITECTURE;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_COORDINATE_ARTIFACT_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_COORDINATE_CLASSIFIER;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_COORDINATE_EXTENSION;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_COORDINATE_GROUP_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_COORDINATE_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_COORDINATE_PACKAGE_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_COORDINATE_PLATFORM;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_COORDINATE_QUALIFIER;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_COORDINATE_VERSION;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_EFFECTIVE_LICENSE_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_EFFECTIVE_LICENSE_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_FORMAT;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_HASH;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_LABEL_COLOR;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_LABEL_DESCRIPTION;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_LABEL_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_LABEL_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_LICENSE_THREAT_GROUP_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_LICENSE_THREAT_LEVEL;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.ITEM_TYPE;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.PARENT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.PARENT_ORGANIZATION_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_EVALUATION_STAGE;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_THREAT_CATEGORY;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_THREAT_LEVEL;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_VIOLATION_CONSTRAINT_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_VIOLATION_POLICY_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_VIOLATION_POLICY_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_VIOLATION_THREAT_CATEGORY;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_VIOLATION_WAIVER_STATUS;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.REPORT_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.SBOM_SPECIFICATION;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.VULNERABILITY_DESCRIPTION;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.VULNERABILITY_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.VULNERABILITY_SEVERITY;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.VULNERABILITY_STATUS;
import static com.sonatype.insight.brain.search.index.ItemType.APPLICATION;
import static com.sonatype.insight.brain.search.index.ItemType.LEGAL_VIOLATION;
import static com.sonatype.insight.brain.search.index.ItemType.NON_VULNERABLE_COMPONENT;
import static com.sonatype.insight.brain.search.index.ItemType.POLICY;
import static com.sonatype.insight.brain.search.index.ItemType.POLICY_VIOLATION;
import static com.sonatype.insight.brain.search.index.ItemType.SBOM_METADATA;
import static com.sonatype.insight.brain.search.index.ItemType.SECURITY_VULNERABILITY;

/**
 * Immutable case-sensitive registry of every field-name accepted by the global
 * search query language, keyed on the token the user types (e.g. {@code applicationName}).
 * Each entry declares its owning Lucene label, value shape, and the entity
 * types for which it is meaningful.
 */
public final class FieldMap
{
  private static final Set<ItemType> APP_TYPES = EnumSet.of(APPLICATION);

  private static final Set<ItemType> COMPONENT_BEARING_TYPES = EnumSet.of(
      NON_VULNERABLE_COMPONENT,
      POLICY_VIOLATION,
      LEGAL_VIOLATION);

  private static final Set<ItemType> LICENSE_TYPES = EnumSet.of(NON_VULNERABLE_COMPONENT);

  private static final Set<ItemType> POLICY_TYPES = EnumSet.of(
      POLICY,
      POLICY_VIOLATION,
      LEGAL_VIOLATION);

  private static final Set<ItemType> VIOLATION_TYPES = EnumSet.of(
      POLICY_VIOLATION,
      LEGAL_VIOLATION);

  private static final Set<ItemType> VULNERABILITY_TYPES = EnumSet.of(
      SECURITY_VULNERABILITY,
      POLICY_VIOLATION);

  private static final Set<ItemType> ALL_TYPES = EnumSet.allOf(ItemType.class);

  private static final Set<ItemType> ORG_CARRYING_TYPES = EnumSet.of(
      APPLICATION,
      NON_VULNERABLE_COMPONENT,
      POLICY_VIOLATION,
      LEGAL_VIOLATION,
      SBOM_METADATA,
      POLICY);

  private static final Set<ItemType> REPORT_CARRYING_TYPES = EnumSet.of(
      APPLICATION,
      NON_VULNERABLE_COMPONENT,
      POLICY_VIOLATION,
      LEGAL_VIOLATION);

  private static final Set<ItemType> STAGE_TYPES = EnumSet.of(
      APPLICATION,
      POLICY_VIOLATION,
      LEGAL_VIOLATION);

  private static final Set<String> CATEGORY_COLORS = Set.of(
      "light-red", "light-green", "light-blue", "light-purple",
      "dark-red", "dark-green", "dark-blue", "dark-purple",
      "orange", "yellow");

  private static final Set<String> THREAT_CATEGORIES = Set.of("security", "license", "quality", "other");

  private static final Set<String> WAIVER_STATUSES = Set.of("Active", "Expired");

  private static final Set<String> EVALUATION_STAGES = Set.of(
      "proxy", "develop", "source", "build", "stage-release", "release", "operate", "compliance");

  private static final Set<String> VULNERABILITY_STATUSES = Set.of(
      "Open", "Acknowledged", "Not Applicable", "Confirmed");

  private static final Set<String> SBOM_SPECIFICATIONS = Set.of("CycloneDX", "SPDX");

  private static final Set<String> ITEM_TYPE_VALUES = Set.of(
      "APPLICATION", "COMPONENT", "SECURITY_VULNERABILITY", "POLICY_VIOLATION", "LEGAL_VIOLATION", "POLICY");

  private final Map<String, FieldEntry> entries;

  private FieldMap(Map<String, FieldEntry> entries) {
    this.entries = Map.copyOf(entries);
  }

  public Optional<FieldEntry> lookup(String fieldName) {
    return Optional.ofNullable(entries.get(fieldName));
  }

  public boolean isKnown(String fieldName) {
    return entries.containsKey(fieldName);
  }

  public Set<String> knownFieldNames() {
    return entries.keySet();
  }

  public static FieldMap defaultMap() {
    return DEFAULT;
  }

  private static final FieldMap DEFAULT = build();

  private static FieldMap build() {
    Map<String, FieldEntry> m = new LinkedHashMap<>();

    // Discriminator
    m.put("itemType", FieldEntry.keyword(ITEM_TYPE.label, ALL_TYPES, ITEM_TYPE_VALUES));

    // Application
    m.put("applicationName", FieldEntry.keyword(APPLICATION_NAME.label, APP_TYPES));
    m.put("applicationId", FieldEntry.keyword(APPLICATION_ID.label, APP_TYPES));
    m.put("applicationPublicId", FieldEntry.keyword(APPLICATION_PUBLIC_ID.label, APP_TYPES));
    m.put("applicationVersion", FieldEntry.keyword(APPLICATION_VERSION.label, APP_TYPES));
    m.put("applicationCategoryId", FieldEntry.keyword(APPLICATION_CATEGORY_ID.label, APP_TYPES));
    m.put("applicationCategoryName", FieldEntry.keyword(APPLICATION_CATEGORY_NAME.label, APP_TYPES));
    m.put("applicationCategoryColor",
        FieldEntry.keyword(APPLICATION_CATEGORY_COLOR.label, APP_TYPES, CATEGORY_COLORS));
    m.put("applicationCategoryDescription",
        FieldEntry.text(APPLICATION_CATEGORY_DESCRIPTION.label, APP_TYPES));
    m.put("sbomSpecification",
        FieldEntry.keyword(SBOM_SPECIFICATION.label, APP_TYPES, SBOM_SPECIFICATIONS));

    // Component
    m.put("componentHash", FieldEntry.keyword(COMPONENT_HASH.label, COMPONENT_BEARING_TYPES));
    m.put("componentName", FieldEntry.keyword(COMPONENT_NAME.label, COMPONENT_BEARING_TYPES));
    m.put("componentFormat", FieldEntry.keyword(COMPONENT_FORMAT.label, COMPONENT_BEARING_TYPES));

    // Component coordinates.
    m.put("componentCoordinateName",
        FieldEntry.keyword(COMPONENT_COORDINATE_NAME.label, COMPONENT_BEARING_TYPES));
    m.put("componentCoordinateGroupId",
        FieldEntry.keyword(COMPONENT_COORDINATE_GROUP_ID.label, COMPONENT_BEARING_TYPES));
    m.put("componentCoordinateArtifactId",
        FieldEntry.keyword(COMPONENT_COORDINATE_ARTIFACT_ID.label, COMPONENT_BEARING_TYPES));
    m.put("componentCoordinateVersion",
        FieldEntry.keyword(COMPONENT_COORDINATE_VERSION.label, COMPONENT_BEARING_TYPES));
    m.put("componentCoordinateClassifier",
        FieldEntry.keyword(COMPONENT_COORDINATE_CLASSIFIER.label, COMPONENT_BEARING_TYPES));
    m.put("componentCoordinateExtension",
        FieldEntry.keyword(COMPONENT_COORDINATE_EXTENSION.label, COMPONENT_BEARING_TYPES));
    m.put("componentCoordinateQualifier",
        FieldEntry.keyword(COMPONENT_COORDINATE_QUALIFIER.label, COMPONENT_BEARING_TYPES));
    m.put("componentCoordinateArchitecture",
        FieldEntry.keyword(COMPONENT_COORDINATE_ARCHITECTURE.label, COMPONENT_BEARING_TYPES));
    m.put("componentCoordinatePlatform",
        FieldEntry.keyword(COMPONENT_COORDINATE_PLATFORM.label, COMPONENT_BEARING_TYPES));
    m.put("componentCoordinatePackageId",
        FieldEntry.keyword(COMPONENT_COORDINATE_PACKAGE_ID.label, COMPONENT_BEARING_TYPES));

    // Component labels
    m.put("componentLabelId", FieldEntry.keyword(COMPONENT_LABEL_ID.label, COMPONENT_BEARING_TYPES));
    m.put("componentLabelName",
        FieldEntry.keyword(COMPONENT_LABEL_NAME.label, COMPONENT_BEARING_TYPES));
    m.put("componentLabelColor",
        FieldEntry.keyword(COMPONENT_LABEL_COLOR.label, COMPONENT_BEARING_TYPES, CATEGORY_COLORS));
    m.put("componentLabelDescription",
        FieldEntry.text(COMPONENT_LABEL_DESCRIPTION.label, COMPONENT_BEARING_TYPES));

    // License
    m.put("componentEffectiveLicenseId",
        FieldEntry.keyword(COMPONENT_EFFECTIVE_LICENSE_ID.label, LICENSE_TYPES));
    m.put("componentEffectiveLicenseName",
        FieldEntry.keyword(COMPONENT_EFFECTIVE_LICENSE_NAME.label, LICENSE_TYPES));
    m.put("componentLicenseThreatGroupName",
        FieldEntry.keyword(COMPONENT_LICENSE_THREAT_GROUP_NAME.label, LICENSE_TYPES));
    m.put("componentLicenseThreatLevel",
        FieldEntry.numericInt(COMPONENT_LICENSE_THREAT_LEVEL.label, LICENSE_TYPES));

    // Organization. Map the user-facing organization filters onto the ancestor-carrying index
    // fields so a query on an org matches the org and every descendant, matching v1 classic search
    // (which rewrites organizationName/Id -> parentOrganizationName/Id). Both are KEYWORD-kind:
    // parentOrganizationName is analyzed by LowerCaseKeywordAnalyzer (KeywordTokenizer, no word
    // splitting), so the whole org name is a single lowercased token that a KEYWORD TermQuery
    // matches exactly — a TEXT phrase query would split "Acme Corp" and miss it. parentOrganizationId
    // is a single-token hex UUID, likewise exact-matched as KEYWORD.
    m.put("organizationId", FieldEntry.keyword(PARENT_ORGANIZATION_ID.label, ORG_CARRYING_TYPES));
    m.put("organizationName", FieldEntry.keyword(PARENT_ORGANIZATION_NAME.label, ORG_CARRYING_TYPES));

    // Policy
    m.put("policyId", FieldEntry.keyword(POLICY_ID.label, POLICY_TYPES));
    m.put("policyName", FieldEntry.keyword(POLICY_NAME.label, POLICY_TYPES));
    m.put("policyThreatCategory",
        FieldEntry.keyword(POLICY_THREAT_CATEGORY.label, POLICY_TYPES, THREAT_CATEGORIES));
    m.put("policyThreatLevel", FieldEntry.numericInt(POLICY_THREAT_LEVEL.label, POLICY_TYPES));

    // Violation
    m.put("policyViolationPolicyName",
        FieldEntry.keyword(POLICY_VIOLATION_POLICY_NAME.label, VIOLATION_TYPES));
    m.put("policyViolationConstraintName",
        FieldEntry.keyword(POLICY_VIOLATION_CONSTRAINT_NAME.label, VIOLATION_TYPES));
    m.put("policyViolationPolicyId",
        FieldEntry.keyword(POLICY_VIOLATION_POLICY_ID.label, VIOLATION_TYPES));
    m.put("policyViolationThreatCategory",
        FieldEntry.keyword(POLICY_VIOLATION_THREAT_CATEGORY.label, VIOLATION_TYPES,
            THREAT_CATEGORIES));
    m.put("policyViolationThreatLevel",
        FieldEntry.numericInt(POLICY_VIOLATION_THREAT_LEVEL.label, VIOLATION_TYPES));
    m.put("policyViolationWaiverStatus",
        FieldEntry.keyword(POLICY_VIOLATION_WAIVER_STATUS.label, VIOLATION_TYPES, WAIVER_STATUSES));

    // Vulnerability
    m.put("vulnerabilityId", FieldEntry.keyword(VULNERABILITY_ID.label, VULNERABILITY_TYPES));
    m.put("vulnerabilityDescription",
        FieldEntry.text(VULNERABILITY_DESCRIPTION.label, VULNERABILITY_TYPES));
    m.put("vulnerabilityStatus",
        FieldEntry.keyword(VULNERABILITY_STATUS.label, VULNERABILITY_TYPES, VULNERABILITY_STATUSES));
    m.put("vulnerabilitySeverity",
        FieldEntry.numericFloat(VULNERABILITY_SEVERITY.label, VULNERABILITY_TYPES));
    m.put("policyEvaluationStage",
        FieldEntry.keyword(POLICY_EVALUATION_STAGE.label, STAGE_TYPES, EVALUATION_STAGES));
    m.put("reportId", FieldEntry.keyword(REPORT_ID.label, REPORT_CARRYING_TYPES));

    return new FieldMap(m);
  }
}
