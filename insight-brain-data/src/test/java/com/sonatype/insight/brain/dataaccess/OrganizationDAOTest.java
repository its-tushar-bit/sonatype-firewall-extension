/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticApplicationsConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.DataRetentionPolicyDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProprietaryConfigDAO;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryConnectionDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.NameHelperTest;
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
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.postgres.PostgresServer;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OrganizationDAOTest
    extends AbstractDbDAOTest
{
  private OrganizationDAO dao = new OrganizationDAO();

  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  @Test
  public void testCRUD() throws Exception {
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
  public void testInsert_AllowPolicyViolationGrandfatheringOverride_DefaultsToTrue() {
    organization = new Organization("OrganizationDAOTest");
    assertThat(organization.isAllowPolicyViolationGrandfatheringOverride()).isTrue();

    dao.insert(organization);
    tempEntity.register(organization);
    organization = dao.getById(organization.getId());
    assertThat(organization.isAllowPolicyViolationGrandfatheringOverride()).isTrue();
  }

  @Test
  public void testGetAll() throws Exception {
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
    assertThatThrownBy(() -> {
      dao.getByIdNotNull("non-existent-org");
    }).isInstanceOf(NotFoundException.class);
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
  public void testCannotDeleteRootOrg() {
    organization = dao.getById(Organization.ROOT_ORGANIZATION_ID);
    assertThatThrownBy(() -> {
      dao.delete(organization);
    }).isInstanceOf(BadRequestException.class).hasMessage("Cannot delete the root organization: Root Organization");
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
  public void testInsert_ParentOrganizationIdIsRejectedWhenNotRoot() {
    organization = new Organization();
    organization.setName("testName");
    organization.setParentOrganizationId(tempEntity.newOrganization().getId());
    assertThatThrownBy(() -> {
      dao.insert(organization);
    }).isInstanceOf(BadRequestException.class).hasMessage("Invalid parent organization");

    organization.setParentOrganizationId(Organization.ROOT_ORGANIZATION_ID);
    try {
      dao.insert(organization);
      assertThat(dao.getById(organization.getId())).isNotNull();
    }
    finally {
      dao.delete(organization);
    }
  }

  @Test
  public void testUpdate_ParentOrganizationIdIsForcedToRootSetWhenNull() {
    organization = tempEntity.newOrganization("OrganizationDAOTest");
    organization.setParentOrganizationId(null);
    dao.update(organization);

    organization = dao.getById(organization.getId());
    assertThat(organization.getParentOrganizationId()).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testUpdate_ParentOrganizationIdIsForcedToRootSetWhenNotNull() {
    organization = tempEntity.newOrganization("OrganizationDAOTest");
    organization.setParentOrganizationId("dummy org");
    dao.update(organization);

    organization = dao.getById(organization.getId());
    assertThat(organization.getParentOrganizationId()).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testUpdateRootOrgs_ParentOrganizationIdIsForcedToNull() {
    organization = dao.getById(Organization.ROOT_ORGANIZATION_ID);
    organization.setParentOrganizationId("dummyOrg");
    dao.update(organization);

    organization = dao.getById(organization.getId());
    assertThat(organization.getParentOrganizationId()).isNull();
  }

  @Test
  public void testUpdateRootOrgName() {
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
  public void testValidateNullName_Insert() {
    Organization organization = new Organization(null /* name */);
    assertThatThrownBy(() -> {
      dao.insert(organization);
    }).isInstanceOf(InvalidNameException.class).hasMessage("Name is required.");
  }

  @Test
  public void testValidateNullName_Update() {
    organization.setName(null);
    assertThat(organization.getNameLowercaseNoWhitespace()).isNull();
    assertThatThrownBy(() -> {
      dao.update(organization);
    }).isInstanceOf(InvalidNameException.class).hasMessage("Name is required.");
  }

  @Test
  public void testValidateEmptyName_Insert() {
    assertThatThrownBy(() -> {
      tempEntity.newOrganization(" ");
    }).isInstanceOf(InvalidNameException.class).hasMessage("Name is required.");
  }

  @Test
  public void testValidateEmptyName_Update() {
    organization.setName(" ");
    assertThat(organization.getNameLowercaseNoWhitespace()).isEqualTo("");
    assertThatThrownBy(() -> {
      dao.update(organization);
    }).isInstanceOf(InvalidNameException.class).hasMessage("Name is required.");
  }

  @Test
  public void testValidateNameInvalidChars_Insert() {
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      Organization organization = new Organization(name);
      assertThatThrownBy(() -> {
        dao.insert(organization);
      }).isInstanceOf(InvalidNameException.class).hasMessage(NameHelper.INVALID_CHAR_MESSAGE, "Name", name.charAt(0));
    }
  }

  @Test
  public void testValidateNameInvalidChars_Update() {
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      organization.setName(name);
      assertThatThrownBy(() -> {
        dao.update(organization);
      }).isInstanceOf(InvalidNameException.class).hasMessage(NameHelper.INVALID_CHAR_MESSAGE, "Name", name.charAt(0));
    }
  }

  @Test
  public void testValidateNameValidChars_Insert() {
    for (String name : NameHelperTest.VALID_NAMES) {
      tempEntity.newOrganization(name);
    }
  }

  @Test
  public void testValidateNameValidChars_Update() {
    Organization organization = tempEntity.newOrganization("a");
    for (String name : NameHelperTest.VALID_NAMES) {
      organization.setName(name);
      dao.update(organization);
    }
  }

  @Test
  public void testValidateNameSpaces_Insert() {
    for (String name : NameHelperTest.INVALID_SPACING_NAMES) {
      assertThatThrownBy(() -> {
        tempEntity.newOrganization(name);
      }).isInstanceOf(InvalidNameException.class)
          .hasMessage("Name must not have leading or trailing spaces, or have two spaces in a row.");
    }
  }

  @Test
  public void testValidateNameSpaces_Update() {
    for (String name : NameHelperTest.INVALID_SPACING_NAMES) {
      organization.setName(name);
      assertThatThrownBy(() -> {
        dao.update(organization);
      }).isInstanceOf(InvalidNameException.class)
          .hasMessage("Name must not have leading or trailing spaces, or have two spaces in a row.");
    }
  }

  @Test
  public void testNameIsCaseAndWhitespaceInsensitive() {
    String name = "test string With Case and Whitespace";

    organization = tempEntity.newOrganization(name);

    assertThat(organization.getName()).isEqualTo(name);
    assertThat(organization.getNameLowercaseNoWhitespace()).isEqualTo("teststringwithcaseandwhitespace");

    String name1 = "TEST String      With    cASE and      whitespace";
    Organization organization1 = dao.getByName(name1);
    assertThat(organization1).isNotNull();
    assertThat(organization1.getId()).isEqualTo(organization.getId());
  }

  @Test
  public void testDuplicateName_Insert() {
    tempEntity.newOrganization("testDuplicateName");

    assertThatThrownBy(() -> {
      tempEntity.newOrganization("testDuplicateName");
    }).isInstanceOf(InvalidNameException.class).hasMessage("testDuplicateName is already used as a name.");
  }

  @Test
  public void testDuplicateName_Update() {
    tempEntity.newOrganization("testDuplicateName");
    Organization organization1 = tempEntity.newOrganization("testDuplicateName1");

    organization1.setName("Test Duplicate Name");
    assertThatThrownBy(() -> {
      dao.update(organization1);
    }).isInstanceOf(InvalidNameException.class).hasMessage("Test Duplicate Name is already used as a name.");
  }

  @Test
  public void testValidateNameLength_Insert() {
    String name = StringUtils.repeat("a", NameHelper.MAX_NAME_LENGTH_APP_ORG);
    assertThatThrownBy(() -> {
      tempEntity.newOrganization(name + "a");
    }).isInstanceOf(InvalidNameException.class)
        .hasMessage("Name must be " + NameHelper.MAX_NAME_LENGTH_APP_ORG + " characters or less.");

    tempEntity.newOrganization(name);
  }

  @Test
  public void testValidateNameLength_Update() {
    String name = StringUtils.repeat("a", NameHelper.MAX_NAME_LENGTH_APP_ORG);
    organization.setName(name + "a");
    assertThatThrownBy(() -> {
      dao.update(organization);
    }).isInstanceOf(InvalidNameException.class)
        .hasMessage("Name must be " + NameHelper.MAX_NAME_LENGTH_APP_ORG + " characters or less.");

    organization.setName(name);
    dao.update(organization);
  }

  @Test
  public void testCascadeDeleteToLabels() {
    final LabelDAO labelDAO = new LabelDAO();

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
  public void testCascadeDeleteToProprietaryConfig() {
    Organization organization = tempEntity.newOrganization("organization");
    tempEntity.newProprietaryConfig(organization.getId());

    dao.delete(organization);
    assertThat(new ProprietaryConfigDAO().getByOwnerId(organization.getId())).isNull();
  }

  @Test
  public void testCascadeDeleteToTags() {
    TagDAO tagDAO = new TagDAO();

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
  public void testCascadeDeleteToPolicies() {
    Organization organization = tempEntity.newOrganization("organization");

    tempEntity.newPolicy(organization);
    PolicyDAO policyDAO = new PolicyDAO();
    List<Policy> policies = policyDAO.getByOwnerId(organization.getId());
    assertThat(policies).hasSize(1);

    dao.delete(organization);
    policies = policyDAO.getByOwnerId(organization.getId());
    assertThat(policies).isEmpty();
  }

  @Test
  public void testCascadeDeleteToLicenseOverrides() {
    Organization organization = tempEntity.newOrganization("testCascadeDeleteToLicenseOverrides");
    String organizationId = organization.getId();

    LicenseOverride licenseOverride = new LicenseOverride(organizationId, ComponentIdentifier.createMavenCoordinates(
        "groupId", "artifactId", "version"), LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0", "My comment");
    LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();
    licenseOverrideDAO.insert(licenseOverride);
    List<LicenseOverride> licenseOverrides = licenseOverrideDAO.getByOwnerId(organizationId);
    assertThat(licenseOverrides).hasSize(1);

    dao.delete(organization);
    licenseOverrides = licenseOverrideDAO.getByOwnerId(organizationId);
    assertThat(licenseOverrides).isEmpty();
  }

  @Test
  public void testCascadeDeleteToSecurityVulnerabilityOverrides() {
    Organization organization = tempEntity.newOrganization("testCascadeDeleteToSecurityVulnerabilityOverrides");
    SecurityVulnerabilityOverride securityVulnerabilityOverride = tempEntity.newSecurityVulnerabilityOverride(
        organization.getId(), "hash", "source", "refrenceId", SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED);

    dao.delete(organization);

    assertThat(new SecurityVulnerabilityOverrideDAO().getById(securityVulnerabilityOverride.getId())).isNull();
  }

  @Test
  public void testCascadeDeleteToPolicyWaivers() {
    Organization organization = tempEntity.newOrganization("testCascadeDeleteToPolicyWaivers");

    Policy policy = tempEntity.newPolicy(organization);
    PolicyWaiver policyWaiver = new PolicyWaiver("12345678901234567890", policy.getId(), organization.getId(),
        "My comment");
    PolicyWaiverDAO policyWaiverDAO = new PolicyWaiverDAO();
    policyWaiverDAO.insert(policyWaiver);
    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getActiveByOwnerId(organization.getId());
    assertThat(policyWaivers).hasSize(1);

    dao.delete(organization);
    policyWaivers = policyWaiverDAO.getActiveByOwnerId(organization.getId());
    assertThat(policyWaivers).isEmpty();
  }

  @Test
  public void testCascadeDeleteToMembershipMappings() {
    Organization organization = tempEntity.newOrganization("testCascadeDeleteToMembershipMappings");

    String roleId = new RoleDAO().getApplicationRoles().get(0).getId();
    MembershipMappingDAO membershipMappingDAO = new MembershipMappingDAO();
    membershipMappingDAO.setMembershipMappingsForContextAndRole(organization.getId(), roleId,
        Arrays.asList(new MembershipMapping("admin", MemberType.USER)));

    dao.delete(organization);

    assertThat(membershipMappingDAO.getByContextId(organization.getId())).isEmpty();
  }

  @Test
  public void testCascadeDeleteToPolicyMonitoring() {
    Organization organization = tempEntity.newOrganization("testCascadeDeleteToPolicyMonitoring");

    PolicyMonitoringDAO policyMonitoringDAO = new PolicyMonitoringDAO();
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(organization.getId(), Stage.ID_RELEASE);
    policyMonitoringDAO.insert(policyMonitoring);
    assertThat(policyMonitoringDAO.getByOwnerId(organization.getId())).isNotNull();

    dao.delete(organization);

    assertThat(policyMonitoringDAO.getByOwnerId(organization.getId())).isNull();
  }

  @Test
  public void testCascadeDeleteToDataRetentionPolicies() {
    Organization organization = tempEntity.newOrganization();

    DataRetentionPolicyDAO dataRetentionPolicyDAO = new DataRetentionPolicyDAO();
    dataRetentionPolicyDAO.insert(new DataRetentionPolicy(organization.getId(), "contextId", false, null, null));

    dao.delete(organization);

    assertThat(dataRetentionPolicyDAO.getByOwnerId(organization.getId())).isEmpty();
  }

  @Test
  public void testCascadeDeleteToSourceControl() {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    Organization organization = tempEntity.newOrganization();
    SourceControl sourceControl = tempEntity.newSourceControl(
        organization.getId(), null, "token", null);

    dao.delete(organization);

    assertThat(new SourceControlDAO().getById(sourceControl.getId())).isNull();
  }

  @Test
  public void testDelete_AutomaticApplicationsCreationDisabled_SameOrganizationId() {
    AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO =
        new AutomaticApplicationsConfigurationDAO();

    Organization organization = tempEntity.newOrganization("organization");

    String organizationId = organization.getId();

    automaticApplicationsConfigurationDAO.setEnabled(false);
    automaticApplicationsConfigurationDAO.setOrganizationId(organizationId);

    dao.delete(organization);

    assertThat(automaticApplicationsConfigurationDAO.getOrganizationId()).isEqualTo("");
  }

  @Test
  public void testDelete_AutomaticApplicationsCreationDisabled_DifferentOrganizationId() {
    AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO =
        new AutomaticApplicationsConfigurationDAO();

    Organization organization = tempEntity.newOrganization("organization");

    automaticApplicationsConfigurationDAO.setEnabled(false);
    automaticApplicationsConfigurationDAO.setOrganizationId("otherOrganizationId");

    dao.delete(organization);

    assertThat(automaticApplicationsConfigurationDAO.getOrganizationId()).isEqualTo("otherOrganizationId");
  }

  @Test
  public void testDelete_AutomaticApplicationsCreationEnabled_SameOrganizationId() {
    AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO =
        new AutomaticApplicationsConfigurationDAO();

    Organization organization = tempEntity.newOrganization("organization");

    String organizationId = organization.getId();

    automaticApplicationsConfigurationDAO.setEnabled(true);
    automaticApplicationsConfigurationDAO.setOrganizationId(organizationId);

    assertThatThrownBy(() -> {
      dao.delete(organization);
    }).isInstanceOf(BadRequestException.class).hasMessage(
        "Cannot delete the parent organization for automatic application creation: " + organization.getName() + ".");

    automaticApplicationsConfigurationDAO.setOrganizationId("");
    dao.delete(organization);
  }

  @Test
  public void testDelete_AutomaticApplicationsCreationEnabled_DifferentOrganizationId() {
    AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO =
        new AutomaticApplicationsConfigurationDAO();

    Organization organization = tempEntity.newOrganization("organization");

    automaticApplicationsConfigurationDAO.setEnabled(true);
    automaticApplicationsConfigurationDAO.setOrganizationId("otherOrganizationId");

    dao.delete(organization);

    assertThat(automaticApplicationsConfigurationDAO.getOrganizationId()).isEqualTo("otherOrganizationId");
  }

  @Test
  public void testCRUD_RecordSearchIndexChange() {
    new SystemConfigurationPropertyDAO()
        .update(new SystemConfigurationProperty(SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED, "true"));
    Organization org = tempEntity.newOrganization();

    List<SearchIndexChange> searchIndexChanges = new SearchIndexChangeDAO().getAll();
    assertThat(searchIndexChanges).hasSize(1);
    assertThat(searchIndexChanges.get(0).getChangeType()).isEqualTo(ChangeType.ORGANIZATION);
    assertThat(searchIndexChanges.get(0).getChangeData()).isEqualTo(org.getId());
    new SearchIndexChangeDAO().delete(searchIndexChanges.get(0));

    dao.update(org);
    searchIndexChanges = new SearchIndexChangeDAO().getAll();
    assertThat(searchIndexChanges).hasSize(1);
    assertThat(searchIndexChanges.get(0).getChangeType()).isEqualTo(ChangeType.ORGANIZATION);
    assertThat(searchIndexChanges.get(0).getChangeData()).isEqualTo(org.getId());
    new SearchIndexChangeDAO().delete(searchIndexChanges.get(0));

    dao.delete(org);
    searchIndexChanges = new SearchIndexChangeDAO().getAll();
    assertThat(searchIndexChanges).hasSize(1);
    assertThat(searchIndexChanges.get(0).getChangeType()).isEqualTo(ChangeType.ORGANIZATION);
    assertThat(searchIndexChanges.get(0).getChangeData()).isEqualTo(org.getId());
  }

  @Test
  public void testCascadeDeleteToLocks_H2() {
    // Lock for audit json file store
    Organization organization = tempEntity.newOrganization();
    try (ClusterLock clusterLock = ClusterLock.createForAuditJsonFileStore(organization.getId())) {
      clusterLock.lock();
    }
    assertThat(ClusterLock.LOCKS_BY_ID
        .get(ClusterLock.getLockIdForAuditJsonFileStore(organization.getId()))).isNotNull();

    dao.delete(organization);

    assertThat(ClusterLock.LOCKS_BY_ID
        .get(ClusterLock.getLockIdForAuditJsonFileStore(organization.getId()))).isNull();
  }

  @Test
  public void testCascadeDeleteToLocks_Postgres() {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      LockDAO lockDAO = new LockDAO();
      Organization organization = tempEntity.newOrganization();

      // Lock for audit json file store
      try (ClusterLock clusterLock = ClusterLock.createForAuditJsonFileStore(organization.getId())) {
        clusterLock.lock();
      }
      assertThat(lockDAO.getById(ClusterLock.getLockIdForAuditJsonFileStore(organization.getId()))).isNotNull();

      dao.delete(organization);

      assertThat(lockDAO.getById(ClusterLock.getLockIdForAuditJsonFileStore(organization.getId()))).isNull();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  @Test
  public void testDelete_CascadesToRepositoryConnections() {
    Organization organization = tempEntity.newOrganization();
    RepositoryConnection repositoryConnection = tempEntity.newRepositoryConnection(organization.getId());

    dao.delete(organization);

    RepositoryConnectionDAO repositoryConnectionDAO = new RepositoryConnectionDAO();
    assertThat(repositoryConnectionDAO.getById(repositoryConnection.getId())).isNull();
  }
}
