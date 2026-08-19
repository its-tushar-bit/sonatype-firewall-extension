/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, fireEvent, within } from 'TestRoot/SpecUtil';
import moment from 'moment';
import { STANDARD_DATE_FORMAT } from 'MainRoot/util/dateUtils';
import WaiverRow from 'MainRoot/waivers/WaiverRow';

describe('WaiverRow', () => {
  const mockViolationDetails = {
    constraintViolations: [],
    displayName: { parts: [] },
    filename: 'test-file.js',
    policyViolationId: '12345',
  };
  const expiryTime = moment().add(1, 'day').toDate();

  const mockWaiver = {
    createTime: new Date(),
    expireWhenRemediationAvailable: false,
    scopeOwnerType: 'root_organization',
    matcherStrategy: 'ALL_COMPONENTS',
    reasonText: null,
    policyWaiverId: null,
  };

  const mockContainerImageWaiver = {
    createTime: new Date(),
    expireWhenRemediationAvailable: false,
    creatorName: 'admin',
    scopeOwnerType: 'application',
    scopeOwnerName: 'docker-proxy-library-alpine-3.6',
    forContainerImageComponent: false,
    matcherStrategy: 'ALL_COMPONENTS',
    reasonText: null,
    policyWaiverId: 'b1b59985e2',
  };

  const mockDeleteWaiver = jest.fn();
  const defaultProps = {
    violationDetails: mockViolationDetails,
    waiver: mockWaiver,
  };

  let renderComponent;

  const defaultPreloadedState = {
    orgsAndPolicies: {
      autoWaivers: {
        autoWaiverModal: {},
        autoWaiverExclusions: {
          submitMaskState: false,
        },
      },
    },
  };

  beforeEach(() => {
    renderComponent = (props = {}) =>
      render(<WaiverRow {...defaultProps} {...props} />, { preloadedState: defaultPreloadedState });
  });

  describe('Non auto waiver', () => {
    it('renders WaiverRow without expiry time, author, reason, comment, component version, delete waiver button and conditions', () => {
      renderComponent();
      const cells = screen.getAllByRole('cell');
      const durationCell = cells[0];
      const detailsCell = cells[1];
      expect(within(durationCell).getByText(moment(mockWaiver.createTime).format(STANDARD_DATE_FORMAT))).toBeVisible();
      expect(within(durationCell).getByText('Does not expire')).toBeVisible();
      expect(within(detailsCell).getByText('Root Organization')).toBeVisible();
      // Author and reason are not present so a dash is displayed
      expect(within(detailsCell).getAllByText('—').length).toBe(2);
      expect(within(detailsCell).queryByText('Comment')).not.toBeInTheDocument();
      expect(within(detailsCell).queryByText('Conditions')).not.toBeInTheDocument();
      expect(within(detailsCell).getByText('All')).toBeVisible();
    });

    it('renders WaiverRow with expiry time, author, reason, comment, component version, delete waiver button and conditions', () => {
      renderComponent({
        deleteWaiver: mockDeleteWaiver,
        reasons: ['reason 1', 'reason 2'],
        waiver: {
          ...mockWaiver,
          expiryTime,
          creatorName: 'Creator Name',
          reasonText: 'test reason',
          scopeOwnerType: 'organization',
          scopeOwnerName: 'some org',
          matcherStrategy: 'EXACT_COMPONENT',
          comment: 'test comment',
        },
      });
      const cells = screen.getAllByRole('cell');
      const durationCell = cells[0];
      const detailsCell = cells[1];
      const deleteCell = cells[2];
      expect(within(durationCell).getByText(moment(mockWaiver.createTime).format(STANDARD_DATE_FORMAT))).toBeVisible();
      expect(within(durationCell).getByText(moment(expiryTime).format(STANDARD_DATE_FORMAT))).toBeVisible();
      expect(within(detailsCell).getByText('Organization - some org')).toBeVisible();
      expect(within(detailsCell).getByText('test-file.js')).toBeVisible();
      expect(within(detailsCell).getByText('Creator Name')).toBeVisible();
      expect(within(detailsCell).getByText('test reason')).toBeVisible();
      expect(within(detailsCell).getByText('test comment')).toBeVisible();
      expect(within(detailsCell).getByText('reason 1')).toBeVisible();
      expect(within(detailsCell).getByText('reason 2')).toBeVisible();
      expect(within(deleteCell).getByRole('button')).toBeVisible();
    });

    it('renders WaiverRow expiration When Remediation Available', () => {
      renderComponent({ waiver: { ...mockWaiver, expireWhenRemediationAvailable: true } });
      const cells = screen.getAllByRole('cell');
      const durationCell = cells[0];
      expect(within(durationCell).getByText('When Remediation Available')).toBeVisible();
    });

    it('renders WaiverRow with upgrade available', () => {
      renderComponent({
        waiver: { ...mockWaiver, expireWhenRemediationAvailable: true, expiryTime },
      });
      const cells = screen.getAllByRole('cell');
      const durationCell = cells[0];
      expect(within(durationCell).getByText('Upgrade Available')).toBeVisible();
      expect(within(durationCell).queryByText(moment(expiryTime).format(STANDARD_DATE_FORMAT))).not.toBeInTheDocument();
    });

    it('renders WaiverRow without upgrade available when is similar waiver', () => {
      renderComponent({
        isSimilarWaiver: true,
        waiver: { ...mockWaiver, expireWhenRemediationAvailable: true, expiryTime },
      });
      const cells = screen.getAllByRole('cell');
      const durationCell = cells[0];
      expect(within(durationCell).queryByText('Upgrade Available')).not.toBeInTheDocument();
      expect(within(durationCell).getByText(moment(expiryTime).format(STANDARD_DATE_FORMAT))).toBeVisible();
    });

    it('calls deleteWaiver on delete button click', () => {
      renderComponent({ deleteWaiver: mockDeleteWaiver });
      const cells = screen.getAllByRole('cell');
      const deleteCell = cells[2];
      const deletButton = within(deleteCell).getByRole('button');
      fireEvent.click(deletButton);
      expect(mockDeleteWaiver).toHaveBeenCalledWith(mockWaiver);
    });

    it('renders WaiverRow with expired state', () => {
      renderComponent({
        isWaiverExpired: true,
      });
      const row = screen.getByRole('row');
      expect(row).toHaveClass('list-waivers-row--expired');
    });
  });

  describe('Auto waiver', () => {
    it('renders WaiverRow', () => {
      renderComponent({
        isAutoWaiver: true,
        waiver: { ...mockWaiver, ownerType: 'application', ownerName: 'auto owner' },
      });
      const cells = screen.getAllByRole('cell');
      const durationCell = cells[0];
      const detailsCell = cells[1];
      expect(within(durationCell).getByText(moment(mockWaiver.createTime).format(STANDARD_DATE_FORMAT))).toBeVisible();
      expect(within(durationCell).getByText('Auto')).toBeVisible();
      expect(within(detailsCell).getByText('Application - auto owner')).toBeVisible();
      expect(within(detailsCell).getByText('Any Component')).toBeVisible();
      expect(within(detailsCell).getByText('Current or latest non-violating')).toBeVisible();
      // Author is not present
      expect(within(detailsCell).getByText('—')).toBeVisible();
    });

    it('renders WaiverRow with author', () => {
      renderComponent({
        isAutoWaiver: true,
        waiver: { ...mockWaiver, ownerType: 'organization', ownerName: 'auto owner', creatorName: 'Creator Name' },
      });
      const cells = screen.getAllByRole('cell');
      const durationCell = cells[0];
      const detailsCell = cells[1];
      expect(within(durationCell).getByText(moment(mockWaiver.createTime).format(STANDARD_DATE_FORMAT))).toBeVisible();
      expect(within(durationCell).getByText('Auto')).toBeVisible();
      expect(within(detailsCell).getByText('Organization - auto owner')).toBeVisible();
      expect(within(detailsCell).getByText('Any Component')).toBeVisible();
      expect(within(detailsCell).getByText('Current or latest non-violating')).toBeVisible();
      // Author is not present
      expect(within(detailsCell).getByText('Creator Name')).toBeVisible();
    });

    it('opens and closes Create Auto-Waiver Exclusion Modal', () => {
      renderComponent({
        isAutoWaiver: true,
        waiver: { ...mockWaiver, ownerType: 'application', ownerName: 'auto owner' },
      });

      const deleteButton = screen.getByRole('button');
      expect(deleteButton).toBeInTheDocument();

      fireEvent.click(deleteButton);
      expect(screen.getByText('Remove Automated Waiver')).toBeInTheDocument();

      fireEvent.click(screen.getByText('Cancel'));
      expect(screen.queryByText('Remove Automated Waiver')).not.toBeInTheDocument();
    });
  });

  describe('Container Image Waiver', () => {
    it('renders WaiverRow', () => {
      renderComponent({
        waiver: { ...mockContainerImageWaiver },
        deleteWaiver: mockDeleteWaiver,
      });
      const cells = screen.getAllByRole('cell');
      const durationCell = cells[0];
      const detailsCell = cells[1];
      const deleteCell = cells[2];
      expect(within(durationCell).getByText(moment(mockWaiver.createTime).format(STANDARD_DATE_FORMAT))).toBeVisible();
      expect(within(durationCell).getByText('Does not expire')).toBeVisible();
      expect(within(detailsCell).getByText('Application - docker-proxy-library-alpine-3.6')).toBeVisible();
      expect(within(detailsCell).getByText('All')).toBeVisible();
      expect(within(detailsCell).getByText('admin')).toBeVisible();
      expect(within(deleteCell).getByRole('button')).toBeVisible();
    });

    it('calls deleteWaiver on delete button click', () => {
      renderComponent({
        waiver: { ...mockContainerImageWaiver },
        deleteWaiver: mockDeleteWaiver,
      });
      const cells = screen.getAllByRole('cell');
      const deleteCell = cells[2];
      const deletButton = within(deleteCell).getByRole('button');
      fireEvent.click(deletButton);
      expect(mockDeleteWaiver).toHaveBeenCalledWith(mockContainerImageWaiver);
    });
  });
});
