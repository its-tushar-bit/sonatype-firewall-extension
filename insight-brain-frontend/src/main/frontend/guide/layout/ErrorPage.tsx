/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { Grid } from '@radix-ui/themes';
import { PageLayout, PageHeading, BodyText, Button } from '@guide/ui-core';
import { tokens } from '@guide/ui-core/utils';

interface ErrorPageProps {
  /** URL to navigate to when Retry is clicked. Defaults to "/" */
  retryHref?: string;
  /** Whether to show the "Go back" button. Defaults to true */
  showGoBack?: boolean;
}

export function ErrorPage({ retryHref = '/', showGoBack = true }: ErrorPageProps) {
  return (
    <PageLayout align="center">
      <PageHeading>We hit a snag.</PageHeading>
      <BodyText align="center">
        Please try again, or contact support if the problem persists.
      </BodyText>
      <Grid columns={showGoBack ? '2' : '1'} gap={tokens.space.item}>
        <Button variant="primary" href={retryHref}>
          Retry
        </Button>
        {showGoBack && (
          <Button variant="secondary" onClick={() => window.history.back()}>
            Go back
          </Button>
        )}
      </Grid>
    </PageLayout>
  );
}
