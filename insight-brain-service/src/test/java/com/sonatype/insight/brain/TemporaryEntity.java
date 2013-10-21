/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.User;

import org.junit.rules.ExternalResource;

/**
 * Like TemporaryFolder, just for apps and orgs etc.
 */
public class TemporaryEntity
    extends ExternalResource
{
  private final ApplicationDAO appDAO = new ApplicationDAO();

  private final OrganizationDAO orgDAO = new OrganizationDAO();

  private final UserDAO userDAO = new UserDAO();

  private Collection<Application> apps;

  private Collection<Organization> orgs;

  private Collection<User> users;

  @Override
  protected void before() throws Throwable {
    apps = new ArrayList<Application>();
    orgs = new ArrayList<Organization>();
    users = new ArrayList<User>();
  }

  @Override
  protected void after() {
    for (Application app : apps) {
      appDAO.delete(app);
    }
    for (Organization org : orgs) {
      orgDAO.delete(org);
    }
    for (User user : users) {
      userDAO.delete(user);
    }
  }

  private static String uuid() {
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
    User user = new User(username, "pwd-" + username, "John", "Doe", username + "@void.com");
    userDAO.insert(user);
    users.add(user);
    return user;
  }
}
