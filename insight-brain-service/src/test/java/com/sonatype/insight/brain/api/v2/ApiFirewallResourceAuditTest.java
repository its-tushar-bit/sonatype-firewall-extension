/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallReleaseQuarantineConfigDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryManagerDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiFirewallResourceAuditTest
    extends AbstractAuditTest
{
  private RepositoryManagerDAO repositoryManagerDAO;

  @Before
  public void setUp() {
    repositoryManagerDAO = lookup(RepositoryManagerDAO.class);
  }

  @Test
  public void testSetFirewallAutoUnquarantineConfig() throws Exception {
    // setup: add new dto to list
    List<ApiFirewallReleaseQuarantineConfigDTO> list = new ArrayList<>();
    ApiFirewallReleaseQuarantineConfigDTO dto = new ApiFirewallReleaseQuarantineConfigDTO();
    dto.autoReleaseQuarantineEnabled = true;
    dto.id = LicenseConditionType.ID;
    list.add(dto);

    // when: setting firewall auto unquarantine config
    HttpResponse response = restRequest().path(PublicApiPaths.FIREWALL_RESOURCE_PATH,
        ApiFirewallResource.RELEASE_QUARANTINE_CONFIGURATION_PATH).body(list).put();
    ApiFirewallReleaseQuarantineConfigDTO[] dtos = response.getBody(ApiFirewallReleaseQuarantineConfigDTO[].class);
    assertResponseStatus(200, response);

    // then: expect returned dtos to be greater than zero
    assertThat(dtos).isNotNull().isNotEmpty();

    // then: expect audit log entries to be created
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

  @Test
  public void testConfigureRepositories() throws Exception {
    Repository repository = tempEntity.newRepository();
    repository.setAuditEnabled(!repository.isAuditEnabled());
    ApiRepositoryListDTO dto = new ApiRepositoryListDTO();
    dto.repositories = Collections.singletonList(ApiRepositoryDTO.fromRepository(repository));

    HttpResponse response = restRequest()
        .path(PublicApiPaths.FIREWALL_RESOURCE_PATH, ApiFirewallResource.REPOSITORIES_CONFIGURATION_PATH)
        .parameter(repository.getRepositoryManagerId())
        .body(dto)
        .post();

    assertResponseStatus(204, response);
    AuditDTO auditLog = awaitLogEntries(AuditEvent.CONFIGURE_REPOSITORY, 1).get(0);
    assertCustomData(auditLog, "repositoryManagerId", repository.getRepositoryManagerId());
    assertRepositoryData(auditLog, repository);
  }

  @Test
  public void testAddRepositoryManager() throws Exception {
    ApiRepositoryManagerDTO apiRepositoryManagerDTO = new ApiRepositoryManagerDTO();
    apiRepositoryManagerDTO.instanceId = "testInstanceId";
    apiRepositoryManagerDTO.name = "testName";
    apiRepositoryManagerDTO.productName = "testProductName";
    apiRepositoryManagerDTO.productVersion = "testProductVersion";

    HttpResponse response =
        restRequest().path(PublicApiPaths.FIREWALL_RESOURCE_PATH, ApiFirewallResource.REPOSITORY_MANAGERS_PATH)
            .body(apiRepositoryManagerDTO)
            .post();

    assertResponseStatus(200, response);
    apiRepositoryManagerDTO = response.getBody(ApiRepositoryManagerDTO.class);
    RepositoryManager repositoryManager = repositoryManagerDAO.getById(apiRepositoryManagerDTO.id);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_REPOSITORY_MANAGER, null);
    assertRepositoryManagerData(auditDTO, repositoryManager);
  }

  @Test
  public void testAddRepositoryManager_Unauthorized() throws Exception {
    ApiRepositoryManagerDTO apiRepositoryManagerDTO = new ApiRepositoryManagerDTO();
    apiRepositoryManagerDTO.instanceId = "testInstanceId";
    apiRepositoryManagerDTO.name = "testName";
    apiRepositoryManagerDTO.productName = "testProductName";
    apiRepositoryManagerDTO.productVersion = "testProductVersion";

    restRequest().path(PublicApiPaths.FIREWALL_RESOURCE_PATH, ApiFirewallResource.REPOSITORY_MANAGERS_PATH)
        .with(unauthorizedUser())
        .body(apiRepositoryManagerDTO)
        .post();

    assertAuditLog(AuditEvent.CREATE_REPOSITORY_MANAGER, "unauthorized");
  }

  @Test
  public void testDeleteRepositoryManager() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    restRequest().path(PublicApiPaths.FIREWALL_RESOURCE_PATH, ApiFirewallResource.REPOSITORY_MANAGER_PATH)
        .parameter(repositoryManager.getId())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_REPOSITORY_MANAGER, null /* error */);
    assertRepositoryManagerData(auditDTO, repositoryManager);
  }

  @Test
  public void testDeleteRepositoryManager_Unauthorized() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    restRequest().path(PublicApiPaths.FIREWALL_RESOURCE_PATH, ApiFirewallResource.REPOSITORY_MANAGER_PATH)
        .parameter(repositoryManager.getId())
        .with(unauthorizedUser())
        .delete();

    assertAuditLog(AuditEvent.DELETE_REPOSITORY_MANAGER, "unauthorized");
  }
}
