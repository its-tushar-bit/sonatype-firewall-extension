/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { PageLayout, PageHeading, Button } from '@guide/ui-core';

export function NotFoundPage() {
  return (
    <PageLayout align="center">
      <PageHeading>We couldn&apos;t find that.</PageHeading>
      <Button variant="primary" href="/">Go to home</Button>
    </PageLayout>
  );
}
