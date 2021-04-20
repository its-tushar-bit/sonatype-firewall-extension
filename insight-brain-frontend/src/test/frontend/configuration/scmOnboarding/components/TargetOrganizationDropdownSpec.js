/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import * as enzymeUtils from '../../../enzymeUtils';
import TargetOrganizationDropdown from '../../../../../main/frontend/configuration/scmOnboarding/components/TargetOrganizationDropdown';
import DropdownFilterInput from '../../../../../main/frontend/configuration/scmOnboarding/components/DropdownFilterInput';
import { createOrg } from './utils';

describe('TargetOrganizationDropdown', () => {
  let getShallowComponent;

  beforeEach(() => {
    const mock$State = jasmine.createSpyObj('$state', ['href']);
    mock$State.href.and.returnValue('routerUrl');

    const minimalProps = { $state: mock$State, organizations: [] };

    getShallowComponent = enzymeUtils.getShallowComponent(TargetOrganizationDropdown, minimalProps);
  });

  describe('handles loading state', () => {
    const organizations = [createOrg('a'), createOrg('b')];

    it('displays loading in label', () => {
      // when loadingOrganizations flag is set
      const component = getShallowComponent({ loadingOrganizations: true }),
        filterInput = component.find(DropdownFilterInput);

      // then filter label contains loading indicator
      expect(filterInput.props().label).toEqual('Loading...');
    });

    it('displays selected org', () => {
      // when loadingOrganizations flag not is set and there is a organization
      const selectedOrganization = organizations[1];
      const component = getShallowComponent({ loadingOrganizations: false, organizations, selectedOrganization }),
        filterInput = component.find(DropdownFilterInput);

      // then filter labels contains selected organisation
      expect(filterInput.props().label).toEqual('org-b');
    });

    it('displays select in label', () => {
      // when loadingOrganizations flag not is set and there are no organizations
      const component = getShallowComponent(),
        filterInput = component.find(DropdownFilterInput);

      // then filter labels contains selected organisation
      expect(filterInput.props().label).toEqual('Select');
    });
  });

  describe('opens and collapses dropdown', () => {
    it('opens dropdown', () => {
      // given component with initial open state being false
      const component = getShallowComponent(),
        filterInput = component.find(DropdownFilterInput);
      expect(filterInput.props().isOpen).toEqual(false);

      // when handler is invoked
      filterInput.invoke('onToggleCollapse')();

      // then open state changes
      expect(component.find(DropdownFilterInput).props().isOpen).toEqual(true);
    });

    it('collapses dropdown', () => {
      // given component with initial open state being true
      const component = getShallowComponent(),
        filterInput = component.find(DropdownFilterInput);
      filterInput.invoke('onToggleCollapse')();
      expect(component.find(DropdownFilterInput).props().isOpen).toEqual(true);

      // when handler is invoked
      component.find(DropdownFilterInput).invoke('onToggleCollapse')();

      // then open state changes
      expect(component.find(DropdownFilterInput).props().isOpen).toEqual(false);
    });
  });

  describe('shows organization list', () => {
    const organizations = [createOrg('a'), createOrg('b')];

    it('displays list of organizations with correct properties', () => {
      // given organizations are present
      const selectedOrganization = organizations[0];
      const component = getShallowComponent({ loadingOrganizations: false, organizations, selectedOrganization }),
        filterInput = component.find(DropdownFilterInput);

      /* eslint-disable react/jsx-key */
      // then filter labels contains selected organisations
      expect(
        filterInput.contains([
          <a
            href="routerUrl"
            className="nx-dropdown-button iq-scm-onboarding-dropdown__option iq-scm-onboarding-dropdown__option--selected"
          >
            org-a
          </a>,
          <a href="routerUrl" className="nx-dropdown-button iq-scm-onboarding-dropdown__option">
            org-b
          </a>,
        ])
      ).toEqual(true);
    });
  });

  describe('filters organization list', () => {
    const organizations = [createOrg('a'), createOrg('b')];

    it('displays list of organizations with correct properties', () => {
      // given organizations are present
      const selectedOrganization = organizations[0];
      const component = getShallowComponent({ loadingOrganizations: false, organizations, selectedOrganization }),
        filterInput = component.find(DropdownFilterInput),
        orgA = filterInput.find('a').first(),
        orgB = filterInput.find('a').last();

      // expect filter function to return expected values
      expect(filterInput.invoke('filterFn')(orgA.getElement(), 'a')).toEqual(true);
      expect(filterInput.invoke('filterFn')(orgB.getElement(), 'b')).toEqual(true);
      expect(filterInput.invoke('filterFn')(orgA.getElement(), 'A')).toEqual(true);
      expect(filterInput.invoke('filterFn')(orgB.getElement(), 'B')).toEqual(true);
      expect(filterInput.invoke('filterFn')(orgB.getElement(), 'notmatching')).toEqual(false);
      expect(filterInput.invoke('filterFn')(orgA.getElement(), 'notmatching')).toEqual(false);
    });
  });
});
