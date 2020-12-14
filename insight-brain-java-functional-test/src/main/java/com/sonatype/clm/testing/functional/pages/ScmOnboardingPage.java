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

  public SelenideElement loadError() {
    return child(".nx-alert--load-error");
  }

  public OrganizationsDropdown organizationsDropdown() {
    return new OrganizationsDropdown();
  }

  public SelenideElement resultsTable() {
    return child("#iq-scm-onboarding-repositories");
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
  
  public SelenideElement resultsTablePercentageImported() {
    return $("#scm-import-percentage");
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
    return child("#project-filter input");
  }

  public SelenideElement repositoryCount() {
    return child("#repository-count");
  }

  public SelenideElement selectedTotalCount() {
    return child("#selected-total-count");
  }

  public SelenideElement selectedRepositoryCount() {
    return child("#selected-repository-count");
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
    return child(".host-url-tile .nx-text-input__invalid-message");
  }

  public SelenideElement reloadRepoButton() {
    return child("#iq-scm-load-button");
  }

  public SelenideElement loadingSpinner() {
    return child(".nx-loading-spinner");
  }

  public SelenideElement importRepoButton() {
    return child("#iq-scm-import-button");
  }

  public SelenideElement successMessage() {
    return child(".nx-alert--success");
  }

  public SelenideElement infoMessage() {
    return child(".nx-alert--info");
  }

  public SelenideElement errorMessage() {
    return child(".nx-alert--error");
  }

  public SelenideElement namespaceHeader() {
    return child("#namespace-header");
  }

  public SelenideElement descriptionHeader() {
    return child("#description-header");
  }

  public SelenideElement projectHeader() {
    return child("#project-header");
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
