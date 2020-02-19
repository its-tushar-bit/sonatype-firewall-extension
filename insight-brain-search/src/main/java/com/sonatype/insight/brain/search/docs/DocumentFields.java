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
  public enum DocumentType
  {
    ORGANIZATION,
    APPLICATION,
    SECURITY_VULNERABILITY,
    TAG,
    LABEL,
    POLICY
  }

  public enum FieldIdentifier
  {
    DOCUMENT_TYPE("documentType"),
    ORGANIZATION_ID("organizationId"),
    ORGANIZATION_NAME("organizationName"),
    APPLICATION_ID("applicationId"),
    APPLICATION_NAME("applicationName"),
    APPLICATION_PUBLIC_ID("applicationPublicId"),
    STAGE("stage"),
    SCAN_ID("scanId"),
    HASH("hash"),
    FORMAT("format"),
    COMPONENT_DISPLAY_NAME("componentDisplayName"),
    REFERENCE_ID("refId"),
    STATUS("status"),
    DESCRIPTION("description"),
    TAG_ID("tagId"),
    TAG_NAME("tagName"),
    TAG_COLOR("tagColor"),
    TAG_DESCRIPTION("tagDescription"),
    LABEL_ID("labelId"),
    LABEL_NAME("labelName"),
    LABEL_COLOR("labelColor"),
    LABEL_DESCRIPTION("labelDescription"),
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

  private final Field documentType;

  private Optional<Field> organizationId = Optional.empty();

  private Optional<Field> organizationName = Optional.empty();

  private Optional<Field> applicationId = Optional.empty();

  private Optional<Field> applicationPublicId = Optional.empty();

  private Optional<Field> applicationName = Optional.empty();

  private Optional<Field> stage = Optional.empty();

  private Optional<Field> scan = Optional.empty();

  private Optional<Field> hash = Optional.empty();

  private Optional<Field> format = Optional.empty();

  private Optional<List<Field>> coordinates = Optional.empty();

  private Optional<Field> componentDisplayName = Optional.empty();

  private Optional<Field> refId = Optional.empty();

  private Optional<Field> status = Optional.empty();

  private Optional<Field> description = Optional.empty();

  private Optional<Field> tagId = Optional.empty();

  private Optional<Field> tagName = Optional.empty();

  private Optional<Field> tagColor = Optional.empty();

  private Optional<Field> tagDescription = Optional.empty();

  private Optional<Field> labelId = Optional.empty();

  private Optional<Field> labelName = Optional.empty();

  private Optional<Field> labelColor = Optional.empty();

  private Optional<Field> labelDescription = Optional.empty();

  private Optional<Field> policyId = Optional.empty();

  private Optional<Field> policyName = Optional.empty();

  private Optional<Field> policyThreatCategory = Optional.empty();

  private Optional<List<Field>> policyThreatLevel = Optional.empty();

  public DocumentFields(DocumentType documentType) {
    this.documentType = new StringField(DOCUMENT_TYPE.label, documentType.name(), Store.YES);
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

  public void setStage(final String stageTypeName) {
    this.stage = Optional.of(new StringField(STAGE.label, stageTypeName, Store.YES));
  }

  public void setScan(final String scanId) {
    this.scan = Optional.of(new StringField(SCAN_ID.label, scanId, Store.YES));
  }

  public void setHash(final String hash) {
    this.hash = Optional.of(new StringField(HASH.label, hash, Store.YES));
  }

  public void setFormat(final String format) {
    this.format = Optional.of(new StringField(FORMAT.label, format, Store.YES));
  }

  public void setCoordinates(final Component component) {
    this.coordinates = Optional.of(component.getComponentIdentifier().getCoordinates().entrySet().stream()
        .map(coordinate -> new StringField(coordinate.getKey(), coordinate.getValue(), Store.YES))
        .collect(Collectors.toList()));
  }

  public Optional<Field> getComponentDisplayName() {
    return componentDisplayName;
  }

  public void setComponentDisplayName(final String componentDisplayName) {
    this.componentDisplayName =
        Optional.of(new StringField(COMPONENT_DISPLAY_NAME.label, componentDisplayName, Store.YES));
  }

  public void setRefId(final String refId) {
    this.refId = Optional.of(new StringField(REFERENCE_ID.label, refId, Store.YES));
  }

  public void setStatus(final String status) {
    this.status = Optional.of(new StringField(STATUS.label, status, Store.YES));
  }

  public void setDescription(final String description) {
    this.description = Optional.of(new TextField(DESCRIPTION.label, description, Store.YES));
  }

  public void setTagId(final String tagId) {
    this.tagId = Optional.of(new StringField(TAG_ID.label, tagId, Store.YES));
  }

  public void setTagName(final String tagName) {
    this.tagName = Optional.of(new StringField(TAG_NAME.label, tagName, Store.YES));
  }

  public void setTagColor(final String tagColor) {
    this.tagColor = Optional.of(new StringField(TAG_COLOR.label, tagColor, Store.YES));
  }

  public void setTagDescription(final String tagDescription) {
    this.tagDescription = Optional.of(new StringField(TAG_DESCRIPTION.label, tagDescription, Store.YES));
  }

  public void setLabelId(final String labelId) {
    this.labelId = Optional.of(new StringField(LABEL_ID.label, labelId, Store.YES));
  }

  public void setLabelName(final String labelName) {
    this.labelName = Optional.of(new StringField(LABEL_NAME.label, labelName, Store.YES));
  }

  public void setLabelColor(final String labelColor) {
    this.labelColor = Optional.of(new StringField(LABEL_COLOR.label, labelColor, Store.YES));
  }

  public void setLabelDescription(final String labelDescription) {
    this.labelDescription = Optional.of(new StringField(LABEL_DESCRIPTION.label,
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
    document.add(documentType);
    organizationId.ifPresent(document::add);
    organizationName.ifPresent(document::add);
    applicationId.ifPresent(document::add);
    applicationPublicId.ifPresent(document::add);
    applicationName.ifPresent(document::add);
    stage.ifPresent(document::add);
    scan.ifPresent(document::add);
    hash.ifPresent(document::add);
    format.ifPresent(document::add);
    coordinates.ifPresent(coordinates -> coordinates.forEach(document::add));
    componentDisplayName.ifPresent(document::add);
    refId.ifPresent(document::add);
    status.ifPresent(document::add);
    description.ifPresent(document::add);
    tagId.ifPresent(document::add);
    tagName.ifPresent(document::add);
    tagColor.ifPresent(document::add);
    tagDescription.ifPresent(document::add);
    labelId.ifPresent(document::add);
    labelName.ifPresent(document::add);
    labelColor.ifPresent(document::add);
    labelDescription.ifPresent(document::add);
    policyId.ifPresent(document::add);
    policyName.ifPresent(document::add);
    policyThreatCategory.ifPresent(document::add);
    policyThreatLevel.ifPresent(policyThreatLevel -> policyThreatLevel.forEach(document::add));
    return document;
  }
}
