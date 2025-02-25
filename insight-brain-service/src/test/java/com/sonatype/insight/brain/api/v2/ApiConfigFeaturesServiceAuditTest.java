/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

import static org.eclipse.jetty.http.HttpStatus.NO_CONTENT_204;

public class ApiConfigFeaturesServiceAuditTest
    extends AbstractAuditTest
{
  private SystemConfigurationPropertyDAO configurationPropertyDAO;

  @Before
  public void setUp() {
    configurationPropertyDAO = lookup(SystemConfigurationPropertyDAO.class);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.CONFIG_FEATURES_PATH);
  }

  @Test
  public void testEnableFeature() throws Exception {
    configurationPropertyDAO.set(SystemConfigurationProperty.API_PAGE, "false");
    HttpResponse response = restRequest().path(SystemConfigurationProperty.API_PAGE).post();
    assertResponseStatus(NO_CONTENT_204, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.SET_FEATURES, null);
    assertCustomData(auditDTO, SystemConfigurationProperty.API_PAGE, "null");
  }

  @Test
  public void testEnableFeature_enabledWhenAbsent() throws Exception {
    configurationPropertyDAO.set(SystemConfigurationProperty.DASHBOARD_DISABLED, "true");
    HttpResponse response = restRequest().path(ApiConfigFeaturesService.FEATURE_DASHBOARD).post();

    assertResponseStatus(NO_CONTENT_204, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.SET_FEATURES, null);
    assertCustomData(auditDTO, SystemConfigurationProperty.DASHBOARD_DISABLED, "null");
  }

  @Test
  public void testEnableFeature_Error() throws Exception {
    HttpResponse response = restRequest().path(SystemConfigurationProperty.API_PAGE).post();

    assertResponseStatus(400, response);
    assertAuditLog(AuditEvent.SET_FEATURES, "bad-request");
  }

  @Test
  public void testDisableFeature() throws Exception {
    HttpResponse response = restRequest().path(SystemConfigurationProperty.API_PAGE).delete();
    assertResponseStatus(NO_CONTENT_204, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UNSET_FEATURES, null);
    assertCustomData(auditDTO, SystemConfigurationProperty.API_PAGE, "false");
  }

  @Test
  public void testDisableFeature_enabledWhenAbsent() throws Exception {
    HttpResponse response = restRequest().path(ApiConfigFeaturesService.FEATURE_DASHBOARD).delete();
    assertResponseStatus(NO_CONTENT_204, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UNSET_FEATURES, null);
    assertCustomData(auditDTO, SystemConfigurationProperty.DASHBOARD_DISABLED, "true");
  }

  @Test
  public void testDisableFeature_Error() throws Exception {
    configurationPropertyDAO.set(SystemConfigurationProperty.API_PAGE, "false");
    HttpResponse response = restRequest().path(SystemConfigurationProperty.API_PAGE).delete();

    assertResponseStatus(400, response);
    assertAuditLog(AuditEvent.UNSET_FEATURES, "bad-request");
  }
}
