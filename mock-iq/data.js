// Mock Sonatype IQ Server data — covers a few well-known good/bad packages
// so the extension can be demoed against real npm/PyPI pages.

export const mockComponents = {
  // ===== npm =====
  "pkg:npm/lodash@4.17.21": {
    ecosystem: "npm",
    name: "lodash",
    version: "4.17.21",
    integrityRating: "Normal",
    threatTypes: [],
    cves: [],
    license: { declared: "MIT", observed: ["MIT"] },
  },
  "pkg:npm/lodash@4.17.10": {
    ecosystem: "npm",
    name: "lodash",
    version: "4.17.10",
    integrityRating: "Normal",
    threatTypes: [],
    cves: [
      {
        id: "CVE-2019-10744",
        sonatypeId: "sonatype-2019-0353",
        cvss: 9.1,
        severity: "critical",
        title: "Prototype pollution in defaultsDeep",
        reachable: true,
      },
    ],
    license: { declared: "MIT", observed: ["MIT"] },
    goldenVersion: { version: "4.17.21", fixesCves: ["CVE-2019-10744"], breakingChanges: false },
  },
  "pkg:npm/event-stream@3.3.6": {
    ecosystem: "npm",
    name: "event-stream",
    version: "3.3.6",
    integrityRating: "Malicious",
    threatTypes: ["trojan", "crypto-stealer"],
    cves: [],
    license: { declared: "MIT", observed: ["MIT"] },
    abfMatch: { matched: true, matchedAgainst: "sonatype-2018-flatmap-stream" },
  },
  "pkg:npm/colors@1.4.44-liberty-2": {
    ecosystem: "npm",
    name: "colors",
    version: "1.4.44-liberty-2",
    integrityRating: "Malicious",
    threatTypes: ["hijack", "data-corruption"],
    cves: [],
    license: { declared: "MIT", observed: ["MIT"] },
  },
  "pkg:npm/express@4.17.1": {
    ecosystem: "npm",
    name: "express",
    version: "4.17.1",
    integrityRating: "Normal",
    threatTypes: [],
    cves: [
      {
        id: "CVE-2022-24999",
        sonatypeId: "sonatype-2022-5732",
        cvss: 7.5,
        severity: "high",
        title: "qs DoS via prototype pollution",
        reachable: false,
      },
    ],
    license: { declared: "MIT", observed: ["MIT"] },
    goldenVersion: { version: "4.19.2", fixesCves: ["CVE-2022-24999"], breakingChanges: false },
  },
  "pkg:npm/react@18.3.1": {
    ecosystem: "npm",
    name: "react",
    version: "18.3.1",
    integrityRating: "Normal",
    threatTypes: [],
    cves: [],
    license: { declared: "MIT", observed: ["MIT"] },
  },

  // ===== PyPI =====
  "pkg:pypi/requests@2.32.3": {
    ecosystem: "pypi",
    name: "requests",
    version: "2.32.3",
    integrityRating: "Normal",
    threatTypes: [],
    cves: [],
    license: { declared: "Apache-2.0", observed: ["Apache-2.0"] },
  },
  "pkg:pypi/ctx@0.1.2": {
    ecosystem: "pypi",
    name: "ctx",
    version: "0.1.2",
    integrityRating: "Malicious",
    threatTypes: ["secrets-exfiltration"],
    cves: [],
    license: { declared: "MIT", observed: ["MIT"] },
  },

  // ===== Maven =====
  "pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1": {
    ecosystem: "maven",
    name: "org.apache.logging.log4j:log4j-core",
    version: "2.14.1",
    integrityRating: "Normal",
    threatTypes: [],
    cves: [
      {
        id: "CVE-2021-44228",
        sonatypeId: "sonatype-2021-4793",
        cvss: 10.0,
        severity: "critical",
        title: "Log4Shell — JNDI lookup remote code execution",
        reachable: true,
      },
    ],
    license: { declared: "Apache-2.0", observed: ["Apache-2.0"] },
    goldenVersion: { version: "2.17.1", fixesCves: ["CVE-2021-44228"], breakingChanges: false },
  },
  "pkg:maven/org.apache.logging.log4j/log4j-core@2.17.1": {
    ecosystem: "maven",
    name: "org.apache.logging.log4j:log4j-core",
    version: "2.17.1",
    integrityRating: "Normal",
    threatTypes: [],
    cves: [],
    license: { declared: "Apache-2.0", observed: ["Apache-2.0"] },
  },
  "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.9.10": {
    ecosystem: "maven",
    name: "com.fasterxml.jackson.core:jackson-databind",
    version: "2.9.10",
    integrityRating: "Normal",
    threatTypes: [],
    cves: [
      {
        id: "CVE-2020-36518",
        sonatypeId: "sonatype-2022-1996",
        cvss: 7.5,
        severity: "high",
        title: "Stack overflow via deeply nested JSON",
        reachable: true,
      },
      {
        id: "CVE-2019-20330",
        sonatypeId: "sonatype-2020-0026",
        cvss: 8.1,
        severity: "high",
        title: "Polymorphic deserialization gadget (jodd-db)",
        reachable: false,
      },
    ],
    license: { declared: "Apache-2.0", observed: ["Apache-2.0"] },
    goldenVersion: {
      version: "2.15.4",
      fixesCves: ["CVE-2020-36518", "CVE-2019-20330"],
      breakingChanges: false,
    },
  },
  "pkg:maven/org.springframework/spring-core@5.3.0": {
    ecosystem: "maven",
    name: "org.springframework:spring-core",
    version: "5.3.0",
    integrityRating: "Normal",
    threatTypes: [],
    cves: [
      {
        id: "CVE-2022-22965",
        sonatypeId: "sonatype-2022-2273",
        cvss: 9.8,
        severity: "critical",
        title: "Spring4Shell — class loader manipulation RCE",
        reachable: true,
      },
    ],
    license: { declared: "Apache-2.0", observed: ["Apache-2.0"] },
    goldenVersion: {
      version: "5.3.39",
      fixesCves: ["CVE-2022-22965"],
      breakingChanges: false,
    },
  },
  "pkg:maven/org.springframework/spring-core@5.3.39": {
    ecosystem: "maven",
    name: "org.springframework:spring-core",
    version: "5.3.39",
    integrityRating: "Normal",
    threatTypes: [],
    cves: [],
    license: { declared: "Apache-2.0", observed: ["Apache-2.0"] },
  },
  "pkg:maven/com.google.guava/guava@30.0-jre": {
    ecosystem: "maven",
    name: "com.google.guava:guava",
    version: "30.0-jre",
    integrityRating: "Normal",
    threatTypes: [],
    cves: [
      {
        id: "CVE-2023-2976",
        sonatypeId: "sonatype-2023-1789",
        cvss: 7.1,
        severity: "high",
        title: "FileBackedOutputStream temp-file permissions",
        reachable: false,
      },
    ],
    license: { declared: "Apache-2.0", observed: ["Apache-2.0"] },
    goldenVersion: { version: "32.1.3-jre", fixesCves: ["CVE-2023-2976"], breakingChanges: false },
  },
  "pkg:maven/org.apache.commons/commons-text@1.9": {
    ecosystem: "maven",
    name: "org.apache.commons:commons-text",
    version: "1.9",
    integrityRating: "Normal",
    threatTypes: [],
    cves: [
      {
        id: "CVE-2022-42889",
        sonatypeId: "sonatype-2022-5505",
        cvss: 9.8,
        severity: "critical",
        title: "Text4Shell — variable interpolation RCE",
        reachable: true,
      },
    ],
    license: { declared: "Apache-2.0", observed: ["Apache-2.0"] },
    goldenVersion: { version: "1.10.0", fixesCves: ["CVE-2022-42889"], breakingChanges: false },
  },
  "pkg:maven/com.h2database/h2@1.4.199": {
    ecosystem: "maven",
    name: "com.h2database:h2",
    version: "1.4.199",
    integrityRating: "Normal",
    threatTypes: [],
    cves: [
      {
        id: "CVE-2022-23221",
        sonatypeId: "sonatype-2022-0317",
        cvss: 9.8,
        severity: "critical",
        title: "JDBC URL CREATE ALIAS RCE",
        reachable: true,
      },
    ],
    license: { declared: "EPL-1.0", observed: ["EPL-1.0", "MPL-2.0"] },
    goldenVersion: { version: "2.2.224", fixesCves: ["CVE-2022-23221"], breakingChanges: true },
  },
  "pkg:maven/org.apache.struts/struts2-core@2.5.16": {
    ecosystem: "maven",
    name: "org.apache.struts:struts2-core",
    version: "2.5.16",
    integrityRating: "Normal",
    threatTypes: [],
    cves: [
      {
        id: "CVE-2018-11776",
        sonatypeId: "sonatype-2018-0605",
        cvss: 8.1,
        severity: "high",
        title: "Namespace OGNL injection RCE",
        reachable: true,
      },
    ],
    license: { declared: "Apache-2.0", observed: ["Apache-2.0"] },
    goldenVersion: { version: "2.5.33", fixesCves: ["CVE-2018-11776"], breakingChanges: false },
  },
  "pkg:maven/org.yaml/snakeyaml@1.29": {
    ecosystem: "maven",
    name: "org.yaml:snakeyaml",
    version: "1.29",
    integrityRating: "Normal",
    threatTypes: [],
    cves: [
      {
        id: "CVE-2022-1471",
        sonatypeId: "sonatype-2022-6438",
        cvss: 8.3,
        severity: "high",
        title: "Constructor unsafe deserialization",
        reachable: true,
      },
    ],
    license: { declared: "Apache-2.0", observed: ["Apache-2.0"] },
    goldenVersion: { version: "2.0", fixesCves: ["CVE-2022-1471"], breakingChanges: true },
  },
  // Typosquat — note the swapped 'l' for capital 'I'
  "pkg:maven/org.apache.commoms/commons-text@1.9": {
    ecosystem: "maven",
    name: "org.apache.commoms:commons-text",
    version: "1.9",
    integrityRating: "Malicious",
    threatTypes: ["typosquat", "dropper"],
    cves: [],
    license: { declared: "Apache-2.0", observed: ["Apache-2.0"] },
    abfMatch: { matched: true, matchedAgainst: "sonatype-2024-typosquat-commons-text" },
  },
  // Hijacked release with crypto-stealer payload
  "pkg:maven/io.acme.utils/acme-utils@1.4.7": {
    ecosystem: "maven",
    name: "io.acme.utils:acme-utils",
    version: "1.4.7",
    integrityRating: "Malicious",
    threatTypes: ["hijack", "secrets-exfiltration"],
    cves: [],
    license: { declared: "MIT", observed: ["MIT"] },
  },
  // Suspicious — pending Sonatype Research review
  "pkg:maven/com.example.tools/json-helper@0.0.1-RC1": {
    ecosystem: "maven",
    name: "com.example.tools:json-helper",
    version: "0.0.1-RC1",
    integrityRating: "Suspicious",
    threatTypes: [],
    cves: [],
    license: { declared: "unknown", observed: [] },
  },
  // Clean modern release — popular OSS
  "pkg:maven/org.junit.jupiter/junit-jupiter@5.10.2": {
    ecosystem: "maven",
    name: "org.junit.jupiter:junit-jupiter",
    version: "5.10.2",
    integrityRating: "Normal",
    threatTypes: [],
    cves: [],
    license: { declared: "EPL-2.0", observed: ["EPL-2.0"] },
  },
  // ===== Maven — clean, allowed =====
  "pkg:maven/org.slf4j/slf4j-api@2.0.13": {
    ecosystem: "maven",
    name: "org.slf4j:slf4j-api",
    version: "2.0.13",
    integrityRating: "Normal",
    threatTypes: [],
    cves: [],
    license: { declared: "MIT", observed: ["MIT"] },
  },
  "pkg:maven/ch.qos.logback/logback-classic@1.5.6": {
    ecosystem: "maven",
    name: "ch.qos.logback:logback-classic",
    version: "1.5.6",
    integrityRating: "Normal",
    threatTypes: [],
    cves: [],
    license: { declared: "EPL-1.0", observed: ["EPL-1.0", "LGPL-2.1"] },
  },
  "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.17.2": {
    ecosystem: "maven",
    name: "com.fasterxml.jackson.core:jackson-databind",
    version: "2.17.2",
    integrityRating: "Normal",
    threatTypes: [],
    cves: [],
    license: { declared: "Apache-2.0", observed: ["Apache-2.0"] },
  },
  "pkg:maven/org.apache.commons/commons-lang3@3.14.0": {
    ecosystem: "maven",
    name: "org.apache.commons:commons-lang3",
    version: "3.14.0",
    integrityRating: "Normal",
    threatTypes: [],
    cves: [],
    license: { declared: "Apache-2.0", observed: ["Apache-2.0"] },
  },
  "pkg:maven/com.google.guava/guava@33.2.1-jre": {
    ecosystem: "maven",
    name: "com.google.guava:guava",
    version: "33.2.1-jre",
    integrityRating: "Normal",
    threatTypes: [],
    cves: [],
    license: { declared: "Apache-2.0", observed: ["Apache-2.0"] },
  },
  "pkg:maven/org.mockito/mockito-core@5.12.0": {
    ecosystem: "maven",
    name: "org.mockito:mockito-core",
    version: "5.12.0",
    integrityRating: "Normal",
    threatTypes: [],
    cves: [],
    license: { declared: "MIT", observed: ["MIT"] },
  },
  "pkg:maven/org.springframework.boot/spring-boot-starter@3.3.2": {
    ecosystem: "maven",
    name: "org.springframework.boot:spring-boot-starter",
    version: "3.3.2",
    integrityRating: "Normal",
    threatTypes: [],
    cves: [],
    license: { declared: "Apache-2.0", observed: ["Apache-2.0"] },
  },
  "pkg:maven/io.micrometer/micrometer-core@1.13.2": {
    ecosystem: "maven",
    name: "io.micrometer:micrometer-core",
    version: "1.13.2",
    integrityRating: "Normal",
    threatTypes: [],
    cves: [],
    license: { declared: "Apache-2.0", observed: ["Apache-2.0"] },
  },

  // GPL license — would trip a license policy in many orgs
  "pkg:maven/mysql/mysql-connector-java@8.0.30": {
    ecosystem: "maven",
    name: "mysql:mysql-connector-java",
    version: "8.0.30",
    integrityRating: "Normal",
    threatTypes: [],
    cves: [
      {
        id: "CVE-2023-21971",
        sonatypeId: "sonatype-2023-1234",
        cvss: 6.5,
        severity: "medium",
        title: "MySQL Connectors DoS",
        reachable: false,
      },
    ],
    license: {
      declared: "GPL-2.0-with-Classpath-exception",
      observed: ["GPL-2.0-with-Classpath-exception"],
      advancedLegalPack: "Copyleft Limited",
    },
  },
};

