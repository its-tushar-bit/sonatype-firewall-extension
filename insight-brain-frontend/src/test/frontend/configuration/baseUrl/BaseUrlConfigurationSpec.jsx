/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import BaseUrlConfiguration from 'MainRoot/configuration/baseUrl/BaseUrlConfiguration';

import * as RouterStateContext from 'MainRoot/react/RouterStateContext';
import * as baseUrlConfigurationSelectors from 'MainRoot/configuration/baseUrl/baseUrlConfigurationSelectors';
import { render, fireEvent, screen, waitFor } from 'TestRoot/SpecUtil';
import { getConfigurationUrl } from 'MainRoot/util/CLMLocation';
import axios from 'axios';
import { getGlobalPermissionTestUrl } from 'MainRoot/utilAngular/CLMContextLocation';

describe('BaseUrlConfiguration', () => {
  let renderComponent;
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  const globalPermissionTestUrl = getGlobalPermissionTestUrl();
  const filledFormState = {
    baseUrl: { value: 'someUrl' },
  };

  const cleanFormState = {
    serverUrl: { value: '' },
  };

  const serverData = {
    serverUrl: 'http://localhost:8070',
  };

  const baseUrlConfigurationParameters = {};

  beforeEach(() => {
    spyOn(baseUrlConfigurationSelectors, 'selectBaseUrlConfigurationSlice').and.returnValue(
      baseUrlConfigurationParameters
    );
    spyOn(RouterStateContext, 'useRouterState').and.returnValue({
      get: jasmine.createSpy('useRouterState.get'),
      href: jasmine.createSpy('useRouterState.href'),
    });
    renderComponent = () => render(<BaseUrlConfiguration />);
  });

  describe('componment load', () => {
    it('passes error when a load error exists', () => {
      const errorMessage = 'Error on page load';
      spyOn(baseUrlConfigurationSelectors, 'selectLoadError').and.callFake(() => errorMessage);
      renderComponent();
      expect(screen.getByText(new RegExp('Error on page load'))).toBeVisible();
    });

    it('renders form when page load successfully', function () {
      spyOn(baseUrlConfigurationSelectors, 'selectLoading').and.callFake(() => false);
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
      spyOn(baseUrlConfigurationSelectors, 'selectLoading').and.callFake(() => false);
      spyOn(baseUrlConfigurationSelectors, 'selectFormState').and.callFake(() => filledFormState);
      renderComponent();
      expect(screen.getByRole('textbox', { name: 'Base URL' })).toHaveValue(filledFormState.baseUrl.value);
    });
  });

  describe('on Save Configuration Button', function () {
    it('calls update when the form is submitted', async () => {
      mockAxiosCalls({
        put: {
          [globalPermissionTestUrl]: Promise.resolve({ data: ['CONFIGURE_SYSTEM'] }),
          [getConfigurationUrl()]: Promise.resolve({
            data: {},
          }),
        },
      });
      spyOn(baseUrlConfigurationSelectors, 'selectFormState').and.callFake(() => filledFormState);
      spyOn(baseUrlConfigurationSelectors, 'selectIsDirty').and.callFake(() => true);
      spyOn(baseUrlConfigurationSelectors, 'selectHasAllRequiredFields').and.callFake(() => true);
      renderComponent();
      await waitFor(() => screen.getByText('Save Configuration'));
      expect(screen.getByRole('button', { name: 'Save Configuration' })).toBeVisible();
      fireEvent.click(screen.getByText('Save Configuration'));
      expect(axios.put.calls.count()).toBe(1);
    });

    it('shows correct form alert when no data provided', async () => {
      spyOn(baseUrlConfigurationSelectors, 'selectFormState').and.callFake(() => cleanFormState);
      spyOn(baseUrlConfigurationSelectors, 'selectIsDirty').and.callFake(() => false);
      renderComponent();
      await waitFor(() => screen.getByText('Save Configuration'));
      fireEvent.click(screen.getByText('Save Configuration'));
      expect(screen.getByRole('alert')).toHaveTextContent('There are no changes to update');
    });

    it('shows correct form alert when data partially provided', async () => {
      spyOn(baseUrlConfigurationSelectors, 'selectFormState').and.callFake(() => cleanFormState);
      spyOn(baseUrlConfigurationSelectors, 'selectIsDirty').and.callFake(() => true);
      renderComponent();
      await waitFor(() => screen.getByText('Save Configuration'));
      fireEvent.click(screen.getByText('Save Configuration'));
      expect(screen.getByRole('alert')).toHaveTextContent('Base URL is required data.');
    });
  });

  describe('on Cancel button', function () {
    it('cancel button is disabled when the form is clean', async () => {
      spyOn(baseUrlConfigurationSelectors, 'selectFormState').and.callFake(() => cleanFormState);
      spyOn(baseUrlConfigurationSelectors, 'selectIsDirty').and.callFake(() => false);
      renderComponent();
      await waitFor(() => screen.getByText('Cancel'));
      expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
      expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
    });

    it('keep cancel button enabled when the form is filled', async () => {
      spyOn(baseUrlConfigurationSelectors, 'selectFormState').and.callFake(() => filledFormState);
      spyOn(baseUrlConfigurationSelectors, 'selectIsDirty').and.callFake(() => true);
      renderComponent();
      await waitFor(() => screen.getByText('Cancel'));
      expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
    });
  });

  describe('on Delete button', function () {
    it('disabled Delete button when the form is clean', async () => {
      spyOn(baseUrlConfigurationSelectors, 'selectFormState').and.callFake(() => cleanFormState);
      spyOn(baseUrlConfigurationSelectors, 'selectIsDirty').and.callFake(() => false);
      renderComponent();
      await waitFor(() => screen.getByText('Delete Configuration'));
      expect(screen.getByRole('button', { name: 'Delete Configuration' })).toHaveAttribute('aria-disabled', 'true');
    });

    it('disable Delete button when the form is filled but serverData is clean', async () => {
      spyOn(baseUrlConfigurationSelectors, 'selectFormState').and.callFake(() => filledFormState);
      spyOn(baseUrlConfigurationSelectors, 'selectIsDirty').and.callFake(() => true);
      spyOn(baseUrlConfigurationSelectors, 'selectServerData').and.callFake(() => null);
      renderComponent();
      await waitFor(() => screen.getByText('Delete Configuration'));
      expect(screen.getByRole('button', { name: 'Delete Configuration' })).toHaveAttribute('aria-disabled', 'true');
    });

    it('enable Delete button when the form is filled with serverData', async () => {
      spyOn(baseUrlConfigurationSelectors, 'selectFormState').and.callFake(() => filledFormState);
      spyOn(baseUrlConfigurationSelectors, 'selectIsDirty').and.callFake(() => false);
      spyOn(baseUrlConfigurationSelectors, 'selectServerData').and.callFake(() => serverData);
      renderComponent();
      await waitFor(() => screen.getByText('Delete Configuration'));
      expect(screen.getByRole('button', { name: 'Delete Configuration' })).toHaveAttribute('aria-disabled', 'false');
    });
  });
});
