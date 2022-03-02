/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

public class ApplicationReportFilter
    extends BasicElement<ApplicationReportFilter>
{
  public ApplicationReportFilter() {
    super("#iq-component-filter-popover");
  }

  public SelenideElement closeButton() {
    return child(".iq-popover-header__close-btn");
  }

  public ProprietaryFilter proprietaryFilter() {
    return new ProprietaryFilter(childSelector("#proprietary-filter"));
  }

  public InnerSourceFilter innerSourceFilter() {
    return new InnerSourceFilter(childSelector("#inner-source-filter"));
  }

  public MatchStateFilter matchStateFilter() {
    return new MatchStateFilter(childSelector("#match-state-filter"));
  }

  public ViolationStateFilter violationStateFilter() {
    return new ViolationStateFilter(childSelector("#violation-state-filter"));
  }

  public DependencyTypeFilter dependencyTypeFilter() {
    return new DependencyTypeFilter(childSelector("#dependency-type-filter"));
  }

  public PolicyTypeFilter policyTypeFilter() {
    return new PolicyTypeFilter(childSelector("#policy-type-filter"));
  }

  public static class ProprietaryFilter
      extends NxTreeViewMultiSelect
  {
    public ProprietaryFilter(String selector) {
      super(selector);
    }

    public NxCheckbox nonProprietary() {
      return super.checkboxItem(2);
    }

    public NxCheckbox proprietary() {
      return super.checkboxItem(3);
    }
  }

  public static class InnerSourceFilter
      extends NxTreeViewMultiSelect
  {
    public InnerSourceFilter(String selector) {
      super(selector);
    }

    public NxCheckbox nonInnerSource() {
      return super.checkboxItem(2);
    }

    public NxCheckbox innerSource() {
      return super.checkboxItem(3);
    }
  }

  public static class MatchStateFilter
      extends NxTreeViewMultiSelect
  {
    public MatchStateFilter(String selector) {
      super(selector);
    }

    public NxCheckbox exact() {
      return super.checkboxItem(2);
    }

    public NxCheckbox similar() {
      return super.checkboxItem(3);
    }

    public NxCheckbox unknown() {
      return super.checkboxItem(4);
    }
  }

  public static class ViolationStateFilter
      extends NxTreeViewMultiSelect
  {
    public ViolationStateFilter(String selector) {
      super(selector);
    }

    public NxCheckbox notViolating() {
      return super.checkboxItem(2);
    }

    public NxCheckbox open() {
      return super.checkboxItem(3);
    }

    public NxCheckbox waived() {
      return super.checkboxItem(4);
    }

    public NxCheckbox grandfathered() {
      return super.checkboxItem(5);
    }
  }

  public static class DependencyTypeFilter
      extends NxTreeViewMultiSelect
  {
    public DependencyTypeFilter(final String selector) {
      super(selector);
    }

    public NxCheckbox direct() {
      return super.checkboxItem(2);
    }

    public NxCheckbox transitive() {
      return super.checkboxItem(3);
    }

    public NxCheckbox unknown() {
      return super.checkboxItem(4);
    }
  }

  public static class PolicyTypeFilter
      extends NxTreeViewMultiSelect
  {
    public PolicyTypeFilter(String selector) {
      super(selector);
    }

    public NxCheckbox security() {
      return super.checkboxItem(2);
    }

    public NxCheckbox license() {
      return super.checkboxItem(3);
    }

    public NxCheckbox quality() {
      return super.checkboxItem(4);
    }

    public NxCheckbox other() {
      return super.checkboxItem(5);
    }
  }
}
