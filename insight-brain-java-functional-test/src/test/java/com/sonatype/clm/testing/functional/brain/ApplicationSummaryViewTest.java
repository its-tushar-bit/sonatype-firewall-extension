/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.testing.functional.elements.ActionDropDown;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebDriver;

import static com.codeborne.selenide.Condition.visible;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class ApplicationSummaryViewTest
    extends AbstractSummaryViewTest
{

  private static final String YE_OLE_APPLICATION = "Ye Ole Application";

  private Application application;

  @Before
  public void init() {
    application = tempEntity.newApplicationWithParent(getClass().getSimpleName(), YE_OLE_APPLICATION);
    super.init(application);
  }

  @Override
  @Test
  public void testReportLinks() {
    List<StageType> stages = new ArrayList<>();
    stages.add(StageTypes.BUILD);
    stages.add(StageTypes.STAGE_RELEASE);
    stages.add(StageTypes.RELEASE);
    stages.add(StageTypes.OPERATE);

    ActionDropDown.actionButton().click();
    ActionDropDown.reportLinks().shouldHaveSize(stages.size());

    for (int i = 0; i < ActionDropDown.reportLinks().size(); i++) {
      ActionDropDown.reportLink(i).shouldBe(visible, ActionDropDown.disabled())
          .shouldHave(ActionDropDown
              .reportLinkText(stages.get(i).getName()));
    }

    List<PolicyEvaluation> policyEvaluations = new ArrayList<>();

    for (StageType stage : stages) {
      policyEvaluations.add(
          tempEntity.newPolicyEvaluation(application.getId(), stage.getId(), stage.getId() + "FakeScanID"));
    }

    refresh();

    ActionDropDown.actionButton().click();
    ActionDropDown.reportLinks().shouldHaveSize(policyEvaluations.size());

    for (int i = 0; i < ActionDropDown.reportLinks().size(); i++) {
      ActionDropDown.reportLink(i).shouldBe(visible).shouldNotBe(ActionDropDown.disabled()).shouldHave(
          ActionDropDown.reportLinkText(stages.get(i).getName()));

      ActionDropDown.reportLink(i).followLink();
      Selenide.switchTo().window(1);

      assertThat(WebDriverRunner.getWebDriver().getCurrentUrl(),
          equalTo(ActionDropDown.reportLinkUrl(application.getPublicId(), policyEvaluations.get(i).getScanId())));

      WebDriverRunner.getWebDriver().close();
      Selenide.switchTo().window(0);
      ActionDropDown.actionButton().click();
    }
  }
}
