describe('AppSecurityController', function() {
  beforeEach(module('ApplicationSecurityModule', 'CLMAppLocation', function ($provide) {
    $provide.value('$modal', {});
    $provide.value('ApplicationId', 'bom1-12345678');
    $provide.value('OrganizationId', '');
  }));

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
          group : false,
          internalName : 'oldlady',
          displayName : 'Old Lady'
        }]
      },{
        ownerId : '862f7a486bff473b9205007595399ffe',
        ownerName : 'Ye Ole Org',
        ownerType : 'organization',
        members : [{
          group : false,
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
        group : false,
        internalName : 'oldlady',
        displayName : 'Old Lady'
      },{
        group : false,
        internalName : 'testuser',
        displayName : 'Fred Flintstone'
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
            group: false,
            internalName: 'fred',
            displayName: 'Fred Flintstone'
          }, {
            group: false,
            internalName: 'barn',
            displayName: 'Barney Rubble'
          }, {
            group: false,
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