/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.IOException;
import java.util.List;

import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.hds.AugmentUtil;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonStore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @since 1.13.0
 */
public class SecurityAuditMigratorTest
  extends AbstractAuditMigratorTest
{
  @Override
  protected void verifyAuditHistory(final JsonStore auditStore, final boolean isOrg) throws IOException {
    ArrayNode aaData = (ArrayNode) auditStore.history(null, getAuditFileName()).get("aaData");
    for (JsonNode auditJson : aaData) {
      JsonNode componentIdentifier = auditJson.get("componentIdentifier");
      assertThat(componentIdentifier.get("format").textValue(), is("maven"));

      JsonNode coordinates = componentIdentifier.get("coordinates");
      assertThat(coordinates.get("groupId").textValue(), is(ANTLR_COMPONENT.get(ComponentIdentifier.MAVEN_GROUP_ID)));
      assertThat(coordinates.get("artifactId").textValue(),
          is(ANTLR_COMPONENT.get(ComponentIdentifier.MAVEN_ARTIFACT_ID)));
      assertThat(coordinates.get("version").textValue(), is(ANTLR_COMPONENT.get(ComponentIdentifier.VERSION)));
    }

    // Security Augmentation only exists on the application level
    if (!isOrg) {
      verifySecurityAugmentation();
    }
  }

  private void verifySecurityAugmentation() throws IOException {
    List<SecurityVulnerability> securityVulnerabilities = Lists.asList(
        new SecurityVulnerability("98703", "osvdb", 5f),
        new SecurityVulnerability("CVE-2013-2186", "cve", 8f),
        new SecurityVulnerability[0]
    );

    ArrayNode securityData = AugmentUtil
        .getSVData(getInsightWork(), applicationId, ANTLR_COMPONENT, securityVulnerabilities);

    for (JsonNode securityNode : securityData) {
      assertThat(securityNode.get("status").textValue(), is("Not Applicable"));
      assertThat(securityNode.get("comment").textValue(),
          is(securityNode.get("reference").textValue() + " is not applicable."));
    }
  }

  @Override
  protected AbstractAuditGAVMigrator getAuditMigrator(final InsightWork insightWork) {
    return new SecurityAuditGAVMigrator(insightWork);
  }

  @Override
  protected String getTestFolder() {
    return this.getClass().getSimpleName();
  }

  @Override
  protected String getDifferentAuditFileName() {
    return "license.json";
  }
}
