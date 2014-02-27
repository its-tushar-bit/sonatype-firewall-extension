describe('Eclipse View Details tests', function() {
  var httpBackend,
      scope,
      query = { appId: 'appId', groupId: 'gid', artifactId: 'aid', version: '1.0', hash: '12345678901234567890', instanceId: 'iid' },
      wnd,
      data = {
        observedLicenses: [
          {licenseId: 'UNSPECIFIED', licenseName: 'Not Provided'}
        ],
        declaredLicenses: [
          {licenseId: 'Apache-2.0-EPL-1.0', licenseName: 'Apache-2.0 or EPL-1.0'}
        ],
        overriddenLicenses: [
          {licenseId: 'EPL-1.0', licenseName: 'EPL-1.0'}
        ],
        securityVulnerabilities: [],
        policyAlerts: []
      };

  beforeEach(module('viewdetails', function($provide) {
    $provide.constant('query', query);
  }));

  beforeEach(inject(function($httpBackend, $rootScope, $controller, $window) {
    httpBackend = $httpBackend;
    scope = $rootScope.$new();
    wnd = $window;
  }));

  afterEach(function() {
    httpBackend.verifyNoOutstandingExpectation();
    httpBackend.verifyNoOutstandingRequest();
  });

  describe('Legacy Plugin', function() {
    beforeEach(inject(function($httpBackend, $controller) {
      httpBackend.expectGET(new RegExp('/rest/ide/component/details/appId')).respond(angular.copy(data));
      $controller('view', { $scope: scope });
      httpBackend.flush();
    }));

    it('Test License Processing', function() {
      expect(scope.data).not.toBeUndefined();
      expect(scope.data.observedLicenses).toEqual(['Not Provided']);
      expect(scope.data.declaredLicenses).toEqual(['Apache-2.0 or EPL-1.0']);
      expect(scope.data.overriddenLicenses).toEqual(['EPL-1.0']);
    });
  });

  describe('Error Handling', function() {
    it('ignores HTML bodies', inject(function($httpBackend, $controller) {
      httpBackend.expectGET(new RegExp('/rest/ide/component/details/appId')).respond(500, '<html>Error</html>', {'Content-Type': 'text/html'});
      $controller('view', { $scope: scope });
      httpBackend.flush();
      expect(scope.data).toBeNull();
      expect(scope.error).toEqual(500);
      expect(scope.errorMessage).toEqual('Error 500');
    }));

    it('uses plain text bodies', inject(function($httpBackend, $controller) {
      httpBackend.expectGET(new RegExp('/rest/ide/component/details/appId')).respond(500, 'Oops', {'Content-Type': 'text/plain'});
      $controller('view', { $scope: scope });
      httpBackend.flush();
      expect(scope.data).toBeNull();
      expect(scope.error).toEqual(500);
      expect(scope.errorMessage).toEqual('Oops');
    }));

    it('falls back to error code if no message supplied', inject(function($httpBackend, $controller) {
      httpBackend.expectGET(new RegExp('/rest/ide/component/details/appId')).respond(500, '', {'Content-Type': 'text/plain'});
      $controller('view', { $scope: scope });
      httpBackend.flush();
      expect(scope.data).toBeNull();
      expect(scope.error).toEqual(500);
      expect(scope.errorMessage).toEqual('Error 500');
    }));
  });

  describe('Auth-aware Plugin', function() {
    var headers = { 'Authorization': 'Basic foo' };

    beforeEach(inject(function($httpBackend, $controller) {
      angular.extend(query, { deferLoad: 'true' });
      httpBackend.expectGET(new RegExp('/rest/ide/component/details/appId'), function(reqHeaders) {
        var match = true;
        angular.forEach(headers, function(value, key) {
          match = match && reqHeaders[key] === value;
        });
        return match;
      }).respond(angular.copy(data));
      $controller('view', { $scope: scope });
    }));

    it('defers loading data until request headers are set', function() {
      expect(scope.data).toBeNull();
      wnd.setClmHeaders(headers);
      httpBackend.flush();
      expect(scope.data).not.toBeUndefined();
      expect(scope.data.observedLicenses).toEqual(['Not Provided']);
      expect(scope.data.declaredLicenses).toEqual(['Apache-2.0 or EPL-1.0']);
      expect(scope.data.overriddenLicenses).toEqual(['EPL-1.0']);
    });
  });
});
