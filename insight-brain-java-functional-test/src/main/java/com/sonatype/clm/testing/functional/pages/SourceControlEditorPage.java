/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Dropdown;
import com.sonatype.clm.testing.functional.elements.ErrorBox;
import com.sonatype.clm.testing.functional.elements.IqRadio;
import com.sonatype.clm.testing.functional.elements.IqToggle;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.*;
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
    return root().$("h2");
  }

  public static SelenideElement subTitle() {
    return $(".iq-tile-header__subtitle");
  }

  public static Dropdown provider() {
    return new Dropdown(SOURCE_CONTROL_EDITOR_ID, "dropdown-selector");
  }

  public static IqRadio tokenInheritRadio() {
    return new IqRadio($("#editor-source-control-token-inherit"));
  }

  public static IqRadio tokenOverrideRadio() {
    return new IqRadio($("#editor-source-control-token-override"));
  }

  public static SelenideElement token() {
    return $("#editor-source-control-token");
  }

  public static SelenideElement credentialsUsername() {
    return $("#editor-source-control-credentials-username");
  }

  public static SelenideElement credentialsToken() {
    return $("#editor-source-control-credentials-token");
  }

  public static IqRadio credentialsInheritRadio() {
    return new IqRadio($("#editor-source-control-credentials-inherit"));
  }

  public static IqRadio credentialsOverrideRadio() {
    return new IqRadio($("#editor-source-control-credentials-override"));
  }

  public static IqRadio providerInheritRadio() {
    return new IqRadio($("#editor-source-control-provider-inherit"));
  }

  public static IqRadio providerOverrideRadio() {
    return new IqRadio($("#editor-source-control-provider-override"));
  }

  public static SelenideElement saveButton() {
    return root().$("button[type^=submit]");
  }

  public static SelenideElement deleteButton() {
    return $("#delete-source-control-button");
  }

  public static SelenideElement testConfigButton() {
    return $("#test-source-control-config-button");
  }

  public ErrorBox error() {
    return new ErrorBox(selector, ".iq-alert.iq-alert--error");
  }

  public static SelenideElement tokenWarning() {
    return $("#source-control-token-warning");
  }

  public static SelenideElement repositoryUrlControls() {
    return $("#repository-url-row");
  }

  public static SelenideElement repositoryUrl() {
    return $("#editor-source-control-url");
  }

  public static SelenideElement advancedSettingsTree() {
    return $(".iq-tree-view--source-control-editor");
  }

  public static SelenideElement advancedSettings() {
    return $("#source-control-advanced-section");
  }

  public static IqToggle sshEnabledToggle() {
    return new IqToggle($("#editor-source-control-ssh-enabled-toggle"));
  }

  public static IqRadio sshEnabledInheritRadio() {
    return new IqRadio($("#editor-source-control-ssh-enabled-inherit"));
  }

  public static IqRadio sshEnabledEnableRadio() {
    return new IqRadio($("#editor-source-control-ssh-enabled-enable"));
  }

  public static IqRadio sshEnabledDisableRadio() {
    return new IqRadio($("#editor-source-control-ssh-enabled-disable"));
  }

  public static IqRadio remediationPullRequestsInheritRadio() {
    return new IqRadio($("#editor-source-control-remediation-pull-requests-inherit"));
  }

  public static IqRadio remediationPullRequestsEnableRadio() {
    return new IqRadio($("#editor-source-control-remediation-pull-requests-enable"));
  }

  public static IqRadio remediationPullRequestsDisableRadio() {
    return new IqRadio($("#editor-source-control-remediation-pull-requests-disable"));
  }

  public static SelenideElement remediationPullRequestNotSupportedAlert() {
    return $("#source-control-remediation-pull-requests-unavailable");
  }

  public static IqRadio pullRequestCommentingInheritRadio() {
    return new IqRadio($("#editor-source-control-pull-request-commenting-inherit"));
  }

  public static IqRadio pullRequestCommentingEnableRadio() {
    return new IqRadio($("#editor-source-control-pull-request-commenting-enable"));
  }

  public static IqRadio pullRequestCommentingDisableRadio() {
    return new IqRadio($("#editor-source-control-pull-request-commenting-disable"));
  }

  public static SelenideElement pullRequestCommentingNotSupportedAlert() {
    return $("#source-control-pull-request-commenting-unavailable");
  }

  public static IqRadio sourceControlEvaluationsInheritRadio() {
    return new IqRadio($("#editor-source-control-evaluations-inherit"));
  }

  public static IqRadio sourceControlEvaluationsEnableRadio() {
    return new IqRadio($("#editor-source-control-evaluations-enable"));
  }

  public static IqRadio sourceControlEvaluationsDisableRadio() {
    return new IqRadio($("#editor-source-control-evaluations-disable"));
  }

  public static SelenideElement sourceControlEvaluationsNotSupportedAlert() {
    return $("#source-control-evaluations-unavailable");
  }

  public static IqToggle remediationPullRequestsToggle() {
    return new IqToggle($("#editor-source-control-remediation-pull-requests-toggle"));
  }

  public static IqToggle pullRequestCommentingToggle() {
    return new IqToggle($("#editor-source-control-pull-request-commenting-toggle"));
  }

  public static IqToggle sourceControlEvaluationsToggle() {
    return new IqToggle($("#editor-source-control-evaluations-toggle"));
  }

  public static IqRadio baseBranchInheritRadio() {
    return new IqRadio($("#editor-source-control-branch-inherit"));
  }

  public static IqRadio baseBranchOverrideRadio() {
    return new IqRadio($("#editor-source-control-branch-override"));
  }

  public static SelenideElement baseBranchInput() {
    return $("#editor-source-control-branch");
  }

  public static SelenideElement advancedElementsTrigger() {
    return $("#source-control-advanced-settings-trigger");
  }

  public static SelenideElement defaultBranchNotSupportedAlert() {
    return $("#source-control-base-branch-unavailable");
  }

  public static SelenideElement advancedSectionRule() {
    return $(".iq-hr");
  }

  public static SelenideElement notSupported() {
    return $("#source-control-not-supported");
  }

  public static SelenideElement form() {
    return $(".iq-form");
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

  public static class TestResults
      extends BasicElement<TestResults>
  {
    public TestResults(String selector)  {
      super(selector);
      $(selector).scrollIntoView(false);
    }

    public SelenideElement title() {
      return child("h4");
    }

    public ElementsCollection rows() {
      return children("ul li.iq-list__item");
    }
  }

  /**
   * Represents the table of Pull Request metrics.
   */
  public static class MetricsTable
      extends BasicElement<MetricsTable>
  {
    public MetricsTable() {
      super("#metricsTable");
    }

    public int rowCount() {
      return rows().size();
    }

    public ElementsCollection rows() {
      return children("table tbody tr");
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

    public boolean isPopulated() {
      return !isEmpty();
    }

    public boolean isEmpty() {
      ElementsCollection td = columns();
      return td.size() == 1 && td.get(0).has(attribute("colspan", "5"));
    }

    public String title() {
      return columns().get(0).text();
    }

    public boolean created() {
      return columns().get(1).find("i").has(cssClass("fa-check-circle"));
    }

    public String totalTime() {
      return columns().get(2).text();
    }

    public String errors() {
      return columns().get(3).text();
    }

    public String started() {
      return columns().get(4).text();
    }
  }
}
