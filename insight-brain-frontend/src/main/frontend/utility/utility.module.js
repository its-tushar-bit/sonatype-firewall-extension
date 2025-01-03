/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import utilityServicesModule from './services/utility.services.module';
import commonServicesModule from '../utilAngular/CommonServices';
import FormDataHttpInterceptor from './services/form.data.http.interceptor.factory';

export default angular
  .module('utility', ['ui.router.state', 'ngAria', commonServicesModule.name, utilityServicesModule.name])
  .config([
    '$httpProvider',
    function ($httpProvider) {
      $httpProvider.interceptors.push('form.data.http.interceptor');
    },
  ])
  .factory('form.data.http.interceptor', FormDataHttpInterceptor);
