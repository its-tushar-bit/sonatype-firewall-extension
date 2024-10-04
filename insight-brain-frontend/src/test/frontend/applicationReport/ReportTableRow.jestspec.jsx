/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { remove } from 'ramda';

import ReportTableRow from 'MainRoot/applicationReport/ReportTableRow';
import { render, screen, fireEvent, within } from 'TestRoot/SpecUtil';
import * as applicationReportSelectors from 'MainRoot/applicationReport/applicationReportSelectors';
import { serializeComponentIdentifier } from 'MainRoot/util/componentIdentifierUtils';
import * as productFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';

describe('ReportTableRow component', function () {
  let renderComponent, onClickSpy, minimalProps, selectSelectedReportSpy, selectIsAggregatedSpy, selectIsAutoWaiversSpy;
  const npmProducerComponentKey = serializeComponentIdentifier({
    format: 'npm',
    coordinates: {
      packageId: 'npm-producer',
      version: 'file:../npm-producer',
    },
  });

  const mockReportData = [
    {
      policyThreatLevel: 9,
      waived: false,
      legacyViolation: false,
      directDependency: false,
      dependencyInfo: {
        rootAncestors: [npmProducerComponentKey],
      },
    },
    {
      policyThreatLevel: 9,
      waived: false,
      legacyViolation: false,
      directDependency: false,
      dependencyInfo: {
        rootAncestors: [npmProducerComponentKey],
      },
    },
    {
      policyThreatLevel: 9,
      waived: true,
      legacyViolation: false,
      directDependency: false,
      dependencyInfo: {
        rootAncestors: [npmProducerComponentKey],
      },
    },
    {
      policyThreatLevel: 9,
      waived: false,
      legacyViolation: true,
      directDependency: false,
      dependencyInfo: {
        rootAncestors: [npmProducerComponentKey],
      },
    },
    {
      policyThreatLevel: 0,
      waived: false,
      legacyViolation: false,
      directDependency: false,
      dependencyInfo: {
        rootAncestors: [npmProducerComponentKey],
      },
    },
    {
      policyThreatLevel: 9,
      waived: false,
      legacyViolation: false,
      directDependency: false,
      dependencyInfo: {},
    },
  ];

  beforeEach(function () {
    onClickSpy = jest.fn('onClick').mockImplementation(() => {});
    minimalProps = {
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
        componentIdentifier: {
          format: 'npm',
          coordinates: {
            packageId: 'npm-producer',
            version: 'file:../npm-producer',
          },
        },
        serializedComponentIdentifier: npmProducerComponentKey,
        innerSource: false,
      },
      onClick: onClickSpy,
    };

    selectSelectedReportSpy = jest.spyOn(applicationReportSelectors, 'selectSelectedReport').mockReturnValue({
      allEntries: mockReportData,
    });
    selectIsAggregatedSpy = jest.spyOn(applicationReportSelectors, 'selectIsAggregated').mockReturnValue(false);
    selectIsAutoWaiversSpy = jest.spyOn(productFeaturesSelectors, 'selectIsAutoWaiversEnabled').mockReturnValue(true);

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
    expect(directDependencyIndicator.closest('div')).toHaveClass('iq-dependency-indicator direct');
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
    expect(transitiveDependencyIndicator.closest('div')).toHaveClass('iq-dependency-indicator transitive');
  });

  describe('transitive violations indicator', function () {
    it('renders transitive violations indicator when selectIsAggregated is set, plural scenario', function () {
      selectIsAggregatedSpy.mockReturnValue(true);
      const props = {
        component: {
          ...minimalProps.component,
          innerSource: true,
        },
      };
      renderComponent(props);
      const transitiveViolationsIndicator = screen.getByText('2 transitive violations');

      expect(transitiveViolationsIndicator).toBeVisible();
    });

    it('renders transitive violations indicator when selectIsAggregated is set, singular scenario', function () {
      selectIsAggregatedSpy.mockReturnValue(true);
      selectSelectedReportSpy.mockReturnValue({
        allEntries: remove(0, 1, mockReportData),
      });
      const props = {
        component: {
          ...minimalProps.component,
          innerSource: true,
        },
      };
      renderComponent(props);
      const transitiveViolationsIndicator = screen.getByText('1 transitive violation');

      expect(transitiveViolationsIndicator).toBeVisible();
    });

    it('does not render transitive violations indicator when selectIsAggregated is cleared', function () {
      const props = {
        component: {
          ...minimalProps.component,
          innerSource: true,
        },
      };
      renderComponent(props);
      const transitiveViolationsIndicator = screen.queryByText('transitive violation', { exact: false });

      expect(transitiveViolationsIndicator).toBeNull();
    });

    it('does not render transitive violations indicator when innerSource is false', function () {
      selectIsAggregatedSpy.mockReturnValue(true);
      renderComponent();
      const transitiveViolationsIndicator = screen.queryByText('transitive violation', { exact: false });

      expect(transitiveViolationsIndicator).toBeNull();
    });
  });

  it('renders dependency indicators for a transitive InnerSource dependency type', function () {
    const component = {
      derivedDependencyType: 'transitive',
      isOnlyInnerSourceTransitiveDependency: true,
    };

    renderComponent({ component });

    expect(screen.getByText('T')).toBeVisible();
    expect(screen.getByText('IS')).toBeVisible();
  });

  it('renders dependency indicators for a direct InnerSource dependency type', function () {
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

  it('renders legacy policy violations text', function () {
    const props = {
      component: {
        derivedComponentName: 'Component A',
        policyName: 'None',
        policyThreatLevel: 0,
        derivedDependencyType: 'unknown',
        legacyViolation: true,
      },
    };
    renderComponent(props);
    const legacyViolationsIndicator = screen.getByText('Legacy');

    expect(legacyViolationsIndicator).toBeVisible();
  });

  it('renders InnerSource parents tooltip message when the component is brought in by an InnerSource dependency', async function () {
    const props = {
      component: {
        policyName: 'None',
        policyThreatLevel: 0,
        innerSourceParentsDerivedComponentNames: ['Component A', 'Component B'],
        isOnlyInnerSourceTransitiveDependency: true,
      },
    };
    SpecUtil.requestIdleCallbackInvokeImmediateJest();

    renderComponent(props);
    const indicator = screen.getByText('IS');

    fireEvent.mouseOver(indicator);

    const tooltip = await screen.findByRole('tooltip');

    expect(
      within(tooltip).getByText('This component was brought in by the following InnerSource components:', {
        exact: false,
      })
    ).toBeInTheDocument();
  });

  describe('auto waiver indicator', function () {
    const aggregatedView = [
      { isAggregated: false, description: 'Non-Aggregated View' },
      { isAggregated: true, description: 'Aggregated View' },
    ];

    describe.each(aggregatedView)('when $description', ({ isAggregated }) => {
      it('does not render auto waiver indicator when auto waivers are not enabled even is auto waived before', function () {
        selectIsAutoWaiversSpy.mockReturnValue(false);
        selectIsAggregatedSpy.mockReturnValue(isAggregated);
        const props = {
          component: {
            ...minimalProps.component,
            waivedWithAutoWaiver: true,
          },
        };
        renderComponent(props);
        const autoWaiverIndicator = screen.queryByText('Auto');

        expect(autoWaiverIndicator).toBeNull();
      });

      it('renders auto waiver indicator when component has auto waived violations', function () {
        const props = {
          component: {
            ...minimalProps.component,
            waivedWithAutoWaiver: true,
          },
        };
        renderComponent(props);
        selectIsAggregatedSpy.mockReturnValue(isAggregated);
        const autoWaiverIndicator = screen.getByText('Auto');

        expect(autoWaiverIndicator).toBeVisible();
      });

      it('does not render auto waiver indicator when component does not have waived violations', function () {
        renderComponent();
        selectIsAggregatedSpy.mockReturnValue(isAggregated);
        const autoWaiverIndicator = screen.queryByText('Auto');

        expect(autoWaiverIndicator).toBeNull();
      });
    });
  });
});
