/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { range } from 'ramda';

import applicationReportModule from '../../../../main/frontend/applicationReport/module';
import { mapStateToThis } from '../../../../main/frontend/applicationReport/results/applicationReportResults';

describe('applicationReportResults', function () {
  let vm, scope, OwnerContext, mockModal, $q;

  beforeEach(angular.mock.module(applicationReportModule.name));

  beforeEach(
    angular.mock.module(function ($provide) {
      SpecUtil.mockNgRedux($provide);
    })
  );

  beforeEach(inject(function (_$componentController_, $rootScope, _OwnerContext_, _$q_) {
    OwnerContext = _OwnerContext_;
    scope = $rootScope.$new();
    $q = _$q_;
    mockModal = jasmine.createSpyObj('Modal', ['open']);
    vm = _$componentController_('applicationReportResults', {
      $state: {
        params: { publicId: 'testApp', scanId: 'testReport' },
        current: { name: 'applicationReport.policy' },
      },
      $scope: scope,
      Modal: mockModal,
    });
    scope.vm = vm;
    vm.$onInit();
  }));

  describe('$onInit()', function () {
    it('subscribes to the redux store', () => {
      expect(vm.unsubscribe).toBeDefined();
    });

    it('watches vm.reportParameters and sets OwnerType, OwnerId and ScanId in OwnerContext', function () {
      spyOn(OwnerContext, 'setOwnerType');
      spyOn(OwnerContext, 'setOwnerId');
      spyOn(OwnerContext, 'setScanId');
      vm.reportParameters = {
        appId: 'test-application-23424iufg',
        scanId: 'test-scan-23424iufg',
      };
      scope.$digest();
      expect(OwnerContext.setOwnerType).toHaveBeenCalledWith('application');
      expect(OwnerContext.setOwnerId).toHaveBeenCalledWith('test-application-23424iufg');
      expect(OwnerContext.setScanId).toHaveBeenCalledWith('test-scan-23424iufg');
    });

    it('watches vm.reportParameters and handles null value', function () {
      spyOn(OwnerContext, 'setOwnerId');
      spyOn(OwnerContext, 'setScanId');
      vm.reportParameters = null;
      scope.$digest();
      expect(OwnerContext.setOwnerId).not.toHaveBeenCalled();
      expect(OwnerContext.setScanId).not.toHaveBeenCalled();
    });
  });

  describe('getReportPdfDownloadUrl()', function () {
    it('generates a PDF link from app id and scan id', () => {
      vm.metadata = {
        application: {
          publicId: 'appId',
        },
      };
      vm.reportParameters = {
        scanId: 'scanId',
      };
      expect(vm.getReportPdfDownloadUrl()).toEqual('/rest/report/appId/scanId/printReport');
    });
  });

  describe('getViewSbomUrl()', function () {
    it('generates a SBOM link from app id and scan id', () => {
      vm.metadata = {
        application: {
          id: 'appId',
        },
      };
      vm.reportParameters = {
        scanId: 'scanId',
      };
      expect(vm.getViewSbomUrl()).toEqual('/ui/links/cycloneDx/appId/reports/scanId');
    });
  });

  describe('getInnerSourceParentsTooltip()', function () {
    it('builds a tooltip for a transitive dependency from an InnerSource parent', () => {
      const component = {
        innerSourceParentsDerivedComponentNames: ['parent1 : v1'],
      };
      const tooltipText = vm.getInnerSourceParentsTooltip(component);
      expect(tooltipText).toContain('This component was brought in by the following InnerSource component:');
      expect(tooltipText).toContain('parent1 : v1');
    });

    it('builds a tooltip for a transitive dependency from multiple InnerSource parents', () => {
      const component = {
        innerSourceParentsDerivedComponentNames: ['parent1 : v1', 'parent2 : v1.0'],
      };
      const tooltipText = vm.getInnerSourceParentsTooltip(component);
      expect(tooltipText).toContain('This component was brought in by the following InnerSource components:');
      expect(tooltipText).toContain('parent1 : v1');
      expect(tooltipText).toContain('parent2 : v1.0');
    });
  });

  describe('vm.selectedReport.displayedEntries watcher', function () {
    let $timeout;

    beforeEach(inject(function (_$timeout_) {
      $timeout = _$timeout_;
    }));

    afterEach(function () {
      $timeout.verifyNoPendingTasks();
    });

    it('populates vm.renderedEntries in chunks of 100 at a time', function () {
      expect(vm.renderedEntries).toEqual([]);

      vm.selectedReport = { displayedEntries: [1, 2, 3] };
      scope.$digest();
      expect(vm.renderedEntries).toEqual([1, 2, 3]);

      vm.selectedReport = { displayedEntries: range(1, 351) };
      scope.$digest();
      expect(vm.renderedEntries).toEqual(range(1, 101));

      $timeout.flush();
      expect(vm.renderedEntries).toEqual(range(1, 201));

      $timeout.flush();
      expect(vm.renderedEntries).toEqual(range(1, 301));

      $timeout.flush();
      expect(vm.renderedEntries).toEqual(range(1, 351));
    });

    it(
      'restarts populating vm.renderedEntries if vm.selectedReport.displayedEntries changes while it is in ' +
        'progress',
      function () {
        vm.selectedReport = { displayedEntries: range(1, 351) };
        scope.$digest();
        expect(vm.renderedEntries).toEqual(range(1, 101));

        $timeout.flush();
        expect(vm.renderedEntries).toEqual(range(1, 201));

        vm.selectedReport = { displayedEntries: range(5, 151) };
        scope.$digest();
        expect(vm.renderedEntries).toEqual(range(5, 105));

        $timeout.flush();
        expect(vm.renderedEntries).toEqual(range(5, 151));
      }
    );

    it('clears vm.renderedEntries when vm.selectedReport is not defined', function () {
      vm.selectedReport = { displayedEntries: [1, 2, 3] };
      scope.$digest();
      expect(vm.renderedEntries).toEqual([1, 2, 3]);

      vm.selectedReport = null;
      scope.$digest();
      expect(vm.renderedEntries).toEqual([]);
    });

    it('clears vm.renderedEntries when vm.selectedReport.displayedEntries is empty', function () {
      vm.selectedReport = { displayedEntries: [1, 2, 3] };
      scope.$digest();
      expect(vm.renderedEntries).toEqual([1, 2, 3]);

      vm.selectedReport = { displayedEntries: [] };
      scope.$digest();
      expect(vm.renderedEntries).toEqual([]);
    });

    it('opens CIP when rendering and the appropiate index for the policvyViolationId is found', function () {
      vm.refreshReportUrlRemovePolicyViolationId = jasmine.createSpy('refreshReportUrlRemovePolicyViolationId');
      vm.openCipModal = jasmine.createSpy('openCipModal');

      // Ensure the previous displayed entries are not defined (opening the report for the first time)
      vm.selectedReport = undefined;
      scope.$digest();

      vm.aggregate = true;
      vm.reportParameters = {
        policyViolationId: 'policyViolation99',
      };
      const numberToHashedComponentFunction = (number) => {
        return { policyViolationId: `policyViolation${number}` };
      };

      vm.selectedReport = {
        displayedEntries: range(0, 100).map(numberToHashedComponentFunction),
      };
      scope.$digest();
      expect(vm.refreshReportUrlRemovePolicyViolationId).toHaveBeenCalled();
      expect(vm.openCipModal).toHaveBeenCalledWith(99);
    });

    it(
      'opens CIP when rendering and the violation is not found in displayedEntries but exists in allEntries ' +
        'with a matching component hash',
      function () {
        vm.refreshReportUrlRemovePolicyViolationId = jasmine.createSpy('refreshReportUrlRemovePolicyViolationId');
        vm.openCipModal = jasmine.createSpy('openCipModal');

        // Ensure the previous displayed entries are not defined (opening the report for the first time)
        vm.selectedReport = undefined;
        scope.$digest();

        vm.aggregate = true;
        vm.reportParameters = {
          policyViolationId: 'policyViolation150',
        };
        const numberToHashedComponentFunction = (number) => {
          return {
            policyViolationId: `policyViolation${number}`,
            hash: `hash${number - 100}`,
          };
        };
        const numberToHashedComponentFunctionForDisplayed = (number) => {
          return {
            policyViolationId: `policyViolation${number}`,
            hash: `hash${number}`,
          };
        };

        vm.selectedReport = {
          displayedEntries: range(0, 100).map(numberToHashedComponentFunctionForDisplayed),
          allEntries: range(0, 200).map(numberToHashedComponentFunction),
        };
        scope.$digest();
        expect(vm.refreshReportUrlRemovePolicyViolationId).toHaveBeenCalled();
        expect(vm.openCipModal).toHaveBeenCalledWith(50);
      }
    );

    it(
      'does not open CIP when rendering violation is not found in displayedEntries and the component hash is in ' +
        'allEntries but has no match in displayed entries',
      function () {
        vm.openCipModal = jasmine.createSpy('openCipModal');

        // Ensure the previous displayed entries are not defined (opening the report for the first time)
        vm.selectedReport = undefined;
        scope.$digest();

        vm.aggregate = true;
        vm.reportParameters = {
          policyViolationId: 'policyViolation150',
        };
        const numberToHashedComponentFunction = (number) => {
          return {
            policyViolationId: `policyViolation${number}`,
            hash: `hash${number - 100}`,
          };
        };
        const numberToHashedComponentFunctionForDisplayed = (number) => {
          return {
            policyViolationId: `policyViolation${number}`,
            hash: `hash${number}`,
          };
        };

        vm.selectedReport = {
          displayedEntries: range(0, 20).map(numberToHashedComponentFunctionForDisplayed),
          allEntries: range(0, 200).map(numberToHashedComponentFunction),
        };
        scope.$digest();
        expect(vm.openCipModal).not.toHaveBeenCalled();
      }
    );

    it('does not open CIP when rendering and there is a selected index already in place (reevaluation)', function () {
      vm.refreshReportUrlRemovePolicyViolationId = jasmine.createSpy('refreshReportUrlRemovePolicyViolationId');
      vm.openCipModal = jasmine.createSpy('openCipModal');

      // Ensure the previous displayed entries are not defined (report reevaluation)
      vm.selectedReport = undefined;
      scope.$digest();

      vm.selectedComponentIndex = 0;
      vm.reportParameters = {
        policyViolationId: 'policyViolation99',
      };
      const numberToHashedComponentFunction = (number) => {
        return { policyViolationId: `policyViolation${number}` };
      };

      vm.selectedReport = {
        displayedEntries: range(0, 10).map(numberToHashedComponentFunction),
      };
      scope.$digest();
      expect(vm.refreshReportUrlRemovePolicyViolationId).not.toHaveBeenCalled();
      expect(vm.openCipModal).not.toHaveBeenCalled();
    });

    it('opens CIP when rendering and the appropriate index for the componentHash is found', function () {
      vm.refreshReportUrlRemoveComponentHashAndTabId = jasmine.createSpy('refreshReportUrlRemoveComponentHashAndTabId');
      vm.openCipModal = jasmine.createSpy('openCipModal');

      // Ensure the previous displayed entries are not defined (opening the report for the first time)
      vm.selectedReport = undefined;
      scope.$digest();

      vm.aggregate = true;
      vm.reportParameters = {
        componentHash: 'componentHash99',
        tabId: 'policy',
      };
      const numberToHashedComponentFunction = (number) => {
        return { hash: `componentHash${number}` };
      };

      vm.selectedReport = {
        displayedEntries: range(0, 100).map(numberToHashedComponentFunction),
      };
      scope.$digest();
      expect(vm.refreshReportUrlRemoveComponentHashAndTabId).toHaveBeenCalled();
      expect(vm.openCipModal).toHaveBeenCalledWith(99);
    });

    it('opens CIP when rendering and the appropriate index for the componentHash is not found in displayedEntries but exists in allEntries', function () {
      vm.refreshReportUrlRemoveComponentHashAndTabId = jasmine.createSpy('refreshReportUrlRemoveComponentHashAndTabId');
      vm.openCipModal = jasmine.createSpy('openCipModal');

      // Ensure the previous displayed entries are not defined (opening the report for the first time)
      vm.selectedReport = undefined;
      scope.$digest();

      vm.aggregate = true;
      vm.reportParameters = {
        componentHash: 'componentHash150',
        tabId: 'policy',
      };
      const numberToHashedComponentFunction = (number) => {
        return { hash: `componentHash${number}` };
      };

      vm.selectedReport = {
        displayedEntries: range(0, 100).map(numberToHashedComponentFunction),
        allEntries: range(0, 200).map(numberToHashedComponentFunction),
      };
      scope.$digest();
      expect(vm.refreshReportUrlRemoveComponentHashAndTabId).toHaveBeenCalled();
      expect(vm.openCipModal).toHaveBeenCalledWith(150);
    });
  });

  describe('$onDestroy()', function () {
    it('unsubscribes from redux store', function () {
      expect(vm.unsubscribe).not.toHaveBeenCalled();
      vm.$onDestroy();
      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });

  describe('reload', function () {
    it('calls loadReport action with proper params', function () {
      vm.reload();
      expect(vm.loadReport).toHaveBeenCalledWith('testApp', 'testReport', false);
    });
  });

  describe('coveragePercent', function () {
    it('returns 0 if the totalArtifactCount is 0', function () {
      vm.selectedReport = { totalArtifactCount: 0, knownArtifactCount: 0 };

      expect(vm.coveragePercent()).toBe(0);
    });

    it('returns the ratio of knownArtifactCount to totalArtifactCount as a percent', function () {
      vm.selectedReport = { totalArtifactCount: 60, knownArtifactCount: 45 };

      expect(vm.coveragePercent()).toBe(75);
    });

    it('rounds the returned percent to a whole number', function () {
      vm.selectedReport = { totalArtifactCount: 300, knownArtifactCount: 151 };

      expect(vm.coveragePercent()).toBe(50);
    });
  });

  describe('mapStateToThis', () => {
    it('spreads the applicationReport object from state', () => {
      let state = {
        applicationReport: {
          pendingLoads: new Set(),
          foo: 'bar',
          substringFilters: {},
        },
      };

      let output = mapStateToThis(state);
      expect(output).toEqual(jasmine.objectContaining({ foo: 'bar' }));
    });

    it('maps substring filters to fields appropriately', () => {
      let state = {
        applicationReport: {
          pendingLoads: new Set(),
          substringFilters: {
            derivedComponentName: 'filter1',
            policyName: 'filter2',
          },
        },
      };

      let output = mapStateToThis(state);
      expect(output.derivedComponentNameSubstringFilter).toEqual('filter1');
      expect(output.policyNameSubstringFilter).toEqual('filter2');
    });

    it('sets the loading flag based on whether pendingLoads is empty', function () {
      const loadingState = {
          applicationReport: {
            pendingLoads: new Set(['foo']),
            substringFilters: {},
          },
        },
        nonLoadingState = {
          applicationReport: {
            pendingLoads: new Set(),
            substringFilters: {},
          },
        };

      expect(mapStateToThis(loadingState).loading).toBe(true);
      expect(mapStateToThis(nonLoadingState).loading).toBe(false);
    });
  });

  describe('openCipModal', function () {
    it('calls selects component with provided index and opens cip modal', function () {
      mockModal.open.and.returnValue({
        result: $q.resolve('foo'),
      });
      vm.selectedReport = {
        displayedEntries: [
          { policyViolationId: 'policyViolationIndex0' },
          { policyViolationId: 'policyViolationIndex1' },
          { policyViolationId: 'policyViolationIndex2' },
          { policyViolationId: 'policyViolationIndex3' },
          { policyViolationId: 'policyViolationIndex4' },
        ],
      };

      vm.openCipModal(3);
      expect(vm.selectComponent).toHaveBeenCalledWith(3);
      expect(mockModal.open).toHaveBeenCalled();
    });
  });

  describe('onRowClick', function () {
    beforeEach(() => {
      vm.selectComponent = jasmine.createSpy('selectComponent');
      vm.openCipModal = jasmine.createSpy('openCipModal');
      vm.goToComponentDetailsPage = jasmine.createSpy('goToComponentDetailsPage');
      vm.selectedComponent = { hash: 'selectedHash' };
    });

    it('calls vm.selectComponent with the component index if $state.params.componentDetailsEnabled is present', function () {
      vm.$state.params.componentDetailsEnabled = true;
      vm.onRowClick(0);
      expect(vm.selectComponent).toHaveBeenCalledWith(0);
      expect(vm.openCipModal).not.toHaveBeenCalled();
    });

    it('calls goToComponentDetailsPage if $state.params.componentDetailsEnabled', function () {
      vm.$state.params.componentDetailsEnabled = true;
      vm.onRowClick(0);
      expect(vm.goToComponentDetailsPage).toHaveBeenCalledWith('selectedHash');
    });

    it('calls openCipModal if $state.params.componentDetailsEnabled is false or not present', function () {
      vm.$state.params = {};
      vm.onRowClick(0);
      expect(vm.openCipModal).toHaveBeenCalledWith(0);
      expect(vm.selectComponent).not.toHaveBeenCalled();
      expect(vm.goToComponentDetailsPage).not.toHaveBeenCalled();

      vm.$state.params.componentDetailsEnabled = false;
      vm.onRowClick(0);
      expect(vm.openCipModal).toHaveBeenCalledWith(0);
      expect(vm.selectComponent).not.toHaveBeenCalled();
      expect(vm.goToComponentDetailsPage).not.toHaveBeenCalled();
    });
  });

  describe('getTransitiveViolationsCount()', function () {
    it('returns a count of the transitive policy violations', () => {
      vm.selectedReport = {
        allEntries: [
          {
            // intentionally empty
          },
          {
            policyThreatLevel: 1,
          },
          {
            dependencyInfo: {},
          },
          {
            policyThreatLevel: 1,
            dependencyInfo: {},
          },
          {
            policyThreatLevel: 1,
            dependencyInfo: {
              rootAncestors: [],
            },
          },
          {
            dependencyInfo: {
              rootAncestors: [
                {
                  format: 'npm',
                  coordinates: {
                    packageId: 'name1',
                    version: '1',
                  },
                },
              ],
            },
          },
          {
            policyThreatLevel: 0,
            dependencyInfo: {
              rootAncestors: [
                {
                  format: 'npm',
                  coordinates: {
                    packageId: 'name1',
                    version: '1',
                  },
                },
              ],
            },
          },
          {
            waived: true,
            policyThreatLevel: 1,
            dependencyInfo: {
              rootAncestors: [
                {
                  format: 'npm',
                  coordinates: {
                    packageId: 'name1',
                    version: '1',
                  },
                },
              ],
            },
          },
          {
            grandfathered: true,
            policyThreatLevel: 1,
            dependencyInfo: {
              rootAncestors: [
                {
                  format: 'npm',
                  coordinates: {
                    packageId: 'name1',
                    version: '1',
                  },
                },
              ],
            },
          },
          {
            policyThreatLevel: 1,
            dependencyInfo: {
              rootAncestors: [
                {
                  format: 'npm',
                  coordinates: {
                    packageId: 'name1',
                    version: '1',
                  },
                },
              ],
            },
          },
          {
            policyThreatLevel: 10,
            dependencyInfo: {
              rootAncestors: [
                {
                  format: 'npm',
                  coordinates: {
                    packageId: 'name1',
                    version: '1',
                  },
                },
              ],
            },
          },
          {
            policyThreatLevel: 1,
            dependencyInfo: {
              rootAncestors: [
                {
                  format: 'npm',
                  coordinates: {
                    packageId: 'name2',
                    version: '1',
                  },
                },
              ],
            },
          },
          {
            policyThreatLevel: 1,
            dependencyInfo: {
              rootAncestors: [
                {
                  format: 'npm',
                  coordinates: {
                    packageId: 'name1',
                    version: '2',
                  },
                },
              ],
            },
          },
        ],
      };
      const component1 = {
        componentIdentifier: {
          format: 'npm',
          coordinates: {
            packageId: 'name1',
            version: '1',
          },
        },
      };
      const component2 = {
        componentIdentifier: {
          format: 'npm',
          coordinates: {
            packageId: 'name1',
            version: '2',
          },
        },
      };
      const unknown = {
        componentIdentifier: {
          format: 'npm',
          coordinates: {
            packageId: 'unknown',
            version: '1',
          },
        },
      };
      expect(vm.getTransitiveViolationsCount(component1)).toBe('2 transitive violations');
      expect(vm.getTransitiveViolationsCount(component2)).toBe('1 transitive violation');
      expect(vm.getTransitiveViolationsCount(unknown)).toBe('0 transitive violations');
    });
  });
});
