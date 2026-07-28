/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.ItemType;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexableField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_CATEGORY_COLOR;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_CATEGORY_DESCRIPTION;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_CATEGORY_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_CATEGORY_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_PUBLIC_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_STAGE_SEVERITY_COUNT;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_VERSION;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_FORMAT;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_HASH;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_LABEL_COLOR;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_LABEL_DESCRIPTION;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_LABEL_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_LABEL_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.ITEM_TYPE;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.ORGANIZATION_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.ORGANIZATION_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.PARENT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.PARENT_ORGANIZATION_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_EVALUATION_STAGE;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_THREAT_CATEGORY;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_THREAT_LEVEL;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.REPORT_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.SBOM_SPECIFICATION;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.VULNERABILITY_DESCRIPTION;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.VULNERABILITY_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.VULNERABILITY_SEVERITY;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.VULNERABILITY_STATUS;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_VIOLATION_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_VIOLATION_THREAT_CATEGORY;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_VIOLATION_POLICY_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_VIOLATION_POLICY_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_VIOLATION_WAIVER_STATUS;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_VIOLATION_CONSTRAINT_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.ALLOWED_CONTEXT_IDS;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_EFFECTIVE_LICENSE_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_EFFECTIVE_LICENSE_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_LICENSE_THREAT_GROUP_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_LICENSE_THREAT_LEVEL;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.DOCUMENT_KEY;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_WAIVER_AUTO;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_WAIVER_COMMENT;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_WAIVER_CREATED_AT;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_WAIVER_CREATED_AT_EPOCH_MS;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_WAIVER_EXPIRES_AT;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_WAIVER_EXPIRES_AT_EPOCH_MS;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_WAIVER_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_WAIVER_POLICY_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_WAIVER_POLICY_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_WAIVER_REASON;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_WAIVER_SCOPE_OWNER_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_WAIVER_SCOPE_OWNER_TYPE;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_WAIVER_THREAT_LEVEL;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_WAIVER_WAIVED_BY;

public class DocumentBuilder
{
  private static final Logger log = LoggerFactory.getLogger(DocumentBuilder.class);

  private Document document;

  private final ItemType itemType;

  private Optional<Field> organizationId = Optional.empty();

  private Optional<Field> organizationName = Optional.empty();

  private Optional<Field[]> parentOrganizationNames = Optional.empty();

  private Optional<Field[]> parentOrganizationIds = Optional.empty();

  private Optional<Field> applicationId = Optional.empty();

  private Optional<Field> applicationPublicId = Optional.empty();

  private Optional<Field> applicationName = Optional.empty();

  private Optional<Field> policyEvaluationStage = Optional.empty();

  private Optional<Field> reportId = Optional.empty();

  private Optional<Field> componentHash = Optional.empty();

  private Optional<Field> componentFormat = Optional.empty();

  private Optional<Field[]> componentCoordinates = Optional.empty();

  private Optional<Field> componentName = Optional.empty();

  private Optional<Field> vulnerabilityId = Optional.empty();

  private Optional<Field> vulnerabilityStatus = Optional.empty();

  private Optional<Field[]> vulnerabilitySeverity = Optional.empty();

  private Optional<Field> vulnerabilityDescription = Optional.empty();

  private Optional<Field> applicationCategoryId = Optional.empty();

  private Optional<Field> applicationCategoryName = Optional.empty();

  private Optional<Field> applicationCategoryColor = Optional.empty();

  private Optional<Field> applicationCategoryDescription = Optional.empty();

  private Optional<Field> componentLabelId = Optional.empty();

  private Optional<Field> componentLabelName = Optional.empty();

  private Optional<Field> componentLabelColor = Optional.empty();

  private Optional<Field> componentLabelDescription = Optional.empty();

  private Optional<Field> policyId = Optional.empty();

  private Optional<Field> policyName = Optional.empty();

  private Optional<Field> policyThreatCategory = Optional.empty();

  private Optional<Field[]> policyThreatLevel = Optional.empty();

  private Optional<Field> applicationVersion = Optional.empty();

  private Optional<Field> sbomSpecification = Optional.empty();

  private Optional<Field> policyViolationId = Optional.empty();

  private Optional<Field> policyViolationThreatCategory = Optional.empty();

  private Optional<Field[]> policyViolationThreatLevel = Optional.empty();

