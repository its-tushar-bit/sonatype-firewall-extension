/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.api.v2.FirewallPermissionGate;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverReasonDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.telemetry.PolicyWaiverTelemetryCreator;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.owner.OwnerService;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.brain.utils.IdUtils;

import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Set;

/**
 * Unit tests for {@link ApiPolicyWaiverService#checkOwnerInFirewallScope} (tested via
 * {@link ApiPolicyWaiverService#getPolicyWaiverForFirewall}).
 *
 * <p>
 * Covers all owner-type branches: REPOSITORY (permit/deny), APPLICATION (permit/deny),
 * ORGANIZATION, REPOSITORY_MANAGER, and REPOSITORY_CONTAINER (all always permitted for scoped users).
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiPolicyWaiverServiceFirewallScopeTest
{
  @Mock
  private TelemetrySender telemetrySender;

  @Mock
  private PolicyWaiverDAO policyWaiverDAO;

  @Mock
  private PolicyDAO policyDAO;

  @Mock
  private ApplicationDAO applicationDAO;

  @Mock
  private OwnerDAO ownerDAO;

  @Mock
  private PolicyEvaluationDAO policyEvaluationDAO;

  @Mock
  private ApiPolicyViolationServiceV2 apiPolicyViolationServiceV2;

  @Mock
  private PolicyWaiverTelemetryCreator policyWaiverTelemetryCreator;

  @Mock
  private CurrentUser currentUser;

  @Mock
  private OwnerService ownerService;

  @Mock
  private RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  @Mock
  private PolicyViolationDAO policyViolationDAO;

  @Mock
  private OrganizationDAO organizationDAO;

  @Mock
  private PolicyWaiverReasonDAO policyWaiverReasonDAO;

  @Mock
  private RepositoryDAO repositoryDAO;

  @Mock
  private IdUtils idUtils;

  @Mock
  private TelemetryUtils telemetryUtils;

  @Mock
  private FirewallPermissionGate firewallPermissionGate;

  private ApiPolicyWaiverService service;

  private static final String PERMITTED_REPO_ID = "permitted-repo-id";

  private static final String OTHER_REPO_ID = "other-repo-id";

  private static final String WAIVER_ID = "waiver-id";

  private static final Set<String> PERMITTED = Set.of(PERMITTED_REPO_ID);

  private PolicyWaiver waiver;

  @Before
  public void setUp() {
    service = new ApiPolicyWaiverService(
        telemetrySender, policyWaiverDAO, policyDAO, applicationDAO, ownerDAO,
        policyEvaluationDAO, apiPolicyViolationServiceV2, policyWaiverTelemetryCreator,
        currentUser, ownerService, repositoryPolicyViolationDAO, policyViolationDAO,
        organizationDAO, policyWaiverReasonDAO, repositoryDAO, idUtils, telemetryUtils,
        firewallPermissionGate);

    waiver = new PolicyWaiver();
    waiver.setId(WAIVER_ID);

    Policy mockPolicy = new Policy();
    mockPolicy.setName("test-policy");
    when(policyDAO.getById(any())).thenReturn(mockPolicy);
  }

  // --- REPOSITORY scope ---

  @Test
  public void getPolicyWaiverForFirewall_Repository_PermittedRepo_Succeeds() {
    Repository repo = new Repository();
    repo.setId(PERMITTED_REPO_ID);
    waiver.setOwnerId(PERMITTED_REPO_ID);
    when(idUtils.getOwnerNotNull(OwnerType.REPOSITORY, PERMITTED_REPO_ID)).thenReturn(repo);
    when(policyWaiverDAO.getByIdAndOwnerIdNotNull(WAIVER_ID, PERMITTED_REPO_ID)).thenReturn(waiver);

    ApiPolicyWaiverDTO dto = service.getPolicyWaiverForFirewall(
        OwnerType.REPOSITORY, PERMITTED_REPO_ID, WAIVER_ID, PERMITTED);

    assertThat(dto).isNotNull();
  }

  @Test
  public void getPolicyWaiverForFirewall_Repository_NonPermittedRepo_Throws() {
    Repository repo = new Repository();
    repo.setId(OTHER_REPO_ID);
    when(idUtils.getOwnerNotNull(OwnerType.REPOSITORY, OTHER_REPO_ID)).thenReturn(repo);

    assertThatThrownBy(
        () -> service.getPolicyWaiverForFirewall(OwnerType.REPOSITORY, OTHER_REPO_ID, WAIVER_ID, PERMITTED))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("Access denied");
  }

  // --- APPLICATION scope ---

  @Test
  public void getPolicyWaiverForFirewall_Application_LinkedOrg_Succeeds() {
    String shadowOrgId = "shadow-org-id";
    String appId = "container-app-id";

    Application app = new Application();
    app.setId(appId);
    app.setOrganizationId(shadowOrgId);
    waiver.setOwnerId(appId);

    when(idUtils.getOwnerNotNull(OwnerType.APPLICATION, appId)).thenReturn(app);
    when(organizationDAO.getOrganizationIdsByRelatedRepositoryIds(PERMITTED)).thenReturn(Set.of(shadowOrgId));
    when(policyWaiverDAO.getByIdAndOwnerIdNotNull(WAIVER_ID, appId)).thenReturn(waiver);

    ApiPolicyWaiverDTO dto = service.getPolicyWaiverForFirewall(
        OwnerType.APPLICATION, appId, WAIVER_ID, PERMITTED);

    assertThat(dto).isNotNull();
  }

  @Test
  public void getPolicyWaiverForFirewall_Application_UnlinkedOrg_Throws() {
    String appId = "lc-app-id";
    String lcOrgId = "lc-org-id";

    Application app = new Application();
    app.setId(appId);
    app.setOrganizationId(lcOrgId);

    when(idUtils.getOwnerNotNull(OwnerType.APPLICATION, appId)).thenReturn(app);
    when(organizationDAO.getOrganizationIdsByRelatedRepositoryIds(PERMITTED)).thenReturn(Collections.emptySet());

    assertThatThrownBy(() -> service.getPolicyWaiverForFirewall(OwnerType.APPLICATION, appId, WAIVER_ID, PERMITTED))
        .isInstanceOf(UnauthorizedException.class)
        .hasMessageContaining("Access denied");
  }

  // --- Org-level owners: always accessible to any scoped user ---

  @Test
  public void getPolicyWaiverForFirewall_Organization_AnyScoped_Succeeds() {
    Organization org = new Organization();
    org.setId(Organization.ROOT_ORGANIZATION_ID);
    waiver.setOwnerId(Organization.ROOT_ORGANIZATION_ID);

    when(idUtils.getOwnerNotNull(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID)).thenReturn(org);
    when(policyWaiverDAO.getByIdAndOwnerIdNotNull(WAIVER_ID, Organization.ROOT_ORGANIZATION_ID)).thenReturn(waiver);

    ApiPolicyWaiverDTO dto = service.getPolicyWaiverForFirewall(
        OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID, WAIVER_ID, PERMITTED);

    assertThat(dto).isNotNull();
  }

  @Test
  public void getPolicyWaiverForFirewall_RepositoryManager_AnyScoped_Succeeds() {
    RepositoryManager rm = new RepositoryManager();
    rm.setId("rm-id");
    waiver.setOwnerId("rm-id");

    when(idUtils.getOwnerNotNull(OwnerType.REPOSITORY_MANAGER, "rm-id")).thenReturn(rm);
    when(policyWaiverDAO.getByIdAndOwnerIdNotNull(WAIVER_ID, "rm-id")).thenReturn(waiver);

    ApiPolicyWaiverDTO dto = service.getPolicyWaiverForFirewall(
        OwnerType.REPOSITORY_MANAGER, "rm-id", WAIVER_ID, PERMITTED);

    assertThat(dto).isNotNull();
  }

  @Test
  public void getPolicyWaiverForFirewall_RepositoryContainer_AnyScoped_Succeeds() {
    RepositoryContainer rc = RepositoryContainer.SINGLETON;
    waiver.setOwnerId(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    when(idUtils.getOwnerNotNull(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID))
        .thenReturn(rc);
    when(policyWaiverDAO.getByIdAndOwnerIdNotNull(WAIVER_ID, RepositoryContainer.REPOSITORY_CONTAINER_ID))
        .thenReturn(waiver);

    ApiPolicyWaiverDTO dto = service.getPolicyWaiverForFirewall(
        OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID, WAIVER_ID, PERMITTED);

    assertThat(dto).isNotNull();
  }

  // --- Full-access (null permittedRepositoryIds) bypasses scope check ---

  @Test
  public void getPolicyWaiverForFirewall_NullPermitted_FullAccess_Succeeds() {
    Repository repo = new Repository();
    repo.setId(OTHER_REPO_ID);
    waiver.setOwnerId(OTHER_REPO_ID);

    when(idUtils.getOwnerNotNull(OwnerType.REPOSITORY, OTHER_REPO_ID)).thenReturn(repo);
    when(policyWaiverDAO.getByIdAndOwnerIdNotNull(WAIVER_ID, OTHER_REPO_ID)).thenReturn(waiver);

    // null permittedRepositoryIds = full access, no scope check
    ApiPolicyWaiverDTO dto = service.getPolicyWaiverForFirewall(
        OwnerType.REPOSITORY, OTHER_REPO_ID, WAIVER_ID, null);

    assertThat(dto).isNotNull();
  }
}
