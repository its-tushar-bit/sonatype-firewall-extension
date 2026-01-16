/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { render } from 'TestRoot/SpecUtil';
import FirewallUnquarantineTable from 'MainRoot/firewall/autounquarantine/FirewallUnquarantineTable';
import { formatDate, STANDARD_DATE_FORMAT } from 'MainRoot/util/dateUtils';

describe('FirewallUnquarantineTable', () => {
  const mockQuarantineDate = new Date('2025-01-01T10:00:00Z').getTime();
  const mockDateCleared = new Date('2025-01-08T14:30:00Z').getTime();

  const baseProps = {
    loadReleaseQuarantineList: jest.fn(),
    setAutoUnquarantineGridPage: jest.fn(),
    setAutoUnquarantineGridSorting: jest.fn(),
    selectReleaseQuarantineComponent: jest.fn(),
    goToRepositoryComponentDetailsPage: jest.fn(),
    loadedReleaseQuarantineList: true,
    loadAutoUnquarantineGridError: null,
    releaseQuarantinePageCount: 0,
    releaseQuarantineList: [],
    pageSize: 10,
    sortDir: null,
    sortField: null,
  };

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('rendering', () => {
    it('renders table with correct headers', () => {
      render(<FirewallUnquarantineTable {...baseProps} />);

      const table = screen.getByRole('table');
      const headerCells = within(table).getAllByRole('columnheader');

      expect(headerCells).toHaveLength(5);
      expect(headerCells[0]).toHaveTextContent('Component');
      expect(headerCells[1]).toHaveTextContent('Quarantine Date');
      expect(headerCells[2]).toHaveTextContent('Repository');
      expect(headerCells[3]).toHaveTextContent('Date Cleared');
      expect(headerCells[4]).toHaveTextContent('Select Row');
    });

    it('renders empty message when no data found', () => {
      render(<FirewallUnquarantineTable {...baseProps} />);

      const table = screen.getByRole('table');
      const rowGroups = within(table).getAllByRole('rowgroup');
      const bodyRows = within(rowGroups[1]).getAllByRole('row');
      const bodyCells = within(bodyRows[0]).getAllByRole('cell');

      expect(bodyRows).toHaveLength(1);
      expect(bodyCells[0]).toHaveTextContent('No data found.');
    });

    it('renders loading state when data not loaded', () => {
      render(<FirewallUnquarantineTable {...baseProps} loadedReleaseQuarantineList={false} />);

      const table = screen.getByRole('table');
      const rowGroups = within(table).getAllByRole('rowgroup');
      const bodyRows = within(rowGroups[1]).getAllByRole('row');

      expect(bodyRows[0]).toHaveClass('nx-table-row');
    });

    it('renders error message when loadAutoUnquarantineGridError is provided', () => {
      const errorMessage = 'Failed to load quarantine data';
      render(<FirewallUnquarantineTable {...baseProps} loadAutoUnquarantineGridError={errorMessage} />);

      const table = screen.getByRole('table');
      const rowGroups = within(table).getAllByRole('rowgroup');
      const bodyRows = within(rowGroups[1]).getAllByRole('row');
      const bodyCells = within(bodyRows[0]).getAllByRole('cell');
      const errorAlert = within(bodyCells[0]).getByRole('alert');

      expect(errorAlert).toHaveTextContent(`An error occurred loading data. ${errorMessage}`);
    });
  });

  describe('data rendering', () => {
    it('renders table with single row of data', () => {
      const mockData = [
        {
          componentDisplayText: 'com.example:lib:1.0.0',
          repository: 'maven-central',
          repositoryId: 'repo-1',
          componentIdentifier: 'com.example:lib:1.0.0',
          hash: 'abc123def456',
          matchState: 'QUARANTINE',
          pathname: '/some/path',
          quarantineDate: mockQuarantineDate,
          dateCleared: mockDateCleared,
        },
      ];

      render(<FirewallUnquarantineTable {...baseProps} releaseQuarantineList={mockData} />);

      const table = screen.getByRole('table');
      const rowGroups = within(table).getAllByRole('rowgroup');
      const bodyRows = within(rowGroups[1]).getAllByRole('row');
      const bodyCells = within(bodyRows[0]).getAllByRole('cell');

      expect(bodyRows).toHaveLength(1);
      expect(bodyCells[0]).toHaveTextContent('com.example:lib:1.0.0');
      expect(bodyCells[1]).toHaveTextContent(formatDate(mockQuarantineDate, STANDARD_DATE_FORMAT));
      expect(bodyCells[2]).toHaveTextContent('maven-central');
      expect(bodyCells[3]).toHaveTextContent(formatDate(mockDateCleared, STANDARD_DATE_FORMAT));
    });

    it('renders table with multiple rows of data', () => {
      const mockData = [
        {
          componentDisplayText: 'com.example:lib1:1.0.0',
          repository: 'maven-central',
          repositoryId: 'repo-1',
          componentIdentifier: 'com.example:lib1:1.0.0',
          hash: 'hash1',
          matchState: 'QUARANTINE',
          pathname: '/path1',
          quarantineDate: mockQuarantineDate,
          dateCleared: mockDateCleared,
        },
        {
          componentDisplayText: 'com.example:lib2:2.0.0',
          repository: 'npm-registry',
          repositoryId: 'repo-2',
          componentIdentifier: 'com.example:lib2:2.0.0',
          hash: 'hash2',
          matchState: 'QUARANTINE',
          pathname: '/path2',
          quarantineDate: mockQuarantineDate,
          dateCleared: mockDateCleared,
        },
      ];

      render(<FirewallUnquarantineTable {...baseProps} releaseQuarantineList={mockData} />);

      const table = screen.getByRole('table');
      const rowGroups = within(table).getAllByRole('rowgroup');
      const bodyRows = within(rowGroups[1]).getAllByRole('row');

      expect(bodyRows).toHaveLength(2);
      expect(bodyRows[0]).toHaveTextContent('com.example:lib1:1.0.0');
      expect(bodyRows[1]).toHaveTextContent('com.example:lib2:2.0.0');
    });

    it('truncates long component names with overflow tooltip', () => {
      const longComponentName = 'com.example.very.long.package.name:very-long-library-name:1.0.0-SNAPSHOT';
      const mockData = [
        {
          componentDisplayText: longComponentName,
          repository: 'maven-central',
          repositoryId: 'repo-1',
          componentIdentifier: longComponentName,
          hash: 'abc123',
          matchState: 'QUARANTINE',
          pathname: '/path',
          quarantineDate: mockQuarantineDate,
          dateCleared: mockDateCleared,
        },
      ];

      render(<FirewallUnquarantineTable {...baseProps} releaseQuarantineList={mockData} />);

      const componentCell = screen.getByText(longComponentName);
      expect(componentCell).toBeInTheDocument();
    });

    it('truncates long repository names with overflow tooltip', () => {
      const longRepositoryName = 'very-long-repository-name-for-testing-truncation';
      const mockData = [
        {
          componentDisplayText: 'com.example:lib:1.0.0',
          repository: longRepositoryName,
          repositoryId: 'repo-1',
          componentIdentifier: 'com.example:lib:1.0.0',
          hash: 'abc123',
          matchState: 'QUARANTINE',
          pathname: '/path',
          quarantineDate: mockQuarantineDate,
          dateCleared: mockDateCleared,
        },
      ];

      render(<FirewallUnquarantineTable {...baseProps} releaseQuarantineList={mockData} />);

      const repositoryCell = screen.getByText(longRepositoryName);
      expect(repositoryCell).toBeInTheDocument();
    });
  });

  describe('sorting', () => {
    it('calls setAutoUnquarantineGridSorting when quarantine date header is clicked', async () => {
      render(<FirewallUnquarantineTable {...baseProps} />);

      const quarantineDateHeader = screen.getByText('Quarantine Date');
      await userEvent.click(quarantineDateHeader);

      expect(baseProps.setAutoUnquarantineGridSorting).toHaveBeenCalledWith('asc', 'quarantineTime');
      expect(baseProps.loadReleaseQuarantineList).toHaveBeenCalled();
    });

    it('calls setAutoUnquarantineGridSorting when date cleared header is clicked', async () => {
      render(<FirewallUnquarantineTable {...baseProps} />);

      const dateClaredHeader = screen.getByText('Date Cleared');
      await userEvent.click(dateClaredHeader);

      expect(baseProps.setAutoUnquarantineGridSorting).toHaveBeenCalledWith('asc', 'releaseQuarantineTime');
      expect(baseProps.loadReleaseQuarantineList).toHaveBeenCalled();
    });

    it('cycles through sort directions: null -> asc -> desc -> null', async () => {
      const { rerender } = render(<FirewallUnquarantineTable {...baseProps} />);

      // First click: null -> asc
      const header = screen.getByText('Quarantine Date');
      await userEvent.click(header);
      expect(baseProps.setAutoUnquarantineGridSorting).toHaveBeenCalledWith('asc', 'quarantineTime');

      // Second click: asc -> desc
      baseProps.setAutoUnquarantineGridSorting.mockClear();
      baseProps.loadReleaseQuarantineList.mockClear();

      rerender(<FirewallUnquarantineTable {...baseProps} sortField="quarantineTime" sortDir="asc" />);

      await userEvent.click(header);
      expect(baseProps.setAutoUnquarantineGridSorting).toHaveBeenCalledWith('desc', 'quarantineTime');

      // Third click: desc -> null
      baseProps.setAutoUnquarantineGridSorting.mockClear();
      baseProps.loadReleaseQuarantineList.mockClear();

      rerender(<FirewallUnquarantineTable {...baseProps} sortField="quarantineTime" sortDir="desc" />);

      await userEvent.click(header);
      expect(baseProps.setAutoUnquarantineGridSorting).toHaveBeenCalledWith(null, null);
    });

    it('shows sort direction indicator on sorted column', () => {
      render(<FirewallUnquarantineTable {...baseProps} sortField="quarantineTime" sortDir="asc" />);

      const quarantineDateHeader = screen.getByText('Quarantine Date');
      expect(quarantineDateHeader).toBeInTheDocument();
    });

    it('clears sort direction on unsorted column', () => {
      render(<FirewallUnquarantineTable {...baseProps} sortField="quarantineTime" sortDir="asc" />);

      const dateClaredHeader = screen.getByText('Date Cleared');
      expect(dateClaredHeader).toBeInTheDocument();
    });
  });

  describe('pagination', () => {
    it('renders pagination controls when page count is greater than 0', () => {
      render(<FirewallUnquarantineTable {...baseProps} releaseQuarantinePageCount={3} currentPage={0} />);

      const pagination = screen.getByRole('navigation');
      expect(pagination).toBeInTheDocument();
    });

    it('calls setAutoUnquarantineGridPage and loadReleaseQuarantineList when page changes', async () => {
      const mockData = [
        {
          componentDisplayText: 'com.example:lib:1.0.0',
          repository: 'maven-central',
          repositoryId: 'repo-1',
          componentIdentifier: 'com.example:lib:1.0.0',
          hash: 'abc123',
          matchState: 'QUARANTINE',
          pathname: '/path',
          quarantineDate: mockQuarantineDate,
          dateCleared: mockDateCleared,
        },
      ];

      render(
        <FirewallUnquarantineTable
          {...baseProps}
          releaseQuarantineList={mockData}
          releaseQuarantinePageCount={2}
          currentPage={0}
        />
      );

      const pagination = screen.getByRole('navigation');
      const pageButtons = within(pagination).getAllByRole('button');

      // Click on page 2 (index 2 since first button is "previous")
      await userEvent.click(pageButtons[2]);

      expect(baseProps.setAutoUnquarantineGridPage).toHaveBeenCalledWith(1);
      expect(baseProps.loadReleaseQuarantineList).toHaveBeenCalled();
    });

    it('disables previous button on first page', () => {
      render(<FirewallUnquarantineTable {...baseProps} releaseQuarantinePageCount={2} currentPage={0} />);

      const pagination = screen.getByRole('navigation');
      const buttons = within(pagination).getAllByRole('button');
      const previousButton = buttons[0];

      expect(previousButton).toHaveAttribute('aria-label', 'goto first page');
      expect(previousButton).toHaveAttribute('aria-disabled', 'true');
    });

    it('shows correct current page indicator', () => {
      render(<FirewallUnquarantineTable {...baseProps} releaseQuarantinePageCount={3} currentPage={1} />);

      const pagination = screen.getByRole('navigation');
      const pageButtons = within(pagination).getAllByRole('button');

      // Current page button should have aria-current
      expect(pageButtons[2]).toHaveAttribute('aria-current', 'page');
    });
  });

  describe('navigation', () => {
    it('calls goToRepositoryComponentDetailsPage when table row is clicked', async () => {
      const mockData = [
        {
          componentDisplayText: 'com.example:lib:1.0.0',
          repository: 'maven-central',
          repositoryId: 'repo-1',
          componentIdentifier: 'com.example:lib:1.0.0',
          hash: 'abc123def456',
          matchState: 'QUARANTINE_MATCH',
          pathname: '/some/path/lib.jar',
          quarantineDate: mockQuarantineDate,
          dateCleared: mockDateCleared,
        },
      ];

      render(<FirewallUnquarantineTable {...baseProps} releaseQuarantineList={mockData} />);

      const table = screen.getByRole('table');
      const rowGroups = within(table).getAllByRole('rowgroup');
      const bodyRows = within(rowGroups[1]).getAllByRole('row');

      await userEvent.click(bodyRows[0]);

      expect(baseProps.goToRepositoryComponentDetailsPage).toHaveBeenCalledWith(
        'repo-1',
        'com.example:lib:1.0.0',
        'abc123def456',
        'QUARANTINE_MATCH',
        '/some/path/lib.jar',
        'com.example:lib:1.0.0'
      );
    });

    it('passes correct parameters from row data to goToRepositoryComponentDetailsPage', async () => {
      const mockData = [
        {
          componentDisplayText: 'org.springframework:spring-core:5.3.20',
          repository: 'nexus-central',
          repositoryId: 'central-repo-id-123',
          componentIdentifier: 'org.springframework:spring-core:5.3.20',
          hash: 'xyz789abc123',
          matchState: 'THREAT_DETECTED',
          pathname: '/repository/nexus-central/org/springframework/spring-core/5.3.20/spring-core-5.3.20.jar',
          quarantineDate: mockQuarantineDate,
          dateCleared: mockDateCleared,
        },
      ];

      render(<FirewallUnquarantineTable {...baseProps} releaseQuarantineList={mockData} />);

      const table = screen.getByRole('table');
      const rowGroups = within(table).getAllByRole('rowgroup');
      const bodyRows = within(rowGroups[1]).getAllByRole('row');

      await userEvent.click(bodyRows[0]);

      expect(baseProps.goToRepositoryComponentDetailsPage).toHaveBeenCalledWith(
        'central-repo-id-123',
        'org.springframework:spring-core:5.3.20',
        'xyz789abc123',
        'THREAT_DETECTED',
        '/repository/nexus-central/org/springframework/spring-core/5.3.20/spring-core-5.3.20.jar',
        'org.springframework:spring-core:5.3.20'
      );
    });

    it('marks table row as clickable', () => {
      const mockData = [
        {
          componentDisplayText: 'com.example:lib:1.0.0',
          repository: 'maven-central',
          repositoryId: 'repo-1',
          componentIdentifier: 'com.example:lib:1.0.0',
          hash: 'abc123',
          matchState: 'QUARANTINE',
          pathname: '/path',
          quarantineDate: mockQuarantineDate,
          dateCleared: mockDateCleared,
        },
      ];

      render(<FirewallUnquarantineTable {...baseProps} releaseQuarantineList={mockData} />);

      const table = screen.getByRole('table');
      const rowGroups = within(table).getAllByRole('rowgroup');
      const bodyRows = within(rowGroups[1]).getAllByRole('row');

      expect(bodyRows[0]).toHaveClass('nx-clickable');
    });
  });

  describe('date formatting', () => {
    it('formats quarantine date correctly', () => {
      const mockData = [
        {
          componentDisplayText: 'com.example:lib:1.0.0',
          repository: 'maven-central',
          repositoryId: 'repo-1',
          componentIdentifier: 'com.example:lib:1.0.0',
          hash: 'abc123',
          matchState: 'QUARANTINE',
          pathname: '/path',
          quarantineDate: mockQuarantineDate,
          dateCleared: mockDateCleared,
        },
      ];

      render(<FirewallUnquarantineTable {...baseProps} releaseQuarantineList={mockData} />);

      const expectedDate = formatDate(mockQuarantineDate, STANDARD_DATE_FORMAT);
      expect(screen.getByText(expectedDate)).toBeInTheDocument();
    });

    it('formats date cleared correctly', () => {
      const mockData = [
        {
          componentDisplayText: 'com.example:lib:1.0.0',
          repository: 'maven-central',
          repositoryId: 'repo-1',
          componentIdentifier: 'com.example:lib:1.0.0',
          hash: 'abc123',
          matchState: 'QUARANTINE',
          pathname: '/path',
          quarantineDate: mockQuarantineDate,
          dateCleared: mockDateCleared,
        },
      ];

      render(<FirewallUnquarantineTable {...baseProps} releaseQuarantineList={mockData} />);

      const expectedDate = formatDate(mockDateCleared, STANDARD_DATE_FORMAT);
      expect(screen.getByText(expectedDate)).toBeInTheDocument();
    });
  });

  describe('edge cases', () => {
    it('handles empty repository name', () => {
      const mockData = [
        {
          componentDisplayText: 'com.example:lib:1.0.0',
          repository: '',
          repositoryId: 'repo-1',
          componentIdentifier: 'com.example:lib:1.0.0',
          hash: 'abc123',
          matchState: 'QUARANTINE',
          pathname: '/path',
          quarantineDate: mockQuarantineDate,
          dateCleared: mockDateCleared,
        },
      ];

      render(<FirewallUnquarantineTable {...baseProps} releaseQuarantineList={mockData} />);

      const table = screen.getByRole('table');
      const rowGroups = within(table).getAllByRole('rowgroup');
      const bodyRows = within(rowGroups[1]).getAllByRole('row');

      expect(bodyRows).toHaveLength(1);
    });

    it('handles special characters in component display text', () => {
      const specialComponentName = 'com.example:lib-special_chars.test:1.0.0+build123';
      const mockData = [
        {
          componentDisplayText: specialComponentName,
          repository: 'maven-central',
          repositoryId: 'repo-1',
          componentIdentifier: specialComponentName,
          hash: 'abc123',
          matchState: 'QUARANTINE',
          pathname: '/path',
          quarantineDate: mockQuarantineDate,
          dateCleared: mockDateCleared,
        },
      ];

      render(<FirewallUnquarantineTable {...baseProps} releaseQuarantineList={mockData} />);

      expect(screen.getByText(specialComponentName)).toBeInTheDocument();
    });
  });
});
