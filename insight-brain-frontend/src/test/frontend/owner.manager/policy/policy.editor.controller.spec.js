/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from 'MainRoot/owner.manager/owner.manager.module';
import { mapStateToThis } from 'MainRoot/owner.manager/policy/policy.editor.controller';
import * as policySelectors from 'MainRoot/OrgsAndPolicies/policySelectors';
import * as rootPoliciesSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';

describe('policy.editor.controller', function () {
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

    vm = $controller('policy.editor.controller', {
      $scope: scope,
      Modal: modalSpy,
      DeleteModalService: deleteModalServiceSpy,
    });

    scope.vm = vm;
  }));

  describe('mapStateToThis', () => {
    it('maps redux properties to component', () => {
      spyOn(policySelectors, 'selectIsCurrentPolicyDirty').and.returnValue(true);
      spyOn(policySelectors, 'selectIsEditMode').and.returnValue(true);
      spyOn(policySelectors, 'selectIsOrgOwner').and.returnValue(true);
      spyOn(policySelectors, 'selectIsRootOrg').and.returnValue(true);
      spyOn(policySelectors, 'selectHasPolicyCategories').and.returnValue(true);
      spyOn(policySelectors, 'selectReadOnly').and.returnValue(true);
      spyOn(policySelectors, 'selectIsGrandfatheringSupported').and.returnValue(true);

      spyOn(policySelectors, 'selectCurrentPolicy').and.returnValue(null);
      spyOn(policySelectors, 'selectCategories').and.returnValue(null);
      spyOn(policySelectors, 'selectLoadError').and.returnValue(null);
      spyOn(policySelectors, 'selectSubmitError').and.returnValue(null);
      spyOn(policySelectors, 'selectSiblings').and.returnValue(null);
      spyOn(policySelectors, 'selectOriginalProxyStageAction').and.returnValue(null);
      spyOn(rootPoliciesSelectors, 'selectOwnerName').and.returnValue(null);

      const output = mapStateToThis({});

      expect(output.isPolicyDirty).toBeTrue();
      expect(output.isEditMode).toBeTrue();
      expect(output.isOrgOwner).toBeTrue();
      expect(output.isRootOrg).toBeTrue();
      expect(output.hasPolicyCategories).toBeTrue();
      expect(output.readOnly).toBeTrue();
      expect(output.isGrandfatheringSupported).toBeTrue();

      expect(output.dirtyPolicy).toBeNull();
      expect(output.categories).toBeNull();
      expect(output.loadError).toBeNull();
      expect(output.submitError).toBeNull();
      expect(output.siblings).toBeNull();
      expect(output.originalProxyStageAction).toBeNull();
      expect(output.ownerName).toBeNull();
    });
  });

  describe('on pageChangeStarted', () => {
    it('navigates away if form is not dirty', () => {
      vm.isPolicyDirty = false;
      SpecUtil.expectStateChangeNotPrevented(scope);
    });

    it('does not navigate away if form is dirty', () => {
      vm.isPolicyDirty = true;
      SpecUtil.expectStateChangePrevented(scope);
    });
  });

  describe('$onInit()', () => {
    it('subscribes to the redux store', () => {
      expect(vm.unsubscribe).toBeDefined();
    });

    it('calls loadPolicyEditor', () => {
      expect(vm.loadPolicyEditor).toHaveBeenCalledTimes(1);
    });
  });

  describe('$destroy()', () => {
    it('unsubscribes from the redux store', () => {
      expect(vm.unsubscribe).not.toHaveBeenCalled();

      scope.$destroy();

      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });

  describe('delete policy', () => {
    it('calls DeleteModalService.deleteRedux', () => {
      vm.dirtyPolicy = { name: 'mockPolicyName' };

      vm.deletePolicy();

      expect(deleteModalServiceSpy.deleteRedux).toHaveBeenCalledTimes(1);
      expect(deleteModalServiceSpy.deleteRedux.calls.mostRecent().args[1]).toBe(
        'You are about to permanently remove mockPolicyName. This action cannot be undone.'
      );
    });
  });

  describe('save policy', () => {
    beforeEach(() => {
      vm.savePolicy = jasmine.createSpy('vm.savePolicy');
      vm.policyEditorMask = {
        wrap: jasmine.createSpy('wrap'),
      };
    });

    it('calls savePolicy', () => {
      vm.save();

      expect(vm.savePolicy).toHaveBeenCalledTimes(1);
    });
  });

  describe('onNameChange', () => {
    it('calls setPolicyName', () => {
      vm.dirtyPolicy = { name: 'name' };

      expect(vm.setPolicyName).not.toHaveBeenCalled();

      vm.onNameChange();

      expect(vm.setPolicyName).toHaveBeenCalledOnceWith('name');
    });
  });

  describe('onThreatLevelChange', () => {
    it('calls setThreatLevel', () => {
      vm.dirtyPolicy = { threatLevel: 5 };

      expect(vm.setThreatLevel).not.toHaveBeenCalled();

      vm.onThreatLevelChange();

      expect(vm.setThreatLevel).toHaveBeenCalledOnceWith(5);
    });
  });

  describe('onCategoriesChanged', () => {
    it('calls toggleCategoryIsApplied', () => {
      vm.categories = [{ id: 'first' }];

      expect(vm.toggleCategoryIsApplied).not.toHaveBeenCalled();

      vm.onCategoriesChanged({ id: 'first' });

      expect(vm.toggleCategoryIsApplied).toHaveBeenCalledOnceWith(0);
    });
  });

  describe('onPolicyViolationGrandfatheringAllowedChange', () => {
    it('calls togglePolicyViolationGrandfatheringAllowed', () => {
      expect(vm.togglePolicyViolationGrandfatheringAllowed).not.toHaveBeenCalled();

      vm.onPolicyViolationGrandfatheringAllowedChange();

      expect(vm.togglePolicyViolationGrandfatheringAllowed).toHaveBeenCalledTimes(1);
    });
  });

  describe('onHasPolicyCategoriesChange', () => {
    it('calls setHasPolicyCategories', () => {
      expect(vm.setHasPolicyCategories).not.toHaveBeenCalled();

      const hasPolicyCategories = true;
      vm.onHasPolicyCategoriesChange(hasPolicyCategories);

      expect(vm.setHasPolicyCategories).toHaveBeenCalledOnceWith(hasPolicyCategories);
    });
  });
});
