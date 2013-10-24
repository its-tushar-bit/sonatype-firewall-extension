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
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.firefox.FirefoxDriver

reportsDir = "target/test-reports/geb"
baseUrl = System.getProperty('geb.build.baseUrl', 'http://localhost:8070/')
//enable waitFor behaviour for all 'at' checks
atCheckWaiting = true

// http://www.gebish.org/manual/current/sauce-labs.html#saucelabs_integration
// https://saucelabs.com/docs/platforms/webdriver
def sauceBrowser = System.getProperty("geb.sauce.browser")
if (sauceBrowser) {
  driver = {
    def username = System.getenv("GEB_SAUCE_LABS_USER")
    assert username
    def accessKey = System.getenv("GEB_SAUCE_LABS_ACCESS_PASSWORD")
    assert accessKey
    new SauceLabsDriverFactory().create(sauceBrowser, username, accessKey)
  }
}
else {
  driver = { new FirefoxDriver() }
}

environments {

  // run as “mvn -Dgeb.env=chrome test”
  // See: http://code.google.com/p/selenium/wiki/ChromeDriver
  chrome {
    driver = { new ChromeDriver() }
  }

  // See: http://code.google.com/p/selenium/wiki/HtmlUnitDriver
}