  private Optional<Field> policyViolationPolicyName = Optional.empty();

  private Optional<Field> policyViolationPolicyId = Optional.empty();

  private Optional<Field> policyViolationWaiverStatus = Optional.empty();

  private Optional<Field> policyViolationConstraintName = Optional.empty();

  private Optional<Field> componentEffectiveLicenseId = Optional.empty();

  private Optional<Field> componentEffectiveLicenseName = Optional.empty();

  private Optional<Field> componentLicenseThreatGroupName = Optional.empty();

  private Optional<Field[]> componentLicenseThreatLevel = Optional.empty();

  private Optional<Field[]> allowedContextIds = Optional.empty();

  private Optional<Field> policyWaiverId = Optional.empty();

  private Optional<Field> policyWaiverPolicyName = Optional.empty();

  private Optional<Field> policyWaiverPolicyId = Optional.empty();

  private Optional<Field> policyWaiverReason = Optional.empty();

  private Optional<Field> policyWaiverComment = Optional.empty();

  private Optional<Field[]> policyWaiverCreatedAt = Optional.empty();

  private Optional<Field[]> policyWaiverExpiresAt = Optional.empty();

  private Optional<Field[]> policyWaiverCreatedAtEpochMs = Optional.empty();

  private Optional<Field[]> policyWaiverExpiresAtEpochMs = Optional.empty();

  private Optional<Field> policyWaiverScopeOwnerId = Optional.empty();

  private Optional<Field> policyWaiverScopeOwnerType = Optional.empty();

  private Optional<Field[]> policyWaiverThreatLevel = Optional.empty();

  private Optional<Field> policyWaiverWaivedBy = Optional.empty();

  private Optional<Field> policyWaiverAuto = Optional.empty();

  private Optional<Field[]> applicationCategoryNames = Optional.empty();

  private Optional<Field[]> applicationLastEvaluationTimeEpochMs = Optional.empty();

  private Optional<Field[]> applicationStageSeverityCounts = Optional.empty();

  public DocumentBuilder(ItemType itemType) {
    this.itemType = itemType;
    document = new Document();
    document.add(new TextField(ITEM_TYPE.label, itemType.name(), Store.YES));
  }

  public DocumentBuilder setOwner(Owner owner) {
    if (owner.getType() == OwnerType.ORGANIZATION) {
      setOrganizationId(owner.getId());
      setOrganizationName(owner.getName());
      setParentOrganizationIds(Collections.singletonList((Organization) owner));
      setParentOrganizationNames(Collections.singletonList((Organization) owner));
    }
    else if (owner.getType() == OwnerType.APPLICATION) {
      setApplicationId(owner.getId());
      setApplicationPublicId(owner.getPublicId());
      setApplicationName(owner.getName());
    }
    return this;
  }

  public DocumentBuilder setOrganizationId(final String organizationId) {
    this.organizationId = Optional.of(new TextField(ORGANIZATION_ID.label, organizationId, Store.YES));
    return this;
  }

  public DocumentBuilder setOrganizationName(final String organizationName) {
    this.organizationName = Optional.of(new TextField(ORGANIZATION_NAME.label, organizationName, Store.YES));
    return this;
  }

  public DocumentBuilder setParentOrganizationNames(Collection<Organization> parentOrganizations) {
    this.parentOrganizationNames = Optional.of(parentOrganizations.stream()
        .map(
            parentOrg -> new TextField(PARENT_ORGANIZATION_NAME.label, parentOrg.getName(), Store.YES))
        .toArray(Field[]::new));
    return this;
  }

  public DocumentBuilder setParentOrganizationIds(Collection<Organization> parentOrganizations) {
    this.parentOrganizationIds = Optional.of(parentOrganizations.stream()
        .map(
            parentOrg -> new TextField(PARENT_ORGANIZATION_ID.label, parentOrg.getId(), Store.YES))
        .toArray(Field[]::new));
    return this;
  }

  public DocumentBuilder setApplicationId(final String applicationId) {
    // A null id (e.g. an app not yet assigned one on the incremental path) omits the field rather
    // than tripping Lucene's non-null TextField contract; the rollup lookups keyed by id are also
    // skipped upstream in that case (see DocumentBuilderHelper#buildDocument).
    this.applicationId = applicationId == null
        ? Optional.empty()
        : Optional.of(new TextField(APPLICATION_ID.label, applicationId, Store.YES));
    return this;
  }

