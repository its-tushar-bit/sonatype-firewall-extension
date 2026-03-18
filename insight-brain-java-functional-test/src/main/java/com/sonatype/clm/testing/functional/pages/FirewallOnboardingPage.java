/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.elements.FirewallRepositoryList;
import com.sonatype.clm.testing.functional.elements.NxCheckbox;
import com.sonatype.clm.testing.functional.elements.UnsavedModal;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;

import static com.codeborne.selenide.Selenide.$$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class FirewallOnboardingPage
    extends BasicElement<FirewallOnboardingPage>
{
  public static final String ROOT = "#firewall-onboarding-page";

  public FirewallOnboardingPage() {
    super(ROOT);
  }

  public static String url() {
    return BaseUrl.resolvePageUrl("/firewallOnboarding");
  }

  public SelenideElement helpButton() {
    return child("#help-button");
  }

  public Button cancelButton() {
    return new Button(childSelector("#cancel-button"));
  }

  public Button getStartedButton() {
    return new Button(childSelector("#get-started-button"));
  }

  public SelenideElement welcomeTitle() {
    return child(".welcome-screen-content .nx-h1");
  }

  public SelenideElement welcomeSubtitle() {
    return child(".welcome-screen-content .nx-h2");
  }

  public SelenideElement welcomeDescription() {
    return child(".welcome-screen-content .nx-h3");
  }

  public Button continueButton() {
    return new Button(childSelector("#continue-button"));
  }

  public Button previousButton() {
    return new Button(childSelector("#previous-button"));
  }

  public Button launchFirewallButton() {
    return new Button(childSelector("#launch-button"));
  }

  public SelenideElement selectedStepShouldBe(String step) {
    return child(".step.selected").shouldHave(Condition.text(step));
  }

  public SelenideElement steps() {
    return child("#onboarding-steps");
  }

  public SelenideElement actionsFooter() {
    return child("#actions-footer");
  }

  public UnsavedModal incompleteConfigurationModal() {
    return new UnsavedModal();
  }

  public List<FirewallRepositoryList> firewallRepositoryLists() {
    ElementsCollection repositoriesByFormatElements = children(".firewall-repository-list");
    List<FirewallRepositoryList> repositoriesFormatList = new ArrayList<>();

    for (int i = 0; i < repositoriesByFormatElements.size(); i++) {
      repositoriesFormatList
          .add(new FirewallRepositoryList("."
              + repositoriesByFormatElements.get(i).attr("class") + nthChild(i + 1)));
    }

    return repositoriesFormatList;
  }

  public ElementsCollection repositoriesList() {
    return $$(ROOT + " .firewall-repository-list");
  }

  public NxCheckbox supplyChainAttacksProtectionRuleCheckbox() {
    return new NxCheckbox(child("#firewall-rule-supply-chain-attacks"));
  }

  public NxCheckbox namespaceConfusionProtectionRuleCheckbox() {
    return new NxCheckbox(child("#firewall-rule-namespace-confusion"));
  }

  public static WebElementCondition protectionRulesSelectorTitle() {
    return Condition.text("Enable Repository Firewall features");
  }

  public static WebElementCondition proxyRepositoriesSelectorNoProtectionRulesTitle() {
    return Condition.text("You have not enabled recommended protection");
  }

  public static WebElementCondition proxyRepositoriesSelectorTitle() {
    return Condition.text("Enable protection from malicious components");
  }

  public Button closeButton() {
    return new Button("#firewall-welcome-modal-close-btn");
  }

  public SelenideElement maliciousComponentsDocumentationLink() {
    return child("#malicious-components-doc-link");
  }

  public SelenideElement namespaceAttacksDocumentationLink() {
    return child("#namespace-attacks-doc-link");
  }
}
