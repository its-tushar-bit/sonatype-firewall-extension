/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.pages.ComponentLegalOverviewPage.Notice;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class EditNoticesModal
    extends BasicElement<EditNoticesModal>
{
  public EditNoticesModal() {
    super("#edit-notices-attribution-modal");
  }

  public Notice noticeAt(int index) {
    return new Notice("#notice-row-" + index);
  }

  public ElementsCollection allNotices() {
    return children("tbody tr");
  }

  public Button addNoticeButton() {
    return new Button("#add-notice");
  }

  public SelenideElement scopeDropdown() {
    return $("#edit-notice-scope-selection");
  }

  public Button save() {
    return new Button(childSelector(".nx-btn--primary"));
  }

  public Button cancel() {
    return new Button(childSelector(".nx-btn--undefined"));
  }

  public static class Notice
      extends BasicElement<Notice>
  {
    Notice(String selector) {
      super(selector);
    }

    public SelenideElement textInput() {
      return child(".nx-text-input__input");
    }

    public SelenideElement statusCheckbox() {
      return child(".nx-toggle__input");
    }

    public SelenideElement statusToggle() {
      return child(".nx-toggle__content");
    }
  }
}
