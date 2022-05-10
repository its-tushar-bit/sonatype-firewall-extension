/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from '../../../../main/frontend/owner.manager/owner.manager.module';
import legacyConfigurationModule from '../../../../main/frontend/LegacyConfigurationModule';
import { actions as applicationActions } from 'MainRoot/OrgsAndPolicies/applicationsSlice';

describe('change.application.id.controller', function () {
  beforeEach(
    angular.mock.module(ownerManagerModule.name, legacyConfigurationModule.name, function ($provide) {
      SpecUtil.mockNgRedux($provide);
    })
  );

  var vm,
    $rootScope,
    scope,
    mockState,
    app = { id: null, name: null };

  beforeEach(inject(function (_$rootScope_, $controller, $q) {
    mockState = jasmine.createSpyObj('state', ['go']);
    $rootScope = _$rootScope_;
    scope = $rootScope.$new();
    spyOn($rootScope, '$broadcast').and.callThrough();
    scope.$close = jasmine.createSpy('$close');

    spyOn(applicationActions, 'updateApplication').and.returnValue($q.resolve({}));
    vm = $controller('change.application.id.controller', {
      $scope: scope,
      $rootScope: $rootScope,
      $state: mockState,
      owner: app,
      siblings: [],
    });

    vm.applicationIdEditorMask = { wrap: SpecUtil.promiseWrapper($q) };
  }));

  afterEach(function () {
    scope.$destroy();
  });

  describe('on component init', () => {
    it('subscribes to the redux store', () => {
      expect(vm.unsubscribe).toBeDefined();
    });
  });

  describe('on $destroy()', () => {
    it('unsubscribes from the redux store', () => {
      expect(vm.unsubscribe).not.toHaveBeenCalled();
      scope.$destroy();
      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });

  it('Cleans up after save', function () {
    vm.dirtyApp.publicId = 'newId';
    vm.changeApplicationId();
    scope.$apply();

    expect(scope.$close).toHaveBeenCalled();
    expect($rootScope.$broadcast).toHaveBeenCalledWith(
      'reload.owner.tree.data',
      { ...app, publicId: 'newId' },
      'application',
      false
    );
    expect(mockState.go).toHaveBeenCalledWith('management.view.application', {
      applicationPublicId: 'newId',
    });
  });

  it('Checks dirty state', function () {
    expect(vm.isDirty()).toBe(false);
    vm.dirtyApp.publicId = 'foo';
    expect(vm.isDirty()).toBe(true);
    vm.originalApp.publicId = 'foo';
    expect(vm.isDirty()).toBe(false);
  });
});
