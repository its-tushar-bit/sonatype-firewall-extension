/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Card, Box, Text, Flex, Badge, Link, DataList } from '@radix-ui/themes';
import { LinedDataList, CVSSBadge, ItemTitle, useVulnerability } from '@guide/ui-core';
import {
  tokens,
  formatDisplayDate,
  capitalizeFirst,
  getCVSSSeverity,
  getEffectiveCvssSeverity,
  formatReferenceLabel,
} from '@guide/ui-core/utils';
import { ExternalLink, AlertTriangle } from 'lucide-react';
import { formatEpssScore } from '@guide/ui-core/utils';

const CWE_PATTERN = /^CWE-(\d+)$/;
const SAFE_URL_PATTERN = /^https?:\/\//i;

function isCweLinkable(cwe: string): boolean {
  return CWE_PATTERN.test(cwe);
}

function getCweLink(cwe: string): string {
  const match = cwe.match(CWE_PATTERN);
  if (!match) return '#';
  return `https://cwe.mitre.org/data/definitions/${match[1]}.html`;
}

export function SecurityDetailsTab() {
  const vulnerability = useVulnerability();

  return (
    <Box mt={tokens.space.section}>
      <Card size={tokens.card.small}>
        <Box p={tokens.space.item}>
          <ItemTitle mb={tokens.space.section}>{vulnerability.vulnId} Security Details</ItemTitle>
          <Box asChild p={tokens.space.item}>
            <LinedDataList>
              {/* CVE ID */}
              <DataList.Item>
                <DataList.Label>CVE ID</DataList.Label>
                <DataList.Value>
                  <Text size={tokens.sizes.body.sm}>{vulnerability.vulnId}</Text>
                </DataList.Value>
              </DataList.Item>

              {/* CWE */}
              <DataList.Item>
                <DataList.Label>CWE</DataList.Label>
                <DataList.Value>
                  {vulnerability.cwes && vulnerability.cwes.length > 0 ? (
                    <Flex direction="column" gap={tokens.space.inline}>
                      {vulnerability.cwes.map((cwe, index) => (
                        <Flex key={index} align="center" gap={tokens.space.inline}>
                          <Text size={tokens.sizes.body.sm}>{cwe}</Text>
                          {isCweLinkable(cwe) && (
                            <Link
                              href={getCweLink(cwe)}
                              target="_blank"
                              rel="noopener noreferrer"
                              aria-label={`Learn more about ${cwe}`}
                            >
                              <ExternalLink size={tokens.icon.theme} color="var(--blue-9)" />
                            </Link>
                          )}
                        </Flex>
                      ))}
                    </Flex>
                  ) : (
                    <Text size={tokens.sizes.body.sm} color="gray">
                      N/A
                    </Text>
                  )}
                </DataList.Value>
              </DataList.Item>

              {/* CVE Description */}
              <DataList.Item>
                <DataList.Label>CVE Description</DataList.Label>
                <DataList.Value>
                  <Text size={tokens.sizes.body.sm}>{vulnerability.summary || 'Not available'}</Text>
                </DataList.Value>
              </DataList.Item>

              {/* Published Date */}
              <DataList.Item>
                <DataList.Label>Published</DataList.Label>
                <DataList.Value>
                  <Text size={tokens.sizes.body.sm}>{formatDisplayDate(vulnerability.publishedAt)}</Text>
                </DataList.Value>
              </DataList.Item>

              {/* CVSS Score & Severity */}
              <DataList.Item>
                <DataList.Label>CVSS Score &amp; Severity</DataList.Label>
                <DataList.Value>
                  <Flex align="center" gap={tokens.space.inline}>
                    <CVSSBadge score={getEffectiveCvssSeverity(vulnerability)} />
                    <Text size={tokens.sizes.body.sm}>
                      {capitalizeFirst(getCVSSSeverity(getEffectiveCvssSeverity(vulnerability)))}
                    </Text>
                  </Flex>
                </DataList.Value>
              </DataList.Item>

              {/* CVSS Vector */}
              <DataList.Item>
                <DataList.Label>CVSS Vector</DataList.Label>
                <DataList.Value>
                  <Text size={tokens.sizes.body.sm} style={{ wordBreak: 'break-all' }}>
                    {vulnerability.cvssVector || 'Not available'}
                  </Text>
                </DataList.Value>
              </DataList.Item>

              {/* EPSS Score */}
              <DataList.Item>
                <DataList.Label>EPSS Score</DataList.Label>
                <DataList.Value>
                  <Text size={tokens.sizes.body.sm}>{formatEpssScore(vulnerability.epss)}</Text>
                </DataList.Value>
              </DataList.Item>

              {/* Malware */}
              <DataList.Item>
                <DataList.Label>Malware</DataList.Label>
                <DataList.Value>
                  <Text size={tokens.sizes.body.sm}>{vulnerability.isMalware ? 'Yes' : 'No'}</Text>
                </DataList.Value>
              </DataList.Item>

              {/* KEV Status */}
              <DataList.Item>
                <DataList.Label>KEV Status</DataList.Label>
                <DataList.Value>
                  <Flex align="center" gap={tokens.space.inline}>
                    {vulnerability.kev ? (
                      <>
                        <AlertTriangle size={tokens.icon.menu} color="var(--red-9)" />
                        <Badge color="red" variant="soft" size={tokens.badge.small}>
                          Known Exploited
                        </Badge>
                      </>
                    ) : (
                      <Text size={tokens.sizes.body.sm} color="gray">
                        Not in KEV Catalog: No known exploits
                      </Text>
                    )}
                  </Flex>
                </DataList.Value>
              </DataList.Item>

              {/* Vulnerable Methods */}
              {vulnerability.vulnerableMethods && vulnerability.vulnerableMethods.length > 0 && (
                <DataList.Item>
                  <DataList.Label>Vulnerable Methods</DataList.Label>
                  <DataList.Value>
                    <Flex direction="column" gap={tokens.space.inline}>
                      {vulnerability.vulnerableMethods.map((method, index) => (
                        <Text key={index} size={tokens.sizes.body.sm} style={{ fontFamily: 'var(--code-font-family)', fontSize: tokens.code.fontSize, wordBreak: 'break-all' }}>
                          {method.signature}
                        </Text>
                      ))}
                    </Flex>
                  </DataList.Value>
                </DataList.Item>
              )}

              {/* Affected Ecosystems */}
              <DataList.Item>
                <DataList.Label>Affected Ecosystems</DataList.Label>
                <DataList.Value>
                  <Flex align="center" gap={tokens.space.inline}>
                    <Text size={tokens.sizes.body.sm}>
                      {vulnerability.affectedEcosystems?.join(', ') || 'Not available'}
                    </Text>
                    <Badge variant="soft" color="gray" size={tokens.badge.small} radius="full">
                      {vulnerability.affectedEcosystems?.length || 0}
                    </Badge>
                  </Flex>
                </DataList.Value>
              </DataList.Item>

              {/* Source */}
              {vulnerability.source && (
                <DataList.Item>
                  <DataList.Label>Source</DataList.Label>
                  <DataList.Value>
                    <Text size={tokens.sizes.body.sm}>{vulnerability.source}</Text>
                  </DataList.Value>
                </DataList.Item>
              )}

              {/* References — pre-filter to safe HTTP(S) URLs so the section is hidden when none pass */}
              {(() => {
                const safeRefs = vulnerability.references?.filter((ref) => SAFE_URL_PATTERN.test(ref.link)) ?? [];
                if (safeRefs.length === 0) return null;
                return (
                  <DataList.Item>
                    <DataList.Label>References</DataList.Label>
                    <DataList.Value>
                      <Flex direction="column" gap={tokens.space.inline}>
                        {safeRefs.map((ref, index) => {
                          const { label, iconName } = formatReferenceLabel(ref.link);
                          const source = iconName === 'Github' ? 'GitHub' : iconName === 'Gitlab' ? 'GitLab' : null;
                          const displayLabel = source ? `${source} · ${label}` : label;
                          return (
                            <Flex key={index} align="center" gap={tokens.space.inline}>
                              <ExternalLink size={tokens.icon.theme} color="var(--gray-9)" />
                              <Link href={ref.link} title={ref.link} target="_blank" rel="noopener noreferrer">
                                <Text size={tokens.sizes.body.sm}>{displayLabel}</Text>
                              </Link>
                              <Badge variant="soft" color="gray" size={tokens.badge.small}>
                                {ref.type}
                              </Badge>
                            </Flex>
                          );
                        })}
                      </Flex>
                    </DataList.Value>
                  </DataList.Item>
                );
              })()}
            </LinedDataList>
          </Box>
        </Box>
      </Card>
    </Box>
  );
}
