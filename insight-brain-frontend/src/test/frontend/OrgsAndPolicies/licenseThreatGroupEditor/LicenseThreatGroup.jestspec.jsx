/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { actions } from 'MainRoot/OrgsAndPolicies/licenseThreatGroupSlice';
import LicenseThreatGroupEditor from 'MainRoot/OrgsAndPolicies/licenseThreatGroupEditor/LicenseThreatGroupEditor';
import * as ltgSelectors from 'MainRoot/OrgsAndPolicies/licenseThreatGroupSelectors';
import { render, screen, fireEvent } from 'TestRoot/SpecUtil';

import 'TestRoot/SpecUtil';

describe('LicenseThreatGroup', () => {
  let renderComponent,
    selectIsLoadingSpy,
    selectLicenseThreatGroupLoadErrorSpy,
    selectLicenseThreatGroupIsEditModeSpy,
    selectDirtyLicenseThreatGroupSpy,
    setLicenseThreatGroupNameSpy,
    setLicenseThreatGroupThreatLevelSpy,
    saveLicenseThreatGroupSpy,
    loadLicenseThreatGroupEditorSpy,
    removeLicenseThreatGroupSpy;

  beforeEach(() => {
    selectIsLoadingSpy = jest.spyOn(ltgSelectors, 'selectIsLoading').mockReturnValue(false);
    selectLicenseThreatGroupLoadErrorSpy = jest
      .spyOn(ltgSelectors, 'selectLicenseThreatGroupLoadError')
      .mockReturnValue(null);
    selectLicenseThreatGroupIsEditModeSpy = jest
      .spyOn(ltgSelectors, 'selectLicenseThreatGroupIsEditMode')
      .mockReturnValue(false);
    selectDirtyLicenseThreatGroupSpy = jest.spyOn(ltgSelectors, 'selectDirtyLicenseThreatGroup').mockReturnValue({
      name: {
        value: 'name',
        trimmedValue: 'name',
        validationError: null,
        isPristine: true,
      },
    });

    setLicenseThreatGroupNameSpy = jest.spyOn(actions, 'setLicenseThreatGroupName');
    setLicenseThreatGroupThreatLevelSpy = jest.spyOn(actions, 'setLicenseThreatGroupThreatLevel');
    saveLicenseThreatGroupSpy = jest.spyOn(actions, 'saveLicenseThreatGroup').mockReturnValue({
      type: 'licenseThreatGroup/saveLicenseThreatGroup/fulfilled',
      payload: {},
    });
    loadLicenseThreatGroupEditorSpy = jest.spyOn(actions, 'loadLicenseThreatGroupEditor').mockReturnValue({
      type: 'licenseThreatGroup/loadLicenseThreatGroupEditor/fulfilled',
      payload: {
        siblings: [],
        dirtyLTG: {
          name: {
            value: 'name',
            trimmedValue: 'name',
            validationError: null,
            isPristine: true,
          },
        },
      },
    });
    removeLicenseThreatGroupSpy = jest.spyOn(actions, 'removeLicenseThreatGroup');
    jest.spyOn(ltgSelectors, 'selectAvailableLicenses').mockReturnValue([]);

    renderComponent = () => render(<LicenseThreatGroupEditor />);
  });

  it('renders loading indicator', () => {
    selectIsLoadingSpy.mockReturnValue(true);
    renderComponent();

    expect(screen.getByText('Loading…')).toBeVisible();
  });

  it('renders error message on load error', () => {
    selectLicenseThreatGroupLoadErrorSpy.mockReturnValue('Load Error');
    renderComponent();

    const error = screen.getByRole('alert');

    expect(error).toBeVisible();
  });

  it('calls loadLicenseThreatGroupEditor on initial load', () => {
    renderComponent();

    expect(loadLicenseThreatGroupEditorSpy).toHaveBeenCalled();
  });

  it('calls setLicenseThreatGroupName handler on Group Name change', () => {
    renderComponent();

    const name = screen.getAllByRole('textbox')[0];
    fireEvent.change(name, { target: { value: 'group' } });

    expect(setLicenseThreatGroupNameSpy).toHaveBeenCalledWith('group');
  });

  it('sets inline error message if group name fails validation', () => {
    selectDirtyLicenseThreatGroupSpy.mockReturnValue({
      name: {
        value: '',
        trimmedValue: '',
        validationErrors: ['Must be non-empty'],
        isPristine: false,
      },
    });
    renderComponent();
    const alert = screen.getByText('Must be non-empty');
    expect(alert).toBeVisible();
  });

  it('calls setLicenseThreatGroupThreatLevel handler on threat level change', () => {
    renderComponent();
    const dropdown = screen.getAllByRole('button')[0];

    fireEvent.click(dropdown);

    const item = screen.getByText('10 - Critical');
    fireEvent.click(item);

    expect(setLicenseThreatGroupThreatLevelSpy).toHaveBeenCalledWith(10);
  });

  it('calls saveLicenseThreatGroup on save button click', () => {
    jest.spyOn(ltgSelectors, 'selectLicenseThreatGroupIsDirty').mockReturnValue(true);
    selectLicenseThreatGroupIsEditModeSpy.mockReturnValue(true);

    renderComponent();
    const update = screen.getByRole('button', { name: 'Update' });
    fireEvent.click(update);

    expect(saveLicenseThreatGroupSpy).toHaveBeenCalled();
  });

  describe('create mode', () => {
    it('renders tile with the correct page title', () => {
      renderComponent();

      expect(screen.getByText('New License Threat Group')).toBeVisible();
    });

    it('delete button is not present on the page', () => {
      renderComponent();

      expect(screen.queryByText('Delete')).not.toBeInTheDocument();
    });
  });

  describe('edit mode', () => {
    beforeEach(() => {
      selectLicenseThreatGroupIsEditModeSpy.mockReturnValue(true);
    });

    it('renders tile with the correct page title', () => {
      renderComponent();

      expect(screen.getByText('Edit License Threat Group')).toBeVisible();
    });

    it('calls removeLicenseThreatGroup on remove button click', () => {
      renderComponent();
      const remove = screen.getByRole('button', { name: 'Delete' });

      fireEvent.click(remove);
      expect(screen.getByText('Delete License Threat Group')).toBeVisible();
      const modalDeleteButton = screen.getAllByText('Delete')[1];

      fireEvent.click(modalDeleteButton);

      expect(removeLicenseThreatGroupSpy).toHaveBeenCalled();
    });
  });
});
