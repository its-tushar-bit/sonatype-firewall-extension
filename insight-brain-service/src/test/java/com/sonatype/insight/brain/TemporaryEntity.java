/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import com.sonatype.insight.brain.configuration.ldap.LdapAuthenticationMethod;
import com.sonatype.insight.brain.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.configuration.ldap.LdapProtocol;
import com.sonatype.insight.brain.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapConnectionDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
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
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

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

  private final ComponentLabelDAO componentLabelDAO = new ComponentLabelDAO();

  private final LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();

  private final LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = new LicenseThreatGroupLicenseDAO();

  private final LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();

  private final PolicyWaiverDAO waiverDAO = new PolicyWaiverDAO();

  private final LdapServerDAO ldapServerDAO = new LdapServerDAO();

  private final LdapConnectionDAO ldapConnectionDAO = new LdapConnectionDAO();

  private final LdapUserMappingDAO ldapUserMappingDAO = new LdapUserMappingDAO();

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

  @Override
  protected void before() throws Throwable {
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
  }

  public String uuid() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  public Organization newOrganization() {
    return newOrganization("Test Org " + uuid());
  }

  public Organization newOrganization(String name) {
    Organization org = new Organization(name);
    orgDAO.insert(org);
    orgs.add(org);
    return org;
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

  public User newUser() {
    return newUser("user-" + uuid());
  }

  public User newUser(String username) {
    User user = new User(username, USER_PASSWORD_HASH, "John", "Doe", username + "@void.com");
    userDAO.insert(user);
    users.add(user);
    user.setPassword(USER_PASSWORD_CLEAR);
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
    return newLabel(ownerId, null);
  }

  public Label newLabel(String ownerId, Color color) {
    return newLabel(ownerId, uuid(), color);
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

  public void assertTag(Tag expected, Tag actual) {
    assertThat(actual.getOrganizationId(), is(expected.getOrganizationId()));
    assertThat(actual.getName(), is(expected.getName()));
    assertThat(actual.getNameLowercaseNoWhitespace(), is(expected.getNameLowercaseNoWhitespace()));
    assertThat(actual.getDescription(), is(expected.getDescription()));
    assertThat(actual.getColor(), is(expected.getColor()));
  }
}
