/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.ActionsSection;
import com.sonatype.clm.testing.functional.elements.ConstraintSection;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.NotificationsSection;
import com.sonatype.clm.testing.functional.elements.PolicyInheritsToSection;
import com.sonatype.clm.testing.functional.elements.SummarySection;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;

public class PolicyEditorPage
{
  public static final int DEFAULT_THREAT_LEVEL = 5;

  public static String urlToEdit(Owner owner, String policyId) {
    boolean isOrgOrApp = owner.getType().equals(OwnerType.ORGANIZATION) ||
            owner.getType().equals(OwnerType.APPLICATION);
    String ownerId = isOrgOrApp ? owner.getPublicId() : owner.getId();
    return urlToEdit(owner.getType(), ownerId, policyId);
  }

  public static String urlToEdit(OwnerType ownerType, String ownerId, String policyId) {
    return urlToCreate(ownerType, ownerId) + "/" + policyId;
  }

  public static String urlToCreate(Owner owner) {
    boolean isOrgOrApp = owner.getType().equals(OwnerType.ORGANIZATION) ||
            owner.getType().equals(OwnerType.APPLICATION);
    String ownerId = isOrgOrApp ? owner.getPublicId() : owner.getId();
    return urlToCreate(owner.getType(), ownerId);
  }

  public static String urlToCreate(OwnerType ownerType, String ownerId) {
    return BaseUrl.resolvePageUrl("/management/edit/{ownerType}/{ownerId}/policy", ownerType, ownerId);
  }

  public static String firewallUrlToEdit(OwnerType ownerType, String ownerId, String policyId) {
    return firewallUrlToCreate(ownerType, ownerId) + "/" + policyId;
  }

  public static String firewallUrlToCreate(OwnerType ownerType, String ownerId) {
    return BaseUrl.resolvePageUrl("/firewall/management/edit/{ownerType}/{ownerId}/policy", ownerType, ownerId);
  }

  public static SelenideElement title() {
    return $("#policy-editor-summary h1");
  }

  public static SelenideElement footer() {
    return $(".nx-footer");
  }

  public static SelenideElement saveButton() {
    return $(".nx-form__submit-btn");
  }

  public static SelenideElement deleteButton() {
    return $("#delete-policy-button");
  }

  public static SummarySection summarySection() {
    return new SummarySection();
  }

  public static ConstraintSection constraintSection() {
    return new ConstraintSection();
  }

  public static PolicyInheritsToSection inheritanceSection() {
    return new PolicyInheritsToSection();
  }

  public static SelenideElement disabledActionsMessage() {
    return $("#actions-disabled-message");
  }

  public static SelenideElement disabledNotificationsMessage() {
    return $("#notifications-disabled-message");
  }

  public static SelenideElement disabledLegacyViolationMessage() {
    return $("#legacy-violation-disabled-message");
  }

  public static SelenideElement legacyViolationCheckbox() {
    return $("#editor-legacy-violation-checkbox");
  }

  public static ActionsSection actionsSection() {
    return new ActionsSection();
  }

  public static NotificationsSection notificationsSection() {
    return new NotificationsSection();
  }

  public static SelenideElement alert() {
    return $(".nx-alert__content");
  }

  public static SelenideElement linkToLifecycle() {
    return $(".policy-editor-lifecycle-link");
  }

  public static void savePolicy() {
    ScrollUtil.scrollIntoView(saveButton());
    saveButton().shouldNotHave(DISABLED).click();
    FormMask.seeAndWaitForDismissal();
    ScrollUtil.awaitEndOfScrolling(saveButton());
  }

  public static SelenideElement deleteConfirmationInput() {
    return $("#policy-delete-modal input[type='text']");
  }

  public static SelenideElement deleteConfirmationError() {
    return $("#policy-delete-modal .nx-field-validation-message");
  }

  public static SelenideElement deleteConfirmationFormError() {
    return $("#policy-delete-modal .nx-form__validation-errors");
  }
}
