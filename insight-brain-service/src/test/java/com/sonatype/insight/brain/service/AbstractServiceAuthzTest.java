/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import java.util.Date;
import java.util.UUID;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;

/**
 * Common fixture for authorization tests of the service layer components.
 *
 * <p>
 * <b>Migration Note:</b> This class uses the Spring-based test infrastructure.
 * Security configuration is provided by the Spring SecurityConfiguration.
 * </p>
 */
public class AbstractServiceAuthzTest
    extends AbstractComponentTest
{
  @Override
  protected boolean preserveAopProxies() {
    return true;
  }

  @Override
  protected boolean enforceSecurityAspects() {
    return true;
  }

  @Override
  protected void grantDefaultTestUserAllPermissions() {
    // Authz tests must start from default-deny and explicitly grant only the permission under test.
  }

  protected RepositoryManager repositoryManager;

  protected Repository repository;

  protected Organization org;

  protected Application app;

  protected User user;

  @Override
  protected void setUpSecurity() {
    repositoryManager = tempEntity.newRepositoryManager();
    repository = tempEntity.newRepository(repositoryManager, "testPublicId");
    org = tempEntity.newOrganization();
    app = tempEntity.newApplication(org.getId());
    user = tempEntity.newUser();
    subject = new Subject.Builder(securityManager()).buildSubject();
    bindSubject(subject);
  }

  @Override
  protected void tearDownSecurity() {
    super.tearDownSecurity();
  }

  protected void login() {
    if (subject.isAuthenticated()) {
      bindSubject(subject);
      return;
    }

    SimplePrincipalCollection principals = new SimplePrincipalCollection();
    principals.add(new UserPrincipal(user.getUsername(), user.getUsername(), User.INTERNAL_REALM_ID),
        User.INTERNAL_REALM_ID);

    SimpleSession session = new SimpleSession();
    session.setId(UUID.randomUUID().toString());
    session.setStartTimestamp(new Date());

    subject = new Subject.Builder(securityManager())
        .session(session)
        .principals(principals)
        .authenticated(true)
        .buildSubject();
    bindSubject(subject);
  }

  private void bindSubject(Subject subject) {
    ThreadContext.bind(securityManager());
    ThreadContext.bind(subject);
  }

  private SecurityManager securityManager() {
    return lookup(SecurityManager.class);
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

  protected void grantLegalReviewerPermission(String contextId) {
    grantPermission(contextId, Permission.LEGAL_REVIEWER);
  }

  protected void grantEditAccessControlPermission(String contextId) {
    grantPermission(contextId, Permission.EDIT_ACCESS_CONTROL);
  }

  protected String getUsername() {
    return user.getUsername();
  }
}
