/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from 'MainRoot/owner.manager/owner.manager.module';
import * as applicationCategoriesSelectors from 'MainRoot/OrgsAndPolicies/createEditApplicationCategoriesSelectors';
import * as orgsAndPoliciesRootSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { mapStateToThis } from 'MainRoot/owner.manager/category/application.category.tile.controller.org';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';

describe('application.category.tile.controller.org.spec.js', function () {
  let vm, scope, EventNameConstant;

  beforeEach(
    angular.mock.module(ownerManagerModule.name, ($provide) => {
      SpecUtil.mockNgRedux($provide);
    })
  );

  beforeEach(inject(function (_$rootScope_, $controller, $injector) {
    scope = _$rootScope_.$new();
    EventNameConstant = $injector.get('event.name.constant');
    vm = $controller('ApplicationCategoryTileControllerOrg', {
      $scope: scope,
    });
  }));

  describe('mapStateToThis', () => {
    it('maps redux properties to component', () => {
      spyOn(applicationCategoriesSelectors, 'selectAppCategoryOwners').and.returnValue(null);
      spyOn(applicationCategoriesSelectors, 'selectLoadError').and.returnValue(null);
      spyOn(applicationCategoriesSelectors, 'selectIsLoading').and.returnValue(false);
      spyOn(orgsAndPoliciesRootSelectors, 'selectOwnerName').and.returnValue(null);
      spyOn(routerSelectors, 'selectIsOrganization').and.returnValue(true);

      const output = mapStateToThis({});

      expect(output.appCategoryOwners).toBeNull();
      expect(output.error).toBeNull();
      expect(output.loading).toBeFalse();
      expect(output.ownerName).toBeNull();
      expect(output.isOrg).toBeTrue();
    });
  });

  describe('on component init', () => {
    it('subscribes to the redux store', () => {
      expect(vm.unsubscribe).toBeDefined();
    });

    it('does not calls loadApplicableCategories if owner is not organization', () => {
      expect(vm.loadApplicableCategories).not.toHaveBeenCalled();
    });
  });

  describe('on $destroy()', () => {
    it('unsubscribes from the redux store', () => {
      expect(vm.unsubscribe).not.toHaveBeenCalled();

      scope.$destroy();

      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });

  describe('editCategory', () => {
    it('calls goToEditCategory if not inherited', () => {
      let inherited = false;
      vm.editCategory('categoryId', inherited);

      expect(vm.goToEditCategory).toHaveBeenCalledOnceWith('categoryId');
    });

    it('does not call goToEditCategory if inherited', () => {
      let inherited = true;
      vm.editCategory('categoryId', inherited);

      expect(vm.goToEditCategory).not.toHaveBeenCalled();
    });
  });

  describe('handles broadcast events', () => {
    it('calls loadApplicableCategories on policy.imported event', () => {
      expect(vm.loadApplicableCategories).not.toHaveBeenCalled();

      vm.isOrg = true;
      scope.$emit(EventNameConstant.POLICY_IMPORTED);

      expect(vm.loadApplicableCategories).toHaveBeenCalledTimes(1);
    });

    it('calls loadApplicableCategories on broadcasted owner summary reload event', () => {
      expect(vm.loadApplicableCategories).not.toHaveBeenCalled();

      vm.isOrg = true;
      scope.$emit(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA);

      expect(vm.loadApplicableCategories).toHaveBeenCalledTimes(1);
    });

    it('calls updateOwnerHandler on broadcasted policy.imported event', () => {
      expect(vm.updateOwnerHandler).not.toHaveBeenCalled();

      scope.$emit(EventNameConstant.OWNER_UPDATED, { name: 'Bob' });

      expect(vm.updateOwnerHandler).toHaveBeenCalledTimes(1);
    });
  });
});
