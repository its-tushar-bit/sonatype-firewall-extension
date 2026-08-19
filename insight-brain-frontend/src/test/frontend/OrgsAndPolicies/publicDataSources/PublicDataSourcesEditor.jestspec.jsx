/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { fireEvent, screen, waitFor } from '@testing-library/react';
import { axiosMockAdapter, render } from 'TestRoot/SpecUtil';
import PublicDataSourcesEditor from 'MainRoot/OrgsAndPolicies/publicDataSources/PublicDataSourcesEditor';
import { getCpeConfigurationUrl, getOrganizationUrl } from 'MainRoot/util/CLMLocation';

describe('PublicDataSourcesEditor', () => {
  const axiosMock = axiosMockAdapter();
  let baseState;

  // Default mock response for the CPE configuration
  const defaultCpeConfig = {
    enabled: true,
    allowOverride: true,
    enabledInParent: true,
    inheritedFromOrganizationName: null,
    inheritedFromOrganizationAllowOverride: null,
  };

  const setupAndRender = async (stateOverrides = {}, cpeConfig = defaultCpeConfig) => {
    const orgId = baseState.router.currentParams.organizationId;
    axiosMock.onGet(getCpeConfigurationUrl('organization', orgId)).reply(200, cpeConfig);

    const preloadedState = { ...baseState, ...stateOverrides };
    const { store } = render(<PublicDataSourcesEditor />, {
      preloadedState,
    });

    await waitFor(() => {
      expect(screen.queryByText('Loading…')).not.toBeInTheDocument();
    });

    return { store };
  };

  beforeEach(() => {
    baseState = {
      orgsAndPolicies: {
        publicDataSources: {
          loading: false,
          loadError: null,
          isDirty: true,
          submitMaskState: null,
          submitError: null,
          data: {
            allowOverride: true,
            enabled: true,
            enabledInParent: true,
            inheritedFromOrganizationName: null,
            inheritedFromOrganizationAllowOverride: true,
          },
          serverData: {
            allowOverride: true,
            enabled: true,
            enabledInParent: true,
            inheritedFromOrganizationName: null,
            inheritedFromOrganizationAllowOverride: true,
          },
        },
        root: {
          selectedOwner: { id: 'org1', name: 'organization' },
        },
      },
      productFeatures: {
        productFeatures: { 'cpe-matching': true },
      },
      productLicense: {
        license: {
          productEdition: 'Lifecycle',
          products: ['Sonatype SBOM Manager', 'Sonatype Lifecycle'],
        },
      },
      router: {
        currentState: { name: 'organization' },
        isApplication: false,
        isRootOrganization: false,
        currentParams: { organizationId: 'org1' },
      },
    };

    axiosMock.onGet(getOrganizationUrl(baseState.router.currentParams.organizationId)).reply(200, {
      name: 'organization',
      id: 'org1',
    });
  });

  afterEach(() => {
    jest.clearAllMocks();
    axiosMock.reset();
  });

  it('renders the page title and description', async () => {
    await setupAndRender();
    expect(screen.getByText('Public Data Sources')).toBeInTheDocument();
    expect(screen.getByText(/Add public data to your results/i)).toBeInTheDocument();
  });

  it('renders radios and checkbox with correct state', async () => {
    await setupAndRender({}, { ...defaultCpeConfig, enabled: false });

    expect(screen.getByLabelText(/Inherit from parent/i)).toBeInTheDocument();
    const enabledRadios = screen.getAllByRole('radio', { name: /Enabled/i });
    expect(enabledRadios).toHaveLength(2);
    expect(screen.getByLabelText(/Disabled/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Allow users to enable public data sources/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Disabled/i)).toBeChecked();
  });

  it('changes the checked radio button when clicked', async () => {
    await setupAndRender();
    const enabledRadio = screen.getByRole('radio', { name: /^Enabled$/i });
    const disabledRadio = screen.getByRole('radio', { name: /Disabled/i });
    expect(enabledRadio).not.toBeDisabled();
    expect(disabledRadio).not.toBeDisabled();
    expect(enabledRadio).toBeChecked();
    expect(disabledRadio).not.toBeChecked();

    fireEvent.click(disabledRadio);

    await waitFor(() => {
      expect(enabledRadio).not.toBeChecked();
      expect(disabledRadio).toBeChecked();
    });
  });

  it('shows error alert if not supported by license', async () => {
    await setupAndRender({
      productFeatures: { productFeatures: {} },
    });

    expect(screen.getByText(/Public Data Sources are not supported by your license/i)).toBeInTheDocument();
  });

  it('shows validation error when no changes to save', async () => {
    await setupAndRender({
      orgsAndPolicies: {
        ...baseState.orgsAndPolicies,
        publicDataSources: { ...baseState.orgsAndPolicies.publicDataSources, isDirty: false },
      },
    });

    fireEvent.click(screen.getByText('Update'));

    await waitFor(() =>
      expect(screen.getByText(/There were validation errors. There are no changes to save./i)).toBeVisible()
    );
  });

  it('disables radios and checkbox if parent does not allow override', async () => {
    await setupAndRender(
      {
        // State overrides
        orgsAndPolicies: {
          ...baseState.orgsAndPolicies,
          publicDataSources: {
            ...baseState.orgsAndPolicies.publicDataSources,
            data: {
              ...baseState.orgsAndPolicies.publicDataSources.data,
              inheritedFromOrganizationAllowOverride: false,
            },
          },
        },
      },
      {
        // CPE config mock
        ...defaultCpeConfig,
        inheritedFromOrganizationName: 'org1',
        inheritedFromOrganizationAllowOverride: false,
      }
    );

    const enabledRadios = screen.getAllByRole('radio', { name: /Enabled/i });
    expect(enabledRadios).toHaveLength(2);
    enabledRadios.forEach((radio) => {
      expect(radio).toBeDisabled();
    });
    expect(screen.getByLabelText(/Disabled/i)).toBeDisabled();
    expect(screen.getByLabelText(/Allow users to enable public data sources/i)).toBeDisabled();
  });

  it('disables all controls when isSbomManager is true', async () => {
    await setupAndRender({
      router: {
        ...baseState.router,
        currentState: { name: 'sbomManager.organization' },
      },
    });

    expect(screen.getByText(/Public Data Sources are configured within Lifecycle/i)).toBeInTheDocument();
    expect(screen.getByText('Click here to update configuration')).toBeInTheDocument();

    const radios = screen.getAllByRole('radio');
    radios.forEach((radio) => {
      expect(radio).toBeDisabled();
    });

    const checkbox = screen.getByLabelText(/Allow users to enable public data sources/i);
    expect(checkbox).toBeDisabled();

    expect(screen.queryByText('Update')).toHaveClass('hidden');
  });

  it('does not show NxInfoAlert when isSbomManager is false', async () => {
    await setupAndRender();

    expect(screen.queryByText('Public Data Sources are configured within Lifecycle.')).not.toBeInTheDocument();
    expect(screen.queryByText('Click here to update configuration')).not.toBeInTheDocument();
  });

  it('does not show NxInfoAlert when isCpeMatchingSupported is false', async () => {
    await setupAndRender({
      router: {
        ...baseState.router,
        currentState: { name: 'sbomManager.organization' },
      },
      productFeatures: {
        productFeatures: { 'cpe-matching': false },
      },
    });

    expect(screen.queryByText('Public Data Sources are configured within Lifecycle.')).not.toBeInTheDocument();
  });

  it('does not show NxInfoAlert when is not a multilicense SBOM Manager product is false', async () => {
    await setupAndRender({
      router: {
        ...baseState.router,
        currentState: { name: 'sbomManager.organization' },
      },
      productFeatures: {
        productFeatures: { 'cpe-matching': true },
      },
      productLicense: {
        license: {
          products: ['Sonatype SBOM Manager'],
        },
      },
    });

    expect(screen.queryByText(/Public Data Sources are configured within Lifecycle/i)).not.toBeInTheDocument();
    expect(screen.queryByText('Click here to update configuration')).not.toBeInTheDocument();
    expect(screen.getByText(/Public Data Sources are not supported by your license/i)).toBeInTheDocument();
  });
});
