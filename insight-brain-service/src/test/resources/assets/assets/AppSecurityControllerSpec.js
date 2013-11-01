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
    

    it('check edit updates the role edit map properly', inject(function() {
      scope.editClick(role1);
      expect(scope.context.roleEditMap[role1.roleId]).toEqual(role1);
    }));
  
    it('check user name lists are ordered as expected', inject(function() {
      expect(scope.context.roleUsers['1da70fae1fd54d6cb7999871ebdb9a36'].applied).toEqual(['Admin BuiltIn', 'Peter Lynch']);
      expect(scope.context.roleUsers['1cddabf7fdaa47d6833454af10e0a3ef'].applied).toEqual(['Brian Fox', 'Damian Bradicich', 'Joel Orlina', 'Jordan Duggan']);
      expect(scope.context.roleUsers['1da70fae1fd54d6cb7999871ebdb9a36'].inherited).toEqual(['Brian Fox', 'Damian Bradicich', 'Jeffrey Wayman', 'Jordan Duggan', 'Kelly Robinson', 'Matthew Piggott', 'Mike Hansen', 'Sunny Gleason']);
      expect(scope.context.roleUsers['1cddabf7fdaa47d6833454af10e0a3ef'].inherited).toEqual(['Admin BuiltIn', 'Jason Swank', 'Jeffrey Wayman', 'Matthew Piggott']);
    }));
    
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

    beforeEach(inject(function ($rootScope, $controller) {
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
          displayName : 'Old Lady'
        }]
      },{
        ownerId : '862f7a486bff473b9205007595399ffe',
        ownerName : 'Ye Ole Org',
        ownerType : 'organization',
        members : [{
          type: "USER",
          internalName : 'oldman',
          displayName : 'Old Man'
        }]
      }];

      $controller('AppSecurityEditorController', {
        $scope : scope
      });
    }));

    afterEach(function () {
      parentScope.$destroy();
    });

    it('Add User', inject(function ($httpBackend, CLMAppLocations) {
      scope.$apply(function () {
        scope.addUser({
          username : 'testuser',
          firstName : 'Fred',
          lastName : 'Flintstone'
        });
      });
      expect(parentScope.mappings[0].members).toEqual([{
        type: "USER",
        internalName : 'testuser',
        displayName : 'Fred Flintstone'
      },{
        type: "USER",
        internalName : 'oldlady',
        displayName : 'Old Lady'
      }]);
      
      $httpBackend.expectPUT(CLMAppLocations.getRoleMappingUrl(scope.roleId), scope.mappings[0].members).respond(204);
      scope.save();
      $httpBackend.flush();
    }));

    it('Remove User', inject(function ($httpBackend, CLMAppLocations) {
      scope.$apply(function () {
        scope.removeUser(0, scope.mappings[0].members[0]);
      });
      expect(parentScope.mappings[0].members).toEqual([]);
      
      $httpBackend.expectPUT(CLMAppLocations.getRoleMappingUrl(scope.roleId), scope.mappings[0].members).respond(204);
      scope.save();
      $httpBackend.flush();
    }));

    // Text used for when showing users
    it('Name', function () {
      expect(scope.getRealname({
        firstName : 'Bob',
        lastName : 'Uruncle'
      })).toEqual('Bob Uruncle');

      expect(scope.getRealname({
        lastName : 'Uruncle'
      })).toEqual('Uruncle');

      expect(scope.getRealname({
        firstName : 'Bob'
      })).toEqual('Bob');
    });

    // Text used for mouseover on users
    it('Tooltip', function () {
      expect(scope.getTooltip({
        firstName : 'Bob',
        lastName : 'Uruncle',
        email : 'bob@example.org'
      })).toEqual('Bob Uruncle <bob@example.org>');

      expect(scope.getTooltip({
        firstName : 'Bob',
        lastName : 'Uruncle'
      })).toEqual('Bob Uruncle');

      expect(scope.getTooltip({
        lastName : 'Uruncle',
        email : 'bob@example.org'
      })).toEqual('Uruncle <bob@example.org>');

      expect(scope.getTooltip({
        firstName : 'Bob',
        email : 'bob@example.org'
      })).toEqual('Bob <bob@example.org>');

      expect(scope.getTooltip({
        firstName : 'Bob',
        lastName : 'Uruncle',
        email : 'bob@example.org',
        realm: 'CLM'
      })).toEqual('Bob Uruncle <bob@example.org> (CLM)');

      expect(scope.getTooltip({
        firstName: 'Bob',
        email: 'bob@example.org',
        realm: 'CLM'
      })).toEqual('Bob <bob@example.org> (CLM)');
    });

    describe('Queries', function () {
      it('Simple', inject(function ($timeout, $httpBackend) {
        scope.$apply(function () {
          scope.queryString = 'bar';
        });

        $httpBackend.expectGET('/rest/user/application/bom1-12345678/query?q=bar').respond([{ id : 'bar' }]);

        $timeout.flush();

        expect(scope.requestActive).toBeTruthy();
        expect(scope.lastQuery).toEqual('bar');
        $httpBackend.flush();

        expect(scope.requestActive).toBeFalsy();
        expect(scope.queryResults).toEqual([{ id : 'bar' }]);
      }));


      it('Query Extended', inject(function ($timeout, $httpBackend) {
        scope.$apply(function () {
          scope.queryString = 'foo';
        });

        $httpBackend.expectGET('/rest/user/application/bom1-12345678/query?q=foo').respond([{ id : 'food' }]);
        $timeout.flush();

        expect(scope.requestActive).toBeTruthy();
        expect(scope.lastQuery).toEqual('foo');

        // User added charactersbefore the server responded
        scope.$apply(function () {
          scope.queryString = 'food';
        });
        $httpBackend.flush();

        expect(scope.lastQuery).toEqual('food');
        expect(scope.queryResults).toEqual([{ id : 'food' }]);
      }));

      it('Query Completely Changed', inject(function ($timeout, $httpBackend) {
        scope.$apply(function () {
          scope.queryString = 'foo';
        });

        $httpBackend.expectGET('/rest/user/application/bom1-12345678/query?q=foo').respond([{ id : 'foo' }]);
        $timeout.flush();

        expect(scope.requestActive).toBeTruthy();
        expect(scope.lastQuery).toEqual('foo');

        // User deleted it and typed something new before the server responded
        scope.$apply(function () {
          scope.queryString = 'bar';
        });
        $httpBackend.flush();

        expect(scope.lastQuery).toEqual('bar');
        expect(scope.queryResults).toBeFalsy();


        $httpBackend.expectGET('/rest/user/application/bom1-12345678/query?q=bar').respond([{ id : 'bar' }]);
        $timeout.flush();
        $httpBackend.flush();
        expect(scope.queryResults).toEqual([{ id : 'bar' }]);
      }));
    });
  });

  describe('userNotIn', function () {
    var filter = null;

    beforeEach(inject(function ($filter) {
      filter = $filter('userNotIn');
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
            username: 'fred',
            firstName: 'Fred',
            lastName : 'Flintstone'
          }, {
            username: 'barn',
            firstName: 'Barney',
            lastName : 'Rubble'
          }, {
            username: 'wilma',
            firstName: 'Wilma',
            lastName : 'Flintstone'
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
        username: 'barn',
        firstName: 'Barney',
        lastName : 'Rubble'
      }, {
        username: 'wilma',
        firstName: 'Wilma',
        lastName : 'Flintstone'
      }]);
      // Original array should not have been modified
      expect(users.length).toEqual(3);

      expect(filter(users, mappings)).toEqual([]);

      expect(filter(users, [{ members : [] }])).toEqual(users);
    });
  });
});