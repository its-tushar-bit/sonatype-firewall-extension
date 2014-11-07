/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.dto.model.ide.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dto.audit.LicenseOverrideAudit;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonStore;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class LicenseOverrideAuditMigratorTest
{

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  private InsightWork insightWork;

  private LicenseOverrideAuditMigrator licenseOverrideAuditMigrator;

  private Application app;

  private JsonStore appAuditStore;

  private JsonStore orgAuditStore;

  private static final ComponentIdentifier ANTLR_COMPONENT = ComponentIdentifier
      .createMavenCoordinates("antlr", "antlr", "2.7.2");

  public void setup(String testDataDir) throws IOException {
    File sonatypeWork = temporaryFolder.newFolder();
    String tempFolderPath = sonatypeWork.getAbsolutePath();
    InsightConfig insightConfig = new InsightConfig();
    insightConfig.setSonatypeWork(tempFolderPath);
    insightWork = new InsightWork(insightConfig);
    licenseOverrideAuditMigrator = new LicenseOverrideAuditMigrator(insightWork);

    FileUtils.copyDirectory(new File("target/test-classes", testDataDir), sonatypeWork);
    Organization organization = tempEntity.newOrganizationWithSpecificId("org1");
    String name = this.getClass().getSimpleName();
    app = tempEntity.newApplicationWithSpecificId("app1", name, name, organization.getId());

    appAuditStore = JsonUtils.fileStore(insightWork.getAuditDir(app.getId()));
    orgAuditStore = JsonUtils.fileStore(insightWork.getAuditDir(organization.getId()));
  }

  @After
  public void verifyMarkerFile() {
    assertThat(insightWork.getAuditDir().exists(), is(true));
    assertThat(new File(insightWork.getAuditDir(), LicenseOverrideAuditMigrator.MARKER_FILE_NAME).exists(), is(true));
  }

  @Test
  public void testMissingAuditDir() throws Exception {
    setup("LicenseOverrideAuditMigratorTest/noAuditDir");
    assertThat(insightWork.getAuditDir().exists(), is(false));
    assertThat(new File(insightWork.getAuditDir(), LicenseOverrideAuditMigrator.MARKER_FILE_NAME).exists(), is(false));
    assertThat(licenseOverrideAuditMigrator.migrate(), is(0));
  }

  @Test
  public void testNoAudits() throws Exception {
    setup("LicenseOverrideAuditMigratorTest/noAudits");
    assertThat(licenseOverrideAuditMigrator.migrate(), is(0));
    assertThat(insightWork.getAuditDir().exists(), is(true));
    assertThat(insightWork.getAuditDir(app.getId()).exists(), is(false));
  }

  @Test
  public void testAppWithAudits() throws Exception {
    setup("LicenseOverrideAuditMigratorTest/appAudits");
    assertThat(licenseOverrideAuditMigrator.migrate(), is(1));
    verifyAuditHistory(appAuditStore);
    assertThat(orgAuditStore.history(null, "licenses.json"), is(nullValue()));
  }

  @Test
  public void testOrgWithAudits() throws Exception {
    setup("LicenseOverrideAuditMigratorTest/orgAudits");
    assertThat(licenseOverrideAuditMigrator.migrate(), is(1));
    verifyAuditHistory(orgAuditStore);
    assertThat(appAuditStore.history(null, "licenses.json"), is(nullValue()));
  }

  @Test
  public void testDeletedAppsAndOrgs() throws Exception {
    setup("LicenseOverrideAuditMigratorTest/deletedAppAndOrg");
    assertThat(licenseOverrideAuditMigrator.migrate(), is(0));
  }

  @Test
  public void testNoLicenseOverrides() throws Exception {
    setup("LicenseOverrideAuditMigratorTest/noLicenseOverrideAudits");
    assertThat(licenseOverrideAuditMigrator.migrate(), is(0));
    assertThat(appAuditStore.history(null, "licenses.json"), is(nullValue()));
    assertThat(orgAuditStore.history(null, "licenses.json"), is(nullValue()));
    assertThat(appAuditStore.history(null, "security.json"), is(notNullValue()));
    assertThat(orgAuditStore.history(null, "security.json"), is(notNullValue()));
  }

  @Test
  public void testMarkerFilePresent() throws Exception {
    setup("LicenseOverrideAuditMigratorTest/markerFilePresent");
    assertThat(new File(insightWork.getAuditDir(), LicenseOverrideAuditMigrator.MARKER_FILE_NAME).exists(), is(true));
    assertThat(licenseOverrideAuditMigrator.migrate(), is(0));
    ArrayNode aaData = (ArrayNode) appAuditStore.history(null, "licenses.json").get("aaData");
    for (JsonNode legacyLicenseOverrideAuditJson : aaData) {
      //file should not be migrated as marker file indicates it already has been
      assertThat(legacyLicenseOverrideAuditJson.get(ComponentIdentifierAdapter.COMPONENT_IDENTIFIER), is(nullValue()));
    }
  }

  private void verifyAuditHistory(JsonStore auditStore) throws IOException {
    ArrayNode aaData = (ArrayNode) auditStore.history(null, "licenses.json").get("aaData");
    List<LicenseOverrideAudit> audits = new ArrayList<>();
    for (JsonNode licenseOverrideAuditJson : aaData) {
      LicenseOverrideAudit licenseOverrideAudit = JsonUtils
          .asPojo(licenseOverrideAuditJson, LicenseOverrideAudit.class);
      assertThat(licenseOverrideAudit.getComponentIdentifier(), is(ANTLR_COMPONENT));
      assertThat(licenseOverrideAudit.getOverriddenLicenses(), hasSize(1));
      audits.add(licenseOverrideAudit);
    }
    assertThat(audits.get(0).getOverriddenLicenses(), contains("AFL-1.2"));
    assertThat(audits.get(1).getOverriddenLicenses(), contains("AAL"));
  }
}
