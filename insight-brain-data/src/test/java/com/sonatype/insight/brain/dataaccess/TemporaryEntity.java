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
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.HashGAV;
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

  private Collection<Application> apps;

  private Collection<Organization> orgs;

  private Collection<User> users;

  private Collection<Role> roles;

  private Collection<Label> labels;

  private Collection<LicenseThreatGroup> licenseThreatGroups;

  private Collection<LicenseOverride> licenseOverrides;

  private Collection<PolicyWaiver> waivers;

  private Collection<LdapServer> ldapServers;

  private Collection<Tag> tags;

  private Collection<ApplicationTag> appTags;

  private Collection<Policy> policies;

  private Collection<HashGAV> claimedComponents;

  private Collection<PolicyEvaluation> policyEvaluations;

  private Collection<PolicyViolation> policyViolations;

  @Override
  protected void before() {
    apps = new ArrayList<Application>();
    orgs = new ArrayList<Organization>();
    users = new ArrayList<User>();
    roles = new ArrayList<Role>();
    labels = new ArrayList<Label>();
    licenseThreatGroups = new ArrayList<LicenseThreatGroup>();
    licenseOverrides = new ArrayList<LicenseOverride>();
    waivers = new ArrayList<PolicyWaiver>();
    ldapServers = new ArrayList<LdapServer>();
    tags = new ArrayList<Tag>();
    appTags = new ArrayList<ApplicationTag>();
    policies = new ArrayList<>();
    claimedComponents = new ArrayList<HashGAV>();
    policyEvaluations = new ArrayList<>();
    policyViolations = new ArrayList<>();
  }

  @Override
  protected void after() {
    for (Tag tag : tags) {
      if (tagDAO.getById(tag.getId()) != null) {
        PolicyTagDAO policyTagDAO = new PolicyTagDAO();
        for (PolicyTag policyTag : policyTagDAO.getByTagId(tag.getId())) {
          policyTagDAO.delete(policyTag);
        }
        tagDAO.delete(tag);
      }
    }
    for (PolicyViolation policyViolation : policyViolations) {
      if (policyViolationDAO.getById(policyViolation.getId()) != null) {
        policyViolationDAO.delete(policyViolation);
      }
    }
    for (PolicyEvaluation policyEvaluation : policyEvaluations) {
      if (policyEvaluationDAO.getById(policyEvaluation.getId()) != null) {
        policyEvaluationDAO.delete(policyEvaluation);
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
    for (Label label : labels) {
      if (labelDAO.getById(label.getId()) != null) {
        labelDAO.delete(label);
      }
    }
    for (LicenseThreatGroup ltg : licenseThreatGroups) {
      if (licenseThreatGroupDAO.getById(ltg.getId()) != null) {
        licenseThreatGroupDAO.delete(ltg);
      }
    }
    for (LicenseOverride override : licenseOverrides) {
      if (licenseOverrideDAO.getById(override.getId()) != null) {
        licenseOverrideDAO.delete(override);
      }
    }
    for (PolicyWaiver waiver : waivers) {
      if (waiverDAO.getById(waiver.getId()) != null) {
        waiverDAO.delete(waiver);
      }
    }
    for (LdapServer ldapServer : ldapServers) {
      if (ldapServerDAO.getById(ldapServer.getId()) != null) {
        ldapServerDAO.delete(ldapServer);
      }
    }
    for (ApplicationTag appTag : appTags) {
      if (appTagDAO.getById(appTag.getId()) != null) {
        appTagDAO.delete(appTag);
      }
    }
    for (Policy policy : policies) {
      if (policyDAO.getById(policy.getId()) != null) {
        policyDAO.delete(policy);
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
    labels.add(label);
    return label;
  }

  public ComponentLabel newComponentLabel(String ownerId, String labelId){
    ComponentLabel componentLabel = new ComponentLabel(ownerId, labelId, uuid().substring(0, 19));
    componentLabelDAO.insert(componentLabel);
    return componentLabel;
  }

  public LicenseThreatGroup newLicenseThreatGroup(String ownerId) {
    LicenseThreatGroup ltg = new LicenseThreatGroup(ownerId, "LTG " + uuid(), 5);
    licenseThreatGroupDAO.insert(ltg);
    licenseThreatGroups.add(ltg);
    return ltg;
  }

  public LicenseThreatGroup newLicenseThreatGroup(String ownerId, String name, int threatLevel) {
    LicenseThreatGroup ltg = new LicenseThreatGroup(ownerId, name, threatLevel);
    licenseThreatGroupDAO.insert(ltg);
    licenseThreatGroups.add(ltg);
    return ltg;
  }

  public LicenseThreatGroupLicense newLicenseThreatGroupLicense(String ownerId, String licenseThreatGroupId) {
    LicenseThreatGroupLicense licenseThreatGroupLicense = new LicenseThreatGroupLicense(ownerId, licenseThreatGroupId,
        "Apache-2.0");
    licenseThreatGroupLicenseDAO.insert(licenseThreatGroupLicense);
    return licenseThreatGroupLicense;
  }

  public LicenseOverride newLicenseOverride(String ownerId, String groupId, String artifactId, String version,
      LicenseOverrideStatus status, String licenseId)
  {
    LicenseOverride override = new LicenseOverride(ownerId, groupId, artifactId, version, status, licenseId, "testing");
    licenseOverrideDAO.insert(override);
    licenseOverrides.add(override);
    return override;
  }

  public PolicyWaiver newWaiver(String policyId, String ownerId) {
    return newWaiver(null, policyId, ownerId);
  }

  public PolicyWaiver newWaiver(String hash, String policyId, String ownerId) {
    PolicyWaiver waiver = new PolicyWaiver(hash, policyId, ownerId, "testing");
    waiverDAO.insert(waiver);
    waivers.add(waiver);
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
    appTags.add(appTag);
    return appTag;
  }

  public PolicyTag newPolicyTag(String policyId, String tagId) {
    PolicyTag policyTag = new PolicyTag(policyId, tagId);
    policyTagDAO.insert(policyTag);
    return policyTag;
  }

  public Policy newPolicy(String ownerId, String name) {
    return newPolicy(ownerId, null /* id */, name);
  }

  public Policy newPolicy(String ownerId, String id, String name) {
    Policy policy = new Policy(id, name);
    policy.setOwnerId(ownerId);
    Constraint constraint = new Constraint(null, "Test Constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint);
    policyDAO.insert(policy);
    policies.add(policy);
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
    policyEvaluations.add(policyEvaluation);
    return policyEvaluation;
  }

  public PolicyEvaluation newPolicyEvaluation(String applicationId, String stageTypeId, String scanId) {
    PolicyEvaluation policyEvaluation = new PolicyEvaluation(applicationId, stageTypeId, scanId);
    policyEvaluationDAO.insert(policyEvaluation);
    policyEvaluations.add(policyEvaluation);
    return policyEvaluation;
  }

  public PolicyEvaluation newPolicyEvaluation(String applicationId, String stageTypeId, String scanId,
      boolean isReevaluation, boolean idForMonitoring, Date time)
  {
    PolicyEvaluation policyEvaluation = new PolicyEvaluation(applicationId, stageTypeId, scanId, isReevaluation,
        idForMonitoring);
    policyEvaluation.setTime(time);
    policyEvaluationDAO.insert(policyEvaluation);
    policyEvaluations.add(policyEvaluation);
    return policyEvaluation;
  }

  public PolicyViolation newPolicyViolation(String policyEvaluationId, Policy policy) {
    return newPolicyViolation(policyEvaluationId, policy, 5, PolicyThreatCategory.LICENSE, "Group1", "Artifact1",
        "Version1");
  }

  public PolicyViolation newPolicyViolation(String policyEvaluationId, Policy policy, int threatLevel,
                                            PolicyThreatCategory category, String groupId, String artifactId, String version) {
    PolicyViolation policyViolation = new PolicyViolation(policyEvaluationId, policy.getId(), policy.getName(), threatLevel,
        category, "hash", groupId, artifactId, version, "[]");
    policyViolationDAO.insert(policyViolation);
    policyViolations.add(policyViolation);
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
}
