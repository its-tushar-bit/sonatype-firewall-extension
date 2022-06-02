/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxToggle } from '@sonatype/react-shared-components';
import RoleEditorPermissionsList from '../../../../main/frontend/security/roleEditor/RoleEditorPermissionsList';
import * as enzymeUtils from '../../enzymeUtils';
import { render, screen } from 'TestRoot/SpecUtil';

describe('RoleEditorPermissionsList', () => {
  let props, mockToggleValue, getShallowComponent;

  beforeEach(() => {
    mockToggleValue = jasmine.createSpy('toggleValue');
    props = {
      permissionsList: [
        { allowed: true, id: 'PERMISSION_ID', description: 'PERMISSION', displayName: 'PERMISSION' },
        { allowed: false, id: 'PERMISSION_ID1', description: 'PERMISSION1', displayName: 'PERMISSION1' },
        { allowed: true, id: 'PERMISSION_ID2', description: 'PERMISSION2', displayName: 'PERMISSION2' },
      ],
      displayName: 'CATEGORY',
      toggleValue: mockToggleValue,
      readonly: false,
    };
    getShallowComponent = enzymeUtils.getShallowComponent(RoleEditorPermissionsList, props);
  });

  describe('click on toggle', () => {
    it('calls toggleValue', () => {
      const toggle = getShallowComponent().find(NxToggle).first();
      toggle.at(0).simulate('change');
      expect(mockToggleValue).toHaveBeenCalled();
    });
  });

  describe('divide permissions in columns', () => {
    it('has just one column', () => {
      const permissionsList = [
        { allowed: true, id: 'PERMISSION_ID', description: 'PERMISSION', displayName: 'PERMISSION' },
      ];
      const columns = getShallowComponent({ permissionsList }).find('.iq-role-editor-permission-group__col');
      const firstColumnToggles = columns.at(0).find(NxToggle);
      const secondColumnToggles = columns.at(1).find(NxToggle);
      expect(firstColumnToggles.length).toBe(1);
      expect(secondColumnToggles.length).toBe(0);
    });

    it('has two equal columns', () => {
      const permissionsList = [
        { allowed: true, id: 'PERMISSION_ID', description: 'PERMISSION', displayName: 'PERMISSION' },
        { allowed: true, id: 'PERMISSION_ID1', description: 'PERMISSION2', displayName: 'PERMISSION1' },
        { allowed: true, id: 'PERMISSION_ID2', description: 'PERMISSION3', displayName: 'PERMISSION2' },
        { allowed: true, id: 'PERMISSION_ID3', description: 'PERMISSION4', displayName: 'PERMISSION3' },
      ];
      const columns = getShallowComponent({ permissionsList }).find('.iq-role-editor-permission-group__col');
      const firstColumnToggles = columns.at(0).find(NxToggle);
      const secondColumnToggles = columns.at(1).find(NxToggle);
      expect(firstColumnToggles.length).toBe(2);
      expect(secondColumnToggles.length).toBe(2);
    });

    it('has its first column bigger than the second one', () => {
      const permissionsList = [
        { allowed: true, id: 'PERMISSION_ID', description: 'PERMISSION', displayName: 'PERMISSION' },
        { allowed: true, id: 'PERMISSION_ID1', description: 'PERMISSION2', displayName: 'PERMISSION1' },
        { allowed: true, id: 'PERMISSION_ID2', description: 'PERMISSION3', displayName: 'PERMISSION2' },
        { allowed: true, id: 'PERMISSION_ID3', description: 'PERMISSION4', displayName: 'PERMISSION3' },
        { allowed: true, id: 'PERMISSION_ID4', description: 'PERMISSION5', displayName: 'PERMISSION4' },
      ];
      const columns = getShallowComponent({ permissionsList }).find('.iq-role-editor-permission-group__col');
      const firstColumnToggles = columns.at(0).find(NxToggle);
      const secondColumnToggles = columns.at(1).find(NxToggle);
      expect(firstColumnToggles.length).toBe(3);
      expect(secondColumnToggles.length).toBe(2);
    });
  });

  describe('description text', () => {
    it('keeps IQ in capital letters', () => {
      props.permissionsList = [
        { allowed: true, id: 'PERMISSION_ID1', displayName: 'Change', description: 'IQ ELEMENTS' },
        { allowed: true, id: 'PERMISSION_ID2', displayName: 'Edit', description: 'DESCRIPTION TEXT' },
      ];
      render(<RoleEditorPermissionsList {...props} />);
      expect(screen.getByText('Change IQ elements')).toBeInTheDocument();
      expect(screen.getByText('Edit description text')).toBeInTheDocument();
    });
  });
});
