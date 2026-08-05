/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { renderNexusOneEstateComponentDetail } from 'TestRoot/nosc/components/detail/estate/renderNexusOneEstateComponentDetail';
import { getApiV2ComponentDetailsUrl } from 'MainRoot/util/CLMLocation';
import { _setBaseUrlForTesting, setBaseUrl } from 'MainRoot/util/urlUtil';

const COMPONENT_HASH = 'legal-hash-1';

const FULL_LICENSE_RESPONSE = {
  componentDetails: [
    {
      matchState: 'exact',
      component: {
        hash: COMPONENT_HASH,
        displayName: 'example 1.0.0',
      },
      licenseData: {
        status: 'Open',
        declaredLicenses: [{ licenseId: 'Apache-2.0', licenseName: 'Apache 2.0' }],
        observedLicenses: [{ licenseId: 'MIT', licenseName: 'MIT License' }],
        effectiveLicenses: [{ licenseId: 'Apache-2.0', licenseName: 'Apache 2.0' }],
      },
    },
  ],
};

describe('EstateComponentLegalTab', () => {
  let axiosMock: ReturnType<typeof axiosMockAdapter>;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
    _setBaseUrlForTesting('http://localhost');
  });

  afterAll(() => {
    setBaseUrl();
  });

  afterEach(() => {
    axiosMock.reset();
  });

  it('renders status and declared / observed / effective license sections', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, FULL_LICENSE_RESPONSE);

    renderNexusOneEstateComponentDetail(COMPONENT_HASH, 'legal');

    expect(await screen.findByTestId('nosc-estate-component-legal')).toBeInTheDocument();
    expect(screen.getByText('Status: Open')).toBeInTheDocument();
    expect(screen.getByTestId('nosc-estate-component-legal-declared')).toHaveTextContent(
      'Apache 2.0 (Apache-2.0)',
    );
    expect(screen.getByTestId('nosc-estate-component-legal-observed')).toHaveTextContent(
      'MIT License (MIT)',
    );
    expect(screen.getByTestId('nosc-estate-component-legal-effective')).toHaveTextContent(
      'Apache 2.0 (Apache-2.0)',
    );
  });

  it('shows None for empty license arrays', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, {
      componentDetails: [
        {
          component: { hash: COMPONENT_HASH, displayName: 'example 1.0.0' },
          licenseData: {
            status: 'Unknown',
            declaredLicenses: [],
            observedLicenses: [],
            effectiveLicenses: [],
          },
        },
      ],
    });

    renderNexusOneEstateComponentDetail(COMPONENT_HASH, 'legal');

    expect(await screen.findByTestId('nosc-estate-component-legal')).toBeInTheDocument();
    expect(screen.getByText('Status: Unknown')).toBeInTheDocument();
    expect(screen.getByTestId('nosc-estate-component-legal-declared')).toHaveTextContent('None');
    expect(screen.getByTestId('nosc-estate-component-legal-observed')).toHaveTextContent('None');
    expect(screen.getByTestId('nosc-estate-component-legal-effective')).toHaveTextContent('None');
  });

  it('shows Legal empty state when HDS returns no component details', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, { componentDetails: [] });

    renderNexusOneEstateComponentDetail(COMPONENT_HASH, 'legal');

    expect(await screen.findByTestId('nosc-estate-component-legal-empty')).toHaveTextContent(
      'No legal details were found for this component.',
    );
  });

  it('shows Legal error + Retry and recovers after a successful reload', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(500);

    renderNexusOneEstateComponentDetail(COMPONENT_HASH, 'legal');

    expect(await screen.findByTestId('nosc-estate-component-legal-error')).toBeInTheDocument();
    expect(screen.getByTestId('nosc-estate-component-legal-retry')).toBeInTheDocument();

    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, FULL_LICENSE_RESPONSE);
    await userEvent.click(screen.getByTestId('nosc-estate-component-legal-retry'));

    await waitFor(() => {
      expect(screen.getByTestId('nosc-estate-component-legal')).toBeInTheDocument();
    });
    expect(screen.getByTestId('nosc-estate-component-legal-declared')).toHaveTextContent('Apache 2.0');
  });
});
