/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;

import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_CATEGORY_COLOR;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_CATEGORY_DESCRIPTION;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_CATEGORY_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_CATEGORY_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_PUBLIC_ID;
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

public class DocumentBuilder
{
  private Document document;

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

  public DocumentBuilder(ItemType itemType) {
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
    this.applicationId = Optional.of(new TextField(APPLICATION_ID.label, applicationId, Store.YES));
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
    return document;
  }

  private void setFields(Field... fields) {
    Arrays.stream(fields).forEach(document::add);
  }
}
