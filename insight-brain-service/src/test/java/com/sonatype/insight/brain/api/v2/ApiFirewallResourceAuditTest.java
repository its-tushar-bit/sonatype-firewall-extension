/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallReleaseQuarantineConfigDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.AutoUnquarantinePolicyConditionTypeDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiFirewallResourceAuditTest
    extends AbstractAuditTest
{
  private final PolicyMonitoringDAO policyMonitoringDAO = new PolicyMonitoringDAO();

  private final AutoUnquarantinePolicyConditionTypeDAO autoUnquarantinePolicyConditionTypeDAO =
      new AutoUnquarantinePolicyConditionTypeDAO();

  @After
  public void cleanUp() {
    policyMonitoringDAO.getAll().forEach(policyMonitoringDAO::delete);
    autoUnquarantinePolicyConditionTypeDAO.getAll().forEach(autoUnquarantinePolicyConditionTypeDAO::delete);
  }

  @Test
  public void testSetFirewallAutoUnquarantineConfig() throws Exception {
    //setup: add new dto to list
    List<ApiFirewallReleaseQuarantineConfigDTO> list = new ArrayList<>();
    ApiFirewallReleaseQuarantineConfigDTO dto = new ApiFirewallReleaseQuarantineConfigDTO();
    dto.autoReleaseQuarantineEnabled = true;
    dto.id = LicenseConditionType.ID;
    list.add(dto);

    //when: setting firewall auto unquarantine config
    HttpResponse response = restRequest().path(PublicApiPaths.FIREWALL_RESOURCE_PATH,
        ApiFirewallResource.RELEASE_QUARANTINE_CONFIGURATION_PATH).body(list).put();
    ApiFirewallReleaseQuarantineConfigDTO[] dtos = response.getBody(ApiFirewallReleaseQuarantineConfigDTO[].class);
    assertResponseStatus(200, response);

    //then: expect returned dtos to be greater than zero
    assertThat(dtos).isNotNull().isNotEmpty();

    //then: expect audit log entries to be created
    AuditDTO auditLog = awaitLogEntries(AuditEvent.CONFIGURE_CONTINUOUS_MONITORING, 1).get(0);
    assertRepositoryContainerData(auditLog);
    assertCustomData(auditLog, "stageId", StageTypes.PROXY.getId());
  }

  @Test
  public void testSetQuarantinedComponentViewAnonymousAccess() throws Exception {
    HttpResponse response = restRequest().path(PublicApiPaths.FIREWALL_RESOURCE_PATH,
        ApiFirewallResource.QUARANTINED_COMPONENT_VIEW_CONFIG_ANONYMOUS_ACCESS_SET).parameter(false).put();
    assertResponseStatus(204, response);

    AuditDTO auditLog = awaitLogEntries(AuditEvent.CONFIGURE_SECURITY_QUARANTINED_COMPONENT_VIEW_ANON_ACCESS, 1).get(0);
    assertCustomData(auditLog, "enabled", false);
  }
}
