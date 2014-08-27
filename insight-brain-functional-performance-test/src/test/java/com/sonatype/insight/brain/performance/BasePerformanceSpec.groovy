/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.performance

import com.sonatype.insight.brain.service.PortAllocator

import geb.report.ReporterSupport
import geb.spock.GebSpec
import groovy.util.logging.Slf4j
import net.lightbody.bmp.proxy.ProxyServer
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.mime.content.ByteArrayBody
import org.apache.http.impl.client.HttpClientBuilder
import org.junit.Rule
import org.junit.rules.TestName
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxProfile
import org.openqa.selenium.phantomjs.PhantomJSDriver
import org.openqa.selenium.phantomjs.PhantomJSDriverService
import org.openqa.selenium.remote.CapabilityType
import org.openqa.selenium.remote.DesiredCapabilities
import spock.lang.Shared

import static org.openqa.selenium.phantomjs.PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY
import static org.openqa.selenium.phantomjs.PhantomJSDriverService.PHANTOMJS_GHOSTDRIVER_PATH_PROPERTY

/**
 * Configures a proxy for all requests and provides convenience mechanisms for exporting HAR captured during test.
 * Copies some code from GebReportingSpec to allow for capturing screenshots and page source while still controlling
 * the driver lifecycle.
 *
 * @since 1.12
 */
@Slf4j
abstract class BasePerformanceSpec
    extends GebSpec
{

  //start copied from GebReportingSpec
  // Ridiculous name to avoid name clashes
  @Rule TestName _gebReportingSpecTestName
  def _gebReportingPerTestCounter = 1
  @Shared _gebReportingSpecTestCounter = 1
  void report(String label = "") {
    browser.report(ReporterSupport.toTestReportLabel(_gebReportingSpecTestCounter, _gebReportingPerTestCounter++, _gebReportingSpecTestName.methodName, label))
  }
  //end

  @Shared
  ProxyServer proxyServer

  @Shared
  DesiredCapabilities capabilities

  static final int PROXY_PORT = PortAllocator.findFreePort(9090)

  static final boolean useHarStorage = System.getProperty('useHarStorage', 'false').toBoolean()
  static final String harStorageUrl = System.getProperty('harStorageUrl', 'http://localhost:5000')

  def setupSpec() {
    //start copied from GebReportingSpec
    reportGroup getClass()
    cleanReportGroupDir()
    //end

    proxyServer = new ProxyServer(PROXY_PORT);
    proxyServer.start();

    //bypass login dialog to remove it from clouding page load timing
    proxyServer.addHeader('Authorization', "Basic ${'admin:admin123'.bytes.encodeBase64().toString()}")

    // get the Selenium proxy object
    org.openqa.selenium.Proxy proxy = proxyServer.seleniumProxy();

    def proxyUrl = "localhost:$PROXY_PORT"
    proxy.setHttpProxy(proxyUrl)
    proxy.setSocksProxy(proxyUrl)

    switch(browser.config.properties['geb.env']) {
      case 'phantom':
        capabilities = DesiredCapabilities.phantomjs();
        capabilities.
            setCapability(PHANTOMJS_EXECUTABLE_PATH_PROPERTY, System.getProperty(PHANTOMJS_GHOSTDRIVER_PATH_PROPERTY))
        capabilities.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, ["--proxy=$proxyUrl"] as String[])
        break
      case 'chrome':
        capabilities = DesiredCapabilities.chrome()
        capabilities.setCapability(CapabilityType.PROXY, proxy)
        break
      default :
        // configure proxy as a desired capability
        FirefoxProfile profile = new FirefoxProfile();
        capabilities = new DesiredCapabilities();
        profile.setAcceptUntrustedCertificates(true);
        profile.setAssumeUntrustedCertificateIssuer(true);
        profile.setPreference("network.proxy.http", "localhost")
        profile.setPreference("network.proxy.http_port", PROXY_PORT)
        profile.setPreference("network.proxy.ssl", "localhost")
        profile.setPreference("network.proxy.ssl_port", PROXY_PORT)
        profile.setPreference("network.proxy.type", 1)
        profile.setPreference("network.proxy.no_proxies_on", "")
        capabilities.setCapability(FirefoxDriver.PROFILE, profile);
        capabilities.setCapability(CapabilityType.PROXY, proxy);
    }
  }

  def setup()
  {
    //start copied from GebReportingSpec
    reportGroup getClass()
    //end

    // assign this as the default driver on the browser for each test
    switch(browser.config.properties['geb.env']) {
      case 'phantom':
        browser.driver = new PhantomJSDriver(capabilities)
        break
      case 'chrome':
        browser.driver = new ChromeDriver(capabilities)
        break
      default:
        browser.driver = new FirefoxDriver(capabilities)
    }
  }

  def cleanup () {
    //start copied from GebReportingSpec
    report "end"
    ++_gebReportingSpecTestCounter
    //end

    // kill the browser to ensure clean state for each test
    browser.driver.quit()
  }

  def cleanupSpec() {
    // after running the spec, kill the proxy server
    proxyServer.stop()
  }

  void reportHAR(String name) {
    if (!useHarStorage) {
      new File(browser.getReportGroupDir(), "${name}.har".toString()).withOutputStream { os ->
        proxyServer.har.writeTo(os)
        proxyServer.endPage()
      }
    } else {
      def client = HttpClientBuilder.create().build()
      def post = new HttpPost("${harStorageUrl}/results/upload")

      def byteStream = new ByteArrayOutputStream()
      proxyServer.har.writeTo(byteStream)
      def body = new ByteArrayBody(byteStream.toByteArray(), "application/json", "${name}.har")

      def entity = new MultipartEntity()
      entity.addPart("file", body)

      post.setEntity(entity)
      client.execute(post)
    }
  }
}
