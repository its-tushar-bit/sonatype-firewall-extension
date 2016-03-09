/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.ActionsNotificationsSection;
import com.sonatype.clm.testing.functional.elements.ConstraintSection;
import com.sonatype.clm.testing.functional.elements.PolicyInheritsToSection;
import com.sonatype.clm.testing.functional.elements.SummarySection;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class PolicyEditorPage
{
  public static final int DEFAULT_THREAT_LEVEL = 5;

  public static String urlToEdit(String ownerType, String ownerId, String policyId) {
    return "assets/index.html#/management/edit/" + ownerType + "/" + ownerId + "/policy/" + policyId;
  }

  public static String urlToCreate(String ownerType, String ownerId) {
    return "assets/index.html#/management/edit/" + ownerType + "/" + ownerId + "/policy";
  }

  public static SelenideElement title() {
    return $("#policy-editor-summary h2");
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

  public static SelenideElement actionsAndNotificationsPill() {
    return $("#policy-actions-button");
  }

  public static SelenideElement inhertancePill() {
    return $("#policy-inheritance-button");
  }

  public static SelenideElement createPill() {
    return $("#policy-create-button");
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

  public static ActionsNotificationsSection actionsNotificationsSection() {
    return new ActionsNotificationsSection();
  }

}
