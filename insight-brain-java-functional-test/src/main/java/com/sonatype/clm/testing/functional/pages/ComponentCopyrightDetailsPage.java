/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public final class ComponentCopyrightDetailsPage
{
  private ComponentCopyrightDetailsPage() {
  }

  public static String urlToApplicationScopeByHash(String publicAppId, String componentHash, int copyrightIndex) {
    return BaseUrl.resolvePageUrl(String.format("/legal/application/%s/component/%s/copyrights/%d",
        publicAppId, componentHash, copyrightIndex));
  }

  public static String urlToApplicationScopeByComponentIdentifier(
      String publicAppId,
      ComponentIdentifier componentIdentifier,
      int copyrightIndex) throws UnsupportedEncodingException
  {
    String componentIdentifierString =
        URLEncoder.encode(ComponentIdentifierAdapter.toJson(componentIdentifier), StandardCharsets.UTF_8.name());
    return BaseUrl.resolvePageUrl(String.format("/legal/application/%s/componentIdentifier/%s/copyrights/%d",
        publicAppId, componentIdentifierString, copyrightIndex));
  }

  public static CopyrightOverview copyrightOverview() {
    return new CopyrightOverview();
  }

  public static CopyrightFilePaths copyrightFilePaths() {
    return new CopyrightFilePaths();
  }

  public static CopyrightList copyrightList() {
    return new CopyrightList();
  }

  public static CopyrightEditor copyrightEditor() {
    return new CopyrightEditor();
  }

  public static SelenideElement copyrightEditButton() {
    return $("#copyright-details-header button.nx-btn--tertiary");
  }

  public static class CopyrightOverview
      extends BasicElement<CopyrightOverview>
  {
    private static final String COPYRIGHT_DETAILS_TILE = "#copyright-details-header";

    public CopyrightOverview() {
      super(COPYRIGHT_DETAILS_TILE);
    }

    private SelenideElement dataAt(final int index) {
      return $(String.format("dl .copyright-overview-item:nth-child(%d) .nx-read-only__data", index));
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

    public SelenideElement getCopyrightText() {
      return $("dl .copyright-overview-text .nx-read-only__data");
    }
  }

  public static class CopyrightFilePaths
      extends BasicElement<CopyrightFilePaths>
  {
    private static final String COPYRIGHT_FILE_PATHS =
        "#copyright-details-contents section#copyright-file-paths";

    public CopyrightFilePaths() {
      super(COPYRIGHT_FILE_PATHS);
    }

    public SelenideElement pathAt(final int index) {
      // need to add 1 as parent contains additional <p> element
      return $(String.format(".file-path-item:nth-child(%d)", index + 1));
    }

    public boolean isOpen(final int index) {
      final SelenideElement element = pathAt(index);
      return element.attr("class").toLowerCase().contains("nx-collapsible-items--expanded");
    }

    public SelenideElement getFilePath(final int index) {
      return pathAt(index).$(".nx-collapsible-items__text");
    }

    public SelenideElement getCopyrightContextText(final int index) {
      return pathAt(index).$(".nx-collapsible-items__children");
    }
  }

  public static class CopyrightList
      extends BasicElement<CopyrightList>
  {
    private static final String COPYRIGHT_LIST_SELECTOR = "#copyright-list";

    public CopyrightList() {
      super(COPYRIGHT_LIST_SELECTOR);
    }

    private SelenideElement itemAt(final int index) {
      return $(String.format("ul li:nth-child(%d)", index));
    }

    public SelenideElement text(final int index) {
      return itemAt(index).$(".nx-list__text");
    }

    public ElementsCollection texts() {
      return $("ul").findAll(".nx-list__text");
    }

    public SelenideElement attributionInclusion(final int index) {
      return itemAt(index).$("div.nx-list__subtext p:nth-child(1)");
    }

    public SelenideElement getItemFileCount(final int index) {
      return itemAt(index).$("div.nx-list__subtext p:nth-child(2)");
    }
  }

  public static class CopyrightEditor
      extends BasicElement<CopyrightEditor>
  {
    private static final String COPYRIGHT_EDITOR_MODAL_SELECTOR = "#copyright-details-header div.nx-modal-backdrop";

    public CopyrightEditor() {
      super(COPYRIGHT_EDITOR_MODAL_SELECTOR);
    }

    public SelenideElement copyrightText(final int index) {
      return $(String.format("td:nth-child(%d) input[type=text]", index));
    }

    public SelenideElement saveButton() {
      return $("button.nx-form__submit-btn");
    }
  }
}
