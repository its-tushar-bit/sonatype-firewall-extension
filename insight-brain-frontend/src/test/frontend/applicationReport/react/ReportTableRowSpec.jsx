/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import ReportTableRow from 'MainRoot/applicationReport/react/ReportTableRow';
import { render, screen, fireEvent } from '../../SpecUtil';

describe('ReportTableRow component', function () {
  let renderComponent, onClickSpy;

  beforeEach(function () {
    onClickSpy = jasmine.createSpy('onClick');
    const minimalProps = {
      index: 0,
      component: {
        derivedComponentName: 'cryptiles : 3.1.4',
        displayName: {
          name: 'cryptiles : 3.1.4',
          parts: [{ field: 'packageId', value: 'cryptiles' }, { value: ' : ' }, { field: 'version', value: '3.1.4' }],
        },
        policyName: 'Security-High',
        policyThreatLevel: 9,
        filenames: ['cryptiles:3.1.4'],
      },
      onClick: onClickSpy,
    };

    renderComponent = (additionalProps = {}) => render(<ReportTableRow {...minimalProps} {...additionalProps} />);
  });

  it('renders a the policy, threat and component cells', function () {
    renderComponent();
    const threat = screen.getByText('9');
    const policy = screen.getByText('Security-High');
    const component = screen.getByText('cryptiles : 3.1.4');
    expect(threat).toBeVisible();
    expect(policy).toBeVisible();
    expect(component).toBeVisible();
  });

  it('calls onClick', () => {
    renderComponent();
    const clickable = screen.getByRole('row');
    fireEvent.click(clickable);
    expect(onClickSpy).toHaveBeenCalledTimes(1);
  });

  it('renders properties with direct dependency type', function () {
    const props = {
      component: {
        derivedComponentName: 'Component A',
        policyName: 'None',
        policyThreatLevel: 0,
        derivedDependencyType: 'direct',
      },
    };
    renderComponent(props);
    const directDependencyIndicator = screen.getByText('D');

    expect(directDependencyIndicator).toBeVisible();
    expect(directDependencyIndicator.closest('div')).toHaveClassName('iq-dependency-indicator direct');
  });

  it('renders properties with transitive dependency type', function () {
    const props = {
      component: {
        derivedComponentName: 'Component A',
        policyName: 'None',
        policyThreatLevel: 0,
        derivedDependencyType: 'transitive',
      },
    };
    renderComponent(props);
    const transitiveDependencyIndicator = screen.getByText('T');

    expect(transitiveDependencyIndicator).toBeVisible();
    expect(transitiveDependencyIndicator.closest('div')).toHaveClassName('iq-dependency-indicator transitive');
  });

  it('renders dependency indicators for a transitive inner source dependency type', function () {
    const component = {
      derivedDependencyType: 'transitive',
      isOnlyInnerSourceTransitiveDependency: true,
    };

    renderComponent({ component });

    expect(screen.getByText('T')).toBeVisible();
    expect(screen.getByText('IS')).toBeVisible();
  });

  it('renders dependency indicators for a direct inner source dependency type', function () {
    const component = {
      derivedDependencyType: 'direct',
      innerSource: true,
    };

    renderComponent({ component });

    expect(screen.getByText('D')).toBeVisible();
    expect(screen.getByText('IS')).toBeVisible();
  });

  it('should not render dependency indicators for an unknown dependency type', function () {
    const component = {
      derivedDependencyType: 'unknown',
    };

    renderComponent({ component });

    expect(screen.queryByText('D')).toBeNull();
    expect(screen.queryByText('T')).toBeNull();
    expect(screen.queryByText('IS')).toBeNull();
  });

  it('renders properties waived', function () {
    const props = {
      component: {
        derivedComponentName: 'Component A',
        policyName: 'None',
        policyThreatLevel: 0,
        derivedDependencyType: 'unknown',
        waived: true,
      },
    };
    renderComponent(props);
    const waivedIndicator = screen.getByText('Waived');

    expect(waivedIndicator).toBeVisible();
  });

  it('renders grandfathered text', function () {
    const props = {
      component: {
        derivedComponentName: 'Component A',
        policyName: 'None',
        policyThreatLevel: 0,
        derivedDependencyType: 'unknown',
        grandfathered: true,
      },
    };
    renderComponent(props);
    const grandfatheredIndicator = screen.getByText('Grandfathered');

    expect(grandfatheredIndicator).toBeVisible();
  });
});
