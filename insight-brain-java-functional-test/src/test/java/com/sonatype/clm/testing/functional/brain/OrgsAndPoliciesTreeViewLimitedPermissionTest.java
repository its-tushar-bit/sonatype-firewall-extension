/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.OwnerTreeViewPage;
import com.sonatype.clm.testing.functional.utils.NameSupplierDictionary;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.text;

public class OrgsAndPoliciesTreeViewLimitedPermissionTest
    extends AbstractFunctionalTest
{
  private ApplicationDAO applicationDAO;

  private List<Organization> organizations;

  private User developerUser;

  private List<Organization> syntheticOrgs = new ArrayList<>();

  private List<Application> applicationsWithPermission = new ArrayList<>();

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(DashboardPage.url());
  }

  @Before
  public void init() {
    applicationDAO = lookup(ApplicationDAO.class);

    organizations = tempEntity.newRelatedOrganizationsAsList(2, 3, 3, new NameSupplierDictionary());
    syntheticOrgs =
        organizations.stream()
            .filter(org -> org.getParentOrganizationId().equals(Organization.ROOT_ORGANIZATION_ID))
            .collect(
                Collectors.toList());
    for (Organization org : syntheticOrgs) {
      applicationsWithPermission.addAll(applicationDAO.getByOrganizationId(org.getId()));
    }

    developerUser = tempEntity.newUser();
    applicationsWithPermission.forEach(app -> {
      tempEntity.newMembershipMapping(
          app.getId(),
          Role.DEVELOPER_ROLE_ID,
          developerUser.getUsername());
    });

    login(developerUser.getUsername(), developerUser.getPassword());
    refreshOrOpen(OwnerTreeViewPage.url());
  }

  @Test
  public void testOwnerTree_limitedPermission() {
    eyesWatcher.eyesCheck("owner tree view limited permissions");

    ElementsCollection treeItems = OwnerTreeViewPage.tree().clickableTreeItems();
    // only applications are clickable
    treeItems.shouldHave(size(6));

    SelenideElement itemToClick = treeItems.get(0);
    String applicationName = itemToClick.text();
    Application application =
        applicationsWithPermission.stream().filter(app -> app.getName().equals(applicationName)).findFirst().get();
    ScrollUtil.scrollIntoView(itemToClick);
    itemToClick.click();

    waitUntilUrl(OwnerSummaryPage.url(OwnerType.APPLICATION, application.getPublicId()));

    SelenideElement title = OwnerSummaryPage.summaryTile().name();
    title.shouldHave(text(applicationName));
  }
}
