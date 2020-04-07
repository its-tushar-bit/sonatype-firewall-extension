###  🤔 Nexus IQ found multiple policy violations introduced by this PR

  #### com.h2database : h2 : 1.4.190
  *10 of 10* Threat Level
  <details>
    <summary>Details</summary>
<p>

Threat (of 10) | Policy | Violation Details
--- | --- | --- |
10 | Unlikely Test Policy | <b>Nonsensical Constraint:</b><ul><li>Found security vulnerability: [CVE-2018-14335](http://localhost:1122/ui/links/vln/CVE-2018-14335)</li><li>Found licenses in the 'Liberal' license threat group ('BSD-3-Clause')</li><li>Found license threat group 'Weak Copyleft' with level >= 1 (level = 2)</li><li>Match state was 'Exact'</li><li>Component does not contain proprietary packages</li><li>Relative popularity was < 100% (relative popularity = 3%)</li></ul><b>Illogical Constraint:</b><ul><li>Found component older than 1 days</li><li>Coordinates were com.h2database : h2 : 1.4.190 (match com.h2database : h2 : * : * : 1.4.190)</li><li>Identification Source was Sonatype</li><li>Found label 'Architecture-Blacklisted'</li><li>Found 'MPL-2.0' license</li><li>License status was Open</li><li>Coordinates were com.h2database : h2 : 1.4.190 (matches package URL pkg:maven/com.h2database/h2@1.4.190?classifier=*&type=jar)</li></ul>
7 | Security-Medium | <b>Medium risk CVSS score:</b><ul><li>Found security vulnerability: [CVE-2018-14335](http://localhost:1122/ui/links/vln/CVE-2018-14335)</li></ul>
</p>
</details>

  #### org.springframework.security : spring-security-web : 4.2.3.RELEASE
  *10 of 10* Threat Level
  <details>
    <summary>Details</summary>
<p>

Threat (of 10) | Policy | Violation Details
--- | --- | --- |
10 | Unlikely Test Policy | <b>Nonsensical Constraint:</b><ul><li>Found security vulnerabilities: [CVE-2018-1199](http://localhost:1122/ui/links/vln/CVE-2018-1199), [sonatype-2017-0507](http://localhost:1122/ui/links/vln/sonatype-2017-0507), [sonatype-2017-0641](http://localhost:1122/ui/links/vln/sonatype-2017-0641), [sonatype-2019-0341](http://localhost:1122/ui/links/vln/sonatype-2019-0341), [sonatype-2019-0469](http://localhost:1122/ui/links/vln/sonatype-2019-0469)</li><li>Found licenses in the 'Liberal' license threat group ('Apache-2.0')</li><li>Match state was 'Exact'</li><li>Component does not contain proprietary packages</li><li>Relative popularity was < 100% (relative popularity = 20%)</li></ul><b>Illogical Constraint:</b><ul><li>Found component older than 1 days</li><li>Identification Source was Sonatype</li><li>License status was Open</li></ul>
9 | Security-High | <b>High risk CVSS score:</b><ul><li>Found security vulnerability: [sonatype-2017-0641](http://localhost:1122/ui/links/vln/sonatype-2017-0641)</li></ul>
7 | Security-Medium | <b>Medium risk CVSS score:</b><ul><li>Found security vulnerabilities: [CVE-2018-1199](http://localhost:1122/ui/links/vln/CVE-2018-1199), [sonatype-2017-0507](http://localhost:1122/ui/links/vln/sonatype-2017-0507), [sonatype-2019-0469](http://localhost:1122/ui/links/vln/sonatype-2019-0469)</li></ul>
3 | Security-Low | <b>Low risk CVSS score:</b><ul><li>Found security vulnerability: [sonatype-2019-0341](http://localhost:1122/ui/links/vln/sonatype-2019-0341)</li></ul>
</p>
</details>

  #### webgoat-server-8.0.0.M1.jar
  *2 of 10* Threat Level
  <details>
    <summary>Details</summary>
<p>

Threat (of 10) | Policy | Violation Details
--- | --- | --- |
2 | Component-Unknown | <b>Unknown 3rd party component:</b><ul><li>Match state was 'Unknown'</li><li>Component does not contain proprietary packages</li></ul>
</p>
</details>

  #### html-tampering-8.0.0.M1.jar
  *2 of 10* Threat Level
  <details>
    <summary>Details</summary>
<p>

Threat (of 10) | Policy | Violation Details
--- | --- | --- |
2 | Component-Unknown | <b>Unknown 3rd party component:</b><ul><li>Match state was 'Unknown'</li><li>Component does not contain proprietary packages</li></ul>
</p>
</details>

#### 😃🏆 Nice work! Nexus IQ determined that you fixed multiple outstanding policy violations

----
### Nexus IQ Report Detail
**Application**: TEST APP
**Organization**: TEST ORG

**Stage**: release

**[See full feature branch report](http://localhost:1122/ui/links/application/TEST_APP_PUBLIC_ID/report/toScanId)**
**[See full default branch report](http://localhost:1122/ui/links/application/TEST_APP_PUBLIC_ID/report/fromScanId)**

[Give feedback](https://community.sonatype.com/t/user-feedback-github-pr-reviews/3811)
