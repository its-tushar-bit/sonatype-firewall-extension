/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import moment from 'moment';
import { render, screen, within } from 'TestRoot/SpecUtil';
import { formatDate, SIMPLE_TIME_FORMAT } from 'MainRoot/util/dateUtils';
import SourceControlAutomatedPullRequestTable from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/SourceControlAutomatedPullRequestTable';

let renderComponent, minimalProps, currentTimeInMilliseconds, formattedTime, defaultPrs;

describe('SourceControlAutomatedPullRequestTable', () => {
  beforeEach(function () {
    currentTimeInMilliseconds = moment().milliseconds();
    formattedTime = formatDate(currentTimeInMilliseconds, SIMPLE_TIME_FORMAT);
    defaultPrs = [
      {
        exceptionThrown: false,
        reasoning: 'a PR was successfully created',
        startTime: currentTimeInMilliseconds,
        successful: true,
        title: 'bump bad-component to 1.1.11',
        totalTime: 1234,
      },
      {
        exceptionThrown: 'bad credentials',
        reasoning: 'a failed attempt to create a PR',
        startTime: currentTimeInMilliseconds,
        successful: false,
        title: 'bump bad-component-plus to 1.0',
        totalTime: 5678,
      },
      {
        exceptionThrown: false,
        reasoning: 'a PR was not successfully created',
        startTime: currentTimeInMilliseconds,
        successful: false,
        title: 'bump bad-component-plus to 1.1112',
        totalTime: 4321,
      },
    ];

    minimalProps = { automatedPullRequests: defaultPrs };

    renderComponent = (additionalProps = {}) =>
      render(<SourceControlAutomatedPullRequestTable {...minimalProps} {...additionalProps} />);
  });

  it('renders a table with the expected columns', () => {
    renderComponent();
    expect(screen.getByRole('table')).toBeVisible();
    expect(screen.getByRole('columnheader', { name: 'Title' })).toBeVisible();
    expect(screen.getByRole('columnheader', { name: 'PR Created?' })).toBeVisible();
    expect(screen.getByRole('columnheader', { name: 'Time Spent (MS)' })).toBeVisible();
    expect(screen.getByRole('columnheader', { name: 'Errors' })).toBeVisible();
    expect(screen.getByRole('columnheader', { name: 'Started' })).toBeVisible();
  });

  it('renders expected rows when there is content to display', () => {
    renderComponent();

    const rows = screen.getAllByRole('row');

    expect(within(rows[1]).getByRole('cell', { name: 'bump bad-component to 1.1.11' })).toBeVisible();
    expect(rows[1].querySelector('.pr-created-success-icon')).toBeInTheDocument();
    expect(within(rows[1]).getByRole('cell', { name: '1234' })).toBeVisible();
    expect(within(rows[1]).getByRole('cell', { name: 'false' })).toBeVisible();
    expect(within(rows[1]).getByRole('cell', { name: formattedTime })).toBeVisible();

    expect(within(rows[2]).getByRole('cell', { name: 'bump bad-component-plus to 1.0' })).toBeVisible();
    expect(rows[2].querySelector('.pr-created-warning-icon')).toBeInTheDocument();
    expect(within(rows[2]).getByRole('cell', { name: '5678' })).toBeVisible();
    expect(within(rows[2]).getByRole('cell', { name: 'bad credentials' })).toBeVisible();
    expect(within(rows[2]).getByRole('cell', { name: formattedTime })).toBeVisible();

    expect(within(rows[3]).getByRole('cell', { name: 'bump bad-component-plus to 1.1112' })).toBeVisible();
    expect(rows[3].querySelector('.pr-created-warning-icon')).toBeInTheDocument();
    expect(within(rows[3]).getByRole('cell', { name: '4321' })).toBeVisible();
    expect(within(rows[3]).getByRole('cell', { name: 'false' })).toBeVisible();
    expect(within(rows[3]).getByRole('cell', { name: formattedTime })).toBeVisible();
  });

  it('renders empty message when there are no rows to display', () => {
    renderComponent({ automatedPullRequests: [] });
    expect(screen.getByRole('row', { name: 'No results available' })).toBeVisible();
  });
});
