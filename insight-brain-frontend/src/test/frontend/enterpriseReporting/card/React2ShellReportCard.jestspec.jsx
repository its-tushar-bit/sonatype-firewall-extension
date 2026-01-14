/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen } from '@testing-library/dom';
import { render } from 'TestRoot/SpecUtil';
import React2ShellReportCard from 'MainRoot/enterpriseReporting/card/React2ShellReportCard';
import * as RouterStateContext from 'MainRoot/react/RouterStateContext';

describe('React2ShellReportCard', () => {
  let mockRouterState;

  beforeEach(() => {
    mockRouterState = {
      href: jest.fn().mockImplementation((stateName) => {
        if (stateName === 'react2ShellReport') {
          return '#/reports/react2shell';
        }
        return '#/default';
      }),
      get: jest.fn(),
      includes: jest.fn(),
    };
    jest.spyOn(RouterStateContext, 'useRouterState').mockReturnValue(mockRouterState);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  const renderComponent = () => render(<React2ShellReportCard />);

  it('should render the React2Shell report card with correct content', () => {
    renderComponent();

    expect(screen.getByRole('region', { name: 'React2Shell Impact' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /React2Shell Impact/i })).toBeInTheDocument();
    expect(
      screen.getByText(/A severe flaw in React Server Components could allow attackers to run arbitrary code/i)
    ).toBeInTheDocument();
  });

  it('should display the NEW spotlight tag', () => {
    renderComponent();

    const newTag = screen.getByText('NEW');
    expect(newTag).toBeInTheDocument();

    const spotlightTag = newTag.closest('.iq-enterprise-reporting-card__spotlight');
    expect(spotlightTag).toBeInTheDocument();
  });

  it('should display feature list with check icons', () => {
    renderComponent();

    expect(screen.getByText('Identify affected applications')).toBeInTheDocument();
    expect(screen.getByText('Prioritize remediation efforts')).toBeInTheDocument();

    const checkIcons = document.querySelectorAll('.enterprise');
    expect(checkIcons.length).toBe(2);
  });

  it('should render the view report link', () => {
    renderComponent();

    const link = screen.getByRole('link', { name: /View React2Shell Impact/i });
    expect(link).toBeInTheDocument();
    expect(link).toHaveAttribute('id', 'react2shell-dashboard-btn');
    expect(link).toHaveAttribute('href', '#/reports/react2shell');
  });

  it('should display warning icon from fontawesome', () => {
    renderComponent();

    const icon = document.querySelector('.iq-enterprise-reporting-card__icon.warning .nx-icon');
    expect(icon).toBeInTheDocument();
  });
});
