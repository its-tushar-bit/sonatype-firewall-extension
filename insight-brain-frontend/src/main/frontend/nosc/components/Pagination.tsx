/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { Button, Flex, Text } from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc/icons';

interface PaginationProps {
  /** 1-based index of the current page. */
  readonly page: number;
  readonly pageSize: number;
  /**
   * Exact total when known. Omit (or pass undefined) for hasNext-only APIs so we
   * do not invent a page count from a lower-bound estimate.
   */
  readonly totalItems?: number;
  /** When true, Next stays enabled even if {@code page >= totalPages} (API signalled more results). */
  readonly hasNextPage?: boolean;
  /** Called with the new 1-based page when the user pages forward/back. */
  readonly onPageChange: (nextPage: number) => void;
  readonly 'data-testid'?: string;
}

/**
 * Shared "showing X–Y of Z" + Prev/Next pagination bar for nosc tables.
 * Extracted from the per-table copies that lived in PolicyFailuresTab and
 * ComponentsTab (CLM-39709 review #6). Pages are 1-based; callers that track
 * a 0-based index adapt at the boundary.
 */
export function Pagination({
  page,
  pageSize,
  totalItems,
  hasNextPage = false,
  onPageChange,
  ...rest
}: PaginationProps): JSX.Element {
  const exactTotal = typeof totalItems === 'number';
  const totalPages = exactTotal ? Math.max(1, Math.ceil(totalItems / pageSize)) : undefined;
  const canGoNext = hasNextPage || (totalPages !== undefined && page < totalPages);
  const firstShown = exactTotal
    ? (totalItems === 0 ? 0 : (page - 1) * pageSize + 1)
    : (page - 1) * pageSize + 1;
  const lastShown = exactTotal
    ? Math.min(page * pageSize, totalItems)
    : firstShown + pageSize - 1;

  return (
    <Flex align="center" justify="between" p="3" {...rest}>
      <Text size="2" color="gray">
        {exactTotal
          ? `Showing ${firstShown}–${lastShown} of ${totalItems}`
          : `Showing page ${page}`}
      </Text>
      <Flex align="center" gap="2">
        <Button
          size="1"
          variant="soft"
          color="gray"
          disabled={page <= 1}
          onClick={() => onPageChange(Math.max(1, page - 1))}
          aria-label="Previous page"
        >
          <ActionIcons.ChevronLeft size={14} />
          Prev
        </Button>
        <Text size="2" color="gray">
          {totalPages !== undefined ? `Page ${page} of ${totalPages}` : `Page ${page}`}
        </Text>
        <Button
          size="1"
          variant="soft"
          color="gray"
          disabled={!canGoNext}
          onClick={() => onPageChange(page + 1)}
          aria-label="Next page"
        >
          Next
        </Button>
      </Flex>
    </Flex>
  );
}
