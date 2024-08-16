/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { RecommendedRemediation } from 'MainRoot/componentDetails/overview/riskRemediation/RecommendedRemediation';
import * as routerStateContext from 'MainRoot/react/RouterStateContext';
import { render, screen, within } from 'TestRoot/SpecUtil';

describe('RecommendedRemediation', () => {
  let renderComponent, hrefSpy, getSpy;

  const minimalProps = {
    ancestors: [],
    routeName: 'applicationReport.componentDetails.overview',
  };

  beforeEach(() => {
    hrefSpy = jest.fn('href').mockImplementation((stateName) => stateName);
    getSpy = jest.fn('get').mockImplementation((state) => state);

    const routerContextMock = { href: hrefSpy, get: getSpy };
    jest.spyOn(routerStateContext, 'useRouterState').mockReturnValue(routerContextMock);

    renderComponent = (props) => render(<RecommendedRemediation {...props} {...minimalProps} />);
  });

  it('header title and content should contain proper wording', () => {
    renderComponent();
    expect(screen.getByRole('heading', { name: 'Recommended Remediation' })).toBeInTheDocument();

    expect(
      screen.getByText(
        'The direct dependencies that brought in this component are listed below. Clicking on a component will take you to its Component Details Page.'
      )
    ).toBeInTheDocument();
  });

  it('The Tile contains a list with one ancestor link', () => {
    const dependencyTreeSubset = [
      {
        hash: '502f98a535313e13cf18',
        displayName: 'org.springframework.data : spring-data-rest-hal-explorer : 3.4.11',
      },
    ];
    renderComponent({
      dependencyTreeSubset,
    });

    const listItems = screen.getAllByRole('listitem');
    expect(listItems.length).toBe(1);

    const link = within(listItems[0]).getByRole('link', {
      name: /org.springframework.data : spring-data-rest-hal-explorer : 3.4.11/i,
    });
    expect(link).toBeInTheDocument();
    expect(link).toHaveAttribute('href', 'applicationReport.componentDetails.overview');
  });

  it('The Tile contains a list with three ancestor links', () => {
    const dependencyTreeSubset = [
      {
        hash: '502f98a535313e13cf18',
        displayName: 'org.springframework.data : spring-data-rest-hal-explorer : 3.4.11',
      },
      {
        hash: '502f98a535313e13cf18',
        displayName: 'org.springframework.boot : spring-boot-actuator-autoconfigure : 2.4.9',
      },
      {
        hash: '502f98a535313e13cf18',
        displayName: 'com.fasterxml.jackson.module : jackson-module-parameter-names : 2.11.4',
      },
    ];
    renderComponent({
      dependencyTreeSubset,
    });

    const listItems = screen.getAllByRole('listitem');
    expect(listItems.length).toBe(3);

    const firstLink = within(listItems[0]).getByRole('link', {
      name: /org.springframework.data : spring-data-rest-hal-explorer : 3.4.11/i,
    });
    expect(firstLink).toBeInTheDocument();

    const secondLink = within(listItems[1]).getByRole('link', {
      name: /org.springframework.boot : spring-boot-actuator-autoconfigure : 2.4.9/i,
    });
    expect(secondLink).toBeInTheDocument();

    const thirdLink = within(listItems[2]).getByRole('link', {
      name: /com.fasterxml.jackson.module : jackson-module-parameter-names : 2.11.4/i,
    });
    expect(thirdLink).toBeInTheDocument();
  });

  it('The Tile contains a list with four ancestor links and a show more link', () => {
    const dependencyTreeSubset = [
      {
        hash: '502f98a535313e13cf18',
        displayName: 'org.springframework.data : spring-data-rest-hal-explorer : 3.4.11',
      },
      {
        hash: '502f98a535313e13cf18',
        displayName: 'org.springframework.boot : spring-boot-actuator-autoconfigure : 2.4.9',
      },
      {
        hash: '502f98a535313e13cf18',
        displayName: 'com.fasterxml.jackson.module : jackson-module-parameter-names : 2.11.4',
      },
      {
        hash: '502f98a535313e13cf128',
        displayName: 'com.somename.othername.modules : jackson-some-parameter-name : 2.0.4',
      },
    ];

    renderComponent({
      dependencyTreeSubset,
      expanded: false,
    });

    const listItems = screen.getAllByRole('listitem');
    expect(listItems.length).toBe(3);

    const firstLink = within(listItems[0]).getByRole('link', {
      name: /org.springframework.data : spring-data-rest-hal-explorer : 3.4.11/i,
    });
    expect(firstLink).toBeInTheDocument();

    const secondLink = within(listItems[1]).getByRole('link', {
      name: /org.springframework.boot : spring-boot-actuator-autoconfigure : 2.4.9/i,
    });
    expect(secondLink).toBeInTheDocument();

    const thirdLink = within(listItems[2]).getByRole('link', {
      name: /com.fasterxml.jackson.module : jackson-module-parameter-names : 2.11.4/i,
    });
    expect(thirdLink).toBeInTheDocument();

    expect(screen.getByRole('button', { name: 'Show more' })).toBeInTheDocument();
  });

  it('The Tile contains a list with four ancestor links and a show more link', () => {
    const dependencyTreeSubset = [
      {
        hash: '502f98a535313e13cf18',
        displayName: 'org.springframework.data : spring-data-rest-hal-explorer : 3.4.11',
      },
      {
        hash: '502f98a535313e13cf18',
        displayName: 'org.springframework.boot : spring-boot-actuator-autoconfigure : 2.4.9',
      },
      {
        hash: '502f98a535313e13cf18',
        displayName: 'com.fasterxml.jackson.module : jackson-module-parameter-names : 2.11.4',
      },
      {
        hash: '502f98a535313e13cf128',
        displayName: 'com.somename.othername.modules : jackson-some-parameter-name : 2.0.4',
      },
    ];

    renderComponent({
      dependencyTreeSubset,
      expanded: true,
    });

    const listItems = screen.getAllByRole('listitem');
    expect(listItems.length).toBe(4);

    const firstLink = within(listItems[0]).getByRole('link', {
      name: /org.springframework.data : spring-data-rest-hal-explorer : 3.4.11/i,
    });
    expect(firstLink).toBeInTheDocument();

    const secondLink = within(listItems[1]).getByRole('link', {
      name: /org.springframework.boot : spring-boot-actuator-autoconfigure : 2.4.9/i,
    });
    expect(secondLink).toBeInTheDocument();

    const thirdLink = within(listItems[2]).getByRole('link', {
      name: /com.fasterxml.jackson.module : jackson-module-parameter-names : 2.11.4/i,
    });
    expect(thirdLink).toBeInTheDocument();

    expect(screen.getByRole('button', { name: 'Show less' })).toBeInTheDocument();
  });

  it('renders alert if dependencyTreeSubset is empty and dependencyTree is supported', () => {
    renderComponent({
      dependencyTreeSubset: [],
    });
    expect(screen.getByRole('img', { name: /info/i })).toBeInTheDocument();
    expect(screen.getByText('Dependency info not available for this report.')).toBeInTheDocument();
  });

  it('renders alert prompting to re-scan if dependencyTreeSubset is empty and dependencyTree is not supported', () => {
    renderComponent({
      dependencyTreeSubset: [],
      dependencyTreeIsNotSupported: true,
    });

    expect(
      screen.getByText('Dependency info not available for this report. Please re-scan the application.')
    ).toBeInTheDocument();
  });
});
