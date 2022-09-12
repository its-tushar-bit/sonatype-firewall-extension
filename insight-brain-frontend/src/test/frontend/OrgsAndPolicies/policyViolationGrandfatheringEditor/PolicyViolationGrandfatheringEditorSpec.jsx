/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { render, screen, fireEvent } from 'TestRoot/SpecUtil';

import PolicyViolationGrandfatheringEditor from 'MainRoot/OrgsAndPolicies/policyViolationGrandfatheringEditor/PolicyViolationGrandfatheringEditor';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { getGrandfatheringUrl } from 'MainRoot/util/CLMLocation';

describe('PolicyViolationGrandfatheringEditor Component', () => {
  let mock, renderComponent;
  const orgLevelState = {
    router: {
      currentState: {
        name: 'management.edit.organization.violation.grandfathering-policy',
      },
      currentParams: {
        organizationId: 'myOrg',
      },
    },
    productFeatures: {
      productFeatures: {
        'policy-grandfathering': true,
      },
    },
  };

  const applicationLevelState = {
    router: {
      currentState: {
        name: 'management.edit.application.violation-grandfathering-policy',
      },
      currentParams: {
        applicationPublicId: '1',
      },
    },
    productFeatures: {
      productFeatures: {
        'policy-grandfathering': true,
      },
    },
  };

  beforeAll(function () {
    mock = axiosMockAdapter();
  });

  beforeEach(() => {
    const defaultPreloadedState = {
      router: {
        currentState: {
          name: 'management.edit.organization.violation.grandfathering-policy',
        },
        currentParams: {
          organizationId: 'ROOT_ORGANIZATION_ID',
        },
      },
      productFeatures: {
        productFeatures: {
          'policy-grandfathering': true,
        },
      },
    };

    renderComponent = (preloadedState) =>
      render(<PolicyViolationGrandfatheringEditor />, { preloadedState: preloadedState || defaultPreloadedState });
  });

  it('renders tile with the correct page title', async () => {
    mock.onGet(getGrandfatheringUrl('organization', 'ROOT_ORGANIZATION_ID')).reply(200, {
      allowChange: true,
      allowOverride: false,
      enabled: false,
      inheritedFromOrganizationName: null,
    });
    renderComponent();
    const title = await screen.findByText('Policy Violation Grandfathering');
    expect(title).toBeVisible();
  });

  it('renders loading indicator', async () => {
    mock.onGet(getGrandfatheringUrl('organization', 'ROOT_ORGANIZATION_ID')).reply(200, {
      allowChange: true,
      allowOverride: false,
      enabled: false,
      inheritedFromOrganizationName: null,
    });
    renderComponent();
    expect(await screen.findByText('Loading…')).toBeVisible();
  });

  it('renders error message', async () => {
    mock.onGet(getGrandfatheringUrl('organization', 'ROOT_ORGANIZATION_ID')).reply(500, 'Error Messages');
    renderComponent();
    expect(await screen.findByRole('alert')).toBeVisible();
    expect(await screen.findByText('An error occurred loading data. Error Messages')).toBeVisible();
  });

  it('renders a disabled Update button on load', async () => {
    mock.onGet(getGrandfatheringUrl('organization', 'ROOT_ORGANIZATION_ID')).reply(200, {
      allowChange: true,
      allowOverride: false,
      enabled: false,
      inheritedFromOrganizationName: null,
    });
    renderComponent();
    const updateButton = await screen.findByRole('button');
    expect(updateButton).toBeVisible();
    expect(updateButton).toHaveClassName('disabled');
    fireEvent.click(updateButton);
    expect(mock.history.put.length).toBe(0);
  });

  describe('changes Update button to enabled when another radio button is clicked', () => {
    const testsToRun = [/inherit/i, /enabled/i, /disabled/i];
    function results(status) {
      switch (status) {
        case 'inherit':
          return '{"allowOverride":false,"enabled":null}';
        case 'enabled':
          return '{"allowOverride":false,"enabled":true}';
        case 'disabled':
          return '{"allowOverride":false,"enabled":false}';
      }
    }
    testsToRun.forEach((grandfathering) => {
      if (grandfathering.source === 'inherit') {
        return;
      }
      it(`on Root Organization level check ${grandfathering.source} grandfathering status`, async () => {
        mock.onGet(getGrandfatheringUrl('organization', 'ROOT_ORGANIZATION_ID')).reply(200, {
          allowChange: true,
          allowOverride: false,
          enabled: grandfathering.source === 'enabled' ? false : true,
          inheritedFromOrganizationName: null,
        });
        renderComponent();
        const radio = await screen.findByRole('radio', { name: grandfathering });
        fireEvent.click(radio);
        const updateButton = screen.getByRole('button');
        expect(updateButton).not.toHaveClassName('disabled');
        fireEvent.click(updateButton);
        expect(mock.history.put.length).toBe(1);
        expect(mock.history.put[0].url).toBe(getGrandfatheringUrl('organization', 'ROOT_ORGANIZATION_ID'));
        expect(mock.history.put[0].data).toBe(results(grandfathering.source));
      });
    });

    testsToRun.forEach((grandfathering) => {
      it(`on Organization level check ${grandfathering.source} grandfathering status`, async () => {
        mock.onGet(getGrandfatheringUrl('organization', 'myOrg')).reply(200, {
          allowChange: true,
          allowOverride: false,
          enabled: grandfathering.source === 'enabled' ? false : true,
          inheritedFromOrganizationName: null,
        });
        renderComponent(orgLevelState);
        const radio = await screen.findByRole('radio', { name: grandfathering });
        fireEvent.click(radio);
        const updateButton = screen.getByRole('button');
        expect(updateButton).not.toHaveClassName('disabled');
        fireEvent.click(updateButton);
        expect(mock.history.put.length).toBe(1);
        expect(mock.history.put[0].url).toBe(getGrandfatheringUrl('organization', 'myOrg'));
        expect(mock.history.put[0].data).toBe(results(grandfathering.source));
      });
    });

    testsToRun.forEach((grandfathering) => {
      it(`on Application level check ${grandfathering.source} grandfathering status`, async () => {
        mock.onGet(getGrandfatheringUrl('application', '1')).reply(200, {
          allowChange: true,
          allowOverride: false,
          enabled: grandfathering.source === 'enabled' ? false : true,
          inheritedFromOrganizationName: null,
        });
        renderComponent(applicationLevelState);
        const radio = await screen.findByRole('radio', { name: grandfathering });
        fireEvent.click(radio);
        const updateButton = screen.getByRole('button');
        expect(updateButton).not.toHaveClassName('disabled');
        fireEvent.click(updateButton);
        expect(mock.history.put.length).toBe(1);
        expect(mock.history.put[0].url).toBe(getGrandfatheringUrl('application', '1'));
        expect(mock.history.put[0].data).toBe(results(grandfathering.source));
      });
    });
  });

  it('changes Update button to enabled when "Allow override" checkbox is checked', async () => {
    mock.onGet(getGrandfatheringUrl('organization', 'ROOT_ORGANIZATION_ID')).reply(200, {
      allowChange: true,
      allowOverride: false,
      enabled: false,
      inheritedFromOrganizationName: null,
    });
    renderComponent();
    const checkbox = await screen.findByRole('checkbox');
    fireEvent.click(checkbox);
    const updateButton = screen.getByRole('button');
    expect(updateButton).not.toHaveClassName('disabled');
    fireEvent.click(updateButton);
    expect(mock.history.put.length).toBe(1);
    expect(mock.history.put[0].url).toBe(getGrandfatheringUrl('organization', 'ROOT_ORGANIZATION_ID'));
    expect(mock.history.put[0].data).toBe('{"allowOverride":true,"enabled":false}');
  });

  it('select Inherit from Parent Organization', async () => {
    mock.onGet(getGrandfatheringUrl('organization', 'myOrg')).reply(200, {
      allowChange: true,
      allowOverride: false,
      enabled: false,
      inheritedFromOrganizationName: null,
    });
    renderComponent(orgLevelState);

    const radio = await screen.findByRole('radio', { name: /inherit/i });
    fireEvent.click(radio);
    const updateButton = screen.getByRole('button');
    expect(updateButton).not.toHaveClassName('disabled');
    fireEvent.click(updateButton);
    expect(mock.history.put.length).toBe(1);
    expect(mock.history.put[0].url).toBe(getGrandfatheringUrl('organization', 'myOrg'));
    expect(mock.history.put[0].data).toBe('{"allowOverride":false,"enabled":null}');
  });

  it('renders a warning message when allowOverride flag is false', async () => {
    mock.onGet(getGrandfatheringUrl('organization', 'myOrg')).reply(200, {
      allowChange: false,
      allowOverride: false,
      enabled: true,
      inheritedFromOrganizationName: null,
    });
    renderComponent(orgLevelState);

    expect(await screen.findByText('The parent selection cannot be overridden.')).toBeVisible();
    const updateButton = await screen.findByRole('button');
    expect(updateButton).toHaveClassName('disabled');
  });

  describe('Grandfathering status message', () => {
    it(`on Application level check shows correct grandfathering status`, async () => {
      mock.onGet(getGrandfatheringUrl('application', '1')).reply(200, {
        allowChange: true,
        allowOverride: false,
        enabled: true,
        inheritedFromOrganizationName: 'Some Org',
      });
      renderComponent(applicationLevelState);
      expect(await screen.findByRole('definition')).toHaveTextContent('Grandfathering is enabled');

      let radio = await screen.findByRole('radio', { name: /inherit/i });
      fireEvent.click(radio);

      const updateButton = screen.getByRole('button');
      fireEvent.click(updateButton);

      expect(await screen.findByRole('definition')).toHaveTextContent(
        'Inherit from Some Org (Grandfathering is enabled)'
      );
    });

    it(`on Organization level check shows correct grandfathering status`, async () => {
      mock.onGet(getGrandfatheringUrl('application', '1')).reply(200, {
        allowChange: true,
        allowOverride: false,
        enabled: false,
        inheritedFromOrganizationName: 'Root Organization',
      });
      renderComponent(applicationLevelState);
      expect(await screen.findByRole('definition')).toHaveTextContent('Grandfathering is disabled');

      let radio = await screen.findByRole('radio', { name: /inherit/i });
      fireEvent.click(radio);

      const updateButton = screen.getByRole('button');
      fireEvent.click(updateButton);

      expect(await screen.findByRole('definition')).toHaveTextContent(
        'Inherit from Root Organization (Grandfathering is disabled)'
      );
    });
  });
});
