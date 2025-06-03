/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqHttpInterceptors from '../../../main/frontend/utilAngular/IqHttpInterceptors';
import * as sessionExpirationManager from 'MainRoot/session/sessionExpirationManager';

describe('IqHttpInterceptors', function () {
  var $httpBackend, $http;

  beforeEach(
    angular.mock.module(iqHttpInterceptors.name, function ($provide) {
      SpecUtil.mockNgRedux($provide);
    })
  );

  beforeEach(inject(function (_$httpBackend_, _$http_) {
    $httpBackend = _$httpBackend_;
    $http = _$http_;
  }));

  describe('serverDateInterceptor', function () {
    var $filter;

    beforeEach(inject(function (_$filter_) {
      $filter = _$filter_;
    }));

    it('calls sessionExpirationManager.setServerDate with the parsed value of the Date HTTP header', function () {
      // make server date 5 seconds behind
      var currentDate = new Date(),
        serverDate = new Date(currentDate - 5000),
        dateHeaderValue = $filter('date')(serverDate, 'EEE, dd MMM yyyy HH:mm:ss', 'GMT') + ' GMT',
        parsedDate;

      function getTimeWithoutMilliseconds(date) {
        return Math.floor(date.getTime() / 1000);
      }

      // mock response with Date header 5 seconds in the past
      $httpBackend.expectGET('test').respond(200, {}, { Date: dateHeaderValue });

      spyOn(sessionExpirationManager, 'setServerDate').and.callFake(function (date) {
        parsedDate = date;
      });

      $http.get('test');

      $httpBackend.flush();

      expect(sessionExpirationManager.setServerDate).toHaveBeenCalled();
      expect(getTimeWithoutMilliseconds(parsedDate)).toEqual(getTimeWithoutMilliseconds(serverDate));
    });

    it('does not call sessionExpirationManager.setServerDate if there is no Date header', function () {
      // mock response with Date header 5 seconds in the past
      $httpBackend.expectGET('test').respond(200);

      spyOn(sessionExpirationManager, 'setServerDate');

      $http.get('test');

      expect(sessionExpirationManager.setServerDate).not.toHaveBeenCalled();
    });
  });
});
