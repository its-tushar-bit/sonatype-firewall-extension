/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.IndexCreationScheduler;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.index.VulnerabilityDescriptionFetcher;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.inject.Binder;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CheckIndex.CheckIndexException;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexFormatTooNewException;
import org.apache.lucene.index.IndexFormatTooOldException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MergePolicy.MergeException;
import org.apache.lucene.index.TwoPhaseCommitTool.CommitFailException;
import org.apache.lucene.index.TwoPhaseCommitTool.PrepareCommitFailException;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.LockReleaseFailedException;
import org.apache.lucene.util.ThreadInterruptedException;
import org.assertj.core.groups.Tuple;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.PENDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class LuceneSearchIndexClientTest
    extends AbstractComponentTest
{
  @Inject
  private LuceneSearchIndexClient luceneSearchIndexClient;

  @Inject
  private DocumentBuilderHelper documentBuilderHelper;

  @Inject
  private ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  @Inject
  private InsightWork work;

  @Inject
  private OrganizationDAO organizationDAO;

  @Inject
  private OwnerDAO ownerDAO;

  @Mock
  private VulnerabilityDescriptionFetcher vulnerabilityDescriptionFetcher;

  @Mock
  private TelemetrySender telemetrySenderMock;

  @Mock
  private TaskScheduler taskSchedulerMock;

  @Mock
  private IndexWriter indexWriterMock;

  @Mock
  private ConversionHelper conversionHelperMock;

  @Mock
  private IndexCreationScheduler mockIndexCreationScheduler;

  @Override
  public void configure(Binder binder) {
    binder.bind(VulnerabilityDescriptionFetcher.class).toInstance(vulnerabilityDescriptionFetcher);
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
    binder.bind(TaskScheduler.class).toInstance(taskSchedulerMock);
    binder.bind(IndexCreationScheduler.class).toInstance(mockIndexCreationScheduler);
    super.configure(binder);
  }

  private LuceneIndexingContext newIndexingContext() {
    return new LuceneIndexingContext(ownerDAO, indexWriterMock, conversionHelperMock);
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
    assertFields(documentBuilderHelper.buildDocument(newIndexingContext(), org),
        field(FieldIdentifier.ITEM_TYPE, ItemType.ORGANIZATION.name(), TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_ID, org.getId(), TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_NAME, org.getName(), TextField.class, true),
        field(FieldIdentifier.PARENT_ORGANIZATION_NAME, org.getName(), TextField.class, true),
        field(FieldIdentifier.PARENT_ORGANIZATION_ID, org.getId(), TextField.class, true));
  }

  @Test
  public void testBuildDocument_Application() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    assertFields(documentBuilderHelper.buildDocument(newIndexingContext(), app),
        field(FieldIdentifier.ITEM_TYPE, ItemType.APPLICATION.name(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_ID, app.getId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_PUBLIC_ID, app.getPublicId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_NAME, app.getName(), TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_ID, org.getId(), TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_NAME, org.getName(), TextField.class, true),
        field(FieldIdentifier.PARENT_ORGANIZATION_ID, org.getId(), TextField.class, true),
        field(FieldIdentifier.PARENT_ORGANIZATION_NAME, org.getName(), TextField.class, true));
  }

  @Test
  public void testBuildDocument_SbomMetadata() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    LuceneIndexingContext indexingContext = newIndexingContext();
    indexingContext.addOwners(Arrays.asList(org, app));

    ThirdPartySbomMetadata sbomMetadata = tempEntity.newThirdPartySbomMetadata(app.getId(), ACTIVE, "bom.xml");
    sbomMetadata.setSbomVersion("1.2.3");
    sbomMetadata.setSpec("CycloneDx");
    thirdPartySbomMetadataDAO.update(sbomMetadata);

    Document document = documentBuilderHelper.buildDocument(indexingContext, sbomMetadata);

    assertFields(document,
        field(FieldIdentifier.ITEM_TYPE, ItemType.SBOM_METADATA.name(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_ID, app.getId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_PUBLIC_ID, app.getPublicId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_NAME, app.getName(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_VERSION, "1.2.3", TextField.class, true),
        field(FieldIdentifier.SBOM_SPECIFICATION, "CycloneDx", TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_ID, org.getId(), TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_NAME, org.getName(), TextField.class, true),
        field(FieldIdentifier.PARENT_ORGANIZATION_ID, org.getId(), TextField.class, true),
        field(FieldIdentifier.PARENT_ORGANIZATION_NAME, org.getName(), TextField.class, true));
  }

  @Test
  public void testBuildDocument_Sbom_ComponentVulnerability() {
    Organization rootOrg = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    LuceneIndexingContext indexingContext = newIndexingContext();
    indexingContext.addOwners(Arrays.asList(org, app));

    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ACTIVE, "bom.xml");
    sbomMetadata.setSbomVersion("1.2.3");
    sbomMetadata.setSpec("CycloneDx");
    thirdPartySbomMetadataDAO.update(sbomMetadata);

    ThirdPartyFileCoordinate thirdPartyFileCoord = tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "asdf",
        "npm", "jquery", "1.1.1", "deadbeef", "pkg:npm/jquery@1.1.1");

    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity = tempEntity.newThirdPartyCoordinateSecurity(
        thirdPartyFileCoord, "CVE-111-1111", "vulnDesc", "http://link", 9.0f, "severityDesc", "");

    Document document = documentBuilderHelper.buildDocument(org, app, sbomMetadata, thirdPartyFileCoord,
        thirdPartyCoordinateSecurity, Arrays.asList(org, rootOrg));

    assertFields(document,
        field(FieldIdentifier.ITEM_TYPE, ItemType.SECURITY_VULNERABILITY.name(), TextField.class, true),
        field(FieldIdentifier.COMPONENT_HASH, "deadbeef", TextField.class, true),
        field(FieldIdentifier.COMPONENT_FORMAT, "npm", TextField.class, true),
        field(FieldIdentifier.COMPONENT_COORDINATE + "PackageId", "jquery", TextField.class, true),
        field(FieldIdentifier.COMPONENT_COORDINATE + "Version", "1.1.1", TextField.class, true),
        field(FieldIdentifier.COMPONENT_NAME, "jquery : 1.1.1", TextField.class, true),
        field(FieldIdentifier.VULNERABILITY_ID, "CVE-111-1111", TextField.class, true),
        field(FieldIdentifier.VULNERABILITY_SEVERITY, 9.0f, FloatPoint.class, false),
        field(FieldIdentifier.VULNERABILITY_SEVERITY, 9.0f, StoredField.class, true),
        field(FieldIdentifier.VULNERABILITY_DESCRIPTION, "vulnDesc", TextField.class, true),
        field(FieldIdentifier.APPLICATION_ID, app.getId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_PUBLIC_ID, app.getPublicId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_NAME, app.getName(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_VERSION, "1.2.3", TextField.class, true),
        field(FieldIdentifier.SBOM_SPECIFICATION, "CycloneDx", TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_NAME, org.getName(), TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_ID, org.getId(), TextField.class, true),
        field(FieldIdentifier.PARENT_ORGANIZATION_ID, org.getId(), TextField.class, true),
        field(FieldIdentifier.PARENT_ORGANIZATION_NAME, org.getName(), TextField.class, true),
        field(FieldIdentifier.PARENT_ORGANIZATION_ID, rootOrg.getId(), TextField.class, true),
        field(FieldIdentifier.PARENT_ORGANIZATION_NAME, rootOrg.getName(), TextField.class, true));
  }

  @Test
  public void testBuildDocument_Sbom_NonVulnerableComponent() {
    Organization rootOrg = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    LuceneIndexingContext indexingContext = newIndexingContext();
    indexingContext.addOwners(Arrays.asList(org, app));

    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ACTIVE, "bom.xml");
    sbomMetadata.setSbomVersion("1.2.3");
    sbomMetadata.setSpec("CycloneDx");
    thirdPartySbomMetadataDAO.update(sbomMetadata);

    ThirdPartyFileCoordinate thirdPartyFileCoord = tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "asdf",
        "npm", "jquery", "1.1.1", "deadbeef", "pkg:npm/jquery@1.1.1");

    Document document = documentBuilderHelper.buildDocument(org, app, sbomMetadata, thirdPartyFileCoord,
        Arrays.asList(org, rootOrg));

    assertFields(document,
        field(FieldIdentifier.ITEM_TYPE, ItemType.NON_VULNERABLE_COMPONENT.name(), TextField.class, true),
        field(FieldIdentifier.COMPONENT_HASH, "deadbeef", TextField.class, true),
        field(FieldIdentifier.COMPONENT_FORMAT, "npm", TextField.class, true),
        field(FieldIdentifier.COMPONENT_COORDINATE + "PackageId", "jquery", TextField.class, true),
        field(FieldIdentifier.COMPONENT_COORDINATE + "Version", "1.1.1", TextField.class, true),
        field(FieldIdentifier.COMPONENT_NAME, "jquery : 1.1.1", TextField.class, true),
        field(FieldIdentifier.APPLICATION_ID, app.getId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_PUBLIC_ID, app.getPublicId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_NAME, app.getName(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_VERSION, "1.2.3", TextField.class, true),
        field(FieldIdentifier.SBOM_SPECIFICATION, "CycloneDx", TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_NAME, org.getName(), TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_ID, org.getId(), TextField.class, true),
        field(FieldIdentifier.PARENT_ORGANIZATION_ID, org.getId(), TextField.class, true),
        field(FieldIdentifier.PARENT_ORGANIZATION_NAME, org.getName(), TextField.class, true),
        field(FieldIdentifier.PARENT_ORGANIZATION_ID, rootOrg.getId(), TextField.class, true),
        field(FieldIdentifier.PARENT_ORGANIZATION_NAME, rootOrg.getName(), TextField.class, true));
  }

  @Test
  public void testBuildDocument_Policy() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app);
    assertFields(documentBuilderHelper.buildDocument(newIndexingContext(), policy),
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
    assertFields(documentBuilderHelper.buildDocument(newIndexingContext(), tag),
        field(FieldIdentifier.ITEM_TYPE, ItemType.APPLICATION_CATEGORY.name(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_CATEGORY_ID, tag.getId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_CATEGORY_NAME, tag.getName(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_CATEGORY_COLOR, tag.getColor().toValue(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_CATEGORY_DESCRIPTION, tag.getDescription(), TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_ID, org.getId(), TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_NAME, org.getName(), TextField.class, true),
        field(FieldIdentifier.PARENT_ORGANIZATION_ID, org.getId(), TextField.class, true),
        field(FieldIdentifier.PARENT_ORGANIZATION_NAME, org.getName(), TextField.class, true));
  }

  @Test
  public void testBuildDocument_Label() {
    Application app = tempEntity.newApplicationWithParent();
    Label label = tempEntity.newLabel(app.getId());
    assertFields(documentBuilderHelper.buildDocument(newIndexingContext(), label),
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
    Organization organization = tempEntity.newOrganization();
    Application app = tempEntity.newApplicationWithParent(organization);
    String reportId = "report-id";
    ComponentIdentifier componentId = ComponentIdentifier.createNpmCoordinates("@org/package", "1.2.3");
    Component component = new Component(componentId);
    component.setHash("01234567890123456789");
    SecurityVulnerability vuln = new SecurityVulnerability("cve", "CVE-4321-1234", 7.5f);
    String vulnDescription = "This is a bad vulnerability, stay clear!";
    when(vulnerabilityDescriptionFetcher.getVulnerabilityDescription(vuln.getRefId())).thenReturn(vulnDescription);
    assertFields(
        documentBuilderHelper.buildDocument(newIndexingContext(), organization, app, StageTypes.BUILD, reportId,
            component, vuln, Arrays.asList(organization)),
        field(FieldIdentifier.ITEM_TYPE, ItemType.SECURITY_VULNERABILITY.name(), TextField.class, true),
        field(FieldIdentifier.COMPONENT_HASH, component.getHash(), TextField.class, true),
        field(FieldIdentifier.COMPONENT_FORMAT, componentId.getFormat(), TextField.class, true),
        field(FieldIdentifier.COMPONENT_COORDINATE + "PackageId", componentId.get(ComponentIdentifier.NPM_PACKAGE_ID),
            TextField.class, true),
        field(FieldIdentifier.COMPONENT_COORDINATE + "Version", componentId.get(ComponentIdentifier.VERSION),
            TextField.class, true),
        field(FieldIdentifier.COMPONENT_NAME, component.getDisplayNameFromIdentifier(), TextField.class, true),
        field(FieldIdentifier.VULNERABILITY_ID, vuln.getRefId(), TextField.class, true),
        field(FieldIdentifier.VULNERABILITY_SEVERITY, vuln.getSeverity(), FloatPoint.class, false),
        field(FieldIdentifier.VULNERABILITY_SEVERITY, vuln.getSeverity(), StoredField.class, true),
        field(FieldIdentifier.VULNERABILITY_STATUS, vuln.getStatus().getName(), TextField.class, true),
        field(FieldIdentifier.VULNERABILITY_DESCRIPTION, vulnDescription, TextField.class, true),
        field(FieldIdentifier.POLICY_EVALUATION_STAGE, StageTypes.BUILD.getId(), TextField.class, true),
        field(FieldIdentifier.REPORT_ID, reportId, TextField.class, true),
        field(FieldIdentifier.APPLICATION_ID, app.getId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_PUBLIC_ID, app.getPublicId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_NAME, app.getName(), TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_NAME, organization.getName(), TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_ID, organization.getId(), TextField.class, true),
        field(FieldIdentifier.PARENT_ORGANIZATION_ID, organization.getId(), TextField.class, true),
        field(FieldIdentifier.PARENT_ORGANIZATION_NAME, organization.getName(), TextField.class, true));
  }

  @Test
  public void testBuildDocument_ThirdPartyVulnerability_IaC() {
    String refId = "FG-1000";
    float severity = 7.5f;
    String iac = IdentificationSource.SONATYPE_IAC.getId();
    tempEntity.newThirdPartyVulnerability(refId, severity, iac);

    Organization org = tempEntity.newOrganization("test");
    Application app = tempEntity.newApplicationWithParent(org);
    String reportId = "report-id";
    ComponentIdentifier componentId = ComponentIdentifier.createIacCoordinates("namespace", "name", "1");
    Component component = new Component(componentId);
    component.setHash("01234567890123456789");
    SecurityVulnerability vuln = new SecurityVulnerability(iac, refId, severity);
    String vulnDescription = "FG-1000 description";
    verifyNoInteractions(vulnerabilityDescriptionFetcher);
    Document document =
        documentBuilderHelper.buildDocument(newIndexingContext(), org, app, StageTypes.BUILD, reportId, component, vuln,
            Arrays.asList(org));
    assertFields(document,
        field(FieldIdentifier.ITEM_TYPE, ItemType.SECURITY_VULNERABILITY.name(), TextField.class, true),
        field(FieldIdentifier.COMPONENT_HASH, component.getHash(), TextField.class, true),
        field(FieldIdentifier.COMPONENT_FORMAT, componentId.getFormat(), TextField.class, true),
        field(FieldIdentifier.COMPONENT_COORDINATE + "Version", componentId.get(ComponentIdentifier.VERSION),
            TextField.class, true),
        field(FieldIdentifier.COMPONENT_COORDINATE + "Name", componentId.get(ComponentIdentifier.IAC_NAME),
            TextField.class, true),
        field(FieldIdentifier.COMPONENT_COORDINATE + "Namespace", componentId.get(ComponentIdentifier.IAC_NAMESPACE),
            TextField.class, true),
        field(FieldIdentifier.COMPONENT_NAME, component.getDisplayNameFromIdentifier(), TextField.class, true),
        field(FieldIdentifier.VULNERABILITY_ID, vuln.getRefId(), TextField.class, true),
        field(FieldIdentifier.VULNERABILITY_SEVERITY, vuln.getSeverity(), FloatPoint.class, false),
        field(FieldIdentifier.VULNERABILITY_SEVERITY, vuln.getSeverity(), StoredField.class, true),
        field(FieldIdentifier.VULNERABILITY_STATUS, vuln.getStatus().getName(), TextField.class, true),
        field(FieldIdentifier.VULNERABILITY_DESCRIPTION, vulnDescription, TextField.class, true),
        field(FieldIdentifier.POLICY_EVALUATION_STAGE, StageTypes.BUILD.getId(), TextField.class, true),
        field(FieldIdentifier.REPORT_ID, reportId, TextField.class, true),
        field(FieldIdentifier.APPLICATION_ID, app.getId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_PUBLIC_ID, app.getPublicId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_NAME, app.getName(), TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_NAME, org.getName(), TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_ID, org.getId(), TextField.class, true),
        field(FieldIdentifier.PARENT_ORGANIZATION_NAME, org.getName(), TextField.class, true),
        field(FieldIdentifier.PARENT_ORGANIZATION_ID, org.getId(), TextField.class, true));
  }

  @Test
  public void testBuildDocument_ThirdPartyVulnerability_NexusContainer() {
    String refId = "Container-1000";
    float severity = 7.5f;
    String sonatypeContainer = "Sonatype-C";
    tempEntity.newThirdPartyVulnerability(refId, severity, sonatypeContainer);

    Organization org = tempEntity.newOrganization("test");
    Application app = tempEntity.newApplicationWithParent(org);
    String reportId = "report-id";
    ComponentIdentifier componentId = ComponentIdentifier.createContainerCoordinates("namespace", "name", "1");
    Component component = new Component(componentId);
    component.setHash("01234567890123456789");
    SecurityVulnerability vuln = new SecurityVulnerability(sonatypeContainer, refId, severity);
    String vulnDescription = "Container-1000 description";
    verifyNoInteractions(vulnerabilityDescriptionFetcher);
    assertFields(
        documentBuilderHelper.buildDocument(newIndexingContext(), org, app, StageTypes.BUILD, reportId, component, vuln,
            Arrays.asList(org)),
        field(FieldIdentifier.ITEM_TYPE, ItemType.SECURITY_VULNERABILITY.name(), TextField.class, true),
        field(FieldIdentifier.COMPONENT_HASH, component.getHash(), TextField.class, true),
        field(FieldIdentifier.COMPONENT_FORMAT, componentId.getFormat(), TextField.class, true),
        field(FieldIdentifier.COMPONENT_COORDINATE + "Version", componentId.get(ComponentIdentifier.VERSION),
            TextField.class, true),
        field(FieldIdentifier.COMPONENT_COORDINATE + "Name", componentId.get(ComponentIdentifier.CONTAINER_NAME),
            TextField.class, true),
        field(FieldIdentifier.COMPONENT_COORDINATE + "Namespace",
            componentId.get(ComponentIdentifier.CONTAINER_NAMESPACE), TextField.class, true),
        field(FieldIdentifier.COMPONENT_NAME, component.getDisplayNameFromIdentifier(), TextField.class, true),
        field(FieldIdentifier.VULNERABILITY_ID, vuln.getRefId(), TextField.class, true),
        field(FieldIdentifier.VULNERABILITY_SEVERITY, vuln.getSeverity(), FloatPoint.class, false),
        field(FieldIdentifier.VULNERABILITY_SEVERITY, vuln.getSeverity(), StoredField.class, true),
        field(FieldIdentifier.VULNERABILITY_STATUS, vuln.getStatus().getName(), TextField.class, true),
        field(FieldIdentifier.VULNERABILITY_DESCRIPTION, vulnDescription, TextField.class, true),
        field(FieldIdentifier.POLICY_EVALUATION_STAGE, StageTypes.BUILD.getId(), TextField.class, true),
        field(FieldIdentifier.REPORT_ID, reportId, TextField.class, true),
        field(FieldIdentifier.APPLICATION_ID, app.getId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_PUBLIC_ID, app.getPublicId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_NAME, app.getName(), TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_NAME, org.getName(), TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_ID, org.getId(), TextField.class, true),
        field(FieldIdentifier.PARENT_ORGANIZATION_NAME, org.getName(), TextField.class, true),
        field(FieldIdentifier.PARENT_ORGANIZATION_ID, org.getId(), TextField.class, true));
  }

  @Test
  public void testBuildDocument_SBOM_NonVulnerableComponent_NullPurl() {
    Organization rootOrganization = organizationDAO.getByIdNotNull(Organization.ROOT_ORGANIZATION_ID);
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile("bom.xml");
    ThirdPartySbomMetadata thirdPartySbomMetadata = tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(),
        application.getId(), PENDING, "bom.xml");
    ThirdPartyFileCoordinate thirdPartyFileCoordinate = tempEntity.newThirdPartyFileCoordinate(thirdPartyFile,
        "someSource", "someFormat", "someName", "someVersion", "someHash", null);

    assertFields(documentBuilderHelper.buildDocument(organization, application, thirdPartySbomMetadata,
            thirdPartyFileCoordinate, Collections.singletonList(rootOrganization)),
        field(FieldIdentifier.ITEM_TYPE, ItemType.NON_VULNERABLE_COMPONENT.name(), TextField.class, true),
        field(FieldIdentifier.COMPONENT_FORMAT, "someformat", TextField.class, true),
        field(FieldIdentifier.COMPONENT_COORDINATE + "Name", "someName", TextField.class, true),
        field(FieldIdentifier.COMPONENT_COORDINATE + "Version", "someVersion", TextField.class, true),
        field(FieldIdentifier.COMPONENT_NAME, "someName : someVersion", TextField.class, true),
        field(FieldIdentifier.APPLICATION_ID, application.getId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_PUBLIC_ID, application.getPublicId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_NAME, application.getName(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_VERSION, thirdPartySbomMetadata.getSbomVersion(), TextField.class, true),
        field(FieldIdentifier.SBOM_SPECIFICATION, thirdPartySbomMetadata.getSpec(), TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_ID, organization.getId(), TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_NAME, organization.getName(), TextField.class, true),
        field(FieldIdentifier.COMPONENT_HASH, thirdPartyFileCoordinate.getHash(), TextField.class, true),
        field(FieldIdentifier.PARENT_ORGANIZATION_NAME, rootOrganization.getName(), TextField.class, true),
        field(FieldIdentifier.PARENT_ORGANIZATION_ID, rootOrganization.getId(), TextField.class, true));
  }

  @Test
  public void testBuildDocument_SBOM_NonVulnerableComponent_InvalidPurl() {
    Organization rootOrganization = organizationDAO.getByIdNotNull(Organization.ROOT_ORGANIZATION_ID);
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile("bom.xml");
    ThirdPartySbomMetadata thirdPartySbomMetadata = tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(),
        application.getId(), PENDING, "bom.xml");
    ThirdPartyFileCoordinate thirdPartyFileCoordinate = tempEntity.newThirdPartyFileCoordinate(thirdPartyFile,
        "someSource", "someFormat", "someName", "someVersion", "someHash", "invalid");

    assertFields(documentBuilderHelper.buildDocument(organization, application, thirdPartySbomMetadata,
            thirdPartyFileCoordinate, Collections.singletonList(rootOrganization)),
        field(FieldIdentifier.ITEM_TYPE, ItemType.NON_VULNERABLE_COMPONENT.name(), TextField.class, true),
        field(FieldIdentifier.COMPONENT_FORMAT, "someformat", TextField.class, true),
        field(FieldIdentifier.COMPONENT_COORDINATE + "Name", "someName", TextField.class, true),
        field(FieldIdentifier.COMPONENT_COORDINATE + "Version", "someVersion", TextField.class, true),
        field(FieldIdentifier.COMPONENT_NAME, "someName : someVersion", TextField.class, true),
        field(FieldIdentifier.APPLICATION_ID, application.getId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_PUBLIC_ID, application.getPublicId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_NAME, application.getName(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_VERSION, thirdPartySbomMetadata.getSbomVersion(), TextField.class, true),
        field(FieldIdentifier.SBOM_SPECIFICATION, thirdPartySbomMetadata.getSpec(), TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_ID, organization.getId(), TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_NAME, organization.getName(), TextField.class, true),
        field(FieldIdentifier.COMPONENT_HASH, thirdPartyFileCoordinate.getHash(), TextField.class, true),
        field(FieldIdentifier.PARENT_ORGANIZATION_NAME, rootOrganization.getName(), TextField.class, true),
        field(FieldIdentifier.PARENT_ORGANIZATION_ID, rootOrganization.getId(), TextField.class, true));
  }

  @Test
  public void testBuildDocument_SBOM_NonVulnerableComponent_ValidPurl() {
    Organization rootOrganization = organizationDAO.getByIdNotNull(Organization.ROOT_ORGANIZATION_ID);
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile("bom.xml");
    ThirdPartySbomMetadata thirdPartySbomMetadata = tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(),
        application.getId(), PENDING, "bom.xml");
    ThirdPartyFileCoordinate thirdPartyFileCoordinate = tempEntity.newThirdPartyFileCoordinate(thirdPartyFile,
        "someSource", "someFormat", "someName", "someVersion", "someHash", "pkg:maven/g/a@v?type=jar");

    assertFields(documentBuilderHelper.buildDocument(organization, application, thirdPartySbomMetadata,
            thirdPartyFileCoordinate, Collections.singletonList(rootOrganization)),
        field(FieldIdentifier.ITEM_TYPE, ItemType.NON_VULNERABLE_COMPONENT.name(), TextField.class, true),
        field(FieldIdentifier.COMPONENT_FORMAT, "maven", TextField.class, true),
        field(FieldIdentifier.COMPONENT_COORDINATE + "GroupId", "g", TextField.class, true),
        field(FieldIdentifier.COMPONENT_COORDINATE + "ArtifactId", "a", TextField.class, true),
        field(FieldIdentifier.COMPONENT_COORDINATE + "Version", "v", TextField.class, true),
        field(FieldIdentifier.COMPONENT_COORDINATE + "Extension", "jar", TextField.class, true),
        field(FieldIdentifier.COMPONENT_NAME, "g : a : v", TextField.class, true),
        field(FieldIdentifier.APPLICATION_ID, application.getId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_PUBLIC_ID, application.getPublicId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_NAME, application.getName(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_VERSION, thirdPartySbomMetadata.getSbomVersion(), TextField.class, true),
        field(FieldIdentifier.SBOM_SPECIFICATION, thirdPartySbomMetadata.getSpec(), TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_ID, organization.getId(), TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_NAME, organization.getName(), TextField.class, true),
        field(FieldIdentifier.COMPONENT_HASH, thirdPartyFileCoordinate.getHash(), TextField.class, true),
        field(FieldIdentifier.PARENT_ORGANIZATION_NAME, rootOrganization.getName(), TextField.class, true),
        field(FieldIdentifier.PARENT_ORGANIZATION_ID, rootOrganization.getId(), TextField.class, true));
  }

  @Test
  public void testBuildDocument_SBOM_SecurityVulnerability_NullPurl() {
    Organization rootOrganization = organizationDAO.getByIdNotNull(Organization.ROOT_ORGANIZATION_ID);
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile("bom.xml");
    ThirdPartySbomMetadata thirdPartySbomMetadata = tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(),
        application.getId(), PENDING, "bom.xml");
    ThirdPartyFileCoordinate thirdPartyFileCoordinate = tempEntity.newThirdPartyFileCoordinate(thirdPartyFile,
        "someSource", "someFormat", "someName", "someVersion", "someHash", null);
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate, "someRefId", "someDescription", "someLink",
            5.5f, "someFixedBy", "someVulSource", "someCvssVectorString", "someSevDesc", "someCwes", "aRMethod",
            "someRecommendations", "someAdvisories", "SBOM");

    assertFields(
        documentBuilderHelper.buildDocument(organization, application, thirdPartySbomMetadata,
            thirdPartyFileCoordinate, thirdPartyCoordinateSecurity, Collections.singletonList(rootOrganization)),
        field(FieldIdentifier.ITEM_TYPE, ItemType.SECURITY_VULNERABILITY.name(), TextField.class, true),
        field(FieldIdentifier.COMPONENT_FORMAT, "someformat", TextField.class, true),
        field(FieldIdentifier.COMPONENT_COORDINATE + "Name", "someName", TextField.class, true),
        field(FieldIdentifier.COMPONENT_COORDINATE + "Version", "someVersion", TextField.class, true),
        field(FieldIdentifier.COMPONENT_NAME, "someName : someVersion", TextField.class, true),
        field(FieldIdentifier.VULNERABILITY_ID, "someRefId", TextField.class, true),
        field(FieldIdentifier.VULNERABILITY_SEVERITY, 5.5f, FloatPoint.class, false),
        field(FieldIdentifier.VULNERABILITY_SEVERITY, 5.5f, StoredField.class, true),
        field(FieldIdentifier.VULNERABILITY_DESCRIPTION, "someDescription", TextField.class, true),
        field(FieldIdentifier.APPLICATION_ID, application.getId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_PUBLIC_ID, application.getPublicId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_NAME, application.getName(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_VERSION, thirdPartySbomMetadata.getSbomVersion(), TextField.class, true),
        field(FieldIdentifier.SBOM_SPECIFICATION, thirdPartySbomMetadata.getSpec(), TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_ID, organization.getId(), TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_NAME, organization.getName(), TextField.class, true),
        field(FieldIdentifier.COMPONENT_HASH, thirdPartyFileCoordinate.getHash(), TextField.class, true),
        field(FieldIdentifier.PARENT_ORGANIZATION_NAME, rootOrganization.getName(), TextField.class, true),
        field(FieldIdentifier.PARENT_ORGANIZATION_ID, rootOrganization.getId(), TextField.class, true));
  }

  @Test
  public void testBuildDocument_SBOM_SecurityVulnerability_InvalidPurl() {
    Organization rootOrganization = organizationDAO.getByIdNotNull(Organization.ROOT_ORGANIZATION_ID);
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile("bom.xml");
    ThirdPartySbomMetadata thirdPartySbomMetadata = tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(),
        application.getId(), PENDING, "bom.xml");
    ThirdPartyFileCoordinate thirdPartyFileCoordinate = tempEntity.newThirdPartyFileCoordinate(thirdPartyFile,
        "someSource", "someFormat", "someName", "someVersion", "someHash", "invalid");
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate, "someRefId", "someDescription", "someLink",
            5.5f, "someFixedBy", "someVulSource", "someCvssVectorString", "someSevDesc", "someCwes", "aRMethod",
            "someRecommendations", "someAdvisories", "SBOM");

    assertFields(
        documentBuilderHelper.buildDocument(organization, application, thirdPartySbomMetadata,
            thirdPartyFileCoordinate,
            thirdPartyCoordinateSecurity, Collections.singletonList(rootOrganization)),
        field(FieldIdentifier.ITEM_TYPE, ItemType.SECURITY_VULNERABILITY.name(), TextField.class, true),
        field(FieldIdentifier.COMPONENT_FORMAT, "someformat", TextField.class, true),
        field(FieldIdentifier.COMPONENT_COORDINATE + "Name", "someName", TextField.class, true),
        field(FieldIdentifier.COMPONENT_COORDINATE + "Version", "someVersion", TextField.class, true),
        field(FieldIdentifier.COMPONENT_NAME, "someName : someVersion", TextField.class, true),
        field(FieldIdentifier.VULNERABILITY_ID, "someRefId", TextField.class, true),
        field(FieldIdentifier.VULNERABILITY_SEVERITY, 5.5f, FloatPoint.class, false),
        field(FieldIdentifier.VULNERABILITY_SEVERITY, 5.5f, StoredField.class, true),
        field(FieldIdentifier.VULNERABILITY_DESCRIPTION, "someDescription", TextField.class, true),
        field(FieldIdentifier.APPLICATION_ID, application.getId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_PUBLIC_ID, application.getPublicId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_NAME, application.getName(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_VERSION, thirdPartySbomMetadata.getSbomVersion(), TextField.class, true),
        field(FieldIdentifier.SBOM_SPECIFICATION, thirdPartySbomMetadata.getSpec(), TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_ID, organization.getId(), TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_NAME, organization.getName(), TextField.class, true),
        field(FieldIdentifier.COMPONENT_HASH, thirdPartyFileCoordinate.getHash(), TextField.class, true),
        field(FieldIdentifier.PARENT_ORGANIZATION_NAME, rootOrganization.getName(), TextField.class, true),
        field(FieldIdentifier.PARENT_ORGANIZATION_ID, rootOrganization.getId(), TextField.class, true));
  }

  @Test
  public void testBuildDocument_SBOM_SecurityVulnerability_ValidPurl() {
    Organization rootOrganization = organizationDAO.getByIdNotNull(Organization.ROOT_ORGANIZATION_ID);
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile("bom.xml");
    ThirdPartySbomMetadata thirdPartySbomMetadata = tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(),
        application.getId(), PENDING, "bom.xml");
    ThirdPartyFileCoordinate thirdPartyFileCoordinate = tempEntity.newThirdPartyFileCoordinate(thirdPartyFile,
        "someSource", "someFormat", "someName", "someVersion", "someHash", "pkg:maven/g/a@v?type=jar");
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate, "someRefId", "someDescription", "someLink",
            5.5f, "someFixedBy", "someVulSource", "someCvssVectorString", "someSevDesc", "someCwes", "aRMethod",
            "someRecommendations", "someAdvisories", "SBOM");

    assertFields(
        documentBuilderHelper.buildDocument(organization, application, thirdPartySbomMetadata,
            thirdPartyFileCoordinate,
            thirdPartyCoordinateSecurity, Collections.singletonList(rootOrganization)),
        field(FieldIdentifier.ITEM_TYPE, ItemType.SECURITY_VULNERABILITY.name(), TextField.class, true),
        field(FieldIdentifier.COMPONENT_FORMAT, "maven", TextField.class, true),
        field(FieldIdentifier.COMPONENT_COORDINATE + "GroupId", "g", TextField.class, true),
        field(FieldIdentifier.COMPONENT_COORDINATE + "ArtifactId", "a", TextField.class, true),
        field(FieldIdentifier.COMPONENT_COORDINATE + "Version", "v", TextField.class, true),
        field(FieldIdentifier.COMPONENT_COORDINATE + "Extension", "jar", TextField.class, true),
        field(FieldIdentifier.COMPONENT_NAME, "g : a : v", TextField.class, true),
        field(FieldIdentifier.VULNERABILITY_ID, "someRefId", TextField.class, true),
        field(FieldIdentifier.VULNERABILITY_SEVERITY, 5.5f, FloatPoint.class, false),
        field(FieldIdentifier.VULNERABILITY_SEVERITY, 5.5f, StoredField.class, true),
        field(FieldIdentifier.VULNERABILITY_DESCRIPTION, "someDescription", TextField.class, true),
        field(FieldIdentifier.APPLICATION_ID, application.getId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_PUBLIC_ID, application.getPublicId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_NAME, application.getName(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_VERSION, thirdPartySbomMetadata.getSbomVersion(), TextField.class, true),
        field(FieldIdentifier.SBOM_SPECIFICATION, thirdPartySbomMetadata.getSpec(), TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_ID, organization.getId(), TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_NAME, organization.getName(), TextField.class, true),
        field(FieldIdentifier.COMPONENT_HASH, thirdPartyFileCoordinate.getHash(), TextField.class, true),
        field(FieldIdentifier.PARENT_ORGANIZATION_NAME, rootOrganization.getName(), TextField.class, true),
        field(FieldIdentifier.PARENT_ORGANIZATION_ID, rootOrganization.getId(), TextField.class, true));
  }

  @Test
  public void testBuildDocument_SBOM_SecurityVulnerability_NullDescription() {
    Organization rootOrganization = organizationDAO.getByIdNotNull(Organization.ROOT_ORGANIZATION_ID);
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile("bom.xml");
    ThirdPartySbomMetadata thirdPartySbomMetadata = tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(),
        application.getId(), PENDING, "bom.xml");
    ThirdPartyFileCoordinate thirdPartyFileCoordinate = tempEntity.newThirdPartyFileCoordinate(thirdPartyFile,
        "someSource", "someFormat", "someName", "someVersion", "someHash", "pkg:maven/g/a@v?type=jar");
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate, "someRefId", null, "someLink",
            5.5f, "someFixedBy", "someVulSource", "someCvssVectorString", "someSevDesc", "someCwes", "aRMethod",
            "someRecommendations", "someAdvisories", "SBOM");

    assertFields(
        documentBuilderHelper.buildDocument(organization, application, thirdPartySbomMetadata,
            thirdPartyFileCoordinate,
            thirdPartyCoordinateSecurity, Collections.singletonList(rootOrganization)),
        field(FieldIdentifier.ITEM_TYPE, ItemType.SECURITY_VULNERABILITY.name(), TextField.class, true),
        field(FieldIdentifier.COMPONENT_FORMAT, "maven", TextField.class, true),
        field(FieldIdentifier.COMPONENT_COORDINATE + "GroupId", "g", TextField.class, true),
        field(FieldIdentifier.COMPONENT_COORDINATE + "ArtifactId", "a", TextField.class, true),
        field(FieldIdentifier.COMPONENT_COORDINATE + "Version", "v", TextField.class, true),
        field(FieldIdentifier.COMPONENT_COORDINATE + "Extension", "jar", TextField.class, true),
        field(FieldIdentifier.COMPONENT_NAME, "g : a : v", TextField.class, true),
        field(FieldIdentifier.VULNERABILITY_ID, "someRefId", TextField.class, true),
        field(FieldIdentifier.VULNERABILITY_SEVERITY, 5.5f, FloatPoint.class, false),
        field(FieldIdentifier.VULNERABILITY_SEVERITY, 5.5f, StoredField.class, true),
        field(FieldIdentifier.APPLICATION_ID, application.getId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_PUBLIC_ID, application.getPublicId(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_NAME, application.getName(), TextField.class, true),
        field(FieldIdentifier.APPLICATION_VERSION, thirdPartySbomMetadata.getSbomVersion(), TextField.class, true),
        field(FieldIdentifier.SBOM_SPECIFICATION, thirdPartySbomMetadata.getSpec(), TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_ID, organization.getId(), TextField.class, true),
        field(FieldIdentifier.ORGANIZATION_NAME, organization.getName(), TextField.class, true),
        field(FieldIdentifier.COMPONENT_HASH, thirdPartyFileCoordinate.getHash(), TextField.class, true),
        field(FieldIdentifier.PARENT_ORGANIZATION_NAME, rootOrganization.getName(), TextField.class, true),
        field(FieldIdentifier.PARENT_ORGANIZATION_ID, rootOrganization.getId(), TextField.class, true));
  }

  @Test
  public void testPopulateIndex_Telemetry() throws Exception {
    long start = System.currentTimeMillis();
    luceneSearchIndexClient.populateIndex();
    long duration = (System.currentTimeMillis() - start) / 1000;
    long size;
    try (Stream<Path> files = Files.walk(work.getSearchIndexDir().toPath().getParent())) {
      size = files.filter(Files::isRegularFile).mapToLong(file -> file.toFile().length()).sum();
    }

    ArgumentCaptor<TelemetryData> telemetryDataCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock).send(telemetryDataCaptor.capture());
    TelemetryData telemetryData = telemetryDataCaptor.getValue();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.ADVANCED_SEARCH_INDEXING);
    assertThat(telemetryData.getAttributes()).containsEntry(SearchIndexClient.SEARCH_INDEX_REINDEX, true);
    assertThat((Long) telemetryData.getAttributes().get(SearchIndexClient.SEARCH_INDEX_DURATION_SECONDS))
        .isGreaterThanOrEqualTo(0).isLessThanOrEqualTo(duration);
    assertThat((Long) telemetryData.getAttributes().get(SearchIndexClient.SEARCH_INDEX_SIZE_BYTES)).isEqualTo(size);
  }

  @Test
  public void testIsChangeSpecificError_ParseException() {
    Exception e = new IOException(new ParseException("Parse error"));
    assertThat(luceneSearchIndexClient.isChangeSpecificError(e)).isTrue();
  }

  @Test
  public void testIsChangeSpecificError_IllegalArgumentException() {
    Exception e = new IOException(new IllegalArgumentException("Invalid field"));
    assertThat(luceneSearchIndexClient.isChangeSpecificError(e)).isTrue();
  }

  @Test
  public void testIsChangeSpecificError_NullPointerException() {
    Exception e = new IOException(new NullPointerException("Null field"));
    assertThat(luceneSearchIndexClient.isChangeSpecificError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_FileSystemException() {
    Exception e = new FileSystemException("Cannot access file");
    assertThat(luceneSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_FileNotFoundException() {
    Exception e = new FileNotFoundException("Index file not found");
    assertThat(luceneSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_CorruptIndexException() {
    Exception e = new CorruptIndexException("Index is corrupt", "resource");
    assertThat(luceneSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_LockObtainFailedException() {
    Exception e = new LockObtainFailedException("Cannot obtain lock");
    assertThat(luceneSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_AlreadyClosedException() {
    Exception e = new AlreadyClosedException("Index already closed");
    assertThat(luceneSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_TimeoutException() {
    Exception e = new IOException(new TimeoutException("Operation timed out"));
    assertThat(luceneSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_NoSpaceLeft() {
    Exception e = new IOException("No space left on device");
    assertThat(luceneSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_AccessDenied() {
    Exception e = new IOException("Access denied to index directory");
    assertThat(luceneSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_TooManyOpenFiles() {
    Exception e = new IOException("Too many open files");
    assertThat(luceneSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_IndexFormatTooNewException() {
    Exception e = new IndexFormatTooNewException("resource", 10, 5, 8);
    assertThat(luceneSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_IndexFormatTooOldException() {
    Exception e = new IndexFormatTooOldException("resource", 3, 5, 8);
    assertThat(luceneSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_LockReleaseFailedException() {
    Exception e = new LockReleaseFailedException("Failed to release lock");
    assertThat(luceneSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_PrepareCommitFailException() {
    Exception e = new PrepareCommitFailException(new IOException("Prepare failed"), null);
    assertThat(luceneSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_CommitFailException() {
    Exception e = new CommitFailException(new IOException("Commit failed"), null);
    assertThat(luceneSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_MergeException() {
    Exception e = new MergeException("Merge failed");
    assertThat(luceneSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_ThreadInterruptedException() {
    Exception e = new ThreadInterruptedException(new InterruptedException("Thread interrupted"));
    assertThat(luceneSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_CheckIndexException() {
    Exception e = new CheckIndexException("Index check failed");
    assertThat(luceneSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_NotEnoughSpace() {
    Exception e = new IOException("Not enough space on device");
    assertThat(luceneSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_AccessIsDenied() {
    Exception e = new IOException("Access is denied");
    assertThat(luceneSearchIndexClient.isSystemicError(e)).isTrue();
  }

  @Test
  public void testIsSystemicError_PermissionDenied() {
    Exception e = new IOException("Permission denied");
    assertThat(luceneSearchIndexClient.isSystemicError(e)).isTrue();
  }
}
