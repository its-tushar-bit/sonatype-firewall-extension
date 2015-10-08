/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.DeleteModal;
import com.sonatype.clm.testing.functional.pages.LabelEditorPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.label.Label;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.open;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class LabelEditorTest
    extends AbstractFunctionalTest
{
  private Label label;

  @BeforeClass
  public static void beforeClass() {
    open(ReportListPage.URL);
    loginAsAdmin();
  }

  @Before
  public void before() {
    // Application name can include characters that need to be encoded
    Application application = tempEntity.newApplicationWithParent(getClass().getSimpleName());
    label = tempEntity.newLabel(application.getId());
    refreshOrOpen(LabelEditorPage.url(application.getType().toString(), application.getPublicId(), label.getId()));
  }

  @Test
  public void testDeleteLabelModal() {
    LabelEditorPage.deleteButton().shouldBe(visible);
    LabelEditorPage.deleteButton().click();

    DeleteModal.root().shouldBe(visible);
    DeleteModal.header().shouldHave(DeleteModal.headerText("Label"));
    DeleteModal.body().shouldHave(DeleteModal.bodyText(label.getLabel()));
  }

  @Test
  public void testDeleteLabel() {
    LabelEditorPage.deleteButton().shouldBe(visible);
    LabelEditorPage.deleteButton().click();

    DeleteModal.root().shouldBe(visible);
    DeleteModal.deleteButton().click();

    // Modal should be hidden 800 ms after delete REST call is successful
    DeleteModal.root().shouldNotBe(visible);

    LabelDAO labelDAO = new LabelDAO();
    label = labelDAO.getById(label.getId());

    assertThat(label, is(nullValue()));
  }

  @Test
  public void testDeleteLabelCancel() {
    LabelEditorPage.deleteButton().shouldBe(visible);
    LabelEditorPage.deleteButton().click();

    DeleteModal.root().shouldBe(visible);
    DeleteModal.cancelButton().click();

    DeleteModal.root().shouldNotBe(visible);
    LabelEditorPage.labelName().shouldHave(value(label.getLabel()));

    LabelDAO labelDAO = new LabelDAO();
    label = labelDAO.getById(label.getId());

    assertThat(label, is(not(nullValue())));
  }
}