  public DocumentBuilder setApplicationPublicId(final String applicationPublicId) {
    this.applicationPublicId = Optional.of(new TextField(APPLICATION_PUBLIC_ID.label, applicationPublicId, Store.YES));
    return this;
  }

  public DocumentBuilder setApplicationName(final String applicationName) {
    this.applicationName = Optional.of(new TextField(APPLICATION_NAME.label, applicationName, Store.YES));
    return this;
  }

  public DocumentBuilder setPolicyEvaluationStage(final StageType stageType) {
    this.policyEvaluationStage =
        Optional.of(new TextField(POLICY_EVALUATION_STAGE.label, stageType.getId(), Store.YES));
    return this;
  }

  public DocumentBuilder setReportId(final String reportId) {
    this.reportId = Optional.of(new TextField(REPORT_ID.label, reportId, Store.YES));
    return this;
  }

  public DocumentBuilder setComponentHash(final String hash) {
    this.componentHash = Optional.of(new TextField(COMPONENT_HASH.label, hash, Store.YES));
    return this;
  }

  public DocumentBuilder setComponentFormat(final String format) {
    this.componentFormat = Optional.of(new TextField(COMPONENT_FORMAT.label, format, Store.YES));
    return this;
  }

  public DocumentBuilder setComponentCoordinates(final Component component) {
    this.componentCoordinates = Optional.of(component.getComponentIdentifier()
        .getCoordinates()
        .entrySet()
        .stream()
        .map(
            coordinate -> new TextField(getFieldNameForCoordinate(coordinate.getKey()), coordinate.getValue(),
                Store.YES))
        .toArray(Field[]::new));
    return this;
  }

  public DocumentBuilder setComponentCoordinates(final ComponentIdentifier componentIdentifier) {
    this.componentCoordinates = Optional.of(componentIdentifier.getCoordinates()
        .entrySet()
        .stream()
        .map(
            coordinate -> new TextField(getFieldNameForCoordinate(coordinate.getKey()), coordinate.getValue(),
                Store.YES))
        .toArray(Field[]::new));
    return this;
  }

  public static String getFieldNameForCoordinate(String coordinateName) {
    return FieldIdentifier.COMPONENT_COORDINATE.label + Character.toUpperCase(coordinateName.charAt(0))
        + coordinateName.substring(1);
  }

  public DocumentBuilder setComponentName(final String componentDisplayName) {
    this.componentName = Optional.of(new TextField(COMPONENT_NAME.label, componentDisplayName, Store.YES));
    return this;
  }

  public DocumentBuilder setVulnerabilitySeverity(final Float vulnerabilitySeverity) {
    if (vulnerabilitySeverity != null) {
      this.vulnerabilitySeverity =
          Optional.of(new Field[]{new FloatPoint(VULNERABILITY_SEVERITY.label, vulnerabilitySeverity),
            new StoredField(VULNERABILITY_SEVERITY.label, vulnerabilitySeverity)});
    }
    return this;
  }

  public DocumentBuilder setVulnerabilityId(final String refId) {
    this.vulnerabilityId = Optional.of(new TextField(VULNERABILITY_ID.label, refId, Store.YES));
    return this;
  }

  public DocumentBuilder setVulnerabilityStatus(final String status) {
    this.vulnerabilityStatus = Optional.of(new TextField(VULNERABILITY_STATUS.label, status, Store.YES));
    return this;
  }

  public DocumentBuilder setVulnerabilityDescription(final String description) {
    if (description != null) {
      this.vulnerabilityDescription =
          Optional.of(new TextField(VULNERABILITY_DESCRIPTION.label, description, Store.YES));
    }
    return this;
  }

  public DocumentBuilder setApplicationCategoryId(final String tagId) {
    this.applicationCategoryId = Optional.of(new TextField(APPLICATION_CATEGORY_ID.label, tagId, Store.YES));
    return this;
  }

  public DocumentBuilder setApplicationCategoryName(final String tagName) {
    this.applicationCategoryName = Optional.of(new TextField(APPLICATION_CATEGORY_NAME.label, tagName, Store.YES));
    return this;
  }

