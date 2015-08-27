/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.configuration.ldap.LdapAuthenticationMethod;
import com.sonatype.insight.brain.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.configuration.ldap.LdapProtocol;
import com.sonatype.insight.brain.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapConnectionDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.dataaccess.notification.UserViewedProductNotificationDAO;
import com.sonatype.insight.brain.dataaccess.policy.FirstOccurrencePolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.WaivedPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.security.RolePermissionDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.dataaccess.tag.ApplicationTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.PolicyTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.brain.model.notification.UserViewedProductNotification;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.FirstOccurrencePolicyViolation;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.WaivedPolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.RolePermission;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.codehaus.plexus.util.StringUtils;
import org.junit.rules.ExternalResource;

/**
 * Like TemporaryFolder, just for apps and orgs etc.
 */
public class TemporaryEntity
    extends ExternalResource
{
  public static final String USER_PASSWORD_CLEAR = "secret";

  private static final String USER_PASSWORD_HASH = "$shiro1$SHA-256$10$Gsv3gW95oRKzzxp37k/wJA==$T2VDhMzPuXN7VTobkLUcwDsxxJJXj5pInbW7YUn8muY=";

  private final ApplicationDAO appDAO = new ApplicationDAO();

  private final OrganizationDAO orgDAO = new OrganizationDAO();

  private final UserDAO userDAO = new UserDAO();

  private final RoleDAO roleDAO = new RoleDAO(true);

  private final RolePermissionDAO rolePermDAO = new RolePermissionDAO();

  private final MembershipMappingDAO membershipMappingDAO = new MembershipMappingDAO();

  private final LabelDAO labelDAO = new LabelDAO();

  private final TagDAO tagDAO = new TagDAO();

  private final ApplicationComponentDAO appComponentDAO = new ApplicationComponentDAO();

  private final ApplicationTagDAO appTagDAO = new ApplicationTagDAO();

  private final PolicyTagDAO policyTagDAO = new PolicyTagDAO();

  private final PolicyDAO policyDAO = new PolicyDAO();

  private final PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();

  private final PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();

  private final FirstOccurrencePolicyViolationDAO firstOccurrencePolicyViolationDAO = new FirstOccurrencePolicyViolationDAO();

  private final WaivedPolicyViolationDAO waivedPolicyViolationDAO = new WaivedPolicyViolationDAO();

  private final ComponentLabelDAO componentLabelDAO = new ComponentLabelDAO();

  private final LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();

  private final LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = new LicenseThreatGroupLicenseDAO();

  private final LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();

  private final PolicyWaiverDAO waiverDAO = new PolicyWaiverDAO();

  private final LdapServerDAO ldapServerDAO = new LdapServerDAO();

  private final LdapConnectionDAO ldapConnectionDAO = new LdapConnectionDAO();

  private final LdapUserMappingDAO ldapUserMappingDAO = new LdapUserMappingDAO();

  private final HashComponentIdentifierDAO hashComponentIdentifierDAO = new HashComponentIdentifierDAO();

  private final DashboardFilterDAO dashboardFilterDAO = new DashboardFilterDAO();

  private final UserViewedProductNotificationDAO userViewedNotificationMappingDAO =
      new UserViewedProductNotificationDAO();

  private final PolicyMonitoringDAO policyMonitoringDAO = new PolicyMonitoringDAO();

  private final RepositoryManagerDAO repositoryManagerDAO = new RepositoryManagerDAO();

  private final RepositoryDAO repositoryDAO = new RepositoryDAO();

  private final RepositoryComponentDAO repositoryComponentDAO = new RepositoryComponentDAO();

  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO = new RepositoryPolicyViolationDAO();

  private Collection<Application> apps;

  private Collection<Organization> orgs;

  private Collection<LicenseOverride> licenseOverrides;

  private Collection<User> users;

  private Collection<Role> roles;

  private Collection<LdapServer> ldapServers;

  private Collection<HashComponentIdentifier> claimedComponents;

  private Collection<DashboardFilter> dashboardFilters;

  private Collection<UserViewedProductNotification> userViewedNotificationMappings;

  private Collection<Policy> policies;

  private Collection<PolicyTag> policyTags;

  private Collection<Tag> tags;

  private Collection<Label> labels;

  private Collection<LicenseThreatGroup> licenseThreatGroups;

  private Collection<PolicyMonitoring> policyMonitorings;

  private Collection<RepositoryManager> repositoryManagers;

  @Override
  protected void before() {
    apps = new ArrayList<>();
    orgs = new ArrayList<>();
    licenseOverrides = new ArrayList<>();
    users = new ArrayList<>();
    roles = new ArrayList<>();
    ldapServers = new ArrayList<>();
    claimedComponents = new ArrayList<>();
    dashboardFilters = new ArrayList<>();
    userViewedNotificationMappings = new ArrayList<>();
    policies = new ArrayList<>();
    policyTags = new ArrayList<>();
    tags = new ArrayList<>();
    labels = new ArrayList<>();
    licenseThreatGroups = new ArrayList<>();
    policyMonitorings = new ArrayList<>();
    repositoryManagers = new ArrayList<>();
  }

  @Override
  protected void after() {
    /*
     * For our purposes, it's irrelevant whether the entity has been manually deleted or updated in the meantime, we
     * just want it gone. Hence the defensive coding below to avoid optimistic lock errors and other JPA fun.
     */
    for (DashboardFilter dashboardFilter : dashboardFilters) {
      if ((dashboardFilter = dashboardFilterDAO.getByUsername(dashboardFilter.getUsername())) != null) {
        dashboardFilterDAO.delete(dashboardFilter);
      }
    }
    for (PolicyTag policyTag : policyTags) {
      if ((policyTag = policyTagDAO.getById(policyTag.getId())) != null) {
        policyTagDAO.delete(policyTag);
      }
    }
    for (Application app : apps) {
      if ((app = appDAO.getById(app.getId())) != null) {
        appDAO.delete(app);
      }
    }
    for (Organization org : orgs) {
      if ((org = orgDAO.getById(org.getId())) != null) {
        orgDAO.delete(org);
      }
    }
    for (LicenseOverride override : licenseOverrides) {
      if ((override = licenseOverrideDAO.getById(override.getId())) != null) {
        licenseOverrideDAO.delete(override);
      }
    }
    for (User user : users) {
      if ((user = userDAO.getById(user.getId())) != null) {
        userDAO.delete(user);
      }
    }
    for (Role role : roles) {
      if ((role = roleDAO.getById(role.getId())) != null) {
        roleDAO.delete(role);
      }
    }
    for (LdapServer ldapServer : ldapServers) {
      if ((ldapServer = ldapServerDAO.getById(ldapServer.getId())) != null) {
        ldapServerDAO.delete(ldapServer);
      }
    }
    for (HashComponentIdentifier claimedComponent : claimedComponents) {
      if ((claimedComponent = hashComponentIdentifierDAO.getById(claimedComponent.getId())) != null) {
        hashComponentIdentifierDAO.delete(claimedComponent);
      }
    }

    for (UserViewedProductNotification userViewedNotificationMapping : userViewedNotificationMappings) {
      if ((userViewedNotificationMapping = userViewedNotificationMappingDAO.getByUsernameAndNotificationId(
          userViewedNotificationMapping.getUsername(), userViewedNotificationMapping.getNotificationId())) != null) {
        userViewedNotificationMappingDAO.delete(userViewedNotificationMapping);
      }
    }
    for (Policy policy : policies) {
      if ((policy = policyDAO.getById(policy.getId())) != null) {
        policyDAO.delete(policy);
      }
    }

    for (Label label : labels) {
      if ((label = labelDAO.getById(label.getId())) != null) {
        labelDAO.delete(label);
      }
    }
    for (Tag tag : tags) {
      if ((tag = tagDAO.getById(tag.getId())) != null) {
        tagDAO.delete(tag);
      }
    }

    for (LicenseThreatGroup licenseThreatGroup : licenseThreatGroups) {
      if ((licenseThreatGroup = licenseThreatGroupDAO.getById(licenseThreatGroup.getId())) != null) {
        licenseThreatGroupDAO.delete(licenseThreatGroup);
      }
    }

    for (PolicyMonitoring policyMonitoring : policyMonitorings) {
      if ((policyMonitoring = policyMonitoringDAO.getById(policyMonitoring.getId())) != null) {
        policyMonitoringDAO.delete(policyMonitoring);
      }
    }

    for (RepositoryManager repositoryManager : repositoryManagers) {
      if ((repositoryManager = repositoryManagerDAO.getById(repositoryManager.getId())) != null) {
        repositoryManagerDAO.delete(repositoryManager);
      }
    }
  }

  public String uuid() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  public DashboardFilter newDashboardFilter(String username, String filter) {
    DashboardFilter dashboardFilter = new DashboardFilter();
    dashboardFilter.setUsername(username);
    dashboardFilter.setFilter(filter);
    dashboardFilterDAO.insert(dashboardFilter);
    dashboardFilters.add(dashboardFilter);
    return dashboardFilter;
  }

  public DashboardFilter updateDashboardFilter(String username, String filter) {
    DashboardFilter dashboardFilter = new DashboardFilter();
    dashboardFilter.setUsername(username);
    dashboardFilter.setFilter(filter);

    DashboardFilter existingDashboardFilter = dashboardFilterDAO.getByUsername(username);
    if (existingDashboardFilter == null) {
      dashboardFilterDAO.insert(dashboardFilter);
    }
    else {
      dashboardFilter.setId(existingDashboardFilter.getId());
      dashboardFilterDAO.update(dashboardFilter);
    }
    dashboardFilters.add(dashboardFilter);
    return dashboardFilter;
  }

  public Organization newOrganization() {
    return newOrganization("Test Org " + uuid());
  }

  public Organization newOrganization(Organization parentOrg) {
    return newOrganization("Test Org " + uuid(), parentOrg);
  }

  public Organization newOrganization(String name) {
    return newOrganization(name, null /* parentOrg */);
  }

  public Organization newOrganization(String name, Organization parentOrg) {
    Organization org = new Organization(name);
    if (parentOrg != null) {
      org.setParentOrganizationId(parentOrg.getId());
    }
    orgDAO.insert(org);
    orgs.add(org);
    return org;
  }

  public Organization newOrganizationWithSpecificId(String id) {
    Organization org = new Organization(uuid());
    org.setId(id);
    orgDAO.insert(org);
    orgs.add(org);
    return org;
  }

  public List<Organization> newOrganizations(int orgCount) {
    List<Organization> organizations = new ArrayList<>();
    for (int index = 0; index < orgCount; ++index) {
      organizations.add(newOrganization());
    }
    return organizations;
  }

  public void register(DashboardFilter... dashboardFilters) {
    Collections.addAll(this.dashboardFilters, dashboardFilters);
  }

  public void register(Application... applications) {
    Collections.addAll(apps, applications);
  }

  public void register(Organization... organizations) {
    Collections.addAll(orgs, organizations);
  }

  public void register(Role... roles) {
    Collections.addAll(this.roles, roles);
  }

  public void register(User... users) {
    Collections.addAll(this.users, users);
  }

  public void register(HashComponentIdentifier... hashComponentIdentifiers) {
    Collections.addAll(claimedComponents, hashComponentIdentifiers);
  }

  public Application newApplicationWithParent(String appPublicId) {
    // Application Name must be unique
    return newApplicationWithParent(appPublicId, "DUMMY-NAME-" + uuid());
  }

  public Application newApplicationWithParent(String publicId, String name) {
    Organization org = newOrganization(name);
    return newApplication(name, publicId, org.getId());
  }

  public Application newApplication(String orgId) {
    return newApplication(uuid(), orgId);
  }

  public Application newApplication(String publicId, String orgId) {
    return newApplication("Test App " + uuid(), publicId, orgId);
  }

  public Application newApplication(String name, String publicId, String orgId) {
    Application app = new Application(publicId, name, orgId);
    appDAO.insert(app);
    apps.add(app);
    return app;
  }

  /**
   * Creates an application with an invalid public ID for tests that verify backwards compatibility for existing
   * applications with public IDs created before the validation for application public IDs was introduced.
   */
  public Application newApplicationWithInvalidPublicId(String invalidPublicId) {
    Application app = new Application(invalidPublicId, "App with Invalid Public ID", newOrganization().getId());
    app.setId("app_with_invalid_public_id");
    try (TransactionContext tx = appDAO.createTransactionContext()) {
      tx.begin();
      tx.persist(app);
      tx.commit();
      register(app);
    }
    return app;
  }

  public Application newApplication(String name, String publicId, String orgId, String contactInternalName) {
    Application app = new Application(publicId, name, orgId);
    app.setContactInternalName(contactInternalName);
    appDAO.insert(app);
    apps.add(app);
    return app;
  }

  public Application newApplicationWithSpecificId(String id, String name, String publicId, String orgId) {
    Application app = new Application(publicId, name, orgId);
    app.setId(id);
    appDAO.insert(app);
    apps.add(app);
    return app;
  }

  public List<Application> newApplications(String orgId, int appCount) {
    List<Application> applications = new ArrayList<>();
    for (int index = 0; index < appCount; ++index) {
      applications.add(newApplication(orgId));
    }
    return applications;
  }

  public User newUser() {
    return newUser("user-" + uuid());
  }

  public User newUser(String username) {
    return newUser(username, "John", "Doe", username + "@void.com");
  }

  public User newUser(String username, String firstName, String lastName, String email) {
    User user = newUser(username, USER_PASSWORD_HASH, firstName, lastName, email);
    user.setPassword(USER_PASSWORD_CLEAR);
    return user;
  }

  public User newUser(String username, String passwordHash, String firstName, String lastName, String email) {
    User user = new User(username, passwordHash, firstName, lastName, email);
    userDAO.insert(user);
    users.add(user);
    return user;
  }

  public Role newRole(boolean global, Permission... permissions) {
    return newRole("Role " + uuid(), global, permissions);
  }

  public Role newRole(String name, boolean global, Permission... permissions) {
    return newRole(name, name /* description */, global, permissions);
  }

  public Role newRole(String name, String description, boolean global, Permission... permissions) {
    Role role = new Role();
    role.setName(name);
    role.setDescription(description);
    role.setGlobal(global);
    roleDAO.insert(role);
    roles.add(role);
    for (Permission permission : permissions) {
      rolePermDAO.insert(new RolePermission(role.getId(), permission));
    }
    return role;
  }

  public MembershipMapping newMembershipMapping(String contextId, String roleId, String username) {
    return newMembershipMapping(contextId, roleId, username, MemberType.USER);
  }

  public MembershipMapping newMembershipMapping(String contextId, String roleId, String memberName,
      MemberType memberType)
  {
    MembershipMapping membershipMapping = new MembershipMapping(contextId, roleId, memberName, memberType);
    membershipMappingDAO.insert(membershipMapping);
    return membershipMapping;
  }

  public Label newLabel(String ownerId) {
    return newLabel(ownerId, uuid());
  }

  public Label newLabel(String ownerId, Color color) {
    return newLabel(ownerId, uuid(), color);
  }

  public Label newLabel(String ownerId, String labelText) {
    return newLabel(ownerId, labelText, Color.white);
  }

  public Label newLabel(String ownerId, String labelText, Color color){
    Label label = new Label(ownerId, labelText, color);
    labelDAO.insert(label);
    labels.add(label);
    return label;
  }

  /**
   * Creates a label with invalid label text for backwards compatibility tests. Prior to 1.13 labels could use any
   * characters except for spaces and tabs.
   */
  public Label newLabelWithInvalidLabelText(String ownerId, String labelText, Color color) {
    Label label = new Label(ownerId, labelText, color);
    label.setId("label_with_invalid_label_text");
    try (TransactionContext tx = labelDAO.createTransactionContext()) {
      tx.begin();
      tx.persist(label);
      tx.commit();
    }
    labels.add(label);
    return label;
  }

  public ComponentLabel newComponentLabel(String ownerId, String labelId){
    return newComponentLabel(ownerId, labelId, uuid().substring(0, 19));
  }

  public ComponentLabel newComponentLabel(String ownerId, String labelId, String hash){
    ComponentLabel componentLabel = new ComponentLabel(ownerId, labelId, hash);
    componentLabelDAO.insert(componentLabel);
    return componentLabel;
  }

  public LicenseThreatGroup newLicenseThreatGroup(String ownerId) {
    return newLicenseThreatGroup(ownerId, "LTG" + uuid(), 5);
  }

  public LicenseThreatGroup newLicenseThreatGroup(String ownerId, String name, int threatLevel) {
    return newLicenseThreatGroup(ownerId, name, threatLevel, new String[0]);
  }

  public LicenseThreatGroup newLicenseThreatGroup(String ownerId, String name, int threatLevel, String... licenseIds) {
    LicenseThreatGroup ltg = new LicenseThreatGroup(ownerId, name, threatLevel);
    licenseThreatGroupDAO.insert(ltg);
    licenseThreatGroups.add(ltg);

    for (String licenseId : licenseIds) {
      newLicenseThreatGroupLicense(ownerId, ltg.getId(), licenseId);
    }

    return ltg;
  }

  public LicenseThreatGroupLicense newLicenseThreatGroupLicense(String ownerId, String licenseThreatGroupId) {
    return newLicenseThreatGroupLicense(ownerId, licenseThreatGroupId, "Apache-2.0");
  }

  public LicenseThreatGroupLicense newLicenseThreatGroupLicense(String ownerId, String licenseThreatGroupId, String licenseId) {
    LicenseThreatGroupLicense licenseThreatGroupLicense = new LicenseThreatGroupLicense(ownerId, licenseThreatGroupId,
        licenseId);
    licenseThreatGroupLicenseDAO.insert(licenseThreatGroupLicense);
    return licenseThreatGroupLicense;
  }

  public LicenseOverride newLicenseOverride(String ownerId, ComponentIdentifier componentIdentifier,
      LicenseOverrideStatus status, Set<String> licenseIds)
  {
    return newLicenseOverride(ownerId, componentIdentifier, status, licenseIds, "testing");
  }

  public LicenseOverride newLicenseOverride(String ownerId, ComponentIdentifier componentIdentifier,
      LicenseOverrideStatus status, String licenseId)
  {
    return newLicenseOverride(ownerId, componentIdentifier, status, licenseId, "testing");
  }

  public LicenseOverride newLicenseOverride(String ownerId, ComponentIdentifier componentIdentifier,
      LicenseOverrideStatus status, String licenseId, String comment)
  {
    return newLicenseOverride(ownerId, componentIdentifier, status,
        licenseId != null ? Collections.singleton(licenseId) : null, comment);
  }

  public LicenseOverride newLicenseOverride(String ownerId, ComponentIdentifier componentIdentifier,
    LicenseOverrideStatus status, Set<String> licenseIds, String comment)
  {
    LicenseOverride override = new LicenseOverride(ownerId, componentIdentifier, status, licenseIds, comment);
    licenseOverrideDAO.insert(override);
    licenseOverrides.add(override);
    return override;
  }

  public PolicyWaiver newWaiver(String policyId, String ownerId) {
    return newWaiver(null, policyId, ownerId);
  }

  public PolicyWaiver newWaiver(String hash, String policyId, String ownerId) {
    return newWaiver(hash, policyId, ownerId, "testing");
  }

  public PolicyWaiver newWaiver(String hash, String policyId, String ownerId, String comment) {
    PolicyWaiver waiver = new PolicyWaiver(hash, policyId, ownerId, comment);
    waiverDAO.insert(waiver);
    return waiver;
  }

  public LdapServer newLdapServer(String name) {
    LdapServer ldapServer = new LdapServer(name);
    ldapServerDAO.insert(ldapServer);
    ldapServers.add(ldapServer);
    return ldapServer;
  }

  public LdapConnection newLdapConnection(String ldapServerId) {
    return newLdapConnection(ldapServerId, 389);
  }

  public LdapConnection newLdapConnection(String ldapServerId, int port) {
    LdapConnection ldapConnection = new LdapConnection();
    ldapConnection.setServerId(ldapServerId);
    ldapConnection.setProtocol(LdapProtocol.LDAP);
    ldapConnection.setHostname("localhost");
    ldapConnection.setPort(port);
    ldapConnection.setAuthenticationMethod(LdapAuthenticationMethod.NONE);
    ldapConnection.setSystemUsername("system");
    ldapConnection.setSearchBase("dc=company,dc=com");
    ldapConnectionDAO.insert(ldapConnection);
    return ldapConnection;
  }

  public LdapUserMapping newLdapUserMapping(String ldapServerId) {
    LdapUserMapping umap = new LdapUserMapping();
    umap.setServerId(ldapServerId);
    umap.setUserBaseDN("ou=users");
    umap.setUserObjectClass("person");
    umap.setUserIDAttribute("uid");
    umap.setUserRealNameAttribute("givenName");
    umap.setUserEmailAttribute("mail");
    umap.setUserSubtree(true);
    umap.setGroupMappingType(LdapGroupMappingType.STATIC);
    umap.setGroupBaseDN("ou=groups");
    umap.setGroupIDAttribute("cn");
    umap.setGroupSubtree(true);
    umap.setGroupObjectClass("groupOfNames");
    umap.setGroupMemberAttribute("member");
    umap.setGroupMemberFormat("uid=${username}");
    ldapUserMappingDAO.insert(umap);
    return umap;
  }

  public Tag newTag(String orgId) {
    return newTag(orgId, "Tag name " + uuid());
  }

  public Tag newTag(String orgId, String name) {
    return newTag(orgId, name, Color.yellow);
  }

  public Tag newTag(String orgId, String name, Color color) {
    Tag tag = new Tag(orgId, name, "description", color);
    tagDAO.insert(tag);
    tags.add(tag);
    return tag;
  }

  public ApplicationTag newApplicationTag(String appId, String tagId) {
    ApplicationTag appTag = new ApplicationTag(appId, tagId);
    appTagDAO.insert(appTag);
    return appTag;
  }

  public PolicyTag newPolicyTag(String policyId, String tagId) {
    PolicyTag policyTag = new PolicyTag(policyId, tagId);
    policyTagDAO.insert(policyTag);
    policyTags.add(policyTag);
    return policyTag;
  }

  public Policy newPolicy(Policy policy) {
    policyDAO.insert(policy);
    policies.add(policy);
    return policy;
  }

  public Policy newPolicy(String ownerId, String name, int threatLevel) {
    Policy policy = new Policy(null /* id */, name);
    policy.setOwnerId(ownerId);
    policy.setThreatLevel(threatLevel);
    Constraint constraint = new Constraint(null, "Test Constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint);
    return newPolicy(policy);
  }

  public Policy newPolicy(String ownerId, String name) {
    return newPolicy(ownerId, null /* id */, name);
  }

  public Policy newPolicy(String ownerId, String id, String name) {
    return newPolicy(ownerId, id, name, 5);
  }

  private Policy newPolicy(String ownerId, String id, String name, int threatLevel) {
    Policy policy = new Policy(id, name);
    policy.setThreatLevel(threatLevel);
    policy.setOwnerId(ownerId);
    Constraint constraint = new Constraint(null, "Test Constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint);
    return newPolicy(policy);
  }

  public HashComponentIdentifier newClaimedComponent(String hash, ComponentIdentifier componentIdentifier) {
    HashComponentIdentifier claimedComponent = new HashComponentIdentifier(hash, componentIdentifier);
    claimedComponent.setComment("testing");
    claimedComponent.setCreateTime(new Date());
    hashComponentIdentifierDAO.insert(claimedComponent);
    claimedComponents.add(claimedComponent);
    return claimedComponent;
  }

  public PolicyEvaluation newPolicyEvaluation(String applicationId, String stageTypeId, String scanId, Date time) {
    PolicyEvaluation policyEvaluation = new PolicyEvaluation(applicationId, stageTypeId, scanId);
    policyEvaluation.setTime(time);
    policyEvaluationDAO.insert(policyEvaluation);
    return policyEvaluation;
  }

  public PolicyEvaluation newPolicyEvaluation(String applicationId, String stageTypeId, String scanId) {
    PolicyEvaluation policyEvaluation = new PolicyEvaluation(applicationId, stageTypeId, scanId);
    policyEvaluationDAO.insert(policyEvaluation);
    return policyEvaluation;
  }

  public PolicyEvaluation newPolicyEvaluation(String applicationId, String stageTypeId, String scanId,
      boolean isReevaluation, boolean isForMonitoring, Date time)
  {
    PolicyEvaluation policyEvaluation = new PolicyEvaluation(applicationId, stageTypeId, scanId, isReevaluation,
        isForMonitoring);
    policyEvaluation.setTime(time);
    policyEvaluationDAO.insert(policyEvaluation);
    return policyEvaluation;
  }

  public PolicyEvaluation newPolicyEvaluation(String applicationId, String stageTypeId, String scanId,
      boolean isReevaluation, boolean isForMonitoring, boolean isForObsoleteScan, Date time)
  {
    PolicyEvaluation policyEvaluation = new PolicyEvaluation(applicationId, stageTypeId, scanId, isReevaluation,
        isForMonitoring);
    policyEvaluation.setTime(time);
    policyEvaluation.setForObsoleteScan(isForObsoleteScan);
    policyEvaluationDAO.insert(policyEvaluation);
    return policyEvaluation;
  }

  public PolicyViolation newPolicyViolation(PolicyEvaluation evaluation, Policy policy,
      ComponentIdentifier componentIdentifier, String hash, String reason)
  {
    Constraint constraint = policy.getConstraints().get(0);
    Condition condition = constraint.getConditions().get(0);
    ConstraintFact constraintFact = new ConstraintFact(constraint.getId(), constraint.getName(), constraint
        .getOperator().name());
    ConditionFact conditionFact = new ConditionFact(condition.getConditionTypeId(), "summary", reason);
    constraintFact.addConditionFact(conditionFact);

    PolicyViolation policyViolation = new PolicyViolation(evaluation, policy, hash, componentIdentifier,
        Collections.singletonList(constraintFact), null /* pathnames */);
    policyViolationDAO.insert(policyViolation);
    return policyViolation;
  }

  public PolicyViolation newPolicyViolation(PolicyEvaluation evaluation, Policy policy, String groupId,
      String artifactId, String version, String hash, String reason)
  {
    ComponentIdentifier componentIdentifier = null;
    if (groupId != null) {
      componentIdentifier = ComponentIdentifier.createMavenCoordinates(groupId, artifactId, version);
    }
    return newPolicyViolation(evaluation, policy, componentIdentifier, hash, reason);
  }

  public PolicyViolation newPolicyViolation(PolicyEvaluation evaluation, Policy policy) {
    return newPolicyViolation(evaluation, policy, policy.getThreatLevel(), PolicyThreatCategory.LICENSE, "Group1",
        "Artifact1", "Version1");
  }

  public WaivedPolicyViolation newWaivedPolicyViolation(PolicyEvaluation evaluation, Policy policy,
      ComponentIdentifier componentIdentifier, String hash, PolicyWaiver policyWaiver)
  {
    PolicyViolation policyViolation = new PolicyViolation(evaluation, policy.getId(), policy.getName(),
        policy.getThreatLevel(), policy.getThreatCategory(), hash, componentIdentifier, "[]", "unknown.jar");
    policyViolation.setWaived(true);
    policyViolationDAO.insert(policyViolation);

    WaivedPolicyViolation waivedPolicyViolation = new WaivedPolicyViolation(policyViolation.getId(),
        policyWaiver.getId(), policyWaiver.getComment());
    waivedPolicyViolationDAO.insert(waivedPolicyViolation);

    return waivedPolicyViolation;
  }

  public WaivedPolicyViolation newWaivedPolicyViolation(PolicyEvaluation evaluation, Policy policy,
      String groupId, String artifactId, String version, String hash, PolicyWaiver policyWaiver) {
    return newWaivedPolicyViolation(evaluation, policy, ComponentIdentifier.createMavenCoordinates(groupId,
        artifactId, version), hash, policyWaiver);
  }

  public WaivedPolicyViolation newWaivedPolicyViolation(PolicyEvaluation evaluation, Policy policy,
      PolicyWaiver policyWaiver)
  {
    return newWaivedPolicyViolation(evaluation, policy, "Group1", "Artifact1", "Version1", "hash", policyWaiver);
  }

  public PolicyViolation newPolicyViolation(PolicyEvaluation evaluation, Policy policy, int threatLevel,
      PolicyThreatCategory category)
  {
    return newPolicyViolation(evaluation, policy, threatLevel, category, null /* groupId */, null /* artifactId */,
        null /* version */, "hash");
  }

  public PolicyViolation newPolicyViolation(PolicyEvaluation evaluation, Policy policy, int threatLevel,
      PolicyThreatCategory category, String groupId, String artifactId, String version)
  {
    return newPolicyViolation(evaluation, policy, threatLevel, category, groupId, artifactId, version, "hash");
  }

  public PolicyViolation newPolicyViolation(PolicyEvaluation evaluation, Policy policy, int threatLevel,
      PolicyThreatCategory category, String groupId, String artifactId, String version, String hash)
  {
    return newPolicyViolation(evaluation, policy, threatLevel, category, groupId, artifactId, version, hash, null);
  }

  public PolicyViolation newPolicyViolation(PolicyEvaluation evaluation, Policy policy, int threatLevel,
      PolicyThreatCategory category, ComponentIdentifier componentIdentifier, String hash)
  {
    return newPolicyViolation(evaluation, policy, threatLevel, category, componentIdentifier, hash, null);
  }

  public PolicyViolation newPolicyViolation(PolicyEvaluation evaluation, Policy policy, int threatLevel,
      PolicyThreatCategory category, String groupId, String artifactId, String version, String hash, String actionTypeId)
  {
    return newPolicyViolation(evaluation, policy, threatLevel, category,
        (groupId != null ? ComponentIdentifier.createMavenCoordinates(groupId, artifactId, version) : null), hash,
        actionTypeId);
  }

  public PolicyViolation newPolicyViolation(PolicyEvaluation evaluation, Policy policy, int threatLevel,
      PolicyThreatCategory category, ComponentIdentifier componentIdentifier, String hash, String actionTypeId)
  {
    PolicyViolation policyViolation = new PolicyViolation(evaluation, policy.getId(), policy.getName(), threatLevel,
        category, hash, componentIdentifier, "[]", "unknown.jar");
    policyViolation.setActionTypeId(actionTypeId);
    policyViolationDAO.insert(policyViolation);
    return policyViolation;
  }

  public FirstOccurrencePolicyViolation newFirstOccurrencePolicyViolation(String policyViolationId,
      String applicationId, String stageTypeId)
  {
    FirstOccurrencePolicyViolation firstOccurrencePolicyViolation = new FirstOccurrencePolicyViolation(
        policyViolationId, applicationId, stageTypeId);
    firstOccurrencePolicyViolationDAO.insert(firstOccurrencePolicyViolation);
    return firstOccurrencePolicyViolation;
  }

  public ApplicationComponent newApplicationComponent(String applicationId, String stageTypeId, String hash,
      ComponentIdentifier componentIdentifier)
  {
    return newApplicationComponent(applicationId, stageTypeId, hash, componentIdentifier, null /* pathnames */);
  }

  public ApplicationComponent newApplicationComponent(String applicationId, String stageTypeId, String hash,
      MatchState matchState, boolean proprietary)
  {
    return newApplicationComponent(applicationId, stageTypeId, hash,
        ComponentIdentifier.createMavenCoordinates("Group1", "Artifact1", "Version1"), null, matchState, proprietary,
        new Date());
  }

  public ApplicationComponent newApplicationComponent(String applicationId, String stageTypeId, String hash,
      ComponentIdentifier componentIdentifier, String pathnamesString)
  {
    return newApplicationComponent(applicationId, stageTypeId, hash, componentIdentifier, pathnamesString,
        MatchState.EXACT, false, new Date());
  }

  public ApplicationComponent newApplicationComponent(String applicationId, String stageTypeId, String hash,
      ComponentIdentifier componentIdentifier, String pathnamesString, MatchState matchState, boolean proprietary,
      Date time)
  {
    List<String> pathnames = StringUtils.isBlank(pathnamesString) ? null : Collections.singletonList(pathnamesString);
    ApplicationComponent applicationComponent = new ApplicationComponent(applicationId, stageTypeId, time, hash,
        componentIdentifier, matchState.getId(), IdentificationSource.SONATYPE.getId(), proprietary, pathnames);
    appComponentDAO.insert(applicationComponent);
    return applicationComponent;
  }

  public UserViewedProductNotification newUserViewedNotificationMapping(final String username,
      final String notificationId)
  {
    UserViewedProductNotification userViewedNotificationMapping = new UserViewedProductNotification();
    userViewedNotificationMapping.setUsername(username);
    userViewedNotificationMapping.setNotificationId(notificationId);

    userViewedNotificationMappingDAO.insert(userViewedNotificationMapping);
    userViewedNotificationMappings.add(userViewedNotificationMapping);
    return userViewedNotificationMapping;
  }

  public PolicyMonitoring newPolicyMonitoring(String ownerId, String stageTypeId) {
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(ownerId, stageTypeId);
    return newPolicyMonitoring(policyMonitoring);
  }

  public PolicyMonitoring newPolicyMonitoring(PolicyMonitoring policyMonitoring) {
    policyMonitoringDAO.insert(policyMonitoring);
    policyMonitorings.add(policyMonitoring);
    return policyMonitoring;
  }

  public RepositoryManager newRepositoryManager() {
    return newRepositoryManager(uuid());
  }

  public RepositoryManager newRepositoryManager(String instanceId) {
    RepositoryManager repositoryManager = new RepositoryManager();
    repositoryManager.setInstanceId(instanceId);
    repositoryManagerDAO.insert(repositoryManager);
    repositoryManagers.add(repositoryManager);
    return repositoryManager;
  }

  public Repository newRepository() {
    return newRepository(uuid());
  }

  public Repository newRepository(String publicId) {
    RepositoryManager repositoryManager = newRepositoryManager();
    return newRepository(repositoryManager, publicId);
  }

  public Repository newRepository(RepositoryManager repositoryManager, String publicId) {
    Repository repository = new Repository(repositoryManager.getId(), publicId);
    repositoryDAO.insert(repository);
    return repository;
  }

  public Repository newRepository(String repositoryManagerInstanceId, String publicId) {
    RepositoryManager repositoryManager = newRepositoryManager(repositoryManagerInstanceId);
    Repository repository = new Repository(repositoryManager.getId(), publicId);
    repositoryDAO.insert(repository);
    return repository;
  }

  public Repository newRepository(RepositoryManager repositoryManager, String publicId, boolean enabled) {
    Repository repository = new Repository(repositoryManager.getId(), publicId);
    repository.setEnabled(enabled);
    repositoryDAO.insert(repository);
    return repository;
  }

  public RepositoryPolicyViolation newRepositoryPolicyViolation(String repositoryId, int threatLevel, String pathname,
      ComponentIdentifier componentIdentifier)
  {
    return newRepositoryPolicyViolation(repositoryId, threatLevel, pathname, false, true, "policyId", "policyName",
        componentIdentifier);
  }

  public RepositoryPolicyViolation newRepositoryPolicyViolation(String repositoryId, int threatLevel, String pathname,
      boolean isWaived, boolean isLatestEval, String policyId, String policyName,
      ComponentIdentifier componentIdentifier)
  {
    RepositoryPolicyViolation policyViolation = new RepositoryPolicyViolation(repositoryId, pathname, new Date(),
        policyId, policyName, threatLevel, PolicyThreatCategory.LICENSE, "hash",
        componentIdentifier, "[]" /* constraintFacts */);
    policyViolation.setWaived(isWaived);
    policyViolation.setLatestEvaluation(isLatestEval);
    repositoryPolicyViolationDAO.insert(policyViolation);
    return policyViolation;
  }

  public RepositoryComponent newRepositoryComponent(String repositoryId)
  {
    RepositoryComponent repositoryComponent = new RepositoryComponent(repositoryId, "path", new Date(), "hash",
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), MatchState.EXACT.getId(),
        IdentificationSource.SONATYPE.getId(), new Date(), true /* canBeQuarantined */);
    repositoryComponentDAO.insert(repositoryComponent);
    return repositoryComponent;
  }

  public RepositoryPolicyViolation newRepositoryPolicyViolation(String repositoryId) {
    RepositoryPolicyViolation policyViolation = new RepositoryPolicyViolation(repositoryId, "path", new Date(),
        "policyId", "policyName", 5 /* threatLevel */, PolicyThreatCategory.LICENSE, "hash",
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), "[]" /* constraintFacts */);
    repositoryPolicyViolationDAO.insert(policyViolation);
    return policyViolation;
  }
}
