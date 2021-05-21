/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

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
  ProductFeatures
) {
  var vm = this;

  vm.ownerType = undefined;
  vm.ownerName = undefined;
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
  vm.isAutomationSupported = undefined;
  vm.isSourceControlSupported = undefined;
  vm.effectiveProvider = undefined;
  vm.doLoad();

  $scope.$on(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA, function () {
    doLoad();
  });
  $scope.$on(EventNameConstant.OWNER_UPDATED, updatedOwnerHandler);

  function doLoad() {
    vm.error = undefined;
    vm.loading = true;

    let ownerPromise;

    if (vm.isApp) {
      ownerPromise = ApplicationStore.getById(CLMContextLocations.getEntityId());
      vm.ownerType = 'application';
    } else if (vm.isOrg) {
      ownerPromise = OrganizationStore.getById(CLMContextLocations.getEntityId());
      vm.ownerType = 'organization';
    }

    if (ownerPromise !== undefined) {
      const promises = [ownerPromise, ProductFeatures.load()];
      $q.all(promises)
        .then(function (results) {
          vm.ownerName = results[0].name;
          let isNotificationsSupported = ProductFeatures.isAvailable('notifications');
          vm.isAutomationSupported = ProductFeatures.isAvailable('automation');
          vm.isSourceControlSupported = isNotificationsSupported || vm.isAutomationSupported;
          if (vm.isSourceControlSupported) {
            return getSourceControl(results[0].id);
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

  function updatedOwnerHandler(event, newOwner) {
    vm.ownerName = newOwner.name;
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
  'ProductFeatures',
];
