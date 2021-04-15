/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import IqOrgAppPicker from '../../../main/frontend/components/iqOrgAppPicker/IqOrgAppPicker';
import { NxStatefulTreeViewMultiSelect } from '@sonatype/react-shared-components';
import * as enzymeUtils from '../enzymeUtils';

describe('IqOrgAppPicker', function () {
  let getShallowComponent,
    getMountedComponent,
    mountedComponent,
    mockOrganizations,
    mockApplications,
    mockSelectedOrganizations,
    mockSelectedApplications,
    onChangeSpy,
    wrapper,
    minimalProps;

  mockOrganizations = [
    { id: 'fooOrg', name: 'Foo Org' },
    { id: 'barOrg', name: 'Bar Org' },
    { id: 'bazOrg', name: 'Baz Org' },
  ];

  mockApplications = [
    { id: 'fooApp1', name: 'Foo App 1', organizationId: 'fooOrg' },
    { id: 'fooApp2', name: 'Foo App 2', organizationId: 'fooOrg' },
    { id: 'barApp1', name: 'Bar App 1', organizationId: 'barOrg' },
    { id: 'barApp2', name: 'Bar App 2', organizationId: 'barOrg' },
  ];

  mockSelectedOrganizations = new Set();
  mockSelectedApplications = new Set();

  beforeEach(function () {
    onChangeSpy = jasmine.createSpy('onChange');

    minimalProps = {
      applications: mockApplications,
      organizations: mockOrganizations,
      selectedOrganizations: mockSelectedOrganizations,
      selectedApplications: mockSelectedApplications,
      onChange: onChangeSpy,
    };
    getShallowComponent = enzymeUtils.getShallowComponent(IqOrgAppPicker, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(IqOrgAppPicker, minimalProps);
    wrapper = getShallowComponent(minimalProps);
  });

  afterEach(function () {
    if (mountedComponent) {
      mountedComponent.unmount();
    }

    mountedComponent = null;
  });

  it('returns 2 NxStatefulTreeViewMultiSelect components', () => {
    expect(wrapper.find(NxStatefulTreeViewMultiSelect).length).toBe(2);
  });

  it('sets selectedIds from passed-in selected prop', () => {
    const selectedOrganizations = new Set(['mock']);
    const selectedApplications = new Set(['mockApp']);

    const selectedIdsWrapper = getShallowComponent({
      ...minimalProps,
      selectedOrganizations,
      selectedApplications,
    });
    const orgMultiSelect = selectedIdsWrapper.find(NxStatefulTreeViewMultiSelect).at(0);
    const appMultiSelect = selectedIdsWrapper.find(NxStatefulTreeViewMultiSelect).at(1);
    expect(orgMultiSelect).toHaveProp('selectedIds', new Set(['mock']));
    expect(appMultiSelect).toHaveProp('selectedIds', new Set(['mockApp']));
  });

  describe('when all orgs are deselected (none)', function () {
    describe('when all orgs are selected (all)', function () {
      it('selects all apps', function () {
        mountedComponent = getMountedComponent(new Set(), new Set());

        const orgMultiSelect = mountedComponent.find(NxStatefulTreeViewMultiSelect).at(0);
        const orgAllNone = orgMultiSelect.find('.nx-checkbox__input').at(0);
        orgAllNone.simulate('change');

        expect(onChangeSpy).toHaveBeenCalledWith(
          new Set(['fooOrg', 'barOrg', 'bazOrg']),
          new Set(['fooApp1', 'fooApp2', 'barApp1', 'barApp2'])
        );
      });

      it('deselects all apps when you use all/none to unselect all orgs', function () {
        const selectedOrganizations = new Set(['fooOrg', 'barOrg', 'bazOrg']);
        const selectedApplications = new Set(['fooApp1', 'fooApp2', 'barApp1', 'barApp2']);
        mountedComponent = getMountedComponent({
          ...minimalProps,
          selectedOrganizations,
          selectedApplications,
        });

        const orgMultiSelect = mountedComponent.find(NxStatefulTreeViewMultiSelect).at(0);
        const orgAllNone = orgMultiSelect.find('.nx-checkbox__input').at(0);
        orgAllNone.simulate('change');
        expect(onChangeSpy).toHaveBeenCalledWith(new Set(), new Set());
      });
    });
  });

  describe('when an org is selected', function () {
    it('selects related apps', function () {
      const selectedOrganizations = new Set();
      const selectedApplications = new Set(['fooApp2', 'barApp1']);
      mountedComponent = getMountedComponent({
        ...minimalProps,
        selectedOrganizations,
        selectedApplications,
      });

      const orgMultiSelect = mountedComponent.find(NxStatefulTreeViewMultiSelect).at(0);
      const fooOrg = orgMultiSelect.find('.nx-checkbox__input').at(1);
      fooOrg.simulate('change');

      const newSelectedOrganizations = new Set(['fooOrg']);
      const expectedSelectedApplications = new Set(['fooApp1', 'fooApp2', 'barApp1']);

      expect(onChangeSpy).toHaveBeenCalledWith(newSelectedOrganizations, expectedSelectedApplications);
    });
  });

  describe('when an org is toggled to be deselected', function () {
    describe('when all related apps are selected', function () {
      it('deselects related apps', function () {
        const selectedOrganizations = new Set(['fooOrg']);
        const selectedApplications = new Set(['fooApp1', 'fooApp2', 'barApp1']);
        mountedComponent = getMountedComponent({
          ...minimalProps,
          selectedOrganizations,
          selectedApplications,
        });

        const orgMultiSelect = mountedComponent.find(NxStatefulTreeViewMultiSelect).at(0);
        const fooOrg = orgMultiSelect.find('.nx-checkbox__input').at(1);
        fooOrg.simulate('change');

        const newSelectedOrganizations = new Set();
        const expectedSelectedApplications = new Set(['barApp1']);

        expect(onChangeSpy).toHaveBeenCalledWith(newSelectedOrganizations, expectedSelectedApplications);
      });
    });
  });

  describe('when an org is not selected and was not toggled', function () {
    // this is to fix CLM-8852
    describe('when all related apps are selected', function () {
      it('does not deselect related apps', function () {
        const selectedOrganizations = new Set();
        const selectedApplications = new Set(['fooApp1', 'fooApp2']);
        mountedComponent = getMountedComponent({
          ...minimalProps,
          selectedOrganizations,
          selectedApplications,
        });

        const newSelectedOrganizations = new Set(['barOrg']);
        const expectedSelectedApplications = new Set(['fooApp1', 'fooApp2', 'barApp1', 'barApp2']);

        const orgMultiSelect = mountedComponent.find(NxStatefulTreeViewMultiSelect).at(0);
        const barOrg = orgMultiSelect.find('.nx-checkbox__input').at(2);
        barOrg.simulate('change');

        expect(onChangeSpy).toHaveBeenCalledWith(newSelectedOrganizations, expectedSelectedApplications);
      });
    });
  });

  describe('changing applications', function () {
    it('does not select an org when all related apps are selected', function () {
      const selectedApplications = new Set(['fooApp1']);
      const selectedOrganizations = new Set();
      mountedComponent = getMountedComponent({
        ...minimalProps,
        selectedOrganizations,
        selectedApplications,
      });

      const newSelectedApplications = new Set(['fooApp1', 'fooApp2']);
      const expectedSelectedOrganizations = new Set();

      const appMultiSelect = mountedComponent.find(NxStatefulTreeViewMultiSelect).at(1);
      const fooApp2 = appMultiSelect.find('.nx-checkbox__input').at(2);
      fooApp2.simulate('change');

      expect(onChangeSpy).toHaveBeenCalledWith(expectedSelectedOrganizations, newSelectedApplications);
    });

    it('deselects an org if not all related apps are selected', function () {
      const selectedApplications = new Set(['fooApp1', 'fooApp2', 'barApp1', 'barApp2']);
      const selectedOrganizations = new Set(['fooOrg', 'barOrg']);
      mountedComponent = getMountedComponent({
        ...minimalProps,
        selectedOrganizations,
        selectedApplications,
      });

      const newSelectedApplications = new Set(['fooApp1', 'barApp1', 'barApp2']);
      const expectedSelectedOrganizations = new Set(['barOrg']);

      const appMultiSelect = mountedComponent.find(NxStatefulTreeViewMultiSelect).at(1);
      const fooApp2 = appMultiSelect.find('.nx-checkbox__input').at(2);
      fooApp2.simulate('change');

      expect(onChangeSpy).toHaveBeenCalledWith(expectedSelectedOrganizations, newSelectedApplications);
    });
  });
});
