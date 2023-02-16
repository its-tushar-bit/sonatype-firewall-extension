/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxStatefulForm, NxWarningAlert } from '@sonatype/react-shared-components';

import AddSuccessMetricsReport from '../../../../../main/frontend/labs/successMetrics/addSuccessMetricsReport/AddSuccessMetricsReport';
import IqOrgAppPicker from '../../../../../main/frontend/components/iqOrgAppPicker/IqOrgAppPicker';

import * as enzymeUtils from '../../../enzymeUtils';

describe('AddSuccessMetricsReport', () => {
  let getShallowComponent, minimalProps, mockDismiss, mockClose, mockSubmit, mockLoadOrgsAndApps, modalContainer;

  beforeEach(() => {
    mockDismiss = jasmine.createSpy('dismiss');
    mockClose = jasmine.createSpy('close');
    mockSubmit = jasmine.createSpy('submit');
    mockLoadOrgsAndApps = jasmine.createSpy('loadOrgsAndApps');

    minimalProps = {
      dismiss: mockDismiss,
      close: mockClose,
      submit: mockSubmit,
      loadOrgsAndApps: mockLoadOrgsAndApps,
      applications: [],
      organizations: [],
      selectedOrgsAndApps: {
        organizations: new Set([]),
        applications: new Set([]),
      },
      reportName: {
        trimmedValue: '',
      },
      ownersMap: {
        ROOT_ORGANIZATION_ID: {
          type: 'organization',
          id: 'ROOT_ORGANIZATION_ID',
          name: 'Root Organization',
          synthetic: true,
          parentOrganizationId: null,
          applicationIds: null,
          subOrgs: 0,
          totalApps: 0,
          organizationIds: [],
        },
      },
      topParentOrganizationId: 'ROOT_ORGANIZATION_ID',
    };

    getShallowComponent = enzymeUtils.getShallowComponent(AddSuccessMetricsReport, minimalProps);
    modalContainer = document.createElement('div');
    document.body.appendChild(modalContainer);
  });

  afterEach(() => {
    if (modalContainer) {
      document.body.removeChild(modalContainer);
      modalContainer = null;
    }
  });

  describe('on load', () => {
    it('calls loadOrgsAndApps', () => {
      const getMountedComponent = enzymeUtils.getMountedComponent(AddSuccessMetricsReport, minimalProps, {
        attachTo: modalContainer,
      });
      const mountedComponent = getMountedComponent();
      expect(mockLoadOrgsAndApps).toHaveBeenCalledTimes(1);
      mountedComponent.unmount();
    });
  });

  it('shows a warning alter when includeLatestData is true', () => {
    const shallowComponent = getShallowComponent({ includeLatestData: true });
    const alert = shallowComponent.find(NxWarningAlert);
    expect(alert).toExist();
  });

  it('shows IqOrgAppPicker when isAllApplications is false', () => {
    const shallowComponent = getShallowComponent({ isAllApplications: false });
    const orgAppPicker = shallowComponent.find(IqOrgAppPicker);
    expect(orgAppPicker).toExist();
  });

  it('calls cancel function on form cancel', () => {
    const shallowComponent = getShallowComponent();
    const form = shallowComponent.find(NxStatefulForm);
    form.simulate('cancel');
    expect(mockDismiss).toHaveBeenCalledTimes(1);
  });

  it('calls submit function on form submit', () => {
    const shallowComponent = getShallowComponent();
    const form = shallowComponent.find(NxStatefulForm);
    form.simulate('submit');
    expect(mockSubmit).toHaveBeenCalledTimes(1);
  });

  it('sets false to NxStatefulForm loading prop', () => {
    const shallowComponent = getShallowComponent({ loading: false });
    const form = shallowComponent.find(NxStatefulForm);
    expect(form).toHaveProp('loading', false);
  });

  it('sets an error to NxStatefulForm loadError prop', () => {
    const loadError = 'some error happened';
    const shallowComponent = getShallowComponent({ loadError });
    const form = shallowComponent.find(NxStatefulForm);
    expect(form).toHaveProp('loadError', loadError);
  });

  it('sets an error to NxStatefulForm submitError prop', () => {
    const submitError = 'some error happened';
    const shallowComponent = getShallowComponent({ submitError });
    const form = shallowComponent.find(NxStatefulForm);
    expect(form).toHaveProp('submitError', submitError);
  });

  it('sets false to NxStatefulForm submitMaskState prop', () => {
    const shallowComponent = getShallowComponent({ submitMaskState: false });
    const form = shallowComponent.find(NxStatefulForm);
    expect(form).toHaveProp('submitMaskState', false);
  });

  it('has validationErrors prop due to reportName empty', () => {
    const shallowComponent = getShallowComponent({ reportName: { trimmedValue: '' } });
    const form = shallowComponent.find(NxStatefulForm);
    expect(form).toHaveProp('validationErrors', 'Unable to submit: fields with invalid or missing data.');
  });

  it('has validationErrors prop due to organizations selected and applications selected empty', () => {
    const shallowComponent = getShallowComponent({
      selectedOrgsAndApps: { organizations: new Set([]), applications: new Set([]) },
    });
    const form = shallowComponent.find(NxStatefulForm);
    expect(form).toHaveProp('validationErrors', 'Unable to submit: fields with invalid or missing data.');
  });

  it('does not have validationErrors', () => {
    const shallowComponent = getShallowComponent({
      selectedOrgsAndApps: { organizations: new Set([{}]), applications: new Set([{}]) },
      reportName: { trimmedValue: 'REPORT-NAME' },
    });
    const form = shallowComponent.find(NxStatefulForm);
    expect(form).toHaveProp('validationErrors', null);
  });
});
