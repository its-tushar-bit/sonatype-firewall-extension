/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
const clmEndpointTemplate = {
  openView: angular.noop,
  type: 'ide',
};

const Brain = window.Brain;

window.clmEndpoint = angular.copy(clmEndpointTemplate);

describe('Eclipse View Details tests', function () {
  var viewDetailsModule,
    Insight,
    httpBackend,
    scope,
    query,
    wnd,
    data = {
      observedLicenses: [{ licenseId: 'UNSPECIFIED', licenseName: 'Not Provided' }],
      declaredLicenses: [
        {
          licenseId: 'Apache-2.0-EPL-1.0',
          licenseName: 'Apache-2.0 or EPL-1.0',
        },
      ],
      overriddenLicenses: [{ licenseId: 'EPL-1.0', licenseName: 'EPL-1.0' }],
      securityVulnerabilities: [],
      policyAlerts: [],
    };

  beforeEach(function () {
    viewDetailsModule = require('inject-loader!../../../main/frontend/version-graph/viewdetails')().default;

    angular.mock.module(viewDetailsModule.name);

    Insight = window.Insight;
  });

  beforeEach(inject(function ($httpBackend, $rootScope, $controller, $window) {
    httpBackend = $httpBackend;
    scope = $rootScope.$new();
    wnd = $window;
    query = {
      appId: 'appId',
      groupId: 'gid',
      artifactId: 'aid',
      version: '1.0',
      hash: '12345678901234567890',
      instanceId: 'iid',
      format: 'maven',
      matchState: 'similar',
      proprietary: false,
    };
  }));

  afterEach(function () {
    httpBackend.verifyNoOutstandingExpectation();
    httpBackend.verifyNoOutstandingRequest();
  });

  describe('setLogger', function () {
    afterEach(function () {
      Insight.resetLogger();
    });

    it('Exceptions before registration are logged', function (done) {
      inject(function ($exceptionHandler) {
        var spy = jasmine.createSpy('logger');
        $exceptionHandler(new Error('foo'));
        Insight.setLogger(spy);

        var interval = setInterval(function () {
          if (spy.calls.count() > 0) {
            clearInterval(interval);
            expect(spy).toHaveBeenCalled();
            expect(spy.calls.first().args[0]).toMatch(/Error: foo.*/);
            done();
          }
        }, 10);
      });
    });

    it('Exceptions after registration are logged', inject(function ($exceptionHandler) {
      var spy = jasmine.createSpy('logger');
      Insight.setLogger(spy);
      $exceptionHandler(new Error('foo'));

      expect(spy).toHaveBeenCalled();
      expect(spy.calls.first().args[0]).toMatch(/Error: foo.*/);
    }));
  });

  describe('Legacy Plugin', function () {
    beforeEach(inject(function ($httpBackend, $controller) {
      httpBackend.expectGET(new RegExp('/rest/ide/componentDetails/application/appId')).respond(angular.copy(data));
      $controller('view', { $scope: scope, query: angular.copy(query) });
      httpBackend.flush();
    }));

    it('Test License Processing', function () {
      expect(scope.data).not.toBeUndefined();
      expect(scope.data.observedLicenses).toEqual(['Not Provided']);
      expect(scope.data.declaredLicenses).toEqual(['Apache-2.0 or EPL-1.0']);
      expect(scope.data.overriddenLicenses).toEqual(['EPL-1.0']);
    });
  });

  describe('Error Handling', function () {
    it('ignores HTML bodies', inject(function ($httpBackend, $controller) {
      httpBackend
        .expectGET(new RegExp('/rest/ide/componentDetails/application/appId'))
        .respond(500, '<html>Error</html>', { 'Content-Type': 'text/html' });

      $controller('view', { $scope: scope, query: angular.copy(query) });
      httpBackend.flush();
      expect(scope.data).toBeNull();
      expect(scope.error).toEqual(500);
      expect(scope.errorMessage).toEqual('Error 500');
    }));

    it('uses plain text bodies', inject(function ($httpBackend, $controller) {
      httpBackend
        .expectGET(new RegExp('/rest/ide/componentDetails/application/appId'))
        .respond(500, 'Oops', { 'Content-Type': 'text/plain' });
      $controller('view', { $scope: scope, query: angular.copy(query) });
      httpBackend.flush();
      expect(scope.data).toBeNull();
      expect(scope.error).toEqual(500);
      expect(scope.errorMessage).toEqual('Oops');
    }));

    it('falls back to error code if no message supplied', inject(function ($httpBackend, $controller) {
      httpBackend
        .expectGET(new RegExp('/rest/ide/componentDetails/application/appId'))
        .respond(500, '', { 'Content-Type': 'text/plain' });
      $controller('view', { $scope: scope, query: angular.copy(query) });
      httpBackend.flush();
      expect(scope.data).toBeNull();
      expect(scope.error).toEqual(500);
      expect(scope.errorMessage).toEqual('Error 500');
    }));
  });

  describe('Auth-aware Plugin', function () {
    var headers = { Authorization: 'Basic foo' };

    beforeEach(inject(function ($httpBackend, $controller) {
      angular.extend(query, { deferLoad: 'true' });
      httpBackend
        .expectGET(new RegExp('/rest/ide/componentDetails/application/appId'), function (reqHeaders) {
          var match = true;
          angular.forEach(headers, function (value, key) {
            match = match && reqHeaders[key] === value;
          });
          return match;
        })
        .respond(angular.copy(data));
      $controller('view', { $scope: scope, query: angular.copy(query) });
    }));

    it('defers loading data until request headers are set', function (done) {
      jasmine.clock().install();
      expect(scope.data).toBeNull();
      wnd.setClmHeaders(headers);

      jasmine.clock().tick(10);
      jasmine.clock().uninstall();

      httpBackend.flush();
      expect(scope.data).not.toBeUndefined();
      expect(scope.data.observedLicenses).toEqual(['Not Provided']);
      expect(scope.data.declaredLicenses).toEqual(['Apache-2.0 or EPL-1.0']);
      expect(scope.data.overriddenLicenses).toEqual(['EPL-1.0']);

      done();
    });
  });

  describe('componentIdentifier', function () {
    beforeEach(function () {
      spyOn(Brain.ide, 'getComponentUrl').and.callThrough();
    });

    it('legacy', inject(function ($controller) {
      httpBackend.expectGET(new RegExp('/rest/ide/componentDetails/application/appId')).respond(angular.copy(data));
      $controller('view', { $scope: scope, query: angular.copy(query) });
      expect(Brain.ide.getComponentUrl).toHaveBeenCalledWith(
        'application',
        'appId',
        'maven',
        '12345678901234567890',
        'similar',
        false,
        {
          groupId: 'gid',
          artifactId: 'aid',
          version: '1.0',
          classifier: undefined,
          extension: undefined,
        }
      );
      httpBackend.flush();
    }));

    it('JSON', inject(function ($controller) {
      query = {
        appId: 'appId',
        hash: '12345678901234567890',
        instanceId: 'iid',
        matchState: 'similar',
        proprietary: false,
        componentIdentifier: JSON.stringify({
          format: 'nuget',
          coordinates: {
            packageId: 'foo',
            version: '1.0',
          },
        }),
      };

      httpBackend.expectGET(new RegExp('/rest/ide/componentDetails/application/appId')).respond(angular.copy(data));
      $controller('view', { $scope: scope, query: angular.copy(query) });
      expect(Brain.ide.getComponentUrl).toHaveBeenCalledWith(
        'application',
        'appId',
        'nuget',
        '12345678901234567890',
        'similar',
        false,
        {
          packageId: 'foo',
          version: '1.0',
        }
      );
      httpBackend.flush();
    }));
  });
});
