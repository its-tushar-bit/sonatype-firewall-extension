/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import ProductLicenseContainer from 'MainRoot/configuration/license/ProductLicenseContainer';
import { axiosMockAdapter, render, screen } from 'TestRoot/SpecUtil';
import { initialState as initialProductLicenseState } from 'MainRoot/configuration/license/productLicenseReducer';
import { getLicenseUploadUrl, getPermissionContextTestUrl } from 'MainRoot/util/CLMLocation';
import { fireEvent, waitFor, waitForElementToBeRemoved } from '@testing-library/react';
import * as ProductLicenseUtils from 'MainRoot/utility/services/ProductLicense';

describe('ProductLicense', () => {
  const loadingText = 'Loading…';
  const licenseAgreementText = 'End User License Agreement';
  const errorText = 'some-license-error';
  const initialHref = '/';

  const { location } = window;

  let axiosMock;
  let reload;
  let loadIfNotYetLoaded;

  beforeEach(() => {
    axiosMock = axiosMockAdapter();

    delete window.location;
    reload = jest.fn();
    window.location = { reload, href: initialHref };

    // We have to mock the function rather than the axios call, because the logic
    // to determine if the data has been fetched is being stored in private variable.
    // That does not get reset between tests so behavior is affected by previous tests.
    // This is a bad pattern, once fixed we should instead mock axiosMock.onPost(getLicenseUploadUrl())
    loadIfNotYetLoaded = jest.spyOn(ProductLicenseUtils, 'loadIfNotYetLoaded');
  });

  afterEach(() => {
    window.location = location;
    axiosMock.reset();
  });

  it('renders a message asking the user to install a license if none is installed', async () => {
    givenUserHasPermissionsToInstallLicense();
    givenLicenseIsNotInstalledYet();

    await renderComponentAndWaitForLoadToComplete();

    expect(await screen.findByText(/No product licenses to display/i)).toBeVisible();
  });

  it('handles selection of a new a new license for the first time', async () => {
    givenUserHasPermissionsToInstallLicense();
    givenLicenseIsNotInstalledYet();
    givenLicenseUploadWillSucceed();

    await renderComponentAndWaitForLoadToComplete();

    // has a button to select a new license file
    expect(screen.getByRole('button', { name: 'Install License' })).toBeVisible();

    await simulateFileSelection();

    // should show license agreement after selection
    const modal = await screen.getByText(licenseAgreementText);
    expect(modal).toBeVisible();

    const acceptButton = screen.getByRole('button', { name: 'I Accept' });
    expect(acceptButton).toBeVisible();

    // hides the license agreement and reloads the page on successful upload
    acceptButton.click();
    expect(await screen.queryByText(licenseAgreementText)).not.toBeInTheDocument();
    await waitFor(() => expect(reload).toHaveBeenCalled());

    // sets location to getting started if no prior license was selected
    expect(window.location.href).toEqual('#/gettingStarted');
  });

  it('handles selection of a new a new license when one already exists', async () => {
    givenUserHasPermissionsToInstallLicense();
    givenExistingLicenseInstalled();
    givenLicenseUploadWillSucceed();

    await renderComponentAndWaitForLoadToComplete();

    // has a button to upload a new license
    expect(screen.getByRole('button', { name: 'Update License' })).toBeVisible();

    await simulateFileSelection();

    // should show license agreement after selection
    const modal = await screen.getByText(licenseAgreementText);
    expect(modal).toBeVisible();

    const acceptButton = screen.getByRole('button', { name: 'I Accept' });
    expect(acceptButton).toBeVisible();

    // hides the license agreement and reloads the page on successful upload
    acceptButton.click();
    expect(await screen.queryByText(licenseAgreementText)).not.toBeInTheDocument();
    await waitFor(() => expect(reload).toHaveBeenCalled());

    // show not redirect to getting started when this is a change of license rather than a first time license
    expect(window.location.href).toEqual(initialHref);
  });

  it('does not reload and shows errors when license upload fails', async () => {
    givenUserHasPermissionsToInstallLicense();
    givenLicenseIsNotInstalledYet();
    givenLicenseUploadWillFail();

    renderComponent();

    expect(screen.getByText(loadingText)).toBeVisible();
    expect(await screen.findByText(loadingText)).not.toBeInTheDocument();

    // has a button to select a new license file
    const installLicenseButton = screen.getByRole('button', { name: 'Install License' });
    expect(installLicenseButton).toBeVisible();

    await simulateFileSelection();

    // should show license agreement after selection
    const modal = await screen.getByText(licenseAgreementText);
    expect(modal).toBeVisible();

    const acceptButton = screen.getByRole('button', { name: 'I Accept' });
    expect(acceptButton).toBeVisible();

    // hides the license agreement, shows errors, and does not reload on failure to upload
    acceptButton.click();
    expect(await screen.queryByText(licenseAgreementText)).not.toBeInTheDocument();
    const errorMessage = await screen.findByRole('alert');
    expect(errorMessage).toBeVisible();
    expect(errorMessage.textContent).toEqual(errorText);
    expect(reload).not.toHaveBeenCalled();
  });

  function renderComponent(stateOverrides) {
    render(<ProductLicenseContainer />, { preloadedState: getState(stateOverrides) });
  }

  async function renderComponentAndWaitForLoadToComplete() {
    renderComponent();

    expect(screen.getByText(loadingText)).toBeVisible();
    await waitForElementToBeRemoved(screen.getByText(loadingText));
  }

  function getState(stateOverrides = {}) {
    return {
      productLicense: {
        ...initialProductLicenseState,
        ...stateOverrides,
      },
    };
  }

  function givenUserHasPermissionsToInstallLicense() {
    const permissions = ['CONFIGURE_SYSTEM'];
    axiosMock.onPut(getPermissionContextTestUrl('global', 'global'), permissions).reply(200, ['WRITE']);
  }

  function givenExistingLicenseInstalled() {
    const licenseResponse = {
      productEdition: 'Auditor',
      products: ['Sonatype Auditor'],
      fingerprint: 'some-finger-print-id',
      expiryTimestamp: Date.now() + 100000,
      licensedUsersToDisplay: null,
      applicationLimitToDisplay: null,
      applicationCountToDisplay: null,
      firewallUsersToDisplay: null,
      sbomLimitToDisplay: null,
      contactName: 'Andres Perez',
      contactCompany: 'Test Sonatype Inc',
      contactEmail: 'aperez@sonatype.com',
      properties: {
        support24: 'false',
        temporary: 'false',
        'clm.products': 'Risk',
        'clm.licenseVersion': '1',
        supportNDE: 'false',
        'clm.allowedClmStages': 'Release',
        sfObjectId: 'some-id',
        trial: 'False',
        licensingModel: 'user-based',
        'clm.maxActiveApplications': '10',
        ponumber: 'po42',
        salesforceAccountId: 'some-id',
      },
    };

    loadIfNotYetLoaded.mockReturnValue(licenseResponse);
  }
  function givenLicenseIsNotInstalledYet() {
    loadIfNotYetLoaded.mockRejectedValue({ response: { status: 402 } });
  }

  function givenLicenseUploadWillSucceed() {
    axiosMock.onPost(getLicenseUploadUrl()).reply(200);
  }

  function givenLicenseUploadWillFail() {
    axiosMock.onPost(getLicenseUploadUrl()).reply(500, errorText);
  }

  async function simulateFileSelection() {
    const file = new File(['(⌐□_□)'], 'license.lic', { type: 'text/plain;charset=utf-8' });

    const main = screen.getByRole('main');
    const hiddenFileInput = main.querySelector('#license-input');

    await waitFor(() => {
      fireEvent.change(hiddenFileInput, {
        target: { files: [file] },
      });
    });
  }
});
