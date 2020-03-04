/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.search.docs.DocumentBuilder.FieldIdentifier;
import com.sonatype.insight.brain.search.docs.DocumentBuilder.ItemType;
import com.sonatype.insight.brain.search.index.IndexService.IndexingContext;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexableField;
import org.assertj.core.groups.Tuple;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

public class IndexServiceTest
    extends AbstractComponentTest
{
  @Inject
  private IndexService indexService;

  @Mock
  private VulnerabilityDescriptionFetcher vulnerabilityDescriptionFetcher;

  @Override
  public void configure(Binder binder) {
    binder.bind(VulnerabilityDescriptionFetcher.class).toInstance(vulnerabilityDescriptionFetcher);
    super.configure(binder);
  }

  private IndexingContext newIndexingContext() {
    return indexService.new IndexingContext();
  }

  private Object fieldValue(IndexableField field) {
    Object value = field.numericValue();
    if (value == null) {
      value = field.stringValue();
    }
    return value;
  }

  private void assertFields(Document document, Tuple... fields) {
    assertThat(document).isNotNull();
    assertThat(document.getFields())
        .extracting(IndexableField::name, this::fieldValue, Object::getClass, field -> field.fieldType().stored())
        .containsExactlyInAnyOrder(fields);
  }

  private Tuple field(
      FieldIdentifier fieldName,
      Object fieldValue,
      Class<? extends IndexableField> fieldType,
      boolean stored)
  {
    return field(fieldName.label, fieldValue, fieldType, stored);
  }

  private Tuple field(String fieldName, Object fieldValue, Class<? extends IndexableField> fieldType, boolean stored) {
    return tuple(fieldName, fieldValue, fieldType, stored);
  }

  @Test
  public void testBuildDocument_Organization() {
    Organization org = tempEntity.newOrganization();
    assertFields(indexService.buildDocument(newIndexingContext(), org),
        field(FieldIdentifier.ITEM_TYPE, ItemType.ORGANIZATION.name(), TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_ID, org.getId(), TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_NAME, org.getName(), TextField.class, true));
  }

  @Test
  public void testBuildDocument_Application() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    assertFields(indexService.buildDocument(newIndexingContext(), app),
        field(FieldIdentifier.ITEM_TYPE, ItemType.APPLICATION.name(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_ID, app.getId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_PUBLIC_ID, app.getPublicId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_NAME, app.getName(), TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_ID, org.getId(), TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_NAME, org.getName(), TextField.class, true));
  }

  @Test
  public void testBuildDocument_Policy() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app);
    assertFields(indexService.buildDocument(newIndexingContext(), policy),
        field(FieldIdentifier.ITEM_TYPE, ItemType.POLICY.name(), TextField.class, true),
        field(FieldIdentifier.POLICY_ID, policy.getId(), TextField.class, true),
        field(FieldIdentifier.POLICY_NAME, policy.getName(), TextField.class, true),
        field(FieldIdentifier.POLICY_THREAT_CATEGORY, policy.getThreatCategory().getName(), TextField.class, true),
        field(FieldIdentifier.POLICY_THREAT_LEVEL, policy.getThreatLevel(), IntPoint.class, false),
        field(FieldIdentifier.POLICY_THREAT_LEVEL, policy.getThreatLevel(), StoredField.class, true),
        field(FieldIdentifier.APPLICATION_ID, app.getId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_PUBLIC_ID, app.getPublicId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_NAME, app.getName(), TextField.class, true));
  }

  @Test
  public void testBuildDocument_Tag() {
    Organization org = tempEntity.newOrganization();
    Tag tag = tempEntity.newTag(org.getId());
    assertFields(indexService.buildDocument(newIndexingContext(), tag),
        field(FieldIdentifier.ITEM_TYPE, ItemType.APPLICATION_CATEGORY.name(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_CATEGORY_ID, tag.getId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_CATEGORY_NAME, tag.getName(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_CATEGORY_COLOR, tag.getColor().toValue(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_CATEGORY_DESCRIPTION, tag.getDescription(), TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_ID, org.getId(), TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_NAME, org.getName(), TextField.class, true));
  }

  @Test
  public void testBuildDocument_Label() {
    Application app = tempEntity.newApplicationWithParent();
    Label label = tempEntity.newLabel(app.getId());
    assertFields(indexService.buildDocument(newIndexingContext(), label),
        field(FieldIdentifier.ITEM_TYPE, ItemType.COMPONENT_LABEL.name(), TextField.class, true),
        field(FieldIdentifier.COMPONENT_LABEL_ID, label.getId(), TextField.class, true),
        field(FieldIdentifier.COMPONENT_LABEL_NAME, label.getLabel(), TextField.class, true),
        field(FieldIdentifier.COMPONENT_LABEL_COLOR, label.getColor().toValue(), TextField.class, true),
        field(FieldIdentifier.COMPONENT_LABEL_DESCRIPTION, "", TextField.class, true),
        field(FieldIdentifier.APPLICATION_ID, app.getId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_PUBLIC_ID, app.getPublicId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_NAME, app.getName(), TextField.class, true));
  }

  @Test
  public void testBuildDocument_ComponentVulnerability() {
    Application app = tempEntity.newApplicationWithParent();
    String reportId = "report-id";
    ComponentIdentifier componentId = ComponentIdentifier.createNpmCoordinates("@org/package", "1.2.3");
    Component component = new Component(componentId);
    component.setHash("01234567890123456789");
    SecurityVulnerability vuln = new SecurityVulnerability("cve", "CVE-4321-1234", 7.5f);
    String vulnDescription = "This is a bad vulnerability, stay clear!";
    when(vulnerabilityDescriptionFetcher.getVulnerabilityDescription(vuln.getRefId())).thenReturn(vulnDescription);
    assertFields(indexService.buildDocument(newIndexingContext(), app, StageTypes.BUILD, reportId, component, vuln),
        field(FieldIdentifier.ITEM_TYPE, ItemType.SECURITY_VULNERABILITY.name(), TextField.class, true),
        field(FieldIdentifier.COMPONENT_HASH, component.getHash(), TextField.class, true),
        field(FieldIdentifier.COMPONENT_FORMAT, componentId.getFormat(), TextField.class, true),
        field(FieldIdentifier.COMPONENT_COORDINATE + "PackageId", componentId.get(ComponentIdentifier.NPM_PACKAGE_ID),
            TextField.class, true),
        field(FieldIdentifier.COMPONENT_COORDINATE + "Version", componentId.get(ComponentIdentifier.VERSION),
            TextField.class, true),
        field(FieldIdentifier.COMPONENT_NAME, component.getDisplayName(), TextField.class, true),
        field(FieldIdentifier.VULNERABILITY_ID, vuln.getRefId(), TextField.class, true),
        field(FieldIdentifier.VULNERABILITY_SEVERITY, vuln.getSeverity(), FloatPoint.class, false),
        field(FieldIdentifier.VULNERABILITY_SEVERITY, vuln.getSeverity(), StoredField.class, true),
        field(FieldIdentifier.VULNERABILITY_STATUS, vuln.getStatus().getName(), TextField.class, true),
        field(FieldIdentifier.VULNERABILITY_DESCRIPTION, vulnDescription, TextField.class, true),
        field(FieldIdentifier.POLICY_EVALUATION_STAGE, StageTypes.BUILD.getName(), TextField.class, true),
        field(FieldIdentifier.REPORT_ID, reportId, TextField.class, true),
        field(FieldIdentifier.APPLICATION_ID, app.getId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_PUBLIC_ID, app.getPublicId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_NAME, app.getName(), TextField.class, true));
  }
}
