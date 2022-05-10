/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import AtlassianCrowdConfiguration from 'MainRoot/configuration/crowd/AtlassianCrowdConfiguration';

import * as RouterStateContext from 'MainRoot/react/RouterStateContext';
import * as atlassianCrowdConfigurationSelectors from 'MainRoot/configuration/crowd/atlassianCrowdConfigurationSelectors';
import { render, screen, fireEvent, waitFor } from 'TestRoot/SpecUtil';
import axios from 'axios';
import { getCrowdConfigurationTestUrl, getCrowdConfigurationUrl } from 'MainRoot/util/CLMLocation';
import { getGlobalPermissionTestUrl } from 'MainRoot/utilAngular/CLMContextLocation';

describe('AtlassianCrowdConfiguration', () => {
  let renderComponent;
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  const crowdConfigurationUrl = getCrowdConfigurationUrl();
  const crowdConfigurationTestUrl = getCrowdConfigurationTestUrl();
  const globalPermissionTestUrl = getGlobalPermissionTestUrl();
  const filledFormState = {
    serverUrl: { value: 'someUrl' },
    applicationName: { value: 'someAppName' },
    applicationPassword: { value: 'someAppPass' },
  };

  const partFilledFormState = {
    serverUrl: { value: 'someUrl' },
    applicationName: { value: '' },
    applicationPassword: { value: '' },
  };

  const cleanformState = {
    serverUrl: { value: '' },
    applicationName: { value: '' },
    applicationPassword: { value: '' },
  };

  const serverData = {
    serverUrl: 'http://localhost:8070',
    applicationName: 'Sonatype',
    applicationPassword: 'admin123',
  };

  const crowdConfigurationParameters = {};

  beforeEach(() => {
    spyOn(atlassianCrowdConfigurationSelectors, 'selectAtlassianCrowdConfigurationSlice').and.returnValue(
      crowdConfigurationParameters
    );
    spyOn(RouterStateContext, 'useRouterState').and.returnValue({
      get: jasmine.createSpy('useRouterState.get'),
      href: jasmine.createSpy('useRouterState.href'),
    });
    renderComponent = () => render(<AtlassianCrowdConfiguration />);
  });

  describe('component load', () => {
    it('passes error when a load error exits', () => {
      const errorMessage = 'Error on page load';
      spyOn(atlassianCrowdConfigurationSelectors, 'selectLoadError').and.callFake(() => errorMessage);
      renderComponent();
      expect(screen.getByText(new RegExp('Error on page load'))).toBeVisible();
    });

    it('renders form when page load successfully', function () {
      spyOn(atlassianCrowdConfigurationSelectors, 'selectLoading').and.callFake(() => false);
      renderComponent();
      expect(screen.getByRole('heading', { name: 'Atlassian Crowd' })).toBeVisible();
      expect(screen.getByRole('heading', { name: 'Configure Atlassian Crowd Connection' })).toBeVisible();
      expect(screen.getByRole('textbox', { name: 'Crowd Server URL' })).toBeVisible();
      expect(screen.getByRole('textbox', { name: 'Crowd Application Name' })).toBeVisible();
      expect(screen.getByRole('button', { name: 'Test Configuration' })).toBeVisible();
      expect(screen.getByRole('button', { name: 'Delete Configuration' })).toBeVisible();
      expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
      expect(screen.getByRole('button', { name: 'Submit disabled: There are no changes to update' })).toBeVisible();
    });

    it('inputs filled when serverData', function () {
      spyOn(atlassianCrowdConfigurationSelectors, 'selectLoading').and.callFake(() => false);
      spyOn(atlassianCrowdConfigurationSelectors, 'selectFormState').and.callFake(() => filledFormState);
      renderComponent();
      expect(screen.getByRole('textbox', { name: 'Crowd Server URL' })).toHaveValue(filledFormState.serverUrl.value);
      expect(screen.getByRole('textbox', { name: 'Crowd Application Name' })).toHaveValue(
        filledFormState.applicationName.value
      );
    });
  });

  describe('on Save Configuration Button', function () {
    it('calls update when the form is submitted', async () => {
      mockAxiosCalls({
        put: {
          [globalPermissionTestUrl]: Promise.resolve({ data: ['CONFIGURE_SYSTEM'] }),
          [crowdConfigurationUrl]: Promise.resolve({
            data: {},
          }),
        },
      });
      spyOn(atlassianCrowdConfigurationSelectors, 'selectFormState').and.callFake(() => filledFormState);
      spyOn(atlassianCrowdConfigurationSelectors, 'selectIsDirty').and.callFake(() => true);
      spyOn(atlassianCrowdConfigurationSelectors, 'selectHasAllRequiredData').and.callFake(() => true);
      renderComponent();
      await waitFor(() => screen.getByText('Save Configuration'));
      expect(screen.getByRole('button', { name: 'Save Configuration' })).toBeVisible();
      fireEvent.click(screen.getByText('Save Configuration'));
      expect(axios.put.calls.count()).toBe(1);
    });

    it('disable save button when no data provided', async () => {
      spyOn(atlassianCrowdConfigurationSelectors, 'selectFormState').and.callFake(() => cleanformState);
      spyOn(atlassianCrowdConfigurationSelectors, 'selectIsDirty').and.callFake(() => false);
      renderComponent();
      await waitFor(() => screen.getByText('Save Configuration'));
      expect(screen.getByRole('button', { name: 'Submit disabled: There are no changes to update' })).toHaveClassName(
        'disabled'
      );
    });

    it('disable save button when data partially provided', async () => {
      spyOn(atlassianCrowdConfigurationSelectors, 'selectFormState').and.callFake(() => partFilledFormState);
      spyOn(atlassianCrowdConfigurationSelectors, 'selectIsDirty').and.callFake(() => true);
      renderComponent();
      await waitFor(() => screen.getByText('Save Configuration'));
      expect(
        screen.getByRole('button', {
          name: 'Submit disabled: Server URL, Application Name and Application Password are required data.',
        })
      ).toHaveClassName('disabled');
    });
  });

  describe('on Test Configuration button', function () {
    it('calls test when the data was provided', async () => {
      mockAxiosCalls({
        post: {
          [globalPermissionTestUrl]: Promise.resolve({ data: ['CONFIGURE_SYSTEM'] }),
          [crowdConfigurationTestUrl]: Promise.resolve({
            data: { code: 200, message: '' },
          }),
        },
      });
      spyOn(atlassianCrowdConfigurationSelectors, 'selectFormState').and.callFake(() => filledFormState);
      spyOn(atlassianCrowdConfigurationSelectors, 'selectIsDirty').and.callFake(() => true);
      spyOn(atlassianCrowdConfigurationSelectors, 'selectHasAllRequiredData').and.callFake(() => true);
      renderComponent();
      await waitFor(() => screen.getByText('Test Configuration'));
      expect(screen.getByRole('button', { name: 'Test Configuration' })).toBeVisible();
      fireEvent.click(screen.getByText('Test Configuration'));
      expect(axios.post.calls.count()).toBe(1);
    });

    it('disabled test button when the form is clean', async () => {
      spyOn(atlassianCrowdConfigurationSelectors, 'selectFormState').and.callFake(() => cleanformState);
      spyOn(atlassianCrowdConfigurationSelectors, 'selectIsDirty').and.callFake(() => false);
      renderComponent();
      await waitFor(() => screen.getByText('Test Configuration'));
      expect(screen.getByRole('button', { name: 'Test Configuration' })).toHaveAttribute('aria-disabled', 'true');
    });
  });

  describe('on Cancel button', function () {
    it('cancel button is enabled when the form is clean', async () => {
      spyOn(atlassianCrowdConfigurationSelectors, 'selectFormState').and.callFake(() => cleanformState);
      spyOn(atlassianCrowdConfigurationSelectors, 'selectIsDirty').and.callFake(() => false);
      renderComponent();
      await waitFor(() => screen.getByText('Cancel'));
      expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
    });

    it('keep cancel button enabled when the form is filled', async () => {
      spyOn(atlassianCrowdConfigurationSelectors, 'selectFormState').and.callFake(() => filledFormState);
      spyOn(atlassianCrowdConfigurationSelectors, 'selectIsDirty').and.callFake(() => true);
      renderComponent();
      await waitFor(() => screen.getByText('Cancel'));
      expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
    });
  });

  describe('on Delete button', function () {
    it('disabled Delete button when the form is clean', async () => {
      spyOn(atlassianCrowdConfigurationSelectors, 'selectFormState').and.callFake(() => cleanformState);
      spyOn(atlassianCrowdConfigurationSelectors, 'selectIsDirty').and.callFake(() => false);
      renderComponent();
      await waitFor(() => screen.getByText('Delete Configuration'));
      expect(screen.getByRole('button', { name: 'Delete Configuration' })).toHaveAttribute('aria-disabled', 'true');
    });

    it('disable Delete button when the form is filled but serverData is clean', async () => {
      spyOn(atlassianCrowdConfigurationSelectors, 'selectFormState').and.callFake(() => filledFormState);
      spyOn(atlassianCrowdConfigurationSelectors, 'selectIsDirty').and.callFake(() => true);
      spyOn(atlassianCrowdConfigurationSelectors, 'selectServerData').and.callFake(() => null);
      renderComponent();
      await waitFor(() => screen.getByText('Delete Configuration'));
      expect(screen.getByRole('button', { name: 'Delete Configuration' })).toHaveAttribute('aria-disabled', 'true');
    });

    it('enable Delete button when the form is filled with serverData', async () => {
      spyOn(atlassianCrowdConfigurationSelectors, 'selectFormState').and.callFake(() => filledFormState);
      spyOn(atlassianCrowdConfigurationSelectors, 'selectIsDirty').and.callFake(() => false);
      spyOn(atlassianCrowdConfigurationSelectors, 'selectServerData').and.callFake(() => serverData);
      renderComponent();
      await waitFor(() => screen.getByText('Delete Configuration'));
      expect(screen.getByRole('button', { name: 'Delete Configuration' })).toHaveAttribute('aria-disabled', 'false');
    });
  });
});
