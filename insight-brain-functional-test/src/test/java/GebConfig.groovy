/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*
 This is the Geb configuration file.
 
 See: http://www.gebish.org/manual/current/configuration.html
*/

import geb.driver.SauceLabsDriverFactory
import org.openqa.selenium.Dimension
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.remote.RemoteWebDriver

reportsDir = "target/test-reports/geb"
baseUrl = System.getProperty('geb.build.baseUrl', 'http://localhost:9070/')
//enable waitFor behaviour for all 'at' checks
atCheckWaiting = true

// Consult these documents for how to configure for SauceLabs
// http://www.gebish.org/manual/current/sauce-labs.html#saucelabs_integration
// https://saucelabs.com/docs/platforms/webdriver
def sauceBrowser = System.getProperty("geb.sauce.browser")
if (sauceBrowser) {
  driver = {
    def username = System.getProperty("GEB_SAUCE_LABS_USER")
    assert username
    def accessKey = System.getProperty("GEB_SAUCE_LABS_ACCESS_PASSWORD")
    assert accessKey
    new SauceLabsDriverFactory().create(sauceBrowser, username, accessKey)
  }
  //increase default timeouts to account for remote execution
  waiting{
    timeout = 10
    retryInterval = 0.5
  }
}
else {
  driver = { configure(new FirefoxDriver()) }
}

environments {

  // run as “mvn -Dgeb.env=chrome test”
  // See: http://code.google.com/p/selenium/wiki/ChromeDriver
  chrome {
    driver = { configure(new ChromeDriver()) }
  }

  // See: http://code.google.com/p/selenium/wiki/HtmlUnitDriver
}

def configure(final RemoteWebDriver driver) {
  driver.manage().window().setSize(new Dimension(1280, 1024))
  return driver
}