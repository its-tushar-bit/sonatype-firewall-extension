/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global.fieldmap;

import java.util.Optional;

import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.ItemType;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FieldMapTest
{
  private final FieldMap map = FieldMap.defaultMap();

  @Test
  public void lookupApplicationName() {
    Optional<FieldEntry> entry = map.lookup("applicationName");
    assertThat(entry).isPresent();
    assertThat(entry.get().label()).isEqualTo(FieldIdentifier.APPLICATION_NAME.label);
    assertThat(entry.get().kind()).isEqualTo(FieldKind.KEYWORD);
    // applicationName is indexed on violation docs too (via the owning application), so violation-
    // scoped queries must resolve it rather than compile to MatchNoDocsQuery.
    assertThat(entry.get().allowedTypes())
        .containsExactlyInAnyOrder(
            ItemType.APPLICATION, ItemType.POLICY_VIOLATION, ItemType.LEGAL_VIOLATION);
  }

  @Test
  public void lookupIsCaseSensitive() {
    assertThat(map.lookup("applicationname")).isEmpty();
    assertThat(map.lookup("APPLICATIONNAME")).isEmpty();
    assertThat(map.lookup("ApplicationName")).isEmpty();
  }

  @Test
  public void lookupUnknownField() {
    assertThat(map.lookup("bogusField")).isEmpty();
    assertThat(map.isKnown("bogusField")).isFalse();
  }

  @Test
  public void isKnownReturnsTrueForRegisteredField() {
    assertThat(map.isKnown("vulnerabilityId")).isTrue();
  }

  @Test
  public void itemTypeSpansAllEntityTypes() {
    FieldEntry entry = map.lookup("itemType").orElseThrow();
    assertThat(entry.allowedTypes()).contains(ItemType.APPLICATION,
        ItemType.NON_VULNERABLE_COMPONENT,
        ItemType.SECURITY_VULNERABILITY,
        ItemType.POLICY_VIOLATION,
        ItemType.LEGAL_VIOLATION,
        ItemType.POLICY);
  }

  @Test
  public void applicationCategoryDescriptionIsText() {
    FieldEntry entry = map.lookup("applicationCategoryDescription").orElseThrow();
    assertThat(entry.kind()).isEqualTo(FieldKind.TEXT);
  }

  @Test
  public void vulnerabilityDescriptionIsText() {
    FieldEntry entry = map.lookup("vulnerabilityDescription").orElseThrow();
    assertThat(entry.kind()).isEqualTo(FieldKind.TEXT);
  }

  @Test
  public void componentLabelDescriptionIsText() {
    FieldEntry entry = map.lookup("componentLabelDescription").orElseThrow();
    assertThat(entry.kind()).isEqualTo(FieldKind.TEXT);
  }

  @Test
  public void organizationNameTargetsParentOrgFieldForSubtreeMatching() {
    // v2 parity with v1 classic search: the user-facing organizationName filter must query the
    // ancestor-carrying parentOrganizationName index field so an org query also matches descendant
    // orgs. That field is analyzed by LowerCaseKeywordAnalyzer (single lowercased token), so the
    // entry is KEYWORD-kind for exact whole-value matching.
    FieldEntry entry = map.lookup("organizationName").orElseThrow();
    assertThat(entry.label()).isEqualTo(FieldIdentifier.PARENT_ORGANIZATION_NAME.label);
    assertThat(entry.kind()).isEqualTo(FieldKind.KEYWORD);
  }

  @Test
  public void organizationIdTargetsParentOrgFieldForSubtreeMatching() {
    // Same subtree parity as organizationName; the id value is a single-token hex UUID, so KEYWORD
    // exact matching against parentOrganizationId is sufficient.
    FieldEntry entry = map.lookup("organizationId").orElseThrow();
    assertThat(entry.label()).isEqualTo(FieldIdentifier.PARENT_ORGANIZATION_ID.label);
    assertThat(entry.kind()).isEqualTo(FieldKind.KEYWORD);
  }

  @Test
  public void policyThreatLevelIsIntegerNumeric() {
    FieldEntry entry = map.lookup("policyThreatLevel").orElseThrow();
    assertThat(entry.kind()).isEqualTo(FieldKind.NUMERIC);
    assertThat(entry.numericType()).isEqualTo(Integer.class);
  }

  @Test
  public void vulnerabilitySeverityIsFloatNumeric() {
    FieldEntry entry = map.lookup("vulnerabilitySeverity").orElseThrow();
    assertThat(entry.kind()).isEqualTo(FieldKind.NUMERIC);
    assertThat(entry.numericType()).isEqualTo(Float.class);
  }

  @Test
  public void componentLicenseThreatLevelIsIntegerNumeric() {
    FieldEntry entry = map.lookup("componentLicenseThreatLevel").orElseThrow();
    assertThat(entry.kind()).isEqualTo(FieldKind.NUMERIC);
    assertThat(entry.numericType()).isEqualTo(Integer.class);
  }

  @Test
  public void policyViolationThreatLevelIsIntegerNumeric() {
    FieldEntry entry = map.lookup("policyViolationThreatLevel").orElseThrow();
    assertThat(entry.kind()).isEqualTo(FieldKind.NUMERIC);
    assertThat(entry.numericType()).isEqualTo(Integer.class);
  }

  @Test
  public void policyThreatCategoryCarriesEnumValues() {
    FieldEntry entry = map.lookup("policyThreatCategory").orElseThrow();
    assertThat(entry.enumValues()).containsExactlyInAnyOrder("security", "license", "quality", "other");
  }

  @Test
  public void vulnerabilityStatusCarriesEnumValues() {
    FieldEntry entry = map.lookup("vulnerabilityStatus").orElseThrow();
    assertThat(entry.enumValues())
        .containsExactlyInAnyOrder("Open", "Acknowledged", "Not Applicable", "Confirmed");
  }

  @Test
  public void sbomSpecificationCarriesEnumValues() {
    FieldEntry entry = map.lookup("sbomSpecification").orElseThrow();
    assertThat(entry.enumValues()).containsExactlyInAnyOrder("CycloneDX", "SPDX");
  }

  @Test
  public void applicationScopedFieldsNotValidForVulnerabilities() {
    FieldEntry entry = map.lookup("applicationName").orElseThrow();
    assertThat(entry.allowedTypes()).doesNotContain(ItemType.SECURITY_VULNERABILITY);
  }

  @Test
  public void vulnerabilityIdNotValidForApplications() {
    FieldEntry entry = map.lookup("vulnerabilityId").orElseThrow();
    assertThat(entry.allowedTypes()).doesNotContain(ItemType.APPLICATION);
    assertThat(entry.allowedTypes()).contains(ItemType.SECURITY_VULNERABILITY, ItemType.POLICY_VIOLATION);
  }

  @Test
  public void componentFieldsValidForBothViolationsAndComponent() {
    FieldEntry entry = map.lookup("componentName").orElseThrow();
    assertThat(entry.allowedTypes()).contains(ItemType.NON_VULNERABLE_COMPONENT,
        ItemType.POLICY_VIOLATION,
        ItemType.LEGAL_VIOLATION);
  }

  @Test
  public void policyFieldsValidForPolicyAndViolations() {
    FieldEntry entry = map.lookup("policyName").orElseThrow();
    assertThat(entry.allowedTypes()).contains(ItemType.POLICY, ItemType.POLICY_VIOLATION, ItemType.LEGAL_VIOLATION);
  }

  @Test
  public void licenseFieldsScopedToComponent() {
    FieldEntry entry = map.lookup("componentEffectiveLicenseId").orElseThrow();
    assertThat(entry.allowedTypes()).containsExactly(ItemType.NON_VULNERABLE_COMPONENT);
  }

  @Test
  public void coordinateFieldsUseFieldIdentifierLabels() {
    // Coordinate labels come from the FieldIdentifier enum, matching the coordinate naming
    // convention used by DocumentBuilder.getFieldNameForCoordinate.
    assertThat(map.lookup("componentCoordinateArtifactId").orElseThrow().label())
        .isEqualTo(FieldIdentifier.COMPONENT_COORDINATE_ARTIFACT_ID.label);
    assertThat(map.lookup("componentCoordinateGroupId").orElseThrow().label())
        .isEqualTo(FieldIdentifier.COMPONENT_COORDINATE_GROUP_ID.label);
    assertThat(map.lookup("componentCoordinatePackageId").orElseThrow().label())
        .isEqualTo(FieldIdentifier.COMPONENT_COORDINATE_PACKAGE_ID.label);
  }

  @Test
  public void knownFieldNamesIncludesEveryDocumentedFilter() {
    assertThat(map.knownFieldNames()).contains(
        "itemType",
        "applicationName", "applicationId", "applicationPublicId", "applicationVersion",
        "applicationCategoryId", "applicationCategoryName", "applicationCategoryColor",
        "applicationCategoryDescription", "sbomSpecification",
        "componentHash", "componentName", "componentFormat",
        "componentCoordinateName", "componentCoordinateGroupId", "componentCoordinateArtifactId",
        "componentCoordinateVersion", "componentCoordinateClassifier", "componentCoordinateExtension",
        "componentCoordinateQualifier", "componentCoordinateArchitecture", "componentCoordinatePlatform",
        "componentCoordinatePackageId",
        "componentLabelId", "componentLabelName", "componentLabelColor", "componentLabelDescription",
        "componentEffectiveLicenseId", "componentEffectiveLicenseName",
        "componentLicenseThreatGroupName", "componentLicenseThreatLevel",
        "organizationId", "organizationName",
        "policyId", "policyName", "policyThreatCategory", "policyThreatLevel",
        "policyViolationPolicyName", "policyViolationConstraintName", "policyViolationPolicyId",
        "policyViolationThreatCategory", "policyViolationThreatLevel", "policyViolationWaiverStatus",
        "vulnerabilityId", "vulnerabilityDescription", "vulnerabilityStatus", "vulnerabilitySeverity",
        "policyEvaluationStage", "reportId");
  }

  @Test
  public void numericFieldFactoryRejectsUnsupportedType() {
    assertThat(new FieldEntry("x", FieldKind.NUMERIC, java.util.Set.of(), null, Integer.class).numericType())
        .isEqualTo(Integer.class);
    org.assertj.core.api.Assertions
        .assertThatThrownBy(() -> new FieldEntry("x", FieldKind.NUMERIC, java.util.Set.of(), null, Long.class))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Integer.class or Float.class");
  }
}
