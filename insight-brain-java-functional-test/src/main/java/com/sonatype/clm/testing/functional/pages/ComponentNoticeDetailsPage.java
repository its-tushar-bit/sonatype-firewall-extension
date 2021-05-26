/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Owner;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public final class ComponentNoticeDetailsPage
{
  private ComponentNoticeDetailsPage() {}

  public static String urlToApplicationScope(String publicAppId, String componentHash, int noticeIndex) {
    return BaseUrl.resolvePageUrl(String.format("/legal/application/%s/component/%s/notices/%d",
        publicAppId, componentHash, noticeIndex));
  }

  public static String url(Owner owner, String componentHash, int noticeIndex) {
    return BaseUrl.resolvePageUrl(
        String.format("/legal/%s/%s/component/%s/notices/%d",
            owner.getType().toString(), owner.getPublicId(), componentHash, noticeIndex));
  }

  public static NoticeOverview noticeOverview() {
    return new NoticeOverview();
  }

  public static NoticeList noticeList() {
    return new NoticeList();
  }

  public static NoticeHeader noticeHeader() {
    return new NoticeHeader();
  }

  public static NoticeFileEditor noticeFileEditor() {
    return new NoticeFileEditor();
  }

  public static class NoticeOverview
      extends BasicElement<NoticeOverview>
  {
    private static final String NOTICE_DETAILS_TILE = "#notice-details-tile";

    public NoticeOverview() {
      super(NOTICE_DETAILS_TILE);
    }

    private SelenideElement dataAt(final int index) {
      return $(String.format("dl .notice-overview-item:nth-child(%d) ", index));
    }

    public SelenideElement getAttributionReportStatus() {
      return dataAt(1);
    }

    public SelenideElement getScope() {
      return dataAt(2);
    }

    public SelenideElement getSource() {
      return dataAt(3);
    }

    public SelenideElement getNoticeText() {
      return $("dl .notice-overview-text .nx-scrollable");
    }
  }

  public static class NoticeList
      extends BasicElement<NoticeList>
  {
    private static final String NOTICE_LIST_SELECTOR = "notice-details-list";

    public NoticeList() {
      super(NOTICE_LIST_SELECTOR);
    }

    public SelenideElement itemAt(final int index) {
      return $(String.format("ul li:nth-child(%d)", index));
    }

    public SelenideElement attributionInclusion(final int index) {
      return itemAt(index).$("div.nx-list__subtext");
    }
  }

  public static class NoticeHeader
      extends BasicElement<NoticeHeader>
  {
    private static final String NOTICE_HEADER_SELECTOR = "notice-details-header";

    public NoticeHeader() {
      super(NOTICE_HEADER_SELECTOR);
    }

    public static SelenideElement noticeEditButton() {
      return $("notice-details-header button.nx-btn--tertiary");
    }
  }

  public static class NoticeFileEditor
      extends BasicElement<ComponentNoticeDetailsPage.NoticeFileEditor>
  {
    private static final String NOTICE_EDITOR_MODAL_SELECTOR = "notice-details-header div.nx-modal-backdrop";

    public NoticeFileEditor() {
      super(NOTICE_EDITOR_MODAL_SELECTOR);
    }

    public SelenideElement noticeText(final int index) {
      return $(String.format("td:nth-child(%d) textarea", index));
    }

    public SelenideElement saveButton() {
      return $("button.nx-form__submit-btn");
    }
  }
}
