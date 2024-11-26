/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen, fireEvent, within } from 'TestRoot/SpecUtil';
import PrioritiesPageHeader from 'MainRoot/development/prioritiesPage/PrioritiesPageHeader';
import moment from 'moment';
import * as routerStateContext from 'MainRoot/react/RouterStateContext';

const publicAppId = 'TestApp';
const scanId = 'testScanId';

describe('PrioritiesPageHeader', () => {
  let renderComponent;

  const metadata = {
    reportTime: 1702041439230,
    reportTitle: 'Build Report',
    application: {
      name: 'TestApp',
      nameLowercaseNoWhitespace: 'testapp',
      id: 'a03a6722af3f47fc8b7de86c78176de5',
      publicId: publicAppId,
      publicIdLowercase: 'testapp',
    },
    stageId: 'build',
    commitHash: null,
    initiator: 'admin',
    scanTriggerType: 'Continuous Integration',
    totalRisk: 138,
    forMonitoring: false,
    reevaluation: false,
  };

  const defaultPreloadedState = {
    applicationReport: {
      metadata,
      selectedReport: {
        criticalViolationCount: 133,
        severeViolationCount: 23,
        moderateViolationCount: 13,
        nonLowViolationCount: 83,
        policyComponentCount: 253,
        totalArtifactCount: 303,
        legacyViolationCount: 33,
        aggregatedEntries: [{ waivedViolations: 5 }, { waivedViolations: 10 }],
      },
      dependencyTree: [
        {
          children: null,
          isOpen: true,
          treePath: [2],
          originalTreePath: [2],
          hash: '20554954120b3cc9f088',
          policyThreatLevel: 10,
          displayName: 'somedisplayname',
          isInnerSource: false,
        },
      ],
    },
    router: {
      currentParams: {
        publicAppId,
        scanId,
      },
      currentState: {
        name: 'prioritiesPageFromDashboard',
      },
    },
  };

  beforeEach(() => {
    renderComponent = (preloadedState) =>
      render(<PrioritiesPageHeader />, { preloadedState: preloadedState || defaultPreloadedState });
  });

  it('renders a header with the app name', () => {
    renderComponent();
    expect(screen.getByRole('heading', { name: 'TestApp - Priorities' })).toBeInTheDocument();
  });

  describe('header actions', () => {
    beforeEach(() => {
      const routerContextMock = {
        href: jest.fn('href').mockImplementation((stateName) => stateName),
        get: jest.fn('get').mockImplementation((state) => state),
      };
      jest.spyOn(routerStateContext, 'useRouterState').mockReturnValue(routerContextMock);
    });

    describe('view dropdown', () => {
      it('renders a "View" dropdown with options', async () => {
        renderComponent();
        const viewDropdown = await screen.findByRole('button', { name: /view/i });
        expect(viewDropdown).toBeInTheDocument();

        fireEvent.click(viewDropdown);

        expect(screen.getByRole('link', { name: /lifecycle report/i })).toBeInTheDocument();
        expect(screen.getByRole('link', { name: /dependencies/i })).toBeInTheDocument();
      });

      it('renders a "View Full Report" link', async () => {
        renderComponent();
        const viewDropdown = await screen.findByRole('button', { name: /view/i });
        expect(viewDropdown).toBeInTheDocument();

        fireEvent.click(viewDropdown);

        const lifecycleReportLink = await screen.findByRole('link', { name: /lifecycle report/i });
        expect(lifecycleReportLink).toBeInTheDocument();
        expect(lifecycleReportLink).toHaveAttribute('href', 'applicationReport.policy');
        expect(lifecycleReportLink).toHaveAttribute('target', '_blank');
      });

      describe('dependencies link', () => {
        it('renders a "Dependencies" link when dependency tree is available', async () => {
          renderComponent();
          const viewDropdown = await screen.findByRole('button', { name: /view/i });
          expect(viewDropdown).toBeInTheDocument();

          fireEvent.click(viewDropdown);

          const dependenciesLink = await screen.findByRole('link', { name: /dependencies/i });
          expect(dependenciesLink).toBeInTheDocument();
          expect(dependenciesLink).not.toHaveAttribute('aria-disabled', 'true');

          expect(dependenciesLink).toHaveAttribute(
            'href',
            'componentDetailsPageWithinPrioritiesPageContainerFromDashboard.dependencyTree'
          );
        });

        it('renders a disabled "Dependencies" link when dependency tree is not available', async () => {
          const preloadedState = {
            applicationReport: {
              metadata,
              selectedReport: {
                criticalViolationCount: 133,
                severeViolationCount: 23,
                moderateViolationCount: 13,
                nonLowViolationCount: 83,
                policyComponentCount: 253,
                totalArtifactCount: 303,
                legacyViolationCount: 33,
                aggregatedEntries: [{ waivedViolations: 5 }, { waivedViolations: 10 }],
              },
              dependencyTree: [],
            },
            router: {
              currentParams: {
                publicAppId,
                scanId,
              },
              currentState: {
                name: 'prioritiesPageFromDashboard',
              },
            },
          };
          renderComponent(preloadedState);

          const viewDropdown = await screen.findByRole('button', { name: /view/i });
          expect(viewDropdown).toBeInTheDocument();

          fireEvent.click(viewDropdown);

          const dependenciesLink = await screen.findByRole('link', { name: /dependencies/i });
          expect(dependenciesLink).toBeInTheDocument();
          expect(dependenciesLink).toHaveAttribute('aria-disabled', 'true');
        });
      });
    });
  });

  describe('summary section', () => {
    it('renders section', () => {
      renderComponent();
      expect(screen.getByTestId('iq-priorities-page-summary-section')).toBeInTheDocument();
    });

    it('renders correct time and date', async () => {
      const reportTime = metadata.reportTime;
      const expectedFormattedDate = moment(reportTime).format('YYYY-MM-DD HH:mm:ss [UTC]ZZ');
      const expectedFormattedText = moment(reportTime).fromNow();
      renderComponent();
      const expectedText = screen.getByText(expectedFormattedText);
      expect(expectedText).toBeInTheDocument();

      fireEvent.mouseOver(expectedText);

      const tooltip = await screen.findByRole('tooltip');
      expect(tooltip).toHaveTextContent(expectedFormattedDate);
    });

    describe('violations section', () => {
      it('renders the correct number of violations', async () => {
        renderComponent();
        const summarySection = screen.getByTestId('iq-priorities-page-summary-section');

        const critical = within(summarySection).getByText('Critical');
        expect(critical).toBeInTheDocument();
        expect(within(summarySection).getByText('133')).toBeInTheDocument();

        const severe = within(summarySection).getByText('Severe');
        expect(severe).toBeInTheDocument();
        expect(within(summarySection).getByText('23')).toBeInTheDocument();

        const moderate = within(summarySection).getByText('Moderate');
        expect(moderate).toBeInTheDocument();
        expect(within(summarySection).getByText('13')).toBeInTheDocument();

        const legacy = within(summarySection).getByText('Legacy');
        expect(legacy).toBeInTheDocument();
        expect(within(summarySection).getByText('33')).toBeInTheDocument();

        const waived = within(summarySection).getByText('Waived');
        expect(waived).toBeInTheDocument();
        expect(within(summarySection).getByText('15')).toBeInTheDocument();
      });

      it('renders correct number of identified and total components', () => {
        renderComponent();
        const summarySection = screen.getByTestId('iq-priorities-page-summary-section');
        expect(within(summarySection).getByText('Affecting 253 of 303 identified components')).toBeInTheDocument();
      });
    });

    describe('triggered by section', () => {
      it('renders a "Triggered by" text based on scanTriggerType', () => {
        renderComponent();
        expect(screen.getByText('Triggered by:')).toBeInTheDocument();
        expect(screen.getByText(metadata.scanTriggerType)).toBeInTheDocument();
      });

      it('renders text (Continuous Monitoring) if forMonitoring is true', () => {
        const preloadedState = {
          applicationReport: {
            ...defaultPreloadedState.applicationReport,
            metadata: {
              ...metadata,
              forMonitoring: true,
            },
          },
        };

        renderComponent(preloadedState);
        expect(screen.getByText(/(continuous monitoring)/i)).toBeInTheDocument();
      });

      it('renders text (Re-evaluation) if reevaluation is true', () => {
        const preloadedState = {
          applicationReport: {
            ...defaultPreloadedState.applicationReport,
            metadata: {
              ...metadata,
              reevaluation: true,
            },
          },
        };

        renderComponent(preloadedState);
        expect(screen.getByText(/(re-evaluation)/i)).toBeInTheDocument();
      });
    });

    describe('commit hash section', () => {
      const originalNavigator = window.navigator;
      const writeText = jest.fn();

      Object.assign(navigator, {
        clipboard: {
          writeText,
        },
      });

      afterAll(() => {
        Object.assign(navigator, originalNavigator);
      });

      it('does not render if commit hash is null', () => {
        renderComponent();
        expect(screen.queryByText('Commit')).not.toBeInTheDocument();
      });

      it('renders if commit hash is present', () => {
        const commitHash = '473a9adb0824525dd69d375f067de0290deb2183';
        const expectedCommitHash = commitHash.substring(0, 7);

        const preloadedState = {
          applicationReport: {
            ...defaultPreloadedState.applicationReport,
            metadata: {
              ...defaultPreloadedState.applicationReport.metadata,
              commitHash,
            },
          },
        };

        renderComponent(preloadedState);
        expect(screen.getByText('Commit:')).toBeInTheDocument();
        expect(screen.getByText(expectedCommitHash)).toBeInTheDocument();
      });

      it('renders with a copy icon that copies the commit hash if commit hash is present', async () => {
        const commitHash = '473a9adb0824525dd69d375f067de0290deb2183';
        const preloadedState = {
          applicationReport: {
            ...defaultPreloadedState.applicationReport,
            metadata: {
              ...defaultPreloadedState.applicationReport.metadata,
              commitHash,
            },
          },
        };

        renderComponent(preloadedState);

        const copyIcon = screen.getAllByRole('img', { hidden: true })[1];
        fireEvent.click(copyIcon);
        expect(navigator.clipboard.writeText).toHaveBeenCalledWith(commitHash);
      });
    });

    describe('stage section', () => {
      it('does not render if stageId is null', () => {
        const preloadedState = {
          applicationReport: {
            ...defaultPreloadedState.applicationReport,
            metadata: {
              ...defaultPreloadedState.applicationReport.metadata,
              stageId: null,
            },
          },
        };

        renderComponent(preloadedState);
        expect(screen.queryByText('Stage:')).not.toBeInTheDocument();
      });

      it('renders if stageId is present', () => {
        renderComponent();
        expect(screen.getByText('Stage:')).toBeInTheDocument();
        expect(screen.getByText('Build')).toBeInTheDocument();
      });
    });
  });
});
