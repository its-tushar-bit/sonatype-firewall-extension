/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { Grid } from '@radix-ui/themes';
import { useNavigate } from 'react-router';
import { PageLayout, PageHeading, BodyText, Button } from '@guide/ui-core';
import { tokens } from '@guide/ui-core/utils';
import { reloadPage, getErrorRetryCount } from 'GuideRoot/utils/navigation';

const MAX_RETRIES = 3;

interface ErrorPageProps {
  onRetry?: () => void;
  showGoBack?: boolean;
}

export function ErrorPage({ onRetry = reloadPage, showGoBack = true }: ErrorPageProps) {
  const navigate = useNavigate();
  const retriesExhausted = getErrorRetryCount() >= MAX_RETRIES;

  const handleGoBack = () => {
    // Use browser history length rather than React Router's state.idx: the
    // sidebar uses plain href anchors (not <Link>), so pushState is never called
    // and history.state is always null. history.length > 1 reliably detects a
    // previous page regardless of the navigation mechanism.
    if (window.history.length > 1) {
      navigate(-1);
    } else {
      navigate('/');
    }
  };

  return (
    <PageLayout align="center">
      <PageHeading>We hit a snag.</PageHeading>
      <BodyText align="center">
        Please try again, or contact support if the problem persists.
      </BodyText>
      <Grid columns={showGoBack && !retriesExhausted ? '2' : '1'} gap={tokens.space.item}>
        {retriesExhausted ? (
          <BodyText align="center">
            Still not working? Please contact support.
          </BodyText>
        ) : (
          <Button variant="primary" onClick={onRetry}>
            Retry
          </Button>
        )}
        {showGoBack && (
          <Button variant="secondary" onClick={handleGoBack}>
            Go back
          </Button>
        )}
      </Grid>
    </PageLayout>
  );
}
