/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.IOException;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.json.store.JsonStore;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class NullHashModifiedMigratorTest
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  private File sonatypeWork;

  private InsightConfig insightConfig;

  private InsightWork insightWork;

  private NullHashModifiedMigrator nullHashModifiedMigrator;

  private Application app;

  @Before
  public void setup() throws IOException {
    sonatypeWork = temporaryFolder.newFolder();
    String tempFolderPath = sonatypeWork.getAbsolutePath();
    insightConfig = new InsightConfig();
    insightConfig.setSonatypeWork(tempFolderPath);
    insightWork = new InsightWork(insightConfig);
    nullHashModifiedMigrator = new NullHashModifiedMigrator(insightWork);

    app = tempEntity.newApplicationWithParent("NullHashModifiedMigratorTest");
    copyBom("/NullHashModifiedMigratorTest/app-bom.json", app.getId());
    copyBom("/NullHashModifiedMigratorTest/org-bom.json", app.getOrganizationId());
  }

  @Test
  public void testMigrator() throws Exception {
    JsonStore appAuditStore = JsonUtils.fileStore(insightWork.getAuditDir(app.getId()));
    JsonStore orgAuditStore = JsonUtils.fileStore(insightWork.getAuditDir(app.getOrganizationId()));
    ArrayNode bom = createBom();
    ArrayNode augmentedBom = appAuditStore.augment(bom, "bom.json");

    assertNotModified("tomcat", "tomcat-util", "5.5.23", augmentedBom); // null hash from app
    assertNotModified("org.apache.geronimo.framework", "geronimo-security", "2.2", augmentedBom); // null hash from org
    assertModified("org.apache.geronimo.framework", "geronimo-security", "2.1", augmentedBom); // hash from app

    nullHashModifiedMigrator.migrate();

    appAuditStore = JsonUtils.fileStore(insightWork.getAuditDir(app.getId()));
    bom = createBom();
    augmentedBom = orgAuditStore.augment(bom, "bom.json");
    augmentedBom = appAuditStore.augment(augmentedBom, "bom.json");

    assertModified("tomcat", "tomcat-util", "5.5.23", augmentedBom);
    assertModified("org.apache.geronimo.framework", "geronimo-security", "2.2", augmentedBom);
    assertModified("org.apache.geronimo.framework", "geronimo-security", "2.1", augmentedBom);
  }

  private ArrayNode createBom() {
    ArrayNode node = new ArrayNode(JsonNodeFactory.instance);
    node.add(createGav("tomcat", "tomcat-util", "5.5.23", "555d7549ef7ec13ce546"));
    node.add(createGav("org.apache.geronimo.framework", "geronimo-security", "2.1", "848d7549ef7ec13ce546"));
    node.add(createGav("org.apache.geronimo.framework", "geronimo-security", "2.2", "aaaa7549ef7ec13ce546"));
    return node;
  }

  private ObjectNode createGav(String groupId, String artifactId, String version, String hash) {
    ObjectNode node = new ObjectNode(JsonNodeFactory.instance);
    node.set("groupId", new TextNode(groupId));
    node.set("artifactId", new TextNode(artifactId));
    node.set("version", new TextNode(version));
    node.set("hash", new TextNode(hash));
    return node;
  }

  private static void assertModified(String groupId, String artifactId, String version, ArrayNode store) {
    Assert.assertTrue(isModified(groupId, artifactId, version, store));
  }

  private static void assertNotModified(String groupId, String artifactId, String version, ArrayNode store) {
    Assert.assertFalse(isModified(groupId, artifactId, version, store));
  }

  private static boolean isModified(String groupId, String artifactId, String version, ArrayNode nodes) {
    for (JsonNode node : nodes) {
      if (groupId.equals(node.get("groupId").asText()) && artifactId.equals(node.get("artifactId").asText())
          && version.equals(node.get("version").asText())) {
        return node.path("modified").asBoolean(false);
      }
    }
    throw new IllegalArgumentException("Unable to find " + groupId + ":" + artifactId + ":" + version);
  }

  private void copyBom(String source, String destinationId) throws IOException {
    File appAuditDir = insightWork.getAuditDir(destinationId);
    appAuditDir.mkdir();
    FileUtils.copyFile(new File(getClass().getResource(source).getFile()), new File(appAuditDir, "bom.json"));
  }
}
