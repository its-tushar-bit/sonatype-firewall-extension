/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

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
}
