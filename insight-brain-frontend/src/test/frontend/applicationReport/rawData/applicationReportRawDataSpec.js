/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import applicationReportModule from '../../../../main/frontend/applicationReport/module';
import { mapStateToThis } from '../../../../main/frontend/applicationReport/rawData/applicationReportRawData';

describe('applicationReportRawData', function () {
  let vm, SelectedComponent, OwnerContext;

  beforeEach(angular.mock.module(applicationReportModule.name));

  beforeEach(
    angular.mock.module(function ($provide) {
      SpecUtil.mockNgRedux($provide);
    })
  );

  beforeEach(inject(function (_$componentController_) {
    SelectedComponent = jasmine.createSpyObj('SelectedComponent', ['toggle']);
    OwnerContext = {
      scanId: 'scanId',
      ownerId: 'ownerId',
      ownerType: 'ownerType',
    };
    vm = _$componentController_('applicationReportRawData', {
      SelectedComponent,
      OwnerContext,
    });
    vm.$onInit();
  }));

  describe('$onInit()', function () {
    it('subscribes to the redux store', () => {
      expect(vm.unsubscribe).toBeDefined();
    });

    it('calls loadReportRawData action', () => {
      expect(vm.loadReportRawData).toHaveBeenCalled();
    });
  });

  describe('$onDestroy()', function () {
    it('unsubscribes from redux store', function () {
      expect(vm.unsubscribe).not.toHaveBeenCalled();
      vm.$onDestroy();
      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });

  describe('load()', function () {
    it('calls loadReportRawData action', function () {
      vm.load();
      expect(vm.loadReportRawData).toHaveBeenCalled();
    });
  });

  describe('getLicenseTooltip', function () {
    it('returns an HTML string with the declared and observed licenses', function () {
      const data = {
        license: {
          declaredLicenses: ['foo', 'bar'],
          observedLicenses: ['foo', 'baz'],
        },
      };

      expect(vm.getLicenseTooltip(data)).toMatch(
        // eslint-disable-next-line max-len
        /^\s*<dl class="iq-license-table">\s*<dt>Declared:<\/dt><dd>foo, bar<\/dd>\s*<dt>Observed:<\/dt><dd>foo, baz<\/dd>\s*<\/dl>\s*$/
      );
    });

    it("Uses '-' as a placeholder if the declaredLicenses list is empty", function () {
      const data = {
        license: {
          declaredLicenses: [],
          observedLicenses: ['foo', 'baz'],
        },
      };

      expect(vm.getLicenseTooltip(data)).toMatch(
        // eslint-disable-next-line max-len
        /^\s*<dl class="iq-license-table">\s*<dt>Declared:<\/dt><dd>-<\/dd>\s*<dt>Observed:<\/dt><dd>foo, baz<\/dd>\s*<\/dl>\s*$/
      );
    });

    it("Uses '-' as a placeholder if the observedLicenses list is empty", function () {
      const data = {
        license: {
          declaredLicenses: ['foo', 'bar'],
          observedLicenses: [],
        },
      };

      expect(vm.getLicenseTooltip(data)).toMatch(
        // eslint-disable-next-line max-len
        /^\s*<dl class="iq-license-table">\s*<dt>Declared:<\/dt><dd>foo, bar<\/dd>\s*<dt>Observed:<\/dt><dd>-<\/dd>\s*<\/dl>\s*$/
      );
    });

    it("Uses '-' as a placeholder if the `declaredLicences property is missing", function () {
      const data = {
        license: {
          observedLicenses: ['foo', 'baz'],
        },
      };

      expect(vm.getLicenseTooltip(data)).toMatch(
        // eslint-disable-next-line max-len
        /^\s*<dl class="iq-license-table">\s*<dt>Declared:<\/dt><dd>-<\/dd>\s*<dt>Observed:<\/dt><dd>foo, baz<\/dd>\s*<\/dl>\s*$/
      );
    });

    it("Uses '-' as a placeholder if the observedLicenses list is missing", function () {
      const data = {
        license: {
          declaredLicenses: ['foo', 'bar'],
        },
      };

      expect(vm.getLicenseTooltip(data)).toMatch(
        // eslint-disable-next-line max-len
        /^\s*<dl class="iq-license-table">\s*<dt>Declared:<\/dt><dd>foo, bar<\/dd>\s*<dt>Observed:<\/dt><dd>-<\/dd>\s*<\/dl>\s*$/
      );
    });

    it("Uses '-' as a placeholder if the license object is missing", function () {
      const data = {};

      expect(vm.getLicenseTooltip(data)).toMatch(
        // eslint-disable-next-line max-len
        /^\s*<dl class="iq-license-table">\s*<dt>Declared:<\/dt><dd>-<\/dd>\s*<dt>Observed:<\/dt><dd>-<\/dd>\s*<\/dl>\s*$/
      );
    });
  });

  describe('mapStateToThis', () => {
    it('spreads the applicationReport object from state', () => {
      let state = {
        applicationReport: {
          pendingLoads: new Set(),
          foo: 'bar',
          rawDataSubstringFilters: {},
          rawDataNumericFilters: {},
        },
        vulnerabilityDetailsModal: {
          vulnerabilityId: null,
        },
      };

      let output = mapStateToThis(state);
      expect(output).toEqual(jasmine.objectContaining({ foo: 'bar' }));
    });

    it('maps substring filters to fields appropriately', () => {
      let state = {
        applicationReport: {
          pendingLoads: new Set(),
          rawDataSubstringFilters: {
            derivedComponentName: 'filter1',
            licenseSortKey: 'filter2',
            securityCode: 'filter3',
          },
          rawDataNumericFilters: {},
        },
        vulnerabilityDetailsModal: {
          vulnerabilityId: null,
        },
      };

      let output = mapStateToThis(state);
      expect(output.derivedComponentNameSubstringFilter).toEqual('filter1');
      expect(output.licenseSortKeySubstringFilter).toEqual('filter2');
      expect(output.securityCodeSubstringFilter).toEqual('filter3');
    });

    it('maps the numeric filters from the cvssScore array to fields appropriately', () => {
      let state = {
        applicationReport: {
          pendingLoads: new Set(),
          rawDataSubstringFilters: {},
          rawDataNumericFilters: {
            cvssScore: [1, 3.5],
          },
        },
        vulnerabilityDetailsModal: {
          vulnerabilityId: null,
        },
      };

      let output = mapStateToThis(state);
      expect(output.cvssMinNumericFilter).toEqual(1);
      expect(output.cvssMaxNumericFilter).toEqual(3.5);
    });

    it('sets default cvss filters if filters inside rawDataNumericFilters are not arrays', () => {
      let state = {
        applicationReport: {
          pendingLoads: new Set(),
          rawDataSubstringFilters: {},
          rawDataNumericFilters: {
            cvssScore: 9,
          },
        },
        vulnerabilityDetailsModal: {
          vulnerabilityId: null,
        },
      };

      let output = mapStateToThis(state);
      expect(output.cvssMinNumericFilter).toBeUndefined();
      expect(output.cvssMaxNumericFilter).toBeUndefined();
    });

    it('sets the loading flag based on whether pendingLoads is empty', function () {
      const loadingState = {
          applicationReport: {
            pendingLoads: new Set(['foo']),
            rawDataSubstringFilters: {},
            rawDataNumericFilters: {},
          },
          vulnerabilityDetailsModal: {
            vulnerabilityId: null,
          },
        },
        nonLoadingState = {
          applicationReport: {
            pendingLoads: new Set(),
            rawDataSubstringFilters: {},
            rawDataNumericFilters: {},
          },
          vulnerabilityDetailsModal: {
            vulnerabilityId: null,
          },
        };

      expect(mapStateToThis(loadingState).loading).toBe(true);
      expect(mapStateToThis(nonLoadingState).loading).toBe(false);
    });

    it('maps vulnerabilityId from state', () => {
      let stateWithIdPresent = {
        applicationReport: {
          pendingLoads: new Set(),
          rawDataSubstringFilters: {},
          rawDataNumericFilters: {},
        },
        vulnerabilityDetailsModal: {
          vulnerabilityId: 'CVE-3456',
        },
      };

      let stateWithNullId = {
        applicationReport: {
          pendingLoads: new Set(),
          rawDataSubstringFilters: {},
          rawDataNumericFilters: {},
        },
        vulnerabilityDetailsModal: {
          vulnerabilityId: null,
        },
      };

      expect(mapStateToThis(stateWithIdPresent)).toEqual(jasmine.objectContaining({ vulnerabilityId: 'CVE-3456' }));
      expect(mapStateToThis(stateWithNullId)).toEqual(jasmine.objectContaining({ vulnerabilityId: null }));
    });
  });

  describe('openVulnerabilitiesModal', function () {
    let mockRawDataEntry;

    beforeEach(function () {
      mockRawDataEntry = {
        source: 'cvs',
        securityCode: 'sonatype-2014-0015',
        license: {
          hash: '16e2da53f9d2c1744211',
          componentIdentifier: {
            format: 'a-name',
            coordinates: {
              name: 'org.webjars angularjs',
              qualifier: '',
              version: '1.2.16',
            },
          },
        },
        componentIdentifier: {
          format: 'a-name',
          coordinates: {
            name: 'org.webjars bootstrap',
            qualifier: '',
            version: '3.1.1',
          },
        },
        identificationSource: 'identificationSource',
      };
    });

    it('calls selectedComponent.toggle first and then calls openVulnerabilityDetailsModal', function () {
      const { securityCode, componentIdentifier } = mockRawDataEntry;
      vm.openVulnerabilitiesModal(mockRawDataEntry);
      expect(SelectedComponent.toggle).toHaveBeenCalledBefore(vm.openVulnerabilityDetailsModal);
      expect(SelectedComponent.toggle).toHaveBeenCalledWith(mockRawDataEntry);
      expect(vm.openVulnerabilityDetailsModal).toHaveBeenCalledWith({
        vulnerabilityId: securityCode,
        componentIdentifier,
        thirdPartyScanParameters: {
          identificationSource: 'identificationSource',
          scanId: 'scanId',
          ownerId: 'ownerId',
          ownerType: 'ownerType',
        },
      });
    });
  });
});
