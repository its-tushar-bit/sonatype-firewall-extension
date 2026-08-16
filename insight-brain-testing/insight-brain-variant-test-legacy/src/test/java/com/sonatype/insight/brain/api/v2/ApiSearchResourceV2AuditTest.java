/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.brain.variant.LegacyServerTest;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.junit.jupiter.api.Test;

@LegacyServerTest
public class ApiSearchResourceV2AuditTest
    extends AbstractAuditTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.SEARCH_RESOURCE_PATH_V2).query("stageId", Stage.ID_BUILD);
  }

  @Test
  public void testSearchComponent_ByHash() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    String hash = "1249e25aebb15358bedd";
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanId");
    tempEntity.newApplicationComponent(app.getId(), Stage.ID_BUILD, hash, null /* componentIdentifier */);
    tempEntity.newApplicationWithParent();
    mockReport("scanId", "/" + getClass().getSimpleName() + "/report");

    restRequest().query("hash", hash).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.SEARCH_COMPONENT_USES, null);
    assertCustomData(auditDTO, "componentHash", hash);
    assertCustomObject(auditDTO, "componentIdentifier", null);
    assertCustomData(auditDTO, "inspectedApplicationCount", 2);
    assertCustomData(auditDTO, "resultRecordCount", 1);
  }

  @Test
  public void testSearchComponent_ByComponentIdentifier() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanId");
    tempEntity.newApplicationComponent(app.getId(), Stage.ID_BUILD, "hash", componentIdentifier);
    tempEntity.newApplicationWithParent();
    mockReport("scanId", "/" + getClass().getSimpleName() + "/report");

    restRequest().query("componentIdentifier", componentIdentifier).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.SEARCH_COMPONENT_USES, null);
    assertCustomData(auditDTO, "componentHash", null);
    assertCustomObject(auditDTO, "componentIdentifier", componentIdentifier);
    assertCustomData(auditDTO, "inspectedApplicationCount", 2);
    assertCustomData(auditDTO, "resultRecordCount", 1);
  }

  @Test
  public void testSearchComponent_ByPackageUrl() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    String packageUrl = "pkg:maven/g/a@v";
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanId");
    ComponentIdentifier componentIdentifier = new PackageUrlIdentifier(packageUrl).toComponentIdentifier();
    tempEntity.newApplicationComponent(app.getId(), Stage.ID_BUILD, "hash",
        componentIdentifier);
    tempEntity.newApplicationWithParent();
    mockReport("scanId", "/" + getClass().getSimpleName() + "/report");

    restRequest().query("packageUrl", packageUrl).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.SEARCH_COMPONENT_USES, null);
    assertCustomData(auditDTO, "componentHash", null);
    assertCustomObject(auditDTO, "componentIdentifier", componentIdentifier);
    assertCustomData(auditDTO, "inspectedApplicationCount", 2);
    assertCustomData(auditDTO, "resultRecordCount", 1);
  }

  @Test
  public void testSearchComponent_ByHashAndComponentIdentifier() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    String hash = "1249e25aebb15358bedd";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanId");
    tempEntity.newApplicationComponent(app.getId(), Stage.ID_BUILD, hash, componentIdentifier);
    tempEntity.newApplicationWithParent();
    mockReport("scanId", "/" + getClass().getSimpleName() + "/report");

    restRequest().query("hash", hash).query("componentIdentifier", componentIdentifier).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.SEARCH_COMPONENT_USES, null);
    assertCustomData(auditDTO, "componentHash", hash);
    assertCustomObject(auditDTO, "componentIdentifier", componentIdentifier);
    assertCustomData(auditDTO, "inspectedApplicationCount", 2);
    assertCustomData(auditDTO, "resultRecordCount", 1);
  }

  @Test
  public void testSearchComponent_ByHashAndPackageUrl() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    String hash = "1249e25aebb15358bedd";
    String packageUrl = "pkg:maven/g/a@v";
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanId");
    ComponentIdentifier componentIdentifier = new PackageUrlIdentifier(packageUrl).toComponentIdentifier();
    tempEntity.newApplicationComponent(app.getId(), Stage.ID_BUILD, hash, componentIdentifier);
    tempEntity.newApplicationWithParent();
    mockReport("scanId", "/" + getClass().getSimpleName() + "/report");

    restRequest().query("hash", hash).query("packageUrl", packageUrl).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.SEARCH_COMPONENT_USES, null);
    assertCustomData(auditDTO, "componentHash", hash);
    assertCustomObject(auditDTO, "componentIdentifier", componentIdentifier);
    assertCustomData(auditDTO, "inspectedApplicationCount", 2);
    assertCustomData(auditDTO, "resultRecordCount", 1);
  }

  @Test
  public void testSearchComponent_Error() throws Exception {
    // Invalid hash should trigger a bad-request error.
    String hash = "foo";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");

    restRequest().query("hash", hash).query("componentIdentifier", componentIdentifier).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.SEARCH_COMPONENT_USES, "bad-request");
    assertCustomData(auditDTO, "componentHash", hash);
    assertCustomObject(auditDTO, "componentIdentifier", componentIdentifier);
  }
}
