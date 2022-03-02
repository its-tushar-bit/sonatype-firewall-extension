/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from 'MainRoot/owner.manager/owner.manager.module';
import { mapStateToThis } from 'MainRoot/owner.manager/category/category.editor.controller';
import * as orgsAndPoliciesApplicationCategoriesSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesApplicationCategoriesSelectors';

describe('category.editor.controller', function () {
  beforeEach(
    angular.mock.module(ownerManagerModule.name, ($provide) => {
      SpecUtil.mockNgRedux($provide);
    })
  );

  let vm, scope, modalSpy, deleteModalServiceSpy;

  beforeEach(inject((_$rootScope_, $controller) => {
    scope = _$rootScope_.$new();
    modalSpy = jasmine.createSpyObj('Modal', ['open']);
    deleteModalServiceSpy = jasmine.createSpyObj('DeleteModalService', ['deleteRedux']);

    vm = $controller('category.editor.controller', {
      $scope: scope,
      Modal: modalSpy,
      DeleteModalService: deleteModalServiceSpy,
    });

    scope.vm = vm;
    vm.$onInit();
  }));

  describe('mapStateToThis', () => {
    it('maps redux properties to component', () => {
      spyOn(orgsAndPoliciesApplicationCategoriesSelectors, 'selectIsLoading').and.returnValue(false);
      spyOn(orgsAndPoliciesApplicationCategoriesSelectors, 'selectIsDirty').and.returnValue(false);
      spyOn(orgsAndPoliciesApplicationCategoriesSelectors, 'selectIsEditMode').and.returnValue(true);
      spyOn(orgsAndPoliciesApplicationCategoriesSelectors, 'selectLoadError').and.returnValue(null);

      spyOn(orgsAndPoliciesApplicationCategoriesSelectors, 'selectSiblings').and.returnValue(null);
      spyOn(orgsAndPoliciesApplicationCategoriesSelectors, 'selectCurrentCategory').and.returnValue(null);
      spyOn(orgsAndPoliciesApplicationCategoriesSelectors, 'selectTagPolicyList').and.returnValue(null);
      const output = mapStateToThis({});

      expect(output.loading).toBeFalse();
      expect(output.isDirty).toBeFalse();
      expect(output.isEditMode).toBeTrue();
      expect(output.loadError).toBeNull();
      expect(output.siblings).toBeNull();
      expect(output.dirtyCategory).toBeNull();
      expect(output.tagPolicyList).toBeNull();
    });
  });

  describe('$onInit()', () => {
    it('subscribes to the redux store', () => {
      expect(vm.unsubscribe).toBeDefined();
    });

    it('calls loadLabelsEditor', () => {
      expect(vm.loadCategoryEditor).toHaveBeenCalledTimes(1);
    });
  });

  describe('$onDestroy()', () => {
    it('unsubscribes from the redux store', () => {
      expect(vm.unsubscribe).not.toHaveBeenCalled();

      vm.$onDestroy();

      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });

  describe('on pageChangeStarted', () => {
    it('navigates away if form is not dirty', () => {
      vm.isDirty = false;
      SpecUtil.expectStateChangeNotPrevented(scope);
    });

    it('does not navigate away if form is dirty', () => {
      vm.isDirty = true;
      SpecUtil.expectStateChangePrevented(scope);
    });
  });

  describe('save category changes', () => {
    beforeEach(() => {
      vm.saveApplicationCategory = jasmine.createSpy('vm.saveApplicationCategory');
      vm.categoryEditorMask = {
        wrap: jasmine.createSpy('wrap'),
      };
    });

    it('calls saveApplicationCategory', () => {
      vm.save();

      expect(vm.saveApplicationCategory).toHaveBeenCalledTimes(1);
    });
  });

  describe('delete Category', () => {
    beforeEach(() => {
      vm.tagPolicyList = [];
    });

    it('calls Modal.open when no associated policy tag exist', () => {
      vm.tagPolicyList = ['a'];

      vm.deleteCategory();

      expect(modalSpy.open).toHaveBeenCalledTimes(1);
    });

    it('calls DeleteModalService.deleteRedux with default message', () => {
      vm.associatedApplicationNames = [];

      vm.deleteCategory();

      expect(deleteModalServiceSpy.deleteRedux).toHaveBeenCalledTimes(1);
      expect(deleteModalServiceSpy.deleteRedux.calls.mostRecent().args[1]).toBe(
        'Are you sure you want to delete this application category? '
      );
    });

    it('calls DeleteModalService.deleteRedux with enhanced message', () => {
      vm.associatedApplicationNames = ['associatedApplication', 'anotherApplication'];

      vm.deleteCategory();

      expect(deleteModalServiceSpy.deleteRedux).toHaveBeenCalledTimes(1);
      expect(deleteModalServiceSpy.deleteRedux.calls.mostRecent().args[1]).toBe(
        'Are you sure you want to delete this application category? It is in use by the following applications: associatedApplication, anotherApplication.'
      );
    });
  });

  describe('onDescriptionChange', () => {
    it('calls setCategoryDescription', () => {
      vm.dirtyCategory = { description: 'description' };

      expect(vm.setCategoryDescription).not.toHaveBeenCalled();

      vm.onDescriptionChange();

      expect(vm.setCategoryDescription).toHaveBeenCalledOnceWith('description');
    });
  });

  describe('onNameChange', () => {
    it('calls setCategoryName', () => {
      vm.dirtyCategory = { name: 'name' };

      expect(vm.setCategoryDescription).not.toHaveBeenCalled();

      vm.onNameChange();

      expect(vm.setCategoryName).toHaveBeenCalledOnceWith('name');
    });
  });

  describe('onColorChange', () => {
    it('calls setCategoryColor', () => {
      vm.dirtyCategory = { color: 'red' };

      expect(vm.setCategoryDescription).not.toHaveBeenCalled();

      vm.onColorChange();

      expect(vm.setCategoryColor).toHaveBeenCalledOnceWith('red');
    });
  });
});
