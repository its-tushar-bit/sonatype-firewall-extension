/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { Button, Card, Flex, Text } from '@radix-ui/themes';
import { LoadingSkeleton } from 'MainRoot/nosc/components/LoadingSkeleton';
import { useEstateComponentDetailShellContext } from './estateComponentDetailContext';
import type { EstateComponentLicense } from './estateComponentDetailsApi';

function LicenseList({
  title,
  licenses,
  testId,
}: {
  readonly title: string;
  readonly licenses: ReadonlyArray<EstateComponentLicense> | undefined;
  readonly testId: string;
}): JSX.Element {
  return (
    <Card data-testid={testId}>
      <Flex direction="column" gap="2" p="4">
        <Text size="3" weight="medium">
          {title}
        </Text>
        {!licenses?.length ? (
          <Text size="2" color="gray">
            None
          </Text>
        ) : (
          licenses.map((license, idx) => (
            <Text key={`${license.licenseId ?? 'lic'}-${idx}`} size="2">
              {license.licenseName || license.licenseId || 'Unknown license'}
              {license.licenseId && license.licenseName ? ` (${license.licenseId})` : ''}
            </Text>
          ))
        )}
      </Flex>
    </Card>
  );
}

export function EstateComponentLegalTab(): JSX.Element {
  const { hdsStatus, details, retryHds } = useEstateComponentDetailShellContext();

  if (hdsStatus === 'loading') {
    return <LoadingSkeleton height={180} data-testid="nosc-estate-component-legal-loading" />;
  }

  if (hdsStatus === 'error') {
    return (
      <Flex
        direction="column"
        gap="3"
        align="start"
        mt="4"
        data-testid="nosc-estate-component-legal-error"
      >
        <Text size="2" color="red">
          Legal details are temporarily unavailable. Other tabs still work.
        </Text>
        <Button size="2" variant="soft" onClick={retryHds} data-testid="nosc-estate-component-legal-retry">
          Retry
        </Button>
      </Flex>
    );
  }

  if (hdsStatus === 'empty' || !details) {
    return (
      <Flex direction="column" gap="2" mt="4" data-testid="nosc-estate-component-legal-empty">
        <Text size="2" color="gray">
          No legal details were found for this component.
        </Text>
      </Flex>
    );
  }

  const licenseData = details.licenseData;

  return (
    <Flex direction="column" gap="4" mt="4" data-testid="nosc-estate-component-legal">
      <Text size="2" color="gray">
        Status: {licenseData?.status || '—'}
      </Text>
      <LicenseList
        title="Declared licenses"
        licenses={licenseData?.declaredLicenses}
        testId="nosc-estate-component-legal-declared"
      />
      <LicenseList
        title="Observed licenses"
        licenses={licenseData?.observedLicenses}
        testId="nosc-estate-component-legal-observed"
      />
      <LicenseList
        title="Effective licenses"
        licenses={licenseData?.effectiveLicenses}
        testId="nosc-estate-component-legal-effective"
      />
    </Flex>
  );
}
