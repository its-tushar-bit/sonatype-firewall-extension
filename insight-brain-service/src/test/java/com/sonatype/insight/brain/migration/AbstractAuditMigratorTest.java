/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.File;
import java.io.IOException;

import com.sonatype.clm.dto.model.ide.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonStore;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @since 1.13.0
 */
public abstract class AbstractAuditMigratorTest
{

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  private InsightWork insightWork;

  private Application app;

  private AbstractAuditGAVMigrator auditMigrator;

  protected JsonStore appAuditStore;

  protected JsonStore orgAuditStore;

  //common component randomly selected for testing
  protected static final ComponentIdentifier ANTLR_COMPONENT = ComponentIdentifier
      .createMavenCoordinates("antlr", "antlr", "2.7.2");

  public void setup(String testDataDir) throws IOException {
    File sonatypeWork = temporaryFolder.newFolder();
    String tempFolderPath = sonatypeWork.getAbsolutePath();
    InsightConfig insightConfig = new InsightConfig();
    insightConfig.setSonatypeWork(tempFolderPath);
    insightWork = new InsightWork(insightConfig);
    auditMigrator = getAuditMigrator(insightWork);
    FileUtils.copyDirectory(new File("target/test-classes", getTestFolder() + "/" + testDataDir), sonatypeWork);
    Organization organization = tempEntity.newOrganizationWithSpecificId("org1");
    String name = this.getClass().getSimpleName();
    app = tempEntity.newApplicationWithSpecificId("app1", name, name, organization.getId());

    appAuditStore = JsonUtils.fileStore(insightWork.getAuditDir(app.getId()));
    orgAuditStore = JsonUtils.fileStore(insightWork.getAuditDir(organization.getId()));
  }

  @Test
  public void testMissingAuditDir() throws Exception {
    setup("noAuditDir");
    assertThat(insightWork.getAuditDir().exists(), is(false));
    assertThat(new File(insightWork.getAuditDir(), auditMigrator.getMarkerFilename()).exists(), is(false));
    assertThat(auditMigrator.migrate(), is(0));
  }

  @Test
  public void testNoAudits() throws Exception {
    setup("noAudits");
    assertThat(auditMigrator.migrate(), is(0));
    assertThat(insightWork.getAuditDir().exists(), is(true));
    assertThat(insightWork.getAuditDir(app.getId()).exists(), is(false));
  }

  @Test
  public void testAppWithAudits() throws Exception {
    setup("appAudits");
    assertThat(auditMigrator.migrate(), is(1));
    verifyAuditHistory(appAuditStore);
    assertThat(orgAuditStore.history(null, auditMigrator.getAuditFileName()), is(nullValue()));
  }

  @Test
  public void testOrgWithAudits() throws Exception {
    setup("orgAudits");
    assertThat(auditMigrator.migrate(), is(1));
    verifyAuditHistory(orgAuditStore);
    assertThat(appAuditStore.history(null, auditMigrator.getAuditFileName()), is(nullValue()));
  }

  @Test
  public void testDeletedAppsAndOrgs() throws Exception {
    setup("deletedAppAndOrg");
    assertThat(auditMigrator.migrate(), is(0));
  }

  @Test
  public void testMarkerFilePresent() throws Exception {
    setup("markerFilePresent");
    assertThat(new File(insightWork.getAuditDir(), auditMigrator.getMarkerFilename()).exists(), is(true));
    assertThat(auditMigrator.migrate(), is(0));
    ArrayNode aaData = (ArrayNode) appAuditStore.history(null, auditMigrator.getAuditFileName()).get("aaData");
    for (JsonNode auditJson : aaData) {
      //file should not be migrated as marker file indicates it already has been
      assertThat(auditJson.get(ComponentIdentifierAdapter.COMPONENT_IDENTIFIER), is(nullValue()));
    }
  }

  @Test
  public void testNoOverrides() throws Exception {
    setup("noAuditsForThisMigrator");
    assertThat(auditMigrator.migrate(), is(0));
    assertThat(appAuditStore.history(null, auditMigrator.getAuditFileName()), is(nullValue()));
    assertThat(orgAuditStore.history(null, auditMigrator.getAuditFileName()), is(nullValue()));
    assertThat(appAuditStore.history(null, "security.json"), is(notNullValue()));
    assertThat(orgAuditStore.history(null, "security.json"), is(notNullValue()));
  }

  public InsightWork getInsightWork() {
    return insightWork;
  }

  protected String getAuditFileName() {
    return auditMigrator.getAuditFileName();
  }

  /**
   * Perform specific validations of the auditStore.
   */
  protected abstract void verifyAuditHistory(final JsonStore auditStore) throws IOException;

  /**
   * Return a new instance of the auditMigrator to test.
   */
  protected abstract AbstractAuditGAVMigrator getAuditMigrator(final InsightWork insightWork);

  /**
   * The name of the root test folder used to provide test data. Individual test scenarios should
   * be provided in nested folders here.
   */
  protected abstract String getTestFolder();
}
