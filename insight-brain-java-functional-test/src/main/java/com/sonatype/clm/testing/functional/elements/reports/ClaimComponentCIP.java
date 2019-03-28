/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.reports;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Selenide.$$;

public class ClaimComponentCIP
    extends BasicElement<ClaimComponentCIP>
{
  private static final String ROOT_ID = "#claim-component-editor";

  public static Condition ERROR_CLASS = cssClass("error");

  public ClaimComponentCIP() {
    super(ROOT_ID);
  }

  public ElementsCollection allFormInputs() {
    return $$(ROOT_ID + " input," + ROOT_ID + "textarea");
  }

  public ControlGroup group() {
    return new ControlGroup("#cip-claim-group-id");
  }

  public ControlGroup extension() {
    return new ControlGroup("#cip-claim-extension");
  }

  public ControlGroup artifactId() {
    return new ControlGroup("#cip-claim-artifact-id");
  }

  public ControlGroup created() {
    return new ControlGroup("#cip-claim-created-date");
  }

  public ControlGroup version() {
    return new ControlGroup("#cip-claim-version");
  }

  public ControlGroup classifier() {
    return new ControlGroup("#cip-claim-classifier");
  }

  public ControlGroup comment() {
    return new ControlGroup("#cip-claim-comment");
  }

  public SelenideElement revokeBtn() {
    return child("#cip-claim-revoke");
  }

  public SelenideElement cancelBtn() {
    return child("#cip-claim-cancel");
  }

  public SelenideElement updateBtn() {
    return child("#cip-claim-update");
  }

  public SelenideElement claimBtn() {
    return child("#cip-claim-submit");
  }

  public SelenideElement validationErrors() {
    return child("#validation-error-messages");
  }

  public static class ConfirmRevokeClaimDialog
      extends BasicElement<ConfirmRevokeClaimDialog>
  {
    public ConfirmRevokeClaimDialog() {
      super("#confirm-revoke-claim-dialog");
    }

    public SelenideElement cancelButton() {
      return child(".btn-cancel");
    }

    public SelenideElement revokeClaimButton() {
      return child(".btn-primary");
    }
  }

  public static class ControlGroup
      extends BasicElement<ControlGroup>
  {
    public ControlGroup(String... selectors) {
      super(selectors);
    }

    public SelenideElement input() {
      return child("input, textarea");
    }
  }
}
