/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.componentdetails;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.NxFormSelect;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class ManageLabelsContentTab
    extends BasicElement<ManageLabelsContentTab>
{
  public enum RepositoryComponentLabelsScopes
  {
    ROOT_ORGANIZATION,
    ALL_REPOSITORIES,
    REPOSITORY_MANAGER,
    REPOSITORY
  }

  public ManageLabelsContentTab(String selector) {
    super(selector);
  }

  public ElementsCollection applicableLabels() {
    return children(".nx-tag--unselected");
  }

  public ElementsCollection appliedLabels() {
    return children(".nx-tag--selected");
  }

  public SelenideElement applicableLabelText(int index) {
    return this.applicableLabels().get(index).find(".nx-tag__text");
  }

  public SelenideElement appliedLabelText(int index) {
    return this.appliedLabels().get(index).find(".nx-tag__text");
  }

  public AddLabelModal addLabelModal() {
    return new AddLabelModal("#iq-apply-label-modal");
  }

  public RemoveLabelModal removeLabelModal() {
    return new RemoveLabelModal(".nx-modal");
  }

  public static class AddLabelModal
      extends BasicElement<AddLabelModal>
  {
    public AddLabelModal(String selector) {
      super(selector);
    }

    public ElementsCollection labelsScopesList() {
      return children(".nx-radio-checkbox");
    }

    public NxFormSelect labelsScopesDropdown() {
      return new NxFormSelect("#iq-apply-label-scope");
    }

    public SelenideElement labelsScope(int index) {
      NxFormSelect dropdown = labelsScopesDropdown();
      return dropdown.listItem(index);
    }

    public SelenideElement submitButton() {
      return child(".nx-form__submit-btn");
    }
  }

  public static class RemoveLabelModal
      extends BasicElement<RemoveLabelModal>
  {
    public RemoveLabelModal(String selector) {
      super(selector);
    }

    public SelenideElement confirmRemoveButton() {
      return child(".nx-btn--primary");
    }
  }
}
