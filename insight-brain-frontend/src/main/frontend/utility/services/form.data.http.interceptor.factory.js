/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function FormDataHttpInterceptor($window) {
  return {
    request: function (config) {
      if (config.method === 'POST' && $window.FormData && config.data instanceof FormData) {
        config.headers['Content-Type'] = undefined;
        config.transformRequest = angular.identity;
      }
      return config;
    },
  };
}

FormDataHttpInterceptor.$inject = ['$window'];
