/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.io.File;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.report.HostedRepositoryComponentReportResource;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-stack test for the HRC audit-log endpoint. The unit test
 * {@code HostedRepositoryComponentReportResourceTest#auditLog_delegatesToAuditLogReaderWithResolvedHrc}
 * verifies the resource forwards to {@link AuditLogReader}; this test exercises
 * {@code AuditLogReader.readAuditLog(hrc, ...)} against real audit files planted at ancestor
 * levels of the HRC hierarchy (Repository, RepositoryManager) to confirm those entries merge
 * into the response — the same guarantee that
 * {@code ReportResourceTest#testAuditLog_includesOrgScopedEntries} provides for the app-side path.
 */
@IqH2Test
class IqH2HostedRepositoryComponentReportResourceAuditLogTest
{
  private IqTestContext ctx;

  @BeforeEach
  void enableHostedRepositoryEvaluation() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);
  }

  @AfterEach
  void disableHostedRepositoryEvaluation() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);
  }

  @Test
  void auditLog_mergesEntriesFromRepositoryAndRepositoryManagerAncestors() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager);
    HostedRepositoryComponent hrc = ctx.tempEntity().newHostedRepositoryComponent(repository);

    InsightWork work = ctx.lookup(InsightWork.class);
    File repoAuditDir = work.getAuditDir(repository.getId());
    File managerAuditDir = work.getAuditDir(repositoryManager.getId());
    boolean createdRepoDir = repoAuditDir.mkdirs();
    boolean createdManagerDir = managerAuditDir.mkdirs();

    File repoLicenseFile = new File(repoAuditDir, "licenses.json");
    File managerLicenseFile = new File(managerAuditDir, "licenses.json");

    try {
      JsonUtils.write(repoLicenseFile, licenseAuditEntry("repository-scoped override"));
      JsonUtils.write(managerLicenseFile, licenseAuditEntry("repository-manager-scoped override"));

      HttpResponse response = ctx.restRequest()
          .path(HostedRepositoryComponentReportResource.RESOURCE_PATH)
          .path("{scanId}/auditLog/{path}")
          .parameter(hrc.getId(), "any-scan-id", "licenses.json")
          .get();
      ctx.assertResponseStatus(200, response);

      JsonNode body = response.getBody(JsonNode.class);
      assertThat(body).isNotNull();
      ArrayNode entries = (ArrayNode) body.get("aaData");
      assertThat(entries).as("aaData in HRC audit-log response").isNotNull();

      boolean foundRepositoryEntry = false;
      boolean foundManagerEntry = false;
      for (JsonNode entry : entries) {
        String comment = entry.path("comment").asText();
        if ("repository-scoped override".equals(comment)) {
          foundRepositoryEntry = true;
        }
        else if ("repository-manager-scoped override".equals(comment)) {
          foundManagerEntry = true;
        }
      }
      assertThat(foundRepositoryEntry)
          .as("Repository-scoped audit entry should appear in HRC audit log")
          .isTrue();
      assertThat(foundManagerEntry)
          .as("RepositoryManager-scoped audit entry should appear in HRC audit log")
          .isTrue();
    }
    finally {
      repoLicenseFile.delete();
      managerLicenseFile.delete();
      if (createdRepoDir) {
        repoAuditDir.delete();
      }
      if (createdManagerDir) {
        managerAuditDir.delete();
      }
    }
  }

  private static ArrayNode licenseAuditEntry(String comment) {
    ArrayNode logArray = JsonUtils.arrayNode(null);
    ObjectNode auditEntry = logArray.addObject();
    auditEntry.put("time", System.currentTimeMillis());
    auditEntry.put("user", "admin");
    auditEntry.put("ip", "127.0.0.1");
    auditEntry.putNull("where");
    ArrayNode dataArray = auditEntry.putArray("data");
    ObjectNode dataEntry = dataArray.addObject();
    ObjectNode ci = dataEntry.putObject("componentIdentifier");
    ci.put("format", "maven");
    ObjectNode coords = ci.putObject("coordinates");
    coords.put("groupId", "org.test");
    coords.put("artifactId", "test-artifact");
    coords.put("version", "1.0.0");
    coords.put("classifier", "");
    coords.put("extension", "jar");
    dataEntry.put("status", "Overridden");
    dataEntry.put("comment", comment);
    return logArray;
  }
}
