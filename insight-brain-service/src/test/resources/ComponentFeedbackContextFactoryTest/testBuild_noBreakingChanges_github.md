


# <img title="Severe" alt="Severe" src="https://cdn.sonatype.com/iq-for-scm/1.0/orange-bar.png" width="20" height="20"/> <img title="Direct Dependency" alt="Direct Dependency" src="https://cdn.sonatype.com/iq-for-scm/1.0/d-logo.png" width="20" height="20"/>  Sonatype IQ found critical issues introduced by com.fasterxml.jackson.core.jackson-databind:2.13.1
Threat Level: <strong>Severe (7)</strong> \| [View Component Details in Sonatype Lifecycle](https://iq.example.com/ui/links/application/some-public-app-id/report/some-feature-branch-scan-id/componentDetails/myhash123?source=pr-line-commenting)

## :shield: Recommendation
**Bumping to version 2.15.0** will resolve all policy violations for this component and its dependencies (as of _Jul 05, 2023_)
There are no breaking changes. This version upgrade requires minimal effort.

## :page_facing_up: Security Issue Details

<details>
  <summary title="View all (2)">
    View all (2)
  </summary>
  <p></p>

| **Severity** | **Issue** | **Organization Policy Violation** |
| --- | --- | --- |
| <b>Threat level:</b> 7<br><b>CVSS Score:</b> 6.7<br><br><img title="Sonatype Deep Dive" alt="Sonatype Deep Dive" src="https://cdn.sonatype.com/iq-for-scm/1.0/DeepDive.svg" width="900" /> | [SONATYPE-123-01] The is a description of SONATYPE-123-01 | [View Details](https://iq.example.com/assets/index.html#/violation/pv1?type=violation&sidebarReference=filter) |
| <b>Threat level:</b> 7<br><b>CVSS Score:</b> 5.6<br><br><img title="Sonatype Fast Track" alt="Sonatype Fast Track" src="https://cdn.sonatype.com/iq-for-scm/1.0/FastTrack.svg" width="900" /> | [CVE-123-01] The is a description of CVE-123-01 | [View Details](https://iq.example.com/assets/index.html#/violation/pv1?type=violation&sidebarReference=filter) |

</details>

```suggestion
            <version>2.15.0</version>
```

