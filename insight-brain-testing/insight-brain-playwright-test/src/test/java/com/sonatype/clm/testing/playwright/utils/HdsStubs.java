/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.sonatype.insight.brain.service.HdsMockServerRule;

import org.apache.commons.io.IOUtils;

/**
 * Shared HDS stub bundles for Playwright tests. Group new stub bundles as additional static methods
 * so callers don't have to re-implement the same {@code respondWith(...).atUri(...)} chains.
 */
public final class HdsStubs
{
  private HdsStubs() {
  }

  /**
   * Stub the HDS endpoints required by the Component Legal Overview page and its sub-tiles
   * (Copyright, Notice Files, License Files, Original Sources, Attribution Report). Loads the
   * {@code /legal/*} fixtures from the classpath and stubs an empty {@code /rest/legal/file}.
   */
  public static void legalOverview(HdsMockServerRule hdsServer) throws IOException {
    legalOverview(hdsServer, false);
  }

  /**
   * Variant of {@link #legalOverview(HdsMockServerRule)} where {@code withFiles=true} stubs
   * {@code /rest/legal/file} with the populated {@code legalFileHdsResponse.json} fixture instead
   * of {@code "[]"} — used by Notice Files tests that exercise populated state.
   */
  public static void legalOverview(HdsMockServerRule hdsServer, boolean withFiles) throws IOException {
    hdsServer
        .respondWith(read("/legal/legalLicenseMetadataHdsResponse.json"))
        .atUri("/rest/license/metadata");
    hdsServer
        .respondWith(read("/legal/legalCommentHdsResponse.json"))
        .atUri("/rest/legal/comment");
    hdsServer
        .respondWith(withFiles ? read("/legal/legalFileHdsResponse.json") : "[]")
        .atUri("/rest/legal/file");
    hdsServer
        .respondWith("[]")
        .atUri("/rest/legal/source-link");
    hdsServer
        .respondWith(read("/legal/componentDetails.json"))
        .atUri("rest/ci/componentDetails");
    hdsServer
        .respondWith(read("/legal/componentDetailsList.json"))
        .atUri("rest/ci/componentDetails/list");
  }

  private static String read(String absoluteResourcePath) throws IOException {
    try (InputStream in = HdsStubs.class.getResourceAsStream(absoluteResourcePath)) {
      if (in == null) {
        throw new IOException("Resource not found on classpath: " + absoluteResourcePath);
      }
      return IOUtils.toString(in, StandardCharsets.UTF_8);
    }
  }
}
