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

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public final class ComponentLicenseFileDetailsPage
{
  private ComponentLicenseFileDetailsPage() {
  }

  public static String urlToApplicationScopeByHash(String publicAppId, String componentHash, int licenseIndex) {
    return BaseUrl.resolvePageUrl(String.format("/legal/application/%s/component/%s/licenseFiles/%d",
        publicAppId, componentHash, licenseIndex));
  }

  public static String urlToApplicationScopeByComponentIdentifier(
      String publicAppId,
      ComponentIdentifier componentIdentifier,
      int licenseIndex) throws UnsupportedEncodingException
  {
    String componentIdentifierString =
        URLEncoder.encode(ComponentIdentifierAdapter.toJson(componentIdentifier), StandardCharsets.UTF_8.name());
    return BaseUrl.resolvePageUrl(String.format("/legal/application/%s/componentIdentifier/%s/licenseFiles/%d",
        publicAppId, componentIdentifierString, licenseIndex));
  }

  public static SelenideElement editButton() {
    return $("#edit-license-files");
  }

  public static LicenseFileEditor licenseFileEditor() {
    return new LicenseFileEditor();
  }

  public static LicenseFileOverview licenseFileOverview() {
    return new LicenseFileOverview();
  }

  public static LicenseFileList licenseFileList() {
    return new LicenseFileList();
  }

  public static class LicenseFileOverview
      extends BasicElement<LicenseFileOverview>
  {
    private static final String LICENSE_FILE_DETAILS_TILE = "#legal-file-details-tile";

    public LicenseFileOverview() {
      super(LICENSE_FILE_DETAILS_TILE);
    }

    private SelenideElement dataAt(final int index) {
      return $(String.format("dl .legal-file-overview-item:nth-child(%d) ", index));
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

    public SelenideElement getLicenseText() {
      return $("dl .legal-file-overview-text .nx-scrollable");
    }
  }

  public static class LicenseFileList
      extends BasicElement<LicenseFileList>
  {
    private static final String LICENSE_LIST_SELECTOR = "#license-files-details-list";

    public LicenseFileList() {
      super(LICENSE_LIST_SELECTOR);
    }

    public SelenideElement itemAt(final int index) {
      return $(String.format("ul li:nth-child(%d)", index));
    }

    public SelenideElement attributionInclusion(final int index) {
      return itemAt(index).$("div.nx-list__subtext");
    }
  }

  public static class LicenseFileEditor
      extends BasicElement<ComponentLicenseFileDetailsPage.LicenseFileEditor>
  {
    private static final String LICENSE_EDITOR_MODAL_SELECTOR = "#license-details-header div.nx-modal-backdrop";

    public LicenseFileEditor() {
      super(LICENSE_EDITOR_MODAL_SELECTOR);
    }

    public SelenideElement licenseText(final int index) {
      return $(String.format("td:nth-child(%d) textarea", index));
    }

    public SelenideElement saveButton() {
      return $("button.nx-form__submit-btn");
    }
  }
}
