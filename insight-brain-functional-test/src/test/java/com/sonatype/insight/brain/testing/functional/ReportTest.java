/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional;

import java.io.File;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateResource;
import com.sonatype.insight.brain.service.InsightBrainService;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.testing.functional.ReportPage.Report;
import com.sonatype.insight.brain.testing.functional.ReportPage.ReportSummaryPage;

import com.google.common.base.Function;
import com.google.common.io.Resources;
import com.sun.jersey.core.util.Base64;
import com.yammer.dropwizard.testing.JsonHelpers;
import com.yammer.dropwizard.testing.junit.DropwizardServiceRule;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.plexus.util.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class ReportTest  {
  @ClassRule
  public static TestRule startServiceRule = new DropwizardServiceRule<InsightConfig>(InsightBrainService.class,  Resources.getResource("config-test.yml").getPath());

  private static WebDriver driver; 

  private String appId = ReportTest.class.getSimpleName();

  private String scanId = "scan1234";
  
  
  @BeforeClass
  public static void boot() {
    driver = new FirefoxDriver();
  }

  @AfterClass
  public static void shutdown() {
    driver.close();
  }

  @Before
  public void setup() throws Exception {
    // Create Application
    ApplicationDAO applicationDAO = new ApplicationDAO();
    Application app = new Application(appId, "asdf", null);
    applicationDAO.insert(app);

    // copy scan
    File sonatypeWork = getConfig().getSonatypeWork();
    final File saasReportFile1 = new File(new File(new File(new File(sonatypeWork, "report"), app.getId()), scanId), "report.zip");
    FileUtils.copyURLToFile(getClass().getResource("/ReportTest/report.zip"), saasReportFile1);
    // Trigger evaluation
    post(getEvalURL(appId, scanId), JsonHelpers.asJson(new Stage(Stage.ID_BUILD)), "admin", "admin123");
  }

  @After
  public void teardown() {
    ApplicationDAO applicationDAO = new ApplicationDAO();
    applicationDAO.delete(applicationDAO.getByPublicId(appId));
  }

  @Test
  public void testReportLink() {
    driver.get(getUiLinksReportUrl(appId, scanId));
    PageFactory.initElements(driver, Login.class).doLogin("admin", "admin123");

    final ReportPage reportPage = PageFactory.initElements(driver, ReportPage.class);
    wait(10, ExpectedConditions.visibilityOf(reportPage.getReportFrame()));

    driver.switchTo().frame(reportPage.getReportFrame());
    wait(10, ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

    // Verify content of report
    ReportSummaryPage summary = PageFactory.initElements(driver, Report.class).getSummary();
    Assert.assertEquals(28, summary.getComponentsIdentified());
    Assert.assertEquals(36, summary.getSecurityAlerts());
  }

  private static void wait(int time, Function<WebDriver, ?> isTrue) {
    new WebDriverWait(driver, time).until(isTrue);
  }

  private static String getUrl() {
    return "http://localhost:" + getConfig().getHttpConfiguration().getPort() + "/";
  }

  @SuppressWarnings("unchecked")
  private static InsightConfig getConfig() {
    return ((DropwizardServiceRule<InsightConfig>) startServiceRule).getConfiguration();
  }

  private static String getEvalURL(String appId, String scanId) {
    return getUrl() + PolicyEvaluateResource.SERVICE_PATH.replace("{applicationPublicId}", appId) + "?scanId=" + scanId;
  }

  private static String getUiLinksReportUrl(String appId, String scanId) {
    return getUrl() + UserInterfaceLinksResource.SERVICE_PATH + "/"
        + UserInterfaceLinksResource.REPORT_PATH.replace("{applicationPublicId}", appId).replace("{scanId}", scanId);
  }

  private static void post(String url, String content, String user, String pass) throws Exception {
    HttpClient client = new DefaultHttpClient();
    HttpPost post = new HttpPost(url);
    post.setEntity(new StringEntity(content, ContentType.APPLICATION_JSON));
    post.setHeader("Authorization", "Basic " + Base64.encode(user + ":" + pass));
    HttpResponse response = client.execute(post);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  public static class Login
  {
    @FindBy(id = "login-username")
    private WebElement username;

    @FindBy(id = "login-password")
    private WebElement password;

    @FindBy(id = "login-action")
    private WebElement submit;

    public void doLogin(String user, String pass) {
      ReportTest.wait(10, ExpectedConditions.visibilityOf(username));
      username.sendKeys(user);
      password.sendKeys(pass);
      submit.click();
    }
  }
}
