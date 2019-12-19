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
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

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

  public static SelenideElement saveButton() {
    return root().$("button[type^=submit]");
  }

  public static SelenideElement deleteButton() {
    return $("#delete-source-control-button");
  }

  public ErrorBox error() {
    return new ErrorBox(selector, ".iq-alert.iq-alert--error");
  }

  public static SelenideElement providerWarning() {
    return $("#source-control-provider-warning");
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

  public static SelenideElement repositoryUrlInfo() {
    return $("#editor-source-control-url-info");
  }

  public static SelenideElement advancedSettingsTree() {
    return $(".iq-tree-view--source-control-editor");
  }

  public static SelenideElement advancedSettings() {
    return $("#source-control-advanced-section");
  }

  public static IqRadio pullRequestsInheritRadio() {
    return new IqRadio($("#editor-source-control-pull-request-inherit"));
  }

  public static IqRadio pullRequestsEnableRadio() {
    return new IqRadio($("#editor-source-control-pull-request-enable"));
  }

  public static IqRadio pullRequestsDisableRadio() {
    return new IqRadio($("#editor-source-control-pull-request-disable"));
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

  public static SelenideElement pullRequestNotSupportedAlert() {
    return $("#source-control-pull-request-unavailable");
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
}
