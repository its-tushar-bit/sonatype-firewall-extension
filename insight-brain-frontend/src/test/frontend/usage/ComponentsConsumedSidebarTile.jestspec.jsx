/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter, render, screen } from 'TestRoot/SpecUtil';
import * as ProductFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import ComponentsConsumedSidebarTile from 'MainRoot/usage/ComponentsConsumedSidebarTile';
import { getConsumptionSummaryUrl } from 'MainRoot/util/CLMLocation';
import * as RouterActions from 'MainRoot/reduxUiRouter/routerActions';

describe('ComponentsConsumedSidebarTile', () => {
  let axiosMock;
  let stateGoSpy;
  let usageDashboardEnabledSpy;

  function makeState(overrides = {}) {
    return {
      usage: {
        summary: null,
        loadingSummary: false,
        loadErrorSummary: null,
        ...(overrides.usage || {}),
      },
    };
  }

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    stateGoSpy = jest.spyOn(RouterActions, 'stateGo').mockImplementation(() => () => {});
    usageDashboardEnabledSpy = jest
      .spyOn(ProductFeaturesSelectors, 'selectIsUsageDashboardEnabled')
      .mockReturnValue(true);
    axiosMock.onGet(getConsumptionSummaryUrl()).reply(200, {
      consumed: 650000,
      limit: 1000000,
      percentUsed: 65,
      remaining: 350000,
    });
  });

  afterEach(() => {
    axiosMock.reset();
    stateGoSpy.mockRestore();
    usageDashboardEnabledSpy.mockRestore();
  });

  it('renders nothing when usage dashboard is disabled', () => {
    usageDashboardEnabledSpy.mockReturnValue(false);
    const { container } = render(<ComponentsConsumedSidebarTile />, {
      preloadedState: makeState(),
    });
    expect(container).toBeEmptyDOMElement();
  });

  it('renders skeleton during initial load', () => {
    render(<ComponentsConsumedSidebarTile />, {
      preloadedState: makeState({ usage: { loadingSummary: true } }),
    });
    expect(screen.getByTestId('iq-components-consumed-tile__skeleton')).toBeInTheDocument();
  });

  it('renders nothing on fetch error when no summary cached', () => {
    const { container } = render(<ComponentsConsumedSidebarTile />, {
      preloadedState: makeState({ usage: { loadErrorSummary: 'boom' } }),
    });
    expect(container).toBeEmptyDOMElement();
  });

  it('renders consumed only (no progress bar) when limit is null', () => {
    render(<ComponentsConsumedSidebarTile />, {
      preloadedState: makeState({
        usage: { summary: { consumed: 650000, limit: null } },
      }),
    });
    expect(screen.getByText(/650k/)).toBeInTheDocument();
    expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
  });

  it('renders progress bar with compact consumed/limit text', () => {
    render(<ComponentsConsumedSidebarTile />, {
      preloadedState: makeState({
        usage: { summary: { consumed: 1635, limit: 1000 } },
      }),
    });
    // Compact format: 1.6k / 1k (rounded to nearest hundred for the k-band)
    const fill = screen.getByTestId('iq-components-consumed-tile__bar-fill');
    expect(fill).toHaveStyle({ width: '100%' });
    expect(fill.className).toMatch(/--over/);
  });

  it('compact format applies in expanded state (1.6k / 1k)', () => {
    render(<ComponentsConsumedSidebarTile />, {
      preloadedState: makeState({
        usage: { summary: { consumed: 1635, limit: 1000 } },
      }),
    });
    expect(screen.getByText(/1\.6k/)).toBeInTheDocument();
    expect(screen.getByText(/1k/)).toBeInTheDocument();
  });

  it('renders progress ring + icon when collapsed=true', () => {
    const { container } = render(<ComponentsConsumedSidebarTile collapsed={true} />, {
      preloadedState: makeState({
        usage: { summary: { consumed: 1635, limit: 1000 } },
      }),
    });
    expect(container.querySelector('.iq-components-consumed-tile--collapsed')).toBeInTheDocument();
    expect(container.querySelector('.iq-components-consumed-tile__ring')).toBeInTheDocument();
    expect(container.querySelector('.iq-components-consumed-tile__icon')).toBeInTheDocument();
    // Bar track from expanded state should NOT be present
    expect(container.querySelector('.iq-components-consumed-tile__bar-track')).not.toBeInTheDocument();
  });

  it('collapsed ring uses over-limit style when consumed > limit', () => {
    const { container } = render(<ComponentsConsumedSidebarTile collapsed={true} />, {
      preloadedState: makeState({
        usage: { summary: { consumed: 1635, limit: 1000 } },
      }),
    });
    const ringFill = container.querySelector('.iq-components-consumed-tile__ring-fill');
    expect(ringFill.className.baseVal || ringFill.getAttribute('class')).toMatch(/--over/);
  });

  it('collapsed widget exposes the canonical full-precision text via aria-label', () => {
    render(<ComponentsConsumedSidebarTile collapsed={true} />, {
      preloadedState: makeState({
        usage: { summary: { consumed: 1635, limit: 1000 } },
      }),
    });
    const btn = screen.getByRole('button', { name: /Components: 1,635 \/ 1,000 \(164%\)/ });
    expect(btn).toBeInTheDocument();
  });

  it('saturates with over-limit modifier when consumed > limit', () => {
    render(<ComponentsConsumedSidebarTile />, {
      preloadedState: makeState({
        usage: { summary: { consumed: 1500000, limit: 1000000 } },
      }),
    });
    const fill = screen.getByTestId('iq-components-consumed-tile__bar-fill');
    expect(fill).toHaveStyle({ width: '100%' });
    expect(fill.className).toMatch(/iq-components-consumed-tile__bar-fill--over/);
  });

  it('navigates to usage page on click', async () => {
    const user = userEvent.setup();
    render(<ComponentsConsumedSidebarTile />, {
      preloadedState: makeState({
        usage: { summary: { consumed: 1, limit: 100 } },
      }),
    });
    await user.click(screen.getByRole('button', { name: /components/i }));
    expect(stateGoSpy).toHaveBeenCalledWith('usage');
  });
});
