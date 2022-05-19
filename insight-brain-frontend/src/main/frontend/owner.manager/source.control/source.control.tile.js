/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { find, propEq } from 'ramda';
import { unwrapResult } from '@reduxjs/toolkit';
import { actions } from 'MainRoot/productFeatures/productFeaturesSlice';
import { selectIsSourceControlForSourceTileSupported } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectSelectedOwnerName } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { actions as applicationActions } from 'MainRoot/OrgsAndPolicies/applicationsSlice';
import { actions as organizationsActions } from 'MainRoot/OrgsAndPolicies/organizationsSlice';

import template from './source.control.tile.html';
import { valueFromHierarchy } from '../../configuration/scmOnboarding/utils/providers';

export default {
  template: template,
  controllerAs: 'vm',
  controller: SourceControlTileController,
};

function SourceControlTileController(
  $scope,
  SameOwnerStateNavigationService,
  EventNameConstant,
  CLMContextLocations,
  OrganizationStore,
  ApplicationStore,
  $q,
  Messages,
  SourceControlService,
  $ngRedux
) {
  var vm = this;

  vm.ownerType = undefined;
  vm.loading = false;
  vm.error = undefined;
  vm.isApp = CLMContextLocations.isApplication();
  vm.isOrg = CLMContextLocations.isOrganization();
  vm.isRootOrg = CLMContextLocations.isRootOrg();
  vm.doLoad = doLoad;
  vm.editSourceControl = editSourceControl;
  vm.sourceControl = undefined;
  vm.providerTypesMap = SourceControlService.getProviderTypesMap();
  vm.itemText = undefined;
  vm.itemSubText = undefined;
  vm.effectiveProvider = undefined;

  vm.unsubscribe = $ngRedux.connect(mapStateToThis, {
    loadProductFeatures: actions.fetchProductFeaturesIfNeeded,
    loadApplications: applicationActions.loadApplications,
    loadOrganizations: organizationsActions.loadOrganizations,
  })(vm);

  vm.doLoad();

  $scope.$on(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA, function () {
    doLoad();
  });

  $scope.$on('$destroy', function () {
    vm.unsubscribe();
  });

  function doLoad() {
    vm.error = undefined;
    vm.loading = true;

    let ownerPromise;

    if (vm.isApp) {
      ownerPromise = vm.loadApplications();
      vm.ownerType = 'application';
    } else if (vm.isOrg) {
      ownerPromise = vm.loadOrganizations();
      vm.ownerType = 'organization';
    }

    if (ownerPromise !== undefined) {
      const promises = [ownerPromise, vm.loadProductFeatures()];

      $q.all(promises)
        .then(function (results) {
          const siblings = unwrapResult(results[0]);
          unwrapResult(results[1]);

          const entityId = CLMContextLocations.getEntityId();
          const owner = find(propEq(vm.isApp ? 'publicId' : 'id', entityId))(siblings);

          if (vm.isSourceControlSupported) {
            return getSourceControl(owner.id);
          }
        })
        .catch(function (e) {
          vm.error = Messages.getHttpErrorMessage(e);
        })
        .finally(function () {
          vm.loading = false;
        });
    }
  }

  function getSourceControl(ownerInternalId) {
    return SourceControlService.getCompositeSourceControlRecord(vm.ownerType, ownerInternalId).then(function (result) {
      vm.sourceControl = typeof result !== 'undefined' && result !== null ? result : undefined;
      if (vm.sourceControl !== undefined) {
        vm.effectiveProvider = effectiveProvider();
        vm.itemText = getItemText();
        vm.itemSubText = getItemSubText();
      }
    });
  }

  function editSourceControl() {
    SameOwnerStateNavigationService.goEdit('edit-source-control');
  }

  function effectiveProvider() {
    return !vm.sourceControl ? null : valueFromHierarchy(vm.sourceControl.provider);
  }

  function getItemText() {
    let text = '';
    if (vm.sourceControl && vm.effectiveProvider) {
      if (vm.isOrg) {
        text = vm.providerTypesMap[vm.effectiveProvider];
      } else {
        text = vm.sourceControl.repositoryUrl ? vm.sourceControl.repositoryUrl : 'Repository URL needed';
      }
    }
    return text;
  }

  function getItemSubText() {
    let text,
      token = vm.sourceControl.token.value,
      parentValue = vm.sourceControl.token.parentValue,
      parentName = vm.sourceControl.token.parentName,
      orgProvider = vm.sourceControl.provider ? vm.sourceControl.provider.value : null,
      provider = vm.providerTypesMap[vm.effectiveProvider];

    if (!vm.sourceControl || !vm.effectiveProvider) {
      text = 'Source Control not configured';
    } else {
      if (vm.isRootOrg) {
        text = 'Provides the default source control configuration settings';
      } else if (!!orgProvider && !token) {
        text = 'Inherit access token';
      } else if (!token) {
        text = `Inherit access token${parentValue ? ` from ${parentName}` : ''}\
${vm.isApp ? ` (${provider})` : ''}`;
      } else {
        text = `Provides default access token for ${vm.ownerName}${vm.isApp ? ` (${provider})` : ''}`;
      }
    }
    return text;
  }
}
const mapStateToThis = (state) => ({
  isSourceControlSupported: selectIsSourceControlForSourceTileSupported(state),
  ownerName: selectSelectedOwnerName(state),
});

SourceControlTileController.$inject = [
  '$scope',
  'SameOwnerStateNavigationService',
  'event.name.constant',
  'CLMContextLocations',
  'OrganizationStore',
  'ApplicationStore',
  '$q',
  'Messages',
  'SourceControlService',
  '$ngRedux',
];
