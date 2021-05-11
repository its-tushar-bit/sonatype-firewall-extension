/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export const progressOptions = [
  {
    id: 'NOT_STARTED',
    name: 'Unreviewed',
  },
  {
    id: 'OPEN',
    name: 'In Progress or Completed',
  },
];

export const expandedProgressOptions = [
  {
    id: 'UNREVIEWED',
    name: 'Unreviewed',
  },
  {
    id: 'IN_PROGRESS',
    name: 'In Progress',
  },
  {
    id: 'FLAGGED',
    name: 'Flagged',
  },
  {
    id: 'COMPLETED',
    name: 'Completed',
  },
];

export const reviewStatusDisplayNames = {
  FLAGGED: 'Flagged',
  IN_PROGRESS: 'In Progress',
  NOT_STARTED: 'Not Started',
  UNREVIEWED: 'Unreviewed',
  COMPLETED: 'Completed',
};

export const statusRanking = {
  FLAGGED: 10,
  IN_PROGRESS: 7,
  UNREVIEWED: 5,
  NOT_STARTED: 3,
  COMPLETED: 1,
};
