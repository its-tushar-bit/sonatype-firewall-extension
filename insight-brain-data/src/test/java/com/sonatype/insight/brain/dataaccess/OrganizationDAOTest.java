/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticApplicationsConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.DataRetentionPolicyDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProprietaryConfigDAO;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLock;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.lock.H2ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.lock.PostgresClusterLockManager;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryConnectionDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlOrganizationImportEventDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCvssSeverityDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCvssVectorDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCweDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomRemediationDAO;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.SearchIndexChange.ChangeType;
import com.sonatype.insight.brain.model.configuration.DataRetentionPolicy;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCvssSeverity;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCvssVector;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCwe;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomRemediation;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OrganizationDAOTest extends NameableDAOTest<Organization>
{
  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  private LabelDAO labelDAO;

  private ProprietaryConfigDAO proprietaryConfigDAO;

  private TagDAO tagDAO;

  private PolicyDAO policyDAO;

  private LicenseOverrideDAO licenseOverrideDAO;

  private SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO;

  private PolicyWaiverDAO policyWaiverDAO;

  private RoleDAO roleDAO;

  private MembershipMappingDAO membershipMappingDAO;

  private PolicyMonitoringDAO policyMonitoringDAO;

  private DataRetentionPolicyDAO dataRetentionPolicyDAO;

  private SourceControlDAO sourceControlDAO;

  private SourceControlOrganizationImportEventDAO sourceControlOrganizationImportEventDAO;

  private AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO;

  private SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  private SearchIndexChangeDAO searchIndexChangeDAO;

  private RepositoryConnectionDAO repositoryConnectionDAO;

  private VulnerabilityCustomRemediationDAO vulnerabilityCustomRemediationDAO;

  private VulnerabilityCustomCweDAO vulnerabilityCustomCweDAO;

  private VulnerabilityCustomCvssVectorDAO vulnerabilityCustomCvssVectorDAO;

  private VulnerabilityCustomCvssSeverityDAO vulnerabilityCustomCvssSeverityDAO;

  private OrganizationDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    labelDAO = daoFactory.createLabelDAO();
    proprietaryConfigDAO = daoFactory.createProprietaryConfigDAO();
    tagDAO = daoFactory.createTagDAO();
    policyDAO = daoFactory.createPolicyDAO();
    licenseOverrideDAO = daoFactory.createLicenseOverrideDAO();
    securityVulnerabilityOverrideDAO = daoFactory.createSecurityVulnerabilityOverrideDAO();
    policyWaiverDAO = daoFactory.createPolicyWaiverDAO();
    roleDAO = daoFactory.createRoleDAO();
    membershipMappingDAO = daoFactory.createMembershipMappingDAO();
    policyMonitoringDAO = daoFactory.createPolicyMonitoringDAO();
    dataRetentionPolicyDAO = daoFactory.createDataRetentionPolicyDAO();
    sourceControlDAO = daoFactory.createSourceControlDAO();
    sourceControlOrganizationImportEventDAO = daoFactory.createSourceControlOrganizationImportEventDAO();
    automaticApplicationsConfigurationDAO = daoFactory.createAutomaticApplicationsConfigurationDAO();
    systemConfigurationPropertyDAO = daoFactory.createSystemConfigurationPropertyDAO();
    searchIndexChangeDAO = daoFactory.createSearchIndexChangeDAO();
    repositoryConnectionDAO = daoFactory.createRepositoryConnectionDAO();
    vulnerabilityCustomRemediationDAO = daoFactory.createVulnerabilityCustomRemediationDAO();
    vulnerabilityCustomCweDAO = daoFactory.createVulnerabilityCustomCweDAO();
    vulnerabilityCustomCvssVectorDAO = daoFactory.createVulnerabilityCustomCvssVectorDAO();
    vulnerabilityCustomCvssSeverityDAO = daoFactory.createVulnerabilityCustomCvssSeverityDAO();
    dao = daoFactory.createOrganizationDAO();
  }

  @Override
  protected Organization createNameable(String a) {
    return tempEntity.newOrganization(a);
  }

  @Override
  protected AbstractOperationalSqlDAO<Organization> getDao() {
    return dao;
  }

  @Override
  protected int getMaxNameLength() {
    return NameHelper.MAX_NAME_LENGTH_APP_ORG;
  }

  @Override
  protected Organization getEntityByName(String name) {
    return dao.getByName(name);
  }

  @Test
  public void testCRUD() {
    // Create
    organization = tempEntity.newOrganization("OrganizationDAOTest");
    String organizationId = organization.getId();
    organization = dao.getById(organizationId);
    assertThat(organization.getName()).isEqualTo("OrganizationDAOTest");
    assertThat(organization.getParentOrganizationId()).isEqualTo(Organization.ROOT_ORGANIZATION_ID);

    // Update
    organization.setName("OrganizationDAOTest New name");
    dao.update(organization);
    organization = dao.getById(organizationId);
    assertThat(organization.getName()).isEqualTo("OrganizationDAOTest New name");

    // Delete
    dao.delete(organization);
    organization = dao.getById(organizationId);
    assertThat(organization).isNull();
  }

  @Test
  public void testInsert_AllowLegacyViolationOverride_DefaultsToTrue() {
    organization = new Organization("OrganizationDAOTest");
    assertThat(organization.isAllowLegacyViolationOverride()).isTrue();

    dao.insert(organization);
    organization = dao.getById(organization.getId());
    assertThat(organization.isAllowLegacyViolationOverride()).isTrue();
  }

  @Test
  public void testGetAll() {
    // Create a few orgs
    int orgCount = 3;
    tempEntity.newOrganizations(orgCount);

    // getAll should return orgCount + 2, to account for org created by AbstractDbDAOTest and one for the root org
    assertThat(dao.getAll()).hasSize(orgCount + 2);
  }

  @Test
  public void testGetByIdNotNull() {
    Organization expected = tempEntity.newOrganization();
    Organization actual = dao.getByIdNotNull(expected.getId());

    assertThat(actual.getId()).isEqualTo(expected.getId());
    assertThat(actual.getName()).isEqualTo(expected.getName());
    assertThat(actual.getParentOrganizationId()).isEqualTo(expected.getParentOrganizationId());
  }

  @Test
  public void testGetByIdNotNull_null() {
    assertThatThrownBy(() -> dao.getByIdNotNull("non-existent-org")).isInstanceOf(NotFoundException.class);
  }

  @Test
  public void testGetByNames() {
    Organization org1 = tempEntity.newOrganization("org1");
    tempEntity.newOrganization("org2");
    Organization org3 = tempEntity.newOrganization("org3");

    List<Organization> orgs = dao.getByNames(Sets.newHashSet(org3.getName(), org1.getName()));
    assertThat(orgs).extracting(Organization::getId).containsExactly(org1.getId(), org3.getId());
  }

  @Test
  public void testGetByNames_NormalizesNames() {
    Organization organization = tempEntity.newOrganization("My AwEsOmE OrG");

    List<Organization> orgs = dao.getByNames(Collections.singleton("mY aWeSoMe OrG"));

    assertThat(orgs).extracting(Organization::getId).containsExactly(organization.getId());
  }

  @Test
  public void testGetByNames_GivenEmpty() {
    assertThat(dao.getByNames(Collections.emptySet())).isEmpty();
  }

  @Test
  public void testGetByNames_NotFound() {
    assertThat(dao.getByNames(Collections.singleton("doesNotExist"))).isEmpty();
  }

  @Test
  public void testGetByNames_SomeNotFound() {
    Organization organization = tempEntity.newOrganization("org1");

    List<Organization> orgs = dao.getByNames(Sets.newHashSet("doesNotExist1", organization.getName(), "doesNotExist2"));
    assertThat(orgs).extracting(Organization::getId).containsExactly(organization.getId());
  }

  @Test
  public void testDelete_CannotDeleteRootOrg() {
    organization = dao.getById(Organization.ROOT_ORGANIZATION_ID);
    assertThatThrownBy(() -> dao.delete(organization)).isInstanceOf(BadRequestException.class)
        .hasMessage("Cannot delete the root organization: Root Organization");
  }

  @Test
  public void testInsert_ParentOrganizationIdIsForcedToRootWhenNull() {
    organization = new Organization();
    organization.setId("testId");
    organization.setName("testName");
    try {
      dao.insert(organization);
      organization = dao.getById("testId");
      assertThat(organization.getParentOrganizationId()).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
    }
    finally {
      dao.delete(organization);
    }
  }

  @Test
  public void testInsert_ParentOrganizationIdIsNotRoot() {
    Organization parentOrg = tempEntity.newOrganization("Test Parent Org");
    organization = new Organization();
    organization.setName("testName");
    organization.setParentOrganizationId(parentOrg.getId());

    try {
      dao.insert(organization);
      organization = dao.getById(organization.getId());
      assertThat(organization.getParentOrganizationId()).isEqualTo(parentOrg.getId());
    }
    finally {
      dao.delete(organization);
    }
  }

  @Test
  public void testInsert_ParentOrganizationIdIsRejectedWhenInvalid() {
    organization = new Organization();
    organization.setName("testName");
    organization.setParentOrganizationId("invalid-org-id");
    assertThatThrownBy(() -> {
      dao.insert(organization);
    }).isInstanceOf(BadRequestException.class).hasMessage("Invalid parent organization");
  }

  @Test
  public void testUpdate_RootOrgsParentOrganizationIdIsForcedToNull() {
    organization = dao.getById(Organization.ROOT_ORGANIZATION_ID);
    organization.setParentOrganizationId("dummyOrg");
    dao.update(organization);

    organization = dao.getById(organization.getId());
    assertThat(organization.getParentOrganizationId()).isNull();
  }

  @Test
  public void testUpdate_ParentOrganizationIdIsNull() {
    organization = tempEntity.newOrganization("OrganizationDAOTest");
    organization.setParentOrganizationId(null);
    assertThatThrownBy(() -> {
      dao.update(organization);
    }).isInstanceOf(BadRequestException.class).hasMessage("Parent organization id cant be null.");
  }

  @Test
  public void testUpdate_ParentOrganizationIdHasChanged() {
    organization = tempEntity.newOrganization("OrganizationDAOTest");
    Organization parentOrg = tempEntity.newOrganization("Test Parent Org");
    String parentOrgId = parentOrg.getId();
    organization.setParentOrganizationId(parentOrgId);
    dao.update(organization);

    organization = dao.getById(organization.getId());
    assertThat(parentOrgId).isEqualTo(organization.getParentOrganizationId());
  }

  @Test
  public void testUpdate_RootOrgName() {
    organization = dao.getById(Organization.ROOT_ORGANIZATION_ID);
    String originalName = organization.getName();
    organization.setName("Test Root");
    try {
      dao.update(organization);

      organization = dao.getById(Organization.ROOT_ORGANIZATION_ID);
      assertThat(organization.getName()).isEqualTo("Test Root");
    }
    finally {
      organization.setName(originalName);
      dao.update(organization);
    }
  }

  @Test
  public void testDelete_CascadeToLabels() {
    Organization organization = tempEntity.newOrganization("organization");

    String organizationId = organization.getId();

    Label label = new Label();
    label.setLabel("label");
    label.setColor(Color.dark_purple);
    label.setOwnerId(organization.getId());
    labelDAO.insert(label);

    // sanity check
    assertThat(labelDAO.getByOwnerId(organizationId)).isNotEmpty();

    dao.delete(organization);

    assertThat(labelDAO.getByOwnerId(organizationId)).isEmpty();
  }

  @Test
  public void testDelete_CascadeToProprietaryConfig() {
    Organization organization = tempEntity.newOrganization("organization");
    tempEntity.newProprietaryConfig(organization.getId());

    dao.delete(organization);
    assertThat(proprietaryConfigDAO.getByOwnerId(organization.getId())).isNull();
  }

  @Test
  public void testDelete_CascadeToTags() {
    Organization organization = tempEntity.newOrganization("organization");

    String organizationId = organization.getId();

    Tag tag = new Tag(organizationId, "testCascadeDeleteToTags", "testCascadeDeleteToTags", Color.yellow);
    tagDAO.insert(tag);

    // sanity check
    assertThat(tagDAO.getByOrganizationId(organizationId)).isNotEmpty();

    dao.delete(organization);

    assertThat(tagDAO.getByOrganizationId(organizationId)).isEmpty();
  }

  @Test
  public void testDelete_CascadeToPolicies() {
    Organization organization = tempEntity.newOrganization("organization");

    tempEntity.newPolicy(organization);
    List<Policy> policies = policyDAO.getByOwnerId(organization.getId());
    assertThat(policies).hasSize(1);

    dao.delete(organization);
    policies = policyDAO.getByOwnerId(organization.getId());
    assertThat(policies).isEmpty();
  }

  @Test
  public void testDelete_CascadesToPolicyOverrides() {
    Organization organization = tempEntity.newOrganization("organization");

    Map<String, String> policyActionsOverrides = new HashMap<>();
    policyActionsOverrides.put("build", "warn");
    Policy policyWithOverrides = tempEntity.newPolicy(organization.getParentOrganizationId());
    policyWithOverrides.addPolicyActionsOverride(organization.getId(), policyActionsOverrides);
    policyWithOverrides.addPolicyActionsOverride("fakeOwnerId", policyActionsOverrides);
    Notifications policyNotificationsOverride = new Notifications();
    policyNotificationsOverride.add(new UserNotification("user@domain", BuildStageType.ID));
    policyWithOverrides.addPolicyNotificationsOverride(organization.getId(), policyNotificationsOverride);
    policyWithOverrides.addPolicyNotificationsOverride("fakeOwnerId", policyNotificationsOverride);
    policyDAO.update(policyWithOverrides);

    dao.delete(organization);
    Policy policy = policyDAO.getById(policyWithOverrides.getId());
    assertThat(policy.getPolicyActionsOverrides().keySet()).containsExactly("fakeOwnerId");
    assertThat(policy.getPolicyNotificationsOverrides().keySet()).containsExactly("fakeOwnerId");
  }

  @Test
  public void testDelete_CascadeToLicenseOverrides() {
    Organization organization = tempEntity.newOrganization("testCascadeDeleteToLicenseOverrides");
    String organizationId = organization.getId();

    LicenseOverride licenseOverride = new LicenseOverride(organizationId, ComponentIdentifier.createMavenCoordinates(
        "groupId", "artifactId", "version"), LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment");
    licenseOverrideDAO.insert(licenseOverride);
    List<LicenseOverride> licenseOverrides = licenseOverrideDAO.getByOwnerId(organizationId);
    assertThat(licenseOverrides).hasSize(1);

    dao.delete(organization);
    licenseOverrides = licenseOverrideDAO.getByOwnerId(organizationId);
    assertThat(licenseOverrides).isEmpty();
  }

  @Test
  public void testDelete_CascadeToSecurityVulnerabilityOverrides() {
    Organization organization = tempEntity.newOrganization("testCascadeDeleteToSecurityVulnerabilityOverrides");
    SecurityVulnerabilityOverride securityVulnerabilityOverride = tempEntity.newSecurityVulnerabilityOverride(
        organization.getId(), "hash", "source", "refrenceId", SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED);

    dao.delete(organization);

    assertThat(securityVulnerabilityOverrideDAO.getById(securityVulnerabilityOverride.getId())).isNull();
  }

  @Test
  public void testDelete_CascadeToPolicyWaivers() {
    Organization organization = tempEntity.newOrganization("testCascadeDeleteToPolicyWaivers");

    Policy policy = tempEntity.newPolicy(organization);
    PolicyWaiver policyWaiver = new PolicyWaiver("12345678901234567890", policy.getId(), organization.getId(),
        "My comment");
    policyWaiverDAO.insert(policyWaiver);
    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getActiveByOwnerId(organization.getId());
    assertThat(policyWaivers).hasSize(1);

    dao.delete(organization);
    policyWaivers = policyWaiverDAO.getActiveByOwnerId(organization.getId());
    assertThat(policyWaivers).isEmpty();
  }

  @Test
  public void testDelete_CascadeToMembershipMappings() {
    Organization organization = tempEntity.newOrganization("testCascadeDeleteToMembershipMappings");

    String roleId = roleDAO.getApplicationRoles().get(0).getId();
    membershipMappingDAO.setMembershipMappingsForContextAndRole(organization.getId(), roleId,
        Collections.singletonList(new MembershipMapping("admin", MemberType.USER)));

    dao.delete(organization);

    assertThat(membershipMappingDAO.getByContextId(organization.getId())).isEmpty();
  }

  @Test
  public void testDelete_CascadeToPolicyMonitoring() {
    Organization organization = tempEntity.newOrganization("testCascadeDeleteToPolicyMonitoring");

    PolicyMonitoring policyMonitoring = new PolicyMonitoring(organization.getId(), Stage.ID_RELEASE);
    policyMonitoringDAO.insert(policyMonitoring);
    assertThat(policyMonitoringDAO.getByOwnerId(organization.getId())).isNotNull();

    dao.delete(organization);

    assertThat(policyMonitoringDAO.getByOwnerId(organization.getId())).isNull();
  }

  @Test
  public void testDelete_CascadeToDataRetentionPolicies() {
    Organization organization = tempEntity.newOrganization();

    dataRetentionPolicyDAO.insert(new DataRetentionPolicy(organization.getId(), "contextId", false, null, null));

    dao.delete(organization);

    assertThat(dataRetentionPolicyDAO.getByOwnerId(organization.getId())).isEmpty();
  }

  @Test
  public void testDelete_CascadeToSourceControl() {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    Organization organization = tempEntity.newOrganization();
    SourceControl sourceControl = tempEntity.newSourceControl(
        organization.getId(), null, "token", null);

    dao.delete(organization);

    assertThat(sourceControlDAO.getById(sourceControl.getId())).isNull();
  }

  @Test
  public void testDelete_CascadeToSourceControlOrganizationImportEvents() {
    Organization org = tempEntity.newOrganization();
    tempEntity.newSourceControlOrganizationImportEvent(org.getId(), "scm-url", -1, 0);

    dao.delete(org);
    assertThat(sourceControlOrganizationImportEventDAO.getByOrganizationId(org.getId())).isEmpty();
  }

  @Test
  public void testDelete_AutomaticApplicationsCreationDisabled_SameOrganizationId() {
    Organization organization = tempEntity.newOrganization("organization");

    String organizationId = organization.getId();

    automaticApplicationsConfigurationDAO.setEnabled(false);
    automaticApplicationsConfigurationDAO.setOrganizationId(organizationId);

    dao.delete(organization);

    assertThat(automaticApplicationsConfigurationDAO.getOrganizationId()).isEqualTo("");
  }

  @Test
  public void testDelete_AutomaticApplicationsCreationDisabled_DifferentOrganizationId() {
    Organization organization = tempEntity.newOrganization("organization");

    automaticApplicationsConfigurationDAO.setEnabled(false);
    automaticApplicationsConfigurationDAO.setOrganizationId("otherOrganizationId");

    dao.delete(organization);

    assertThat(automaticApplicationsConfigurationDAO.getOrganizationId()).isEqualTo("otherOrganizationId");
  }

  @Test
  public void testDelete_AutomaticApplicationsCreationEnabled_SameOrganizationId() {
    Organization organization = tempEntity.newOrganization("organization");

    String organizationId = organization.getId();

    automaticApplicationsConfigurationDAO.setEnabled(true);
    automaticApplicationsConfigurationDAO.setOrganizationId(organizationId);

    assertThatThrownBy(() -> dao.delete(organization)).isInstanceOf(BadRequestException.class).hasMessage(
        "Cannot delete the parent organization for automatic application creation: " + organization.getName() + ".");

    automaticApplicationsConfigurationDAO.setOrganizationId("");
    dao.delete(organization);
  }

  @Test
  public void testDelete_AutomaticApplicationsCreationEnabled_DifferentOrganizationId() {
    Organization organization = tempEntity.newOrganization("organization");

    automaticApplicationsConfigurationDAO.setEnabled(true);
    automaticApplicationsConfigurationDAO.setOrganizationId("otherOrganizationId");

    dao.delete(organization);

    assertThat(automaticApplicationsConfigurationDAO.getOrganizationId()).isEqualTo("otherOrganizationId");
  }

  @Test
  public void testCRUD_RecordSearchIndexChange() {
    systemConfigurationPropertyDAO
        .update(new SystemConfigurationProperty(SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED, "true"));
    Organization org = tempEntity.newOrganization();

    List<SearchIndexChange> searchIndexChanges = searchIndexChangeDAO.getAll();
    assertThat(searchIndexChanges).hasSize(1);
    assertThat(searchIndexChanges.get(0).getChangeType()).isEqualTo(ChangeType.ORGANIZATION);
    assertThat(searchIndexChanges.get(0).getChangeData()).isEqualTo(org.getId());
    searchIndexChangeDAO.delete(searchIndexChanges.get(0));

    dao.update(org);
    searchIndexChanges = searchIndexChangeDAO.getAll();
    assertThat(searchIndexChanges).hasSize(1);
    assertThat(searchIndexChanges.get(0).getChangeType()).isEqualTo(ChangeType.ORGANIZATION);
    assertThat(searchIndexChanges.get(0).getChangeData()).isEqualTo(org.getId());
    searchIndexChangeDAO.delete(searchIndexChanges.get(0));

    dao.delete(org);
    searchIndexChanges = searchIndexChangeDAO.getAll();
    assertThat(searchIndexChanges).hasSize(1);
    assertThat(searchIndexChanges.get(0).getChangeType()).isEqualTo(ChangeType.ORGANIZATION);
    assertThat(searchIndexChanges.get(0).getChangeData()).isEqualTo(org.getId());
  }

  @Test
  public void testDelete_CascadeToLocks_H2() {
    // Lock for audit json file store
    Organization organization = tempEntity.newOrganization();
    try (ClusterLock clusterLock = clusterLockManager.createForAuditJsonFileStore(organization.getId())) {
      clusterLock.lock();
    }

    assertThat(clusterLockManager).isInstanceOf(H2ClusterLockManager.class);
    assertThat(clusterLockManager.lockExists(
        ClusterLockManager.getLockIdForAuditJsonFileStore(organization.getId()))).isTrue();

    dao.delete(organization);

    assertThat(clusterLockManager.lockExists(
        ClusterLockManager.getLockIdForAuditJsonFileStore(organization.getId()))).isFalse();
  }

  @Test
  @PostgresTest
  public void testDelete_CascadeToLocks_Postgres() {
    Organization organization = tempEntity.newOrganization();

    // Lock for audit json file store
    try (ClusterLock clusterLock = clusterLockManager.createForAuditJsonFileStore(organization.getId())) {
      clusterLock.lock();
    }

    assertThat(clusterLockManager).isInstanceOf(PostgresClusterLockManager.class);
    assertThat(clusterLockManager.lockExists(
        ClusterLockManager.getLockIdForAuditJsonFileStore(organization.getId()))).isTrue();

    dao.delete(organization);

    assertThat(clusterLockManager.lockExists(
        ClusterLockManager.getLockIdForAuditJsonFileStore(organization.getId()))).isFalse();
  }

  @Test
  public void testDelete_CascadesToRepositoryConnections() {
    Organization organization = tempEntity.newOrganization();
    RepositoryConnection repositoryConnection = tempEntity.newRepositoryConnection(organization.getId());

    dao.delete(organization);

    assertThat(repositoryConnectionDAO.getById(repositoryConnection.getId())).isNull();
  }

  @Test
  public void testGetByParentOrganizationId() {
    // Create a few orgs
    int orgCount = 3;
    tempEntity.newOrganizations(orgCount);

    // getAll should return orgCount + 1, to account for org created by AbstractDbDAOTest
    assertThat(dao.getByParentOrganizationId(ROOT_ORGANIZATION_ID)).hasSize(orgCount + 1);
  }

  @Test
  public void testDelete_CascadeToVulnerabilityCustomRemediation() {
    Organization organization = tempEntity.newOrganization();
    tempEntity.newVulnerabilityCustomData(organization.getId(), "CVE-2022-1234",
        tempEntity.newTag(organization.getId()), "rem1",
        "testCWE", "testCvssVector1", 6.05F);
    tempEntity.newVulnerabilityCustomData(organization.getId(), "CVE-2022-4321",
        tempEntity.newTag(organization.getId()), "rem2",
        "testCWE", "testCvssVector2", 6.05F);

    List<VulnerabilityCustomRemediation> vulnerabilityCustomRemediationList =
        vulnerabilityCustomRemediationDAO.getByOwnerId(organization.getId());
    assertThat(vulnerabilityCustomRemediationList).extracting(VulnerabilityCustomRemediation::getRefId)
        .containsExactlyInAnyOrder("CVE-2022-1234", "CVE-2022-4321");
    dao.delete(organization);
    vulnerabilityCustomRemediationList =
        vulnerabilityCustomRemediationDAO.getByOwnerId(organization.getId());
    assertThat(vulnerabilityCustomRemediationList).isEmpty();
  }

  @Test
  public void testDelete_CascadeToVulnerabilityCustomCwe() {
    Organization organization = tempEntity.newOrganization();
    tempEntity.newVulnerabilityCustomData(organization.getId(), "CVE-2022-1234",
        tempEntity.newTag(organization.getId()), "rem1",
        "testCWE", "testCvssVector1", 6.05F);
    tempEntity.newVulnerabilityCustomData(organization.getId(), "CVE-2022-4321",
        tempEntity.newTag(organization.getId()), "rem1",
        "testCWE", "testCvssVector2", 6.05F);

    List<VulnerabilityCustomCwe> vulnerabilityCustomCweList = vulnerabilityCustomCweDAO
        .getByOwnerId(organization.getId());
    assertThat(vulnerabilityCustomCweList).extracting(VulnerabilityCustomCwe::getRefId)
        .containsExactlyInAnyOrder("CVE-2022-1234", "CVE-2022-4321");
    dao.delete(organization);
    assertThat(vulnerabilityCustomCweDAO.getByOwnerId(organization.getId())).isEmpty();

  }

  @Test
  public void testDelete_CascadeToCVSSVector() {
    Organization organization = tempEntity.newOrganization();
    tempEntity.newVulnerabilityCustomData(organization.getId(), "CVE-2022-1234",
        tempEntity.newTag(organization.getId()), "rem1",
        "testCWE", "testCvssVector1", 6.05F);
    tempEntity.newVulnerabilityCustomData(organization.getId(), "CVE-2022-4321",
        tempEntity.newTag(organization.getId()), "rem1",
        "testCWE", "testCvssVector2", 6.05F);

    List<VulnerabilityCustomCvssVector> vulnerabilityCustomCvssVectorList =
        vulnerabilityCustomCvssVectorDAO.getByOwnerId(organization.getId());
    assertThat(vulnerabilityCustomCvssVectorList).extracting(VulnerabilityCustomCvssVector::getRefId)
        .containsExactlyInAnyOrder("CVE-2022-1234", "CVE-2022-4321");
    dao.delete(organization);
    assertThat(vulnerabilityCustomCvssVectorDAO.getByOwnerId(organization.getId())).isEmpty();
  }

  @Test
  public void testDelete_CascadeToCVSSSeverity() {
    Organization organization = tempEntity.newOrganization();
    tempEntity.newVulnerabilityCustomData(organization.getId(), "CVE-2022-1234",
        tempEntity.newTag(organization.getId()), "rem1",
        "testCWE", "testCvssVector1", 6.05F);
    tempEntity.newVulnerabilityCustomData(organization.getId(), "CVE-2022-4321",
        tempEntity.newTag(organization.getId()), "rem1",
        "testCWE", "testCvssVector2", 6.05F);

    List<VulnerabilityCustomCvssSeverity> vulnerabilityCustomCvssSeverityList =
        vulnerabilityCustomCvssSeverityDAO.getByOwnerId(organization.getId());
    assertThat(vulnerabilityCustomCvssSeverityList).extracting(VulnerabilityCustomCvssSeverity::getRefId)
        .containsExactlyInAnyOrder("CVE-2022-1234", "CVE-2022-4321");
    dao.delete(organization);
    assertThat(vulnerabilityCustomCvssSeverityDAO.getByOwnerId(organization.getId())).isEmpty();
  }
}
