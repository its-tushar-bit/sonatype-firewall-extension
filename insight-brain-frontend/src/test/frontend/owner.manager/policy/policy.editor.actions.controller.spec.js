/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as policySelectors from 'MainRoot/OrgsAndPolicies/policySelectors';
import * as stagesSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesStagesSelectors';
import ownerManagerModule from 'MainRoot/owner.manager/owner.manager.module';
import { mapStateToThis } from 'MainRoot/owner.manager/policy/policy.editor.actions.controller';

describe('policy.editor.actions.controller', function () {
  beforeEach(
    angular.mock.module(ownerManagerModule.name, function ($provide) {
      $provide.value('$cookies', {
        get: angular.noop,
      });
      SpecUtil.mockNgRedux($provide);
    })
  );

  var vm, $scope;

  beforeEach(inject(function (_$timeout_, _$httpBackend_, $controller, _CLMLocations_, $rootScope) {
    $scope = $rootScope.$new();
    vm = $controller('policy.editor.actions.controller', { $scope }, { actions: [] });
  }));

  describe('mapStateToThis', () => {
    it('sets shouldShowQuarantineWarning, actionStages and loadError to component', () => {
      spyOn(policySelectors, 'selectShouldShowQuarantineWarning').and.returnValue(true);
      spyOn(stagesSelectors, 'selectActionStageTypes').and.returnValue([]);
      spyOn(stagesSelectors, 'selectActionStagesLoadError').and.returnValue('error');

      const { shouldShowQuarantineWarning, actionStages, loadError } = mapStateToThis({});

      expect(shouldShowQuarantineWarning).toBeTrue();
      expect(actionStages).toEqual([]);
      expect(loadError).toEqual('error');
    });
  });

  describe('on create', () => {
    it('subscribes to the redux store', () => {
      expect(vm.unsubscribe).toBeDefined();
    });

    it('calls loadActionStageTypes', () => {
      expect(vm.loadActionStageTypes).toHaveBeenCalledTimes(1);
    });
  });

  describe('$destroy()', () => {
    it('unsubscribes from redux store', () => {
      expect(vm.unsubscribe).not.toHaveBeenCalled();
      $scope.$destroy();
      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });
});
