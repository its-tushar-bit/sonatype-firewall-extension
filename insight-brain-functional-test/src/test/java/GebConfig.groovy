/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*
 This is the Geb configuration file.
 
 See: http://www.gebish.org/manual/current/configuration.html
*/

import java.util.logging.Level

import com.sonatype.insight.brain.testing.functional.utils.PageTweakingWebDriver

import geb.driver.SauceLabsDriverFactory
import org.openqa.selenium.Dimension
import org.openqa.selenium.Platform
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.ie.InternetExplorerDriver
import org.openqa.selenium.phantomjs.PhantomJSDriver
import org.openqa.selenium.phantomjs.PhantomJSDriverService
import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.safari.SafariDriver

reportsDir = "target/test-reports/geb"
// Port is not known until runtime, needs to be set in BaseSpec.groovy
//baseUrl = System.getProperty('geb.build.baseUrl', 'http://localhost:9070/')

//enable waitFor behaviour for all 'at' checks
atCheckWaiting = true
waiting {
  presets {
    slow {
      timeout = 15
    }
  }
}
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
    WebDriver driver = new SauceLabsDriverFactory().create(sauceBrowser, username, accessKey)
    driver.manage().window().maximize()
    return driver
  }
  // increase default timeouts to account for remote execution
  waiting {
    timeout = 10
    retryInterval = 0.5
  }
}
else {
  // see https://github.com/detro/ghostdriver

  String phantomJsBinary = System.getProperty("phantomjs.binary", null)

  driver = {
    DesiredCapabilities capabilities = DesiredCapabilities.phantomjs()
    if (phantomJsBinary) {
      capabilities.setCapability('phantomjs.binary.path', phantomJsBinary)
    }
    capabilities.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, ['--webdriver-loglevel=DEBUG'] as String[]);
    capabilities.
        setCapability(PhantomJSDriverService.PHANTOMJS_GHOSTDRIVER_CLI_ARGS, ["--logLevel=DEBUG"] as String[])
    RemoteWebDriver webDriver = new PhantomJSDriver(capabilities)
    webDriver.setLogLevel(Level.ALL)
    return configure(webDriver)
  }
}

Platform current = Platform.current
environments {

  // run as “mvn -Dgeb.env=chrome test”
  // See: http://code.google.com/p/selenium/wiki/ChromeDriver
  chrome {
    driver = { configure(new ChromeDriver()) }
  }

  // see https://code.google.com/p/selenium/wiki/SafariDriver
  safari {
    if (current.is(Platform.MAC)) {
      driver = { configure(new SafariDriver()) }
    }
    else {
      throw new IllegalStateException('Only runs on mac!')
    }
  }

  // see https://code.google.com/p/selenium/wiki/InternetExplorerDriver
  ie {
    if (current.is(Platform.WINDOWS)) {
      driver = { configure(new InternetExplorerDriver()) }
    }
    else {
      throw new IllegalStateException('Only runs on windows!')
    }
  }

  firefox {
    driver = { configure(new FirefoxDriver()) }
  }

  ci {
    // increase default timeout to account for slower CI server
    waiting {
      timeout = 20
    }
  }
}

def configure(final RemoteWebDriver driver) {
  driver.manage().window().setSize(new Dimension(1280, 1024))
  return disableTransitions(driver)
}

WebDriver disableTransitions(WebDriver driver) {
  return new PageTweakingWebDriver(driver)
}
