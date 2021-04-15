/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import roleMembershipModule from '../../../main/frontend/role.membership/role.membership.module';
import accessMockData from '../stores/access/access.mock.data';

describe('role.membership.controller.spec.js', function () {
  var vm, scope, $httpBackend, $rootScope, $q, CLMContextLocations;

  beforeEach(angular.mock.module(roleMembershipModule.name));

  beforeEach(inject(function (_$rootScope_, _$httpBackend_, _$q_, _CLMContextLocations_) {
    $httpBackend = _$httpBackend_;
    $rootScope = _$rootScope_;
    $q = _$q_;
    CLMContextLocations = _CLMContextLocations_;
  }));

  afterEach(function () {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();

    if (scope) {
      scope.$destroy();
      scope = null;
    }
  });

  /**
   * Create the controller and bind the specified controller props to the controller itself before it's
   * constructor function runs
   */
  function createController(controllerProps) {
    inject(function ($controller, $rootScope) {
      scope = $rootScope.$new();

      vm = $controller('role.membership.controller', { $scope: scope }, controllerProps);
      scope.vm = vm;

      vm.accessEditorSearch = {
        $setPristine: jasmine.createSpy('$setPristine'),
      };
      vm.accessEditor = {
        $setPristine: jasmine.createSpy('$setPristine'),
      };
      vm.accessEditorAddGroup = {
        $setPristine: jasmine.createSpy('$setPristine'),
      };

      $rootScope.$digest();
    });
  }

  it('sets vm.members', function () {
    createController({
      originalMembers: accessMockData.getMoreRoleMappings().membersByRole[0].membersByOwner[0].members,
    });

    expect(vm.members.length).toBe(2);
    vm.members.forEach(function (member) {
      expect(member.picked).toBe(true);
    });
  });

  it('updates vm.members when vm.originalMembers is set to a different object', function () {
    createController({
      originalMembers: [],
    });

    expect(vm.members.length).toBe(0);

    vm.originalMembers = accessMockData.getMoreRoleMappings().membersByRole[0].membersByOwner[0].members;
    $rootScope.$digest();

    expect(vm.members.length).toBe(2);
    vm.members.forEach(function (member) {
      expect(member.picked).toBe(true);
    });
  });

  describe('search', function () {
    beforeEach(function () {
      createController({
        originalMembers: accessMockData.getMoreRoleMappings().membersByRole[0].membersByOwner[0].members,
      });

      vm.accessEditorSearchMask = { wrap: SpecUtil.promiseWrapper($q) };
      vm.query = 'testSearch';
    });

    function doSearch() {
      vm.search();

      $httpBackend
        .expectGET(CLMContextLocations.getFindUsersUrl() + '?q=testSearch')
        .respond(accessMockData.getQueryResults());
    }

    it('sets search in progress flag', function () {
      expect(vm.searchInProgress).toBeFalsy();

      doSearch();

      expect(vm.searchInProgress).toBeTruthy();
      $httpBackend.flush();
      expect(vm.searchInProgress).toBeFalsy();
    });

    it('adds search users to the members array without re-adding or unpicking pre-existing picked members', function () {
      vm.members.some(function (member) {
        if (member.internalName === 'userTest1') {
          member.picked = true;
          return true;
        }
      });

      doSearch();
      $httpBackend.flush();

      //would be 6 if the duplicate detection wasn't working
      expect(vm.members.length).toBe(5);

      //wouldn't be true if the members list was just wiped out and reset from search results
      var userTest1Exists = vm.members.some(function (member) {
        if (member.internalName === 'userTest1') {
          expect(member.picked).toBe(true);
          return true;
        }
      });
      expect(userTest1Exists).toBe(true);
    });
  });

  it('creates correct tooltip message', function () {
    createController();

    expect(vm.getTooltip({ realm: 'foo' })).toBe('foo');
    expect(vm.getTooltip({ realm: 'foo', email: 'test@test.com' })).toBe('foo\ntest@test.com');
    // existing LDAP entry but connection is down so no realm/email
    expect(vm.getTooltip({ displayName: 'test' })).toBe(null);
  });

  describe('typical cases', function () {
    beforeEach(function () {
      createController();
    });

    it('correctly determines whether a group exists', function () {
      expect(vm.groupExists('foo')).toBeFalsy();
      vm.newGroupName = 'foo';
      vm.addGroup();

      expect(vm.groupExists('bar')).toBeFalsy();
      expect(vm.groupExists('foo')).toBeTruthy();
    });

    it('adds an added group to the list of members', function () {
      vm.newGroupName = 'foo';
      vm.addGroup();

      expect(vm.members).toEqual([
        {
          displayName: 'foo',
          email: null,
          internalName: 'foo',
          type: 'GROUP',
        },
      ]);

      expect(vm.accessEditorAddGroup.$setPristine).toHaveBeenCalled();
    });
  });

  describe('makeEditorPristine', function () {
    beforeEach(function () {
      createController();
    });

    it('deletes newGroupName, query, and searchError', function () {
      vm.newGroupName = 'test';
      vm.query = 'test';
      vm.searchError = 'test';

      vm.makeEditorPristine();

      expect(vm.newGroupName).toBeUndefined();
      expect(vm.query).toBeUndefined();
      expect(vm.searchError).toBeUndefined();
    });

    it('calls $setPristine on the accessEditor, accessEditorSearch, and accessEditorAddGroup', function () {
      vm.makeEditorPristine();

      expect(vm.accessEditor.$setPristine).toHaveBeenCalled();
      expect(vm.accessEditorSearch.$setPristine).toHaveBeenCalled();
      expect(vm.accessEditorAddGroup.$setPristine).toHaveBeenCalled();
    });
  });

  it('sets isDirty when the currentMembers have a different set of names from the originalMembers', function () {
    var originalMembers = accessMockData.getMoreRoleMappings().membersByRole[0].membersByOwner[0].members;

    createController({ originalMembers: originalMembers });

    vm.members = originalMembers.map(function (member) {
      var copy = angular.copy(member);
      copy.picked = true;
      return copy;
    });
    expect(vm.isDirty()).toBe(false);

    vm.members[0].picked = false;
    expect(vm.isDirty()).toBe(true);

    vm.members.shift();
    expect(vm.isDirty()).toBe(true);

    vm.members.unshift(originalMembers[0]);
    vm.members[0].picked = true;
    expect(vm.isDirty()).toBe(false);
  });

  describe('getCurrentMembers', function () {
    beforeEach(function () {
      createController();
    });

    it('returns an empty list if the members is empty or undefined', function () {
      vm.members = undefined;
      expect(vm.getCurrentMembers()).toEqual([]);

      vm.members = [];
      expect(vm.getCurrentMembers()).toEqual([]);
    });

    it('returns the members who have their `picked` property set to true', function () {
      var members = [
        {
          picked: true,
        },
        {
          picked: false,
        },
        {
          picked: true,
        },
      ];
      vm.members = members.slice();

      expect(vm.getCurrentMembers().length).toBe(2);
      expect(vm.getCurrentMembers()[0]).toBe(members[0]);
      expect(vm.getCurrentMembers()[1]).toBe(members[2]);

      members = [
        {
          picked: false,
        },
        {
          picked: false,
        },
        {
          picked: false,
        },
      ];
      vm.members = members.slice();

      expect(vm.getCurrentMembers()).toEqual([]);
    });
  });

  describe('getIconName', function () {
    var mapping = {
      USER: 'fa-user',
      GROUP: 'fa-group',
    };

    beforeEach(function () {
      createController();
    });

    Object.keys(mapping).forEach(function (type) {
      it('returns ' + mapping[type] + ' if the item has a ' + type + ' type', function () {
        expect(vm.getIconName({ type: type })).toBe(mapping[type]);
      });
    });
  });
});
