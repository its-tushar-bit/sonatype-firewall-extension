/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import telemetryServiceModule from '../../../../main/frontend/services/telemetryService';

describe('telemetryService', function() {
  var CLMLocations, telemetryService, $cookies, $http;

  beforeEach(angular.mock.module(telemetryServiceModule.name, function($provide) {
    $cookies = jasmine.createSpyObj('$cookies', ['get']);

    $provide.value('$cookies', $cookies);
  }));

  beforeEach(inject(function(_CLMLocations_, _telemetryService_, _$http_) {
    CLMLocations = _CLMLocations_;
    telemetryService = _telemetryService_;
    $http = _$http_;
  }));

  describe('submitData', function() {
    it('submits proper json asynchronously by default', function() {
      spyOn(XMLHttpRequest.prototype, 'open');
      spyOn(XMLHttpRequest.prototype, 'setRequestHeader');
      spyOn(XMLHttpRequest.prototype, 'send');
      $cookies.get.and.returnValue('xsrfCookieTestValue');

      telemetryService.submitData('test_purpose', {
        testAttribute1: 'testAttr1',
        testAttribute2: 'testAttr2'
      });

      expect($cookies.get).toHaveBeenCalledWith($http.defaults.xsrfCookieName);
      expect(XMLHttpRequest.prototype.open).toHaveBeenCalledWith('POST', CLMLocations.getTelemetryUrl(), true);
      expect(XMLHttpRequest.prototype.setRequestHeader).toHaveBeenCalledWith('Content-Type', 'application/json');
      expect(XMLHttpRequest.prototype.setRequestHeader).toHaveBeenCalledWith($http.defaults.xsrfHeaderName,
          'xsrfCookieTestValue');

      var telemetryData = JSON.parse(XMLHttpRequest.prototype.send.calls.argsFor(0));
      expect(telemetryData).toEqual(jasmine.objectContaining({
        purpose: 'test_purpose',
        timestamp: jasmine.any(Number),
        attributes: {
          testAttribute1: 'testAttr1',
          testAttribute2: 'testAttr2'
        }
      }));
    });

    it('submits data synchronously when provided sync flag is true', function() {
      spyOn(XMLHttpRequest.prototype, 'open').and.callThrough();
      spyOn(XMLHttpRequest.prototype, 'send');
      telemetryService.submitData('test_purpose', null, true);
      expect(XMLHttpRequest.prototype.open).toHaveBeenCalledWith('POST', CLMLocations.getTelemetryUrl(), false);
    });

    it('submits data asynchronously when provided sync flag is not true but truthy', function() {
      spyOn(XMLHttpRequest.prototype, 'open').and.callThrough();
      spyOn(XMLHttpRequest.prototype, 'send');
      telemetryService.submitData('test_purpose', null, 1);
      expect(XMLHttpRequest.prototype.open).toHaveBeenCalledWith('POST', CLMLocations.getTelemetryUrl(), true);
    });
  });
});
