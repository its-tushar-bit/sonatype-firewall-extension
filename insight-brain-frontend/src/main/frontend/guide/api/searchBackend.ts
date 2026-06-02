/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { apiFetch, API_PREFIX } from './apiFetch';
import type { ReadonlySearchParams } from '@guide/ui-core/adapters';
import type { SearchResponse } from '@guide/ui-core/types';

export async function searchAll(searchParams: ReadonlySearchParams): Promise<SearchResponse> {
  return apiFetch<SearchResponse>(`${API_PREFIX}/global/search?${searchParams.toString()}`);
}
