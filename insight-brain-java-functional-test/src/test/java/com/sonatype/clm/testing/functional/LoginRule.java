/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.sonatype.insight.brain.security.UserSessionResource;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.openqa.selenium.Cookie;

public class LoginRule
    implements TestRule
{

  protected void before(Login login) throws Throwable {
    HttpClient client = HttpClientBuilder.create().build();

    String baseUrl = Configuration.baseUrl;
    if (!baseUrl.endsWith("/")) {
      baseUrl += "/";
    }
    HttpPost post = new HttpPost(baseUrl + UserSessionResource.RESOURCE_PATH);
    post.setHeader("Authorization",
        "Basic " + Base64.encodeBase64String((login.username() + ":" + login.password()).getBytes()));

    HttpResponse response = client.execute(post);
    if (response.getStatusLine().getStatusCode() >= 300) {
      throw new IllegalStateException("Login to server failed with " + response.getStatusLine().toString());
    }
    for (Header cookie : response.getHeaders("Set-Cookie")) {
      String[] value = cookie.getValue().split(";")[0].split("=");
      WebDriverRunner.getWebDriver().manage().addCookie(new Cookie(value[0], value[1]));
    }
  }

  protected void after() {
    WebDriverRunner.getWebDriver().manage().deleteAllCookies();
  }

  @Override
  public Statement apply(final Statement base, final Description description) {
    final Login login = description.getAnnotation(Login.class);

    if (login == null) {
      return base;
    }

    if (!WebDriverRunner.url().startsWith(Configuration.baseUrl)) {
      Selenide.open(Configuration.baseUrl + "/about");
    }
    return new Statement()
    {
      @Override
      public void evaluate() throws Throwable {
        before(login);
        try {
          base.evaluate();
        }
        finally {
          after();
        }
      }
    };
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.METHOD })
  public @interface Login
  {
    String username() default "admin";

    String password() default "admin123";
  }
}
