/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.CLM;
import com.sonatype.clm.testing.functional.elements.OwnerEditorDialog;
import com.sonatype.clm.testing.functional.elements.OwnerTreeView;
import com.sonatype.clm.testing.functional.elements.OwnerTreeView.OrganizationNode;
import com.sonatype.clm.testing.functional.elements.OwnerTreeView.RootOrganizationNode;
import com.sonatype.clm.testing.functional.elements.UnsavedModal;
import com.sonatype.clm.testing.functional.pages.OrganizationManagementPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;

import com.codeborne.selenide.WebDriverRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.disappear;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CreateOwnerTest
    extends AbstractFunctionalTest
{
  private static final String APP_PUBLIC_ID = "a..bcd";

  private static final String NAME = "gibberish";

  private ApplicationDAO appDAO = new ApplicationDAO();

  private OrganizationDAO organizationDAO = new OrganizationDAO();

  private static Organization parentOrg;

  @BeforeClass
  public static void beforeClass() {
    parentOrg = staticTempEntity.newOrganization("Parent");
    refreshOrOpen(OrganizationManagementPage.URL);
    loginAsAdmin();
  }

  @Before
  public void init() {
    refreshOrOpen(OrganizationManagementPage.URL);
  }

  @After
  public void cleanup() {
    Organization org = getOrgByName(NAME);
    if (org != null) {
      organizationDAO.delete(org);
    }

    Application app = new ApplicationDAO().getByPublicId(APP_PUBLIC_ID);
    if (app != null) {
      appDAO.delete(app);
    }
  }

  @Test
  public void testCreateApplication() throws Exception {
    OwnerTreeView.organizationElements().shouldHaveSize(1);
    OrganizationNode orgNode = OwnerTreeView.organization(0);
    orgNode.treeViewElement().click();
    orgNode.newApplicationButton().shouldBe(visible, enabled).click();

    testIconDirtyState();

    // open application
    OwnerEditorDialog.name().shouldBe(visible, empty).shouldHave(CLM.INITIAL_VALUE);
    OwnerEditorDialog.publicId().shouldBe(visible, empty).shouldHave(CLM.INITIAL_VALUE);
    OwnerEditorDialog.saveButton().shouldBe(disabled);

    // check invalid name
    OwnerEditorDialog.name().val("$$$");
    popoverViolations(OwnerEditorDialog.name()).shouldBe(visible).shouldHave(text("Use valid characters"));

    // should not be able to proceed w/ name error
    OwnerEditorDialog.publicId().val(APP_PUBLIC_ID);
    OwnerEditorDialog.saveButton().shouldBe(disabled);

    // Error should get removed
    OwnerEditorDialog.name().val(NAME);
    popoverViolations(OwnerEditorDialog.name()).shouldNot(exist);
    OwnerEditorDialog.saveButton().shouldBe(enabled);

    // invalid id
    OwnerEditorDialog.publicId().val("a c d $$");
    OwnerEditorDialog.saveButton().shouldBe(disabled);
    popoverViolations(OwnerEditorDialog.publicId()).shouldBe(visible).shouldHave(text("Use valid characters"));

    // check error goes away
    OwnerEditorDialog.publicId().val(APP_PUBLIC_ID);
    popoverViolations(OwnerEditorDialog.publicId()).shouldNot(exist);
    OwnerEditorDialog.saveButton().shouldBe(enabled);

    OwnerEditorDialog.name().shouldNotHave(CLM.INITIAL_VALUE);
    OwnerEditorDialog.publicId().shouldNotHave(CLM.INITIAL_VALUE);

    OwnerEditorDialog.saveButton().click();
    OwnerEditorDialog.root().should(disappear);

    OwnerSummaryPage.SummaryTile.name().should(appear).shouldHave(text(NAME));

    // check backend
    Application app = appDAO.getByPublicId(APP_PUBLIC_ID);
    assertNotNull(app);
    assertEquals(APP_PUBLIC_ID, app.getPublicId());
    assertEquals(parentOrg.getId(), app.getOrganizationId());
    assertEquals(NAME, app.getName());

    orgNode.applicationElements().shouldHaveSize(1).get(0).shouldHave(text(NAME));
  }

  @Test
  public void testCreateOrganization() throws Exception {
    RootOrganizationNode.treeViewElement().shouldBe(visible, enabled).click();
    RootOrganizationNode.newOrganizationButton().shouldBe(visible, enabled).click();

    testIconDirtyState();

    OwnerEditorDialog.name().shouldBe(visible, empty).shouldHave(CLM.INITIAL_VALUE);
    OwnerEditorDialog.publicId().shouldNot(exist);
    OwnerEditorDialog.saveButton().shouldBe(disabled);

    // check invalid name
    OwnerEditorDialog.name().val("$$$");
    popoverViolations(OwnerEditorDialog.name()).shouldBe(visible).shouldHave(text("Use valid characters"));

    // should not be able to proceed w/ name error
    OwnerEditorDialog.saveButton().shouldBe(disabled);

    // Error should get removed
    OwnerEditorDialog.name().val(NAME);
    popoverViolations(OwnerEditorDialog.name()).shouldNot(exist);
    OwnerEditorDialog.saveButton().shouldBe(enabled);

    OwnerEditorDialog.saveButton().click();
    OwnerEditorDialog.root().should(disappear);

    // check backend
    Organization org = getOrgByName(NAME);
    assertNotNull(org);

    OwnerSummaryPage.SummaryTile.name().should(appear).shouldHave(text(NAME));

    OwnerTreeView.organizationElements().shouldHaveSize(2);
    OwnerTreeView.organizationElements().findBy(text(NAME));
  }

  private void testIconDirtyState() {
    OwnerEditorDialog.robotIcon().click();

    UnsavedModal unsavedModal = new UnsavedModal();
    WebDriverRunner.getWebDriver().navigate().back();
    unsavedModal.cancelButton().shouldBe(visible).click();

    OwnerEditorDialog.defaultIcon().click();
  }

  private Organization getOrgByName(String name) {
    for (Organization canadidate : organizationDAO.getAll()) {
      if (name.equals(canadidate.getName())) {
        return canadidate;
      }
    }
    return null;
  }
}
