/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { axiosMockAdapter, render, screen, fireEvent, waitFor } from 'TestRoot/SpecUtil';
import AtlassianCrowdConfiguration from 'MainRoot/configuration/crowd/AtlassianCrowdConfiguration';

import * as RouterStateContext from 'MainRoot/react/RouterStateContext';
import * as atlassianCrowdConfigurationSelectors from 'MainRoot/configuration/crowd/atlassianCrowdConfigurationSelectors';
import { getCrowdConfigurationTestUrl, getCrowdConfigurationUrl } from 'MainRoot/util/CLMLocation';
import { getGlobalPermissionTestUrl } from 'MainRoot/util/CLMContextLocation';

describe('AtlassianCrowdConfiguration', () => {
  let renderComponent;
  let axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });
  const crowdConfigurationUrl = getCrowdConfigurationUrl();
  const crowdConfigurationTestUrl = getCrowdConfigurationTestUrl();
  const globalPermissionTestUrl = getGlobalPermissionTestUrl();
  const filledFormState = {
    serverUrl: { value: 'someUrl', isPristine: false, trimmedValue: 'someUrl', validationErrors: null },
    applicationName: { value: 'someAppName', isPristine: false, trimmedValue: 'someAppName', validationErrors: null },
    applicationPassword: {
      value: 'someAppPass',
      isPristine: false,
      trimmedValue: 'someAppPass',
      validationErrors: null,
    },
  };

  const partFilledFormState = {
    serverUrl: { value: 'someUrl', isPristine: false, trimmedValue: 'someUrl', validationErrors: null },
    applicationName: { value: '', isPristine: true, trimmedValue: '', validationErrors: null },
    applicationPassword: { value: '', isPristine: true, trimmedValue: '', validationErrors: null },
  };

  const cleanformState = {
    serverUrl: { value: '', isPristine: true, trimmedValue: '', validationErrors: null },
    applicationName: { value: '', isPristine: true, trimmedValue: '', validationErrors: null },
    applicationPassword: { value: '', isPristine: true, trimmedValue: '', validationErrors: null },
  };

  const serverData = {
    serverUrl: 'http://localhost:8070',
    applicationName: 'Sonatype',
    applicationPassword: 'admin123',
  };

  const crowdConfigurationParameters = {};

  beforeEach(() => {
    jest
      .spyOn(atlassianCrowdConfigurationSelectors, 'selectAtlassianCrowdConfigurationSlice')
      .mockReturnValue(crowdConfigurationParameters);
    jest.spyOn(RouterStateContext, 'useRouterState').mockReturnValue({
      get: jest.fn(),
      href: jest.fn(),
    });
    renderComponent = () => render(<AtlassianCrowdConfiguration />);
  });

  describe('component load', () => {
    it('passes error when a load error exits', () => {
      const errorMessage = 'Error on page load';
      jest.spyOn(atlassianCrowdConfigurationSelectors, 'selectLoadError').mockImplementation(() => errorMessage);
      renderComponent();
      expect(screen.getByText(new RegExp('Error on page load'))).toBeVisible();
    });

    it('renders form when page load successfully', function () {
      jest.spyOn(atlassianCrowdConfigurationSelectors, 'selectLoading').mockImplementation(() => false);
      renderComponent();
      expect(screen.getByRole('heading', { name: 'Atlassian Crowd' })).toBeVisible();
      expect(screen.getByRole('heading', { name: 'Configure Atlassian Crowd Connection' })).toBeVisible();
      expect(screen.getByRole('textbox', { name: 'Crowd Server URL' })).toBeVisible();
      expect(screen.getByRole('textbox', { name: 'Crowd Application Name' })).toBeVisible();
      expect(screen.getByRole('button', { name: 'Test Configuration' })).toBeVisible();
      expect(screen.getByRole('button', { name: 'Delete Configuration' })).toBeVisible();
      expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
      expect(screen.getByRole('button', { name: 'Save Configuration' })).toBeVisible();
    });

    it('inputs filled when serverData', function () {
      jest.spyOn(atlassianCrowdConfigurationSelectors, 'selectLoading').mockImplementation(() => false);
      jest.spyOn(atlassianCrowdConfigurationSelectors, 'selectFormState').mockImplementation(() => filledFormState);
      renderComponent();
      expect(screen.getByRole('textbox', { name: 'Crowd Server URL' })).toHaveValue(filledFormState.serverUrl.value);
      expect(screen.getByRole('textbox', { name: 'Crowd Application Name' })).toHaveValue(
        filledFormState.applicationName.value
      );
    });
  });

  describe('on Save Configuration Button', function () {
    it('calls update when the form is submitted', async () => {
      axiosMock.onPut(globalPermissionTestUrl).reply(200, ['CONFIGURE_SYSTEM']);
      axiosMock.onPut(crowdConfigurationUrl).reply(200, {});

      jest.spyOn(atlassianCrowdConfigurationSelectors, 'selectFormState').mockImplementation(() => filledFormState);
      jest.spyOn(atlassianCrowdConfigurationSelectors, 'selectIsDirty').mockImplementation(() => true);
      jest.spyOn(atlassianCrowdConfigurationSelectors, 'selectHasAllRequiredData').mockImplementation(() => true);
      renderComponent();
      await waitFor(() => screen.getByText('Save Configuration'));
      expect(screen.getByRole('button', { name: 'Save Configuration' })).toBeVisible();
      fireEvent.click(screen.getByText('Save Configuration'));
      await waitFor(() => {
        expect(axiosMock.history.put.length).toBe(1);
      });
    });

    it('shows correct form alert when no data provided', async () => {
      jest.spyOn(atlassianCrowdConfigurationSelectors, 'selectFormState').mockImplementation(() => cleanformState);
      jest.spyOn(atlassianCrowdConfigurationSelectors, 'selectIsDirty').mockImplementation(() => false);
      renderComponent();
      await waitFor(() => screen.getByText('Save Configuration'));
      fireEvent.click(screen.getByText('Save Configuration'));
      expect(screen.getByRole('alert')).toHaveTextContent('There are no changes to update.');
    });

    it('shows correct form alert when data partially provided', async () => {
      jest.spyOn(atlassianCrowdConfigurationSelectors, 'selectFormState').mockImplementation(() => partFilledFormState);
      jest.spyOn(atlassianCrowdConfigurationSelectors, 'selectIsDirty').mockImplementation(() => true);
      renderComponent();
      await waitFor(() => screen.getByText('Save Configuration'));
      fireEvent.click(screen.getByText('Save Configuration'));
      expect(screen.getByRole('alert')).toHaveTextContent(
        'Server URL, Application Name and Application Password are required data.'
      );
    });
  });

  describe('on Test Configuration button', function () {
    it('calls test when the data was provided', async () => {
      axiosMock.onPost(globalPermissionTestUrl).reply(200, ['CONFIGURE_SYSTEM']);
      axiosMock.onPost(crowdConfigurationTestUrl).reply(200, { code: 200, message: '' });

      jest.spyOn(atlassianCrowdConfigurationSelectors, 'selectFormState').mockImplementation(() => filledFormState);
      jest.spyOn(atlassianCrowdConfigurationSelectors, 'selectIsDirty').mockImplementation(() => true);
      jest.spyOn(atlassianCrowdConfigurationSelectors, 'selectHasAllRequiredData').mockImplementation(() => true);
      renderComponent();
      await waitFor(() => screen.getByText('Test Configuration'));
      expect(screen.getByRole('button', { name: 'Test Configuration' })).toBeVisible();
      fireEvent.click(screen.getByText('Test Configuration'));
      await waitFor(() => {
        expect(axiosMock.history.post.length).toBe(1);
      });
    });

    it('disabled test button when the form is clean', async () => {
      jest.spyOn(atlassianCrowdConfigurationSelectors, 'selectFormState').mockImplementation(() => cleanformState);
      jest.spyOn(atlassianCrowdConfigurationSelectors, 'selectIsDirty').mockImplementation(() => false);
      renderComponent();
      await waitFor(() => screen.getByText('Test Configuration'));
      expect(screen.getByRole('button', { name: 'Test Configuration' })).toHaveAttribute('aria-disabled', 'true');
    });
  });

  describe('on Cancel button', function () {
    it('cancel button is enabled when the form is clean', async () => {
      jest.spyOn(atlassianCrowdConfigurationSelectors, 'selectFormState').mockImplementation(() => cleanformState);
      jest.spyOn(atlassianCrowdConfigurationSelectors, 'selectIsDirty').mockImplementation(() => false);
      renderComponent();
      await waitFor(() => screen.getByText('Cancel'));
      expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
    });

    it('keep cancel button enabled when the form is filled', async () => {
      jest.spyOn(atlassianCrowdConfigurationSelectors, 'selectFormState').mockImplementation(() => filledFormState);
      jest.spyOn(atlassianCrowdConfigurationSelectors, 'selectIsDirty').mockImplementation(() => true);
      renderComponent();
      await waitFor(() => screen.getByText('Cancel'));
      expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
    });
  });

  describe('on Delete button', function () {
    it('disabled Delete button when the form is clean', async () => {
      jest.spyOn(atlassianCrowdConfigurationSelectors, 'selectFormState').mockImplementation(() => cleanformState);
      jest.spyOn(atlassianCrowdConfigurationSelectors, 'selectIsDirty').mockImplementation(() => false);
      renderComponent();
      await waitFor(() => screen.getByText('Delete Configuration'));
      expect(screen.getByRole('button', { name: 'Delete Configuration' })).toHaveAttribute('aria-disabled', 'true');
    });

    it('disable Delete button when the form is filled but serverData is clean', async () => {
      jest.spyOn(atlassianCrowdConfigurationSelectors, 'selectFormState').mockImplementation(() => filledFormState);
      jest.spyOn(atlassianCrowdConfigurationSelectors, 'selectIsDirty').mockImplementation(() => true);
      jest.spyOn(atlassianCrowdConfigurationSelectors, 'selectServerData').mockImplementation(() => null);
      renderComponent();
      await waitFor(() => screen.getByText('Delete Configuration'));
      expect(screen.getByRole('button', { name: 'Delete Configuration' })).toHaveAttribute('aria-disabled', 'true');
    });

    it('enable Delete button when the form is filled with serverData', async () => {
      jest.spyOn(atlassianCrowdConfigurationSelectors, 'selectFormState').mockImplementation(() => filledFormState);
      jest.spyOn(atlassianCrowdConfigurationSelectors, 'selectIsDirty').mockImplementation(() => false);
      jest.spyOn(atlassianCrowdConfigurationSelectors, 'selectServerData').mockImplementation(() => serverData);
      renderComponent();
      await waitFor(() => screen.getByText('Delete Configuration'));
      expect(screen.getByRole('button', { name: 'Delete Configuration' })).toHaveAttribute('aria-disabled', 'false');
    });
  });
});
