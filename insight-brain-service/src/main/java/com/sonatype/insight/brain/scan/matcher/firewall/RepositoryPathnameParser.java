/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.scan.matcher.firewall;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

import org.apache.commons.lang3.StringUtils;

/**
 * Taken from <a
 * href="https://github.com/sonatype/hosted-data-services/blob/main/insight-portal-webapp/src/main/java/com/sonatype/insight/portal/rest/service/component/RepositoryPathnameParser.java">HDS</a>
 */
@Named
@Singleton
public class RepositoryPathnameParser
{
  private final NpmPathnameParser npmPathnameParser;

  private static final String NPM_FORMAT = "npm";

  @Inject
  public RepositoryPathnameParser(final NpmPathnameParser npmPathnameParser) {
    this.npmPathnameParser = npmPathnameParser;
  }

  public ComponentIdentifier parse(String pathname, String format) {
    if (StringUtils.isBlank(pathname) || StringUtils.isBlank(format)) {
      return null;
    }
    if (pathname.startsWith("/")) {
      pathname = pathname.substring(1);
    }
    return format.equals(NPM_FORMAT) ? npmPathnameParser.parsePathname(pathname) : null;
  }
}
