/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticApplicationsConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.CiIntegrationsConfigDao;
import com.sonatype.insight.brain.dataaccess.configuration.CpeMatchingConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.DataRetentionPolicyDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProprietaryConfigDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ScanHealthConfigDAO;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.configuration.VersionEvaluationWindowDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverRequestDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryConnectionDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.ScmUserMappingsDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlOrganizationImportEventDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCvssSeverityDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCvssVectorDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCweDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomRemediationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OrganizationAncestor;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.SearchIndexChange.ChangeType;
import com.sonatype.insight.brain.model.configuration.CiIntegrationsConfig;
import com.sonatype.insight.brain.model.configuration.CpeMatchingConfiguration;
import com.sonatype.insight.brain.model.configuration.DataRetentionPolicy;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.scanhealth.ScanHealthConfig;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.sourcecontrol.ScmUserMappings;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCvssSeverity;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCvssVector;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCwe;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomRemediation;
import com.sonatype.insight.brain.utils.ScmUserMappingsBuilder;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.utils.ScmUserMappingsHelper.getRandomMappings;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class OrganizationDAOTest
    extends NameableDAOTest<Organization>
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

  private OrganizationAncestorDAO orgAncestorDAO;

  private ScmUserMappingsDAO scmUserMappingsDAO;

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
    orgAncestorDAO = daoFactory.createOrganizationAncestorDAO();
    scmUserMappingsDAO = daoFactory.createScmUserMappingsDAO();
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
    tempEntity.newOrganizationWithRepositoryManager("org-with-repo-manager");

    List<Organization> orgs = dao.getAll();

    // The getAll method should have size: orgCount + 3
    // 1. One for the organization with a related repository manager.
    // 2. One for organization created by AbstractDbDAOTest.
    // 3. One for the root organization.
    assertThat(orgs).hasSize(orgCount + 3);
    assertThat(orgs).extracting(Organization::getName)
        .contains("org-with-repo-manager");
  }

  @Test
  public void testGetAllWithoutRelatedRepositories() {
    // Create a few orgs
    int orgCount = 3;

    tempEntity.newOrganizations(orgCount);
    tempEntity.newOrganizationWithRepositoryManager("org-with-repo");
    Organization organizationForRepositoryContainer =
        tempEntity.newOrganizationWithRepositoryManager("org-for-repo-container");
    organizationForRepositoryContainer.setRelatedRepositoryContainerId(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    dao.update(organizationForRepositoryContainer);

    List<Organization> orgs = dao.getAllWithoutRelatedRepositories();

    // getAllWithoutRelatedRepositories should have size: orgCount + 2
    // 1. One for the organization created by AbstractDbDAOTest.
    // 2. One for the root organization.
    assertThat(orgs).hasSize(orgCount + 2);
    assertThat(orgs).extracting(Organization::getName)
        .doesNotContain("org-with-repo")
        .doesNotContain(organizationForRepositoryContainer.getName());
    assertThat(orgs).extracting(Organization::getRelatedRepositoryManagerId)
        .allMatch(Objects::isNull);
    assertThat(orgs).extracting(Organization::getRelatedRepositoryId)
        .allMatch(Objects::isNull);
  }

  @Test
  public void testGetByRelatedRepositoryManagerId() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();

    Organization org1 = tempEntity.newOrganization("org1");
    Organization org2 = tempEntity.newOrganization("org2");
    tempEntity.newOrganization("org3");
    org1.setRelatedRepositoryManagerId(repositoryManager.getId());
    org2.setRelatedRepositoryManagerId(repositoryManager.getId());
    dao.update(org1);
    dao.update(org2);

    List<Organization> result = dao.getByRelatedRepositoryManagerId(repositoryManager.getId());

    assertThat(result).extracting(Organization::getId).containsExactly(org1.getId(), org2.getId());
  }

  @Test
  public void testGetByRelatedRepositoryId() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "repository");

    Organization org1 = tempEntity.newOrganization("org1");
    Organization org2 = tempEntity.newOrganization("org2");
    tempEntity.newOrganization("org3");

    org1.setRelatedRepositoryId(repository.getId());
    org2.setRelatedRepositoryId(repository.getId());

    dao.update(org1);
    dao.update(org2);

    List<Organization> result = dao.getByRelatedRepositoryId(repository.getId());
    assertThat(result).extracting(Organization::getId).containsExactly(org1.getId(), org2.getId());
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
    Organization org4 = tempEntity.newOrganizationWithRepositoryManager("org-with-repo-manager");

    List<Organization> orgs = dao.getByNames(
        Sets.newHashSet(org1.getName(), org3.getName(), org4.getName()));
    assertThat(orgs).extracting(Organization::getId).containsExactly(org4.getId(), org1.getId(), org3.getId());
  }

  @Test
  public void testGetByNamesAndWithoutRelatedRepositories() {
    Organization org1 = tempEntity.newOrganization("org1");
    tempEntity.newOrganization("org2");
    Organization org3 = tempEntity.newOrganization("org3");
    Organization org4 = tempEntity.newOrganizationWithRepositoryManager("org-with-repo-manager");

    List<Organization> orgs = dao.getByNamesAndWithoutRelatedRepositories(
        Sets.newHashSet(org3.getName(), org1.getName(), org4.getName()));
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
  public void testGetByIds() {
    Organization org1 = tempEntity.newOrganization("org1");
    tempEntity.newOrganization("org2");

    List<Organization> orgs = dao.getByIds(Collections.singletonList(org1.getId()));
    assertThat(orgs).usingRecursiveFieldByFieldElementComparator().containsExactly(org1);
  }

  @Test
  public void testGetByIds_MultipleIds() {
    Organization org1 = tempEntity.newOrganization("org1");
    Organization org2 = tempEntity.newOrganization("org2");
    Organization org3 = tempEntity.newOrganization("org3");

    List<Organization> orgs = dao.getByIds(Arrays.asList(org1.getId(), org2.getId(), org3.getId()));
    assertThat(orgs).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(org1, org2, org3);
  }

  @Test
  public void testGetByIds_EmptyCollection() {
    assertThat(dao.getByIds(Collections.emptyList())).isEmpty();
    assertThat(dao.getByIds(null)).isEmpty();
  }

  @Test
  public void testGetByIds_SomeNotFound() {
    Organization org1 = tempEntity.newOrganization("org1");

    List<Organization> orgs = dao.getByIds(Arrays.asList(org1.getId(), "non-existent-id-1", "non-existent-id-2"));
    assertThat(orgs).usingRecursiveFieldByFieldElementComparator().containsExactly(org1);
  }

  @Test
  public void testGetByIds_NotFound() {
    assertThat(dao.getByIds(Collections.singletonList("non-existent-id"))).isEmpty();
  }

  @Test
  public void testGetByIds_Batched() {
    Organization org1 = tempEntity.newOrganization("org1");
    Organization org2 = tempEntity.newOrganization("org2");
    Organization org3 = tempEntity.newOrganization("org3");

    // Spy the DAO to force batching by lowering the IN operator threshold
    // Use doReturn().when() syntax to stub the 0-arg method which is called by the internal
    // getListWithSqlInClause -> getInOperatorThreshold(dataStore) chain
    OrganizationDAO spiedDao = spy(dao);
    when(spiedDao.getInOperatorThreshold()).thenReturn(2);

    List<Organization> orgs = spiedDao.getByIds(Arrays.asList(org1.getId(), org2.getId(), org3.getId()));
    assertThat(orgs).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(org1, org2, org3);
  }

  @Test
  public void selectCountByOrganizationIds_emptyReturnsZero() {
    assertThat(dao.selectCountByOrganizationIds(Collections.emptySet())).isZero();
  }

  @Test
  public void selectCountByOrganizationIds_nullCountsGlobal() {
    long before = dao.selectCountByOrganizationIds(null);
    tempEntity.newOrganization("count-global");

    assertThat(dao.selectCountByOrganizationIds(null)).isEqualTo(before + 1);
  }

  @Test
  public void selectCountByOrganizationIds_countsOnlyRequestedIds() {
    Organization included = tempEntity.newOrganization("count-included");
    tempEntity.newOrganization("count-excluded");

    assertThat(dao.selectCountByOrganizationIds(Set.of(included.getId()))).isEqualTo(1);
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
    dao.insert(organization);
    organization = dao.getById("testId");
    assertThat(organization.getParentOrganizationId()).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testInsert_ParentOrganizationIdIsNotRoot() {
    Organization parentOrg = tempEntity.newOrganization("Test Parent Org");
    organization = new Organization();
    organization.setName("testName");
    organization.setParentOrganizationId(parentOrg.getId());

    dao.insert(organization);
    organization = dao.getById(organization.getId());
    assertThat(organization.getParentOrganizationId()).isEqualTo(parentOrg.getId());
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
  public void testInsert_OrganizationAncestorRecords() {
    Organization parentOrg = tempEntity.newOrganization("Test Parent Org");
    organization = new Organization();
    organization.setName("testName");
    organization.setParentOrganizationId(parentOrg.getId());

    dao.insert(organization);

    assertThat(orgAncestorDAO.getByOrganizationId(organization.getId()))
        .extracting("organizationId", "ancestorId", "ancestorDistance")
        .contains(
            tuple(organization.getId(), organization.getId(), 0),
            tuple(organization.getId(), parentOrg.getId(), 1),
            tuple(organization.getId(), Organization.ROOT_ORGANIZATION_ID, 2));
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
  public void testUpdate_OrganizationAncestorRecords() {
    Organization parentOrg1 = tempEntity.newOrganization("Test Parent Org 1");
    Organization parentOrg2 = tempEntity.newOrganization("Test Parent Org 2");

    organization = new Organization();
    organization.setName("testName");
    organization.setParentOrganizationId(parentOrg1.getId());
    dao.insert(organization);

    Organization sibling = tempEntity.newOrganization(parentOrg1);
    Organization childOrg1 = tempEntity.newOrganization(organization);
    Organization childOrg2 = tempEntity.newOrganization(organization);
    Organization grandchildOrg = tempEntity.newOrganization(childOrg1);

    organization.setParentOrganizationId(parentOrg2.getId());
    dao.update(organization);

    assertThat(orgAncestorDAO.getByOrganizationId(organization.getId()))
        .extracting("organizationId", "ancestorId", "ancestorDistance")
        .contains(
            tuple(organization.getId(), organization.getId(), 0),
            tuple(organization.getId(), parentOrg2.getId(), 1),
            tuple(organization.getId(), Organization.ROOT_ORGANIZATION_ID, 2));

    assertThat(orgAncestorDAO.getByOrganizationId(childOrg1.getId()))
        .extracting("organizationId", "ancestorId", "ancestorDistance")
        .contains(
            tuple(childOrg1.getId(), childOrg1.getId(), 0),
            tuple(childOrg1.getId(), organization.getId(), 1),
            tuple(childOrg1.getId(), parentOrg2.getId(), 2),
            tuple(childOrg1.getId(), Organization.ROOT_ORGANIZATION_ID, 3));

    assertThat(orgAncestorDAO.getByOrganizationId(childOrg2.getId()))
        .extracting("organizationId", "ancestorId", "ancestorDistance")
        .contains(
            tuple(childOrg2.getId(), childOrg2.getId(), 0),
            tuple(childOrg2.getId(), organization.getId(), 1),
            tuple(childOrg2.getId(), parentOrg2.getId(), 2),
            tuple(childOrg2.getId(), Organization.ROOT_ORGANIZATION_ID, 3));

    assertThat(orgAncestorDAO.getByOrganizationId(grandchildOrg.getId()))
        .extracting("organizationId", "ancestorId", "ancestorDistance")
        .contains(
            tuple(grandchildOrg.getId(), grandchildOrg.getId(), 0),
            tuple(grandchildOrg.getId(), childOrg1.getId(), 1),
            tuple(grandchildOrg.getId(), organization.getId(), 2),
            tuple(grandchildOrg.getId(), parentOrg2.getId(), 3),
            tuple(grandchildOrg.getId(), Organization.ROOT_ORGANIZATION_ID, 4));

    // Other org ancestors should be unchanged
    assertThat(orgAncestorDAO.getByOrganizationId(sibling.getId()))
        .extracting("organizationId", "ancestorId", "ancestorDistance")
        .contains(
            tuple(sibling.getId(), sibling.getId(), 0),
            tuple(sibling.getId(), parentOrg1.getId(), 1),
            tuple(sibling.getId(), Organization.ROOT_ORGANIZATION_ID, 2));

    assertThat(orgAncestorDAO.getByOrganizationId(parentOrg1.getId()))
        .extracting("organizationId", "ancestorId", "ancestorDistance")
        .contains(
            tuple(parentOrg1.getId(), parentOrg1.getId(), 0),
            tuple(parentOrg1.getId(), Organization.ROOT_ORGANIZATION_ID, 1));

    assertThat(orgAncestorDAO.getByOrganizationId(parentOrg2.getId()))
        .extracting("organizationId", "ancestorId", "ancestorDistance")
        .contains(
            tuple(parentOrg2.getId(), parentOrg2.getId(), 0),
            tuple(parentOrg2.getId(), Organization.ROOT_ORGANIZATION_ID, 1));

    assertThat(orgAncestorDAO.getByOrganizationId(Organization.ROOT_ORGANIZATION_ID))
        .extracting("organizationId", "ancestorId", "ancestorDistance")
        .contains(tuple(Organization.ROOT_ORGANIZATION_ID, Organization.ROOT_ORGANIZATION_ID, 0));
  }

  @Test
  public void testUpdate_OrganizationAncestorRecords_ParentOrgUnchanged() {
    // If the parent organization is unchanged, the OrganizationAncestor records should not change (not even their IDs).
    List<OrganizationAncestor> orgAncestorsBefore = orgAncestorDAO.getByOrganizationId(organization.getId());

    organization.setName("New name");
    dao.update(organization);

    List<OrganizationAncestor> orgAncestorsAfter = orgAncestorDAO.getByOrganizationId(organization.getId());
    JPA.assertContainsEntitiesExactlyElementsOf(orgAncestorsBefore, orgAncestorsAfter);
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
        organization.getId(), "hash", "source", "referenceId", SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED);

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

    // sanity check
    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getByOwnerId(organization.getId());
    assertThat(policyWaivers).hasSize(1);

    dao.delete(organization);
    policyWaivers = policyWaiverDAO.getByOwnerId(organization.getId());
    assertThat(policyWaivers).isEmpty();
  }

  @Test
  public void testDelete_CascadeToPolicyWaiverRequests() {
    Organization organization = tempEntity.newOrganization();

    Policy policy = tempEntity.newPolicy(organization);
    PolicyWaiverRequest policyWaiverRequest =
        new PolicyWaiverRequest("12345678901234567890", policy.getId(), organization.getId(), "My comment");
    policyWaiverRequest.setPolicyViolationId("policyViolationId");
    tempEntity.newPolicyWaiverRequest(policyWaiverRequest);

    // sanity check
    PolicyWaiverRequestDAO policyWaiverRequestDAO = daoFactory.createPolicyWaiverRequestDAO();
    List<PolicyWaiverRequest> policyWaiverRequests = policyWaiverRequestDAO.getByOwnerId(organization.getId());
    assertThat(policyWaiverRequests).hasSize(1);

    dao.delete(organization);
    policyWaiverRequests = policyWaiverRequestDAO.getByOwnerId(organization.getId());
    assertThat(policyWaiverRequests).isEmpty();
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
  public void testDelete_CascadeToScmUserMappings() {
    Organization organization = tempEntity.newOrganization("testCascadeDeleteToUserMappings");

    List<Entry<String, String>> mappings = getRandomMappings();

    ScmUserMappings scmUserMappings = new ScmUserMappingsBuilder()
        .withId()
        .withRoleId(Role.DEVELOPER_ROLE_ID)
        .withMappings(mappings)
        .withOrganizationId(organization.getId())
        .build();

    scmUserMappingsDAO.addOrUpdate(scmUserMappings);

    dao.delete(organization);

    assertThat(scmUserMappingsDAO.getByOrganizationId(organization.getId())).isNull();
  }

  @Test
  public void testDelete_CascadeToPolicyMonitoring() {
    Organization organization = tempEntity.newOrganization("testCascadeDeleteToPolicyMonitoring");

    PolicyMonitoring policyMonitoring = new PolicyMonitoring(organization.getId(), Stage.ID_RELEASE);
    policyMonitoringDAO.insert(policyMonitoring);
    List<String> orgs = new ArrayList<>();
    orgs.add(organization.getId());
    assertThat(policyMonitoringDAO.getByOwnerId(organization.getId()))
        .isNotEmpty()
        .hasSize(1)
        .extracting("ownerId")
        .isEqualTo(orgs);

    dao.delete(organization);

    assertThat(policyMonitoringDAO.getByOwnerId(organization.getId())).isEmpty();
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
  public void testDelete_CascadesToAutoPolicyWaivers() {
    Organization organization = tempEntity.newOrganization("testCascadeDeleteToAutoPolicyWaivers");
    AutoPolicyWaiver autoPolicyWaiverOne = new AutoPolicyWaiver(
        organization.getId(),
        7,
        true,
        true,
        "creatorId",
        "creatorName",
        new Date());

    AutoPolicyWaiver autoPolicyWaiverTwo = new AutoPolicyWaiver(
        organization.getId(),
        7,
        true,
        true,
        "creatorId",
        "creatorName",
        new Date());

    AutoPolicyWaiver autoPolicyWaiverThree = new AutoPolicyWaiver(
        organization.getId(),
        7,
        true,
        true,
        "creatorId",
        "creatorName",
        new Date());

    AutoPolicyWaiver autoPolicyWaiverFour = new AutoPolicyWaiver(
        "otherOrg",
        7,
        true,
        true,
        "creatorId",
        "creatorName",
        new Date());

    AutoPolicyWaiver autoPolicyWaiverFive = new AutoPolicyWaiver(
        "otherOrg",
        7,
        true,
        true,
        "creatorId",
        "creatorName",
        new Date());

    AutoPolicyWaiverDAO autoPolicyWaiverDAO = daoFactory.createAutoPolicyWaiverDAO();
    autoPolicyWaiverDAO.insert(autoPolicyWaiverOne);
    autoPolicyWaiverDAO.insert(autoPolicyWaiverTwo);
    autoPolicyWaiverDAO.insert(autoPolicyWaiverThree);
    autoPolicyWaiverDAO.insert(autoPolicyWaiverFour);
    autoPolicyWaiverDAO.insert(autoPolicyWaiverFive);

    List<AutoPolicyWaiver> testOrgAutoPolicyWaivers = autoPolicyWaiverDAO.getByOwnerId(organization.getId());
    assertThat(testOrgAutoPolicyWaivers).hasSize(3);

    dao.delete(organization);
    testOrgAutoPolicyWaivers = autoPolicyWaiverDAO.getByOwnerId(organization.getId());
    assertThat(testOrgAutoPolicyWaivers).isEmpty();
    List<AutoPolicyWaiver> otherOrgAutoPolicyWaivers = autoPolicyWaiverDAO.getByOwnerId("otherOrg");
    assertThat(otherOrgAutoPolicyWaivers).hasSize(2);
  }

  @Test
  public void testCascadeDeleteToScanHealthConfig() {
    Organization organization = tempEntity.newOrganization("testCascadeDeleteToScanHealthConfig");

    ScanHealthConfigDAO scanHealthConfigDAO = daoFactory.createScanHealthConfigDAO();
    ScanHealthConfig scanHealthConfig = new ScanHealthConfig(
        organization.getId(),
        OwnerType.ORGANIZATION.toString(),
        "{\"failOnZeroComponents\":true}");
    scanHealthConfigDAO.save(scanHealthConfig);

    // Verify config exists
    assertThat(scanHealthConfigDAO.findByOwner(OwnerType.ORGANIZATION.toString(), organization.getId()))
        .isPresent();

    dao.delete(organization);

    // Verify cascade deletion
    assertThat(scanHealthConfigDAO.findByOwner(OwnerType.ORGANIZATION.toString(), organization.getId()))
        .isEmpty();
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

    assertThatThrownBy(() -> dao.delete(organization)).isInstanceOf(BadRequestException.class)
        .hasMessage(
            "Cannot delete the parent organization for automatic application creation: " + organization.getName()
                + ".");

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
  public void testNewSearchIndexChange_WithRelatedRepositoryManagerOrRepository() {
    Organization orgWithRepoContainer = tempEntity.newOrganization();
    orgWithRepoContainer.setRelatedRepositoryContainerId(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    dao.update(orgWithRepoContainer);
    SearchIndexChange result = dao.newSearchIndexChange(orgWithRepoContainer);
    assertThat(result).isNull();

    Organization orgWithRepoManager = tempEntity.newOrganizationWithRepositoryManager("org-with-repo-manager");
    result = dao.newSearchIndexChange(orgWithRepoManager);
    assertThat(result).isNull();

    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "repository");
    Organization orgWithRepo = tempEntity.newOrganization("org-with-repo");
    orgWithRepo.setRelatedRepositoryId(repository.getId());
    dao.update(orgWithRepo);

    result = dao.newSearchIndexChange(orgWithRepo);
    assertThat(result).isNull();

    Organization orgWithoutRepo = tempEntity.newOrganization("org-without-repo");
    result = dao.newSearchIndexChange(orgWithoutRepo);
    assertThat(result).isNotNull();
    assertThat(result.getChangeType()).isEqualTo(ChangeType.ORGANIZATION);
    assertThat(result.getChangeData()).isEqualTo(orgWithoutRepo.getId());
  }

  @Test
  public void testCRUD_RecordSearchIndexChange() {
    systemConfigurationPropertyDAO
        .update(new SystemConfigurationProperty(SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED, "true"));
    Organization org = tempEntity.newOrganization();
    tempEntity.newOrganizationWithRepositoryManager("org-with-repo-man");

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

  @Test
  public void testDelete_CascadeToCpeMatchingConfiguration() {
    Organization organization = tempEntity.newOrganization();
    CpeMatchingConfiguration cpeMatchingConfiguration = new CpeMatchingConfiguration(organization.getId(), true, false);
    CpeMatchingConfigurationDAO cpeMatchingConfigurationDao = daoFactory.createCpeMatchingConfigurationDAO();

    // create
    cpeMatchingConfigurationDao.insert(cpeMatchingConfiguration);

    // delete organization
    dao.delete(organization);

    // verify deletion
    assertThat(cpeMatchingConfigurationDao.getByOwnerId(organization.getId())).isNull();
  }

  @Test
  public void testDelete_CascadesToCiIntegrationsConfig() {
    Organization organization = tempEntity.newOrganization();
    CiIntegrationsConfig ciIntegrationsConfig = new CiIntegrationsConfig(organization.getId(), "ORGANIZATION", "");
    CiIntegrationsConfigDao ciIntegrationsConfigDao = daoFactory.createCiIntegrationsConfigDao();

    // create
    ciIntegrationsConfigDao.save(ciIntegrationsConfig);

    // delete organization
    dao.delete(organization);

    // verify deletion
    assertThat(ciIntegrationsConfigDao.findByOwner("ORGANIZATION", organization.getId())).isEmpty();
  }

  @Test
  public void testDelete_CascadeToOrganizationAncestor() {
    Organization parent = tempEntity.newOrganization();
    Organization child = tempEntity.newOrganization("child", parent);
    String childId = child.getId();

    assertThat(orgAncestorDAO.getByOrganizationId(childId)).hasSize(3);

    dao.delete(child);

    assertThat(orgAncestorDAO.getByOrganizationId(childId)).isEmpty();
  }

  @Test
  public void testDelete_CascadesToVersionEvaluationWindows() {
    Organization organization = tempEntity.newOrganization();
    Organization other = tempEntity.newOrganization();
    tempEntity.newVersionEvaluationWindow(organization.getId(), "contextId1", 1, 1);
    tempEntity.newVersionEvaluationWindow(organization.getId(), "contextId2", 1, 1);
    tempEntity.newVersionEvaluationWindow(other.getId(), "contextId1", 1, 1);
    tempEntity.newVersionEvaluationWindow(other.getId(), "contextId2", 1, 1);

    dao.delete(organization);

    VersionEvaluationWindowDAO versionEvaluationWindowDAO = daoFactory.createVersionEvaluationWindowDAO();
    assertThat(versionEvaluationWindowDAO.getByOwnerId(organization.getId())).isEmpty();
    assertThat(versionEvaluationWindowDAO.getByOwnerId(other.getId())).isNotEmpty();
  }

  @Test
  public void testGetAllParentOrganizations() {
    assertThat(dao.getAllParentOrganizations(application.getId(), OwnerType.APPLICATION))
        .extracting(Organization::getId)
        .containsExactly(organization.getId(), ROOT_ORGANIZATION_ID);

    assertThat(dao.getAllParentOrganizations(organization.getId(), OwnerType.ORGANIZATION))
        .extracting(Organization::getId)
        .containsExactly(organization.getId(), ROOT_ORGANIZATION_ID);

    assertThat(dao.getAllParentOrganizations(ROOT_ORGANIZATION_ID, OwnerType.ORGANIZATION))
        .extracting(Organization::getId)
        .containsExactly(ROOT_ORGANIZATION_ID);

    Organization org1 = tempEntity.newOrganization("org-1");
    Application app11 = tempEntity.newApplication(org1.getId());

    Organization org11 = tempEntity.newOrganization("org-1-1", org1);
    Application app111 = tempEntity.newApplication(org11.getId());

    Organization org2 = tempEntity.newOrganization("org-2");
    Application app21 = tempEntity.newApplication(org2.getId());

    assertThat(dao.getAllParentOrganizations(org1.getId(), OwnerType.ORGANIZATION))
        .extracting(Organization::getId)
        .containsExactly(org1.getId(), ROOT_ORGANIZATION_ID);

    assertThat(dao.getAllParentOrganizations(app11.getId(), OwnerType.APPLICATION))
        .extracting(Organization::getId)
        .containsExactly(org1.getId(), ROOT_ORGANIZATION_ID);

    assertThat(dao.getAllParentOrganizations(org11.getId(), OwnerType.ORGANIZATION))
        .extracting(Organization::getId)
        .containsExactly(org11.getId(), org1.getId(), ROOT_ORGANIZATION_ID);

    assertThat(dao.getAllParentOrganizations(app111.getId(), OwnerType.APPLICATION))
        .extracting(Organization::getId)
        .containsExactly(org11.getId(), org1.getId(), ROOT_ORGANIZATION_ID);

    assertThat(dao.getAllParentOrganizations(org2.getId(), OwnerType.ORGANIZATION))
        .extracting(Organization::getId)
        .containsExactly(org2.getId(), ROOT_ORGANIZATION_ID);

    assertThat(dao.getAllParentOrganizations(app21.getId(), OwnerType.APPLICATION))
        .extracting(Organization::getId)
        .containsExactly(org2.getId(), ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testGetAllParentOrganizations_NullOwnerType() {
    // the OwnerType parameter is optional and can be left null if unknown (performance is worse this way though)
    assertThat(dao.getAllParentOrganizations(application.getId(), null))
        .extracting(Organization::getId)
        .containsExactly(organization.getId(), ROOT_ORGANIZATION_ID);

    assertThat(dao.getAllParentOrganizations(organization.getId(), null))
        .extracting(Organization::getId)
        .containsExactly(organization.getId(), ROOT_ORGANIZATION_ID);

    assertThat(dao.getAllParentOrganizations(ROOT_ORGANIZATION_ID, null))
        .extracting(Organization::getId)
        .containsExactly(ROOT_ORGANIZATION_ID);

    Organization org1 = tempEntity.newOrganization("org-1");
    Application app11 = tempEntity.newApplication(org1.getId());

    Organization org11 = tempEntity.newOrganization("org-1-1", org1);
    Application app111 = tempEntity.newApplication(org11.getId());

    Organization org2 = tempEntity.newOrganization("org-2");
    Application app21 = tempEntity.newApplication(org2.getId());

    assertThat(dao.getAllParentOrganizations(org1.getId(), null))
        .extracting(Organization::getId)
        .containsExactly(org1.getId(), ROOT_ORGANIZATION_ID);

    assertThat(dao.getAllParentOrganizations(app11.getId(), null))
        .extracting(Organization::getId)
        .containsExactly(org1.getId(), ROOT_ORGANIZATION_ID);

    assertThat(dao.getAllParentOrganizations(org11.getId(), null))
        .extracting(Organization::getId)
        .containsExactly(org11.getId(), org1.getId(), ROOT_ORGANIZATION_ID);

    assertThat(dao.getAllParentOrganizations(app111.getId(), null))
        .extracting(Organization::getId)
        .containsExactly(org11.getId(), org1.getId(), ROOT_ORGANIZATION_ID);

    assertThat(dao.getAllParentOrganizations(org2.getId(), null))
        .extracting(Organization::getId)
        .containsExactly(org2.getId(), ROOT_ORGANIZATION_ID);

    assertThat(dao.getAllParentOrganizations(app21.getId(), null))
        .extracting(Organization::getId)
        .containsExactly(org2.getId(), ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testGetAllParentOrganizations_ByList() {
    final var org1 = tempEntity.newOrganization("org-1");
    final var org2 = tempEntity.newOrganization("org-1-1", org1);
    final var org3 = tempEntity.newOrganization("org-1-2", org2);
    Application app1 = tempEntity.newApplication(org3.getId());
    Application app2 = tempEntity.newApplication(org2.getId());

    final var org4 = tempEntity.newOrganization("org-2");
    final var org5 = tempEntity.newOrganization("org-2-1", org4);
    final var org6 = tempEntity.newOrganization("org-2-2", org5);
    Application app3 = tempEntity.newApplication(org6.getId());

    // === For App Orgs ==
    var results = dao.getAllParentOrganizations(
        Lists.newArrayList(org3.getId()),
        OwnerType.ORGANIZATION);

    assertThat(results.stream().map(Organization::getId).collect(Collectors.toSet()))
        .containsExactlyInAnyOrder(ROOT_ORGANIZATION_ID, org1.getId(), org2.getId(), org3.getId());

    results = dao.getAllParentOrganizations(
        Lists.newArrayList(org6.getId()),
        OwnerType.ORGANIZATION);

    assertThat(results.stream().map(Organization::getId).collect(Collectors.toSet()))
        .containsExactlyInAnyOrder(ROOT_ORGANIZATION_ID, org6.getId(), org5.getId(), org4.getId());

    results = dao.getAllParentOrganizations(
        Lists.newArrayList(org3.getId(), org6.getId()),
        OwnerType.ORGANIZATION);

    assertThat(results.stream().map(Organization::getId).collect(Collectors.toSet()))
        .containsExactlyInAnyOrder(
            ROOT_ORGANIZATION_ID,
            org1.getId(), org2.getId(), org3.getId(),
            org6.getId(), org5.getId(), org4.getId());

    results = dao.getAllParentOrganizations(
        Lists.newArrayList(org2.getId(), org4.getId()),
        OwnerType.ORGANIZATION);

    assertThat(results.stream().map(Organization::getId).collect(Collectors.toSet()))
        .containsExactlyInAnyOrder(ROOT_ORGANIZATION_ID, org1.getId(), org2.getId(), org4.getId());

    // === For App Ids ==
    results = dao.getAllParentOrganizations(
        Lists.newArrayList(app1.getId()),
        OwnerType.APPLICATION);

    assertThat(results.stream().map(Organization::getId).collect(Collectors.toSet()))
        .containsExactlyInAnyOrder(ROOT_ORGANIZATION_ID, org1.getId(), org2.getId(), org3.getId());

    results = dao.getAllParentOrganizations(
        Lists.newArrayList(app2.getId()),
        OwnerType.APPLICATION);

    assertThat(results.stream().map(Organization::getId).collect(Collectors.toSet()))
        .containsExactlyInAnyOrder(ROOT_ORGANIZATION_ID, org1.getId(), org2.getId());

    results = dao.getAllParentOrganizations(
        Lists.newArrayList(app2.getId(), app3.getId()),
        OwnerType.APPLICATION);

    assertThat(results.stream().map(Organization::getId).collect(Collectors.toSet()))
        .containsExactlyInAnyOrder(
            ROOT_ORGANIZATION_ID, org1.getId(), org2.getId(), org6.getId(), org5.getId(), org4.getId());

    // empty if the ids don't match the type provided
    results = dao.getAllParentOrganizations(
        Lists.newArrayList(app1.getId()),
        OwnerType.ORGANIZATION);

    assertThat(results).isEmpty();

    // === No Owner Type Allows Mixed Ids ===
    results = dao.getAllParentOrganizations(
        Lists.newArrayList(app2.getId(), org6.getId()),
        null);

    assertThat(results.stream().map(Organization::getId).collect(Collectors.toSet()))
        .containsExactlyInAnyOrder(
            ROOT_ORGANIZATION_ID, org1.getId(), org2.getId(), org6.getId(), org5.getId(), org4.getId());
  }

  @Test
  public void testGetAllChildOrganizations() {
    assertThat(dao.getAllChildOrganizations(application.getId())).isEmpty();

    assertThat(dao.getAllChildOrganizations(organization.getId()))
        .extracting(Organization::getId)
        .containsExactly(organization.getId());

    assertThat(dao.getAllChildOrganizations(ROOT_ORGANIZATION_ID))
        .extracting(Organization::getId)
        .containsExactly(ROOT_ORGANIZATION_ID, organization.getId());

    Organization org1 = tempEntity.newOrganization("org-1");
    tempEntity.newApplication(org1.getId());

    Organization org11 = tempEntity.newOrganization("org-1-1", org1);
    tempEntity.newApplication(org1.getId());

    Organization org2 = tempEntity.newOrganization("org-2");
    tempEntity.newApplication(org2.getId());

    assertThat(dao.getAllChildOrganizations(org1.getId()))
        .extracting(Organization::getId)
        .containsExactly(org1.getId(), org11.getId());

    assertThat(dao.getAllChildOrganizations(org11.getId()))
        .extracting(Organization::getId)
        .containsExactly(org11.getId());

    List<String> childOrgIds = dao.getAllChildOrganizations(ROOT_ORGANIZATION_ID)
        .stream()
        .map(Organization::getId)
        .collect(Collectors.toList());

    // NOTE: the relative ordering of organization, org1, and org2 in the returned list is an impl detail
    assertThat(childOrgIds).hasSize(5);
    assertThat(childOrgIds).startsWith(ROOT_ORGANIZATION_ID);
    assertThat(childOrgIds.subList(1, 4)).containsExactlyInAnyOrder(organization.getId(), org1.getId(), org2.getId());
    assertThat(childOrgIds).endsWith(org11.getId());
  }

  @Test
  public void searchByNameSubstring_returnsMatchingOrgs() {
    tempEntity.newOrganization("Zeta-acme-Corp");
    tempEntity.newOrganization("Zeta-beta-Systems");
    tempEntity.newOrganization("Zeta-acme-Partners");

    List<Organization> results = dao.searchByNameSubstring("zeta-acme", 10);

    assertThat(results).extracting(Organization::getName)
        .containsExactly("Zeta-acme-Corp", "Zeta-acme-Partners"); // alphabetical
  }

  @Test
  public void searchByNameSubstring_caseInsensitive() {
    tempEntity.newOrganization("Zeta-AcMe-CoRp");

    List<Organization> results = dao.searchByNameSubstring("ZETA-ACME", 10);

    assertThat(results).extracting(Organization::getName).containsExactly("Zeta-AcMe-CoRp");
  }

  @Test
  public void searchByNameSubstring_stripsWhitespaceInQueryAndMatch() {
    // Names are stored with whitespace stripped, so a query with spaces must also normalize.
    tempEntity.newOrganization("Zeta acme Enterprise");

    List<Organization> results = dao.searchByNameSubstring("zeta acme", 10);

    assertThat(results).extracting(Organization::getName).containsExactly("Zeta acme Enterprise");
  }

  @Test
  public void searchByNameSubstring_respectsLimit() {
    tempEntity.newOrganization("Zeta-acme-1");
    tempEntity.newOrganization("Zeta-acme-2");
    tempEntity.newOrganization("Zeta-acme-3");

    List<Organization> results = dao.searchByNameSubstring("zeta-acme", 2);

    assertThat(results).hasSize(2);
  }

  @Test
  public void searchByNameSubstring_noMatch_returnsEmptyList() {
    tempEntity.newOrganization("Zeta-beta-Systems");

    List<Organization> results = dao.searchByNameSubstring("zeta-gamma", 10);

    assertThat(results).isEmpty();
  }
}
