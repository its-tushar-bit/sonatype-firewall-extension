/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.configuration.ldap.LdapAuthenticationMethod;
import com.sonatype.insight.brain.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.configuration.ldap.LdapProtocol;
import com.sonatype.insight.brain.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.dataaccess.component.HashGAVDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapConnectionDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.NewestPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
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
import com.sonatype.insight.brain.model.component.HashGAV;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.NewestPolicyViolation;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.RolePermission;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.model.tag.Tag;

import org.codehaus.plexus.util.StringUtils;
import org.junit.rules.ExternalResource;

/**
 * Like TemporaryFolder, just for apps and orgs etc.
 */
public class TemporaryEntity
    extends ExternalResource
{
  private static final String USER_PASSWORD_CLEAR = "secret";

  private static final String USER_PASSWORD_HASH = "$shiro1$SHA-256$10$Gsv3gW95oRKzzxp37k/wJA==$T2VDhMzPuXN7VTobkLUcwDsxxJJXj5pInbW7YUn8muY=";

  private final ApplicationDAO appDAO = new ApplicationDAO();

  private final OrganizationDAO orgDAO = new OrganizationDAO();

  private final UserDAO userDAO = new UserDAO();

  private final RoleDAO roleDAO = new RoleDAO();

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

  private final NewestPolicyViolationDAO newestPolicyViolationDAO = new NewestPolicyViolationDAO();

  private final ComponentLabelDAO componentLabelDAO = new ComponentLabelDAO();

  private final LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();

  private final LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = new LicenseThreatGroupLicenseDAO();

  private final LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();

  private final PolicyWaiverDAO waiverDAO = new PolicyWaiverDAO();

  private final LdapServerDAO ldapServerDAO = new LdapServerDAO();

  private final LdapConnectionDAO ldapConnectionDAO = new LdapConnectionDAO();

  private final LdapUserMappingDAO ldapUserMappingDAO = new LdapUserMappingDAO();

  private final HashGAVDAO hashGAVDAO = new HashGAVDAO();

  private final DashboardFilterDAO dashboardFilterDAO = new DashboardFilterDAO();

  private Collection<Application> apps;

  private Collection<Organization> orgs;

  private Collection<User> users;

  private Collection<Role> roles;

  private Collection<LdapServer> ldapServers;

  private Collection<HashGAV> claimedComponents;

  private Collection<DashboardFilter> dashboardFilters;

  @Override
  protected void before() {
    apps = new ArrayList<Application>();
    orgs = new ArrayList<Organization>();
    users = new ArrayList<User>();
    roles = new ArrayList<Role>();
    ldapServers = new ArrayList<LdapServer>();
    claimedComponents = new ArrayList<HashGAV>();
    dashboardFilters = new ArrayList<>();
  }

  @Override
  protected void after() {
    for (DashboardFilter dashboardFilter : dashboardFilters) {
      if (dashboardFilterDAO.getByUsername(dashboardFilter.getUsername()) != null) {
        dashboardFilterDAO.delete(dashboardFilter);
      }
    }
    for (Application app : apps) {
      if (appDAO.getById(app.getId()) != null) {
        appDAO.delete(app);
      }
    }
    for (Organization org : orgs) {
      if (orgDAO.getById(org.getId()) != null) {
        orgDAO.delete(org);
      }
    }
    for (User user : users) {
      if (userDAO.getById(user.getId()) != null) {
        userDAO.delete(user);
      }
    }
    for (Role role : roles) {
      if (roleDAO.getById(role.getId()) != null) {
        roleDAO.delete(role);
      }
    }
    for (LdapServer ldapServer : ldapServers) {
      if (ldapServerDAO.getById(ldapServer.getId()) != null) {
        ldapServerDAO.delete(ldapServer);
      }
    }
    for (HashGAV claimedComponent : claimedComponents) {
      if (hashGAVDAO.getById(claimedComponent.getId()) != null) {
        hashGAVDAO.delete(claimedComponent);
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

  public Organization newOrganization() {
    return newOrganization("Test Org " + uuid());
  }

  public Organization newOrganization(String name) {
    return newOrganization(name, true /* createLicenseThreatGroups */);
  }

  public Organization newOrganization(String name, boolean createLicenseThreatGroups) {
    Organization org = new Organization(name);
    orgDAO.insert(org, createLicenseThreatGroups);
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

  public void register(HashGAV... hashGAVs) {
    Collections.addAll(claimedComponents, hashGAVs);
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
    Role role = new Role();
    role.setName(name);
    role.setGlobal(global);
    roleDAO.insert(role);
    roles.add(role);
    for (Permission permission : permissions) {
      rolePermDAO.insert(new RolePermission(role.getId(), permission));
    }
    return role;
  }

  public MembershipMapping newMembershipMapping(String contextId, String roleId, String username) {
    MembershipMapping membershipMapping = new MembershipMapping(contextId, roleId, username, MemberType.USER);
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
  
  public LicenseOverride newLicenseOverride(String ownerId, String groupId, String artifactId, String version,
      LicenseOverrideStatus status, String licenseId)
  {
    return newLicenseOverride(ownerId, groupId, artifactId, version, status, licenseId, "testing");
  }

  public LicenseOverride newLicenseOverride(String ownerId, String groupId, String artifactId, String version,
      LicenseOverrideStatus status, String licenseId, String comment)
  {
    LicenseOverride override = new LicenseOverride(ownerId, groupId, artifactId, version, status, licenseId, comment);
    licenseOverrideDAO.insert(override);
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
    return policyTag;
  }

  public Policy newPolicy(String ownerId, String name, int threatLevel) {
    Policy policy = new Policy(null /* id */, name);
    policy.setOwnerId(ownerId);
    policy.setThreatLevel(threatLevel);
    Constraint constraint = new Constraint(null, "Test Constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint);
    policyDAO.insert(policy);
    return policy;
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
    policyDAO.insert(policy);
    return policy;
  }

  public HashGAV newClaimedComponent(String hash, String groupId, String artifactId, String version) {
    HashGAV claimedComponent = new HashGAV(hash, groupId, artifactId, version, "jar", "");
    claimedComponent.setComment("testing");
    hashGAVDAO.insert(claimedComponent);
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
      boolean isReevaluation, boolean idForMonitoring, Date time)
  {
    PolicyEvaluation policyEvaluation = new PolicyEvaluation(applicationId, stageTypeId, scanId, isReevaluation,
        idForMonitoring);
    policyEvaluation.setTime(time);
    policyEvaluationDAO.insert(policyEvaluation);
    return policyEvaluation;
  }

  public PolicyViolation newPolicyViolation(PolicyEvaluation evaluation, Policy policy, String groupId,
      String artifactId, String version, String hash, String reason)
  {
    Constraint constraint = policy.getConstraints().get(0);
    Condition condition = constraint.getConditions().get(0);
    ConstraintFact constraintFact = new ConstraintFact(constraint.getId(), constraint.getName(), constraint
        .getOperator().name());
    ConditionFact conditionFact = new ConditionFact(condition.getConditionTypeId(), "summary", reason);
    constraintFact.addConditionFact(conditionFact);
    PolicyViolation policyViolation = new PolicyViolation(evaluation, policy, hash, groupId, artifactId, version,
        Collections.singletonList(constraintFact), null /* pathnames */);
    policyViolationDAO.insert(policyViolation);
    return policyViolation;
  }

  public PolicyViolation newPolicyViolation(PolicyEvaluation evaluation, Policy policy) {
    return newPolicyViolation(evaluation, policy, policy.getThreatLevel(), PolicyThreatCategory.LICENSE, "Group1",
        "Artifact1", "Version1");
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
      PolicyThreatCategory category, String groupId, String artifactId, String version, String hash, String actionTypeId)
  {
    PolicyViolation policyViolation = new PolicyViolation(evaluation, policy.getId(), policy.getName(), threatLevel,
        category, hash, groupId, artifactId, version, "[]", "unknown.jar");
    policyViolation.setActionTypeId(actionTypeId);
    policyViolationDAO.insert(policyViolation);
    return policyViolation;
  }

  public NewestPolicyViolation newNewestPolicyViolation(String policyViolationId, String applicationId,
      String stageTypeId)
  {
    NewestPolicyViolation newestPolicyViolation = new NewestPolicyViolation(policyViolationId, applicationId,
        stageTypeId);
    newestPolicyViolationDAO.insert(newestPolicyViolation);
    return newestPolicyViolation;
  }

  public ApplicationComponent newApplicationComponent(String applicationId, String stageTypeId, String hash,
      String groupId, String artifactId, String version)
  {
    return newApplicationComponent(applicationId, stageTypeId, hash, groupId, artifactId, version, null /* pathnames */);
  }

  public ApplicationComponent newApplicationComponent(String applicationId, String stageTypeId, String hash,
      String groupId, String artifactId, String version, String pathnamesString)
  {
    List<String> pathnames = StringUtils.isBlank(pathnamesString) ? null : Collections.singletonList(pathnamesString);
    ApplicationComponent applicationComponent = new ApplicationComponent(applicationId, stageTypeId, new Date(), hash,
        groupId, artifactId, version, MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(),
        false /* proprietary */, pathnames);
    appComponentDAO.insert(applicationComponent);
    return applicationComponent;
  }
}
