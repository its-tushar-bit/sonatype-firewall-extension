/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from '../../../../main/frontend/owner.manager/owner.manager.module';

describe('select.application.contact.controller.spec.js', function () {
  beforeEach(
    angular.mock.module(ownerManagerModule.name, function ($provide) {
      $provide.value('$cookies', {
        get: angular.noop,
      });
    })
  );

  var vm,
    $q,
    scope,
    $timeout,
    $httpBackend,
    deleteServiceResourceDefer,
    mockDeleteService,
    mockOwner = ResourceUtils().createMockResource();

  beforeEach(inject(function ($rootScope, _$q_, _$timeout_, _$httpBackend_) {
    scope = $rootScope.$new();
    $q = _$q_;
    $timeout = _$timeout_;
    deleteServiceResourceDefer = $q.defer();
    mockDeleteService = {
      deleteCustom: function () {
        return deleteServiceResourceDefer.promise;
      },
    };
    $httpBackend = _$httpBackend_;
  }));

  it('Selects current user in search results', function () {
    inject(function ($controller) {
      vm = $controller('select.application.contact.controller', {
        $scope: scope,
        owner: {
          contact: {
            internalName: 'JohnDoe',
          },
        },
      });
    });
    vm.search();
    $httpBackend
      .whenGET('/rest/user/global/global/query?groups=false')
      .respond({
        members: [
          {
            internalName: 'Foo',
          },
          {
            internalName: 'JohnDoe',
          },
        ],
      });
    $httpBackend.flush();
    $timeout.flush();
    expect(vm.selected).toBeDefined();
    expect(vm.selected.internalName).toBe('JohnDoe');
  });

  it('Updates owner with selected contact', function () {
    mockOwner.contact = { internalName: 'John Doe' };
    scope.$close = jasmine.createSpy();
    inject(function ($controller) {
      vm = $controller('select.application.contact.controller', {
        $scope: scope,
        owner: mockOwner,
      });
    });
    vm.selected = { internalName: 'Foo Bar' };
    vm.selectContactFormMask = { wrap: SpecUtil.promiseWrapper($q) };
    vm.updateContact();
    mockOwner.resolveSave();
    $timeout.flush();
    $timeout(function () {}, 1000); // mask delay = 0.8s
    $timeout.flush();
    expect(vm.owner.contactInternalName).toBe('Foo Bar');
    expect(scope.$close).toHaveBeenCalled();
  });

  it('Leaves delete mode when confirmation dialog is cancelled', function () {
    inject(function ($controller) {
      vm = $controller('select.application.contact.controller', {
        $scope: scope,
        owner: {
          contactInternalName: 'Foo',
          contact: {
            displayName: 'Foo Bar',
          },
        },
        DeleteModalService: mockDeleteService,
      });
    });
    vm.removeContact();
    expect(vm.deleteMode).toBe(true);
    deleteServiceResourceDefer.reject();
    $timeout.flush();
    expect(vm.deleteMode).toBe(false);
  });

  it('Checks for dirty state', function () {
    inject(function ($controller) {
      vm = $controller('select.application.contact.controller', {
        $scope: scope,
        owner: {},
      });
    });
    vm.owner.contact = null;
    vm.selected = undefined;
    expect(vm.isDirty()).toBe(false);
    vm.selected = { internalName: 'Foo' };
    expect(vm.isDirty()).toBe(true);
    vm.owner.contact = { internalName: 'Foo' };
    expect(vm.isDirty()).toBe(false);
    vm.owner.contact = { internalName: 'Bar' };
    expect(vm.isDirty()).toBe(true);
  });

  describe('Page Changes', function () {
    beforeEach(inject(function ($controller) {
      vm = $controller('select.application.contact.controller', {
        $scope: scope,
        owner: {},
      });
    }));

    it('clean', function () {
      spyOn(vm, 'isDirty').and.returnValue(false);

      SpecUtil.expectStateChangeNotPrevented(scope);
      expect(vm.unsavedModalVisible).toBeFalsy();
      expect(vm.isDirty).toHaveBeenCalled();
    });

    it('dirty', function () {
      spyOn(vm, 'isDirty').and.returnValue(true);

      SpecUtil.expectStateChangePrevented(scope);
      expect(vm.unsavedModalVisible).toBeTruthy();
      expect(vm.isDirty).toHaveBeenCalled();
    });

    it('Closes', function () {
      scope.$dismiss = jasmine.createSpy();

      scope.$broadcast('pageChangeAccepted');
      expect(scope.$dismiss).toHaveBeenCalled();
    });
  });
});
