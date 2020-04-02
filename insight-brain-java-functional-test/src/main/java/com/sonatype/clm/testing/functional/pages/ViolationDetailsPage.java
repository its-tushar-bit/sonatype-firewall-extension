/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.NxBackButton;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.Condition;

public class ViolationDetailsPage
    extends BasicElement<ViolationDetailsPage>
{
  public static final String ROOT = "#violation-page";

  public static String url(String violationId) {
    return BaseUrl.resolvePageUrl("/violation/{id}", violationId);
  }

  public ViolationDetailsPage() {
    super(ROOT);
  }

  public NxBackButton backButton() {
    return new NxBackButton(childSelector(".nx-page-sidebar"));
  }

  public ViolationDetailsTile detailsTile() {
    return new ViolationDetailsTile(childSelector("#violation-details-tile"));
  }

  public static class ViolationDetailsTile
      extends BasicElement<ViolationDetailsTile>
  {
    private ViolationDetailsTile(String selector) {
      super(selector);
    }

    public SelenideElement headerTitle() {
      return child(".nx-tile-header__title");
    }

    public SelenideElement headerSubtitle() {
      return child(".nx-tile-header__subtitle");
    }

    public SelenideElement threatLevel() {
      return child(".iq-violation-details__threat-level dd");
    }

    public SelenideElement firstReported() {
      return child(".iq-violation-details__first-reported dd");
    }

    public SelenideElement lastReported() {
      return child(".iq-violation-details__last-reported dd");
    }

    public SelenideElement policyType() {
      return child(".iq-violation-details__policy-type dd");
    }

    public ElementsCollection stages() {
      return children(".iq-violation-details__stages dd");
    }

    public ViolationDetailsStage stage(int index) {
      return new ViolationDetailsStage(
          childSelector(".iq-violation-details__stages dd:nth-of-type(" + (index + 1) + ")"));
    }

    public SelenideElement policyOwner() {
      return child(".iq-violation-details__policy-owner dd a");
    }
  }

  public static class ViolationDetailsStage
      extends BasicElement<ViolationDetailsStage>
  {
    private ViolationDetailsStage(String selector) {
      super(selector + " .iq-violation-details__stage");
    }

    public SelenideElement icon() {
      return child(".nx-icon");
    }

    public SelenideElement link() {
      return child("a");
    }

    public static Condition unused() {
      return Condition.cssClass("iq-violation-details__stage--unused");
    }
  }
}
