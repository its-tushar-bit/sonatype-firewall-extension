/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * This file contains HttpInterceptors that are needed only in the top-level IQ UI and not in child frames such as
 * reports (HttpInterceptors.js, in contrast, is bundled in the report js as well)
 */
import SessionSecurityModule from '../SessionSecurityModule';

export default angular
  .module('IqHttpInterceptors', [SessionSecurityModule.name])
  .factory('serverDateInterceptor', [
    'SessionSecurityService',
    function (SessionSecurityService) {
      return {
        response: function (response) {
          var dateString = response.headers('Date'),
            // built-in date parsing on any modern browser should support HTTP date format
            serverDate = dateString ? new Date(dateString) : undefined;

          if (serverDate) {
            SessionSecurityService.setServerDate(serverDate);
          }

          return response;
        },
      };
    },
  ])
  .config([
    '$httpProvider',
    function ($httpProvider) {
      $httpProvider.interceptors.push('serverDateInterceptor');
    },
  ]);
