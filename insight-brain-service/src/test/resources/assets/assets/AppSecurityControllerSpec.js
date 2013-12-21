describe('AppSecurityControllerSpec', function() {
  beforeEach(module('ApplicationSecurityModule', 'CLMAppLocation', function ($provide) {
    $provide.value('$modal', {});
    $provide.value('OrganizationId', '');
    $provide.value('ApplicationId', {
      encoded: function(){ 
        return 'bom1-12345678'; 
      }
    });
    $provide.value('$state', {
      current: {
        name: 'application'
      }
    });
  }));

  describe('AppSecurityController', function(){
    var scope = null, parentScope = null, role1 = null, role2 = null;

    beforeEach(inject(function ($rootScope, $httpBackend, CLMAppLocations, $controller) {
      parentScope = $rootScope.$new();
      scope = parentScope.$new();
      role1 = MockData.getRoleOneData();
      role2 = MockData.getRoleTwoData();

      $httpBackend.expectGET(SpecUtil.toRegExp(CLMAppLocations.getRoleMappingUrl())).respond({
        "membersByRole": [role1, role2]
      });
      $controller('AppSecurityController', {
        $scope : scope
      });
      $httpBackend.flush();
      expect(scope.context.roles.length).toEqual(2);
    }));

    afterEach(inject(function ($httpBackend) {
      parentScope.$destroy();
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    }));

    it('data is loaded into scope', function() {
      expect(scope.context.roles.length).toBe(2);
      expect(scope.context.roles[0].roleId).toBe('1da70fae1fd54d6cb7999871ebdb9a36');
      expect(scope.context.roles[0].membersByOwner.length).toBe(2);
      expect(scope.context.roles[0].membersByOwner[0].ownerId).toBe('bom1-12345678');
      expect(scope.context.roles[0].membersByOwner[0].members.length).toBe(2);
      expect(scope.context.roles[0].membersByOwner[0].members[0].internalName).toBe('admin');
    });

    it('validate roleSaveComplete event is handled properly', inject(function($rootScope) {
      $rootScope.$broadcast('roleSaveComplete', role1.roleId, {
        members: MockData.getRoleSaveCompleteEventMemberList()
      });

      var found;

      for ( var i = 0; i < scope.context.roles.length; i++) {
        if (scope.context.roles[i].roleId === role1.roleId) {
          expect(scope.context.roles[i].membersByOwner[0].members).toEqual(MockData.getRoleSaveCompleteEventMemberList());
          found = true;
          break;
        }
      }

      expect(found).toEqual(true);
    }));
  });

  describe('AppSecurityEditorController', function () {
    var scope = null,
        parentScope = null;

    beforeEach(inject(function ($rootScope, $controller, $compile) {
      parentScope = $rootScope.$new();
      scope = parentScope.$new();
      scope.isDirty = function() {
        return true;
      };
      scope.hide = function() {
      };

      parentScope.mappings = [{
        ownerId : 'bom1-12345678',
        ownerName : 'Hal 9000',
        ownerType : 'application',
        members : [{
          type: "USER",
          internalName : 'oldlady',
          displayName : 'Old Lady',
          email : 'oldlady@foo.com',
          realm : 'old'
        }, {
          type : 'GROUP',
          internalName : 'oldladiesgroup',
          displayName : 'Old Ladies Group',
          realm : 'old'
        }]
      },{
        ownerId : '862f7a486bff473b9205007595399ffe',
        ownerName : 'Ye Ole Org',
        ownerType : 'organization',
        members : [{
          type: "USER",
          internalName : 'oldman',
          displayName : 'Old Man',
          email : 'oldman@foo.com',
          realm : 'old'
        }, {
          type : 'GROUP',
          internalName : 'oldmengroup',
          displayName : 'Old Men Group',
          realm : 'old'
        }]
      }];

      $controller('AppSecurityEditorController', {
        $scope : scope
      });

      $compile("<div app-user-search set-results='setResults($members, $error)' query-string='queryString' request-active='requestActive'></div>")(scope);
    }));

    afterEach(function () {
      parentScope.$destroy();
    });

    it('Add User and Group', inject(function ($httpBackend, CLMAppLocations) {
      scope.$apply(function () {
        scope.addMember({
          type: 'USER',
          internalName : 'testuser',
          displayName : 'Fred Flintstone',
          email : 'fred@flinstone.com',
          realm : 'bedrock'
        });
      });
      expect(parentScope.mappings[0].members).toEqual([{
        type: "USER",
        internalName : 'oldlady',
        displayName : 'Old Lady',
        email : 'oldlady@foo.com',
        realm : 'old'
      },{
        type : 'GROUP',
        internalName : 'oldladiesgroup',
        displayName : 'Old Ladies Group',
        realm : 'old'
      },{
        type: "USER",
        internalName : 'testuser',
        displayName : 'Fred Flintstone',
        email : "fred@flinstone.com",
        realm : "bedrock"
      }]);

      scope.$apply(function () {
        scope.addMember({
          type: 'GROUP',
          internalName : 'finstones',
          displayName : 'Flintstone Family',
          realm : 'bedrock'
        });
      });
      expect(parentScope.mappings[0].members).toEqual([{
        type: "USER",
        internalName : 'oldlady',
        displayName : 'Old Lady',
        email : 'oldlady@foo.com',
        realm : 'old'
      },{
        type : 'GROUP',
        internalName : 'oldladiesgroup',
        displayName : 'Old Ladies Group',
        realm : 'old'
      },{
        type: "USER",
        internalName : 'testuser',
        displayName : 'Fred Flintstone',
        email : "fred@flinstone.com",
        realm : "bedrock"
      },{
        type: 'GROUP',
        internalName : 'finstones',
        displayName : 'Flintstone Family',
        realm : 'bedrock'
      }]);

      $httpBackend.expectPUT(CLMAppLocations.getRoleMappingUrl(scope.roleId), scope.mappings[0].members).respond(204);
      scope.save();
      $httpBackend.flush();
    }));

    it('Remove User', inject(function ($httpBackend, CLMAppLocations) {
      scope.$apply(function () {
        scope.removeMember(0, scope.mappings[0].members[0]);
      });
      expect(parentScope.mappings[0].members).toEqual([{
        type : 'GROUP',
        internalName : 'oldladiesgroup',
        displayName : 'Old Ladies Group',
        realm : 'old'
      }]);
      scope.$apply(function () {
        scope.removeMember(0, scope.mappings[0].members[0]);
      });
      expect(parentScope.mappings[0].members).toEqual([]);

      $httpBackend.expectPUT(CLMAppLocations.getRoleMappingUrl(scope.roleId), scope.mappings[0].members).respond(204);
      scope.save();
      $httpBackend.flush();
    }));

    describe('Queries', function () {
      it('Simple', inject(function ($timeout, $httpBackend) {
        scope.$apply(function () {
          scope.queryString = 'bar';
        });

        $httpBackend.expectGET('/rest/user/application/bom1-12345678/query?q=bar').respond({ members: [{ id : 'bar' }], error: null });

        $timeout.flush();

        expect(scope.requestActive).toBeTruthy();
        expect(scope.$$childHead.lastQuery).toEqual('bar');
        $httpBackend.flush();

        expect(scope.requestActive).toBeFalsy();
        expect(scope.queryResults).toEqual([{ id : 'bar' }]);
      }));

      it('Query Extended', inject(function ($timeout, $httpBackend) {
        scope.$apply(function () {
          scope.queryString = 'foo';
        });

        $httpBackend.expectGET('/rest/user/application/bom1-12345678/query?q=foo').respond({ members: [{ id : 'food' }], error: null });
        $timeout.flush();

        expect(scope.requestActive).toBeTruthy();
        expect(scope.$$childHead.lastQuery).toEqual('foo');

        // User added charactersbefore the server responded
        scope.$apply(function () {
          scope.queryString = 'food';
        });
        $httpBackend.flush();

        expect(scope.$$childHead.lastQuery).toEqual('food');
        expect(scope.queryResults).toEqual([{ id : 'food' }]);
      }));

      it('Query Completely Changed', inject(function ($timeout, $httpBackend) {
        scope.$apply(function () {
          scope.queryString = 'foo';
        });

        $httpBackend.expectGET('/rest/user/application/bom1-12345678/query?q=foo').respond({ members: [{ id : 'foo' }], error: null });
        $timeout.flush();

        expect(scope.requestActive).toBeTruthy();
        expect(scope.$$childHead.lastQuery).toEqual('foo');

        // User deleted it and typed something new before the server responded
        scope.$apply(function () {
          scope.queryString = 'bar';
        });
        $httpBackend.flush();

        expect(scope.$$childHead.lastQuery).toEqual('bar');
        expect(scope.queryResults).toBeFalsy();

        $httpBackend.expectGET('/rest/user/application/bom1-12345678/query?q=bar').respond({ members: [{ id : 'bar' }], error: null });
        $timeout.flush();
        $httpBackend.flush();
        expect(scope.queryResults).toEqual([{ id : 'bar' }]);
      }));

      it('Handles Ldap Connection Failure', inject(function($timeout, $httpBackend) {
        scope.$apply(function () {
          scope.queryString = 'foo';
        });

        $httpBackend.expectGET('/rest/user/application/bom1-12345678/query?q=foo').respond({ members: [{ id : 'foo' }], error: '' });

        $timeout.flush();

        expect(scope.requestActive).toBeTruthy();
        expect(scope.$$childHead.lastQuery).toEqual('foo');
        $httpBackend.flush();

        expect(scope.requestActive).toBeFalsy();
        expect(scope.queryResults).toEqual([{ id : 'foo' }]);
        expect(scope.alerts.length).toEqual(0);

        scope.$apply(function () {
          scope.queryString = 'bar';
        });

        $httpBackend.expectGET('/rest/user/application/bom1-12345678/query?q=bar').respond({ members: [{ id : 'bar' }], error: 'baz' });

        $timeout.flush();

        expect(scope.requestActive).toBeTruthy();
        expect(scope.$$childHead.lastQuery).toEqual('bar');
        $httpBackend.flush();

        expect(scope.requestActive).toBeFalsy();
        expect(scope.queryResults).toEqual([{ id : 'bar' }]);
        expect(scope.alerts).toEqual([{
          type: 'error',
          msg: 'baz'
        }]);
      }));

      it('conditionally shows user and group headers', function() {
        var group = { type: 'GROUP' };
        expect(scope.showGroupingHeader(group, null)).toBe(false);

        var mapping = [{
          members: [{
            type: 'USER'
          }]
        }];
        expect(scope.showGroupingHeader(group, mapping)).toBe(false);

        mapping[0].members.push({ type: 'GROUP' });
        expect(scope.showGroupingHeader(group, mapping)).toBe(true);

        var members = [{ type: 'USER' }];
        expect(scope.showGroupingHeader(group, members)).toBe(false);

        members.push({ type: 'GROUP' });
        expect(scope.showGroupingHeader(group, members)).toBe(true);
      });
    });
  });

  describe('memberNotIn', function () {
    var filter = null;

    beforeEach(inject(function ($filter) {
      filter = $filter('memberNotIn');
    }));

    afterEach(function () {
      filter = null;
    });

    it('Non-values', function () {
      expect(filter(undefined, [])).toBeUndefined();
      expect(filter(null, [])).toEqual(null);
    });

    it('Missing Argument', function () {
      expect(filter([{ foo : 'bar' }], null)).toEqual([{ foo : 'bar' }]);
      expect(filter([{ foo : 'bar' }], undefined)).toEqual([{ foo : 'bar' }]);
    });

    it('Test', function () {
      var users = [{
            internalName: 'fred',
            displayName: 'Fred Flintstone'
          }, {
            internalName: 'barn',
            displayName: 'Barney Rubble'
          }, {
            internalName: 'wilma',
            displayName: 'Wilma Flintstone'
          }],
        mappings = [{
          members: [{
            type: "USER",
            internalName: 'fred',
            displayName: 'Fred Flintstone'
          }, {
            type: "USER",
            internalName: 'barn',
            displayName: 'Barney Rubble'
          }, {
            type: "USER",
            internalName: 'wilma',
            displayName: 'Wilma Flintstone'
          }]
        }];

      expect(filter(users, [{
        members: [{
          internalName: 'fred',
          displayName: 'Fred Flintstone'
        }]
      }])).toEqual([{
        internalName: 'barn',
        displayName: 'Barney Rubble'
      }, {
        internalName: 'wilma',
        displayName: 'Wilma Flintstone'
      }]);
      // Original array should not have been modified
      expect(users.length).toEqual(3);

      expect(filter(users, mappings)).toEqual([]);

      expect(filter(users, [{ members : [] }])).toEqual(users);
    });
  });
});