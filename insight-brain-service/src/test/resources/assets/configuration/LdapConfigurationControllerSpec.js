describe('Tests for the LdapConfigurationController', function() {
  var scope, dialogScope;

  beforeEach(module('LdapConfiguration', function($provide, $stateProvider) {
    $provide.factory('hudson', [
      '$http', function($http) {
        return $http;
      }
    ]);
    $provide.value('$dialog', {
      dialog: function(config) {
        dialogScope = scope.$new();
        return {
          open: function() {
            inject(function($controller) {
              $controller(config.controller, {
                $scope: dialogScope,
                dialog: {
                  close: function() {
                	dialogScope.$destroy();
                  }
                }
              });
            });
          }
        };
      }
    });
    $stateProvider.state('management', {
        url: '/management'
    }).state('management.configuration', {
        parent: 'management',
        url: '/configuration'
    }).state('management.configuration.ldap', {
        parent: 'management.configuration',
        url: '/ldap'
    });
  }));

  describe('LdapConfigurationController', function() {
    var httpBackend, rootScope, state;

    beforeEach(inject(function($httpBackend, $rootScope, $controller, $state, CLMLocations) {
      httpBackend = $httpBackend;
      rootScope = $rootScope;

      $state.current.name = 'management.configuration.ldap';

      scope = $rootScope.$new();
      state = $state;

      httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getLdapConfig())).respond([]);

      $controller('LdapConfigurationController', {
        $scope: scope,
        $state: state
      });

      httpBackend.flush();
    }));

    afterEach(function() {
      httpBackend.verifyNoOutstandingExpectation();
      httpBackend.verifyNoOutstandingRequest();
      scope.$destroy();
    });

    it('create/update/delete ldap configuration', inject(function(CLMLocations) {
   
      // retrieve (empty configuration)

      expect(scope.ldap).not.toBeUndefined();
      expect(scope.ldap.isDirty()).toBeFalsy();
      expect(scope.ldap.id).toBeNull();
      expect(scope.ldap.name).toEqual('');

      // create

      scope.ldap.name = 'config1';
      scope.ldap.protocol = 'LDAP';
      scope.ldap.hostname = 'example.com';
      scope.ldap.port = 389;
      scope.ldap.searchBase = 'DC=example,DC=com';
      scope.ldap.authenticationMethod = 'SIMPLE';
      scope.ldap.saslRealm = '';
      scope.ldap.username = 'guest';
      scope.ldap.password = 'anon';
      scope.ldap.connectionTimeout = 60;
      scope.ldap.retryDelay = 10;

      expect(scope.ldap.isDirty()).toBeTruthy();

      httpBackend.expectPOST(SpecUtil.toRegExp(CLMLocations.getLdapConfig())).respond(
        function(method, url, data) {
          return [200, angular.extend({id: 'id1'}, angular.copy(data)), {}];
        });
      scope.save();
      expect(scope.saving).toBeTruthy();
      httpBackend.flush();
      expect(scope.saving).toBeFalsy();

      expect(scope.ldap.id).toEqual('id1');

      // update

      scope.ldap.authenticationMethod = 'DIGESTMD5';
      scope.ldap.saslRealm = 'testing';
      scope.ldap.username = 'user';
      scope.ldap.password = 'pass';

      expect(scope.ldap.isDirty()).toBeTruthy();

      httpBackend.expectPUT(SpecUtil.toRegExp(CLMLocations.getLdapConfig())).respond(
        function(method, url, data) {
    	  return [200, angular.copy(data), {}];
        });
      scope.save();
      expect(scope.saving).toBeTruthy();
      httpBackend.flush();
      expect(scope.saving).toBeFalsy();

      expect(scope.ldap.isDirty()).toBeFalsy();
      expect(scope.ldap.id).toEqual('id1');

      // delete

      expect(angular.element('#deleteConfigurationModal').css('display')).toBeUndefined();

      scope.confirmDeleteConfiguration(scope.ldap);

      expect(angular.element('#deleteConfigurationModal').css('display')).not.toBe('none');

      httpBackend.expectDELETE(CLMLocations.getLdapConfig() + '/id1').respond({});
      scope.deleteConfiguration();
      httpBackend.flush();

      expect(angular.element('#deleteConfigurationModal').css('display')).toBeUndefined();

      expect(scope.ldap).toBeNull();

    }));

    it('displays confirmation dialog when navigating away from edited data', function() {

      scope.ldap.name = 'new_name';

      var e = scope.$broadcast('pageChangeStarted');
      expect(e.defaultPrevented).toBeTruthy();

      scope.reset();

      dialogScope.discard();

      e = scope.$broadcast('pageChangeStarted');
      expect(e.defaultPrevented).not.toBeTruthy();

    });

    it('test connection', inject(function(CLMLocations) {
      scope.ldap.name = 'config1';
      scope.ldap.protocol = 'LDAP';
      scope.ldap.hostname = 'example.com';
      scope.ldap.port = 389;
      scope.ldap.authenticationMethod = 'SIMPLE';
      scope.ldap.username = 'guest';
      scope.ldap.password = 'anon';

      // configuration is good
      httpBackend.expectPUT(CLMLocations.getLdapConfig() + '/test').respond(
        function(method, url, data) {
          return [200, {status: 'OK'}, {}];
        });
      scope.testConnection();
      httpBackend.flush();
      expect(scope.alerts.length).toBe(1);
      expect(scope.alerts[0].type).toBe('success');

      // configuration is bad
      httpBackend.expectPUT(CLMLocations.getLdapConfig() + '/test').respond(
        function(method, url, data) {
          return [200, {status: 'FAILURE', message: 'foo bar'}, {}];
        });
      scope.testConnection();
      httpBackend.flush();
      expect(scope.alerts.length).toBe(1);
      expect(scope.alerts[0].type).toBe('error');
      expect(scope.alerts[0].msg).toBe('foo bar');

      // clm server misbehaves
      httpBackend.expectPUT(CLMLocations.getLdapConfig() + '/test').respond(
        function(method, url, data) {
          return [500, 'foo bar', {}];
        });
      scope.testConnection();
      httpBackend.flush();
      expect(scope.alerts.length).toBe(1);
      expect(scope.alerts[0].type).toBe('error');
      expect(scope.alerts[0].msg).toBe('foo bar');

      // can't connect to clm server
      httpBackend.expectPUT(CLMLocations.getLdapConfig() + '/test').respond(
        function(method, url, data) {
          return [0, '', {}];
        });
      scope.testConnection();
      httpBackend.flush();
      expect(scope.alerts.length).toBe(1);
      expect(scope.alerts[0].type).toBe('error');
      expect(scope.alerts[0].msg).toBe('Unable to reach CLM server');

    }));
  });
});
