/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Pagination } from 'MainRoot/nosc/components/Pagination';

function renderPagination(props: Partial<React.ComponentProps<typeof Pagination>> = {}) {
  const onPageChange = jest.fn();
  render(
    <Theme>
      <Pagination
        page={1}
        pageSize={10}
        totalItems={25}
        onPageChange={onPageChange}
        data-testid="test-pagination"
        {...props}
      />
    </Theme>,
  );
  return onPageChange;
}

describe('Pagination', () => {
  it('shows the current slice and page counts', () => {
    renderPagination({ page: 2, pageSize: 10, totalItems: 25 });
    expect(screen.getByText('Showing 11–20 of 25')).toBeInTheDocument();
    expect(screen.getByText('Page 2 of 3')).toBeInTheDocument();
  });

  it('disables Prev on the first page', () => {
    renderPagination({ page: 1, pageSize: 10, totalItems: 25 });
    expect(screen.getByRole('button', { name: 'Previous page' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Next page' })).not.toBeDisabled();
  });

  it('disables Next on the last page', () => {
    renderPagination({ page: 3, pageSize: 10, totalItems: 25 });
    expect(screen.getByRole('button', { name: 'Next page' })).toBeDisabled();
  });

  it('enables Next when hasNextPage is true even on the computed last page', () => {
    renderPagination({ page: 1, pageSize: 50, totalItems: 3, hasNextPage: true });
    expect(screen.getByRole('button', { name: 'Next page' })).not.toBeDisabled();
  });

  it('calls onPageChange when paging forward and back', async () => {
    const user = userEvent.setup();
    const onPageChange = renderPagination({ page: 2, pageSize: 10, totalItems: 25 });

    await user.click(screen.getByRole('button', { name: 'Next page' }));
    expect(onPageChange).toHaveBeenCalledWith(3);

    await user.click(screen.getByRole('button', { name: 'Previous page' }));
    expect(onPageChange).toHaveBeenCalledWith(1);
  });

  it('renders zero-state copy when there are no items', () => {
    renderPagination({ page: 1, pageSize: 10, totalItems: 0 });
    expect(screen.getByText('Showing 0–0 of 0')).toBeInTheDocument();
    expect(screen.getByText('Page 1 of 1')).toBeInTheDocument();
  });

  it('omits fabricated totals when totalItems is unknown', () => {
    renderPagination({ page: 2, pageSize: 25, totalItems: undefined, hasNextPage: true });
    expect(screen.getByText('Showing page 2')).toBeInTheDocument();
    expect(screen.getByText('Page 2')).toBeInTheDocument();
    expect(screen.queryByText(/of /)).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Next page' })).not.toBeDisabled();
  });
});
