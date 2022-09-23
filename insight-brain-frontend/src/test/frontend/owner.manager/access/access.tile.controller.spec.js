/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from 'MainRoot/owner.manager/owner.manager.module';
import * as orgsAndPoliciesSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as accessSelectors from 'MainRoot/OrgsAndPolicies/access/accessSelectors';
import { mapStateToThis } from 'MainRoot/owner.manager/access/access.tile.controller';

describe('access.tile.controller', function () {
  var vm, $rootScope;

  beforeEach(
    angular.mock.module(ownerManagerModule.name, function ($provide) {
      SpecUtil.mockNgRedux($provide);
      $provide.value('$cookies', {
        get: angular.noop,
      });
    })
  );

  beforeEach(inject(function (_$rootScope_, $controller) {
    $rootScope = _$rootScope_;

    vm = $controller('AccessTileController', {
      $scope: $rootScope.$new(),
    });
  }));

  describe('on $destroy()', () => {
    it('unsubscribes from redux store', () => {
      expect(vm.unsubscribe).not.toHaveBeenCalled();
      $rootScope.$destroy();
      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });

  describe('mapStateToThis', () => {
    it('sets selectedComponent and showCipModal', function () {
      spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwnerName').and.returnValue('selectSelectedOwnerName');
      spyOn(accessSelectors, 'selectRolesWithoutLocalMembersExist').and.returnValue(
        'selectRolesWithoutLocalMembersExist'
      );
      spyOn(accessSelectors, 'selectExtendedMembersByRole').and.returnValue('selectExtendedMembersByRole');
      spyOn(accessSelectors, 'selectLoading').and.returnValue('selectLoading');
      spyOn(accessSelectors, 'selectLoadError').and.returnValue('selectLoadError');
      spyOn(routerSelectors, 'selectIsRepositories').and.returnValue('selectIsRepositories');

      const output = mapStateToThis({});

      expect(output.ownerName).toBe('selectSelectedOwnerName');
      expect(output.rolesWithoutLocalMembersExist).toBe('selectRolesWithoutLocalMembersExist');
      expect(output.ownersWithRoles).toBe('selectExtendedMembersByRole');
      expect(output.loading).toBe('selectLoading');
      expect(output.error).toBe('selectLoadError');
      expect(output.isRepositories).toBe('selectIsRepositories');
    });
  });
});
