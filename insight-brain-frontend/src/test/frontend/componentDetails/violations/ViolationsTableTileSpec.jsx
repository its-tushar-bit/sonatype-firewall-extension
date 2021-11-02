/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxButton, NxLoadWrapper } from '@sonatype/react-shared-components';

import * as enzymeUtils from '../../enzymeUtils';
import ViolationsTableTile, {
  ViewAllComponentWaiversButton,
  ViewTransitiveViolationsButton,
} from 'MainRoot/componentDetails/ViolationsTableTile/ViolationsTableTile';
import PolicyViolationsTable from 'MainRoot/componentDetails/ViolationsTableTile/PolicyViolationsTable';

describe('ViolationsTableTile', () => {
  let minimalProps, tableProps, getShallow, getMounted;

  beforeEach(function () {
    tableProps = {
      waivers: ['exampleWaiver'],
      violations: [
        {
          policyViolationId: 'policyViolationId',
          policyThreatLevel: 10,
          policyName: 'Security-Blocker',
          actions: [],
          constraints: [],
        },
      ],
      componentName: 'componentName',
      loadPolicyViolationsInformation: jasmine.createSpy('loadPolicyViolationsInformation'),
      loadError: null,
      loading: false,
      goToWaivers: jasmine.createSpy('goToWaivers'),
      showComponentWaiversPopover: false,
      toggleComponentWaiversPopover: jasmine.createSpy('toggleComponentWaiversPopover'),
      setWaiverToDelete: jasmine.createSpy('setWaiverToDelete'),
      showViolationsDetailPopover: false,
      toggleShowViolationsDetailPopover: jasmine.createSpy('toggleShowViolationsDetailPopover'),
      hasPermissionToAddWaivers: true,
      setSelectedPolicyViolationId: jasmine.createSpy('setSelectedPolicyViolationId'),
    };
    minimalProps = {
      ...tableProps,
      isLoadingComponentDetails: false,
      componentDetailsLoadError: null,
      loadComponentDetails: () => {},
      title: 'Title',
      showViewAllComponents: true,
      showViewTransitiveViolations: true,
      violationType: null,
      setViolationType: jasmine.createSpy('setViolationType'),
      stateGo: jasmine.createSpy('stateGo'),
      ownerType: 'someOwnerType',
      ownerId: 'someOwnerId',
      scanId: 'someScanId',
      hash: 'someHash',
    };

    getShallow = enzymeUtils.getShallowComponent(ViolationsTableTile, minimalProps);
    getMounted = enzymeUtils.getMountedComponent(ViolationsTableTile, minimalProps);
  });

  it('renders the title correctly', () => {
    const title = getShallow().find('#violations__tile__title');

    expect(title).toHaveText('Title');
  });

  it('calls setViolationType when the component renders', () => {
    const component = getMounted({ violationType: 'test' });
    expect(minimalProps.setViolationType).toHaveBeenCalledWith('test');
    component.unmount();
  });

  describe('View Transitive Violations button', () => {
    it('is rendered if `showViewTransitiveViolations` is true', () => {
      const component = getShallow();
      const button = component.find(ViewTransitiveViolationsButton).dive().find(NxButton);

      expect(button).toHaveProp('id', 'component-details-view-transitive-violations');
      expect(button).toHaveProp('variant', 'tertiary');
      expect(button).toHaveProp('onClick');
      expect(button).toHaveText('View Transitive Violations');
    });

    it('is not rendered if `showViewTransitiveViolations` is false', () => {
      const component = getShallow({ showViewTransitiveViolations: false });
      const button = component.find(ViewTransitiveViolationsButton);

      expect(button).not.toExist();
    });

    it('calls `stateGo` with the correct arguments when clicked', () => {
      const component = getShallow();
      const button = component.find(ViewTransitiveViolationsButton).dive().find(NxButton);

      button.simulate('click');
      expect(minimalProps.stateGo).toHaveBeenCalledWith('transitiveViolations', {
        ownerType: 'someOwnerType',
        ownerId: 'someOwnerId',
        scanId: 'someScanId',
        hash: 'someHash',
      });
    });
  });

  describe('View Existing Waivers button', () => {
    it('is rendered if `showViewAllComponents` is true', () => {
      const component = getShallow();
      const button = component.find(ViewAllComponentWaiversButton).dive().find(NxButton);

      expect(button).toHaveProp('id', 'component-details-view-waivers');
      expect(button).toHaveProp('variant', 'tertiary');
      expect(button).toHaveProp('onClick', minimalProps.toggleComponentWaiversPopover);
      expect(button).toHaveText('View Existing Waivers');
    });

    it('is not rendered if `showViewAllComponents` is false', () => {
      const component = getShallow({ showViewAllComponents: false });
      const button = component.find(ViewAllComponentWaiversButton);

      expect(button).not.toExist();
    });

    it('calls `toggleComponentWaiversPopover` when clicked', () => {
      const component = getShallow();
      const button = component.find(ViewAllComponentWaiversButton).dive().find(NxButton);

      button.simulate('click');
      expect(minimalProps.toggleComponentWaiversPopover).toHaveBeenCalled();
    });
  });

  it('renders a loading indicator if component details are loading', () => {
    const component = getShallow({ isLoadingComponentDetails: true });
    const loadWrapper = component.find(NxLoadWrapper);

    expect(loadWrapper).toHaveProp('loading', true);
    expect(loadWrapper).toHaveProp('error', minimalProps.componentDetailsLoadError);
    expect(loadWrapper).toHaveProp('retryHandler', minimalProps.loadComponentDetails);
  });

  it('renders an PolicyViolationsTable component passing the appropriate props if component details are loaded', () => {
    const getTable = (el) => {
      return el.find(NxLoadWrapper).dive().find(PolicyViolationsTable);
    };

    let violationsTable;
    violationsTable = getTable(getShallow({ isLoadingComponentDetails: false }));

    expect(violationsTable).toExist();
    expect(violationsTable).toHaveProp(tableProps);
  });
});
