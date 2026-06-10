/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiLegacyViolationStatusDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

/**
 * Audit tests for {@link ApiLegacyViolationConfigResource}.
 * Verifies that setConfig (PUT) writes the CONFIGURE_LEGACY_VIOLATION_STATUS audit event
 * for both APPLICATION and ORGANIZATION owner types.
 */
public class ApiLegacyViolationConfigResourceAuditTest
    extends AbstractAuditTest
{
  private Organization organization;

  private Application application;

  @Before
  public void before() {
    organization = tempEntity.newOrganization();
    application = tempEntity.newApplicationWithParent();
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2);
  }

  @Test
  public void testSetConfig_Application_AuditsConfigureLegacyViolationStatus() throws Exception {
    ApiLegacyViolationStatusDTO request = new ApiLegacyViolationStatusDTO();
    request.enabled = true;

    restRequest().path("application/{ownerId}")
        .parameter(application.getPublicId())
        .body(request)
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_LEGACY_VIOLATION_STATUS, null);
    assertApplicationData(auditDTO, application);
  }

  @Test
  public void testSetConfig_Organization_AuditsConfigureLegacyViolationStatus() throws Exception {
    ApiLegacyViolationStatusDTO request = new ApiLegacyViolationStatusDTO();
    request.enabled = true;
    request.allowOverride = true;

    restRequest().path("organization/{ownerId}")
        .parameter(organization.getId())
        .body(request)
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_LEGACY_VIOLATION_STATUS, null);
    assertOrganizationData(auditDTO, organization);
  }
}
