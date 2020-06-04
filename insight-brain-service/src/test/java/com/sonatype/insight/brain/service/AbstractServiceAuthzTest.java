/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.security.SecurityAopModule;

import com.google.inject.Binder;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.guice.ShiroModule;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;

/**
 * Common fixture for authorization tests of the service layer components.
 */
public class AbstractServiceAuthzTest
    extends AbstractComponentTest
{
  protected Repository repository;

  protected Organization org;

  protected Application app;

  protected User user;

  private ShiroModule shiroModule;

  @Inject
  private SecurityManager securityManager;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    shiroModule = new ShiroModule()
    {
      @Override
      protected void configureShiro() {
        bindRealm().to(InternalRealm.class);
      }
    };
    binder.install(shiroModule);
    binder.install(new SecurityAopModule());
  }

  @Override
  protected void setUpSecurity() {
    repository = tempEntity.newRepository();
    org = tempEntity.newOrganization();
    app = tempEntity.newApplication(org.getId());
    user = tempEntity.newUser();
    ThreadContext.bind(securityManager);
    subject = (new Subject.Builder()).buildSubject();
    ThreadContext.bind(subject);
  }

  @Override
  protected void tearDownSecurity() {
    if (shiroModule != null) {
      // stop worker threads
      shiroModule.destroy();
    }
    super.tearDownSecurity();
  }

  protected void login() {
    if (!subject.isAuthenticated()) {
      subject.login(new UsernamePasswordToken(user.getUsername(), user.getPassword()));
    }
  }

  protected void grantConfigureSystemPermission() {
    grantGlobalPermission(Permission.CONFIGURE_SYSTEM);
  }

  protected void grantWritePermission() {
    grantGlobalPermission(Permission.WRITE);
  }

  protected void grantWritePermission(String contextId) {
    grantPermission(contextId, Permission.WRITE);
  }

  protected void grantReadPermission(String contextId) {
    grantPermission(contextId, Permission.READ);
  }

  protected void grantEvaluateComponentPermission(String contextId) {
    grantPermission(contextId, Permission.EVALUATE_COMPONENT);
  }

  protected void grantEvaluateApplicationPermission(String contextId) {
    grantPermission(contextId, Permission.EVALUATE_APPLICATION);
  }

  protected void grantClaimComponentPermission() {
    grantGlobalPermission(Permission.CLAIM_COMPONENT);
  }

  protected void grantManageAutomaticApplicationCreationPermission() {
    grantGlobalPermission(Permission.MANAGE_AUTOMATIC_APPLICATION_CREATION);
  }

  protected void grantManageAutomaticSourceControlPermission() {
    grantGlobalPermission(Permission.MANAGE_AUTOMATIC_SCM_CONFIGURATION);
  }

  protected void grantPermission(String contextId, Permission permission) {
    Role role = tempEntity.newRole(false /* global */, permission);
    tempEntity.newMembershipMapping(contextId, role.getId(), user.getUsername());
    login();
  }

  protected void grantGlobalPermission(Permission permission) {
    Role role = tempEntity.newRole(true /* global */, permission);
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), user.getUsername());
    login();
  }

  protected void grantAddApplicationPermission(String contextId) {
    grantPermission(contextId, Permission.ADD_APPLICATION);
  }

  protected void grantManageProprietaryPermission(String contextId) {
    grantPermission(contextId, Permission.MANAGE_PROPRIETARY);
  }

  protected String getUsername() {
    return user.getUsername();
  }
}
