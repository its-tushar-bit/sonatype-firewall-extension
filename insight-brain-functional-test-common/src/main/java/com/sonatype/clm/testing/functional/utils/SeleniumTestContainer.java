/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.utils;

import com.codeborne.selenide.Configuration;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

/**
 * Starts a selenium docker container for running functional tests.
 * The image can be chrome or firefox, configurable from the pom.xml file or command line by setting the docker.image
 * system property.
 * By default it starts a selenium container for chrome.
 */
public class SeleniumTestContainer
{
  private static final Logger log = LoggerFactory.getLogger(SeleniumTestContainer.class);

  private static final String DEFAULT_IMAGE = "standalone-chrome:3.141.59-20210607";

  // This is the port baked into the selenium docker images.
  private static final int DEFAULT_PORT = 4444;

  public static void start() {
    String dockerImage = "selenium/" + System.getProperty("docker.image", DEFAULT_IMAGE);
    log.info("Starting selenium container from image {}", dockerImage);
    Capabilities capabilities = dockerImage.contains("firefox") ? new FirefoxOptions() : new ChromeOptions();

    // The functional tests start a singleton container used for all the tests. The container is removed by
    // testcontainers when the jvm exits.
    // So, suppress the warning about the unclosed resource.
    @SuppressWarnings("resource")
    BrowserWebDriverContainer<?> container =
        new BrowserWebDriverContainer<>(dockerImage).withCapabilities(capabilities);
    container.start();
    container.followOutput(new Slf4jLogConsumer(log).withSeparateOutputStreams());

    // Tell selenide how to connect to the selenium container.
    Configuration.remote = "http://127.0.0.1:" + container.getMappedPort(DEFAULT_PORT) + "/wd/hub";

    log.info("Started selenium container on {}", Configuration.remote);
  }
}