// Org policy — mirrors how IQ stages enforce verdicts
export function evaluatePolicy(comp) {
  if (comp.integrityRating === "Malicious") {
    return {
      verdict: "block",
      policyName: "Integrity-Rating",
      stage: "Proxy",
      reasons: [
        `Component is rated Malicious (${comp.threatTypes.join(", ") || "behavioral signals"})`,
      ],
      waiverEligible: false,
    };
  }
  if (comp.integrityRating === "Suspicious" || comp.integrityRating === "Pending") {
    return {
      verdict: "quarantine",
      policyName: "Integrity-Rating",
      stage: "Proxy",
      reasons: ["Awaiting Sonatype Research review"],
      waiverEligible: true,
    };
  }
  const critical = comp.cves.find((c) => c.severity === "critical");
  if (critical) {
    return {
      verdict: "block",
      policyName: "Security-Critical",
      stage: "Build",
      reasons: [`Critical CVE present (${critical.id}, CVSS ${critical.cvss})`],
      waiverEligible: true,
    };
  }
  const high = comp.cves.find((c) => c.severity === "high");
  if (high) {
    return {
      verdict: "warn",
      policyName: "Security-High",
      stage: "Develop",
      reasons: [`High-severity CVE present (${high.id})`],
      waiverEligible: true,
    };
  }
  return {
    verdict: "allow",
    policyName: undefined,
    stage: "Proxy",
    reasons: ["No matching policy violations"],
    waiverEligible: false,
  };
}

export function lookup(purl) {
  const direct = mockComponents[purl];
  if (direct) return direct;

  // Best-effort match for unknown versions: same ecosystem+name → return Unknown
  const m = purl.match(/^pkg:(npm|pypi|maven)\/(.+?)@(.+)$/);
  if (!m) return null;
  const [, ecosystem, name, version] = m;
  return {
    ecosystem,
    name: decodeURIComponent(name),
    version,
    integrityRating: "Unknown",
    threatTypes: [],
    cves: [],
    license: { declared: "unknown", observed: [] },
  };
}
