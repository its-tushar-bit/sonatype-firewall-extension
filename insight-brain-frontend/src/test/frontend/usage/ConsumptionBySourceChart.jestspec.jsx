/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import { render, screen } from 'TestRoot/SpecUtil';
import ConsumptionBySourceChart, { SOURCE_LABELS, SOURCE_COLORS } from 'MainRoot/usage/ConsumptionBySourceChart';

describe('ConsumptionBySourceChart', () => {
  function dataset(breakdown) {
    const consumed = Object.values(breakdown).reduce((acc, n) => acc + n, 0);
    return [{ month: '2026-05', consumed, breakdown }];
  }

  function rowLabels() {
    const items = screen.getAllByRole('listitem');
    return items
      .map((li) => {
        const labelEl = li.querySelector('.iq-usage-source-chart__label');
        return labelEl ? labelEl.textContent : null;
      })
      .filter((t) => t !== null);
  }

  it('renders nothing when sourceBreakdown is missing', () => {
    const { container } = render(<ConsumptionBySourceChart sourceBreakdown={null} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('renders nothing when all counts are zero', () => {
    const { container } = render(<ConsumptionBySourceChart sourceBreakdown={dataset({ UI: 0, CLI: 0 })} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('renders the tile title', () => {
    render(<ConsumptionBySourceChart sourceBreakdown={dataset({ UI: 10 })} />);
    expect(screen.getByText('Consumption by Source')).toBeInTheDocument();
  });

  it('sorts legend rows by count descending', () => {
    render(<ConsumptionBySourceChart sourceBreakdown={dataset({ UI: 10, CLI: 50, API: 25 })} />);
    expect(rowLabels()).toEqual(['CLI', 'API', 'UI']);
  });

  it('breaks count ties alphabetically on display label', () => {
    render(<ConsumptionBySourceChart sourceBreakdown={dataset({ CLI: 20, UI: 20, CI_CD: 20 })} />);
    expect(rowLabels()).toEqual(['CI/CD', 'CLI', 'UI']);
  });

  it('renders the display label for known tokens', () => {
    render(
      <ConsumptionBySourceChart
        sourceBreakdown={dataset({
          REPO_MANAGER: 5,
          CONTINUOUS_MONITOR: 4,
          CI_CD: 3,
        })}
      />
    );
    const labels = rowLabels();
    expect(labels).toContain('Repository Manager');
    expect(labels).toContain('Continuous Monitoring');
    expect(labels).toContain('CI/CD');
  });

  it('renders unknown tokens with their raw value (forward-compat)', () => {
    render(<ConsumptionBySourceChart sourceBreakdown={dataset({ FOO_BAR: 7 })} />);
    expect(rowLabels()).toEqual(['FOO_BAR']);
  });

  it('caps the visible legend at 5 rows and shows "More sources (N)"', () => {
    render(
      <ConsumptionBySourceChart
        sourceBreakdown={dataset({
          CI_CD: 100,
          IDE: 90,
          CLI: 80,
          REPO_MANAGER: 70,
          CONTINUOUS_MONITOR: 60,
          UI: 50,
          API: 40,
          UNKNOWN: 30,
        })}
      />
    );

    expect(rowLabels()).toHaveLength(5);
    expect(screen.getByText(/More sources \(3\)/i)).toBeInTheDocument();
  });

  it('expands to show all rows when "More sources" is clicked; link flips to "Show fewer"', async () => {
    const user = userEvent.setup();
    render(
      <ConsumptionBySourceChart
        sourceBreakdown={dataset({
          CI_CD: 100,
          IDE: 90,
          CLI: 80,
          REPO_MANAGER: 70,
          CONTINUOUS_MONITOR: 60,
          UI: 50,
          API: 40,
          UNKNOWN: 30,
        })}
      />
    );

    await user.click(screen.getByText(/More sources/i));

    expect(rowLabels()).toHaveLength(8);
    expect(screen.getByText(/Show fewer/i)).toBeInTheDocument();
  });

  it('does NOT render "More sources" link when exactly 5 or fewer entries', () => {
    render(<ConsumptionBySourceChart sourceBreakdown={dataset({ UI: 1, CLI: 2, API: 3, CI_CD: 4, IDE: 5 })} />);
    expect(screen.queryByText(/More sources/i)).not.toBeInTheDocument();
    expect(rowLabels()).toHaveLength(5);
  });

  it('bar widths are proportional to counts (max=100%, half=50%)', () => {
    const { container } = render(
      <ConsumptionBySourceChart sourceBreakdown={dataset({ CI_CD: 200, IDE: 100, CLI: 50 })} />
    );
    const fills = container.querySelectorAll('.iq-usage-source-chart__bar-fill');
    expect(fills[0].style.width).toBe('100%');
    expect(fills[1].style.width).toBe('50%');
    expect(fills[2].style.width).toBe('25%');
  });

  it('renders percentage rounded to integer with % suffix', () => {
    render(
      <ConsumptionBySourceChart
        sourceBreakdown={dataset({ CI_CD: 256, IDE: 160, CLI: 93, REPO_MANAGER: 59, CONTINUOUS_MONITOR: 32 })}
      />
    );
    expect(screen.getByText('43%')).toBeInTheDocument();
    expect(screen.getByText('27%')).toBeInTheDocument();
    expect(screen.getByText('16%')).toBeInTheDocument();
    expect(screen.getByText('10%')).toBeInTheDocument();
    expect(screen.getByText('5%')).toBeInTheDocument();
  });

  it('exports a color for every known source token', () => {
    // jsdom drops inline `background-color: var(--...)` so assert the map directly, not the DOM.
    const expectedTokens = ['CI_CD', 'IDE', 'CLI', 'REPO_MANAGER', 'CONTINUOUS_MONITOR', 'UI', 'API', 'UNKNOWN'];
    expectedTokens.forEach((token) => {
      expect(SOURCE_COLORS[token]).toMatch(/^var\(--nx-swatch-[a-z]+-\d+\)$/);
    });
  });

  it('renders one swatch element per legend row', () => {
    const { container } = render(<ConsumptionBySourceChart sourceBreakdown={dataset({ CI_CD: 10, UI: 5 })} />);
    const swatches = container.querySelectorAll('.iq-usage-source-chart__swatch');
    expect(swatches).toHaveLength(2);
  });

  it('label cell has title attribute for truncation tooltip', () => {
    render(<ConsumptionBySourceChart sourceBreakdown={dataset({ REPO_MANAGER: 10 })} />);
    const labelEl = screen.getByText('Repository Manager');
    expect(labelEl).toHaveAttribute('title', 'Repository Manager');
  });

  it('SOURCE_LABELS exports display strings for all eight known tokens', () => {
    const expected = ['UI', 'CLI', 'CI_CD', 'IDE', 'REPO_MANAGER', 'API', 'CONTINUOUS_MONITOR', 'UNKNOWN'];
    expected.forEach((token) => {
      expect(SOURCE_LABELS[token]).toBeDefined();
      expect(typeof SOURCE_LABELS[token]).toBe('string');
    });
  });

  it('selects the most recent month regardless of array order', () => {
    const unordered = [
      { month: '2026-03', consumed: 100, breakdown: { UI: 100 } },
      { month: '2026-05', consumed: 50, breakdown: { CLI: 50 } },
      { month: '2026-04', consumed: 80, breakdown: { API: 80 } },
    ];
    render(<ConsumptionBySourceChart sourceBreakdown={unordered} />);
    expect(rowLabels()).toEqual(['CLI']);
  });
});
