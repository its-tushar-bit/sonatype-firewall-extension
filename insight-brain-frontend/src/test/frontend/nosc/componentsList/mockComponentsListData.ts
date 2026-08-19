/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type { ComponentsCatalogApiResponse } from 'MainRoot/nosc/componentsList/componentsListApi';

export const MOCK_COMPONENTS_CATALOG_RESPONSE: ComponentsCatalogApiResponse = {
  entityType: 'COMPONENT',
  source: 'local',
  catalogAvailable: true,
  page: 1,
  pageSize: 50,
  totalEstimate: 2,
  exactTotalEstimate: true,
  rows: [
    {
      id: 'guava',
      title: 'guava',
      subtitle: '31.1-jre',
      source: 'local',
      fields: { ecosystem: 'maven', organization: 'Java Team' },
    },
    {
      id: 'lodash',
      title: 'lodash',
      subtitle: '4.17.21',
      source: 'local',
      fields: { ecosystem: 'npm', organization: 'Platform' },
    },
  ],
  facets: {
    organization: [
      { value: 'Java Team', count: 1 },
      { value: 'Platform', count: 1 },
    ],
    ecosystem: [
      { value: 'maven', count: 1 },
      { value: 'npm', count: 1 },
    ],
  },
  nextSearchAfter: null,
  warnings: [],
};