  public DocumentBuilder setApplicationCategoryColor(final Color tagColor) {
    this.applicationCategoryColor =
        Optional.of(new TextField(APPLICATION_CATEGORY_COLOR.label, toFieldValue(tagColor), Store.YES));
    return this;
  }

  private static String toFieldValue(Color color) {
    return color.toValue();
  }

  public DocumentBuilder setApplicationCategoryDescription(final String tagDescription) {
    this.applicationCategoryDescription =
        Optional.of(new TextField(APPLICATION_CATEGORY_DESCRIPTION.label, tagDescription, Store.YES));
    return this;
  }

  public DocumentBuilder setComponentLabelId(final String labelId) {
    this.componentLabelId = Optional.of(new TextField(COMPONENT_LABEL_ID.label, labelId, Store.YES));
    return this;
  }

  public DocumentBuilder setComponentLabelName(final String labelName) {
    this.componentLabelName = Optional.of(new TextField(COMPONENT_LABEL_NAME.label, labelName, Store.YES));
    return this;
  }

  public DocumentBuilder setComponentLabelColor(final Color labelColor) {
    this.componentLabelColor =
        Optional.of(new TextField(COMPONENT_LABEL_COLOR.label, toFieldValue(labelColor), Store.YES));
    return this;
  }

  public DocumentBuilder setComponentLabelDescription(final String labelDescription) {
    this.componentLabelDescription = Optional.of(new TextField(COMPONENT_LABEL_DESCRIPTION.label,
        Optional.ofNullable(labelDescription).orElse(""), Store.YES));
    return this;
  }

  public DocumentBuilder setPolicyId(final String policyId) {
    this.policyId = Optional.of(new TextField(POLICY_ID.label, policyId, Store.YES));
    return this;
  }

  public DocumentBuilder setPolicyName(final String policyName) {
    this.policyName = Optional.of(new TextField(POLICY_NAME.label, policyName, Store.YES));
    return this;
  }

  public DocumentBuilder setPolicyThreatCategory(final PolicyThreatCategory policyThreatCategory) {
    this.policyThreatCategory =
        Optional.of(new TextField(POLICY_THREAT_CATEGORY.label, policyThreatCategory.getName(), Store.YES));
    return this;
  }

  public DocumentBuilder setPolicyThreatLevel(final int policyThreatLevel) {
    this.policyThreatLevel = Optional.of(new Field[]{new IntPoint(POLICY_THREAT_LEVEL.label, policyThreatLevel),
      new StoredField(POLICY_THREAT_LEVEL.label, policyThreatLevel)});
    return this;
  }

  public DocumentBuilder setApplicationVersion(final String version) {
    this.applicationVersion = Optional.of(new TextField(APPLICATION_VERSION.label, version, Store.YES));
    return this;
  }

  public DocumentBuilder setSbomSpecification(final String sbomSpecification) {
    this.sbomSpecification = Optional.of(new TextField(SBOM_SPECIFICATION.label, sbomSpecification, Store.YES));
    return this;
  }

  public DocumentBuilder setPolicyViolationId(final String policyViolationId) {
    this.policyViolationId = Optional.of(new TextField(POLICY_VIOLATION_ID.label, policyViolationId, Store.YES));
    return this;
  }

  public DocumentBuilder setPolicyViolationThreatCategory(final PolicyThreatCategory threatCategory) {
    this.policyViolationThreatCategory =
        Optional.of(new TextField(POLICY_VIOLATION_THREAT_CATEGORY.label, threatCategory.getName(), Store.YES));
    return this;
  }

  public DocumentBuilder setPolicyViolationThreatLevel(final int threatLevel) {
    this.policyViolationThreatLevel = Optional.of(new Field[]{
      new IntPoint(POLICY_VIOLATION_THREAT_LEVEL.label, threatLevel),
      new StoredField(POLICY_VIOLATION_THREAT_LEVEL.label, threatLevel)});
    return this;
  }

  public DocumentBuilder setPolicyViolationPolicyName(final String policyName) {
    this.policyViolationPolicyName =
        Optional.of(new TextField(POLICY_VIOLATION_POLICY_NAME.label, policyName, Store.YES));
    return this;
  }

  public DocumentBuilder setPolicyViolationPolicyId(final String policyId) {
    this.policyViolationPolicyId = Optional.of(new TextField(POLICY_VIOLATION_POLICY_ID.label, policyId, Store.YES));
    return this;
  }

