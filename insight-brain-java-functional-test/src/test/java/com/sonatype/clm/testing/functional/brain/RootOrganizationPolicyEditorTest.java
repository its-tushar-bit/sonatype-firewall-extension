/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.Collections;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ConstraintSection;
import com.sonatype.clm.testing.functional.elements.ConstraintSection.ConstraintEditSection;
import com.sonatype.clm.testing.functional.elements.ConstraintSection.ConstraintEditSection.AgeConditionEditSection;
import com.sonatype.clm.testing.functional.elements.SummarySection;
import com.sonatype.clm.testing.functional.elements.ThreatDropdownSelector;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.PolicyEditorPage;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.AgeInDaysConditionType;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class RootOrganizationPolicyEditorTest
    extends AbstractFunctionalTest
{
  private Organization rootOrganization;

  private PolicyDAO policyDAO;

  @Before
  public void init() {
    policyDAO = lookup(PolicyDAO.class);

    rootOrganization = lookup(OrganizationDAO.class).getById(ROOT_ORGANIZATION_ID);
    refreshOrOpen(OwnerSummaryPage.url(rootOrganization));
    OwnerSummaryPage.summaryTile().name().shouldHave(text(rootOrganization.getName()));
  }

  @BeforeClass
  public static void boot() {
    refreshOrOpen(OwnerSummaryPage.urlToRootOrg());
    loginAsAdmin();
  }

  @Test
  public void testQuarantineWarningOnCreatePolicy() {
    setFeatures(LicensedFeature.RELEASE_INTEGRITY, LicensedFeature.HYGIENE, LicensedFeature.POLICY_MONITORING,
        LicensedFeature.POLICY_MANAGEMENT, LicensedFeature.POLICY_READ_ONLY, LicensedFeature.ENFORCEMENT,
        LicensedFeature.NOTIFICATIONS, LicensedFeature.WEBHOOKS_FOR_APPLICATIONS, LicensedFeature.DASHBOARD,
        LicensedFeature.CUSTOM_POLICIES);

    refreshOrOpen(OwnerSummaryPage.url(rootOrganization));
    OwnerSummaryPage.policyTile().addPolicyButton().click();

    SummarySection summary = PolicyEditorPage.summarySection();
    summary.policyName().input().val("New Policy");

    ThreatDropdownSelector.dropdownButton().shouldBe(visible, enabled).click();
    ThreatDropdownSelector.threatLevelListItem(1).shouldBe(visible).click();
    ThreatDropdownSelector.selectedThreatLabel().shouldHave(text(String.valueOf(9)));

    PolicyEditorPage.actionsSection().quarantineWarningMessage().shouldNotBe(visible);
    PolicyEditorPage.actionsSection().proxy().failRadio().click();
    PolicyEditorPage.actionsSection().quarantineWarningMessage().shouldBe(visible);

    ScrollUtil.awaitEndOfScrolling(PolicyEditorPage.actionsSection().quarantineWarningMessage().scrollIntoView(true));

    PolicyEditorPage.actionsSection().build().warnRadio().click();

    ConstraintSection constraintSection = PolicyEditorPage.constraintSection();
    ConstraintEditSection newConstraint = constraintSection.constraintEditor(0);
    newConstraint.name().shouldBe(empty).val("New Constraint");
    AgeConditionEditSection ageCondition = newConstraint.ageCondition(0);
    ageCondition.value().age().shouldBe(empty).val("3");

    PolicyEditorPage.savePolicy();
    PolicyEditorPage.actionsSection().quarantineWarningMessage().shouldNotBe(visible);

    Policy newPolicy = getPolicyByName("New Policy");
    assertThat(newPolicy).isNotNull();
    assertThat(newPolicy.getActions()).containsEntry(Stage.ID_BUILD, "warn");
    assertThat(newPolicy.getActions()).containsEntry(Stage.ID_PROXY, "fail");
  }

  @Test
  public void testQuarantineWarningOnEditPolicy() {
    createPolicy();
    refresh();
    OwnerSummaryPage.policyTile().localPolicyList().row(1).click();

    PolicyEditorPage.actionsSection().quarantineWarningMessage().shouldNotBe(visible);
    PolicyEditorPage.actionsSection().proxy().failRadio().click();
    PolicyEditorPage.actionsSection().quarantineWarningMessage().shouldBe(visible);
    PolicyEditorPage.actionsSection().proxy().warnRadio().click();
    PolicyEditorPage.actionsSection().quarantineWarningMessage().shouldNotBe(visible);
    PolicyEditorPage.actionsSection().proxy().failRadio().click();
    PolicyEditorPage.actionsSection().quarantineWarningMessage().shouldBe(visible);

    ScrollUtil.awaitEndOfScrolling(PolicyEditorPage.actionsSection().quarantineWarningMessage().scrollIntoView(true));
    eyesWatcher.eyesCheck("testQuarantineWarningOnEditPolicy - quarantineWarningMessage is shown");

    PolicyEditorPage.savePolicy();
    PolicyEditorPage.actionsSection().quarantineWarningMessage().shouldNotBe(visible);

    refresh();
    PolicyEditorPage.actionsSection().quarantineWarningMessage().shouldNotBe(visible);
    PolicyEditorPage.actionsSection().proxy().warnRadio().click();
    PolicyEditorPage.actionsSection().quarantineWarningMessage().shouldNotBe(visible);
    PolicyEditorPage.actionsSection().proxy().failRadio().click();
    PolicyEditorPage.actionsSection().quarantineWarningMessage().shouldNotBe(visible);
  }

  private Policy getPolicyByName(String policyName) {
    for (Policy p : policyDAO.getByOwnerId(rootOrganization.getId())) {
      if (p.getName().equals(policyName)) {
        return p;
      }
    }
    return null;
  }

  private Policy createPolicy() {
    Policy policy = tempEntity.newPolicy(rootOrganization.getId(), "original name", 1);
    Constraint constraint1 = new Constraint(policy.getId() + "1", "First Constraint with One Condition", null);
    constraint1.addCondition(new Condition(AgeInDaysConditionType.ID, "older than", "730"));
    policy.setConstraints(Collections.singletonList(constraint1));

    policy.setAction(Stage.ID_DEVELOP, Action.ID_WARN);
    policy.setAction(Stage.ID_BUILD, Action.ID_FAIL);

    policyDAO.update(policy);
    return policy;
  }
}
