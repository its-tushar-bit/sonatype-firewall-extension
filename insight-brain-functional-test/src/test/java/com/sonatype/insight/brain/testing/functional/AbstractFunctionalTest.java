/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.TestInsightBrainService;

import com.google.common.base.Function;
import com.google.common.io.Resources;
import com.sun.jersey.core.util.Base64;
import com.yammer.dropwizard.testing.junit.DropwizardServiceRule;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public abstract class AbstractFunctionalTest
{
  static {
    System.setProperty("javax.net.ssl.trustStore", "src/test/resources/ssl/server-store");
  }

  private static DropwizardServiceRule<InsightConfig> serviceRule;

  /**
   * Creates a new service for each test class. It's crucial to create a fresh rule per class to avoid
   * https://github.com/dropwizard/dropwizard/issues/427.
   */
  @ClassRule
  public static DropwizardServiceRule<InsightConfig> initServiceRule() {
    serviceRule = new DropwizardServiceRule<InsightConfig>(TestInsightBrainService.class,
      Resources.getResource("config-test.yml").getPath());
    return serviceRule;
  }

  protected static WebDriver driver;

  private List<Application> appsToRemove;

  @BeforeClass
  public static void boot() {
    driver = new FirefoxDriver();
  }

  @AfterClass
  public static void shutdown() {
    driver.quit();
  }

  @After
  public final void after() {
    ApplicationDAO applicationDAO = new ApplicationDAO();
    if (appsToRemove != null) {
      for (Application app : appsToRemove) {
        applicationDAO.delete(app);
      }
    }
  }

  protected Application createApplication(String appId, String name) {
    ApplicationDAO applicationDAO = new ApplicationDAO();
    Application app = new Application(appId, name, null);
    applicationDAO.insert(app);
    if (appsToRemove == null) {
      appsToRemove = new ArrayList<Application>();
    }
    appsToRemove.add(app);
    return app;
  }

  protected static void wait(int time, Function<WebDriver, ?> isTrue) {
    new WebDriverWait(driver, time).until(isTrue);
  }

  protected String getBaseUrl() {
    return "http://localhost:" + serviceRule.getLocalPort() + "/";
  }

  protected InsightConfig getConfig() {
    return serviceRule.getConfiguration();
  }

  protected static void post(String url, String content, String user, String pass) throws Exception {
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
      AbstractFunctionalTest.wait(10, ExpectedConditions.visibilityOf(username));
      username.sendKeys(user);
      password.sendKeys(pass);
      submit.click();
    }
  }
}