  public DocumentBuilder setPolicyViolationWaiverStatus(final String waiverStatus) {
    this.policyViolationWaiverStatus =
        Optional.of(new TextField(POLICY_VIOLATION_WAIVER_STATUS.label, waiverStatus, Store.YES));
    return this;
  }

  public DocumentBuilder setPolicyViolationConstraintName(final String constraintName) {
    if (constraintName != null) {
      this.policyViolationConstraintName =
          Optional.of(new TextField(POLICY_VIOLATION_CONSTRAINT_NAME.label, constraintName, Store.YES));
    }
    return this;
  }

  public DocumentBuilder setComponentEffectiveLicenseId(final String licenseId) {
    this.componentEffectiveLicenseId =
        Optional.of(new TextField(COMPONENT_EFFECTIVE_LICENSE_ID.label, licenseId, Store.YES));
    return this;
  }

  public DocumentBuilder setComponentEffectiveLicenseName(final String licenseName) {
    if (licenseName != null) {
      this.componentEffectiveLicenseName =
          Optional.of(new TextField(COMPONENT_EFFECTIVE_LICENSE_NAME.label, licenseName, Store.YES));
    }
    return this;
  }

  public DocumentBuilder setComponentLicenseThreatGroupName(final String threatGroupName) {
    if (threatGroupName != null) {
      this.componentLicenseThreatGroupName =
          Optional.of(new TextField(COMPONENT_LICENSE_THREAT_GROUP_NAME.label, threatGroupName, Store.YES));
    }
    return this;
  }

  public DocumentBuilder setComponentLicenseThreatLevel(final Integer threatLevel) {
    if (threatLevel != null) {
      this.componentLicenseThreatLevel = Optional.of(new Field[]{
        new IntPoint(COMPONENT_LICENSE_THREAT_LEVEL.label, threatLevel),
        new StoredField(COMPONENT_LICENSE_THREAT_LEVEL.label, threatLevel)});
    }
    return this;
  }

  /**
   * Sets the {@code allowedContextIds} permission-filter field: one {@link StringField} per id. A
   * null/empty closure is left unwritten (and warned) — such a doc is then invisible to
   * permission-filtered queries, so indexable docs must always supply a non-empty closure.
   */
  public DocumentBuilder setAllowedContextIds(final Collection<String> contextIds) {
    if (contextIds == null || contextIds.isEmpty()) {
      // Fail-closed: an empty closure leaves the field unwritten, so the doc is invisible to every
      // permission-filtered query. A WARN alone makes systemic drop-out (e.g. an org purge) hard to
      // spot at scale.
      // TODO(CLM-41642): before the consuming endpoints ship, emit an indexing metric/counter for
      // "docs indexed with empty allowedContextIds closure" so operators can detect drop-out.
      log.warn("Refusing to write allowedContextIds for {} document: closure is null or empty; "
          + "doc will be invisible to permission-filtered queries.", itemType);
      return this;
    }
    Field[] fields = contextIds.stream()
        .filter(id -> id != null && !id.isEmpty())
        .distinct()
        .map(id -> (Field) new StringField(ALLOWED_CONTEXT_IDS.label, id, Store.NO))
        .toArray(Field[]::new);
    if (fields.length == 0) {
      log.warn("Refusing to write allowedContextIds for {} document: closure contained only null/empty "
          + "entries; doc will be invisible to permission-filtered queries.", itemType);
      return this;
    }
    this.allowedContextIds = Optional.of(fields);
    return this;
  }

  // ---- Policy waiver setters ---------------------------------------------------------------

  public DocumentBuilder setPolicyWaiverId(final String waiverId) {
    Objects.requireNonNull(waiverId, "waiverId");
    this.policyWaiverId = Optional.of(new TextField(POLICY_WAIVER_ID.label, waiverId, Store.YES));
    return this;
  }

  public DocumentBuilder setPolicyWaiverPolicyName(final String policyName) {
    if (policyName != null) {
      this.policyWaiverPolicyName =
          Optional.of(new TextField(POLICY_WAIVER_POLICY_NAME.label, policyName, Store.YES));
    }
    return this;
  }

  public DocumentBuilder setPolicyWaiverPolicyId(final String policyId) {
    if (policyId != null) {
      this.policyWaiverPolicyId = Optional.of(new TextField(POLICY_WAIVER_POLICY_ID.label, policyId, Store.YES));
    }
    return this;
  }

