/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { AncestorsList } from 'MainRoot/componentDetails/overview/riskRemediation/AncestorsList';
import * as routerStateContext from 'MainRoot/react/RouterStateContext';
import { render, screen, within } from 'TestRoot/SpecUtil';

describe('AncestorsList', () => {
  let renderComponent, hrefSpy, getSpy;

  const defaultPreloadedState = {
    router: {
      currentState: {
        name: 'applicationReport.componentDetails.overview',
      },
    },
  };

  beforeEach(() => {
    hrefSpy = jest.fn('href').mockImplementation((stateName) => {
      if (stateName === 'applicationReport.componentDetails.overview') {
        return 'applicationReport.componentDetails.overview';
      }
      return 'prioritiesPageFromReports.componentDetails.overview';
    });
    getSpy = jest.fn('get').mockImplementation((state) => state);

    const routerContextMock = { href: hrefSpy, get: getSpy };
    jest.spyOn(routerStateContext, 'useRouterState').mockReturnValue(routerContextMock);

    renderComponent = (preloadedState, props) =>
      render(<AncestorsList {...props} />, { preloadedState: preloadedState || defaultPreloadedState });
  });

  it('renders an empty list when dependencyTreeSubset is empty', () => {
    const minimalProps = {
      dependencyTreeSubset: [],
    };
    renderComponent(null, minimalProps);
    expect(screen.getByRole('list')).toBeInTheDocument();
  });

  it('renders a list with one ancestor link', () => {
    const dependencyTreeSubset = [
      {
        hash: '502f98a535313e13cf18',
        displayName: 'org.springframework.data : spring-data-rest-hal-explorer : 3.4.11',
      },
    ];

    const props = {
      dependencyTreeSubset,
    };

    renderComponent(null, props);
    const listItems = screen.getAllByRole('listitem');
    expect(listItems.length).toBe(1);

    const link = within(listItems[0]).getByRole('link', {
      name: /org.springframework.data : spring-data-rest-hal-explorer : 3.4.11/i,
    });
    expect(link).toBeInTheDocument();
    expect(link).toHaveAttribute('href', 'applicationReport.componentDetails.overview');
  });

  it('renders a list with one ancestor link with innerSource label if isInnerSource is true', () => {
    const dependencyTreeSubset = [
      {
        hash: 'somehash',
        displayName: 'somedisplayname',
        isInnerSource: true,
      },
    ];

    const props = {
      dependencyTreeSubset,
    };

    renderComponent(null, props);
    const listItems = screen.getAllByRole('listitem');
    expect(listItems.length).toBe(1);

    expect(within(listItems[0]).getByRole('link', { name: /somedisplayname/i })).toBeInTheDocument();
    expect(within(listItems[0]).getByText('InnerSource')).toBeInTheDocument();
  });

  it('renders a link if rendered within priorities page', () => {
    const dependencyTreeSubset = [
      {
        hash: 'somehash',
        displayName: 'somedisplayname',
        isInnerSource: true,
      },
    ];

    const preloadedState = {
      router: {
        currentState: {
          name: 'componentDetailsPageWithinPrioritiesPageContainerFromDashboard.componentDetails.overview',
        },
      },
    };

    const props = {
      dependencyTreeSubset,
    };

    renderComponent(preloadedState, props);
    const listItems = screen.getAllByRole('listitem');
    expect(listItems.length).toBe(1);

    const link = within(listItems[0]).getByRole('link', { name: /somedisplayname/i });
    expect(link).toBeInTheDocument();
    expect(link).toHaveAttribute('href', 'prioritiesPageFromReports.componentDetails.overview');
  });
});
