/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import javax.inject.Inject;

import com.sonatype.insight.brain.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.CLMRealm;
import com.sonatype.insight.brain.security.CLMShiroAopModule;

import com.google.inject.Binder;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.guice.ShiroModule;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

/**
 * Common fixture for authorization tests of the service layer components.
 */
public class AbstractServiceAuthzTest
    extends AbstractComponentTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  protected Organization org;

  protected Application app;

  private User user;

  @Inject
  private SecurityManager securityManager;

  private Subject subject;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    binder.install(new ShiroModule()
    {
      @Override
      protected void configureShiro() {
        bindRealm().to(CLMRealm.class);
      }
    });
    binder.install(new CLMShiroAopModule());
  }

  @Before
  public void setUpSecurity() {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplication(org.getId());
    user = tempEntity.newUser();
    ThreadContext.bind(securityManager);
    subject = (new Subject.Builder()).buildSubject();
    ThreadContext.bind(subject);
  }

  @After
  public void tearDownSecurity() {
    ThreadContext.unbindSecurityManager();
    ThreadContext.unbindSubject();
  }

  protected void login() {
    if (!subject.isAuthenticated()) {
      subject.login(new UsernamePasswordToken(user.getUsername(), user.getPassword()));
    }
  }

  protected void grantAdminPermission() {
    Role role = tempEntity.newRole(true /* global */, Permission.ADMIN);
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), user.getUsername());
    login();
  }

  protected void grantWritePermission() {
    Role role = tempEntity.newRole(true /* global */, Permission.WRITE);
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), user.getUsername());
    login();
  }

  protected void grantWritePermission(String contextId) {
    Role role = tempEntity.newRole(false /* global */, Permission.WRITE);
    tempEntity.newMembershipMapping(contextId, role.getId(), user.getUsername());
    login();
  }

  protected void grantReadPermission(String contextId) {
    Role role = tempEntity.newRole(false /* global */, Permission.READ);
    tempEntity.newMembershipMapping(contextId, role.getId(), user.getUsername());
    login();
  }
}