  public DocumentBuilder setPolicyWaiverReason(final String reasonText) {
    if (reasonText != null) {
      this.policyWaiverReason = Optional.of(new TextField(POLICY_WAIVER_REASON.label, reasonText, Store.YES));
    }
    return this;
  }

  public DocumentBuilder setPolicyWaiverComment(final String comment) {
    if (comment != null) {
      this.policyWaiverComment = Optional.of(new TextField(POLICY_WAIVER_COMMENT.label, comment, Store.YES));
    }
    return this;
  }

  /**
   * Stored as an ISO-8601 string to give a stable sortable representation without committing to a
   * backend-specific {@code date} mapping; both backends sort lexicographically on this format.
   * {@link StringField} (not analyzed) keeps the value as a single keyword token so range queries
   * match the lexicographic order of the ISO-8601 form. Sort doc-values are added only in
   * {@link LuceneIndexingContext#addDocuments}, not here, so the field does not serialize a null
   * into the OpenSearch _source.
   */
  public DocumentBuilder setPolicyWaiverCreatedAt(final String iso8601) {
    if (iso8601 != null) {
      this.policyWaiverCreatedAt = Optional.of(toIso8601DateFields(POLICY_WAIVER_CREATED_AT.label, iso8601));
    }
    return this;
  }

  public DocumentBuilder setPolicyWaiverExpiresAt(final String iso8601) {
    if (iso8601 != null) {
      this.policyWaiverExpiresAt = Optional.of(toIso8601DateFields(POLICY_WAIVER_EXPIRES_AT.label, iso8601));
    }
    return this;
  }

  /**
   * Sortable epoch-millis twin of {@link #setPolicyWaiverCreatedAt} backing the WAIVER default
   * created-desc sort. A {@link LongPoint} (indexed) plus a {@link StoredField} (so
   * {@link LuceneIndexingContext#addDocuments} can read the numeric value and add the sort
   * doc-values twin); OpenSearch sorts on its {@code long} mapping. Null (missing create time)
   * writes nothing, so such a doc sorts last under created-desc (missing-value default).
   */
  public DocumentBuilder setPolicyWaiverCreatedAtEpochMs(final Long epochMs) {
    if (epochMs != null) {
      this.policyWaiverCreatedAtEpochMs = Optional.of(new Field[]{
        new LongPoint(POLICY_WAIVER_CREATED_AT_EPOCH_MS.label, epochMs),
        new StoredField(POLICY_WAIVER_CREATED_AT_EPOCH_MS.label, epochMs)});
    }
    return this;
  }

  /**
   * Range-queryable epoch-millis twin of {@link #setPolicyWaiverExpiresAt} for the active-vs-expired
   * filter. A {@link LongPoint} (indexed, not stored) supports {@code [now TO *]} range queries; the
   * stored ISO-8601 keyword stays the display/sort form. A null expiry writes nothing, so a
   * never-expiring waiver has no epoch point and is treated as active by the compiled range clause.
   */
  public DocumentBuilder setPolicyWaiverExpiresAtEpochMs(final Long epochMs) {
    if (epochMs != null) {
      this.policyWaiverExpiresAtEpochMs =
          Optional.of(new Field[]{new LongPoint(POLICY_WAIVER_EXPIRES_AT_EPOCH_MS.label, epochMs)});
    }
    return this;
  }

  private static Field[] toIso8601DateFields(final String fieldLabel, final String iso8601) {
    return new Field[]{
      new StringField(fieldLabel, iso8601, Store.YES)
    };
  }

  public DocumentBuilder setPolicyWaiverScopeOwnerId(final String scopeOwnerId) {
    if (scopeOwnerId != null) {
      this.policyWaiverScopeOwnerId =
          Optional.of(new TextField(POLICY_WAIVER_SCOPE_OWNER_ID.label, scopeOwnerId, Store.YES));
    }
    return this;
  }

  public DocumentBuilder setPolicyWaiverScopeOwnerType(final String scopeOwnerType) {
    if (scopeOwnerType != null) {
      this.policyWaiverScopeOwnerType =
          Optional.of(new TextField(POLICY_WAIVER_SCOPE_OWNER_TYPE.label, scopeOwnerType, Store.YES));
    }
    return this;
  }

