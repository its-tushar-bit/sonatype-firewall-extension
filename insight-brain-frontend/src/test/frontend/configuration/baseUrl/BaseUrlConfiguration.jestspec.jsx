/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import '../../SpecUtil';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import BaseUrlConfiguration from 'MainRoot/configuration/baseUrl/BaseUrlConfiguration';

import * as RouterStateContext from 'MainRoot/react/RouterStateContext';
import * as baseUrlConfigurationSelectors from 'MainRoot/configuration/baseUrl/baseUrlConfigurationSelectors';
import { render, fireEvent, screen, waitFor } from 'TestRoot/SpecUtil';
import { getConfigurationUrl } from 'MainRoot/util/CLMLocation';
import { getGlobalPermissionTestUrl } from 'MainRoot/util/CLMContextLocation';

describe('BaseUrlConfiguration', () => {
  let renderComponent;
  let axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });
  const globalPermissionTestUrl = getGlobalPermissionTestUrl();
  const filledFormState = {
    baseUrl: { value: 'someUrl', isPristine: false, trimmedValue: 'someUrl', validationErrors: null },
  };

  const cleanFormState = {
    baseUrl: { value: '', isPristine: true, trimmedValue: '', validationErrors: null },
  };

  const serverData = {
    baseUrl: 'http://localhost:8070',
  };

  const baseUrlConfigurationParameters = {};

  beforeEach(() => {
    jest
      .spyOn(baseUrlConfigurationSelectors, 'selectBaseUrlConfigurationSlice')
      .mockReturnValue(baseUrlConfigurationParameters);
    jest.spyOn(RouterStateContext, 'useRouterState').mockReturnValue({
      get: jest.fn(),
      href: jest.fn(),
    });
    renderComponent = () => render(<BaseUrlConfiguration />);
  });

  describe('componment load', () => {
    it('passes error when a load error exists', () => {
      const errorMessage = 'Error on page load';
      jest.spyOn(baseUrlConfigurationSelectors, 'selectLoadError').mockImplementation(() => errorMessage);
      renderComponent();
      expect(screen.getByText(new RegExp('Error on page load'))).toBeVisible();
    });

    it('renders form when page load successfully', function () {
      jest.spyOn(baseUrlConfigurationSelectors, 'selectLoading').mockImplementation(() => false);
      renderComponent();
      expect(screen.getByRole('heading', { name: 'Base URL' })).toBeVisible();
      expect(screen.getByRole('heading', { name: 'Configure Base URL' })).toBeVisible();
      expect(screen.getByRole('textbox', { name: 'Base URL' })).toBeVisible();
      expect(screen.getByRole('textbox', { name: 'Base URL' })).toBeRequired();
      expect(screen.getByRole('button', { name: 'Delete Configuration' })).toBeVisible();
      expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
      expect(screen.getByRole('button', { name: 'Save Configuration' })).toBeVisible();
    });

    it('inputs filled when serverData', function () {
      jest.spyOn(baseUrlConfigurationSelectors, 'selectLoading').mockImplementation(() => false);
      jest.spyOn(baseUrlConfigurationSelectors, 'selectFormState').mockImplementation(() => filledFormState);
      renderComponent();
      expect(screen.getByRole('textbox', { name: 'Base URL' })).toHaveValue(filledFormState.baseUrl.value);
    });
  });

  describe('on Save Configuration Button', function () {
    it('calls update when the form is submitted', async () => {
      // Mock axios calls using axiosMockAdapter
      axiosMock.onPut(globalPermissionTestUrl).reply(200, ['CONFIGURE_SYSTEM']);
      axiosMock.onPut(getConfigurationUrl()).reply(200, {});
      jest.spyOn(baseUrlConfigurationSelectors, 'selectFormState').mockImplementation(() => filledFormState);
      jest.spyOn(baseUrlConfigurationSelectors, 'selectIsDirty').mockImplementation(() => true);
      jest.spyOn(baseUrlConfigurationSelectors, 'selectHasAllRequiredFields').mockImplementation(() => true);
      renderComponent();
      await waitFor(() => screen.getByText('Save Configuration'));
      expect(screen.getByRole('button', { name: 'Save Configuration' })).toBeVisible();
      fireEvent.click(screen.getByText('Save Configuration'));
      await waitFor(() => {
        expect(axiosMock.history.put.length).toBe(1);
      });
    });

    it('shows correct form alert when no data provided', async () => {
      jest.spyOn(baseUrlConfigurationSelectors, 'selectFormState').mockImplementation(() => cleanFormState);
      jest.spyOn(baseUrlConfigurationSelectors, 'selectIsDirty').mockImplementation(() => false);
      renderComponent();
      await waitFor(() => screen.getByText('Save Configuration'));
      fireEvent.click(screen.getByText('Save Configuration'));
      expect(screen.getByRole('alert')).toHaveTextContent('There are no changes to update.');
    });

    it('shows correct form alert when data partially provided', async () => {
      jest.spyOn(baseUrlConfigurationSelectors, 'selectFormState').mockImplementation(() => cleanFormState);
      jest.spyOn(baseUrlConfigurationSelectors, 'selectIsDirty').mockImplementation(() => true);
      renderComponent();
      await waitFor(() => screen.getByText('Save Configuration'));
      fireEvent.click(screen.getByText('Save Configuration'));
      expect(screen.getByRole('alert')).toHaveTextContent('Base URL is required data.');
    });
  });

  describe('on Cancel button', function () {
    it('cancel button is disabled when the form is clean', async () => {
      jest.spyOn(baseUrlConfigurationSelectors, 'selectFormState').mockImplementation(() => cleanFormState);
      jest.spyOn(baseUrlConfigurationSelectors, 'selectIsDirty').mockImplementation(() => false);
      renderComponent();
      await waitFor(() => screen.getByText('Cancel'));
      expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
      expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
    });

    it('keep cancel button enabled when the form is filled', async () => {
      jest.spyOn(baseUrlConfigurationSelectors, 'selectFormState').mockImplementation(() => filledFormState);
      jest.spyOn(baseUrlConfigurationSelectors, 'selectIsDirty').mockImplementation(() => true);
      renderComponent();
      await waitFor(() => screen.getByText('Cancel'));
      expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
    });
  });

  describe('on Delete button', function () {
    it('disabled Delete button when the form is clean', async () => {
      jest.spyOn(baseUrlConfigurationSelectors, 'selectFormState').mockImplementation(() => cleanFormState);
      jest.spyOn(baseUrlConfigurationSelectors, 'selectIsDirty').mockImplementation(() => false);
      renderComponent();
      await waitFor(() => screen.getByText('Delete Configuration'));
      expect(screen.getByRole('button', { name: 'Delete Configuration' })).toHaveAttribute('aria-disabled', 'true');
    });

    it('disable Delete button when the form is filled but serverData is clean', async () => {
      jest.spyOn(baseUrlConfigurationSelectors, 'selectFormState').mockImplementation(() => filledFormState);
      jest.spyOn(baseUrlConfigurationSelectors, 'selectIsDirty').mockImplementation(() => true);
      jest.spyOn(baseUrlConfigurationSelectors, 'selectServerData').mockImplementation(() => null);
      renderComponent();
      await waitFor(() => screen.getByText('Delete Configuration'));
      expect(screen.getByRole('button', { name: 'Delete Configuration' })).toHaveAttribute('aria-disabled', 'true');
    });

    it('enable Delete button when the form is filled with serverData', async () => {
      jest.spyOn(baseUrlConfigurationSelectors, 'selectFormState').mockImplementation(() => filledFormState);
      jest.spyOn(baseUrlConfigurationSelectors, 'selectIsDirty').mockImplementation(() => false);
      jest.spyOn(baseUrlConfigurationSelectors, 'selectServerData').mockImplementation(() => serverData);
      renderComponent();
      await waitFor(() => screen.getByText('Delete Configuration'));
      expect(screen.getByRole('button', { name: 'Delete Configuration' })).toHaveAttribute('aria-disabled', 'false');
    });
  });
});
