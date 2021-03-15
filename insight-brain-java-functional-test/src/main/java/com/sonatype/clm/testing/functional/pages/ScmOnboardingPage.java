/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.clm.testing.functional.utils.SelectorUtils;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class ScmOnboardingPage
    extends BasicElement<ScmOnboardingPage>
{
  public static String url() {
    return BaseUrl.resolvePageUrl("/onboarding");
  }

  public static String url(String organizationId) {
    return BaseUrl.resolvePageUrl("/onboarding/" + organizationId);
  }

  private static final String ROOT_SELECTOR = "#scm-onboarding-container";

  public ScmOnboardingPage() {
    super(ROOT_SELECTOR);
  }

  public ElementsCollection getPageTitleElements() {
    return children(".nx-page-title");
  }

  public SelenideElement titleReportsCta() {
    return child("#scm-reports-cta");
  }

  public SelenideElement importStatusCta() {
    return child("#scm-success-gotoreports");
  }

  public SelenideElement gitHostError() {
    return modalDialog().$(".nx-alert--error");
  }

  public SelenideElement loadError() {
    return child(".nx-alert--load-error");
  }

  public SelenideElement loadErrorLink() {
    return child(".nx-alert--load-error a");
  }

  public SelenideElement hostUrlAuthError() {
    return child(".nx-modal .nx-alert--error");
  }

  public SelenideElement hostUrlAuthErrorLink() {
    return child(".nx-modal .nx-alert--error a");
  }

  public SelenideElement loadErrorAnchor() {
    return child(".nx-alert--load-error a");
  }

  public SelenideElement retry() {
    return child(".nx-load-error__retry");
  }

  public SelenideElement resultsTable() {
    return child("#iq-scm-onboarding-repositories");
  }

  public SelenideElement resultsTableBody() {
    return child("#iq-scm-onboarding-repositories tbody");
  }

  public ElementsCollection resultsTableProject() {
    return children(".iq-scm-repository-project");
  }

  public ElementsCollection resultsTableDescription() {
    return children(".iq-scm-repository-description .nx-truncate-ellipsis");
  }

  public SelenideElement descriptionTooltip() {
    // Note that this is not a child of the top-level container element! This is an issue with NxTooltip
    return $(".iq-scm-repo-description-tooltip");
  }

  public ElementsCollection resultsTableNamespace() {
    return children(".iq-scm-repository-namespace");
  }

  public SelenideElement donutChartPercentImported() {
    return $("#scm-imported-donut-chart");
  }

  public SelenideElement resultsTableAlreadyImported() {
    return $("#scm-already-imported");
  }

  public SelenideElement resultsTableSelectAll() {
    return child("#iq-scmonboarding-select-all");
  }

  public ElementsCollection paginationButtons() {
    return children(".nx-btn--pagination");
  }

  public SelenideElement selectionCheckboxById(String id) {
    return child(".nx-checkbox", "#" + id);
  }

  public SelenideElement projectFilter() {
    return child("#iq-scmonboarding-project-filter");
  }

  public SelenideElement repositoryCount() {
    return child("#repository-count");
  }

  public SelenideElement selectedToImportCount() {
    return child("#scm-repo-to-import-count");
  }

  public SelenideElement alreadyImportedCount() {
    return child("#scm-already-imported");
  }

  public SelenideElement backButton() {
    return child(".nx-back-button");
  }

  public SelenideElement onboardingPageTitle() {
    return child(".iq-scmonboarding-title");
  }

  public SelenideElement hostUrl() {
    return child("#iq-scm-default-host-field");
  }

  public SelenideElement hostUrlInvalidMessage() {
    return child(".nx-modal-content .nx-text-input__invalid-message");
  }

  public SelenideElement modalDialog() {
    return child(".nx-modal");
  }

  public SelenideElement hostUrlContinueButton() {
    return child(".nx-modal .nx-btn-bar .nx-btn--primary");
  }

  public SelenideElement hostUrlCancelButton() {
    return child(".nx-modal .nx-btn-bar .nx-btn--undefined");
  }

  public SelenideElement loadingSpinner() {
    return child(".nx-loading-spinner");
  }

  public SelenideElement importRepoButton() {
    return child("#iq-scm-import-button");
  }

  public SelenideElement importStatusModal() {
    return child("#scm-import-status-modal");
  }

  public SelenideElement importStatusContinue() {
    return child("#scm-continue-importing");
  }

  public SelenideElement successMessage() {
    return importStatusModal().$(".nx-alert--success");
  }

  public SelenideElement errorMessage() {
    return importStatusModal().$(".nx-alert--error");
  }

  public SelenideElement importSuccessDetailMsg() {
    return child(".scm-import-detail-success");
  }

  public SelenideElement importErrorDetailMsg() {
    return child(".scm-import-detail-error");
  }

  public ElementsCollection importErrorDetails() {
    return children(".scm-import-error-detail-item");
  }

  public SelenideElement namespaceHeader() {
    return child("#namespace-header");
  }

  public SelenideElement namespaceHeaderSort() {
    return child("#namespace-header span");
  }

  public SelenideElement descriptionHeader() {
    return child("#description-header");
  }

  public SelenideElement projectHeader() {
    return child("#project-header");
  }

  public SelenideElement importLabelQuestionIcon() {
    return child("#import-label-question-icon");
  }

  public ElementsCollection orgDropdownItems() {
    return children("#iq-scm-target-organization button");
  }

  public OrganizationsDropdown organizationsDropdown() {
    return new OrganizationsDropdown();
  }

  public SelenideElement newOrgButton() {
    return child("#repository-pane-add-org");
  }

  public SelenideElement createOrgButton() {
    return child("#new-organization-modal button.nx-btn.nx-btn--primary");
  }

  public SelenideElement newOrgName() {
    return child("#new-organization-modal-org-name");
  }

  public SelenideElement newOrgModal() {
    return child("#new-organization-modal");
  }

  public SelenideElement newOrgModalError() {
    return child("#new-organization-modal .nx-load-error__message");
  }

  public static class OrganizationsDropdown
      extends BasicElement<OrganizationsDropdown>
  {
    public OrganizationsDropdown() {
      super("#iq-scm-target-organization");
    }

    public SelenideElement selectedOrganization() {
      return child(".nx-dropdown__toggle");
    }

    public SelenideElement openMenuButton() {
      return $(".nx-dropdown__toggle");
    }

    public OrganizationsDropdownMenu dropdownMenu() {
      return new OrganizationsDropdownMenu(selector);
    }
  }

  public static class OrganizationsDropdownMenu
      extends BasicElement<OrganizationsDropdownMenu>
  {
    public OrganizationsDropdownMenu(String selector) {
      super(selector, ".nx-dropdown-menu");
    }

    public ElementsCollection options() {
      return children(".iq-scm-onboarding-dropdown__option");
    }

    public OrganizationsDropdownOption option(int i) {
      return new OrganizationsDropdownOption("iq-scm-onboarding-dropdown__option", SelectorUtils.nthChild(i + 1));
    }
  }

  public static class OrganizationsDropdownOption
      extends BasicElement<OrganizationsDropdownOption>
  {
    public OrganizationsDropdownOption(String... selectors) {
      super(selectors);
    }
  }
}
