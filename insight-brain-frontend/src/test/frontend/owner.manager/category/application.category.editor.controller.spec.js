/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from 'MainRoot/owner.manager/owner.manager.module';
import OwnerUtils from '../owner.utils';
import { mapStateToThis } from 'MainRoot/owner.manager/category/application.category.editor.controller';
import * as applicationSelectors from 'MainRoot/OrgsAndPolicies/applicationsSelectors';
import * as assignApplicationCategorySelectors from 'MainRoot/OrgsAndPolicies/assignApplicationCategoriesSelectors';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';

describe('application.category.editor.controller.spec.js', function () {
  beforeEach(
    angular.mock.module(ownerManagerModule.name, function ($provide) {
      SpecUtil.mockNgRedux($provide);
    })
  );

  function createTests(type) {
    const isApp = type === 'application';
    let scope, vm, controller;
    beforeEach(inject(function ($rootScope, $controller) {
      scope = $rootScope.$new();
      scope.isApp = true;
      controller = $controller;
      vm = $controller('application.category.editor.controller', {
        $scope: scope,
      });
    }));

    if (isApp) {
      describe('on initialization', () => {
        it('subscribes to the redux store', () => {
          expect(scope.unsubscribe).toBeDefined();
        });
        it('calls loadApplicablePolicyMonitoring', () => {
          expect(scope.loadApplications).toHaveBeenCalledTimes(1);
          expect(scope.loadApplicableCategories).toHaveBeenCalledTimes(1);
          expect(scope.loadAppliedCategories).toHaveBeenCalledTimes(1);
        });
        it('does not call loadApplicablePolicyMonitoring', () => {
          scope.isApp = false;
          vm = controller('application.category.editor.controller', {
            $scope: scope,
          });
          expect(scope.loadApplications).not.toHaveBeenCalled();
          expect(scope.loadApplicableCategories).not.toHaveBeenCalled();
          expect(scope.loadAppliedCategories).not.toHaveBeenCalled();
        });
      });

      describe('on $destroy()', () => {
        it('unsubscribes from redux store', () => {
          expect(scope.unsubscribe).not.toHaveBeenCalled();
          scope.$destroy();
          expect(scope.unsubscribe).toHaveBeenCalledTimes(1);
        });
      });

      describe('on save', () => {
        it('calls saveAppliedCategories on save', () => {
          vm.categoryEditorMask = {
            wrap: jasmine.createSpy('wrap'),
          };
          scope.save();
          expect(scope.saveAppliedCategories).toHaveBeenCalledTimes(1);
        });
      });

      describe('on change categories', () => {
        it('calls updateAppliedCategories on categories change', () => {
          scope.onCategoriesChanged();
          expect(scope.updateAppliedCategories).toHaveBeenCalledTimes(1);
        });
      });

      describe('mapStateToThis', () => {
        it('returns an object with the given keys for the state', () => {
          spyOn(applicationSelectors, 'selectOwnerName').and.returnValue('OwnerName');
          spyOn(applicationSelectors, 'selectLoadingApplications').and.returnValue(false);
          spyOn(assignApplicationCategorySelectors, 'selectLoadingApplicableCategories').and.returnValue(false);
          spyOn(assignApplicationCategorySelectors, 'selectLoadingAppliedCategories').and.returnValue(false);
          spyOn(applicationSelectors, 'selectLoadApplicationsError').and.returnValue(null);
          spyOn(assignApplicationCategorySelectors, 'selectLoadApplicableCategoriesError').and.returnValue(null);
          spyOn(assignApplicationCategorySelectors, 'selectLoadAppliedCategoriesError').and.returnValue('some error');
          spyOn(assignApplicationCategorySelectors, 'selectCategories').and.returnValue([
            {
              id: '13dfce231ca24289bec319fddf4bef88',
              organizationId: 'ROOT_ORGANIZATION_ID',
              name: 'Internal',
              description: 'Applications that are used only by your employees',
              color: 'dark-green',
            },
          ]);
          spyOn(assignApplicationCategorySelectors, 'selectIsDirty').and.returnValue(true);
          spyOn(routerSelectors, 'selectIsApplication').and.returnValue(true);
          spyOn(assignApplicationCategorySelectors, 'selectSubmitApplyCategoriesError').and.returnValue(null);

          const { ownerName, loading, loadError, categories, areCategoriesDirty, isApp, submitError } = mapStateToThis(
            {}
          );

          expect(ownerName).toBe('OwnerName');
          expect(loading).toBeFalse();
          expect(loadError).toBe('some error');
          expect(categories).toEqual([
            {
              id: '13dfce231ca24289bec319fddf4bef88',
              organizationId: 'ROOT_ORGANIZATION_ID',
              name: 'Internal',
              description: 'Applications that are used only by your employees',
              color: 'dark-green',
            },
          ]);
          expect(areCategoriesDirty).toBeTrue();
          expect(isApp).toBeTrue();
          expect(submitError).toBeNull();
        });
      });
    }
  }

  OwnerUtils.runTestsForOwnerTypes(createTests);
});
