/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional;

import java.io.File;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.plexus.util.FileUtils;
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

public class ImportPolicyTest
    extends AbstractFunctionalTest
{
  private String appId = getClass().getSimpleName();

  private WebElement importButton;
  private ImportPolicyDialog dialog;

  @Before
  public void setup() {
    createApplication(appId, appId);
    driver.get(getPolicyUrl());
    PageFactory.initElements(driver, Login.class).doLogin("admin", "admin123");

    By importBy = By.cssSelector("a[title='Import Policies']");
    wait(10, ExpectedConditions.presenceOfElementLocated(importBy));
    importButton = driver.findElement(importBy);

    importButton.click();
    dialog = new ImportPolicyDialog(driver);
  }

  @After
  public void teardown() {
    driver.findElement(By.cssSelector("a[ng-controller='LogoutController']")).click();
  }

  @Test
  public void testInitialButtonState() {
    Assert.assertFalse(dialog.getImportBtn().isEnabled());
    Assert.assertTrue(dialog.getCancelBtn().isEnabled());
  }

  // Tests a local issue with a file upload
  @Test
  public void testDisappearingFile() throws Exception {
    File tempFile = File.createTempFile("ImportPolicyTest", "testDisappearingFile");
    FileUtils.copyFile(getValidImportFile(), tempFile);
    tempFile.deleteOnExit();
    dialog.getInput().sendKeys(tempFile.getAbsolutePath());

    dialog.waitForImportButtonToBeEnabled();

    // We remove the file to simulate an error which the browser might discover
    Assert.assertTrue("Deleted temp file", tempFile.delete());
    dialog.getImportBtn().click();

    wait(10, ExpectedConditions.visibilityOf(dialog.getErrorAlert()));
    // Error message at this point is likely browser specific
  }

  // Tests where the file uploads but is crap
  @Test
  public void testBrokenFile() throws Exception {
    dialog.submitFile(getBadImportFile().getAbsolutePath());
    wait(10, ExpectedConditions.visibilityOf(dialog.getErrorAlert()));
    Assert
        .assertEquals(
            "The file you selected failed to upload correctly, are you certain it is a properly formatted policy import json file?",
            dialog.getErrorAlert().getText());
  }

  // Tests where the file uploads successfully
  @Test
  public void testSuccess() throws Exception {
    dialog.submitFile(getValidImportFile().getAbsolutePath());
    dialog.waitForInvisibility();

    wait(10, ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".policy-top")));

    List<WebElement> elements = driver.findElements(By.cssSelector(".policy-top"));

    Assert.assertEquals(4, elements.size());
    Set<String> names = new HashSet<String>();
    for (WebElement element : elements) {
      names.add(element.getText().trim());
    }
    Assert.assertTrue(names.contains("Security-High"));
    Assert.assertTrue(names.contains("Security-Medium"));
    Assert.assertTrue(names.contains("License-Copyleft"));
    Assert.assertTrue(names.contains("Architecture-Quality"));
  }

  // Tests user cancelling out of the dialog
  @Test
  public void testCancel() {
    dialog.cancelBtn.click();
    // This call will timeout if the dialog does not disappear
    dialog.waitForInvisibility();
  }

  private String getPolicyUrl() {
    return getBaseUrl() + "assets/index.html#/management/application/" + appId + "/policies";
  }

  private File getValidImportFile() throws Exception {
    URI url = getClass().getResource("/ImportPolicyTest/Sonatype-Sample-Policy-1.6.json").toURI();
    return new File(url).getAbsoluteFile();
  }
  
  private File getBadImportFile() throws Exception {
    URI url = getClass().getResource("/ImportPolicyTest/invalid-policy-import-file.txt").toURI();
    return new File(url).getAbsoluteFile();
  }

  public static class ImportPolicyDialog
  {
    @FindBy(css = ".btn-primary")
    private WebElement importBtn;

    @FindBy(css = "button[ng-click='$dismiss()']")
    private WebElement cancelBtn;

    @FindBy(css = "input[type=file]")
    private WebElement input;

    @FindBy(css = ".alert-error")
    private WebElement errorAlert;

    public ImportPolicyDialog(WebDriver driver) {
      WebElement context = driver.findElement(By.id("import-policy-dialog"));
      AbstractFunctionalTest.wait(10, ExpectedConditions.visibilityOf(context));
      PageFactory.initElements(new DefaultElementLocatorFactory(context), this);
      AbstractFunctionalTest.wait(10, ExpectedConditions.visibilityOf(input));
    }

    public WebElement getImportBtn() {
      return importBtn;
    }

    public WebElement getCancelBtn() {
      return cancelBtn;
    }

    public WebElement getInput() {
      return input;
    }

    public WebElement getErrorAlert() {
      return errorAlert;
    }

    /**
     * This call will timeout if the dialog does not disappear.
     */
    public void waitForInvisibility() {
      AbstractFunctionalTest.wait(10,
          ExpectedConditions.invisibilityOfElementLocated(By.id("import-policy-dialog")));
    }

    public void waitForImportButtonToBeEnabled() {
      AbstractFunctionalTest.wait(10, new ExpectedCondition<Boolean>()
      {

        @Override
        public Boolean apply(WebDriver driver) {
          return getImportBtn().isEnabled();
        }

        @Override
        public String toString() {
          return "Import button to be enabled";
        }
      });

    }

    public void submitFile(String filePath) {
      getInput().sendKeys(filePath);

      waitForImportButtonToBeEnabled();
      getImportBtn().click();
    }
  }
}