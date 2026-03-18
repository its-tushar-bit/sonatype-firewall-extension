/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.NxCollapsible;
import com.sonatype.clm.testing.functional.elements.OrgsAndPoliciesSidebar;
import com.sonatype.clm.testing.functional.elements.OwnerSummaryTile;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPageWithLimitedVisibility;
import com.sonatype.clm.testing.functional.utils.NameSupplierDictionary;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;

import com.codeborne.selenide.SelenideElement;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;

public class OrgsAndPoliciesLimitedViewPermissionTest
    extends AbstractFunctionalTest
{
  private ApplicationDAO applicationDAO;

  private OrganizationDAO organizationDAO;

  private List<Organization> organizations;

  private Organization commonAncestorOwner;

  private User developerUser;

  @Before
  public void init() {
    organizationDAO = lookup(OrganizationDAO.class);
    applicationDAO = lookup(ApplicationDAO.class);

    developerUser = tempEntity.newUser();
    refreshOrOpen(OwnerSummaryPageWithLimitedVisibility.baseUrl());
  }

  @After
  public void cleanUp() {
    logout();
  }

  @Test
  public void testOrgsAndPoliciesSideNavbar_userHasAccessToOnlyOneOrg() {
    organizations = tempEntity.newRelatedOrganizationsAsList(1, 5, 3, new NameSupplierDictionary());
    Organization organizationWithPermissions = organizations.get(2);
    commonAncestorOwner = organizationWithPermissions;

    tempEntity.newMembershipMapping(
        organizationWithPermissions.getId(),
        Role.DEVELOPER_ROLE_ID,
        developerUser.getUsername());

    login(developerUser.getUsername(), developerUser.getPassword());
    waitUntilUrl(OwnerSummaryPage.url(OwnerType.ORGANIZATION, commonAncestorOwner.getId()));
    eyesWatcher.eyesCheck("Orgs and policies user with limited view permission - CLA is not synthetic");

    List<Organization> childOrgs = organizationDAO.getByParentOrganizationId(commonAncestorOwner.getId());
    List<Application> childApps = applicationDAO.getByOrganizationId(commonAncestorOwner.getId());
    testSideNavbarContent(commonAncestorOwner, new ArrayList<>(childOrgs), new ArrayList<>(childApps));
    testMainContent(commonAncestorOwner, false);
  }

  @Test
  public void testOrgsAndPoliciesSideNavbar_userHasAccessToOnlyApplications() {
    organizations = tempEntity.newRelatedOrganizationsAsList(1, 5, 3);

    Organization parentOrg1 = organizations.get(3);
    Organization parentOrg2 = organizations.get(2);
    commonAncestorOwner = parentOrg1;

    List<Application> appsWithPermission = new ArrayList<>(applicationDAO.getByOrganizationId(parentOrg1.getId()));
    appsWithPermission.addAll(new ArrayList<>(applicationDAO.getByOrganizationId(parentOrg2.getId())));

    appsWithPermission.forEach(app -> {
      tempEntity.newMembershipMapping(
          app.getId(),
          Role.DEVELOPER_ROLE_ID,
          developerUser.getUsername());
    });

    List<Organization> childOrgs = organizationDAO.getByParentOrganizationId(commonAncestorOwner.getId());
    List<Application> childApps = applicationDAO.getByOrganizationId(parentOrg1.getId());

    login(developerUser.getUsername(), developerUser.getPassword());
    waitUntilUrl(OwnerSummaryPage.url(OwnerType.ORGANIZATION, commonAncestorOwner.getId()));
    testSideNavbarContent(commonAncestorOwner, new ArrayList<>(childOrgs), new ArrayList<>(childApps));
    testMainContent(commonAncestorOwner, true);
  }

  @Test
  public void testOrgsAndPoliciesSideNavbar_userHasAccessToRepositoriesButNoRootOrg() {
    organizations = tempEntity.newRelatedOrganizationsAsList(1, 5, 3);

    Organization parentOrg1 = organizations.get(3);
    Organization parentOrg2 = organizations.get(2);
    commonAncestorOwner = parentOrg1;

    List<Application> appsWithPermission = new ArrayList<>(applicationDAO.getByOrganizationId(parentOrg1.getId()));
    appsWithPermission.addAll(new ArrayList<>(applicationDAO.getByOrganizationId(parentOrg2.getId())));

    appsWithPermission.forEach(app -> {
      tempEntity.newMembershipMapping(
          app.getId(),
          Role.DEVELOPER_ROLE_ID,
          developerUser.getUsername());
    });

    tempEntity
        .newMembershipMapping(RepositoryContainer.REPOSITORY_CONTAINER_ID, Role.DEVELOPER_ROLE_ID,
            developerUser.getUsername());

    login(developerUser.getUsername(), developerUser.getPassword());
    waitUntilUrl(OwnerSummaryPage.url(OwnerType.ORGANIZATION, commonAncestorOwner.getId()));
    eyesWatcher.eyesCheck("Root org is shown with repositories and doted line for limited user");
  }

  @Test
  public void testOrgsAndPoliciesSideNavbar_userHasAccessToRepositoriesAndRootOrg() {
    NameSupplierDictionary nameSupplierDictionary = new NameSupplierDictionary();
    organizations = tempEntity.newRelatedOrganizationsAsList(1, 5, 1, nameSupplierDictionary);
    List<Organization> childOrgs = new LinkedList<>();

    Organization parentOrg1 = organizations.get(3);
    childOrgs.add(organizations.get(organizations.size() - 1));
    tempEntity.newRelatedOrganizationsAsList(parentOrg1, 3, 2, 1, nameSupplierDictionary);

    organizations = tempEntity.newRelatedOrganizationsAsList(1, 3, 1, nameSupplierDictionary);
    Organization parentOrg2 = organizations.get(organizations.size() - 1);
    childOrgs.add(parentOrg2);
    tempEntity.newRelatedOrganizationsAsList(parentOrg1, 3, 2, 1, nameSupplierDictionary);

    tempEntity.newMembershipMapping(
        parentOrg1.getId(),
        Role.DEVELOPER_ROLE_ID,
        developerUser.getUsername());

    tempEntity.newMembershipMapping(
        parentOrg2.getId(),
        Role.DEVELOPER_ROLE_ID,
        developerUser.getUsername());

    tempEntity
        .newMembershipMapping(RepositoryContainer.REPOSITORY_CONTAINER_ID, Role.DEVELOPER_ROLE_ID,
            developerUser.getUsername());

    commonAncestorOwner = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);

    login(developerUser.getUsername(), developerUser.getPassword());
    waitUntilUrl(OwnerSummaryPage.url(OwnerType.ORGANIZATION, commonAncestorOwner.getId()));
    eyesWatcher.eyesCheck("Root org is shown with repositories and no doted line for limited user");
  }

  @Test
  public void testOrgsAndPoliciesSideNavbar_userHasAccessToOnlyOneApplication() {
    organizations = tempEntity.newRelatedOrganizationsAsList(1, 5, 1);

    Organization parentOrg1 = organizations.get(3);
    commonAncestorOwner = parentOrg1;

    List<Application> appsWithPermission = new ArrayList<>(applicationDAO.getByOrganizationId(parentOrg1.getId()));

    appsWithPermission.forEach(app -> {
      tempEntity.newMembershipMapping(
          app.getId(),
          Role.DEVELOPER_ROLE_ID,
          developerUser.getUsername());
    });

    List<Organization> childOrgs = Collections.emptyList();
    List<Application> childApps = applicationDAO.getByOrganizationId(parentOrg1.getId());

    login(developerUser.getUsername(), developerUser.getPassword());
    waitUntilUrl(OwnerSummaryPage.url(OwnerType.ORGANIZATION, commonAncestorOwner.getId()));
    testSideNavbarContent(commonAncestorOwner, new ArrayList<>(childOrgs), new ArrayList<>(childApps));
    testMainContent(commonAncestorOwner, true);
  }

  @Test
  public void testOrgsAndPoliciesSideNavbar_userHasAccessToSeveralOgrs() {
    NameSupplierDictionary nameSupplierDictionary = new NameSupplierDictionary();
    organizations = tempEntity.newRelatedOrganizationsAsList(1, 5, 1, nameSupplierDictionary);
    List<Organization> childOrgs = new LinkedList<>();

    Organization parentOrg1 = organizations.get(3);
    childOrgs.add(organizations.get(organizations.size() - 1));
    tempEntity.newRelatedOrganizationsAsList(parentOrg1, 3, 2, 1, nameSupplierDictionary);

    organizations = tempEntity.newRelatedOrganizationsAsList(1, 3, 1, nameSupplierDictionary);
    Organization parentOrg2 = organizations.get(organizations.size() - 1);
    childOrgs.add(parentOrg2);
    tempEntity.newRelatedOrganizationsAsList(parentOrg1, 3, 2, 1, nameSupplierDictionary);

    tempEntity.newMembershipMapping(
        parentOrg1.getId(),
        Role.DEVELOPER_ROLE_ID,
        developerUser.getUsername());

    tempEntity.newMembershipMapping(
        parentOrg2.getId(),
        Role.DEVELOPER_ROLE_ID,
        developerUser.getUsername());

    List<Application> childApps = Collections.emptyList();
    commonAncestorOwner = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);

    login(developerUser.getUsername(), developerUser.getPassword());
    waitUntilUrl(OwnerSummaryPage.url(OwnerType.ORGANIZATION, commonAncestorOwner.getId()));
    eyesWatcher.eyesCheck("Orgs and policies user with limited view permission - CLA is synthetic and Root org");
    testSideNavbarContent(commonAncestorOwner, new ArrayList<>(childOrgs), new ArrayList<>(childApps));
    testMainContent(commonAncestorOwner, true);
  }

  private void testMainContent(Owner currentOwner, boolean synthetic) {
    if (synthetic) {
      OwnerSummaryPageWithLimitedVisibility.title().shouldHave(text(currentOwner.getName()));
      OwnerSummaryPageWithLimitedVisibility.tree().shouldBe(visible);
      SelenideElement description = OwnerSummaryPageWithLimitedVisibility.titleDescription();
      description.shouldBe(visible);
      description.shouldHave(text("View all organizations and applications on which you have permissions. " +
          "Click on the link for the org or app below to access details."));
    }
    else {
      OwnerSummaryTile summaryTile = OwnerSummaryPage.summaryTile();
      summaryTile.name().shouldBe(visible).shouldHave(text(currentOwner.getName()));
      summaryTile.headerIcon().shouldBe(visible);

      if (OwnerType.APPLICATION.equals(currentOwner.getType())) {
        summaryTile.publicId().shouldBe(visible).shouldHave(text(currentOwner.getPublicId()));
      }
      else {
        summaryTile.publicId().shouldBe(hidden);
      }
    }
  }

  private void testSideNavbarContent(
      Organization parentOrg,
      List<Organization> childOrgs,
      List<Application> childApps)
  {
    OrgsAndPoliciesSidebar orgsAndPoliciesSidebar = OwnerSummaryPageWithLimitedVisibility.sidebar();

    orgsAndPoliciesSidebar.selectedOrg().shouldHave(text(parentOrg.getName()));
    if (childOrgs != null && !childOrgs.isEmpty()) {
      NxCollapsible childOrganizationsCollapsible = orgsAndPoliciesSidebar.getOrganizationList();
      assertThat(childOrganizationsCollapsible).isNotNull();
      assertThat(childOrganizationsCollapsible.children()).hasSameSizeAs(childOrgs);

      childOrgs.sort(Comparator.comparing(organization -> organization.getName().toUpperCase()));
      AtomicInteger index = new AtomicInteger();
      childOrgs.forEach(organization -> {
        OrgsAndPoliciesSidebar.OwnerItem childOrgItem =
            orgsAndPoliciesSidebar.getOrganizationLink(index.getAndIncrement());
        childOrgItem.ownerName().shouldHave(text(organization.getName()));
      });
    }

    if (childApps != null && !childApps.isEmpty()) {
      NxCollapsible childApplicationsCollapsible = orgsAndPoliciesSidebar.getApplicationList();
      assertThat(childApplicationsCollapsible).isNotNull();
      assertThat(childApplicationsCollapsible.children()).hasSameSizeAs(childApps);

      childApps.sort(Comparator.comparing(application -> application.getName().toUpperCase()));
      AtomicInteger index = new AtomicInteger();
      childApps.forEach(application -> {
        OrgsAndPoliciesSidebar.OwnerItem childOrgItem =
            orgsAndPoliciesSidebar.getApplicationLink(index.getAndIncrement());
        childOrgItem.ownerName().shouldHave(text(application.getName()));
      });
    }
  }
}
