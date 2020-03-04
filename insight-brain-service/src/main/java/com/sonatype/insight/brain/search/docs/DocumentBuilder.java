/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.docs;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;

import static com.sonatype.insight.brain.search.docs.DocumentBuilder.FieldIdentifier.*;

public class DocumentBuilder
{
  public enum ItemType
  {
    ORGANIZATION,
    APPLICATION,
    SECURITY_VULNERABILITY,
    APPLICATION_CATEGORY,
    COMPONENT_LABEL,
    POLICY
  }

  public enum FieldIdentifier
  {
    ITEM_TYPE("itemType"),
    ORGANIZATION_ID("organizationId"),
    ORGANIZATION_NAME("organizationName"),
    APPLICATION_ID("applicationId"),
    APPLICATION_NAME("applicationName"),
    APPLICATION_PUBLIC_ID("applicationPublicId"),
    POLICY_EVALUATION_STAGE("policyEvaluationStage"),
    REPORT_ID("reportId"),
    COMPONENT_HASH("componentHash"),
    COMPONENT_FORMAT("componentFormat"),
    COMPONENT_NAME("componentName"),
    COMPONENT_COORDINATE("componentCoordinate"),
    VULNERABILITY_ID("vulnerabilityId"),
    VULNERABILITY_SEVERITY("vulnerabilitySeverity"),
    VULNERABILITY_STATUS("vulnerabilityStatus"),
    VULNERABILITY_DESCRIPTION("vulnerabilityDescription"),
    APPLICATION_CATEGORY_ID("applicationCategoryId"),
    APPLICATION_CATEGORY_NAME("applicationCategoryName"),
    APPLICATION_CATEGORY_COLOR("applicationCategoryColor"),
    APPLICATION_CATEGORY_DESCRIPTION("applicationCategoryDescription"),
    COMPONENT_LABEL_ID("componentLabelId"),
    COMPONENT_LABEL_NAME("componentLabelName"),
    COMPONENT_LABEL_COLOR("componentLabelColor"),
    COMPONENT_LABEL_DESCRIPTION("componentLabelDescription"),
    POLICY_ID("policyId"),
    POLICY_NAME("policyName"),
    POLICY_THREAT_CATEGORY("policyThreatCategory"),
    POLICY_THREAT_LEVEL("policyThreatLevel");

    public final String label;

    FieldIdentifier(String label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return label;
    }
  }

  private final Field itemType;

  private Optional<Field> organizationId = Optional.empty();

  private Optional<Field> organizationName = Optional.empty();

  private Optional<Field> applicationId = Optional.empty();

  private Optional<Field> applicationPublicId = Optional.empty();

  private Optional<Field> applicationName = Optional.empty();

  private Optional<Field> policyEvaluationStage = Optional.empty();

  private Optional<Field> reportId = Optional.empty();

  private Optional<Field> componentHash = Optional.empty();

  private Optional<Field> componentFormat = Optional.empty();

  private Optional<List<Field>> componentCoordinates = Optional.empty();

  private Optional<Field> componentName = Optional.empty();

  private Optional<Field> vulnerabilityId = Optional.empty();

  private Optional<Field> vulnerabilityStatus = Optional.empty();

  private Optional<List<Field>> vulnerabilitySeverity = Optional.empty();

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

  private Optional<List<Field>> policyThreatLevel = Optional.empty();

  public DocumentBuilder(ItemType itemType) {
    this.itemType = new TextField(ITEM_TYPE.label, itemType.name(), Store.YES);
  }

  public DocumentBuilder setOwner(Owner owner) {
    if (owner.getType() == OwnerType.ORGANIZATION) {
      setOrganizationId(owner.getId());
      setOrganizationName(owner.getName());
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

  public DocumentBuilder setPolicyEvaluationStage(final String stageTypeName) {
    this.policyEvaluationStage = Optional.of(new TextField(POLICY_EVALUATION_STAGE.label, stageTypeName, Store.YES));
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
    this.componentCoordinates = Optional.of(component.getComponentIdentifier().getCoordinates().entrySet().stream().map(
        coordinate -> new TextField(getFieldNameForCoordinate(coordinate.getKey()), coordinate.getValue(), Store.YES))
        .collect(Collectors.toList()));
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
          Optional.of(Arrays.asList(new FloatPoint(VULNERABILITY_SEVERITY.label, vulnerabilitySeverity),
              new StoredField(VULNERABILITY_SEVERITY.label, vulnerabilitySeverity)));
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
    this.vulnerabilityDescription = Optional.of(new TextField(VULNERABILITY_DESCRIPTION.label, description, Store.YES));
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
    this.policyThreatLevel = Optional.of(Arrays.asList(new IntPoint(POLICY_THREAT_LEVEL.label, policyThreatLevel),
        new StoredField(POLICY_THREAT_LEVEL.label, policyThreatLevel)));
    return this;
  }

  public Document build() {
    Document document = new Document();
    document.add(itemType);
    organizationId.ifPresent(document::add);
    organizationName.ifPresent(document::add);
    applicationId.ifPresent(document::add);
    applicationPublicId.ifPresent(document::add);
    applicationName.ifPresent(document::add);
    policyEvaluationStage.ifPresent(document::add);
    reportId.ifPresent(document::add);
    componentHash.ifPresent(document::add);
    componentFormat.ifPresent(document::add);
    componentCoordinates.ifPresent(coordinates -> coordinates.forEach(document::add));
    componentName.ifPresent(document::add);
    vulnerabilityId.ifPresent(document::add);
    vulnerabilitySeverity.ifPresent(fields -> fields.forEach(document::add));
    vulnerabilityStatus.ifPresent(document::add);
    vulnerabilityDescription.ifPresent(document::add);
    applicationCategoryId.ifPresent(document::add);
    applicationCategoryName.ifPresent(document::add);
    applicationCategoryColor.ifPresent(document::add);
    applicationCategoryDescription.ifPresent(document::add);
    componentLabelId.ifPresent(document::add);
    componentLabelName.ifPresent(document::add);
    componentLabelColor.ifPresent(document::add);
    componentLabelDescription.ifPresent(document::add);
    policyId.ifPresent(document::add);
    policyName.ifPresent(document::add);
    policyThreatCategory.ifPresent(document::add);
    policyThreatLevel.ifPresent(policyThreatLevel -> policyThreatLevel.forEach(document::add));
    return document;
  }
}
