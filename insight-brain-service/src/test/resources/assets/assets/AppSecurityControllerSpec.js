describe('AppSecurityController', function() {
  beforeEach(module('ApplicationSecurityModule'));

  describe('AppSecurityEditorController', function () {
    var scope = null,
        parentScope = null;

    beforeEach(inject(function ($rootScope, $controller) {
      parentScope = $rootScope.$new();
      scope = parentScope.$new();

      parentScope.users = {
        applied : [],
        inherited : [{
          firstName : 'Old',
          lastName : 'Man'
        }]
      };

      $controller('AppSecurityEditorController', {
        $scope : scope
      });
    }));

    afterEach(function () {
      parentScope.$destroy();
    });

    it('Add User', function () {
      scope.$apply(function () {
        scope.addUser({id : 'bar'});
      });
      expect(parentScope.users.applied).toEqual([{id : 'bar'}]);
      // TODO Persistence
    });

    it('Remove User', function () {
      scope.$apply(function () {
        scope.removeUser({id : 'bar'});
      });
      expect(parentScope.users.applied).toEqual([{id : 'bar'}]);
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
      var x = [{ foo : 'bar' },{ foo : 'xxx' },{ foo : 'zzz' }];

      expect(filter(x, [{ foo : 'bar' }])).toEqual([{ foo : 'xxx' },{ foo : 'zzz' }]);
      // Original array should not have been modified
      expect(x.length).toEqual(3);

      expect(filter(x, x)).toEqual([]);

      expect(filter(x, [])).toEqual(x);
    });
  });
});