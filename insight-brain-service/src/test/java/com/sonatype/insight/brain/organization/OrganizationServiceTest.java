/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.policy.PolicyService;
import com.sonatype.insight.brain.policy.violation.AbstractPolicyViolationLogger;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogDTO;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogDTOAssert;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogEvent;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.webhook.ManagementEvent.OwnerEvent;
import com.sonatype.insight.brain.webhook.TestEventHandler;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.test.LogOutput;

import org.junit.Rule;
import org.junit.Test;

import static com.sonatype.insight.brain.webhook.EventAction.CREATED;
import static com.sonatype.insight.brain.webhook.EventAction.DELETED;
import static com.sonatype.insight.brain.webhook.EventAction.UPDATED;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.assertTrue;

public class OrganizationServiceTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);

  @Inject
  private OrganizationService organizationService;

  @Inject
  private InsightWork work;

  @Inject
  private AsyncEventBus eventBus;

  @Inject
  private PolicyViolationLoggerFactory policyViolationLoggerFactory;

  /**
   * There's a similar protection at the DAO layer but given the order of operations, the service layer needs to prevent
   * deletion of the root org as well before it starts carrying out any other destructive actions like cleaning the
   * filesystem (e.g. icons).
   */
  @Test
  public void testDeleteOrganization_RootOrgCannotBeDeleted() throws Exception {
    File iconDir = new File(work.getOrganizationIconDir(), Organization.ROOT_ORGANIZATION_ID);
    assertThat(iconDir.mkdirs()).isTrue();
    File iconFile = new File(iconDir, "icon.png");
    assertThat(iconFile.createNewFile()).isTrue();

    Organization childOrg = tempEntity.newOrganization();

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      organizationService.deleteOrganization(Organization.ROOT_ORGANIZATION_ID);
    }).withMessageContaining("root organization cannot be deleted");
    assertThat(new OrganizationDAO().getById(childOrg.getId())).isNotNull();
    assertThat(iconFile).isFile();
    assertThat(iconDir).isDirectory();
  }

  @Test
  public void testGetAll() throws Exception {
    OrganizationService organizationService =
        new OrganizationService(null, null, null, new OrganizationDAO(), null, policyViolationLoggerFactory,
            new PolicyService());

    List<Organization> orgs = organizationService.getAll();
    assertThat(orgs).hasSize(1);
  }

  @Test
  public void testAddUpdateAndDeleteOrganizationPostEvents() throws Exception {
    TestEventHandler<OwnerEvent> handler = new TestEventHandler<>(new CountDownLatch(1));
    eventBus.register(handler);

    Organization org = new Organization("testOrg");
    Organization created = organizationService.addOrganization(org);
    final String organizationId = created.getId();

    assertThat(handler.getLatch().await(5, SECONDS)).isTrue();
    assertThat(handler.getEvent().action).isEqualTo(CREATED);
    assertThat(handler.getEvent().ownerId).isEqualTo(organizationId);
    assertThat(handler.getEvent().owner.getId()).isEqualTo(organizationId);

    handler.setLatch(new CountDownLatch(1));

    created.setName("new appId");
    created = organizationService.updateOrganization(created);

    assertThat(handler.getLatch().await(5, SECONDS)).isTrue();
    assertThat(handler.getEvent().action).isEqualTo(UPDATED);
    assertThat(handler.getEvent().ownerId).isEqualTo(organizationId);
    assertThat(handler.getEvent().owner.getId()).isEqualTo(organizationId);

    handler.setLatch(new CountDownLatch(1));

    organizationService.deleteOrganization(created.getId());

    assertThat(handler.getLatch().await(5, SECONDS)).isTrue();
    assertThat(handler.getEvent().action).isEqualTo(DELETED);
    assertThat(handler.getEvent().ownerId).isEqualTo(organizationId);
    assertThat(handler.getEvent().owner.getId()).isEqualTo(organizationId);

    eventBus.unregister(handler);
  }

  @Test
  public void testDeleteOrganization_PolicyViolationLogger_LogsClearEvent() throws Exception {
    Organization organization = tempEntity.newOrganization();

    Date before = new Date();
    organizationService.deleteOrganization(organization.getId());
    Date after = new Date();

    List<PolicyViolationLogDTO> policyViolationLogDTOs = PolicyViolationLogDTOAssert
        .assertPolicyViolationLogDTOs(logOutput, 1);
    PolicyViolationLogDTOAssert
        .assertOrganizationPolicyViolationData(policyViolationLogDTOs.get(0), PolicyViolationLogEvent.CLEAR,
            organization, before, after);
  }

  @Test
  public void testRemoveOverrides() throws Exception {
    //given
    PolicyDAO policyDAO = new PolicyDAO();
    String organizationName = "PolicyResourceTest_testRemoveOverrides";
    Organization organization = tempEntity.newOrganization(organizationName);
    Map<String, String> actionsOverride = new HashMap<>();
    actionsOverride.put("stage-release", "fail");
    Policy firstPolicy = tempEntity.newPolicy();
    Policy secondPolicy = tempEntity.newPolicy();
    firstPolicy.addPolicyActionsOverride(organization.getId(), actionsOverride);
    firstPolicy.addPolicyActionsOverride("appId", actionsOverride);
    secondPolicy.addPolicyActionsOverride(organization.getId(), actionsOverride);
    policyDAO.update(firstPolicy);
    policyDAO.update(secondPolicy);

    //when
    organizationService.deleteOrganization(organization.getId());

    //then
    List<Policy> expectedPolicyState = policyDAO.getAll();
    assertThat(2).isEqualTo(expectedPolicyState.size());

    Map<String, Map<String, String>> firstPolicyActionsOverrides =
        expectedPolicyState.get(0).getPolicyActionsOverrides();
    assertThat(firstPolicyActionsOverrides.size()).isOne();

    assertTrue(firstPolicyActionsOverrides.containsKey("appId"));

    Map<String, Map<String, String>> secondPolicyActionsOverrides =
        expectedPolicyState.get(1).getPolicyActionsOverrides();
    assertThat(secondPolicyActionsOverrides).isEmpty();
  }
}