  public DocumentBuilder setPolicyWaiverThreatLevel(final int threatLevel) {
    this.policyWaiverThreatLevel = Optional.of(new Field[]{
      new IntPoint(POLICY_WAIVER_THREAT_LEVEL.label, threatLevel),
      new StoredField(POLICY_WAIVER_THREAT_LEVEL.label, threatLevel)});
    return this;
  }

  /**
   * Auto-vs-manual discriminator stored as a keyword-style {@link StringField} "true"/"false" so it
   * is exact-match queryable and round-trips through the OpenSearch {@code _source}. No sort
   * doc-values are needed: it is only used for filtering, never sorting.
   */
  public DocumentBuilder setPolicyWaiverAuto(final boolean auto) {
    this.policyWaiverAuto = Optional.of(new StringField(POLICY_WAIVER_AUTO.label, Boolean.toString(auto), Store.YES));
    return this;
  }

  public DocumentBuilder setPolicyWaiverWaivedBy(final String waivedBy) {
    if (waivedBy != null) {
      this.policyWaiverWaivedBy = Optional.of(new TextField(POLICY_WAIVER_WAIVED_BY.label, waivedBy, Store.YES));
    }
    return this;
  }

  // ---- Application evaluation denormalization ----------------------------------------------

  /**
   * Multi-valued category (tag) names for the owning application. Distinct from the single-valued
   * {@link #setApplicationCategoryName} used by APPLICATION_CATEGORY docs; both write the shared
   * {@link FieldIdentifier#APPLICATION_CATEGORY_NAME} label so a category filter matches across
   * category entities, applications, and violation docs. A null/empty collection writes nothing,
   * so pre-reindex docs (no category field) are matched by nothing rather than NPE-ing.
   */
  public DocumentBuilder setApplicationCategoryNames(final Collection<String> categoryNames) {
    if (categoryNames != null && !categoryNames.isEmpty()) {
      this.applicationCategoryNames = Optional.of(categoryNames.stream()
          .filter(Objects::nonNull)
          .map(name -> new TextField(APPLICATION_CATEGORY_NAME.label, name, Store.YES))
          .toArray(Field[]::new));
    }
    return this;
  }

  /**
   * Epoch-millis of the application's latest evaluation. A {@link LongPoint} (indexed, not stored)
   * backs {@code [x TO *]} range filters; a {@link StoredField} keeps the display value. The numeric
   * sort doc-values twin is added in {@link LuceneIndexingContext#addDocuments}, not here, so no null
   * serializes into the OpenSearch {@code _source}. Null (never evaluated) writes nothing.
   */
  public DocumentBuilder setApplicationLastEvaluationTimeEpochMs(final Long epochMs) {
    if (epochMs != null) {
      this.applicationLastEvaluationTimeEpochMs = Optional.of(new Field[]{
        new LongPoint(APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS.label, epochMs),
        new StoredField(APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS.label, epochMs)});
    }
    return this;
  }

  /**
   * Multi-valued per-stage x severity violation counts, each entry {@code "stage:severity:count"}.
   * A null/empty collection writes nothing (an app with no violations emits no entries), so the
   * evaluation-card pills read zero for every bucket rather than NPE-ing on a pre-reindex doc.
   */
  public DocumentBuilder setApplicationStageSeverityCounts(final Collection<String> encodedEntries) {
    if (encodedEntries != null && !encodedEntries.isEmpty()) {
      this.applicationStageSeverityCounts = Optional.of(encodedEntries.stream()
          .filter(Objects::nonNull)
          .map(entry -> new StringField(APPLICATION_STAGE_SEVERITY_COUNT.label, entry, Store.YES))
          .toArray(Field[]::new));
    }
    return this;
  }

