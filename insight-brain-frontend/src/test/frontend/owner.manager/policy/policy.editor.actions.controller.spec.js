/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from '../../../../main/frontend/owner.manager/owner.manager.module';

describe('policy.editor.actions.controller.spec.js', function () {
  beforeEach(
    angular.mock.module(ownerManagerModule.name, function ($provide) {
      $provide.value('$cookies', {
        get: angular.noop,
      });
      SpecUtil.mockNgRedux($provide);
    })
  );

  var vm, stageTypeStoreDefer, CLMLocations, $httpBackend;

  beforeEach(inject(function (
    $q,
    _$timeout_,
    _$httpBackend_,
    $controller,
    _CLMLocations_,
    StageTypeStore
  ) {
    CLMLocations = _CLMLocations_;
    $httpBackend = _$httpBackend_;

    stageTypeStoreDefer = $q.defer();
    spyOn(stageTypeStoreDefer.promise, 'then').and.callThrough();
    spyOn(StageTypeStore, 'getActionStages').and.returnValue(
      stageTypeStoreDefer.promise
    );
    stageTypeStoreDefer.resolve(MockData.getActionStageData());
    vm = $controller('policy.editor.actions.controller', {}, { actions: [] });
  }));

  it('Properly loads action info', function () {
    $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond([]);
    expect(stageTypeStoreDefer.promise.then).toHaveBeenCalled();
    $httpBackend.flush();

    expect(vm.actionStages.length).toBe(6);
  });
});
