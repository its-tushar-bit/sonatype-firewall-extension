/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { always } from 'ramda';

import * as enzymeUtils from '../enzymeUtils';
import ViolationDetailsTile from '../../../main/frontend/violation/ViolationDetailsTile';
import LoadWrapper from '../../../main/frontend/react/LoadWrapper';
import ViolationPage from '../../../main/frontend/violation/ViolationPage';
import SecurityVulnerabilityDetailsTile from '../../../main/frontend/violation/SecurityVulnerabilityDetailsTile';
import PolicyViolationConstraintInfoTile from '../../../main/frontend/violation/PolicyViolationConstraintInfoTile';

describe('ViolationPage', function () {
  let minimalProps,
    loadViolationSpy,
    fetchStageTypesSpy,
    stateGoSpy,
    getShallowComponent,
    getMountedComponent,
    loadFirewallPolicyVulnerabilityDetailsSpy,
    loadFirewallViolationDetailsSpy,
    loadApplicableWaiversSpy;

  beforeEach(function () {
    loadViolationSpy = jasmine.createSpy('loadViolation');
    fetchStageTypesSpy = jasmine.createSpy('fetchStageTypes');
    stateGoSpy = jasmine.createSpy('stateGo');
    loadFirewallPolicyVulnerabilityDetailsSpy = jasmine.createSpy('loadFirewallPolicyVulnerabilityDetails');
    loadFirewallViolationDetailsSpy = jasmine.createSpy('loadFirewallViolationDetails');
    loadApplicableWaiversSpy = jasmine.createSpy('loadApplicableWaivers');

    minimalProps = {
      $state: {
        get: always({
          data: {
            title: 'asdf',
          },
        }),
        href: always('qwerty'),
      },
      selectedViolationId: 'foo',
      loadViolation: loadViolationSpy,
      fetchStageTypes: fetchStageTypesSpy,
      stateGo: stateGoSpy,
      loading: false,
      isFirewallContext: false,
      refId: { value: 'CVE-2012-2098' },
      policyDetail: {
        policyViolationId: '02a6107559a94c39b04d4ec8374b9508',
        policyId: 'd98fb873ed1f48e5b00316d8acddbc0f',
        policyName: 'Security-Medium',
        policyOwner: {
          ownerId: 'ROOT_ORGANIZATION_ID',
          ownerName: 'Root Organization',
          ownerType: 'organization',
        },
        policyThreatLevel: 7,
        policyThreatCategory: 'SECURITY',
        constraints: [
          {
            constraintId: 'c6436a5a051046b1ba2aa94e9fd82a51',
            constraintName: 'Medium risk CVSS score',
            constraintOperator: 'AND',
            conditions: [
              {
                conditionType: 'SecurityVulnerabilitySeverity',
                conditionSummary: 'Security Vulnerability Severity >= 4',
                conditionReason: 'Found security vulnerability CVE-2012-2098 with severity >= 4 (severity = 5.0)',
                conditionTriggerReference: {
                  value: 'CVE-2012-2098',
                  type: 'SECURITY_VULNERABILITY_REFID',
                },
              },
              {
                conditionType: 'SecurityVulnerabilitySeverity',
                conditionSummary: 'Security Vulnerability Severity < 7',
                conditionReason: 'Found security vulnerability CVE-2012-2098 with severity < 7 (severity = 5.0)',
                conditionTriggerReference: {
                  value: 'CVE-2012-2098',
                  type: 'SECURITY_VULNERABILITY_REFID',
                },
              },
            ],
          },
        ],
        constraintFactsJson:
          '[{"constraintId":"c6436a5a051046b1ba2aa94e9fd82a51","constraintName":"Medium risk CVSS score","operatorName":"AND","conditionFacts":[{"conditionTypeId":"SecurityVulnerabilitySeverity","conditionIndex":0,"summary":"Security Vulnerability Severity >= 4","reason":"Found security vulnerability CVE-2012-2098 with severity >= 4 (severity = 5.0)","reference":{"value":"CVE-2012-2098","type":"SECURITY_VULNERABILITY_REFID"},"triggerJson":"{\\"conditionIndex\\":0,\\"trigger\\":{\\"refId\\":\\"CVE-2012-2098\\",\\"severity\\":5.0}}"},{"conditionTypeId":"SecurityVulnerabilitySeverity","conditionIndex":1,"summary":"Security Vulnerability Severity < 7","reason":"Found security vulnerability CVE-2012-2098 with severity < 7 (severity = 5.0)","reference":{"value":"CVE-2012-2098","type":"SECURITY_VULNERABILITY_REFID"},"triggerJson":"{\\"conditionIndex\\":1,\\"trigger\\":{\\"refId\\":\\"CVE-2012-2098\\",\\"severity\\":5.0}}"}]}]',
        policyActionTypeId: null,
        lastReported: '2022-08-10T13:35:40.641+03:00',
      },
      constraintViolations: [
        {
          constraintId: 'c6436a5a051046b1ba2aa94e9fd82a51',
          constraintName: 'Medium risk CVSS score',
          constraintOperator: 'AND',
          conditions: [
            {
              conditionType: 'SecurityVulnerabilitySeverity',
              conditionSummary: 'Security Vulnerability Severity >= 4',
              conditionReason: 'Found security vulnerability CVE-2012-2098 with severity >= 4 (severity = 5.0)',
              conditionTriggerReference: {
                value: 'CVE-2012-2098',
                type: 'SECURITY_VULNERABILITY_REFID',
              },
            },
            {
              conditionType: 'SecurityVulnerabilitySeverity',
              conditionSummary: 'Security Vulnerability Severity < 7',
              conditionReason: 'Found security vulnerability CVE-2012-2098 with severity < 7 (severity = 5.0)',
              conditionTriggerReference: {
                value: 'CVE-2012-2098',
                type: 'SECURITY_VULNERABILITY_REFID',
              },
            },
          ],
        },
      ],
      policyViolations: [
        {
          policyViolationId: '02a6107559a94c39b04d4ec8374b9508',
          policyId: 'd98fb873ed1f48e5b00316d8acddbc0f',
          policyName: 'Security-Medium',
          policyOwner: {
            ownerId: 'ROOT_ORGANIZATION_ID',
            ownerName: 'Root Organization',
            ownerType: 'organization',
          },
          policyThreatLevel: 7,
          policyThreatCategory: 'SECURITY',
          constraints: [
            {
              constraintId: 'c6436a5a051046b1ba2aa94e9fd82a51',
              constraintName: 'Medium risk CVSS score',
              constraintOperator: 'AND',
              conditions: [
                {
                  conditionType: 'SecurityVulnerabilitySeverity',
                  conditionSummary: 'Security Vulnerability Severity >= 4',
                  conditionReason: 'Found security vulnerability CVE-2012-2098 with severity >= 4 (severity = 5.0)',
                  conditionTriggerReference: {
                    value: 'CVE-2012-2098',
                    type: 'SECURITY_VULNERABILITY_REFID',
                  },
                },
                {
                  conditionType: 'SecurityVulnerabilitySeverity',
                  conditionSummary: 'Security Vulnerability Severity < 7',
                  conditionReason: 'Found security vulnerability CVE-2012-2098 with severity < 7 (severity = 5.0)',
                  conditionTriggerReference: {
                    value: 'CVE-2012-2098',
                    type: 'SECURITY_VULNERABILITY_REFID',
                  },
                },
              ],
            },
          ],
          constraintFactsJson:
            '[{"constraintId":"c6436a5a051046b1ba2aa94e9fd82a51","constraintName":"Medium risk CVSS score","operatorName":"AND","conditionFacts":[{"conditionTypeId":"SecurityVulnerabilitySeverity","conditionIndex":0,"summary":"Security Vulnerability Severity >= 4","reason":"Found security vulnerability CVE-2012-2098 with severity >= 4 (severity = 5.0)","reference":{"value":"CVE-2012-2098","type":"SECURITY_VULNERABILITY_REFID"},"triggerJson":"{\\"conditionIndex\\":0,\\"trigger\\":{\\"refId\\":\\"CVE-2012-2098\\",\\"severity\\":5.0}}"},{"conditionTypeId":"SecurityVulnerabilitySeverity","conditionIndex":1,"summary":"Security Vulnerability Severity < 7","reason":"Found security vulnerability CVE-2012-2098 with severity < 7 (severity = 5.0)","reference":{"value":"CVE-2012-2098","type":"SECURITY_VULNERABILITY_REFID"},"triggerJson":"{\\"conditionIndex\\":1,\\"trigger\\":{\\"refId\\":\\"CVE-2012-2098\\",\\"severity\\":5.0}}"}]}]',
          policyActionTypeId: null,
          lastReported: '2022-08-10T13:35:40.641+03:00',
        },
      ],
      selectPolicyId: '02a6107559a94c39b04d4ec8374b9508',
      loadFirewallPolicyVulnerabilityDetails: loadFirewallPolicyVulnerabilityDetailsSpy,
      loadFirewallViolationDetails: loadFirewallViolationDetailsSpy,
      loadApplicableWaivers: loadApplicableWaiversSpy,
      hasPermissionForAppWaivers: true,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(ViolationPage, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(ViolationPage, minimalProps);
  });

  it('renders a LoadWrapper within the page', function () {
    expect(getShallowComponent().find(LoadWrapper)).toExist();
  });

  it("sets the LoadWrapper's loading flag based on the loading, violationDetails, and stageTypes props", function () {
    const getLoadWrapper = (props) => getShallowComponent(props).find(LoadWrapper);

    expect(getLoadWrapper()).toHaveProp('loading', true);
    expect(getLoadWrapper({ violationDetails: {} })).toHaveProp('loading', true);
    expect(getLoadWrapper({ stageTypes: [] })).toHaveProp('loading', true);
    expect(getLoadWrapper({ violationDetails: {}, stageTypes: [] })).toHaveProp('loading', false);
    expect(getLoadWrapper({ violationDetails: {}, stageTypes: [], loading: true })).toHaveProp('loading', true);
  });

  it("sets the LoadWrapper's error from the violationDetailsError and stageTypesError props", function () {
    const getLoadWrapper = (props) => getShallowComponent(props).find(LoadWrapper);

    expect(getLoadWrapper()).toHaveProp('error', undefined);
    expect(getLoadWrapper({ violationDetailsError: 'foo' })).toHaveProp('error', 'foo');
    expect(getLoadWrapper({ stageTypesError: 'foo' })).toHaveProp('error', 'foo');
    expect(getLoadWrapper({ violationDetailsError: 'foo', stageTypesError: 'bar' })).toHaveProp('error', 'foo');
  });

  it("sets the LoadWrapper's retryHandler to a function that calls loadViolation, fetchStateTypes and loadAddWaiverPermission", function () {
    const loadWrapper = getShallowComponent().find(LoadWrapper),
      retryHandler = loadWrapper.prop('retryHandler');

    expect(loadViolationSpy).not.toHaveBeenCalled();
    expect(fetchStageTypesSpy).not.toHaveBeenCalled();

    retryHandler();

    expect(loadViolationSpy).toHaveBeenCalledWith('foo');
    expect(fetchStageTypesSpy).toHaveBeenCalledWith('dashboard');
  });

  it('calls loadViolation with the $state id param, fetchStageTypes with the `dashboard` param and loadAddWaiverPermission on first load', function () {
    getMountedComponent();

    expect(loadViolationSpy).toHaveBeenCalledWith('foo');
    expect(fetchStageTypesSpy).toHaveBeenCalledWith('dashboard');
  });

  it('calls loadViolation whenever the selectedViolationId prop changes', function () {
    const component = getMountedComponent();

    expect(loadViolationSpy).toHaveBeenCalledWith('foo');
    expect(loadViolationSpy).not.toHaveBeenCalledWith('bar');

    component.setProps({ selectedViolationId: 'bar' });

    expect(loadViolationSpy).toHaveBeenCalledWith('bar');
  });

  it('calls loadFirewallPolicyVulnerabilityDetails', function () {
    const component = getMountedComponent({ violationDetails: {} });

    expect(loadFirewallPolicyVulnerabilityDetailsSpy).not.toHaveBeenCalledWith('CVE-2012-2098');

    component.setProps({ isFirewallContext: true });

    expect(loadFirewallPolicyVulnerabilityDetailsSpy).toHaveBeenCalledWith('CVE-2012-2098');
  });

  it('renders a ViolationDetailsTile within the LoadWrapper with $state, stageTypes, violationDetails & stateGo', function () {
    const violationDetails = {},
      stageTypes = {},
      stateGo = () => {},
      component = getShallowComponent({
        ...minimalProps,
        violationDetails,
        stageTypes,
        stateGo,
      }),
      tile = component.find(LoadWrapper).find(ViolationDetailsTile);

    expect(tile).toExist();
    expect(tile.prop('$state')).toBe(minimalProps.$state);
    expect(tile.prop('violationDetails')).toBe(violationDetails);
    expect(tile.prop('stageTypes')).toBe(stageTypes);
    expect(tile.prop('isFirewallContext')).toBe(minimalProps.isFirewallContext);
    expect(tile.prop('policyViolations')).toBe(minimalProps.policyViolations);
    expect(tile.prop('selectPolicyId')).toBe(minimalProps.selectPolicyId);
    expect(tile.prop('policyDetail')).toEqual(minimalProps.policyDetail);
  });

  it('renders a PolicyViolationConstraintInfoTile within the LoadWrapper with correct props', function () {
    const violationDetails = { constraintViolations: 'constraintViolations', isFirewallContext: false };
    const tile = getShallowComponent({ violationDetails }).find(LoadWrapper).find(PolicyViolationConstraintInfoTile);

    expect(tile).toExist();
    expect(tile.prop('constraintViolations')).toBe('constraintViolations');
    expect(tile.prop('isFirewallContext')).toBe(false);
  });

  it("renders a SecurityVulnerabilityDetailsTile with correct props if it's a security vulnerability", function () {
    const violationDetails = {
      policyThreatCategory: 'security',
    };
    const vulnerabilityDetails = { foo: 'bar' };
    let tile = getShallowComponent({
      vulnerabilityDetails,
      violationDetails,
      vulnerabilityDetailsError: 'Test Error',
      vulnerabilityDetailsLoading: true,
    })
      .find(LoadWrapper)
      .find(SecurityVulnerabilityDetailsTile);

    expect(tile).toExist();
    expect(tile.prop('vulnerabilityDetails')).toBe(vulnerabilityDetails);
    expect(tile.prop('error')).toBe('Test Error');
    expect(tile.prop('loading')).toBe(true);
    expect(tile.prop('showTitle')).toBe(true);

    tile = getShallowComponent({
      vulnerabilityDetails,
      violationDetails,
      vulnerabilityDetailsError: 'Test Error',
      vulnerabilityDetailsLoading: true,
      isFromPolicyViolations: true,
    })
      .find(LoadWrapper)
      .find(SecurityVulnerabilityDetailsTile);

    expect(tile.prop('showTitle')).toBe(false);
  });

  it("does not render a SecurityVulnerabilityDetailsTile if it's not a security vulnerability", function () {
    const violationDetails = {
      policyThreatCategory: 'license',
    };
    const vulnerabilityDetails = { foo: 'bar' };
    const tile = getShallowComponent({
      vulnerabilityDetails,
      violationDetails,
      vulnerabilityDetailsError: 'Test Error',
      vulnerabilityDetailsLoading: true,
    })
      .find(LoadWrapper)
      .find(SecurityVulnerabilityDetailsTile);

    expect(tile).not.toExist();
  });

  it('calls loadFirewallViolationDetails with params', function () {
    const component = getMountedComponent({ violationDetails: {} });
    component.setProps({ isFirewallContext: true });
    expect(loadFirewallViolationDetailsSpy).toHaveBeenCalledWith('02a6107559a94c39b04d4ec8374b9508');
  });

  it('calls loadApplicableWaivers with params', function () {
    const component = getMountedComponent({});
    component.setProps({ isFirewallContext: true });
    expect(loadApplicableWaiversSpy).toHaveBeenCalledWith('02a6107559a94c39b04d4ec8374b9508');
  });

  it('renders component name below the vulnerability identifier', function () {
    const violationDetails = {
      policyThreatCategory: 'security',
      filename: 'test',
      displayName: {
        name: 'testFile',
        parts: [
          {
            field: 'Group',
            value: 'a',
          },
          {
            value: ' : ',
          },
          {
            field: 'Artifact',
            value: 'b',
          },
          {
            value: ' : ',
          },
          {
            field: 'Version',
            value: 'c',
          },
        ],
      },
    };
    const vulnerabilityDetails = { foo: 'bar' };
    let tile = getShallowComponent({
      vulnerabilityDetails,
      violationDetails,
      vulnerabilityDetailsLoading: true,
    })
      .find(LoadWrapper)
      .find(SecurityVulnerabilityDetailsTile);

    expect(tile).toExist();
    expect(tile.prop('vulnerabilityDetails')).toBe(vulnerabilityDetails);
    expect(tile.prop('loading')).toBe(true);
    expect(tile.prop('componentName')).toBe('a : b : c');
  });
});
