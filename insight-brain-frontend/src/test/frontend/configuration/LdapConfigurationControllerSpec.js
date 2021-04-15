/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ldapModule from '../../../main/frontend/configuration/ldap/ldap.module';
import { httpInterceptors } from '../../../main/frontend/util/HttpInterceptors';

describe('Tests for the LdapConfigurationController', function () {
  var scope, dialogScope;

  beforeEach(
    angular.mock.module(
      ldapModule.name,
      httpInterceptors.name,
      function ($provide) {
        $provide.value('Modal', {
          open: function (config) {
            dialogScope = scope.$new();
            dialogScope.$close = function () {};
            inject(function ($controller) {
              $controller(config.controller, {
                $scope: dialogScope,
              });
            });
            return {
              result: {
                then: function (success) {
                  success();
                },
              },
            };
          },
        });

        $provide.value('$state', {
          go: angular.noop,
          transitionTo: angular.noop,
          params: {
            ldapId: '123',
          },
          current: {
            name: 'ldap',
          },
        });
        SpecUtil.mockPermissionService($provide);
      }
    )
  );

  describe('LdapConfigurationController', function () {
    function initializeController(available, ldapId) {
      inject(function ($state, $controller, $httpBackend, CLMContextLocations) {
        $state.params.ldapId = ldapId;
        if (!ldapId) {
          $state.current.name = 'create-ldap';
        }

        $controller('LdapConfigurationController', {
          $scope: scope,
          $state: $state,
          isAuthorized: true,
        });

        $httpBackend
          .expectGET(SpecUtil.toRegExp(CLMContextLocations.getLdapConfig()))
          .respond([
            {
              id: '123',
              name: 'config1',
            },
          ]);
        $httpBackend.flush();
      });
    }
    var httpBackend;

    beforeEach(inject(function ($httpBackend, $rootScope, $controller, $state) {
      httpBackend = $httpBackend;

      spyOn($state, 'go').and.returnValue(false);
      spyOn($state, 'transitionTo').and.returnValue(false);

      scope = $rootScope.$new();
      scope.$$childHead = scope.$new();
      scope.$$childHead.ldapNameForm = {
        $save: angular.noop,
      };
      scope.ldapNameForm = {
        $save: angular.noop,
      };

      httpBackend
        .whenGET('owner.manager/state/owner.manager.view.html?')
        .respond('<div></div>');
      httpBackend
        .whenGET('configuration/components/ldap.html?')
        .respond('<div></div>');
      httpBackend
        .whenGET('configuration/components/ldap-connection.html?')
        .respond('<div></div>');
    }));

    afterEach(function () {
      httpBackend.verifyNoOutstandingExpectation();
      httpBackend.verifyNoOutstandingRequest();
      scope.$destroy();
    });

    it('create ldap server', inject(function (CLMContextLocations, $state) {
      initializeController([]);

      // retrieve (empty configuration)
      expect(scope.ldap).not.toBeUndefined();
      expect(scope.ldap.isDirty()).toBeFalsy();
      expect(scope.ldap.id).toBeNull();
      expect(scope.ldap.name).toEqual('');

      // create
      scope.ldap.name = 'config1';

      expect(scope.ldap.isDirty()).toBeTruthy();

      httpBackend
        .expectPOST(SpecUtil.toRegExp(CLMContextLocations.getLdapConfig()))
        .respond(function (method, url, data) {
          return [
            200,
            {
              id: 'id1',
              name: angular.fromJson(data).name,
            },
            {},
          ];
        });

      scope.save();
      expect(scope.saving).toBeTruthy();
      httpBackend.flush();
      expect(scope.saving).toBeFalsy();
      expect($state.go).toHaveBeenCalledWith('edit-ldap.connection', {
        ldapId: 'id1',
      });
    }));

    it('update ldap server', inject(function (CLMContextLocations, $state) {
      initializeController(
        [
          {
            id: '123',
            name: 'config1',
          },
        ],
        '123'
      );

      expect($state.go).toHaveBeenCalledWith('edit-ldap.connection', {
        ldapId: '123',
      });
      $state.go.calls.reset();
      $state.current.name = 'edit-ldap.connection';

      expect(scope.ldap.id).toEqual('123');

      // update
      scope.ldap.name = 'config1changed';

      expect(scope.ldap.isDirty()).toBeTruthy();

      httpBackend
        .expectPUT(SpecUtil.toRegExp(CLMContextLocations.getLdapConfig()))
        .respond(function (method, url, data) {
          var ldapConfig = angular.fromJson(data);
          return [
            200,
            {
              id: ldapConfig.id,
              name: ldapConfig.name,
            },
            {},
          ];
        });

      scope.save();
      expect(scope.saving).toBeTruthy();
      httpBackend.flush();
      expect(scope.saving).toBeFalsy();

      expect(scope.ldap.isDirty()).toBeFalsy();
      expect(scope.ldap.id).toEqual('123');
      expect($state.go).not.toHaveBeenCalled();
    }));

    it('delete ldap server', inject(function (CLMContextLocations, $state) {
      initializeController(
        [
          {
            id: '123',
            name: 'config1',
          },
        ],
        '123'
      );

      expect(
        angular.element('#deleteConfigurationModal').css('display')
      ).toBeUndefined();

      scope.confirmDeleteConfiguration();

      expect(
        angular.element('#deleteConfigurationModal').css('display')
      ).not.toBe('none');

      httpBackend
        .expectDELETE(
          SpecUtil.toRegExp(CLMContextLocations.getLdapConfig() + '/123')
        )
        .respond({});
      scope.deleteConfiguration();
      httpBackend.flush();

      expect(
        angular.element('#deleteConfigurationModal').css('display')
      ).toBeUndefined();

      expect(scope.ldap).toBeNull();
      expect($state.transitionTo).toHaveBeenCalledWith('ldap-servers');
    }));

    describe('cancel', function () {
      it('goes to the LDAP Servers List page', inject(function ($state) {
        initializeController([]);

        scope.cancel();

        expect($state.go).toHaveBeenCalledWith('ldap-servers');
      }));
    });
  });

  describe('LdapConnectionController', function () {
    var httpBackend, state, CLMContextLocations;

    beforeEach(inject(function (
      $httpBackend,
      $rootScope,
      $controller,
      $state,
      _CLMContextLocations_
    ) {
      httpBackend = $httpBackend;
      CLMContextLocations = _CLMContextLocations_;

      $state.current.name = 'edit-ldap.connection';

      scope = $rootScope.$new();
      state = $state;

      scope.ldap = { id: '123' };

      scope.ldapConnectionEditor = {
        'ldap-system-password': {
          $dirty: false,
          $setDirty: function () {
            this.$dirty = true;
          },
          $setPristine: function () {
            this.$dirty = false;
          },
        },
      };

      $controller('LdapConnectionController', {
        $scope: scope,
        $state: state,
      });
    }));

    afterEach(function () {
      httpBackend.verifyNoOutstandingExpectation();
      httpBackend.verifyNoOutstandingRequest();
      scope.$destroy();
    });

    it('create/update/delete ldap connection', inject(function (
      CLMContextLocations
    ) {
      httpBackend
        .expectGET(
          SpecUtil.toRegExp(CLMContextLocations.getLdapConnectionConfig())
        )
        .respond({ serverId: scope.ldap.id });
      httpBackend.flush();

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

      httpBackend
        .expectPUT(
          SpecUtil.toRegExp(CLMContextLocations.getLdapConnectionConfig())
        )
        .respond(function () {
          return [
            200,
            angular.extend(
              {
                id: 'id1',
              },
              angular.copy(scope.ldapConn)
            ),
            {},
          ];
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

      httpBackend
        .expectPUT(
          SpecUtil.toRegExp(CLMContextLocations.getLdapConnectionConfig())
        )
        .respond(function () {
          return [200, angular.copy(scope.ldapConn), {}];
        });
      scope.save();
      expect(scope.saving).toBeTruthy();
      httpBackend.flush();
      expect(scope.saving).toBeFalsy();

      expect(scope.isDirty()).toBeFalsy();
      expect(scope.ldapConn.id).toEqual('id1');
      expect(scope.alerts.length).toEqual(1);
      expect(scope.alerts[0].msg).toEqual('Configuration saved.');
    }));

    it('displays confirmation dialog when navigating away from edited data', function () {
      httpBackend
        .expectGET(
          SpecUtil.toRegExp(CLMContextLocations.getLdapConnectionConfig())
        )
        .respond({ serverId: scope.ldap.id });
      httpBackend.flush();

      scope.ldapConn.username = 'new_name';

      var e = scope.$broadcast('pageChangeStarted');
      expect(e.defaultPrevented).toBeTruthy();

      scope.ldapConnectionEditor = {
        $dirty: true,
        $setPristine: angular.noop,
      };
      spyOn(scope.ldapConnectionEditor, '$setPristine');
      scope.reset();

      dialogScope.discardChanges();

      e = scope.$broadcast('pageChangeStarted');
      expect(e.defaultPrevented).not.toBeTruthy();
      expect(scope.ldapConnectionEditor.$setPristine).toHaveBeenCalled();
    });

    it('test connection', inject(function (CLMContextLocations) {
      httpBackend
        .expectGET(
          SpecUtil.toRegExp(CLMContextLocations.getLdapConnectionConfig())
        )
        .respond({ serverId: scope.ldap.id });
      httpBackend.flush();

      scope.ldapConn.protocol = 'LDAP';
      scope.ldapConn.hostname = 'example.com';
      scope.ldapConn.port = 389;
      scope.ldapConn.authenticationMethod = 'SIMPLE';
      scope.ldapConn.username = 'guest';
      scope.ldapConn.password = 'anon';

      // configuration is good
      httpBackend
        .expectPUT(
          SpecUtil.toRegExp(CLMContextLocations.getLdapConnectionTest())
        )
        .respond(function () {
          return [200, { status: 'OK' }, {}];
        });
      scope.testConnection();
      httpBackend.flush();
      expect(scope.alerts.length).toBe(1);
      expect(scope.alerts[0].type).toBe('success');

      // configuration is bad
      httpBackend
        .expectPUT(
          SpecUtil.toRegExp(CLMContextLocations.getLdapConnectionTest())
        )
        .respond(function () {
          return [200, { status: 'FAILURE', message: 'foo bar' }, {}];
        });
      scope.testConnection();
      httpBackend.flush();
      expect(scope.alerts.length).toBe(1);
      expect(scope.alerts[0].type).toBe('error');
      expect(scope.alerts[0].msg).toBe('foo bar');

      // clm server misbehaves
      httpBackend
        .expectPUT(
          SpecUtil.toRegExp(CLMContextLocations.getLdapConnectionTest())
        )
        .respond(function () {
          return [500, 'foo bar', {}];
        });
      scope.testConnection();
      httpBackend.flush();
      expect(scope.alerts.length).toBe(1);
      expect(scope.alerts[0].type).toBe('error');
      expect(scope.alerts[0].msg).toBe('foo bar');

      // can't connect to clm server
      httpBackend
        .expectPUT(
          SpecUtil.toRegExp(CLMContextLocations.getLdapConnectionTest())
        )
        .respond(function () {
          return [0, '', {}];
        });
      scope.testConnection();
      httpBackend.flush();
      expect(scope.alerts.length).toBe(1);
      expect(scope.alerts[0].type).toBe('error');
      expect(scope.alerts[0].msg).toBe('Unable to reach IQ Server');
    }));

    it('set default protocol port', inject(function () {
      httpBackend
        .expectGET(
          SpecUtil.toRegExp(CLMContextLocations.getLdapConnectionConfig())
        )
        .respond({ serverId: scope.ldap.id });
      httpBackend.flush();

      // empty
      scope.ldapConn.protocol = 'LDAP';
      scope.ldapConn.port = undefined;
      scope.$apply();
      expect(scope.ldapConn.port).toBe(389);
      scope.ldapConn.protocol = 'LDAPS';
      scope.ldapConn.port = undefined;
      scope.$apply();
      expect(scope.ldapConn.port).toBe(636);

      // default
      scope.ldapConn.protocol = 'LDAP';
      scope.ldapConn.port = 636; // old value
      scope.$apply();
      expect(scope.ldapConn.port).toBe(389);
      scope.ldapConn.protocol = 'LDAPS';
      scope.ldapConn.port = 389; // old value
      scope.$apply();
      expect(scope.ldapConn.port).toBe(636);

      // non default non empty is preserved as is
      scope.ldapConn.protocol = 'LDAP';
      scope.ldapConn.port = 1; // old value
      scope.$apply();
      expect(scope.ldapConn.port).toBe(1);
      scope.ldapConn.protocol = 'LDAPS';
      scope.ldapConn.port = 1; // old value
      scope.$apply();
      expect(scope.ldapConn.port).toBe(1);
    }));

    it('does nothing to the password if the form is not ready', inject(function () {
      httpBackend
        .expectGET(
          SpecUtil.toRegExp(CLMContextLocations.getLdapConnectionConfig())
        )
        .respond({ serverId: scope.ldap.id });
      httpBackend.flush();

      scope.ldapConn = undefined;
      scope.clearOrRestorePasswordIfNeeded();
      expect(scope.ldapConn).toBeUndefined();
      expect(
        scope.ldapConnectionEditor['ldap-system-password'].$dirty
      ).toBeFalsy();
    }));

    it('does nothing to the password if it is not an update', inject(function () {
      httpBackend
        .expectGET(
          SpecUtil.toRegExp(CLMContextLocations.getLdapConnectionConfig())
        )
        .respond({ serverId: scope.ldap.id });
      httpBackend.flush();

      scope.ldapConn = {
        hostname: 'hostname',
        port: 389,
        systemPassword: 'password',
      };
      scope.clearOrRestorePasswordIfNeeded();
      expect(scope.ldapConn).toEqual({
        hostname: 'hostname',
        port: 389,
        systemPassword: 'password',
      });
      expect(
        scope.ldapConnectionEditor['ldap-system-password'].$dirty
      ).toBeFalsy();
    }));

    it('does nothing to the password if the user has updated it along with the hostname', inject(function () {
      httpBackend
        .expectGET(
          SpecUtil.toRegExp(CLMContextLocations.getLdapConnectionConfig())
        )
        .respond({
          serverId: scope.ldap.id,
          hostname: 'hostname',
          port: 389,
          systemPassword: '#~FAKE~PASSWORD~#',
        });
      httpBackend.flush();

      scope.ldapConn = {
        hostname: 'hostname1',
        port: 389,
        systemPassword: 'password',
      };
      scope.clearOrRestorePasswordIfNeeded();
      expect(scope.ldapConn).toEqual({
        hostname: 'hostname1',
        port: 389,
        systemPassword: 'password',
      });
      expect(
        scope.ldapConnectionEditor['ldap-system-password'].$dirty
      ).toBeFalsy();
    }));

    it('does nothing to the password if the user has updated it along with the port', inject(function () {
      httpBackend
        .expectGET(
          SpecUtil.toRegExp(CLMContextLocations.getLdapConnectionConfig())
        )
        .respond({
          serverId: scope.ldap.id,
          hostname: 'hostname',
          port: 389,
          systemPassword: '#~FAKE~PASSWORD~#',
        });
      httpBackend.flush();

      scope.ldapConn = {
        hostname: 'hostname',
        port: 3891,
        systemPassword: 'password',
      };
      scope.clearOrRestorePasswordIfNeeded();
      expect(scope.ldapConn).toEqual({
        hostname: 'hostname',
        port: 3891,
        systemPassword: 'password',
      });
      expect(
        scope.ldapConnectionEditor['ldap-system-password'].$dirty
      ).toBeFalsy();
    }));

    it('clears the password if the user has not updated it but has updated the hostname', inject(function () {
      httpBackend
        .expectGET(
          SpecUtil.toRegExp(CLMContextLocations.getLdapConnectionConfig())
        )
        .respond({
          serverId: scope.ldap.id,
          hostname: 'hostname',
          port: 389,
          systemPassword: '#~FAKE~PASSWORD~#',
        });
      httpBackend.flush();

      scope.ldapConn = {
        hostname: 'hostname1',
        port: 389,
        systemPassword: '#~FAKE~PASSWORD~#',
      };
      scope.clearOrRestorePasswordIfNeeded();
      expect(scope.ldapConn).toEqual({
        hostname: 'hostname1',
        port: 389,
        systemPassword: undefined,
      });
      expect(
        scope.ldapConnectionEditor['ldap-system-password'].$dirty
      ).toBeTruthy();
    }));

    it('clears the password if the user has not updated it but has updated the port', inject(function () {
      httpBackend
        .expectGET(
          SpecUtil.toRegExp(CLMContextLocations.getLdapConnectionConfig())
        )
        .respond({
          serverId: scope.ldap.id,
          hostname: 'hostname',
          port: 389,
          systemPassword: '#~FAKE~PASSWORD~#',
        });
      httpBackend.flush();

      scope.ldapConn = {
        hostname: 'hostname',
        port: 3891,
        systemPassword: '#~FAKE~PASSWORD~#',
      };
      scope.clearOrRestorePasswordIfNeeded();
      expect(scope.ldapConn).toEqual({
        hostname: 'hostname',
        port: 3891,
        systemPassword: undefined,
      });
      expect(
        scope.ldapConnectionEditor['ldap-system-password'].$dirty
      ).toBeTruthy();
    }));

    it('restores the password if it is undefined and the host and port are the same', inject(function () {
      httpBackend
        .expectGET(
          SpecUtil.toRegExp(CLMContextLocations.getLdapConnectionConfig())
        )
        .respond({
          serverId: scope.ldap.id,
          hostname: 'hostname',
          port: 389,
          systemPassword: '#~FAKE~PASSWORD~#',
        });
      httpBackend.flush();

      scope.ldapConn = {
        hostname: 'hostname',
        port: 389,
        systemPassword: undefined,
      };
      scope.ldapConnectionEditor['ldap-system-password'].$dirty = true;
      scope.clearOrRestorePasswordIfNeeded();
      expect(scope.ldapConn).toEqual({
        hostname: 'hostname',
        port: 389,
        systemPassword: '#~FAKE~PASSWORD~#',
      });
      expect(
        scope.ldapConnectionEditor['ldap-system-password'].$dirty
      ).toBeFalsy();
    }));

    it('does not show the password needs entry message if the form is not ready', inject(function () {
      httpBackend
        .expectGET(
          SpecUtil.toRegExp(CLMContextLocations.getLdapConnectionConfig())
        )
        .respond({ serverId: scope.ldap.id });
      httpBackend.flush();

      scope.ldapConn = undefined;
      expect(scope.shouldShowPasswordNeedsEntryMessage()).toBeFalsy();
    }));

    it('does not show the password needs entry message if it is not an update', inject(function () {
      httpBackend
        .expectGET(
          SpecUtil.toRegExp(CLMContextLocations.getLdapConnectionConfig())
        )
        .respond({ serverId: scope.ldap.id });
      httpBackend.flush();

      scope.ldapConn = {
        hostname: 'hostname',
        port: 389,
        systemPassword: 'password',
      };
      expect(scope.shouldShowPasswordNeedsEntryMessage()).toBeFalsy();
    }));

    it('does not show the password needs entry message if the hostname and port are the same', inject(function () {
      httpBackend
        .expectGET(
          SpecUtil.toRegExp(CLMContextLocations.getLdapConnectionConfig())
        )
        .respond({
          serverId: scope.ldap.id,
          hostname: 'hostname',
          port: 389,
          systemPassword: '#~FAKE~PASSWORD~#',
        });
      httpBackend.flush();

      scope.ldapConn = {
        hostname: 'hostname',
        port: 389,
        systemPassword: undefined,
      };
      expect(scope.shouldShowPasswordNeedsEntryMessage()).toBeFalsy();
    }));

    it('does not show the password needs entry message if the password and hostname are updated', inject(function () {
      httpBackend
        .expectGET(
          SpecUtil.toRegExp(CLMContextLocations.getLdapConnectionConfig())
        )
        .respond({
          serverId: scope.ldap.id,
          hostname: 'hostname',
          port: 389,
          systemPassword: '#~FAKE~PASSWORD~#',
        });
      httpBackend.flush();

      scope.ldapConn = {
        hostname: 'hostname1',
        port: 389,
        systemPassword: 'password',
      };
      expect(scope.shouldShowPasswordNeedsEntryMessage()).toBeFalsy();
    }));

    it('does not show the password needs entry message if the password and port are updated', inject(function () {
      httpBackend
        .expectGET(
          SpecUtil.toRegExp(CLMContextLocations.getLdapConnectionConfig())
        )
        .respond({
          serverId: scope.ldap.id,
          hostname: 'hostname',
          port: 389,
          systemPassword: '#~FAKE~PASSWORD~#',
        });
      httpBackend.flush();

      scope.ldapConn = {
        hostname: 'hostname',
        port: 3891,
        systemPassword: 'password',
      };
      expect(scope.shouldShowPasswordNeedsEntryMessage()).toBeFalsy();
    }));

    it('shows the password needs entry message if the hostname is updated and the password is undefined', inject(function () {
      httpBackend
        .expectGET(
          SpecUtil.toRegExp(CLMContextLocations.getLdapConnectionConfig())
        )
        .respond({
          serverId: scope.ldap.id,
          hostname: 'hostname',
          port: 389,
          systemPassword: '#~FAKE~PASSWORD~#',
        });
      httpBackend.flush();

      scope.ldapConn = {
        hostname: 'hostname1',
        port: 389,
        systemPassword: undefined,
      };
      expect(scope.shouldShowPasswordNeedsEntryMessage()).toBeTruthy();
    }));

    it('shows the password needs entry message if the port is updated and the password is undefined', inject(function () {
      httpBackend
        .expectGET(
          SpecUtil.toRegExp(CLMContextLocations.getLdapConnectionConfig())
        )
        .respond({
          serverId: scope.ldap.id,
          hostname: 'hostname',
          port: 389,
          systemPassword: '#~FAKE~PASSWORD~#',
        });
      httpBackend.flush();

      scope.ldapConn = {
        hostname: 'hostname',
        port: 3891,
        systemPassword: undefined,
      };
      expect(scope.shouldShowPasswordNeedsEntryMessage()).toBeTruthy();
    }));
  });

  describe('LdapUsermappingController', function () {
    var httpBackend, state;

    beforeEach(inject(function (
      $httpBackend,
      $rootScope,
      $controller,
      $state,
      CLMContextLocations
    ) {
      httpBackend = $httpBackend;

      $state.current.name = 'ldap.usermapping';

      scope = $rootScope.$new();
      state = $state;

      scope.ldap = { id: '123' };

      httpBackend
        .expectGET(
          SpecUtil.toRegExp(CLMContextLocations.getLdapUserMappingConfig())
        )
        .respond({
          serverId: scope.ldap.id,
          userPasswordAttribute: null,
        });

      $controller('LdapUsermappingController', {
        $scope: scope,
        $state: state,
      });

      httpBackend.flush();
    }));

    afterEach(function () {
      httpBackend.verifyNoOutstandingExpectation();
      httpBackend.verifyNoOutstandingRequest();
      scope.$destroy();
    });

    it('create/update/delete ldap user mapping', inject(function (
      CLMContextLocations
    ) {
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
      scope.ldapUserMapping.userMemberOfGroupAttribute =
        'userMemberOfGroupAttribute';

      expect(scope.isDirty()).toBeTruthy();

      httpBackend
        .expectPUT(
          SpecUtil.toRegExp(CLMContextLocations.getLdapUserMappingConfig())
        )
        .respond(function () {
          return [
            200,
            angular.extend(
              {
                id: 'id1',
              },
              angular.copy(scope.ldapUserMapping)
            ),
            {},
          ];
        });
      scope.save();
      expect(scope.saving).toBeTruthy();
      httpBackend.flush();
      expect(scope.saving).toBeFalsy();
      expect(scope.alerts.length).toEqual(1);
      expect(scope.alerts[0].msg).toEqual('Configuration saved.');
      expect(scope.ldapUserMapping.id).toEqual('id1');

      // update

      scope.ldapUserMapping.groupMappingType = 'SIMPLE';

      expect(scope.isDirty()).toBeTruthy();

      httpBackend
        .expectPUT(
          SpecUtil.toRegExp(CLMContextLocations.getLdapUserMappingConfig())
        )
        .respond(function () {
          return [200, angular.copy(scope.ldapUserMapping), {}];
        });
      scope.save();
      expect(scope.saving).toBeTruthy();
      httpBackend.flush();
      expect(scope.saving).toBeFalsy();

      expect(scope.isDirty()).toBeFalsy();
      expect(scope.ldapUserMapping.id).toEqual('id1');
      expect(scope.alerts.length).toEqual(1);
      expect(scope.alerts[0].msg).toEqual('Configuration saved.');
    }));

    it('displays confirmation dialog when navigating away from edited data', function () {
      scope.ldapUserMapping.userBaseDN = 'userBaseDN';

      var e = scope.$broadcast('pageChangeStarted');
      expect(e.defaultPrevented).toBeTruthy();

      scope.ldapUserMappingEditor = {
        $dirty: true,
        $setPristine: angular.noop,
      };
      spyOn(scope.ldapUserMappingEditor, '$setPristine');
      scope.reset();

      dialogScope.discardChanges();

      e = scope.$broadcast('pageChangeStarted');
      expect(e.defaultPrevented).not.toBeTruthy();
      expect(scope.ldapUserMappingEditor.$setPristine).toHaveBeenCalled();
    });
  });
});
