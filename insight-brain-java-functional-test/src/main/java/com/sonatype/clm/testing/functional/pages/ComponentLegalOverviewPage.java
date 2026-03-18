/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.model.Owner;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class ComponentLegalOverviewPage
{
  private static final String ROOT = "#component-legal-overview-details";

  private ComponentLegalOverviewPage() {
  }

  public static String urlToApplicationScope(String publicAppId, String componentHash) {
    return BaseUrl.resolvePageUrl(String.format("/legal/application/%s/component/%s", publicAppId, componentHash));
  }

  public static String urlByComponentIdentifier(
      ComponentIdentifier componentIdentifier,
      String repositoryId) throws UnsupportedEncodingException
  {
    String componentIdentifierString =
        URLEncoder.encode(ComponentIdentifierAdapter.toJson(componentIdentifier), StandardCharsets.UTF_8.name());
    return BaseUrl.resolvePageUrl(String.format("/legal/component/componentIdentifier/%s/repository/%s",
        componentIdentifierString, repositoryId));
  }

  public static String url(Owner owner, String componentHash) {
    return BaseUrl.resolvePageUrl(
        String.format("/legal/%s/%s/component/%s", owner.getType().toString(), owner.getPublicId(), componentHash));
  }

  public static CopyrightStatements copyrightStatements() {
    return new CopyrightStatements();
  }

  public static SelenideElement editCopyrightButton() {
    return $("#edit-copyrights");
  }

  public static class CopyrightStatements
      extends BasicElement<CopyrightStatements>
  {
    private static final String COPYRIGHT_STATEMENT_SECTION = "#copyright-statements-tile";

    CopyrightStatements() {
      super(ROOT, COPYRIGHT_STATEMENT_SECTION);
    }

    public CopyrightStatementElement at(int index) {
      return new CopyrightStatementElement(childSelector(".nx-list li", nthChild(index + 1)));
    }

    public ElementsCollection all() {
      return children(".nx-list li");
    }
  }

  public static class CopyrightStatementElement
      extends BasicElement<CopyrightStatementElement>
  {
    CopyrightStatementElement(String selector) {
      super(selector);
    }

    public String value() {
      return getElement().innerText();
    }
  }

  public static OriginalSources originalSources() {
    return new OriginalSources();
  }

  public static SelenideElement editOriginalSourcesButton() {
    return $("#edit-original-sources");
  }

  public static class OriginalSources
      extends BasicElement<OriginalSources>
  {
    private static final String ORIGINAL_SOURCES_SECTION = "#original-sources-tile";

    OriginalSources() {
      super(ROOT, ORIGINAL_SOURCES_SECTION);
    }

    public CopyrightStatementElement at(int index) {
      return new CopyrightStatementElement(childSelector(".nx-list li", nthChild(index + 1)));
    }

    public ElementsCollection all() {
      return children(".nx-list li");
    }
  }

  public static class OriginalSourceElement
      extends BasicElement<OriginalSourceElement>
  {
    OriginalSourceElement(String selector) {
      super(selector);
    }

    public String value() {
      return getElement().innerText();
    }
  }

  public static Notices notices() {
    return new Notices();
  }

  public static SelenideElement editNoticesButton() {
    return $("#edit-notices");
  }

  public static class Notices
      extends BasicElement<Notices>
  {
    private static final String NOTICE_SECTION = "#notice-texts-tile";

    Notices() {
      super(ROOT, NOTICE_SECTION);
    }

    public Notice at(int index) {
      return new Notice("#notice-section-" + index);
    }

    public ElementsCollection all() {
      return children(".legal-file");
    }
  }

  public static class Notice
      extends BasicElement<Notice>
  {
    Notice(String selector) {
      super(selector);
    }

    public SelenideElement relPath() {
      return child(".legal-file-path");
    }

    public SelenideElement text() {
      return child("blockquote");
    }

    public SelenideElement viewMoreDetailsLink() {
      return child("#legal-file-section-view-more-details a");
    }
  }

  public static SelenideElement editLicensesButton() {
    return $("#edit-licenses");
  }

  public static LicenseFiles licenseFiles() {
    return new LicenseFiles();
  }

  public static SelenideElement editLicenseFilesButton() {
    return $("#edit-license-files");
  }

  public static class LicenseFiles
      extends BasicElement<LicenseFiles>
  {
    private static final String LICENSE_SECTION = "#license-texts-tile";

    LicenseFiles() {
      super(ROOT, LICENSE_SECTION);
    }

    public LicenseFile at(int index) {
      return new LicenseFile("#license-section-" + index);
    }

    public ElementsCollection all() {
      return children(".legal-file");
    }
  }

  public static class LicenseFile
      extends BasicElement<LicenseFile>
  {
    LicenseFile(String selector) {
      super(selector);
    }

    public SelenideElement relPath() {
      return child(".legal-file-path");
    }

    public SelenideElement text() {
      return child("blockquote");
    }

    public SelenideElement viewMoreDetailsLink() {
      return child("#legal-file-section-view-more-details a");
    }
  }

  public static Attribution attribution(String obligationName) {
    return new Attribution(obligationName == null
        ? "#additional-attribution-tile"
        : "#" + obligationName.toLowerCase(Locale.ROOT).replaceAll("\\s+", "-") + "-attribution-tile");
  }

  public static class Attribution
      extends BasicElement<Attribution>
  {
    Attribution(String selector) {
      super(selector);
    }

    public SelenideElement button() {
      return child("button");
    }

    public SelenideElement content() {
      return child(".nx-accordion__content div");
    }
  }

  public static Obligations obligations() {
    return new Obligations();
  }

  public static class Obligations
      extends BasicElement<Obligations>
  {
    private static final String LICENSE_OBLIGATIONS_TILE = "#license-obligations-tile";

    Obligations() {
      super(ROOT, LICENSE_OBLIGATIONS_TILE);
    }

    public ObligationElement at(int index) {
      return new ObligationElement(childSelector(".nx-tile-content--accordion-container details", nthChild(index + 1)));
    }

    public ElementsCollection all() {
      return children(".nx-accordion__summary-wrapper");
    }
  }

  public static class ObligationElement
      extends BasicElement<ObligationElement>
  {
    ObligationElement(String selector) {
      super(selector);
    }

    public String getObligationName() {
      return child("h3.nx-accordion__header-title").innerText();
    }

    public String getObligationStatus() {
      return child("button.nx-segmented-btn__main-btn span").innerText();
    }
  }

  public static class AttributionSummaryTile
      extends BasicElement<AttributionSummaryTile>
  {
    public AttributionSummaryTile() {
      super(ROOT, "#attribution-summary-tile");
    }

    public ElementsCollection getAllAccordions() {
      return children(".nx-accordion");
    }

    public SelenideElement getAccordionByIndex(int index) {
      return child(".nx-accordion:nth-child(" + (index + 1) + ")");
    }

    public SelenideElement openModal() {
      return child(".nx-modal-backdrop");
    }
  }

  public static SelenideElement resolveAllObligationsButton() {
    return $("#mark-all-obligations-resolved");
  }

  public static SelenideElement backLink() {
    return $(".nx-back-button .nx-text-link");
  }
}
