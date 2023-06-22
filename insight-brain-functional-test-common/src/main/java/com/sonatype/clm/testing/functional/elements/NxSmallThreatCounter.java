/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class NxSmallThreatCounter
    extends BasicElement<NxSmallThreatCounter>
{
  public NxSmallThreatCounter(String selector) {
    super(selector);
  }

  public NxSmallThreatCounterSeverity critical() {
    return new NxSmallThreatCounterSeverity(childSelector(".nx-small-threat-counter--critical"));
  }

  public NxSmallThreatCounterSeverity severe() {
    return new NxSmallThreatCounterSeverity(childSelector(".nx-small-threat-counter--severe"));
  }

  public NxSmallThreatCounterSeverity moderate() {
    return new NxSmallThreatCounterSeverity(childSelector(".nx-small-threat-counter--moderate"));
  }

  public NxSmallThreatCounterSeverity low() {
    return new NxSmallThreatCounterSeverity(childSelector(".nx-small-threat-counter--low"));
  }

  public NxSmallThreatCounterSeverity none() {
    return new NxSmallThreatCounterSeverity(childSelector(".nx-small-threat-counter--none"));
  }

  public ElementsCollection all() {
    return children(".nx-small-threat-counter");
  }

  public static class NxSmallThreatCounterSeverity
      extends BasicElement<NxSmallThreatCounterSeverity>
  {
    public NxSmallThreatCounterSeverity(String selector) {
      super(selector);
    }

    public SelenideElement category() {
      return child(".nx-small-threat-counter__category");
    }

    public SelenideElement count() {
      return child(".nx-small-threat-counter__count");
    }
  }
}
