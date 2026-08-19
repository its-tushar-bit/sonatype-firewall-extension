/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { render, screen, fireEvent, axiosMockAdapter } from 'TestRoot/SpecUtil';

import LegacyViolationsEditor from 'MainRoot/OrgsAndPolicies/legacyViolationsEditor/LegacyViolationsEditor';
import { getLegacyViolationURL } from 'MainRoot/util/CLMLocation';

describe('LegacyViolationsEditor Component', () => {
  let mock, renderComponent;
  const orgLevelState = {
    router: {
      currentState: {
        name: 'management.edit.organization.legacy-violation',
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
        name: 'management.edit.application.legacy-violation',
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

  beforeAll(() => {
    mock = axiosMockAdapter();
  });

  beforeEach(() => {
    const defaultPreloadedState = {
      router: {
        currentState: {
          name: 'management.edit.organization.legacy-violation',
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
      render(<LegacyViolationsEditor />, { preloadedState: preloadedState || defaultPreloadedState });
  });

  it('renders tile with the correct page title', async () => {
    mock.onGet(getLegacyViolationURL('organization', 'ROOT_ORGANIZATION_ID')).reply(200, {
      allowChange: true,
      allowOverride: false,
      enabled: false,
      inheritedFromOrganizationName: null,
    });
    renderComponent();
    const title = await screen.findByText('Legacy Violations');
    expect(title).toBeVisible();
  });

  it('renders loading indicator', async () => {
    mock.onGet(getLegacyViolationURL('organization', 'ROOT_ORGANIZATION_ID')).reply(() => new Promise(() => {}));
    renderComponent();
    expect(await screen.findByText('Loading…')).toBeVisible();
  });

  it('renders error message', async () => {
    mock.onGet(getLegacyViolationURL('organization', 'ROOT_ORGANIZATION_ID')).reply(500, 'Error Messages');
    renderComponent();
    expect(await screen.findByRole('alert')).toBeVisible();
    expect(await screen.findByText('An error occurred loading data. Error Messages')).toBeVisible();
  });

  it('renders a disabled Update button on load', async () => {
    mock.onGet(getLegacyViolationURL('organization', 'ROOT_ORGANIZATION_ID')).reply(200, {
      allowChange: true,
      allowOverride: false,
      enabled: false,
      inheritedFromOrganizationName: null,
    });
    renderComponent();
    const updateButton = await screen.findByRole('button', { name: 'Update' });
    expect(updateButton).toBeVisible();
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
    testsToRun.forEach((violationStatus) => {
      if (violationStatus.source === 'inherit') {
        return;
      }
      it(`on Root Organization level check ${violationStatus.source} legacy violation status`, async () => {
        mock.onGet(getLegacyViolationURL('organization', 'ROOT_ORGANIZATION_ID')).reply(200, {
          allowChange: true,
          allowOverride: false,
          enabled: violationStatus.source === 'enabled' ? false : true,
          inheritedFromOrganizationName: null,
        });
        renderComponent();
        const radio = await screen.findByRole('radio', { name: violationStatus });
        fireEvent.click(radio);
        const updateButton = screen.getByRole('button', { name: 'Update' });
        fireEvent.click(updateButton);
        expect(mock.history.put.length).toBe(1);
        expect(mock.history.put[0].url).toBe(getLegacyViolationURL('organization', 'ROOT_ORGANIZATION_ID'));
        expect(mock.history.put[0].data).toBe(results(violationStatus.source));
      });
    });

    testsToRun.forEach((violationStatus) => {
      it(`on Organization level check ${violationStatus.source} legacy violation status`, async () => {
        mock.onGet(getLegacyViolationURL('organization', 'myOrg')).reply(200, {
          allowChange: true,
          allowOverride: false,
          enabled: violationStatus.source === 'enabled' ? false : true,
          inheritedFromOrganizationName: null,
        });
        renderComponent(orgLevelState);
        const radios = await screen.findAllByRole('radio', { name: violationStatus });
        let radioEl;
        if (violationStatus.source === 'disabled') {
          radioEl = radios[1];
        } else {
          radioEl = radios[0];
        }
        fireEvent.click(radioEl);
        const updateButton = screen.getByRole('button', { name: 'Update' });
        fireEvent.click(updateButton);
        expect(mock.history.put.length).toBe(1);
        expect(mock.history.put[0].url).toBe(getLegacyViolationURL('organization', 'myOrg'));
        expect(mock.history.put[0].data).toBe(results(violationStatus.source));
      });
    });

    testsToRun.forEach((violationStatus) => {
      it(`on Application level check ${violationStatus.source} legacy violation status`, async () => {
        mock.onGet(getLegacyViolationURL('application', '1')).reply(200, {
          allowChange: true,
          allowOverride: false,
          enabled: violationStatus.source === 'enabled' ? false : true,
          inheritedFromOrganizationName: null,
        });
        renderComponent(applicationLevelState);
        const radios = await screen.findAllByRole('radio', { name: violationStatus });
        let radioEl;
        if (violationStatus.source === 'disabled') {
          radioEl = radios[1];
        } else {
          radioEl = radios[0];
        }
        fireEvent.click(radioEl);
        const updateButton = screen.getByRole('button', { name: 'Update' });
        fireEvent.click(updateButton);
        expect(mock.history.put.length).toBe(1);
        expect(mock.history.put[0].url).toBe(getLegacyViolationURL('application', '1'));
        expect(mock.history.put[0].data).toBe(results(violationStatus.source));
      });
    });
  });

  it('changes Update button to enabled when "Allow override" checkbox is checked', async () => {
    mock.onGet(getLegacyViolationURL('organization', 'ROOT_ORGANIZATION_ID')).reply(200, {
      allowChange: true,
      allowOverride: false,
      enabled: false,
      inheritedFromOrganizationName: null,
    });
    renderComponent();
    const checkbox = await screen.findByRole('checkbox');
    fireEvent.click(checkbox);
    const updateButton = screen.getByRole('button', { name: 'Update' });
    fireEvent.click(updateButton);
    expect(mock.history.put.length).toBe(1);
    expect(mock.history.put[0].url).toBe(getLegacyViolationURL('organization', 'ROOT_ORGANIZATION_ID'));
    expect(mock.history.put[0].data).toBe('{"allowOverride":true,"enabled":false}');
  });

  it('select Inherit from Parent Organization', async () => {
    mock.onGet(getLegacyViolationURL('organization', 'myOrg')).reply(200, {
      allowChange: true,
      allowOverride: false,
      enabled: false,
      inheritedFromOrganizationName: null,
    });
    renderComponent(orgLevelState);

    const radio = await screen.findByRole('radio', { name: /inherit/i });
    fireEvent.click(radio);
    const updateButton = screen.getByRole('button', { name: 'Update' });
    fireEvent.click(updateButton);
    expect(mock.history.put.length).toBe(1);
    expect(mock.history.put[0].url).toBe(getLegacyViolationURL('organization', 'myOrg'));
    expect(mock.history.put[0].data).toBe('{"allowOverride":false,"enabled":null}');
  });

  it('renders a warning message when allowOverride flag is false', async () => {
    mock.onGet(getLegacyViolationURL('organization', 'myOrg')).reply(200, {
      allowChange: false,
      allowOverride: false,
      enabled: true,
      inheritedFromOrganizationName: null,
    });
    renderComponent(orgLevelState);

    expect(await screen.findByText('The parent selection cannot be overridden.')).toBeVisible();
    expect(await screen.findByRole('button', { name: 'Update' })).toBeVisible();
  });
});
