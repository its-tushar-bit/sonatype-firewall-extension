/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class NxThreatCounter
    extends BasicElement<NxThreatCounter>
{
  public NxThreatCounter(String selector) {
    super(selector);
  }

  public NxThreatCounterSeverity critical() {
    return new NxThreatCounterSeverity(childSelector(".nx-threat-counter--critical"));
  }

  public NxThreatCounterSeverity severe() {
    return new NxThreatCounterSeverity(childSelector(".nx-threat-counter--severe"));
  }

  public NxThreatCounterSeverity moderate() {
    return new NxThreatCounterSeverity(childSelector(".nx-threat-counter--moderate"));
  }

  public NxThreatCounterSeverity low() {
    return new NxThreatCounterSeverity(childSelector(".nx-threat-counter--low"));
  }

  public NxThreatCounterSeverity none() {
    return new NxThreatCounterSeverity(childSelector(".nx-threat-counter--none"));
  }

  public ElementsCollection all() {
    return children(".nx-threat-counter");
  }

  public static class NxThreatCounterSeverity
      extends BasicElement<NxThreatCounterSeverity>
  {
    public NxThreatCounterSeverity(String selector) {
      super(selector);
    }

    public SelenideElement text() {
      return child(".nx-threat-counter__text");
    }

    public SelenideElement count() {
      return child(".nx-threat-counter__count");
    }
  }
}
