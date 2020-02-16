/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.SourceControlEditorPage;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import static com.codeborne.selenide.Condition.text;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractSourceControlEditorTest
    extends AbstractFunctionalTest
{
  static final SourceControlEditorPage PAGE = new SourceControlEditorPage();

  static String TOKEN = "secret_key";

  protected final SourceControlDAO sourceControlDAO = new SourceControlDAO();

  Organization rootOrganization;

  protected Organization organization;

  protected OrganizationDAO organizationDAO = new OrganizationDAO();

  protected static final String YE_OLE_APPLICATION = "Ye Ole Application";

  protected static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  @Before
  public void init() {
    rootOrganization = organizationDAO.getById(ROOT_ORGANIZATION_ID);
  }

  @After
  public void after() {
    deleteRootOrgSourceControl();
  }

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(OwnerSummaryPage.urlToRootOrg());
    loginAsAdmin();
  }

  void deleteRootOrgSourceControl() {
    final SourceControl sourceControl = sourceControlDAO.getByOwnerId(ROOT_ORGANIZATION_ID);
    sourceControlDAO.delete(sourceControl);
  }

  void deleteSourceControl(final String ownerId) {
    final SourceControl sourceControl = sourceControlDAO.getByOwnerId(ownerId);
    sourceControlDAO.delete(sourceControl);
  }

  void assertSourceControl(final String ownerId,
                           final String repositoryUrl,
                           final String token,
                           final SourceControlProvider provider)
  {
    final SourceControl sourceControl = sourceControlDAO.getByOwnerId(ownerId);
    assertThat(sourceControl.getProvider()).isEqualTo(provider);
    assertThat(sourceControl.getRepositoryUrl()).isEqualTo(repositoryUrl);
    assertThat(sourceControl.getOwnerId()).isEqualTo(ownerId);
    assertThat(sourceControl.getToken()).isEqualTo(token);
  }

  void assertSourceControlDoesNotExist(final String ownerId) {
    final SourceControl sourceControl = sourceControlDAO.getByOwnerId(ownerId);
    assertThat(sourceControl).isNull();
  }

  void assertToolTip(final String text) {
    Tooltip.get().shouldHave(text(text));
  }

  abstract void verifyStartNoSourceControl();

  abstract void verifyStartWithSourceControl();
}
