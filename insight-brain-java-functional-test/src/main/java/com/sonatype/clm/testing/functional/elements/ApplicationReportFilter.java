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
    super("#application-report-sidebar");
  }

  public SelenideElement closeButton() {
    return child("#application-report-filter-close-btn");
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
      extends IqTreeViewMultiSelect
  {
    public ProprietaryFilter(String selector) {
      super(selector);
    }

    public IqCheckbox nonProprietary() {
      return super.checkboxItem(2);
    }

    public IqCheckbox proprietary() {
      return super.checkboxItem(3);
    }
  }

  public static class InnerSourceFilter
      extends IqTreeViewMultiSelect
  {
    public InnerSourceFilter(String selector) {
      super(selector);
    }

    public IqCheckbox nonInnerSource() {
      return super.checkboxItem(2);
    }

    public IqCheckbox innerSource() {
      return super.checkboxItem(3);
    }
  }

  public static class MatchStateFilter
      extends IqTreeViewMultiSelect
  {
    public MatchStateFilter(String selector) {
      super(selector);
    }

    public IqCheckbox exact() {
      return super.checkboxItem(2);
    }

    public IqCheckbox similar() {
      return super.checkboxItem(3);
    }

    public IqCheckbox unknown() {
      return super.checkboxItem(4);
    }
  }

  public static class ViolationStateFilter
      extends IqTreeViewMultiSelect
  {
    public ViolationStateFilter(String selector) {
      super(selector);
    }

    public IqCheckbox notViolating() {
      return super.checkboxItem(2);
    }

    public IqCheckbox open() {
      return super.checkboxItem(3);
    }

    public IqCheckbox waived() {
      return super.checkboxItem(4);
    }

    public IqCheckbox grandfathered() {
      return super.checkboxItem(5);
    }
  }

  public static class DependencyTypeFilter
      extends IqTreeViewMultiSelect
  {
    public DependencyTypeFilter(final String selector) {
      super(selector);
    }

    public IqCheckbox direct() {
      return super.checkboxItem(2);
    }

    public IqCheckbox transitive() {
      return super.checkboxItem(3);
    }

    public IqCheckbox unknown() {
      return super.checkboxItem(4);
    }
  }

  public static class PolicyTypeFilter
      extends IqTreeViewMultiSelect
  {
    public PolicyTypeFilter(String selector) {
      super(selector);
    }

    public IqCheckbox security() {
      return super.checkboxItem(2);
    }

    public IqCheckbox license() {
      return super.checkboxItem(3);
    }

    public IqCheckbox quality() {
      return super.checkboxItem(4);
    }

    public IqCheckbox other() {
      return super.checkboxItem(5);
    }
  }
}
