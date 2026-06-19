/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { ResponsivePie } from '@nivo/pie';
import { render, screen } from 'TestRoot/SpecUtil';
import ConsumptionByStageChart from 'MainRoot/usage/ConsumptionByStageChart';

jest.mock('@nivo/pie', () => ({
  ResponsivePie: jest.fn(() => null),
}));

describe('ConsumptionByStageChart', () => {
  function dataset(breakdown) {
    const consumed = Object.values(breakdown).reduce((acc, n) => acc + n, 0);
    return [{ month: '2026-05', consumed, breakdown }];
  }

  function rowLabels() {
    return screen
      .getAllByRole('listitem')
      .map((li) => {
        const labelEl = li.querySelector('.iq-usage-stage-chart__label');
        return labelEl ? labelEl.textContent : null;
      })
      .filter(Boolean);
  }

  it('renders nothing when stageBreakdown is null', () => {
    const { container } = render(<ConsumptionByStageChart stageBreakdown={null} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('renders nothing when stageBreakdown is empty array', () => {
    const { container } = render(<ConsumptionByStageChart stageBreakdown={[]} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('renders nothing when all counts are zero', () => {
    const { container } = render(<ConsumptionByStageChart stageBreakdown={dataset({ build: 0, release: 0 })} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('renders the tile title', () => {
    render(<ConsumptionByStageChart stageBreakdown={dataset({ build: 10 })} />);
    expect(screen.getByText('Consumption by Stage')).toBeInTheDocument();
  });

  it('sorts legend rows by canonical phase order', () => {
    render(<ConsumptionByStageChart stageBreakdown={dataset({ build: 250, release: 132, 'stage-release': 157 })} />);
    expect(rowLabels()).toEqual(['Build', 'Stage Release', 'Release']);
  });

  it('renders display labels for known tokens', () => {
    render(
      <ConsumptionByStageChart stageBreakdown={dataset({ build: 5, 'stage-release': 4, 'continuous-monitoring': 3 })} />
    );
    const labels = rowLabels();
    expect(labels).toContain('Build');
    expect(labels).toContain('Stage Release');
    expect(labels).toContain('Continuous Monitoring');
  });

  it('renders unknown tokens with their raw value (forward-compat)', () => {
    render(<ConsumptionByStageChart stageBreakdown={dataset({ 'some-future-stage': 7 })} />);
    expect(rowLabels()).toEqual(['some-future-stage']);
  });

  it('renders all stage rows without truncation (no "More stages" link)', () => {
    render(
      <ConsumptionByStageChart
        stageBreakdown={dataset({
          build: 100,
          'stage-release': 90,
          release: 80,
          operate: 70,
          develop: 60,
          'continuous-monitoring': 50,
          proxy: 40,
        })}
      />
    );
    expect(rowLabels()).toHaveLength(7);
    expect(screen.queryByText(/More stages/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/Show fewer/i)).not.toBeInTheDocument();
  });

  it('picks the latest month when the array contains multiple', () => {
    render(
      <ConsumptionByStageChart
        stageBreakdown={[
          { month: '2026-03', consumed: 1, breakdown: { release: 1 } },
          { month: '2026-05', consumed: 2, breakdown: { build: 2 } },
        ]}
      />
    );
    expect(rowLabels()).toEqual(['Build']);
  });

  it('renders no inline percentage in stage legend rows', () => {
    // percent spans were removed in favour of bar-track visualisation
    const { container } = render(
      <ConsumptionByStageChart stageBreakdown={dataset({ build: 300, release: 200, Unknown: 200 })} />
    );
    expect(container.querySelector('.iq-usage-stage-chart__percent')).toBeNull();
  });

  const allStages = [
    {
      month: '2026-06',
      consumed: 700,
      breakdown: {
        operate: 100,
        release: 100,
        'stage-release': 100,
        build: 100,
        develop: 100,
        proxy: 50,
        'continuous-monitoring': 100,
        Unknown: 50,
      },
    },
  ];

  it('orders legend by canonical phase order (Develop, Build, Stage Release, Release, Operate, then non-canonical, then Unknown)', () => {
    render(<ConsumptionByStageChart stageBreakdown={allStages} />);
    const labels = screen.getAllByTitle(
      /^(Develop|Build|Stage Release|Release|Operate|Continuous Monitoring|Proxy|Unknown)$/
    );
    // .map keeps DOM order
    const order = labels.map((el) => el.getAttribute('title'));
    // canonical first
    expect(order.indexOf('Develop')).toBeLessThan(order.indexOf('Build'));
    expect(order.indexOf('Build')).toBeLessThan(order.indexOf('Stage Release'));
    expect(order.indexOf('Stage Release')).toBeLessThan(order.indexOf('Release'));
    expect(order.indexOf('Release')).toBeLessThan(order.indexOf('Operate'));
    // non-canonical after Operate
    expect(order.indexOf('Operate')).toBeLessThan(order.indexOf('Continuous Monitoring'));
    expect(order.indexOf('Operate')).toBeLessThan(order.indexOf('Proxy'));
    // Unknown last
    expect(order.indexOf('Unknown')).toBe(order.length - 1);
  });

  it('configures Stage ResponsivePie with division-line props (borderWidth/borderColor) — couples to Nivo prop API', () => {
    render(<ConsumptionByStageChart stageBreakdown={allStages} />);
    const props = ResponsivePie.mock.calls.at(-1)[0];
    expect(props.borderWidth).toBe(2);
    expect(props.borderColor).toBe('var(--nx-color-component-background)');
  });

  it('hover tooltip renders count + percent (Figma annotation #7 for stage donut)', () => {
    // total in allStages: build 200 + release 100 + Unknown 100 = 400.
    render(<ConsumptionByStageChart stageBreakdown={dataset({ build: 200, release: 100, Unknown: 100 })} />);
    const props = ResponsivePie.mock.calls.at(-1)[0];

    const buildTooltip = render(props.tooltip({ datum: { label: 'Build', value: 200 } }));
    expect(buildTooltip.container.textContent).toMatch(/Build.*200.*\(50%\)/);

    const releaseTooltip = render(props.tooltip({ datum: { label: 'Release', value: 100 } }));
    expect(releaseTooltip.container.textContent).toMatch(/Release.*100.*\(25%\)/);
  });

  it('bar widths are proportional to counts using true max, not first-canonical count', () => {
    // Develop (100) comes first in canonical order but Build (1000) has the highest count.
    // maxCount must be 1000 (Build), so Develop bar = 10% and Build bar = 100%.
    const stagesWithBuildLargest = [
      {
        month: '2026-06',
        consumed: 1100,
        breakdown: { develop: 100, build: 1000 },
      },
    ];
    const { container } = render(<ConsumptionByStageChart stageBreakdown={stagesWithBuildLargest} />);
    const fills = container.querySelectorAll('.iq-usage-stage-chart__bar-fill');
    // After canonical sort: Develop is index 0, Build is index 1
    expect(fills[0].style.width).toBe('10%'); // develop: 100/1000 * 100 = 10%
    expect(fills[1].style.width).toBe('100%'); // build: 1000/1000 * 100 = 100%
  });
});
