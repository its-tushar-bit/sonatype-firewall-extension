/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.scan.matcher.firewall;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class NpmPathnameParser
{
  private static final Logger log = LoggerFactory.getLogger(NpmPathnameParser.class);

  private static final Pattern NPM_PACKAGE_PATH_PATTERN = Pattern.compile("(@[^/]+/)?([^/]+)/-/([^/]+)\\.tgz");

  public ComponentIdentifier parsePathname(String path) {
    if (StringUtils.isBlank(path)) {
      return null;
    }

    Matcher matcher = NPM_PACKAGE_PATH_PATTERN.matcher(path);
    if (!matcher.matches()) {
      log.trace("Path '{}' is not an npm path.", path);
      return null;
    }

    String scope = matcher.group(1);
    String packageId = matcher.group(2);
    String packageIdAndVersion = matcher.group(3);

    if (packageId == null || packageIdAndVersion == null) {
      log.trace("Path '{}' is not an npm path.", path);
      return null;
    }

    if (!packageIdAndVersion.startsWith(packageId + "-")) {
      log.trace("Path '{}' is not an npm path.", path);
      return null;
    }
    String version = packageIdAndVersion.substring(packageId.length() + 1);

    packageId = scope == null ? packageId : scope + packageId;
    return ComponentIdentifier.createNpmCoordinates(packageId, version);
  }
}
