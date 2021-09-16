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

driver = {
  if (System.getProperty("remote") != null) {
    def capabilities = new DesiredCapabilities()
    capabilities.setBrowserName("chrome")

    configure(new RemoteWebDriver(new URL(System.getProperty("remote")), capabilities))
  }
  else {
    configure(new ChromeDriver())
  }
}

environments {
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
