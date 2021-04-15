/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import utilityModule from '../../../../main/frontend/utility/utility.module';

describe('cached.service.spec', function () {
  var service, swapUrl;

  beforeEach(angular.mock.module(utilityModule.name));

  beforeEach(inject([
    'cached.service.factory',
    function (cachedServiceFactory) {
      var callCounter = 0;
      swapUrl = false;

      service = cachedServiceFactory.create(function () {
        if (swapUrl) {
          callCounter++;
        }
        return '' + callCounter;
      });
    },
  ]));

  describe('get', function () {
    it('basic', inject(function ($httpBackend) {
      var success = jasmine.createSpy('success'),
        failure = jasmine.createSpy('failure');

      $httpBackend.expectGET('0').respond({ id: 'foo' });
      service.get().then(success, failure);
      $httpBackend.flush();

      expect(failure).not.toHaveBeenCalled();
      expect(success).toHaveBeenCalledWith({ id: 'foo' });
    }));

    it('cleared on URL change', inject(function ($httpBackend) {
      var success = jasmine.createSpy('success'),
        failure = jasmine.createSpy('failure');

      // initial request
      $httpBackend.expectGET('0').respond({ id: 'foo' });
      service.get();
      $httpBackend.flush();

      // second request at different URL
      swapUrl = true;
      $httpBackend.expectGET('1').respond({ id: 'bar' });
      service.get().then(success, failure);
      $httpBackend.flush();

      expect(failure).not.toHaveBeenCalled();
      expect(success).toHaveBeenCalledWith({ id: 'bar' });
    }));

    it('data cached', inject(function ($httpBackend, $timeout) {
      var success = jasmine.createSpy('success'),
        failure = jasmine.createSpy('failure');

      // Initial load
      $httpBackend.expectGET('0').respond({ id: 'foo' });
      service.get();
      $httpBackend.flush();

      // A follow request for the same data
      service.get().then(success, failure);
      $timeout.flush();

      expect(failure).not.toHaveBeenCalled();
      expect(success).toHaveBeenCalledWith({ id: 'foo' });

      $httpBackend.verifyNoOutstandingExpectation();
    }));

    it('error cleared', inject(function ($httpBackend) {
      var success = jasmine.createSpy('success'),
        failure = jasmine.createSpy('failure');

      // initial request
      $httpBackend.expectGET('0').respond(404);
      service.get().then(success, failure);
      $httpBackend.flush();

      expect(failure).toHaveBeenCalled();

      // a successful request for the same URL
      $httpBackend.expectGET('0').respond({ id: 'bar' });
      service.get().then(success, failure);
      $httpBackend.flush();

      expect(success).toHaveBeenCalledWith({ id: 'bar' });
    }));

    it('parallel requests use same http request', inject(function (
      $httpBackend
    ) {
      var success = jasmine.createSpy('success'),
        failure = jasmine.createSpy('failure');

      $httpBackend.expectGET('0').respond({ id: 'foo' });
      service.get();
      service.get().then(success, failure);
      $httpBackend.flush();

      expect(failure).not.toHaveBeenCalled();
      expect(success).toHaveBeenCalledWith({ id: 'foo' });
    }));
  });

  it('refresh', inject(function ($httpBackend) {
    var success = jasmine.createSpy('success'),
      failure = jasmine.createSpy('failure');

    // initial request
    $httpBackend.expectGET('0').respond({ id: 'foo' });
    service.get();
    $httpBackend.flush();

    // refresh
    $httpBackend.expectGET('0').respond({ id: 'bar' });
    service.refresh().then(success, failure);
    $httpBackend.flush();

    expect(failure).not.toHaveBeenCalled();
    expect(success).toHaveBeenCalledWith({ id: 'bar' });
  }));
});
