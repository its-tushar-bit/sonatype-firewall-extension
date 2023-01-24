/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.NxFormSelect;
import com.sonatype.clm.testing.functional.elements.ProprietaryComponentMatcher;
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
  public static final String DUPLICATE_VALUE_MESSAGE = "Duplicate value name";

  public static final String BEGINNING_OR_ENDING_PERIOD_MESSAGE = "Value cannot begin or end with a period “.”";

  public static final String INVALID_PACKAGE_MESSAGE = "Invalid Java package name";

  public static String url(Owner owner) {
    return url(owner.getType(), owner.getPublicId());
  }

  public static String url(OwnerType ownerType, String ownerId) {
    return BaseUrl.resolvePageUrl("/management/edit/{ownerType}/{ownerId}/proprietary", ownerType, ownerId);
  }

  public static ElementsCollection localMatchers() {
    return $$(createSelector(".local-proprietary-component-matchers", ".nx-list__item"));
  }

  public static NxFormSelect typeDropdown() {
    return new NxFormSelect(".nx-form-select");
  }

  public static SelenideElement matcherValue() {
    return $(".nx-text-input__input");
  }

  public static SelenideElement addButton() {
    return $(".nx-btn.nx-btn--tertiary");
  }

  public static SelenideElement updateButton() {
    return $(".nx-form__submit-btn");
  }

  public static SelenideElement matcherInvalidMessage() {
    return $(".nx-text-input > .nx-field-validation-message");
  }

  public static ProprietaryComponentMatcher localMatcher(MatcherType type, String name) {
    return new ProprietaryComponentMatcher(".local-proprietary-component-matchers", type, name);
  }
}
