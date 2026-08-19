/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.utils;

import com.codeborne.selenide.Configuration;

import java.net.URI;

import jakarta.ws.rs.core.UriBuilder;

import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

/**
 * Starts a selenium docker container for running functional tests.
 * The image can be configured from the pom.xml file or command line by setting the docker.image system property.
 * By default it starts a selenium container for chrome.
 */
public class SeleniumTestContainer
{
  private static final Logger log = LoggerFactory.getLogger(SeleniumTestContainer.class);

  private static final String DEFAULT_IMAGE = "standalone-chrome:120.0-20240123";

  // This is the port baked into the selenium docker images.
  private static final int DEFAULT_PORT = 4444;

  /**
   * @param baseUrl the base url of the application under test, as accessible from the docker host
   * @return the baseUrl of the application under test, as accessible from within the selenium container
   */
  public static String start(String baseUrl) {
    String dockerImage = "selenium/" + System.getProperty("docker.image", DEFAULT_IMAGE);
    URI serverUri = URI.create(baseUrl);
    log.info("Starting selenium container from image {}", dockerImage);
    ChromeOptions capabilities = new ChromeOptions();

    Testcontainers.exposeHostPorts(serverUri.getPort());

    // The functional tests start a singleton container used for all the tests. The container is removed by
    // testcontainers when the jvm exits.
    // So, suppress the warning about the unclosed resource.
    @SuppressWarnings("resource")
    BrowserWebDriverContainer<?> container =
        new BrowserWebDriverContainer<>(dockerImage).withCapabilities(capabilities).withAccessToHost(true);

    container.addEnv("SE_OPTS", "--enable-managed-downloads true");
    container.start();

    container.followOutput(new Slf4jLogConsumer(log).withSeparateOutputStreams());
    Configuration.headless = true;
    // Tell selenide how to connect to the selenium container.
    Configuration.remote =
        String.format("http://%s:%s/wd/hub", container.getHost(), container.getMappedPort(DEFAULT_PORT));

    log.info("Started selenium container on {}", Configuration.remote);

    // host.testcontainers.internal is the domain name usable within the container for referencing the host
    return UriBuilder.fromUri(serverUri).host("host.testcontainers.internal").build().toString();
  }
}
