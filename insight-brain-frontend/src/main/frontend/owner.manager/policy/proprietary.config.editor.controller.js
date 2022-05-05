/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { actions, matcherTypes } from 'MainRoot/OrgsAndPolicies/proprietarySlice';
import {
  selectLoadError,
  selectSubmitError,
  selectIsLoading,
  selectLocalMatchers,
  selectProprietaryConfigs,
  selectCurrentConfigs,
  selectPackageMatcher,
  selectRegexMatcher,
  selectMatcherType,
  selectIsDirty,
} from 'MainRoot/OrgsAndPolicies/proprietarySelectors';

const PACKAGE_REGEXP = new RegExp('^[^ /.][^ /]*[^ /.]$');

export default function ProprietaryConfigEditorController($scope, $ngRedux) {
  const vm = this;

  vm.unsubscribe = $ngRedux.connect(mapStateToThis, {
    load: actions.loadProprietaryConfig,
    saveConfig: actions.saveProprietaryConfig,
    removeMatcher: actions.removeMatcher,
    setMatcherPackageValue: actions.setMatcherPackageValue,
    setMatcherRegexValue: actions.setMatcherRegexValue,
    resetMatcher: actions.resetMatcher,
    setMatcherTypeValue: actions.setMatcherType,
    addLocalMatcher: actions.addMatcher,
  })(vm);

  Object.assign(vm, {
    proprietaryConfigEditor: undefined,
    proprietaryConfigEditorMask: undefined,

    // needed for view
    matcherTypes: matcherTypes,
    matcherTypeOptions: [matcherTypes.PACKAGE, matcherTypes.REGEX],

    doLoad() {
      vm.load();
    },

    save() {
      vm.proprietaryConfigEditorMask.wrap(
        vm.saveConfig({
          setPristine: () => {
            vm.proprietaryConfigEditor.$setPristine();
          },
        })
      );
    },

    setPackageValue() {
      const value = vm.packageMatcher || vm.proprietaryConfigEditor['matcher-value'].$viewValue;
      vm.setMatcherPackageValue(value);
    },

    setRegexValue() {
      vm.setMatcherRegexValue(vm.regexMatcher);
    },

    setMatcherType() {
      vm.setMatcherTypeValue(vm.matcherType);
    },

    validatePackage(value) {
      return {
        invalidPrefix: !value || PACKAGE_REGEXP.test(value),
        wildcards: !value || value.indexOf('*') < 0,
      };
    },

    isAddButtonDisabled() {
      const matcherToAdd = vm.matcherType === vm.matcherTypes.REGEX ? vm.regexMatcher : vm.packageMatcher;
      return !vm.proprietaryConfigEditor.$valid || !matcherToAdd;
    },

    addMatcher(keypressEvent) {
      if (keypressEvent) {
        keypressEvent.preventDefault();
      }

      var matcherToAdd = vm.matcherType === matcherTypes.REGEX ? vm.regexMatcher : vm.packageMatcher;

      if (!matcherToAdd) {
        return;
      }

      vm.addLocalMatcher({ type: vm.matcherType, matcher: matcherToAdd });
      vm.resetMatcher();
      vm.proprietaryConfigEditor.$setPristine();
    },
  });

  $scope.$on('pageChangeStarted', (event) => {
    if (vm.isDirty) {
      event.preventDefault();
    }
  });

  vm.doLoad();

  $scope.$on('$destroy', function () {
    vm.unsubscribe();
  });
}

export const mapStateToThis = (state) => {
  return {
    loadError: selectLoadError(state),
    submitError: selectSubmitError(state),
    loading: selectIsLoading(state),
    localMatchers: angular.copy(selectLocalMatchers(state)),
    proprietaryConfigs: angular.copy(selectProprietaryConfigs(state)),
    dirtyProprietaryConfig: angular.copy(selectCurrentConfigs(state)),
    packageMatcher: selectPackageMatcher(state),
    regexMatcher: selectRegexMatcher(state),
    matcherType: selectMatcherType(state),
    isDirty: selectIsDirty(state),
  };
};

ProprietaryConfigEditorController.$inject = ['$scope', '$ngRedux'];
