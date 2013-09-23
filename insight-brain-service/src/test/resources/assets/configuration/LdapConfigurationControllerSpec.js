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
  }));

  describe('LdapConfigurationController', function() {
    var httpBackend, state;

    beforeEach(inject(function($httpBackend, $rootScope, $controller, $state, CLMLocations) {
      httpBackend = $httpBackend;

      $state.current.name = 'management.configuration.ldap';

      scope = $rootScope.$new();
      state = $state;

      httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getLdapConfig())).respond([]);

      httpBackend.whenGET('../assets/management.html?').respond('<div></div>');
      httpBackend.whenGET('../configuration-assets/components/configuration-navigator.html?').respond('<div></div>');
      httpBackend.whenGET('../configuration-assets/components/ldap.html?').respond('<div></div>');
      httpBackend.whenGET('../configuration-assets/components/ldap-connection.html?').respond('<div></div>');

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

    it('create/update/delete ldap server', inject(function(CLMLocations) {

      // retrieve (empty configuration)

      expect(scope.ldap).not.toBeUndefined();
      expect(scope.ldap.isDirty()).toBeFalsy();
      expect(scope.ldap.id).toBeNull();
      expect(scope.ldap.name).toEqual('');

      // create

      scope.ldap.name = 'config1';

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

      scope.ldap.name = 'config1changed';

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
  });

  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  describe('LdapConnectionController', function() {
    var httpBackend, state, getConfigLdapUrl;

    beforeEach(inject(function($httpBackend, $rootScope, $controller, $state, CLMLocations) {
      httpBackend = $httpBackend;

      getConfigLdapUrl = function() {
        return CLMLocations.getLdapConfig() + '/123/connection';
      };

      $state.current.name = 'management.configuration.ldap.connection';

      scope = $rootScope.$new();
      state = $state;

      scope.ldap = {id: "123"};
      scope.getConfigLdapUrl = getConfigLdapUrl;

      httpBackend.expectGET(SpecUtil.toRegExp(getConfigLdapUrl())).respond(404, "");

      $controller('LdapConnectionController', {
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

    it('create/update/delete ldap connection', inject(function(CLMLocations) {

      // retrieve (empty configuration)

      expect(scope.ldapConn).not.toBeUndefined();
      expect(scope.isDirty()).toBeFalsy();
      expect(scope.ldapConn.id).toBeUndefined();

      // create

      scope.ldapConn.protocol = 'LDAP';
      scope.ldapConn.hostname = 'example.com';
      scope.ldapConn.port = 389;
      scope.ldapConn.searchBase = 'DC=example,DC=com';
      scope.ldapConn.authenticationMethod = 'SIMPLE';
      scope.ldapConn.saslRealm = '';
      scope.ldapConn.username = 'guest';
      scope.ldapConn.password = 'anon';
      scope.ldapConn.connectionTimeout = 60;
      scope.ldapConn.retryDelay = 10;

      expect(scope.isDirty()).toBeTruthy();

      httpBackend.expectPUT(CLMLocations.getLdapConfig() + '/123/connection').respond(function(method, url, data) {
        return [200, angular.extend({
          id: 'id1'
        }, angular.copy(data)), {}];
      });
      scope.save();
      expect(scope.saving).toBeTruthy();
      httpBackend.flush();
      expect(scope.saving).toBeFalsy();

      expect(scope.ldapConn.id).toEqual('id1');

      // update

      scope.ldapConn.authenticationMethod = 'DIGESTMD5';
      scope.ldapConn.saslRealm = 'testing';
      scope.ldapConn.username = 'user';
      scope.ldapConn.password = 'pass';

      expect(scope.isDirty()).toBeTruthy();

      httpBackend.expectPUT(CLMLocations.getLdapConfig() + '/123/connection').respond(function(method, url, data) {
        return [200, angular.copy(data), {}];
      });
      scope.save();
      expect(scope.saving).toBeTruthy();
      httpBackend.flush();
      expect(scope.saving).toBeFalsy();

      expect(scope.isDirty()).toBeFalsy();
      expect(scope.ldapConn.id).toEqual('id1');
    }));

    it('displays confirmation dialog when navigating away from edited data', function() {

      scope.ldapConn.username = 'new_name';

      var e = scope.$broadcast('pageChangeStarted');
      expect(e.defaultPrevented).toBeTruthy();

      scope.ldapConnectionEditor = {
        $dirty: true      
      };

      scope.reset();

      dialogScope.discard();

      e = scope.$broadcast('pageChangeStarted');
      expect(e.defaultPrevented).not.toBeTruthy();

    });

    it('test connection', inject(function(CLMLocations) {
      scope.ldapConn.protocol = 'LDAP';
      scope.ldapConn.hostname = 'example.com';
      scope.ldapConn.port = 389;
      scope.ldapConn.authenticationMethod = 'SIMPLE';
      scope.ldapConn.username = 'guest';
      scope.ldapConn.password = 'anon';

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



  
  
  
  
  
  
  
  describe('LdapUsermappingController', function() {
    var httpBackend, state, getConfigLdapUrl;

    beforeEach(inject(function($httpBackend, $rootScope, $controller, $state, CLMLocations) {
      httpBackend = $httpBackend;

      getConfigLdapUrl = function() {
        return CLMLocations.getLdapConfig() + '/123/userMapping';
      };

      $state.current.name = 'management.configuration.ldap.usermapping';

      scope = $rootScope.$new();
      state = $state;

      scope.ldap = {id: "123"};
      scope.getConfigLdapUrl = getConfigLdapUrl;

      httpBackend.expectGET(SpecUtil.toRegExp(getConfigLdapUrl())).respond(404, "");

      $controller('LdapUsermappingController', {
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

    it('create/update/delete ldap user mapping', inject(function(CLMLocations) {

      // retrieve (empty configuration)

      expect(scope.ldapUserMapping).not.toBeUndefined();
      expect(scope.isDirty()).toBeFalsy();
      expect(scope.ldapUserMapping.id).toBeUndefined();
      expect(scope.ldapUserMapping.serverId).toBe('123');

      // create

      scope.ldapUserMapping.userBaseDN = 'userBaseDN';
      scope.ldapUserMapping.userSubtree = true;
      scope.ldapUserMapping.userObjectClass = 'userObjectClass';
      scope.ldapUserMapping.userFilter = 'userFilter';
      scope.ldapUserMapping.userIDAttribute = 'userIDAttribute';
      scope.ldapUserMapping.realNameAttribute = 'realNameAttribute';
      scope.ldapUserMapping.emailAttribute = 'emailAttribute';
      scope.ldapUserMapping.passwordAttribute = 'passwordAttribute';
      scope.ldapUserMapping.groupMappingType = 'NONE';
      scope.ldapUserMapping.groupBaseDN = 'groupBaseDN';
      scope.ldapUserMapping.groupSubtree = 'groupSubtree';
      scope.ldapUserMapping.groupObjectClass = 'groupObjectClass';
      scope.ldapUserMapping.groupIDAttribute = 'groupIDAttribute';
      scope.ldapUserMapping.groupMemberAttribute = 'groupMemberAttribute';
      scope.ldapUserMapping.groupMemberFormat = 'groupMemberFormat';
      scope.ldapUserMapping.userMemberOfGroupAttribute = 'userMemberOfGroupAttribute';

      expect(scope.isDirty()).toBeTruthy();

      httpBackend.expectPUT(CLMLocations.getLdapConfig() + '/123/userMapping').respond(function(method, url, data) {
        return [200, angular.extend({
          id: 'id1'
        }, angular.copy(data)), {}];
      });
      scope.save();
      expect(scope.saving).toBeTruthy();
      httpBackend.flush();
      expect(scope.saving).toBeFalsy();

      expect(scope.ldapUserMapping.id).toEqual('id1');

      // update

      scope.ldapUserMapping.groupMappingType = 'SIMPLE';

      expect(scope.isDirty()).toBeTruthy();

      httpBackend.expectPUT(CLMLocations.getLdapConfig() + '/123/userMapping').respond(function(method, url, data) {
        return [200, angular.copy(data), {}];
      });
      scope.save();
      expect(scope.saving).toBeTruthy();
      httpBackend.flush();
      expect(scope.saving).toBeFalsy();

      expect(scope.isDirty()).toBeFalsy();
      expect(scope.ldapUserMapping.id).toEqual('id1');
    }));

    it('displays confirmation dialog when navigating away from edited data', function() {

      scope.ldapUserMapping.userBaseDN = 'userBaseDN';

      var e = scope.$broadcast('pageChangeStarted');
      expect(e.defaultPrevented).toBeTruthy();

      scope.ldapUserMappingEditor = {
        $dirty: true      
      };

      scope.reset();

      dialogScope.discard();

      e = scope.$broadcast('pageChangeStarted');
      expect(e.defaultPrevented).not.toBeTruthy();

    });

  });
});
