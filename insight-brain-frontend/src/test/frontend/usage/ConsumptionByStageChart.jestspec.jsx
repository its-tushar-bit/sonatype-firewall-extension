/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import ConsumptionByStageChart from 'MainRoot/usage/ConsumptionByStageChart';

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

  it('sorts legend rows by count descending', () => {
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

  it('renders a percent for every legend row matching count/total', () => {
    // 700 total: build=300 (43%), release=200 (29%), Unknown=200 (29%)
    render(<ConsumptionByStageChart stageBreakdown={dataset({ build: 300, release: 200, Unknown: 200 })} />);
    expect(screen.getByText('43%')).toBeInTheDocument();
    expect(screen.getAllByText('29%')).toHaveLength(2);
  });
});
