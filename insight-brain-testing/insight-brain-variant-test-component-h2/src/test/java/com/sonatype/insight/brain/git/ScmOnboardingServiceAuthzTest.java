/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.Collections;
import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.git.dto.ImportRepositoriesRequest;
import com.sonatype.insight.brain.git.dto.ImportScmOrganizationRequest;
import com.sonatype.insight.brain.git.dto.OnboardingOrganization;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlOrganizationImportEvent;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class ScmOnboardingServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  private static final String PROVIDER = "github";

  public static final String GITHUB_COM = "http://github.com";

  @Inject
  public ScmOnboardingService scmOnboardingService;

  @Test
  public void testLoadRepositories_Authorized() {
    grantAddApplicationPermission(org.getId());
    grantReadPermission(org.getId());

    assertThatThrownBy(() -> scmOnboardingService.loadRepositories(org.getId(), GITHUB_COM))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("No provider configured");
  }

  @Test
  public void testLoadRepositories_Unauthenticated() throws Exception {
    assertThrows(UnauthenticatedException.class,
        () -> scmOnboardingService.loadRepositories(org.getId(), GITHUB_COM));
  }

  @Test
  public void testLoadRepositories_Unauthenticated_nullOrg() throws Exception {
    assertThrows(UnauthenticatedException.class,
        () -> scmOnboardingService.loadRepositories(null, GITHUB_COM));
  }

  @Test
  public void testLoadRepositories_Unauthorized() throws Exception {
    login();
    assertThrows(UnauthorizedException.class,
        () -> scmOnboardingService.loadRepositories(org.getId(), GITHUB_COM));
  }

  @Test
  public void testGetDefaultHostUrl_Authorized() {
    grantAddApplicationPermission(org.getId());
    grantReadPermission(org.getId());
    scmOnboardingService.getDefaultHostUrl(PROVIDER, org.getId());
  }

  @Test
  public void testGetDefaultHostUrl_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> scmOnboardingService.getDefaultHostUrl(PROVIDER, org.getId()));
  }

  @Test
  public void testGetDefaultHostUrl_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> scmOnboardingService.getDefaultHostUrl(PROVIDER, org.getId()));
  }

  @Test
  public void testImportRepositories_Authorized() {
    grantAddApplicationPermission(org.getId());
    scmOnboardingService
        .importRepositories(org.getId(), new ImportRepositoriesRequest(Collections.emptyList(), 0, 0));
  }

  @Test
  public void testImportRepositories_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> scmOnboardingService.importRepositories(org.getId(), new ImportRepositoriesRequest()));
  }

  @Test
  public void testImportRepositories_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> scmOnboardingService.importRepositories(org.getId(), new ImportRepositoriesRequest()));
  }

  @Test
  public void testValidateScmHostUrl_Authorized() {
    grantGlobalPermission(Permission.READ);
    scmOnboardingService.validateScmHostUrl("GITHUB", "https://localhost/org/proj");
  }

  @Test
  public void testValidateScmHostUrl_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> scmOnboardingService.validateScmHostUrl("GITHUB", "https://localhost/org/proj"));
  }

  @Test
  public void testValidateScmHostUrl_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> scmOnboardingService.validateScmHostUrl("GITHUB", "https://localhost/org/proj"));
  }

  @Test
  public void testGetOrgsForOnboarding_Authorized() {
    grantReadPermission(org.getId());

    final List<OnboardingOrganization> organizations = scmOnboardingService.getOrgsForOnboarding();
    assertThat(organizations).hasSize(1);
    final OnboardingOrganization onboardingOrganization = organizations.get(0);
    assertThat(onboardingOrganization.organization.getId()).isEqualTo(org.getId());
    assertThat(onboardingOrganization.organization.getName()).isEqualTo(org.getName());
  }

  @Test
  public void testGetOrgsForOnboarding_partiallyAuthorized() {
    // given a second org
    Organization org2 = tempEntity.newOrganization("org2");

    // given read permission to only one org
    grantReadPermission(org.getId());

    // when we get the list of orgs
    List<OnboardingOrganization> organizations = scmOnboardingService.getOrgsForOnboarding();

    // then it should only be the org where we have permissions
    assertThat(organizations).hasSize(1);
    final OnboardingOrganization onboardingOrganization = organizations.get(0);
    assertThat(onboardingOrganization.organization.getId()).isEqualTo(org.getId());
    assertThat(onboardingOrganization.organization.getName()).isEqualTo(org.getName());

    // when we get permission to the other org
    grantReadPermission(org2.getId());

    // and we re-query
    organizations = scmOnboardingService.getOrgsForOnboarding();

    // then it should have all orgs
    assertThat(organizations).hasSize(2);
  }

  @Test
  public void testGetOrgsForOnboarding_Unauthorized() {
    final List<OnboardingOrganization> organizations = scmOnboardingService.getOrgsForOnboarding();
    assertThat(organizations).isEmpty();
  }

  @Test
  public void testImportScmOrganization_Unauthenticated() throws Exception {
    assertThrows(UnauthenticatedException.class,
        () -> scmOnboardingService.importScmOrganization(org.getId(), new ImportScmOrganizationRequest()));
  }

  @Test
  public void testImportScmOrganization_Unauthorized() throws Exception {
    login();
    assertThrows(UnauthorizedException.class,
        () -> scmOnboardingService.importScmOrganization(org.getId(), new ImportScmOrganizationRequest()));
  }

  @Test
  public void testImportScmOrganization_Authorized() {
    grantAddApplicationPermission(org.getId());
    assertThrows(BadRequestException.class,
        () -> scmOnboardingService.importScmOrganization(org.getId(), new ImportScmOrganizationRequest()));
  }

  @Test
  public void testGetImportScmOrganizationStatus_Unauthenticated() throws Exception {
    SourceControlOrganizationImportEvent event = tempEntity.newSourceControlOrganizationImportEvent();
    assertThrows(UnauthenticatedException.class,
        () -> scmOnboardingService.getImportScmOrganizationStatus(org.getId(), event.getId()));
  }

  @Test
  public void testGetImportScmOrganizationStatus_Unauthorized() throws Exception {
    login();
    SourceControlOrganizationImportEvent event = tempEntity.newSourceControlOrganizationImportEvent();
    assertThrows(UnauthorizedException.class,
        () -> scmOnboardingService.getImportScmOrganizationStatus(org.getId(), event.getId()));
  }

  @Test
  public void testGetImportScmOrganizationStatus_Authorized() throws Exception {
    grantAddApplicationPermission(org.getId());
    SourceControlOrganizationImportEvent event =
        tempEntity.newSourceControlOrganizationImportEvent(org.getId(), "url", -1, 0);
    assertThat(scmOnboardingService.getImportScmOrganizationStatus(org.getId(), event.getId())).isNotNull();
  }
}