  public Document build() {
    organizationId.ifPresent(this::setFields);
    organizationName.ifPresent(this::setFields);
    applicationId.ifPresent(this::setFields);
    applicationPublicId.ifPresent(this::setFields);
    applicationName.ifPresent(this::setFields);
    policyEvaluationStage.ifPresent(this::setFields);
    reportId.ifPresent(this::setFields);
    componentHash.ifPresent(this::setFields);
    componentFormat.ifPresent(this::setFields);
    componentCoordinates.ifPresent(this::setFields);
    componentName.ifPresent(this::setFields);
    vulnerabilityId.ifPresent(this::setFields);
    vulnerabilitySeverity.ifPresent(this::setFields);
    vulnerabilityStatus.ifPresent(this::setFields);
    vulnerabilityDescription.ifPresent(this::setFields);
    applicationCategoryId.ifPresent(this::setFields);
    applicationCategoryName.ifPresent(this::setFields);
    applicationCategoryColor.ifPresent(this::setFields);
    applicationCategoryDescription.ifPresent(this::setFields);
    componentLabelId.ifPresent(this::setFields);
    componentLabelName.ifPresent(this::setFields);
    componentLabelColor.ifPresent(this::setFields);
    componentLabelDescription.ifPresent(this::setFields);
    policyId.ifPresent(this::setFields);
    policyName.ifPresent(this::setFields);
    policyThreatCategory.ifPresent(this::setFields);
    policyThreatLevel.ifPresent(this::setFields);
    parentOrganizationNames.ifPresent(this::setFields);
    parentOrganizationIds.ifPresent(this::setFields);
    applicationVersion.ifPresent(this::setFields);
    sbomSpecification.ifPresent(this::setFields);
    policyViolationId.ifPresent(this::setFields);
    policyViolationThreatCategory.ifPresent(this::setFields);
    policyViolationThreatLevel.ifPresent(this::setFields);
    policyViolationPolicyName.ifPresent(this::setFields);
    policyViolationPolicyId.ifPresent(this::setFields);
    policyViolationWaiverStatus.ifPresent(this::setFields);
    policyViolationConstraintName.ifPresent(this::setFields);
    componentEffectiveLicenseId.ifPresent(this::setFields);
    componentEffectiveLicenseName.ifPresent(this::setFields);
    componentLicenseThreatGroupName.ifPresent(this::setFields);
    componentLicenseThreatLevel.ifPresent(this::setFields);
    allowedContextIds.ifPresent(this::setFields);
    policyWaiverId.ifPresent(this::setFields);
    policyWaiverPolicyName.ifPresent(this::setFields);
    policyWaiverPolicyId.ifPresent(this::setFields);
    policyWaiverReason.ifPresent(this::setFields);
    policyWaiverComment.ifPresent(this::setFields);
    policyWaiverCreatedAt.ifPresent(this::setFields);
    policyWaiverCreatedAtEpochMs.ifPresent(this::setFields);
    policyWaiverExpiresAt.ifPresent(this::setFields);
    policyWaiverExpiresAtEpochMs.ifPresent(this::setFields);
    policyWaiverScopeOwnerId.ifPresent(this::setFields);
    policyWaiverScopeOwnerType.ifPresent(this::setFields);
    policyWaiverThreatLevel.ifPresent(this::setFields);
    policyWaiverWaivedBy.ifPresent(this::setFields);
    policyWaiverAuto.ifPresent(this::setFields);
    applicationCategoryNames.ifPresent(this::setFields);
    applicationLastEvaluationTimeEpochMs.ifPresent(this::setFields);
    applicationStageSeverityCounts.ifPresent(this::setFields);
    addDocumentKey();
    return document;
  }

  /**
   * Stable {@link FieldIdentifier#DOCUMENT_KEY} tie-breaker (SHA-256 over sorted field pairs).
   * {@code allowedContextIds} is excluded because that permission closure can change without the
   * document's identity changing. The Lucene sort doc-values twin is added separately in
   * {@link LuceneIndexingContext}, not here: a same-named doc-values field would serialize a null
   * into the OpenSearch {@code _source} and NPE on read-back.
   */
  private void addDocumentKey() {
    List<String> parts = new ArrayList<>();
    for (IndexableField field : document.getFields()) {
      if (ALLOWED_CONTEXT_IDS.label.equals(field.name())) {
        continue;
      }
      String value = field.stringValue();
      if (value == null && field.numericValue() != null) {
        value = field.numericValue().toString();
      }
      parts.add(field.name() + '=' + (value == null ? "" : value));
    }
    Collections.sort(parts);
    String key = sha256Hex(String.join("\n", parts));
    document.add(new StringField(DOCUMENT_KEY.label, key, Store.YES));
  }

  private static String sha256Hex(final String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(md.digest(input.getBytes(StandardCharsets.UTF_8)));
    }
    catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private void setFields(Field... fields) {
    Arrays.stream(fields).forEach(document::add);
  }
}
