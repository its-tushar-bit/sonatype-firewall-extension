/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import systemNoticeModule from '../../../main/frontend/systemNotice/systemNoticeModule';
import clmLocationModule from '../../../main/frontend/util/CLMLocation';
import SystemNoticeMockData from './systemNoticeMockData';

describe('systemNoticeServiceSpec.js', function () {
  beforeEach(
    angular.mock.module(systemNoticeModule.name, clmLocationModule.name)
  );

  var systemNoticeService, $httpBackend, successSpy, failSpy, CLMLocations;

  beforeEach(inject(function (
    _systemNoticeService_,
    _$httpBackend_,
    _CLMLocations_
  ) {
    systemNoticeService = _systemNoticeService_;
    $httpBackend = _$httpBackend_;
    successSpy = jasmine.createSpy('successSpy');
    failSpy = jasmine.createSpy('failSpy');
    CLMLocations = _CLMLocations_;
  }));

  afterEach(function () {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  describe('getting the system notice', function () {
    it('returns it when the request succeeds', function () {
      systemNoticeService.getSystemNotice().then(successSpy).catch(failSpy);
      $httpBackend
        .expectGET(CLMLocations.getSystemNoticeFetchUrl())
        .respond(SystemNoticeMockData.getSystemNotice('message', true));
      $httpBackend.flush();

      expect(successSpy).toHaveBeenCalledWith(
        SystemNoticeMockData.getSystemNotice('message', true)
      );
      expect(failSpy).not.toHaveBeenCalled();
    });

    it('throws a failed request', function () {
      systemNoticeService.getSystemNotice().then(successSpy).catch(failSpy);
      $httpBackend
        .expectGET(CLMLocations.getSystemNoticeFetchUrl())
        .respond(404, 'not found');
      $httpBackend.flush();

      expect(successSpy).not.toHaveBeenCalled();
      expect(failSpy).toHaveBeenCalled();
      expect(failSpy.calls.mostRecent().args[0].status).toEqual(404);
      expect(failSpy.calls.mostRecent().args[0].data).toEqual('not found');
    });
  });

  it('returns the default system notice on get default system notice', function () {
    var defaultSystemNotice = systemNoticeService.getDefaultSystemNotice();
    expect(defaultSystemNotice.message).toEqual(
      'Error: could not get the system notice from the server'
    );
    expect(defaultSystemNotice.enabled).toBe(true);
  });

  describe('saving the system notice and putting it in the request', function () {
    it('returns no content when the request succeeds', function () {
      var systemNotice = SystemNoticeMockData.getSystemNotice('message', true);
      systemNoticeService
        .saveSystemNotice(systemNotice)
        .then(successSpy)
        .catch(failSpy);
      $httpBackend
        .expectPUT(CLMLocations.getSystemNoticeUrl(), systemNotice)
        .respond(204, 'no content');
      $httpBackend.flush();

      expect(successSpy).toHaveBeenCalledWith('no content');
      expect(failSpy).not.toHaveBeenCalled();
    });

    it('throws a failed request', function () {
      var systemNotice = SystemNoticeMockData.getSystemNotice('message', true);
      systemNoticeService
        .saveSystemNotice(systemNotice)
        .then(successSpy)
        .catch(failSpy);
      $httpBackend
        .expectPUT(CLMLocations.getSystemNoticeUrl(), systemNotice)
        .respond(401, 'unauthorized');
      $httpBackend.flush();

      expect(successSpy).not.toHaveBeenCalled();
      expect(failSpy).toHaveBeenCalled();
      expect(failSpy.calls.mostRecent().args[0].status).toEqual(401);
      expect(failSpy.calls.mostRecent().args[0].data).toEqual('unauthorized');
    });
  });
});
