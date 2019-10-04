/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import java.util.concurrent.TimeUnit;

import org.openqa.selenium.JavascriptExecutor;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.Wait;

public class KeycloakLoginPage
{
  public static void login(String username, String password) {
    // Keycloak's login page takes some time to load, especially the first time it is being visited by a fresh browser.
    // Default timeout, although rarely, causes flaky tests with timeouts due to document not being ready within limits.
    Wait().withTimeout(10, TimeUnit.SECONDS)
        .until(wd -> ((JavascriptExecutor) wd).executeScript("return document.readyState").equals("complete"));

    $("#username").sendKeys(username);
    $("#password").sendKeys(password);
    $("#kc-login").click();
  }
}
