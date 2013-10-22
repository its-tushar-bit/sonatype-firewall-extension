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
    var scope = null, parentScope = null;
    
    beforeEach(inject(function ($rootScope) {
      parentScope = $rootScope.$new();
      scope = parentScope.$new();
    }));
    
    afterEach(inject(function ($httpBackend) {
      parentScope.$destroy();
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    }));
    

    it('test it all...ya all of it', inject(function($httpBackend, $controller, $rootScope, CLMAppLocations) {
      var role1 = {
        "roleId": "1da70fae1fd54d6cb7999871ebdb9a36",
        "roleName": "Developer",
        "roleDescription": "Allows to evaluate policies.",
        "membersByOwner": [{
          "ownerId": "bom1-12345678",
          "ownerName": "app",
          "ownerType": "application",
          "members": [{
            "type": "USER",
            "internalName": "admin",
            "displayName": "Admin BuiltIn"
          }, {
            "type": "USER",
            "internalName": "plynch",
            "displayName": "Peter Lynch"
          }]
        }, {
          "ownerId": "58634626a6b747e3b3e585512b682832",
          "ownerName": "test",
          "ownerType": "organization",
          "members": [{
            "type": "USER",
            "internalName": "bfox",
            "displayName": "Brian Fox"
          }, {
            "type": "USER",
            "internalName": "dbradicich",
            "displayName": "Damian Bradicich"
          }, {
            "type": "USER",
            "internalName": "jduggan",
            "displayName": "Jordan Duggan"
          }, {
            "type": "USER",
            "internalName": "jwayman",
            "displayName": "Jeffrey Wayman"
          }, {
            "type": "USER",
            "internalName": "krobinson",
            "displayName": "Kelly Robinson"
          }, {
            "type": "USER",
            "internalName": "mhansen",
            "displayName": "Mike Hansen"
          }, {
            "type": "USER",
            "internalName": "mpiggott",
            "displayName": "Matthew Piggott"
          }, {
            "type": "USER",
            "internalName": "sgleason",
            "displayName": "Sunny Gleason"
          }]
        }]
      }, role2 = {
        "roleId": "1cddabf7fdaa47d6833454af10e0a3ef",
        "roleName": "Owner",
        "roleDescription": "Allows to manage policies.",
        "membersByOwner": [{
          "ownerId": "bom1-12345678",
          "ownerName": "app",
          "ownerType": "application",
          "members": [{
            "type": "USER",
            "internalName": "bfox",
            "displayName": "Brian Fox"
          }, {
            "type": "USER",
            "internalName": "dbradicich",
            "displayName": "Damian Bradicich"
          }, {
            "type": "USER",
            "internalName": "jduggan",
            "displayName": "Jordan Duggan"
          }, {
            "type": "USER",
            "internalName": "jorlina",
            "displayName": "Joel Orlina"
          }]
        }, {
          "ownerId": "58634626a6b747e3b3e585512b682832",
          "ownerName": "test",
          "ownerType": "organization",
          "members": [{
            "type": "USER",
            "internalName": "admin",
            "displayName": "Admin BuiltIn"
          }, {
            "type": "USER",
            "internalName": "jswank",
            "displayName": "Jason Swank"
          }, {
            "type": "USER",
            "internalName": "jwayman",
            "displayName": "Jeffrey Wayman"
          }, {
            "type": "USER",
            "internalName": "mpiggott",
            "displayName": "Matthew Piggott"
          }]
        }]
      };
      
      $httpBackend.expectGET(CLMAppLocations.getRoleMappingUrl()).respond({
        "membersByRole": [role1, role2]
      });
      $controller('AppSecurityController', {
        $scope : scope
      });
      $httpBackend.flush();
      expect(scope.context.roles.length).toEqual(2);
      
      // now do an edit and see what happens!
      scope.editClick(role1);
      expect(scope.context.roleEditMap[role1.roleId]).toEqual(role1);
      
      // now get the list of usernames, make sure what we would expect
      expect(scope.getUserNames(role1)).toEqual('Admin BuiltIn, Peter Lynch');
      expect(scope.getUserNames(role2)).toEqual('Brian Fox, Damian Bradicich, Joel Orlina, Jordan Duggan');
      
      // now get the list of inherited usernames, make sure what we would expect
      expect(scope.getInheritedUserNames(role1)).toEqual('Brian Fox, Damian Bradicich, Jeffrey Wayman, Jordan Duggan, Kelly Robinson, Matthew Piggott, Mike Hansen, Sunny Gleason');
      expect(scope.getInheritedUserNames(role2)).toEqual('Admin BuiltIn, Jason Swank, Jeffrey Wayman, Matthew Piggott');
      
      // now lets fire an event and make sure it gets handled properly
      $rootScope.$broadcast('roleSaveComplete', role1.roleId, {
        members: [{
          "type": "USER",
          "internalName": "test1",
          "displayName": "Test1"
        }, {
          "type": "USER",
          "internalName": "test2",
          "displayName": "Test2"
        }, {
          "type": "USER",
          "internalName": "test3",
          "displayName": "Test3"
        }, {
          "type": "USER",
          "internalName": "test4",
          "displayName": "Test4"
        }]
      });
      
      var found;
      

      for ( var i = 0; i < scope.context.roles.length; i++) {
        if (scope.context.roles[i].roleId === role1.roleId) {
          expect(scope.context.roles[i].membersByOwner[0].members).toEqual([{
            type: 'USER',
            internalName: 'test1',
            displayName: 'Test1'
          }, {
            type: 'USER',
            internalName: 'test2',
            displayName: 'Test2'
          }, {
            type: 'USER',
            internalName: 'test3',
            displayName: 'Test3'
          }, {
            type: 'USER',
            internalName: 'test4',
            displayName: 'Test4'
          }]);
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

    it('Add User', function () {
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
      // TODO Persistence
    });

    it('Remove User', function () {
      scope.$apply(function () {
        scope.removeUser(0);
      });
      expect(parentScope.mappings[0].members).toEqual([]);
      // TODO Persistence
    });

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