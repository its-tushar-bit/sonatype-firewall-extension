/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular */
(function() {
  'use strict';

  /**
   * Enables tipsy tooltip on an element(with fixed parameters)
   */
  angular.module('cip.label.editor').directive('tip', function() {
    return function(scope, element) {
      /**
       * Note: Setting html:false to prevent XSS attacks. See CLM-4637 for more details.
       */
      $(element).tipsy({fade: true, gravity: $.fn.tipsy.autoWE, html: false, opacity: 1.0, delayOut: 0});
    };
  });
}());
