/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.viewdetails

import groovy.json.JsonOutput

/**
 * @since 1.12
 */
class EclipseViewDetailsPage
    extends AbstractViewDetailsPage
{
  static url = 'assets/version-graph/ide/eclipse/viewdetails.html'

  /**
   * Configure authentication header to be used for ajax requests on the page
   */
  def setAuthHeaders(String username, String password) {
    def map = ['Authorization': 'Basic ' + "${username}:${password}".bytes.encodeBase64().toString()]
    def authJson = JsonOutput.toJson(map)
    browser.js.exec(authJson, 'window.setClmHeaders(jQuery.parseJSON(arguments[0]));')
  }

  /**
   * Hook to force reload of the page
   */
  def reload() {
    browser.js.exec('angular.element("html").scope().$broadcast("reload")')
  }
}
