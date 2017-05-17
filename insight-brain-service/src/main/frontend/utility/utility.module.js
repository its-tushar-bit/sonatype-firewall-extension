/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, angularDebug */
(function(angular) {
  'use strict';

  var utility = angular.module('utility', ['ui.router.state', 'ngAria', 'CommonServices', 'FormsModule', 'utility.directives', 'utility.services']).config([
    '$httpProvider', function($httpProvider) {
      $httpProvider.interceptors.push('form.data.http.interceptor');
    }
  ]);

}(angular));
