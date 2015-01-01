/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.dto.audit.LicenseOverrideAudit;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonStore;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class LicenseOverrideAuditMigratorTest
    extends AbstractAuditMigratorTest
{

  @Override
  protected void verifyAuditHistory(JsonStore auditStore, final boolean isOrg) throws IOException {
    ArrayNode aaData = (ArrayNode) auditStore.history(null, getAuditFileName()).get(
        "aaData");
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

  @Override
  protected AbstractAuditGAVMigrator getAuditMigrator(final InsightWork insightWork) {
    return new LicenseOverrideAuditGAVMigrator(getInsightWork());
  }

  @Override
  protected String getTestFolder() {
    return this.getClass().getSimpleName();
  }
}
