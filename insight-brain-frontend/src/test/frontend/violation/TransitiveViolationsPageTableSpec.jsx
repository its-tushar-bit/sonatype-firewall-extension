/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import * as enzymeUtils from '../enzymeUtils';
import { NxFontAwesomeIcon, NxTableBody, NxTableCell, NxThreatIndicator } from '@sonatype/react-shared-components';
import { faExclamationCircle, faExclamationTriangle } from '@fortawesome/pro-solid-svg-icons';
import TransitiveViolationsPageTable from '../../../main/frontend/violation/TransitiveViolationsPageTable';

describe('TransitiveViolationsPageTable', function () {
  let minimalProps, spySetSortingParameters, spySetFilteringParameters, getMountedComponent, getShallowComponent;

  beforeEach(function () {
    spySetSortingParameters = jasmine.createSpy('spySetSortingParameters');
    spySetFilteringParameters = jasmine.createSpy('spySetFilteringParameters');
    minimalProps = {
      stageTypeId: 'someStageTypeId',
      componentTransitivePolicyViolations: {
        loading: false,
        error: null,
        sortConfiguration: {
          key: 'threatLevel',
          dir: 'desc',
        },
        filterConfiguration: {
          policyName: '',
          displayName: '',
        },
        data: {
          displayName: 'someDisplayName',
          isInnerSource: false,
          displayedViolations: [],
        },
      },
      setSortingParameters: spySetSortingParameters,
      setFilteringParameters: spySetFilteringParameters,
    };
    getShallowComponent = enzymeUtils.getShallowComponent(TransitiveViolationsPageTable, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(TransitiveViolationsPageTable, minimalProps);
  });

  describe('transitive policy violation', function () {
    const getShallowComponentWithTransitivePolicyViolation = (transitivePolicyViolation) => {
      return getShallowComponent({
        ...minimalProps,
        componentTransitivePolicyViolations: {
          ...minimalProps.componentTransitivePolicyViolations,
          data: {
            ...minimalProps.componentTransitivePolicyViolations.data,
            displayedViolations: [
              {
                policyViolationId: 'somePolicyViolationId',
                policyName: 'somePolicyName',
                threatLevel: 0,
                threatCategory: 'someThreatCategory',
                displayName: 'someDisplayName',
                ...transitivePolicyViolation,
              },
            ],
          },
        },
      });
    };

    const expectTransitivePolicyViolationWithThreatLevelHasClass = (threatLevel, className) => {
      const wrapper = getShallowComponentWithTransitivePolicyViolation({ threatLevel: threatLevel });
      const policyName = wrapper.find('.iq-transitive-violations-page-policy-name');
      expect(policyName).toMatchSelector(className);
    };

    it('with threat level above 7 is critical', function () {
      expectTransitivePolicyViolationWithThreatLevelHasClass(10, '.iq-threat-level--critical');
      expectTransitivePolicyViolationWithThreatLevelHasClass(9, '.iq-threat-level--critical');
      expectTransitivePolicyViolationWithThreatLevelHasClass(8, '.iq-threat-level--critical');
    });

    it('with threat level above 3 is severe', function () {
      expectTransitivePolicyViolationWithThreatLevelHasClass(7, '.iq-threat-level--severe');
      expectTransitivePolicyViolationWithThreatLevelHasClass(6, '.iq-threat-level--severe');
      expectTransitivePolicyViolationWithThreatLevelHasClass(5, '.iq-threat-level--severe');
      expectTransitivePolicyViolationWithThreatLevelHasClass(4, '.iq-threat-level--severe');
    });

    it('with threat level above 1 is moderate', function () {
      expectTransitivePolicyViolationWithThreatLevelHasClass(3, '.iq-threat-level--moderate');
      expectTransitivePolicyViolationWithThreatLevelHasClass(2, '.iq-threat-level--moderate');
    });

    it('with threat level 1 is low', function () {
      expectTransitivePolicyViolationWithThreatLevelHasClass(1, '.iq-threat-level--low');
    });

    it('with threat level 0 is none', function () {
      expectTransitivePolicyViolationWithThreatLevelHasClass(0, '.iq-threat-level--none');
    });

    it('with action fail uses the correct icon and text', function () {
      const wrapper = getShallowComponentWithTransitivePolicyViolation({ action: 'fail' });
      const actionFailIcon = wrapper.find('.nx-icon--fail');
      expect(actionFailIcon).toExist();
      const parent = actionFailIcon.parents('div').at(0);
      expect(parent.find(NxFontAwesomeIcon)).toHaveProp('icon', faExclamationCircle);
      expect(parent.text()).toContain('Failing SomeStageTypeId');
    });

    it('with action warn uses the correct icon and text', function () {
      const wrapper = getShallowComponentWithTransitivePolicyViolation({ action: 'warn' });
      const actionWarnIcon = wrapper.find('.nx-icon--warn');
      expect(actionWarnIcon).toExist();
      const parent = actionWarnIcon.parents('div').at(0);
      expect(parent.find(NxFontAwesomeIcon)).toHaveProp('icon', faExclamationTriangle);
      expect(parent.text()).toContain('Warning');
    });

    it('has its threat level, policy name, and display name in cells', function () {
      const wrapper = getShallowComponentWithTransitivePolicyViolation();
      const row = wrapper.findWhere((node) => node.key() === 'somePolicyViolationId').at(0);
      const cells = row.find(NxTableCell);
      expect(cells.length).toBe(3);
      const nxThreatIndicator = cells.at(0).find(NxThreatIndicator);
      expect(nxThreatIndicator).toHaveProp('policyThreatLevel', 0);
      const nxThreatNumber = cells.at(0).find('.nx-threat-number');
      expect(nxThreatNumber).toHaveText('0');
      expect(cells.at(0).html()).toContain('0');
      expect(cells.at(1).html()).toContain('somePolicyName');
      expect(cells.at(2).html()).toContain('someDisplayName');
    });
  });

  describe('sorting', function () {
    it('has initial sorting by threat level descending', function () {
      const wrapper = getShallowComponent();
      const threatLevelHeaderCell = wrapper.find('#iq-transitive-violations-page-threat-level');
      expect(threatLevelHeaderCell).toHaveProp('isSortable');
      expect(threatLevelHeaderCell).toHaveProp('sortDir', 'desc');
      expect(threatLevelHeaderCell.childAt(0).text()).toEqual('Threat');
      const policyNameHeaderCell = wrapper.find('#iq-transitive-violations-page-policy-name');
      expect(policyNameHeaderCell).toHaveProp('isSortable');
      expect(policyNameHeaderCell).toHaveProp('sortDir', null);
      expect(policyNameHeaderCell.childAt(0).text()).toEqual('Policy/Action');
      const displayNameHeaderCell = wrapper.find('#iq-transitive-violations-page-display-name');
      expect(displayNameHeaderCell).toHaveProp('isSortable');
      expect(displayNameHeaderCell).toHaveProp('sortDir', null);
      expect(displayNameHeaderCell.childAt(0).text()).toEqual('Component');
    });

    it('on threat level header cell click calls setSortingParameters with the correct parameters', function () {
      const wrapper = getShallowComponent();
      const headerCell = wrapper.find('#iq-transitive-violations-page-threat-level');
      headerCell.simulate('click');
      expect(spySetSortingParameters.calls.mostRecent().args[0]).toEqual('threatLevel');
    });

    it('on policy name header cell click calls setSortingParameters with the correct parameters', function () {
      const wrapper = getShallowComponent();
      const headerCell = wrapper.find('#iq-transitive-violations-page-policy-name');
      headerCell.simulate('click');
      expect(spySetSortingParameters.calls.mostRecent().args[0]).toEqual('policyName');
    });

    it('on display name header cell click calls setSortingParameters with the correct parameters', function () {
      const wrapper = getShallowComponent();
      const headerCell = wrapper.find('#iq-transitive-violations-page-display-name');
      headerCell.simulate('click');
      expect(spySetSortingParameters.calls.mostRecent().args[0]).toEqual('displayName');
    });
  });

  describe('filtering', function () {
    it('on policy name filter input calls setFilteringParameters with the correct parameters', function () {
      const wrapper = getShallowComponent();
      const policyNameFilterInput = wrapper.find('#iq-transitive-violations-page-policy-name-filter');
      expect(policyNameFilterInput).toHaveProp('placeholder', 'Policy Name');
      expect(policyNameFilterInput).toHaveProp('value', '');
      policyNameFilterInput.simulate('change', 'some text');
      expect(spySetFilteringParameters).toHaveBeenCalledWith({
        policyName: 'some text',
      });
    });

    it('on display name filter input calls setFilteringParameters with the correct parameters', function () {
      const wrapper = getShallowComponent();
      const displayNameFilterInput = wrapper.find('#iq-transitive-violations-page-display-name-filter');
      expect(displayNameFilterInput).toHaveProp('placeholder', 'Component Name');
      expect(displayNameFilterInput).toHaveProp('value', '');
      displayNameFilterInput.simulate('change', 'some text');
      expect(spySetFilteringParameters).toHaveBeenCalledWith({
        displayName: 'some text',
      });
    });
  });

  it('displays empty message with no transitive policy violations', function () {
    const wrapper = getMountedComponent();
    const nxTableBody = wrapper.find(NxTableBody);
    expect(nxTableBody).toHaveProp('emptyMessage', 'None');
    expect(nxTableBody.html()).toContain('None');
  });
});
