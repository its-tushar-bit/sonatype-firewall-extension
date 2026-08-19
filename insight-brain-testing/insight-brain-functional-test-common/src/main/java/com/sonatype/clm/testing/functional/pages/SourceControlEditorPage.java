/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.NxFormSelect;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Selenide.$;

public class SourceControlEditorPage
    extends BasicElement<SourceControlEditorPage>
{
  private static final String SOURCE_CONTROL_EDITOR_ID = "#source-control-editor";

  public static String url(final String ownerType, final String ownerId) {
    return BaseUrl.resolvePageUrl("/management/edit/{ownerType}/{ownerId}/source-control", ownerType, ownerId);
  }

  public SourceControlEditorPage() {
    super(SOURCE_CONTROL_EDITOR_ID);
  }

  private static final SelenideElement root = $("#source-control-editor");

  public static SelenideElement root() {
    return root;
  }

  public static SelenideElement title() {
    return root().$("h1");
  }

  public static SelenideElement subTitle() {
    return $(".nx-page-title__description");
  }

  public static NxFormSelect providerSelect() {
    return new NxFormSelect("#source-control-provider-select");
  }

  public static SourceControlFieldset credentialsFieldset() {
    return new SourceControlFieldset("#editor-source-control-token");
  }

  public static SelenideElement token() {
    return $("#source-control-token");
  }

  public static SelenideElement username() {
    return $("#source-control-username");
  }

  public static SourceControlFieldset providerFieldset() {
    return new SourceControlFieldset("#editor-source-control-provider");
  }

  public static SelenideElement saveButton() {
    return root().$(".nx-form__submit-btn");
  }

  public static SelenideElement resetButton() {
    return $("#reset-source-control-button");
  }

  public static SelenideElement testConfigButton() {
    return $("#test-source-control-config-button");
  }

  public static SelenideElement validationError() {
    return root().$(".nx-form__validation-errors");
  }

  public static SelenideElement tokenWarning() {
    return $("#source-control-token-warning");
  }

  public static SelenideElement repositoryUrlControls() {
    return $("#editor-source-control-url");
  }

  public static SelenideElement repositoryUrl() {
    return $("#editor-source-control-url");
  }

  public static SourceControlFieldset sshEnabledFieldset() {
    return new SourceControlFieldset("#source-control-ssh");
  }

  public static SelenideElement remediationPullRequestNotSupportedAlert() {
    return $("#source-control-remediation-pull-requests-unavailable");
  }

  public static SelenideElement pullRequestCommentingNotSupportedAlert() {
    return $("#source-control-pull-request-commenting-unavailable");
  }

  public static SelenideElement sourceControlEvaluationsNotSupportedAlert() {
    return $("#source-control-evaluations-unavailable");
  }

  public static SourceControlFieldset remediationPullRequestsFieldset() {
    return new SourceControlFieldset("#source-control-remediation-pull-requests");
  }

  public static SourceControlFieldset pullRequestCommentingFieldset() {
    return new SourceControlFieldset("#source-control-pull-request-commenting");
  }

  public static SourceControlFieldset sourceControlEvaluationsFieldset() {
    return new SourceControlFieldset("#source-control-evaluations");
  }

  public static SourceControlFieldset automatedCommitFeedbackFieldset() {
    return new SourceControlFieldset("#automated-commit-feedback");
  }

  public static SourceControlFieldset manualPullRequestsFieldset() {
    return new SourceControlFieldset("#manual-pull-requests");
  }

  public static SourceControlFieldset baseBranchFieldset() {
    return new SourceControlFieldset("#source-control-default-branch");
  }

  public static SourceControlFieldset innerSourceAutomatedUpdatesFieldset() {
    return new SourceControlFieldset("#inner-source-automated-updates");
  }

  public static SelenideElement baseBranchInput() {
    return $("#editor-source-control-branch");
  }

  public static SelenideElement defaultBranchNotSupportedAlert() {
    return $("#source-control-base-branch-unavailable");
  }

  public static SelenideElement notSupported() {
    return $("#source-control-not-supported");
  }

  public static SelenideElement form() {
    return $(".nx-form");
  }

  public static MetricsTable metricsTable() {
    return new MetricsTable();
  }

  public static SelenideElement testResultsElement() {
    return $("#scm-config-results");
  }

  public static TestResults testResults() {
    return new TestResults("#scm-config-results");
  }

  public static class SourceControlFieldset
      extends BasicElement<SourceControlFieldset>
  {
    public SourceControlFieldset(String selector) {
      super(selector);
    }

    public SelenideElement mainLabel() {
      return this.getElement().$(".nx-radio-checkbox.nx-radio");
    }

    public SelenideElement toggle() {
      return this.getElement().$(".nx-toggle__input");
    }

    public SelenideElement toggleControl() {
      return child(".nx-toggle__control");
    }

    public ElementsCollection radioInputs() {
      return this.getElement().$$(".nx-radio__input");
    }

    public ElementsCollection labels() {
      return this.getElement().$$(".nx-radio-checkbox__content");
    }
  }

  public static class TestResults
      extends BasicElement<TestResults>
  {
    public TestResults(String selector) {
      super(selector);
      $(selector).scrollIntoView(false);
    }

    public SelenideElement title() {
      return child("#scm-config-results-title");
    }

    public ElementsCollection rows() {
      return children("ul li.nx-list__item");
    }
  }

  /**
   * Represents the table of Pull Request metrics.
   */
  public static class MetricsTable
      extends BasicElement<MetricsTable>
  {
    public MetricsTable() {
      super(".iq-automated-pr-table");
    }

    public int rowCount() {
      return rows().size();
    }

    public ElementsCollection rows() {
      return getElement().$$("table tbody tr");
    }

    public MetricsTableRow getRow(int index) {
      return new MetricsTableRow(rows().get(index));
    }

    public void scrollIntoView() {
      rows().last().scrollIntoView(true);
    }
  }

  /**
   * Represents a single row in the table.
   */
  public static class MetricsTableRow
  {
    private SelenideElement row;

    public MetricsTableRow(final SelenideElement selenideElement) {
      this.row = selenideElement;
    }

    public ElementsCollection columns() {
      return row.findAll("td");
    }

    public boolean isEmpty() {
      ElementsCollection td = columns();
      return td.size() == 1 && td.get(0).has(attribute("colspan", "4"));
    }

    public SelenideElement title() {
      return columns().get(0);
    }

    public SelenideElement statusIcon() {
      return columns().get(1).find("svg");
    }

    public Tooltip statusIconTooltip() {
      return Tooltip.get();
    }

    public SelenideElement totalTime() {
      return columns().get(2);
    }

    public SelenideElement started() {
      return columns().get(3);
    }
  }
}
