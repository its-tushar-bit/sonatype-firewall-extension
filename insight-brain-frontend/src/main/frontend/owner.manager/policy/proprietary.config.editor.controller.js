/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function ProprietaryConfigEditorController($scope, Messages, ProprietaryConfigHierarchyStore) {
  var vm = this,
    PACKAGE_REGEXP = new RegExp('^[^ /.][^ /]*[^ /.]$');

  vm.matcherTypes = { PACKAGE: 'Package', REGEX: 'Regular Expression' };

  vm.addMatcher = addMatcher;
  vm.dirtyProprietaryConfig = undefined;
  vm.doLoad = doLoad;
  vm.isAddButtonDisabled = isAddButtonDisabled;
  vm.loadError = undefined;
  vm.localMatchers = undefined;
  vm.matcherType = vm.matcherTypes.PACKAGE;
  vm.matcherTypeOptions = [vm.matcherTypes.PACKAGE, vm.matcherTypes.REGEX];
  vm.packageMatcher = '';
  vm.proprietaryConfigEditor = undefined;
  vm.proprietaryConfigEditorMask = undefined;
  vm.proprietaryConfigs = undefined;
  vm.regexMatcher = undefined;
  vm.resetMatcher = resetMatcher;
  vm.removeMatcher = removeMatcher;
  vm.save = save;
  vm.submitError = undefined;
  vm.validatePackage = validatePackage;

  vm.doLoad();

  $scope.$on('pageChangeStarted', function (event) {
    if (vm.dirtyProprietaryConfig.isDirty()) {
      event.preventDefault();
    }
  });

  function doLoad() {
    delete vm.loadError;

    ProprietaryConfigHierarchyStore.get().then(
      function (results) {
        vm.localMatchers = [];
        vm.proprietaryConfigs = results;
        vm.proprietaryConfigs.forEach(function (configOwner, index) {
          var proprietaryConfig = configOwner.proprietaryConfig[0];
          if (index === 0) {
            vm.dirtyProprietaryConfig = proprietaryConfig.$clone();
            proprietaryConfig.packages.forEach(function (component) {
              var matcher = {
                type: vm.matcherTypes.PACKAGE,
                matcher: component,
              };
              vm.localMatchers.push(matcher);
            });
            proprietaryConfig.regexes.forEach(function (regex) {
              var matcher = { type: vm.matcherTypes.REGEX, matcher: regex };
              vm.localMatchers.push(matcher);
            });
          }
        });
      },
      function (error) {
        vm.loadError = Messages.getHttpErrorMessage(error);
      }
    );
  }

  function save() {
    delete vm.submitError;

    vm.proprietaryConfigEditorMask.wrap(vm.dirtyProprietaryConfig.$save()).then(
      function () {
        vm.proprietaryConfigEditor.$setPristine();
      },
      function (error) {
        vm.submitError = Messages.getHttpErrorMessage(error);
      }
    );
  }

  function addMatcher(keypressEvent) {
    if (keypressEvent) {
      keypressEvent.preventDefault();
    }

    var matcherToAdd = vm.matcherType === vm.matcherTypes.REGEX ? vm.regexMatcher : vm.packageMatcher;

    if (!matcherToAdd) {
      return;
    }

    vm.localMatchers.push({ type: vm.matcherType, matcher: matcherToAdd });
    if (vm.matcherType === vm.matcherTypes.REGEX) {
      vm.dirtyProprietaryConfig.regexes.push(matcherToAdd);
    } else {
      vm.dirtyProprietaryConfig.packages.push(matcherToAdd);
    }

    resetMatcher();
    vm.proprietaryConfigEditor.$setPristine();
  }

  function resetMatcher() {
    vm.packageMatcher = undefined;
    vm.regexMatcher = undefined;
  }

  function removeMatcher(theMatcher) {
    vm.localMatchers = vm.localMatchers.filter(function (aMatcher) {
      return aMatcher !== theMatcher;
    });
    if (theMatcher.type === vm.matcherTypes.REGEX) {
      vm.dirtyProprietaryConfig.regexes = vm.dirtyProprietaryConfig.regexes.filter(function (aMatcher) {
        return aMatcher !== theMatcher.matcher;
      });
    } else {
      vm.dirtyProprietaryConfig.packages = vm.dirtyProprietaryConfig.packages.filter(function (aMatcher) {
        return aMatcher !== theMatcher.matcher;
      });
    }
  }

  function validatePackage(value) {
    return {
      invalidPrefix: !value || PACKAGE_REGEXP.test(value),
      wildcards: !value || value.indexOf('*') < 0,
    };
  }

  function isAddButtonDisabled() {
    var matcherToAdd = vm.matcherType === vm.matcherTypes.REGEX ? vm.regexMatcher : vm.packageMatcher;
    return !matcherToAdd;
  }
}

ProprietaryConfigEditorController.$inject = ['$scope', 'Messages', 'ProprietaryConfigHierarchyStore'];
