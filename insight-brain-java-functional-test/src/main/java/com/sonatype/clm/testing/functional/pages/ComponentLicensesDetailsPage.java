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
import com.sonatype.insight.brain.model.Owner;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public final class ComponentLicensesDetailsPage
{
  private ComponentLicensesDetailsPage() {
  }

  public static String urlToApplicationScopeByHash(String publicAppId, String componentHash, int licenseIndex) {
    return BaseUrl.resolvePageUrl(String.format("/legal/application/%s/component/%s/licenses/%d",
        publicAppId, componentHash, licenseIndex));
  }

  public static String urlToApplicationScopeByComponentIdentifier(
      String publicAppId,
      ComponentIdentifier componentIdentifier,
      int licenseIndex) throws UnsupportedEncodingException
  {
    String componentIdentifierString =
        URLEncoder.encode(ComponentIdentifierAdapter.toJson(componentIdentifier), StandardCharsets.UTF_8.name());
    return BaseUrl.resolvePageUrl(String.format("/legal/application/%s/componentIdentifier/%s/licenses/%d", publicAppId,
        componentIdentifierString, licenseIndex));
  }

  public static String url(Owner owner, String componentHash, int licenseIndex) {
    return BaseUrl.resolvePageUrl(
        String.format("/legal/%s/%s/component/%s/licenses/%d",
            owner.getType().toString(), owner.getPublicId(), componentHash, licenseIndex));
  }

  public static ComponentLicenseOverview componentLicenseOverview() {
    return new ComponentLicenseOverview();
  }

  public static LicenseList licenseList() {
    return new LicenseList();
  }

  public static LicenseObligations licenseObligations() {
    return new LicenseObligations();
  }

  public static class ComponentLicenseOverview
      extends BasicElement<ComponentLicenseOverview>
  {
    private static final String COMPONENT_OVERVIEW_TILE = "#component-license-overview-tile";

    public ComponentLicenseOverview() {
      super(COMPONENT_OVERVIEW_TILE);
    }

    private SelenideElement dataAt(final int index) {
      return $(String.format("dl .copyright-overview-item:nth-child(%d) .nx-read-only__data", index));
    }

    public SelenideElement getDeclaredLicense() {
      return $("#component-license-overview__declared-licenses");
    }

    public SelenideElement getObservedLicense() {
      return $("#component-license-overview__observed-licenses");
    }

    public SelenideElement getEffectiveLicense() {
      return $("#component-license-overview__effective-licenses");
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

  public static class LicenseList
      extends BasicElement<LicenseList>
  {
    private static final String LICENSE_LIST_SELECTOR = "#license-list";

    public LicenseList() {
      super(LICENSE_LIST_SELECTOR);
    }

    private SelenideElement itemAt(final int index) {
      return $(String.format("ul li:nth-child(%d)", index));
    }

    public SelenideElement licenseItem(final int index) {
      return itemAt(index).$("span.nx-list__text");
    }
  }

  public static class LicenseObligations
      extends BasicElement<LicenseObligations>
  {
    private static final String OBLIGATIONS_SELECTOR = "#license-full-details-tile";

    public LicenseObligations() {
      super(OBLIGATIONS_SELECTOR);
    }

    public SelenideElement obligationAt(final int index) {
      return $(String.format("dl div:nth-child(%d) q", index));
    }

    public ElementsCollection highlightedObligations() {
      return $(".component-license-details-license-preformatted").findAll("mark");
    }
  }
}
