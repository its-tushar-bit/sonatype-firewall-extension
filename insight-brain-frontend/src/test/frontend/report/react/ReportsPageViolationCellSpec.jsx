/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, fireEvent } from 'TestRoot/SpecUtil';
import ReportsPageViolationCell from 'MainRoot/report/react/ReportsPageViolationCell';

describe('ReportsPageViolationCell', () => {
  let renderComponent, hrefUiRouterStateSpy, props;

  beforeEach(() => {
    hrefUiRouterStateSpy = jasmine.createSpy('hrefUiRouterState');
    props = {
      stage: 'build',
      app: {
        id: '137a638c1a474963b464a81e50be9fe4',
        publicId: 'CDPAPPGO',
        name: 'Test app',
        organizationId: 'fce7dfaa30b047d585190ac632a07bfd',
        organizationName: 'CDP Go',
        policyEvaluations: {
          build: {
            id: '1c09f8e97e9247cfb37778462efbc205',
            applicationId: '137a638c1a474963b464a81e50be9fe4',
            stageTypeId: 'build',
            scanId: '4d8ad4f41b7d46a79d95e1359d40b861',
            time: 1636403882656,
            commitHash: null,
            initiator: 'admin',
            scanTriggerType: 'WEB_UI',
            forObsoleteScan: false,
            reevaluation: false,
            forMonitoring: false,
          },
        },
        policyEvaluationsResults: {
          build: {
            alerts: [],
            affectedComponentCount: 664,
            criticalComponentCount: 649,
            severeComponentCount: 13,
            moderateComponentCount: 2,
            criticalPolicyViolationCount: 709,
            severePolicyViolationCount: 152,
            moderatePolicyViolationCount: 7,
            grandfatheredPolicyViolationCount: 0,
            totalComponentCount: 1157,
          },
        },
        contact: {
          internalName: null,
          displayName: null,
          email: null,
          realm: null,
          error: null,
        },
        hasPendingSourceControlPolicyEvaluation: false,
      },
      hrefUiRouterState: hrefUiRouterStateSpy,
    };
    renderComponent = (props) => render(<ReportsPageViolationCell {...props} />);
  });

  describe('when there are no violations', () => {
    beforeEach(() => {
      const noViolationResults = {
        build: {
          alerts: [],
          affectedComponentCount: 664,
          criticalComponentCount: 0,
          severeComponentCount: 0,
          moderateComponentCount: 0,
          criticalPolicyViolationCount: 709,
          severePolicyViolationCount: 152,
          moderatePolicyViolationCount: 7,
          grandfatheredPolicyViolationCount: 0,
          totalComponentCount: 1157,
        },
      };
      renderComponent({ ...props, app: { ...props.app, policyEvaluationsResults: noViolationResults } });
    });

    it('Renders component without violations and "No violations" message', () => {
      expect(screen.queryByText('No violations')).toBeVisible();
    });
  });

  describe('when there are pending results', () => {
    beforeEach(() => {
      const pendingResultsProps = {
        stage: 'source',
        app: {
          id: '137a638c1a474963b464a81e50be9fe4',
          publicId: 'CDPAPPGO',
          name: 'Test app',
          organizationId: 'fce7dfaa30b047d585190ac632a07bfd',
          organizationName: 'CDP Go',
          policyEvaluations: {
            source: {
              id: '1c09f8e97e9247cfb37778462efbc205',
              applicationId: '137a638c1a474963b464a81e50be9fe4',
              stageTypeId: 'source',
              scanId: '4d8ad4f41b7d46a79d95e1359d40b861',
              time: 1636403882656,
              commitHash: null,
              initiator: 'admin',
              scanTriggerType: 'WEB_UI',
              forObsoleteScan: false,
              reevaluation: false,
              forMonitoring: false,
            },
          },
          policyEvaluationsResults: {},
          contact: {
            internalName: null,
            displayName: null,
            email: null,
            realm: null,
            error: null,
          },
          hasPendingSourceControlPolicyEvaluation: true,
        },
        hrefUiRouterState: () => {},
      };

      renderComponent({ ...pendingResultsProps });
    });

    it('Renders the component with the "pending" message', () => {
      expect(screen.getByText('pending')).toBeVisible();
    });
  });

  it('Renders the component', () => {
    renderComponent(props);

    const criticalPolicyViolationCount = screen.getByText('709');
    const severePolicyViolationCount = screen.getByText('152');
    const moderatePolicyViolationCount = screen.getByText('7');

    expect(criticalPolicyViolationCount).toBeVisible();
    expect(severePolicyViolationCount).toBeVisible();
    expect(moderatePolicyViolationCount).toBeVisible();

    const viewReport = screen.getByText('View Report');
    expect(viewReport).toBeVisible();
    fireEvent.click(viewReport);
    expect(hrefUiRouterStateSpy).toHaveBeenCalled();
  });
});
