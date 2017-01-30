/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.ActionsSection;
import com.sonatype.clm.testing.functional.elements.ConstraintSection;
import com.sonatype.clm.testing.functional.elements.NotificationsSection;
import com.sonatype.clm.testing.functional.elements.PolicyInheritsToSection;
import com.sonatype.clm.testing.functional.elements.SummarySection;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.OwnerType;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class PolicyEditorPage
{
  public static final int DEFAULT_THREAT_LEVEL = 5;

  public static String urlToEdit(OwnerType ownerType, String ownerId, String policyId) {
    return urlToEdit(ownerType.toString(), ownerId, policyId);
  }

  public static String urlToEdit(String ownerType, String ownerId, String policyId) {
    return urlToCreate(ownerType, ownerId) + "/" + policyId;
  }

  public static String urlToCreate(OwnerType ownerType, String ownerId) {
    return urlToCreate(ownerType.toString(), ownerId);
  }

  public static String urlToCreate(String ownerType, String ownerId) {
    return BaseUrl.uriBuilder().fragment("/management/edit/{ownerType}/{ownerId}/policy").build(ownerType, ownerId)
        .toString();
  }

  public static SelenideElement title() {
    return $("#policy-editor-summary h1");
  }

  public static SelenideElement saveButton() {
    return $("#save-policy-button");
  }

  public static SelenideElement deleteButton() {
    return $("#delete-policy-button");
  }

  public static SelenideElement constraintsPill() {
    return $("#policy-constraints-button");
  }

  public static SelenideElement actionsPill() {
    return $("#policy-actions-button");
  }

  public static SelenideElement notificationsPill() {
    return $("#policy-notifications-button");
  }

  public static SelenideElement inhertancePill() {
    return $("#policy-inheritance-button");
  }

  public static SelenideElement endOfPagePill() {
    return $("#policy-endofpage-button");
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

  public static ActionsSection actionsSection() {
    return new ActionsSection();
  }

  public static NotificationsSection notificationsSection() {
    return new NotificationsSection();
  }

}
