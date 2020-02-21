/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.docs;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.model.component.Component;

import lombok.Getter;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

import static com.sonatype.insight.brain.search.docs.DocumentFields.FieldIdentifier.*;

@Getter
public class DocumentFields
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
    VULNERABILITY_ID("vulnerabilityId"),
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

    public static Set<String> labelIdentifiers() {
      return Arrays.stream(values()).map(f -> f.label).collect(Collectors.toSet());
    }

    public static FieldIdentifier byLabel(String label) {
      return Arrays.stream(FieldIdentifier.values()).filter(fieldIdentifier -> fieldIdentifier.label.equals(label))
          .findAny().get();
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

  public DocumentFields(ItemType itemType) {
    this.itemType = new StringField(ITEM_TYPE.label, itemType.name(), Store.YES);
  }

  public void setOrganizationId(final String organizationId) {
    this.organizationId = Optional.of(new StringField(ORGANIZATION_ID.label, organizationId, Store.YES));
  }

  public void setOrganizationName(final String organizationName) {
    this.organizationName = Optional.of(new TextField(ORGANIZATION_NAME.label, organizationName, Store.YES));
  }

  public void setApplicationId(final String applicationId) {
    this.applicationId = Optional.of(new StringField(APPLICATION_ID.label, applicationId, Store.YES));
  }

  public void setApplicationPublicId(final String applicationPublicId) {
    this.applicationPublicId =
        Optional.of(new StringField(APPLICATION_PUBLIC_ID.label, applicationPublicId, Store.YES));
  }

  public void setApplicationName(final String applicationName) {
    this.applicationName = Optional.of(new TextField(APPLICATION_NAME.label, applicationName, Store.YES));
  }

  public void setPolicyEvaluationStage(final String stageTypeName) {
    this.policyEvaluationStage = Optional.of(new StringField(POLICY_EVALUATION_STAGE.label, stageTypeName, Store.YES));
  }

  public void setReportId(final String reportId) {
    this.reportId = Optional.of(new StringField(REPORT_ID.label, reportId, Store.YES));
  }

  public void setComponentHash(final String hash) {
    this.componentHash = Optional.of(new StringField(COMPONENT_HASH.label, hash, Store.YES));
  }

  public void setComponentFormat(final String format) {
    this.componentFormat = Optional.of(new StringField(COMPONENT_FORMAT.label, format, Store.YES));
  }

  public void setComponentCoordinates(final Component component) {
    this.componentCoordinates = Optional.of(component.getComponentIdentifier().getCoordinates().entrySet().stream()
        .map(coordinate -> new StringField(toFieldName(coordinate.getKey()), coordinate.getValue(), Store.YES))
        .collect(Collectors.toList()));
  }

  private static String toFieldName(String coordinateName) {
    return "componentCoordinate" + Character.toUpperCase(coordinateName.charAt(0)) + coordinateName.substring(1);
  }

  public void setComponentName(final String componentDisplayName) {
    this.componentName =
        Optional.of(new StringField(COMPONENT_NAME.label, componentDisplayName, Store.YES));
  }

  public void setVulnerabilityId(final String refId) {
    this.vulnerabilityId = Optional.of(new StringField(VULNERABILITY_ID.label, refId, Store.YES));
  }

  public void setVulnerabilityStatus(final String status) {
    this.vulnerabilityStatus = Optional.of(new StringField(VULNERABILITY_STATUS.label, status, Store.YES));
  }

  public void setVulnerabilityDescription(final String description) {
    this.vulnerabilityDescription = Optional.of(new TextField(VULNERABILITY_DESCRIPTION.label, description, Store.YES));
  }

  public void setApplicationCategoryId(final String tagId) {
    this.applicationCategoryId = Optional.of(new StringField(APPLICATION_CATEGORY_ID.label, tagId, Store.YES));
  }

  public void setApplicationCategoryName(final String tagName) {
    this.applicationCategoryName = Optional.of(new StringField(APPLICATION_CATEGORY_NAME.label, tagName, Store.YES));
  }

  public void setApplicationCategoryColor(final String tagColor) {
    this.applicationCategoryColor = Optional.of(new StringField(APPLICATION_CATEGORY_COLOR.label, tagColor, Store.YES));
  }

  public void setApplicationCategoryDescription(final String tagDescription) {
    this.applicationCategoryDescription =
        Optional.of(new StringField(APPLICATION_CATEGORY_DESCRIPTION.label, tagDescription, Store.YES));
  }

  public void setComponentLabelId(final String labelId) {
    this.componentLabelId = Optional.of(new StringField(COMPONENT_LABEL_ID.label, labelId, Store.YES));
  }

  public void setComponentLabelName(final String labelName) {
    this.componentLabelName = Optional.of(new StringField(COMPONENT_LABEL_NAME.label, labelName, Store.YES));
  }

  public void setComponentLabelColor(final String labelColor) {
    this.componentLabelColor = Optional.of(new StringField(COMPONENT_LABEL_COLOR.label, labelColor, Store.YES));
  }

  public void setComponentLabelDescription(final String labelDescription) {
    this.componentLabelDescription = Optional.of(new StringField(COMPONENT_LABEL_DESCRIPTION.label,
        Optional.ofNullable(labelDescription).orElse(""), Store.YES));
  }

  public void setPolicyId(final String policyId) {
    this.policyId = Optional.of(new StringField(POLICY_ID.label, policyId, Store.YES));
  }

  public void setPolicyName(final String policyName) {
    this.policyName = Optional.of(new StringField(POLICY_NAME.label, policyName, Store.YES));
  }

  public void setPolicyThreatCategory(final String policyThreatCategory) {
    this.policyThreatCategory =
        Optional.of(new StringField(POLICY_THREAT_CATEGORY.label, policyThreatCategory, Store.YES));
  }

  public void setPolicyThreatLevel(final int policyThreatLevel) {
    this.policyThreatLevel = Optional.of(Arrays.asList(new IntPoint(POLICY_THREAT_LEVEL.label, policyThreatLevel),
        new StoredField(POLICY_THREAT_LEVEL.label, policyThreatLevel)));
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
