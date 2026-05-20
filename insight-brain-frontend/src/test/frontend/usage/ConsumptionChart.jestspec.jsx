/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import { render, screen } from 'TestRoot/SpecUtil';

jest.mock('recharts', () => {
  const actual = jest.requireActual('recharts');
  const ReactLib = jest.requireActual('react');
  const MockedResponsiveContainer = ({ children }) =>
    ReactLib.createElement(
      'div',
      { style: { width: 800, height: 400 } },
      ReactLib.cloneElement(children, { width: 800, height: 400 })
    );
  return {
    ...actual,
    ResponsiveContainer: MockedResponsiveContainer,
  };
});

import ConsumptionChart from 'MainRoot/usage/ConsumptionChart';

describe('ConsumptionChart', () => {
  function entry(month, breakdown) {
    return { month, breakdown };
  }

  it('renders nothing when historyBreakdown is null', () => {
    const { container } = render(
      <ConsumptionChart historyBreakdown={null} aggregation="monthly" onAggregationChange={() => {}} />
    );
    expect(container).toBeEmptyDOMElement();
  });

  it('renders nothing when historyBreakdown is empty', () => {
    const { container } = render(
      <ConsumptionChart historyBreakdown={[]} aggregation="monthly" onAggregationChange={() => {}} />
    );
    expect(container).toBeEmptyDOMElement();
  });

  it('renders the tile header when data is present', () => {
    render(
      <ConsumptionChart
        historyBreakdown={[entry('2026-01-01', { 'App Scan + Re-evaluate': 100 })]}
        aggregation="monthly"
        onAggregationChange={() => {}}
      />
    );
    expect(screen.getByText('Consumption by Type')).toBeInTheDocument();
  });

  it('renders aggregation select with the current value', () => {
    render(
      <ConsumptionChart
        historyBreakdown={[entry('2026-01-01', { 'App Scan + Re-evaluate': 100 })]}
        aggregation="weekly"
        onAggregationChange={() => {}}
      />
    );
    const select = screen.getByRole('combobox');
    expect(select).toHaveValue('weekly');
  });

  it('calls onAggregationChange when the aggregation select is changed', async () => {
    const user = userEvent.setup();
    const onAggregationChange = jest.fn();
    render(
      <ConsumptionChart
        historyBreakdown={[entry('2026-01-01', { 'App Scan + Re-evaluate': 100 })]}
        aggregation="monthly"
        onAggregationChange={onAggregationChange}
      />
    );

    await user.selectOptions(screen.getByRole('combobox'), 'daily');

    expect(onAggregationChange).toHaveBeenCalled();
  });

  it('renders the aggregation options: daily, weekly, monthly', () => {
    render(
      <ConsumptionChart
        historyBreakdown={[entry('2026-01-01', { 'App Scan + Re-evaluate': 100 })]}
        aggregation="monthly"
        onAggregationChange={() => {}}
      />
    );
    expect(screen.getByRole('option', { name: 'Daily' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Weekly' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Monthly' })).toBeInTheDocument();
  });

  it('accepts multi-month data without crashing', () => {
    const { container } = render(
      <ConsumptionChart
        historyBreakdown={[
          entry('2026-01-01', { 'App Scan + Re-evaluate': 100, 'Continuous Monitoring': 30 }),
          entry('2026-02-01', { 'App Scan + Re-evaluate': 200, 'Continuous Monitoring': 50 }),
          entry('2026-03-01', { 'App Scan + Re-evaluate': 150, 'Component Details': 20 }),
        ]}
        aggregation="monthly"
        onAggregationChange={() => {}}
      />
    );
    expect(container).not.toBeEmptyDOMElement();
    expect(screen.getByText('Consumption by Type')).toBeInTheDocument();
  });

  it('renders a limit reference label when monthlyLimit is provided', () => {
    render(
      <ConsumptionChart
        historyBreakdown={[entry('2026-01-01', { 'App Scan + Re-evaluate': 100 })]}
        aggregation="monthly"
        onAggregationChange={() => {}}
        monthlyLimit={20000}
      />
    );
    expect(screen.getByText('Limit: 20k')).toBeInTheDocument();
  });

  it('does not render a limit label when monthlyLimit is null', () => {
    render(
      <ConsumptionChart
        historyBreakdown={[entry('2026-01-01', { 'App Scan + Re-evaluate': 100 })]}
        aggregation="monthly"
        onAggregationChange={() => {}}
        monthlyLimit={null}
      />
    );
    expect(screen.queryByText(/^Limit:/)).not.toBeInTheDocument();
  });

  it('does not render a limit label when monthlyLimit is zero', () => {
    render(
      <ConsumptionChart
        historyBreakdown={[entry('2026-01-01', { 'App Scan + Re-evaluate': 100 })]}
        aggregation="monthly"
        onAggregationChange={() => {}}
        monthlyLimit={0}
      />
    );
    expect(screen.queryByText(/^Limit:/)).not.toBeInTheDocument();
  });

  it('renders overage shading when total consumption exceeds the limit', () => {
    const { container } = render(
      <ConsumptionChart
        historyBreakdown={[entry('2026-01-01', { 'App Scan + Re-evaluate': 25000 })]}
        aggregation="monthly"
        onAggregationChange={() => {}}
        monthlyLimit={20000}
      />
    );
    expect(container.querySelector('.recharts-reference-area-rect')).toBeInTheDocument();
  });

  it('does not render overage shading when total consumption is below the limit', () => {
    const { container } = render(
      <ConsumptionChart
        historyBreakdown={[entry('2026-01-01', { 'App Scan + Re-evaluate': 1000 })]}
        aggregation="monthly"
        onAggregationChange={() => {}}
        monthlyLimit={20000}
      />
    );
    expect(container.querySelector('.recharts-reference-area-rect')).not.toBeInTheDocument();
  });
});
