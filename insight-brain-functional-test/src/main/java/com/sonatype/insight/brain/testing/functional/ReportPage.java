/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional;

import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.pagefactory.DefaultElementLocatorFactory;

public class ReportPage
{
  @FindBy(tagName = "iframe")
  private WebElement reportIFrame;

  @FindBy(css = ".reportTitle")
  private WebElement reportTitle;

  private WebDriver driver;

  public ReportPage(WebDriver driver) {
    this.driver = driver;
  }

  public WebElement getReportFrame() {
    return reportIFrame;
  }

  /**
   * Must switch WebDriver to the report iframe prior to use
   */
  public Report getReport() {
    return PageFactory.initElements(driver, Report.class);
  }

  public String getTitle() {
    return reportTitle.getText();
  }

  /**
   * The CI Report
   */
  public static class Report
  {
    @FindBy(id = "summary")
    private WebElement summary;

    public ReportSummaryPage getSummary() {
      return new ReportSummaryPage(summary);
    }
  }

  /**
   * Represents the summary page of the report
   */
  public static class ReportSummaryPage
  {
    @FindBy(css = ".topBorder:nth-child(3) .span5 .value_lrg")
    private WebElement componentsIdentified;

    @FindBy(css = "#svHeader .value_lrg")
    private WebElement securityAlerts;

    public ReportSummaryPage(SearchContext parent) {
      PageFactory.initElements(new DefaultElementLocatorFactory(parent), this);
    }

    public int getComponentsIdentified() {
      return Integer.valueOf(componentsIdentified.getText());
    }

    public int getSecurityAlerts() {
      return Integer.valueOf(securityAlerts.getText());
    }
  }
}
