/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import mainHeaderModule from '../../../../main/frontend/mainHeader/module';
import CLMLocationModule from '../../../../main/frontend/util/CLMLocation';

describe('CurrentUserService', function () {
  let CLMLocations, $httpBackend, service;

  beforeEach(angular.mock.module(mainHeaderModule.name, CLMLocationModule.name));

  beforeEach(inject(function (_CLMLocations_, _$httpBackend_, CurrentUser) {
    CLMLocations = _CLMLocations_;
    $httpBackend = _$httpBackend_;

    service = CurrentUser;
  }));

  afterEach(function () {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  describe('fetch', function () {
    it('fetches the user session and resolves the waitForLogin promise with the data', function () {
      $httpBackend.expectGET(CLMLocations.getSessionUrl()).respond('foo');

      const loginSpy = jasmine.createSpy();
      service.waitForLogin().then(loginSpy);
      service.fetch();

      $httpBackend.flush();

      expect(loginSpy).toHaveBeenCalledWith('foo');
    });

    it('passes true as the waitForLogin http config if no parameter is passed to it', inject(function ($http, $q) {
      spyOn($http, 'get').and.returnValue($q.defer().promise);

      service.fetch();

      expect($http.get).toHaveBeenCalledWith(CLMLocations.getSessionUrl(), {
        waitForLogin: true,
      });
    }));

    it('passes its parameter as the waitForLogin http config', inject(function ($http, $q) {
      spyOn($http, 'get').and.returnValue($q.defer().promise);

      service.fetch('asdf');

      expect($http.get).toHaveBeenCalledWith(CLMLocations.getSessionUrl(), {
        waitForLogin: 'asdf',
      });
    }));

    it('rejects the waitforLogin promise if the REST call fails with an error other than 401', function () {
      $httpBackend.expectGET(CLMLocations.getSessionUrl()).respond(500);

      const loginSpy = jasmine.createSpy();
      service.waitForLogin().catch(loginSpy);
      service.fetch();

      $httpBackend.flush();

      expect(loginSpy).toHaveBeenCalledWith(jasmine.objectContaining({ status: 500 }));
    });

    it('neither rejects nor resolves the waitForLogin promise if the REST call fails with a 401', function () {
      $httpBackend.expectGET(CLMLocations.getSessionUrl()).respond(500);

      const loginSpy = jasmine.createSpy();
      service.waitForLogin().then(loginSpy, loginSpy);
      service.fetch();

      $httpBackend.flush();

      expect(loginSpy).not.toHaveBeenCalledWith();
    });
  });
});
