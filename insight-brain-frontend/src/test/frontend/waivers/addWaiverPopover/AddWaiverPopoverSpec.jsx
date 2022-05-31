/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxSubmitMask } from '@sonatype/react-shared-components';

import * as enzymeUtils from '../../enzymeUtils';
import { nxDateInputStateHelpers } from '@sonatype/react-shared-components';
import LoadWrapper from '../../../../main/frontend/react/LoadWrapper';
import AddWaiverForm from '../../../../main/frontend/waivers/AddWaiverForm';
import AddWaiverPopover from '../../../../main/frontend/waivers/addWaiverPopover/AddWaiverPopover';
import IqPopover from '../../../../main/frontend/react/IqPopover/IqPopover';

describe('AddWaiverPopover', function () {
  let minimalProps, openVulnerabilityDetailsModalMock, loadAddWaiverDataSpy, getShallowComponent, getMountedComponent;

  beforeEach(function () {
    loadAddWaiverDataSpy = jasmine.createSpy('loadAddWaiverDataSpy');

    openVulnerabilityDetailsModalMock = jasmine.createSpy('openVulnerabilityDetailsModal').and.returnValue({
      type: 'OPEN_VULNERABILITY_DETAILS_MODAL',
    });

    minimalProps = {
      loading: false,
      onClose: () => {},
      violationId: 'violationId',
      waiverComments: {
        value: '',
        isPristine: true,
      },
      expiryTime: null,
      customExpiryTime: nxDateInputStateHelpers.initialState(''),
      loadAddWaiverData: loadAddWaiverDataSpy,
      saveWaiver: () => {},
      setWaiverComment: () => {},
      setWaiverScope: () => {},
      setApplyToAllComponents: () => {},
      setExpiryTime: () => {},
      cancelAction: () => {},
    };

    getShallowComponent = enzymeUtils.getShallowComponent(AddWaiverPopover, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(AddWaiverPopover, minimalProps);
  });

  it('renders a component', function () {
    expect(getShallowComponent()).toExist();
  });

  it('renders an IqPopover', () => {
    const el = getShallowComponent();
    const popover = el.find(IqPopover);
    expect(popover).toExist();
    expect(popover).toHaveProp('size', 'large');
  });

  it('renders an IqPopover header with close button', () => {
    const el = getShallowComponent();
    const header = el.find(IqPopover.Header);
    expect(header).toHaveProp('headerTitle', 'Add Waiver');
    expect(header).toHaveProp('onClose', jasmine.any(Function));
  });

  it('renders a loading LoadWrapper when loading is true', function () {
    const component = getShallowComponent({ loading: true });
    const loadWrapper = component.find(LoadWrapper);
    expect(loadWrapper).toHaveProp('loading', true);
  });

  it('renders a loading LoadWrapper when the violationDetails prop is missing', function () {
    const component = getShallowComponent({ violationDetails: null });
    const loadWrapper = component.find(LoadWrapper);
    expect(loadWrapper).toHaveProp('loading', true);
  });

  it('renders a loading LoadWrapper when the availableWaiverScopes are missing', function () {
    const component = getShallowComponent({ availableWaiverScopes: null });
    const loadWrapper = component.find(LoadWrapper);
    expect(loadWrapper).toHaveProp('loading', true);
  });

  it('calls loadAddWaiverData when the LoadWrapper retryHandler is invoked', function () {
    const loadWrapper = getShallowComponent().find(LoadWrapper),
      retryHandler = loadWrapper.prop('retryHandler');

    expect(loadAddWaiverDataSpy).not.toHaveBeenCalled();

    retryHandler();

    expect(loadAddWaiverDataSpy).toHaveBeenCalledWith('violationId');
  });

  it('renders the AddWaiverForm with props if the page is not loading', function () {
    const fullProps = {
      violationId: 'policyViolationId',
      loading: false,
      onClose: () => {},
      violationDetails: {
        filename: 'filename',
        constraintViolations: [
          {
            constraintName: 'constraint name',
            reasons: [
              {
                reason: 'reason',
                reference: {
                  value: 'CVE-67890',
                },
              },
            ],
          },
        ],
        policyName: 'policyName',
        policyViolationId: 'policyViolationId',
        threatLevel: 5,
      },
      openVulnerabilityDetailsModal: openVulnerabilityDetailsModalMock,
      applyToAllComponents: true,
      expiryTime: '7',
      availableWaiverScopes: [{ id: 'id', name: 'name', label: 'Application', type: 'application' }],
      selectedWaiverScope: {
        id: 'id',
        name: 'name',
        label: 'Application',
        type: 'application',
      },
    };
    const component = getShallowComponent(fullProps),
      loadWrapperChildren = enzymeUtils.getLoadWrapperChildren(component),
      addWaiverForm = loadWrapperChildren.find(AddWaiverForm);

    expect(addWaiverForm).toHaveProp('applyToAllComponents', true);
    expect(addWaiverForm).toHaveProp('artifactName', 'filename');
    expect(addWaiverForm).toHaveProp('componentName', 'filename');
    expect(addWaiverForm).toHaveProp('policyName', 'policyName');
    expect(addWaiverForm).toHaveProp('policyViolationId', 'policyViolationId');
    expect(addWaiverForm).toHaveProp('reasons', ['reason']);
    expect(addWaiverForm).toHaveProp('threatLevelCategory', 'severe');
    expect(addWaiverForm).toHaveProp('constraintName', 'constraint name');
    expect(addWaiverForm).toHaveProp('availableWaiverScopes', fullProps.availableWaiverScopes);
    expect(addWaiverForm).toHaveProp('selectedWaiverScope', fullProps.selectedWaiverScope);
    expect(addWaiverForm).toHaveProp('setWaiverScope', minimalProps.setWaiverScope);
    expect(addWaiverForm).toHaveProp('setWaiverComment', minimalProps.setWaiverComment);
    expect(addWaiverForm).toHaveProp('setApplyToAllComponents', minimalProps.setApplyToAllComponents);
    expect(addWaiverForm).toHaveProp('saveWaiver', minimalProps.saveWaiver);
    expect(addWaiverForm).toHaveProp('openVulnerabilityDetailsModal', openVulnerabilityDetailsModalMock);
    expect(addWaiverForm).toHaveProp('vulnerabilityId', 'CVE-67890');
    expect(addWaiverForm).toHaveProp('expiryTime', fullProps.expiryTime);
    expect(addWaiverForm).toHaveProp('setExpiryTime', minimalProps.setExpiryTime);
  });

  it('renders NxSubmitMask with success message when submitMaskState is true', function () {
    const component = getShallowComponent({ submitMaskState: true });
    const nxSubmitMask = component.find(NxSubmitMask);
    expect(nxSubmitMask).toExist();
    expect(nxSubmitMask).toHaveProp('success', true);
    expect(nxSubmitMask).toHaveProp('message', 'Creating waiver…');
    expect(nxSubmitMask).toHaveProp('successMessage', 'Success!');
  });

  it('renders NxSubmitMask with loading message when submitMaskState is false', function () {
    const component = getShallowComponent({ submitMaskState: false });
    const nxSubmitMask = component.find(NxSubmitMask);
    expect(nxSubmitMask).toExist();
    expect(nxSubmitMask).toHaveProp('success', false);
    expect(nxSubmitMask).toHaveProp('message', 'Creating waiver…');
    expect(nxSubmitMask).toHaveProp('successMessage', 'Success!');
  });

  it('does not renders NxSubmitMask when submitMaskState is null', function () {
    const component = getShallowComponent({ submitMaskState: null });
    const nxSubmitMask = component.find(NxSubmitMask);
    expect(nxSubmitMask).not.toExist();
  });

  it('calls `loadAddWaiverData` with the violationId', function () {
    getMountedComponent();
    expect(loadAddWaiverDataSpy).toHaveBeenCalledWith('violationId');
  });

  it('calls `loadAddWaiverData` if the violationId changes', function () {
    const component = getMountedComponent();

    expect(loadAddWaiverDataSpy).toHaveBeenCalledTimes(1);
    expect(loadAddWaiverDataSpy).toHaveBeenCalledWith('violationId');

    component.setProps({
      ...minimalProps,
      violationId: 'violationId2',
    });
    expect(loadAddWaiverDataSpy).toHaveBeenCalledTimes(2);
    expect(loadAddWaiverDataSpy.calls.argsFor(1)[0]).toEqual('violationId2');
  });
});
