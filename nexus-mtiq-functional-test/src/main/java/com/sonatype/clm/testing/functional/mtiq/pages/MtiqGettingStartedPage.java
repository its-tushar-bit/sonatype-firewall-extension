/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.mtiq.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.pages.GettingStartedPage;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class MtiqGettingStartedPage
    extends GettingStartedPage
{
  public LearningTopicsSummaryTile learningTopicsSummary() {
    return new LearningTopicsSummaryTile();
  }

  public SystemSetupSummaryTile systemSetupSummary() {
    return new SystemSetupSummaryTile();
  }

  public static class LearningTopicsSummaryTile
      extends BasicElement<LearningTopicsSummaryTile>
  {
    private static final String ROOT = "#learning-topics";

    LearningTopicsSummaryTile() {
      super(ROOT);
    }

    public ElementsCollection sectionTopics() {
      return children(".nx-read-only__item > .nx-grid-header__title");
    }
  }

  public static class SystemSetupSummaryTile
      extends BasicElement<SystemSetupSummaryTile>
  {
    private static final String ROOT = "#system-setup";

    SystemSetupSummaryTile() {
      super(ROOT);
    }

    public ElementsCollection setupSections() {
      return children(".nx-read-only__item > .nx-read-only__label");
    }

    public SelenideElement addingUsersTopics() {
      return child("#system-setup-adding-users .nx-read-only__item > .nx-grid-header__title");
    }
  }
}
