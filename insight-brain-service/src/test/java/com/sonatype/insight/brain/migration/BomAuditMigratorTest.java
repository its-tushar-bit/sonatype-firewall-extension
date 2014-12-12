/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.IOException;

import com.sonatype.insight.brain.dto.audit.BomAudit;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonStore;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @since 1.13.0
 */
public class BomAuditMigratorTest
    extends AbstractAuditMigratorTest
{

  @Override
  protected String getAuditFileName() {
    return "bom.json";
  }

  @Override
  protected void verifyAuditHistory(final JsonStore auditStore, final boolean isOrg) throws IOException {
    ArrayNode aaData = (ArrayNode) auditStore.history(null, getAuditFileName()).get(
        "aaData");
    for (JsonNode auditJson : aaData) {
      BomAudit bomAudit = JsonUtils.asPojo(auditJson, BomAudit.class);
      assertThat(bomAudit.getComponentIdentifier(), is(ANTLR_COMPONENT));
      assertThat(bomAudit.isModified(), is(true));
    }
  }

  @Override
  protected AbstractAuditGAVMigrator getAuditMigrator(final InsightWork insightWork) {
    return new BomAuditGAVMigrator(insightWork);
  }

  @Override
  protected String getTestFolder() {
    return this.getClass().getSimpleName();
  }
}
