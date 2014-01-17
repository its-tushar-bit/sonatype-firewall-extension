/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional;

import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.pagefactory.DefaultElementLocatorFactory;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class ContactUserTest
    extends AbstractFunctionalTest
{
  private String appId = getClass().getSimpleName();

  private ContactUserDialog dialog;

  private WebElement contactUserField;

  private String getPolicyUrl() {
    return getBaseUrl() + "assets/index.html#/management/application/" + appId + "/policies";
  }

  @Before
  public void setup() {
    createApplication(appId, appId);
    driver.get(getPolicyUrl());
    PageFactory.initElements(driver, Login.class).doLogin("admin", "admin123");

    By field = By.id("contact-field");
    wait(10, ExpectedConditions.presenceOfElementLocated(field));
    contactUserField = driver.findElement(field);
    contactUserField.click();
    dialog = new ContactUserDialog(driver);
  }

  @After
  public void teardown() {
    driver.findElement(By.cssSelector(".dashboard-user a.btn")).click();
    driver.findElement(By.cssSelector("a[ng-click='logout()']")).click();
  }

  @Test
  public void testContactUser() {
    Assert.assertEquals(0, dialog.getQueryResultSize());
    dialog.setUserQuery("admin");
    dialog.waitOnQueryResultSize(1);
    dialog.getQueryResults().get(0).click();
    Assert.assertEquals("Admin BuiltIn", contactUserField.getText());
  }

  public static class ContactUserDialog
  {
    @FindBy(css = ".btn-danger")
    private WebElement removeBtn;

    @FindBy(css = "button[ng-click='$dismiss()']")
    private WebElement cancelBtn;

    @FindBy(css = "input[type=text]")
    private WebElement input;

    @FindBy(css = ".alert-error")
    private WebElement errorAlert;

    @FindBy(css = ".large-select-list-item")
    private List<WebElement> queryResults;

    public ContactUserDialog(WebDriver driver) {
      WebElement context = driver.findElement(By.id("contact-modal-dialog"));
      AbstractFunctionalTest.wait(10, ExpectedConditions.visibilityOf(context));

      PageFactory.initElements(new DefaultElementLocatorFactory(context), this);
      AbstractFunctionalTest.wait(10, ExpectedConditions.visibilityOf(input));
    }

    public void setUserQuery(String query) {
      input.sendKeys(query);
    }

    public int getQueryResultSize() {
      return queryResults.size();
    }

    public List<WebElement> getQueryResults() {
      return queryResults;
    }

    public void waitOnQueryResultSize(final int size) {
      AbstractFunctionalTest.wait(10, new ExpectedCondition<Boolean>()
      {
        @Override
        public Boolean apply(WebDriver driver) {
          return getQueryResultSize() == size;
        }

        @Override
        public String toString() {
          return "Query Image to Disappear";
        }
      });
    }
  }
}
