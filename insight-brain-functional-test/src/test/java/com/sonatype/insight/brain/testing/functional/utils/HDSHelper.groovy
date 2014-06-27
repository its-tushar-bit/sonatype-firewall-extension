/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.utils

/**
 * @since 1.12
 */
class HDSHelper
{
  static final String JUNIT_DETAILS_FILE = '/canned-hds-responses/componentDetailsJunit.json'

  static final String JUNIT_DETAILS_LIST_FILE = '/canned-hds-responses/componentDetailsListJunit.json'

  static String createComponentDetailURL(Map component) {
    "rest/ide/component/details?groupId=${component.groupId}&artifactId=${component.artifactId}" +
        "&version=${component.version}"
  }

  static String createComponentDetailListURL(Map component) {
    "rest/ide/component/details/list?groupId=${component.groupId}&artifactId=${component.artifactId}" +
        "&version=${component.version}"
  }
}
