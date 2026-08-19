/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { render, screen } from 'TestRoot/SpecUtil';
import React from 'react';
import PolicyViolationsTableRow from 'MainRoot/componentDetails/ViolationsTableTile/PolicyViolationsTableRow';

describe('PolicyViolationsTableRow', () => {
  const defaultState = {
    violation: {
      policyViolationId: '1',
      policyThreatLevel: 1,
      policyName: 'Test Policy',
      constraints: [],
    },
    setSelectedPolicyViolationId: jest.fn(),
  };

  const defaultNumberOfClasses = 3;

  const renderComponent = (state) => {
    render(<PolicyViolationsTableRow {...state} />);
  };

  it('should render all columns', () => {
    renderComponent(defaultState);

    expect(screen.getByText(/1/i)).toBeInTheDocument();
    expect(screen.getByText(/test policy/i)).toBeInTheDocument();
    expect(screen.queryByText(/not reachable/i)).not.toBeInTheDocument();
    expect(screen.getByText('-')).toBeInTheDocument();
    expect(screen.getByText(/open/i)).toBeInTheDocument();
  });

  it('should render all columns except reachability', () => {
    const state = {
      ...defaultState,
      violation: {
        ...defaultState.violation,
        reachabilityStatus: 'NON_REACHABLE',
      },
      isLegalTab: true,
    };

    renderComponent(state);

    expect(screen.getByRole('row').className.split(' ')).toHaveLength(defaultNumberOfClasses);
    expect(screen.getByText(/1/i)).toBeInTheDocument();
    expect(screen.getByText(/test policy/i)).toBeInTheDocument();
    expect(screen.getByText(/not reachable/i)).toBeInTheDocument();
    expect(screen.queryByText('-')).not.toBeInTheDocument();
    expect(screen.getByText(/open/i)).toBeInTheDocument();
  });

  it('should render - in the reachability column', () => {
    renderComponent(defaultState);

    expect(screen.getByText('-')).toBeInTheDocument();
  });

  it('should render Not Reachable in the reachability column', () => {
    const newState = {
      ...defaultState,
      violation: {
        ...defaultState.violation,
        reachabilityStatus: 'NON_REACHABLE',
      },
    };

    renderComponent(newState);

    expect(screen.getByText('Not Reachable')).toBeInTheDocument();
  });

  it('should render Reachable in the reachability column', () => {
    const newState = {
      ...defaultState,
      violation: {
        ...defaultState.violation,
        reachabilityStatus: 'REACHABLE',
      },
    };

    renderComponent(newState);

    expect(screen.getByText('Reachable')).toBeInTheDocument();
  });

  describe('Waiver status', () => {
    it('should render Auto as the waiver status', () => {
      const state = {
        ...defaultState,
        violation: {
          ...defaultState.violation,
          waivedWithAutoWaiver: true,
        },
        isAutoWaiversEnabled: true,
      };

      renderComponent(state);

      expect(screen.getByRole('row').className.split(' ')).toHaveLength(defaultNumberOfClasses + 1);
      expect(screen.getByRole('row').className).toContain('iq-policy-violation-row--auto');
      expect(screen.getByText('Auto')).toBeInTheDocument();
    });

    it('should render Legacy as the waiver status', () => {
      const state = {
        ...defaultState,
        violation: {
          ...defaultState.violation,
          legacyViolation: true,
        },
      };

      renderComponent(state);

      expect(screen.getByRole('row').className.split(' ')).toHaveLength(defaultNumberOfClasses + 1);
      expect(screen.getByRole('row').className).toContain('iq-policy-violation-row--legacy');
      expect(screen.getByText('Legacy')).toBeInTheDocument();
    });

    describe('Renders Open', () => {
      it('Should not render small text - No expired waivers', () => {
        const state = {
          ...defaultState,
          violation: {
            ...defaultState.violation,
            expiredWaivers: [],
          },
        };

        renderComponent(state);

        expect(screen.getByRole('row').className.split(' ')).toHaveLength(defaultNumberOfClasses);
        expect(screen.getByText('Open')).toBeInTheDocument();
        expect(screen.queryByText('Waiver expired')).not.toBeInTheDocument();
      });

      it('Should not render small text - Expired waiver out of range', () => {
        const date = new Date();
        date.setDate(date.getDate() - 10); // Subtract 10 days

        const state = {
          ...defaultState,
          violation: {
            ...defaultState.violation,
            expiredWaivers: [
              {
                expiryTime: date.getTime(),
              },
            ],
          },
        };

        renderComponent(state);

        expect(screen.getByRole('row').className.split(' ')).toHaveLength(defaultNumberOfClasses);
        expect(screen.getByText('Open')).toBeInTheDocument();
        expect(screen.queryByText('Waiver expired')).not.toBeInTheDocument();
      });

      it('Should render small text - Expired waiver in lower range', () => {
        const date = new Date();
        date.setDate(date.getDate() - 9); // Subtract 9 days

        const state = {
          ...defaultState,
          violation: {
            ...defaultState.violation,
            expiredWaivers: [
              {
                expiryTime: date.getTime(),
              },
            ],
          },
        };

        renderComponent(state);

        expect(screen.getByRole('row').className.split(' ')).toHaveLength(defaultNumberOfClasses + 1);
        expect(screen.getByRole('row').className).toContain('iq-policy-violation-row--expired');
        expect(screen.getByText('Open')).toBeInTheDocument();
        expect(screen.queryByText('Waiver expired')).toBeInTheDocument();
      });

      it('Should render small text - Expired waiver in upper range', () => {
        const date = new Date();

        const state = {
          ...defaultState,
          violation: {
            ...defaultState.violation,
            expiredWaivers: [
              {
                expiryTime: date.getTime(),
              },
            ],
          },
        };

        renderComponent(state);

        expect(screen.getByRole('row').className.split(' ')).toHaveLength(defaultNumberOfClasses + 1);
        expect(screen.getByRole('row').className).toContain('iq-policy-violation-row--expired');
        expect(screen.getByText('Open')).toBeInTheDocument();
        expect(screen.queryByText('Waiver expired')).toBeInTheDocument();
      });

      it('Should render small text - Multiple expired waivers but one in range', () => {
        const date = new Date();
        date.setDate(date.getDate() - 10); // Subtract 10 days
        const date1 = new Date();
        date1.setDate(date1.getDate() - 1); // Subtract 1 day

        const state = {
          ...defaultState,
          violation: {
            ...defaultState.violation,
            expiredWaivers: [
              {
                expiryTime: date.getTime(),
              },
              {
                expiryTime: date1.getTime(),
              },
            ],
            waived: true,
          },
        };

        renderComponent(state);

        expect(screen.getByRole('row').className.split(' ')).toHaveLength(defaultNumberOfClasses + 1);
        expect(screen.getByRole('row').className).toContain('iq-policy-violation-row--expired');
        expect(screen.getByText('Open')).toBeInTheDocument();
        expect(screen.queryByText('Waiver expired')).toBeInTheDocument();
      });
    });

    describe('Renders Waived', () => {
      // is being flaky - skip for now
      it.skip('Should not render small text - No expiring waivers', () => {
        const state = {
          ...defaultState,
          violation: {
            ...defaultState.violation,
            applicableWaivers: ['1'],
            waived: true,
          },
        };

        renderComponent(state);

        expect(screen.getByRole('row').className.split(' ')).toHaveLength(defaultNumberOfClasses + 1);
        expect(screen.getByRole('row').className).toContain('iq-policy-violation-row--remediated');
        expect(screen.getByText('Waived')).toBeInTheDocument();
        expect(screen.queryByText('Expires in 1 day')).not.toBeInTheDocument();
        expect(screen.queryByText(/Expires in [2-9] days/)).not.toBeInTheDocument();
      });

      it("Should render small text - Multiple expiring waivers is today's range", () => {
        // Use day-based offsets to avoid flakiness when tests run near midnight
        const date = new Date();
        date.setHours(23, 59, 59); // End of today - expires today (0 days)
        const date1 = new Date();
        date1.setDate(date1.getDate() + 1); // Tomorrow - expires in 1 day
        date1.setHours(12, 0, 0); // Midday tomorrow to ensure it's solidly in the next day

        const state = {
          ...defaultState,
          violation: {
            ...defaultState.violation,
            applicableWaivers: ['1', '2'],
            waived: true,
          },
          waivers: [
            {
              id: '1',
              expiryTime: date.getTime(),
            },
            {
              id: '2',
              expiryTime: date1.getTime(),
            },
          ],
        };

        renderComponent(state);

        expect(screen.getByRole('row').className.split(' ')).toHaveLength(defaultNumberOfClasses + 1);
        expect(screen.getByRole('row').className).toContain('iq-policy-violation-row--expiring');
        expect(screen.getByText('Waived')).toBeInTheDocument();
        expect(screen.getByText('Expires in 1 day')).toBeInTheDocument();
        expect(screen.queryByText(/Expires in [2-9] days/)).not.toBeInTheDocument();
      });

      it('Should not render small text - Furthest expiring waiver outside range', () => {
        const date = new Date();
        date.setHours(date.getHours() + 1); // Add 1 hour
        const date1 = new Date();
        date1.setHours(date1.getHours() + 23); // Add 23 hour
        const date2 = new Date();
        date2.setDate(date2.getDate() + 2); // Add 2 days
        const date3 = new Date();
        date3.setDate(date3.getDate() + 9); // Add 9 days
        const date4 = new Date();
        date4.setDate(date4.getDate() + 11); // Add 11 days

        const state = {
          ...defaultState,
          violation: {
            ...defaultState.violation,
            applicableWaivers: ['1', '2', '3', '4', '5'],
            waived: true,
          },
          waivers: [
            {
              id: '1',
              expiryTime: date.getTime(),
            },
            {
              id: '2',
              expiryTime: date1.getTime(),
            },
            {
              id: '3',
              expiryTime: date2.getTime(),
            },
            {
              id: '4',
              expiryTime: date3.getTime(),
            },
            {
              id: '5',
              expiryTime: date4.getTime(),
            },
          ],
        };

        renderComponent(state);

        expect(screen.getByRole('row').className.split(' ')).toHaveLength(defaultNumberOfClasses + 1);
        expect(screen.getByRole('row').className).toContain('iq-policy-violation-row--remediated');
        expect(screen.getByText('Waived')).toBeInTheDocument();
        expect(screen.queryByText('Expires in 1 day')).not.toBeInTheDocument();
        expect(screen.queryByText(/Expires in [2-9] days/)).not.toBeInTheDocument();
      });

      it('Should render small text - Expiring lower range waiver', () => {
        const date = new Date();
        date.setDate(date.getDate() + 1);

        const state = {
          ...defaultState,
          violation: {
            ...defaultState.violation,
            applicableWaivers: ['1'],
            waived: true,
          },
          waivers: [
            {
              id: '1',
              expiryTime: date.getTime(),
            },
          ],
        };

        renderComponent(state);

        expect(screen.getByRole('row').className.split(' ')).toHaveLength(defaultNumberOfClasses + 1);
        expect(screen.getByRole('row').className).toContain('iq-policy-violation-row--expiring');
        expect(screen.getByText('Waived')).toBeInTheDocument();
        expect(screen.queryByText('Expires in 1 day')).toBeInTheDocument();
      });

      it('Should render small text - Expiring close to lower range waiver', () => {
        const date = new Date();
        date.setDate(date.getDate() + 2);

        const state = {
          ...defaultState,
          violation: {
            ...defaultState.violation,
            applicableWaivers: ['1'],
            waived: true,
          },
          waivers: [
            {
              id: '1',
              expiryTime: date.getTime(),
            },
          ],
        };

        renderComponent(state);

        expect(screen.getByRole('row').className.split(' ')).toHaveLength(defaultNumberOfClasses + 1);
        expect(screen.getByRole('row').className).toContain('iq-policy-violation-row--expiring');
        expect(screen.getByText('Waived')).toBeInTheDocument();
        expect(screen.queryByText('Expires in 1 day')).not.toBeInTheDocument();
        expect(screen.queryByText(/Expires in 2 days/)).toBeInTheDocument();
      });

      it('Should render small text - Expiring upper range waiver', () => {
        const date = new Date();
        date.setDate(date.getDate() + 9);

        const state = {
          ...defaultState,
          violation: {
            ...defaultState.violation,
            applicableWaivers: ['1'],
            waived: true,
          },
          waivers: [
            {
              id: '1',
              expiryTime: date.getTime(),
            },
          ],
        };

        renderComponent(state);

        expect(screen.getByRole('row').className.split(' ')).toHaveLength(defaultNumberOfClasses + 1);
        expect(screen.getByRole('row').className).toContain('iq-policy-violation-row--expiring');
        expect(screen.getByText('Waived')).toBeInTheDocument();
        expect(screen.queryByText('Expires in 1 day')).not.toBeInTheDocument();
        expect(screen.queryByText(/Expires in 9 days/)).toBeInTheDocument();
      });

      it('Should not render small text - Multiple expiring waivers out of range', () => {
        const date = new Date();
        date.setDate(date.getDate() + 10); // Add 10 days
        const date1 = new Date();
        date1.setDate(date1.getDate() + 20); // Add 20 day

        const state = {
          ...defaultState,
          violation: {
            ...defaultState.violation,
            expiredWaivers: [
              {
                expiryTime: date.getTime(),
              },
              {
                expiryTime: date1.getTime(),
              },
            ],
            applicableWaivers: ['1', '2'],
            waived: true,
          },
          waivers: [
            {
              id: '1',
              expiryTime: date.getTime(),
            },
            {
              id: '2',
              expiryTime: date1.getTime(),
            },
          ],
        };

        renderComponent(state);

        expect(screen.getByRole('row').className.split(' ')).toHaveLength(defaultNumberOfClasses + 1);
        expect(screen.getByRole('row').className).toContain('iq-policy-violation-row--remediated');
        expect(screen.getByText('Waived')).toBeInTheDocument();
        expect(screen.queryByText('Expires in 1 day')).not.toBeInTheDocument();
        expect(screen.queryByText(/Expires in [2-9] days/)).not.toBeInTheDocument();
      });
    });

    it('Should render Unapplied Waiver', () => {
      const state = {
        ...defaultState,
        violation: {
          ...defaultState.violation,
          applicableWaivers: ['1'],
          waived: false,
        },
      };

      renderComponent(state);

      expect(screen.getByRole('row').className.split(' ')).toHaveLength(defaultNumberOfClasses);
      expect(screen.getByText('Unapplied Waiver')).toBeInTheDocument();
    });
  });
});
