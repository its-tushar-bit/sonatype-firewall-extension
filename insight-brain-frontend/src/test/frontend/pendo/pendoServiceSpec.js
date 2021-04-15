/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import pendoModule from '../../../main/frontend/pendo/module';

describe('pendoService', function () {
  var $httpBackend, CLMLocations, $window, pendoService;

  beforeEach(
    angular.mock.module(pendoModule.name, function ($provide) {
      var doc = {
        createElement: function () {
          return {};
        },
        getElementsByTagName: function () {
          return [
            {
              parentNode: {
                insertBefore: function () {},
              },
            },
          ];
        },
      };
      $provide.value('$document', [doc]);
    })
  );

  beforeEach(inject(function (_$httpBackend_, _CLMLocations_, _$window_, _pendoService_) {
    $httpBackend = _$httpBackend_;
    CLMLocations = _CLMLocations_;
    $window = _$window_;
    pendoService = _pendoService_;

    $window.pendo = jasmine.createSpyObj('pendo', ['initialize']);
  }));

  afterEach(inject(function ($window) {
    $httpBackend.verifyNoOutstandingRequest();
    $httpBackend.verifyNoOutstandingExpectation();

    delete $window.pendo;
  }));

  it('initializes pendo when start is called', function () {
    $httpBackend.expectGET(CLMLocations.getUserTelemetryConfig()).respond({ visitors: {}, account: {} });

    pendoService.start();

    $httpBackend.flush();

    expect($window.pendo.initialize).toHaveBeenCalledWith({
      account: {},
      visitors: {},
      excludeAllText: true,
      excludeTitle: true,
      guides: {
        disabled: true,
      },
      contentHost: CLMLocations.getUserTelemetryProxy(),
      dataHost: CLMLocations.getUserTelemetryProxy(),
      sanitizeUrl: jasmine.any(Function),
    });
  });

  describe('flush', function () {
    it('calls pendo.flushNow if it is defined', function () {
      var flushRetval = {};

      $window.pendo.flushNow = jasmine.createSpy('flushNow').and.returnValue(flushRetval);

      var retval = pendoService.flush();

      expect($window.pendo.flushNow).toHaveBeenCalled();
      expect(retval).toBe(flushRetval);
    });

    it('returns a resolved promise if pendo.flushNow is not defined', inject(function ($rootScope) {
      var resolved = false;
      pendoService.flush().then(function () {
        resolved = true;
      });

      $rootScope.$digest();
      expect(resolved).toBe(true);
    }));
  });
});
