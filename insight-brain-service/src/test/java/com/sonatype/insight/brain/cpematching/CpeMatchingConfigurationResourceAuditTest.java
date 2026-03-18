/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.cpematching;

import com.sonatype.insight.brain.common.test.SlowTest;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.api.PublicApiPaths.CPE_MATCHING_CONFIGURATION_RESOURCE_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class CpeMatchingConfigurationResourceAuditTest
    extends AbstractAuditTest
{
  @Before
  public void setUp() {
    getTestProductLicenseManager().setFeatures(LicensedFeature.CPE_MATCHING);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(CPE_MATCHING_CONFIGURATION_RESOURCE_PATH);
  }

  @Test
  public void testUpdateCpeMatchingConfiguration() throws Exception {
    Application app1 = tempEntity.newApplicationWithParent();
    CpeMatchingConfigurationRequest requestDTO = new CpeMatchingConfigurationRequest();
    requestDTO.enabled = true;
    HttpResponse response = restRequest().parameter("application", app1.getId())
        .body(requestDTO)
        .put();
    assertResponseStatus(200, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_CPE_MATCHING_CONFIGURATION, null);
    assertCustomData(auditDTO, "enabled", true);
  }

  @Test
  public void testUpdateCpeMatchingConfiguration_unauthorized() throws Exception {
    Application app1 = tempEntity.newApplicationWithParent();
    CpeMatchingConfigurationRequest requestDTO = new CpeMatchingConfigurationRequest();
    requestDTO.enabled = true;
    HttpResponse response = restRequest().parameter("application", app1.getId())
        .body(requestDTO)
        .with(unauthorizedUser())
        .put();
    assertResponseStatus(403, response);
    assertAuditLog(AuditEvent.UPDATE_CPE_MATCHING_CONFIGURATION, "unauthorized");
  }

  @Test
  public void testUpdateCpeMatchingConfiguration_noRequestObjectError() throws Exception {
    Application app1 = tempEntity.newApplicationWithParent();
    HttpResponse response = restRequest().parameter("application", app1.getId()).put();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("CPE matching configuration cannot be null");
    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_CPE_MATCHING_CONFIGURATION, "bad-request");
    assertCustomData(auditDTO, "enabled", null);
  }
}
