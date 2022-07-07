/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from 'MainRoot/owner.manager/owner.manager.module';
import { mapStateToThis } from 'MainRoot/owner.manager/summary/owner.summary.tiles.container.controller';
import * as ownerSummarySelectors from 'MainRoot/OrgsAndPolicies/ownerSummarySelectors';

describe('owner.summary.tiles.container.controller', function () {
  let vm;
  let scope;

  beforeEach(
    angular.mock.module(ownerManagerModule.name, function ($provide) {
      SpecUtil.mockNgRedux($provide);
    })
  );

  beforeEach(inject(function (_$rootScope_, $controller) {
    scope = _$rootScope_.$new();
    vm = $controller('OwnerSummaryTilesContainerController', {
      $scope: scope,
    });
  }));

  describe('on component init', () => {
    it('subscribes to the redux store', () => {
      expect(vm.unsubscribe).toBeDefined();
    });
  });

  describe('on $destroy()', () => {
    it('unsubscribes from redux store', () => {
      expect(vm.unsubscribe).not.toHaveBeenCalled();
      scope.$destroy();
      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });

  describe('mapStateToThis', () => {
    it('sets loading and loadError', () => {
      spyOn(ownerSummarySelectors, 'selectLoading').and.returnValue(true);
      spyOn(ownerSummarySelectors, 'selectLoadError').and.returnValue(null);

      const output = mapStateToThis({});

      expect(output.loading).toBe(true);
      expect(output.loadError).toBe(null);
    });
  });
});
