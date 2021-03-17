/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Owner;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class ComponentLegalOverviewPage
{
  private static final String ROOT = "#component-legal-overview-details";

  private ComponentLegalOverviewPage() {}

  public static String urlToApplicationScope(String publicAppId, String componentHash) {
    return BaseUrl.resolvePageUrl(String.format("/legal/application/%s/component/%s", publicAppId, componentHash));
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
      return children(".nx-tile-subsection.legal-file");
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
  }

  public static Licenses licenses() {
    return new Licenses();
  }

  public static SelenideElement editLicensesButton() {
    return $("#edit-licenses");
  }

  public static class Licenses
      extends BasicElement<Licenses>
  {
    private static final String LICENSE_SECTION = "#license-texts-tile";

    Licenses() {
      super(ROOT, LICENSE_SECTION);
    }

    public License at(int index) {
      return new License("#license-section-" + index);
    }

    public ElementsCollection all() {
      return children(".nx-tile-subsection.legal-file");
    }
  }

  public static class License
      extends BasicElement<License>
  {
    License(String selector) {
      super(selector);
    }

    public SelenideElement relPath() {
      return child(".legal-file-path");
    }

    public SelenideElement text() {
      return child("blockquote");
    }
  }
}
