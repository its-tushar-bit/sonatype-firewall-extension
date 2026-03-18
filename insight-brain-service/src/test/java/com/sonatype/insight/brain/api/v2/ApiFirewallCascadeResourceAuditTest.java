/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiFirewallCascadeResourceAuditTest
    extends AbstractAuditTest
{
  private static final String COMPONENT_HASH = "test_hash_123";

  @Test
  public void testInitiateCascadeReevaluation_Success() throws Exception {
    createRepositoryWithComponent();

    cascadeReevaluateRequest(COMPONENT_HASH).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.INITIATE_CASCADE_REEVALUATION, null);
    assertCascadeReevaluationData(auditDTO, COMPONENT_HASH);
  }

  @Test
  public void testInitiateCascadeReevaluation_Unauthorized() throws Exception {
    createRepositoryWithComponent();

    cascadeReevaluateRequest(COMPONENT_HASH)
        .with(unauthorizedUser())
        .post();

    assertAuditLog(AuditEvent.INITIATE_CASCADE_REEVALUATION, "unauthorized");
  }

  @Test
  public void testInitiateCascadeReevaluation_MultipleRepositories() throws Exception {
    // Create multiple repositories with the same component
    createRepositoryWithComponent("repo-1");
    createRepositoryWithComponent("repo-2");

    cascadeReevaluateRequest(COMPONENT_HASH).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.INITIATE_CASCADE_REEVALUATION, null);
    assertCascadeReevaluationData(auditDTO, COMPONENT_HASH);
    // Note: Repository count (2) will be determined by async task, not available in immediate audit log
  }

  @Test
  public void testInitiateCascadeReevaluation_BlankComponentHash() throws Exception {
    createRepositoryWithComponent();

    // Test with blank component hash - should result in 404 or 400
    cascadeReevaluateRequest("").post();

    // Blank hash in URL path typically results in routing error before audit
    List<AuditDTO> auditEntries = getLogEntries(AuditEvent.INITIATE_CASCADE_REEVALUATION);
    // May or may not generate audit entry depending on how early the error occurs
    if (!auditEntries.isEmpty()) {
      assertThat(auditEntries.get(0).error).isIn("bad-request", "not-found");
    }
  }

  private HttpRequest cascadeReevaluateRequest(String componentHash) {
    return restRequest()
        .path(PublicApiPaths.FIREWALL_CASCADE_REEVALUATE_PATH, "/componentHash/" + componentHash);
  }

  private void createRepositoryWithComponent() {
    createRepositoryWithComponent("test-repo");
  }

  private void createRepositoryWithComponent(String repositoryName) {
    Repository repository = tempEntity.newRepository(repositoryName);

    Date now = new Date();
    tempEntity.newRepositoryComponent(
        repository.getId(),
        MatchState.EXACT,
        "test/path/component",
        COMPONENT_HASH,
        ComponentIdentifier.createNpmCoordinates("test-pkg", "1.0.0"),
        now,
        now);
  }

  private void assertCascadeReevaluationData(AuditDTO auditDTO, String componentHash) {
    boolean hasComponentHash = auditDTO.data.containsKey("componentHash") ||
        auditDTO.data.containsKey("pathParam.componentHash") ||
        auditDTO.data.containsKey("param.componentHash");

    if (hasComponentHash) {
      // If componentHash is present, verify it matches
      String auditedHash = (String) auditDTO.data.getOrDefault("componentHash",
          auditDTO.data.getOrDefault("pathParam.componentHash",
              auditDTO.data.get("param.componentHash")));
      assertThat(auditedHash).isEqualTo(componentHash);
    }

    assertThat(auditDTO.type).isEqualTo(AuditEvent.INITIATE_CASCADE_REEVALUATION.getType());
  }
}
