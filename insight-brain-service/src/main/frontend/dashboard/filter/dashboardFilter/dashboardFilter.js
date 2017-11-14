/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './dashboardFilter.html';

var dashboardFilter = {
  template: template,
  controller: DashboardFilterController,
  controllerAs: 'vm'
};

export default dashboardFilter;

function DashboardFilterController($rootScope, $scope, $http, $q, CLMLocations, ApplicationStore, StageTypeStore,
                                   OrganizationStore, EventNameConstant, filterService, $state) {
  var vm = this,
      appliedFilter,
      appliedFilterName,
      uncategorizedCategory = {
        description: 'uncategorized applications',
        id: null, // NOTE that in this case null specifically means include uncategorized apps
        name: 'No Category',
        nameLowercaseNoWhitespace: 'nocategory'
      };

  // Available
  vm.organizations = undefined;
  vm.applications = undefined;
  vm.categories = undefined;
  vm.stages = undefined;
  vm.policyTypes = [
    {
      id: 'SECURITY',
      name: 'Security'
    }, {
      id: 'LICENSE',
      name: 'License'
    }, {
      id: 'QUALITY',
      name: 'Quality'
    }, {
      id: 'OTHER',
      name: 'Other'
    }
  ];
  vm.policyViolationStates = [
    {
      id: 'OPEN',
      name: 'Open'
    }, {
      id: 'WAIVED',
      name: 'Waived'
    }
  ];
  vm.ages = [
    {
      name: 'past 24 hours',
      maxDaysOld: 1
    },
    {
      name: 'past 7 days',
      maxDaysOld: 7
    },
    {
      name: 'past 30 days',
      maxDaysOld: 30
    },
    {
      name: 'past 90 days',
      maxDaysOld: 90
    },
    {
      name: 'past 12 months',
      maxDaysOld: 365
    },
    {
      name: 'all time',
      maxDaysOld: null
    }
  ];
  vm.policySliderRangeHighlights = [
    { start: 0, end: 0.5, cls: 'threat-none' },
    { start: 0.5, end: 1.5, cls: 'threat-low' },
    { start: 1.5, end: 3.5, cls: 'threat-moderate' },
    { start: 3.5, end: 7.5, cls: 'threat-severe' },
    { start: 7.5, end: 10, cls: 'threat-critical' }
  ];

  var defaultAge = vm.ages[2];

  // User selected
  vm.selected = undefined;

  vm.loadError = undefined;
  vm.saveError = undefined;
  vm.savedNamedFilters = [];
  vm.loadErrorFilterName = undefined;

  vm.doLoad = doLoad;
  vm.isDirty = isDirty;
  vm.showDirtyAsterisk = false;
  vm.clear = clear;
  vm.revert = revert;
  vm.activeFilterName = undefined;
  vm.applyCurrentFilter = applyCurrentFilter;
  vm.loadFilterFromJson = loadFilterFromJson;

  vm.onFilterSelected = onFilterSelected;
  vm.onActiveFilterDeleted = onActiveFilterDeleted;
  vm.onFilterSaved = onFilterSaved;
  vm.toggleManageFiltersDropdown = toggleManageFiltersDropdown;
  vm.selectOrgsAndApps = selectOrgsAndApps;

  vm.showAgeFilter = undefined;
  vm.isAgeFilterReadOnly = undefined;
  vm.needsAcknowledgement = undefined;

  function selectOrgsAndApps(organizationsSet, applicationsSet) {
    const organizations = setToObject(organizationsSet);
    const applications = setToObject(applicationsSet);
    vm.selected = {...vm.selected, organizations, applications};
  }

  function setToObject(set) {
    const obj = {};
    set.forEach(id => obj[id] = true);
    return obj;
  }

  function shouldShowAgeFilter() {
    return ($state.$current.name === 'dashboard.overview.violations') &&
        ($state.params.timeFilterFeature || (vm.selected && vm.selected.age !== defaultAge));
  }

  function shouldAgeFilterBeReadOnly() {
    return !$state.params.timeFilterFeature;
  }

  function selectPredefinedAge(maxDaysOld) {
    var filtered = vm.ages.filter(function(age) {
      return age.maxDaysOld === maxDaysOld;
    });
    return filtered.length === 1 ? filtered[0] : defaultAge;
  }

  function toggleManageFiltersDropdown(open) {
    vm.isManageFiltersDropdownOpen = open;
  }

  vm.doLoad();

  $scope.$on('reloadFilter', function() {
    vm.doLoad();
  });

  $scope.$watch(function() {
    return $state.$current.name;
  }, function() {
    vm.showAgeFilter = shouldShowAgeFilter();
    vm.isAgeFilterReadOnly = shouldAgeFilterBeReadOnly();
  });

  // fire the UPDATE_DASHBOARD_FILTERS_DIRTINESS event whenever the value of isDirty changes
  $scope.$watch(isDirty, $rootScope.$broadcast.bind($rootScope, EventNameConstant.UPDATE_DASHBOARD_FILTERS_DIRTINESS));

  function doLoad() {
    delete vm.loadError;

    var promises = [
      ApplicationStore.get(), StageTypeStore.getDashboardStages(), OrganizationStore.get(),
      $http.get(CLMLocations.getApplicationTagsUrl()), $http.get(CLMLocations.getDashboardFilters()),
      $http.get(CLMLocations.getDashboardSavedFilters())
    ];

    $q.all(promises).then(function(data) {
      var activeFilter = data[4].data.filter;
      vm.needsAcknowledgement = data[4].data.needsAcknowledgement;

      vm.organizations = angular.copy(data[2]); // copied as we modify objects
      vm.applications = data[0];
      vm.stages = data[1];
      vm.categories = data[3].data;
      vm.savedNamedFilters = data[5].data;
      vm.activeFilterName = appliedFilterName = data[4].data.basedOnFilterName;

      angular.forEach(vm.categories, function(category) {
        for (var i = 0; i < vm.organizations.length; i++) {
          if (category.organizationId === vm.organizations[i].id) {
            category.owner = vm.organizations[i].name;
            break;
          }
        }
      });

      vm.categories.push(uncategorizedCategory);

      vm.applications.forEach(function(application) {
        var orgExists = vm.organizations.some(function(organization) {
          return application.organizationId === organization.id;
        });

        if (!orgExists) {
          vm.organizations.push({id: application.organizationId, name: application.organizationName});
        }
      });

      vm.organizations = vm.organizations.filter(function(organization) {
        return organization.id !== 'ROOT_ORGANIZATION_ID';
      });

      if (activeFilter) {
        vm.loadFilterFromJson(activeFilter);
      }

      appliedFilter = angular.copy(vm.selected);
      var savedNamedFilter = vm.activeFilterName && vm.savedNamedFilters.filter(function(savedFilter) {
        return savedFilter.name === vm.activeFilterName;
      })[0];
      if (savedNamedFilter && !angular.equals(activeFilter, savedNamedFilter.filter)) {
        vm.showDirtyAsterisk = true;
      }
      $rootScope.$broadcast(EventNameConstant.UPDATE_DASHBOARD_FILTERS, activeFilter, vm.needsAcknowledgement);
    }, function(error) {
      vm.loadError = error;
    });
  }

  function clear() {
    resetFilter();
    delete vm.loadErrorFilterName;
    delete vm.activeFilterName;
  }

  function revert() {
    vm.selected = angular.copy(appliedFilter);
    delete vm.loadErrorFilterName;
    vm.activeFilterName = appliedFilterName;
  }

  function resetFilter() {
    vm.selected = {
      organizations: {},
      applications: {},
      categories: {},
      stages: {},
      policyTypes: {},
      policyViolationStates: {OPEN: true},
      age: selectPredefinedAge(),
      policyThreatLevels: [2, 10]
    };
  }

  function applyCurrentFilter() {
    delete vm.saveError;
    delete vm.loadErrorFilterName;

    if (!vm.isDirty() && !vm.needsAcknowledgement) {
      return;
    }

    var namedFilter = {
      filter: filterService.filterToJson(vm.selected),
      basedOnFilterName: vm.activeFilterName
    };

    applyFilter(namedFilter).then(function() {
      vm.showDirtyAsterisk = true;
      appliedFilter = angular.copy(vm.selected);
      vm.showAgeFilter = shouldShowAgeFilter();
    }, function(error) {
      vm.saveError = error;
    });
  }

  function loadFilterFromJson(filterJson) {
    resetFilter();
    filterJson = angular.copy(filterJson); // copied as we modify app filters
    (filterJson.organizationFilters || []).forEach(function(organizationId) {

      var orgExists = vm.organizations.some(function(organization) {
        return organization.id === organizationId;
      });

      if (orgExists) {
        vm.selected.organizations[organizationId] = true;

        vm.applications.forEach(function(application) {
          if (application.organizationId === organizationId) {
            var appExistsInFilter = (filterJson.applicationFilters || []).some(function(applicationId) {
              return application.id === applicationId;
            });

            if (!appExistsInFilter) {
              (filterJson.applicationFilters || []).push(application.id);
            }
          }
        });
      }
    });

    (filterJson.applicationFilters || []).forEach(function(applicationId) {
      vm.selected.applications[applicationId] = true;
    });

    var existingCategoryIds = vm.categories.map(function(category) {
      return category.id;
    });

    (filterJson.tagFilters || []).forEach(function(categoryId) {
      // avoid adding no-longer-existing category ids to vm.selected.categories
      if (existingCategoryIds.indexOf(categoryId) !== -1) {
        vm.selected.categories[categoryId] = true;
      }
    });

    (filterJson.stageTypeFilters || []).forEach(function(stageId) {
      vm.selected.stages[stageId] = true;
    });

    (filterJson.policyThreatCategoryFilters || []).forEach(function(policyTypeId) {
      vm.selected.policyTypes[policyTypeId] = true;
    });

    if (filterJson.policyViolationStates) {
      vm.selected.policyViolationStates = {};
      filterJson.policyViolationStates.forEach(function(statusId) {
        vm.selected.policyViolationStates[statusId] = true;
      });
    }

    vm.selected.age = selectPredefinedAge(filterJson.maxDaysOld);

    vm.showAgeFilter = shouldShowAgeFilter();

    vm.isAgeFilterReadOnly = shouldAgeFilterBeReadOnly();

    vm.selected.policyThreatLevels = [filterJson.minPolicyThreatLevel, filterJson.maxPolicyThreatLevel];
  }

  function onFilterSelected(savedFilter) {
    delete vm.loadErrorFilterName;

    savedFilter.basedOnFilterName = savedFilter.name;
    applyFilter(savedFilter).then(function(activeFilter) {
      vm.loadFilterFromJson(activeFilter);
      appliedFilter = angular.copy(vm.selected);
      appliedFilterName = vm.activeFilterName = savedFilter.name;
      vm.showDirtyAsterisk = false;
    }, function() {
      vm.loadErrorFilterName = savedFilter.name;
    });

  }

  function onActiveFilterDeleted() {
    appliedFilterName = vm.activeFilterName = undefined;
  }

  function onFilterSaved(filterName) {
    appliedFilterName = vm.activeFilterName = filterName;
    vm.showDirtyAsterisk = false;
  }

  /**
   * Persists active filter and applies it to dashboard results
   * @param filterJson
   * @returns Promise wrapping active filter json
   */
  function applyFilter(filterJson) {
    return $http.put(CLMLocations.getDashboardFilters(), filterJson).then(function(activeFilter) {
      appliedFilterName = vm.activeFilterName;
      vm.needsAcknowledgement = false;
      $rootScope.$broadcast(EventNameConstant.UPDATE_DASHBOARD_FILTERS, activeFilter.data, vm.needsAcknowledgement);
      return activeFilter.data;
    });
  }

  function isDirty() {
    return !angular.equals(vm.selected, appliedFilter);
  }
}

DashboardFilterController.$inject = [
  '$rootScope', '$scope', '$http', '$q', 'CLMLocations', 'ApplicationStore', 'StageTypeStore', 'OrganizationStore',
  'event.name.constant', 'dashboardFilterService', '$state'
];
