/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Application;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class ApplicationLatestEvaluationsPage
    extends BasicElement<ApplicationLatestEvaluationsPage>
{
  public static final int POLICY_EVALUATION_LIMIT = 20;

  private static final String ROOT = "#application-latest-evaluations-page";

  public ApplicationLatestEvaluationsPage() {
    super(ROOT);
  }

  public static String url(final Application application, final String stageId) {
    return BaseUrl.resolvePageUrl(
        String.format("/applicationLatestEvaluations/%s/stage/%s", application.getPublicId(), stageId));
  }

  public SelenideElement title() {
    return child(".nx-page-title .nx-h1");
  }

  public SelenideElement description() {
    return child(".nx-page-title__description");
  }

  public SelenideElement table() {
    return child(".nx-table");
  }

  public ElementsCollection tableHeaders() {
    return children("table thead th");
  }

  public ElementsCollection tableBodyRows() {
    return children("table tbody tr");
  }

  public ElementsCollection tableBodyRowColumns(final int row) {
    return tableBodyRows().get(row).findAll("td");
  }

  public SelenideElement reportLink(final int row) {
    return tableBodyRowColumns(row).get(5).find("a");
  }

  public SelenideElement criticalPolicyViolationCount(final int row) {
    return tableBodyRowColumns(row).get(3).find(".nx-small-threat-counter--critical .nx-small-threat-counter__count");
  }

  public SelenideElement severePolicyViolationCount(final int row) {
    return tableBodyRowColumns(row).get(3).find(".nx-small-threat-counter--severe .nx-small-threat-counter__count");
  }

  public SelenideElement moderatePolicyViolationCount(final int row) {
    return tableBodyRowColumns(row).get(3).find(".nx-small-threat-counter--moderate .nx-small-threat-counter__count");
  }
}
