/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.ProprietaryComponentMatcher;
import com.sonatype.clm.testing.functional.elements.Dropdown;
import com.sonatype.clm.testing.functional.elements.ProprietaryComponentMatcher.MatcherType;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.createSelector;

public class ProprietaryConfigEditorPage
{
  public static final String ROOT_SELECTOR = "#proprietary-config-editor";

  public static final String DUPLICATE_PACKAGE_MESSAGE = "Package prefix already specified";

  public static final String WILDCARD_PACKAGE_MESSAGE = "Wildcards are not allowed/required for packages";

  public static final String INVALID_PACKAGE_MESSAGE = "Invalid package prefix: e.g. com.sonatype";

  public static final String DUPLICATE_REGEX_MATCHER_MESSAGE = "Regular Expression already specified";

  public static String url(Owner owner) {
    return url(owner.getType(), owner.getPublicId());
  }

  public static String url(OwnerType ownerType, String ownerId) {
    return BaseUrl.resolvePageUrl("/management/edit/{ownerType}/{ownerId}/proprietary", ownerType, ownerId);
  }

  public static ElementsCollection localMatchers() {
    return $$(createSelector(".local-proprietary-component-matchers", ".iq-list__item"));
  }

  public static Dropdown typeDropdown() {
    return new Dropdown("#matcher-type");
  }

  public static SelenideElement matcherValue() {
    return $("#matcher-value");
  }

  public static SelenideElement addButtton() {
    return $("#editor-matcher-add");
  }

  public static SelenideElement updateButton() {
    return $(createSelector(ROOT_SELECTOR, "button[type^=submit]"));
  }

  public static ProprietaryComponentMatcher localMatcher(MatcherType type, String name) {
    return new ProprietaryComponentMatcher(".local-proprietary-component-matchers", type, name);
  }
}
