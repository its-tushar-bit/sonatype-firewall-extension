/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.sbom.export;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.sbom.utils.SbomSpdxUtils;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.scan.file.SbomFormat;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.spdx.jacksonstore.MultiFormatStore;
import org.spdx.jacksonstore.MultiFormatStore.Format;
import org.spdx.library.DefaultModelStore;
import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.ModelCopyManager;
import org.spdx.library.SpdxConstants;
import org.spdx.library.model.ExternalRef;
import org.spdx.library.model.ReferenceType;
import org.spdx.library.model.Relationship;
import org.spdx.library.model.SpdxCreatorInformation;
import org.spdx.library.model.SpdxDocument;
import org.spdx.library.model.SpdxModelFactory;
import org.spdx.library.model.SpdxPackage;
import org.spdx.library.model.enumerations.ReferenceCategory;
import org.spdx.library.model.enumerations.RelationshipType;
import org.spdx.library.model.license.AnyLicenseInfo;
import org.spdx.library.model.license.ExtractedLicenseInfo;
import org.spdx.library.model.license.InvalidLicenseStringException;
import org.spdx.library.model.license.LicenseInfoFactory;
import org.spdx.library.model.license.ListedLicenses;
import org.spdx.library.model.license.SpdxListedLicense;
import org.spdx.storage.simple.InMemSpdxStore;

import static org.spdx.library.SpdxConstants.NON_STD_LICENSE_ID_PRENUM;

public abstract class AbstractSpdxExporter
    extends AbstractSbomExporter
{
  static final String INVALID_REF_REGEX = "[^0-9a-zA-Z\\.\\-\\+]";

  protected SbomFormat sbomFormat;

  protected Format format;

  protected MultiFormatStore multiFormatStore;

  protected ModelCopyManager copyManager = new ModelCopyManager();

  protected AbstractSpdxExporter(
      final InsightWork insightWork,
      final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO,
      final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO,
      final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO,
      final ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDAO,
      final BaseUrl baseUrl,
      final IdUtils idUtils,
      final VersionService versionService)
  {
    super(insightWork, thirdPartyFileCoordinateDAO, thirdPartyCoordinateSecurityDAO,
        thirdPartyCoordinateLicenseDAO, thirdPartyVulnerabilityExploitabilityExchangeDAO, baseUrl, idUtils,
        versionService);
  }

  void init() {
    DefaultModelStore.reset();
    sbomFormat = SbomFormat.forString(exportParams.sbomMetadata.getSpecFormat());
    format = sbomFormat == SbomFormat.JSON ? Format.JSON : Format.XML;
    multiFormatStore = new MultiFormatStore(new InMemSpdxStore(), format, MultiFormatStore.Verbose.COMPACT);
  }

  SpdxDocument createNewDocumentFrom(
      final SpdxDocument originalDocument) throws InvalidSPDXAnalysisException
  {
    SpdxDocument newDocument =
        SpdxModelFactory.createSpdxDocument(multiFormatStore, getBillOfMaterialsPath(), copyManager);
    setMetadata(newDocument);
    SpdxPackage rootPackage = SbomSpdxUtils.getRootPackage(originalDocument);
    copyComponents(originalDocument, newDocument, rootPackage);
    newDocument.setExternalDocumentRefs(originalDocument.getExternalDocumentRefs());
    return newDocument;
  }

  void setMetadata(SpdxDocument newDocument) throws InvalidSPDXAnalysisException {
    newDocument.setSpecVersion("SPDX-" + exportParams.exportSpecification.getVersion());
    newDocument.setName(idUtils.getPublicOwnerId(OwnerType.APPLICATION, exportParams.sbomMetadata.getApplicationId()));
    newDocument.setDataLicense(new SpdxListedLicense(SpdxConstants.SPDX_DATA_LICENSE_ID));

    SpdxCreatorInformation creatorInfo = new SpdxCreatorInformation();
    String date = LocalDateTime.now(ZoneOffset.UTC).format(DATE_TIME_FORMATTER.toFormatter());

    creatorInfo.setCreated(date);
    creatorInfo.getCreators().add("Tool: Sonatype SBOM Manager - " + versionService.getFullVersion());
    //not adding data date (for NDE customers) until is needed.
    newDocument.setCreationInfo(creatorInfo);
  }

  private ExternalRef newVulnerabilityRefFor(SpdxDocument document, final ThirdPartyCoordinateSecurity dbVulnerability)
      throws InvalidSPDXAnalysisException
  {
    String comment = null;
    if (StringUtils.isNotBlank(dbVulnerability.getVulnerabilitySource())) {
      comment = "source: " + dbVulnerability.getVulnerabilitySource().toUpperCase(Locale.ROOT);
    }
    String locator;
    if (StringUtils.isNotBlank(dbVulnerability.getLink())) {
      locator = dbVulnerability.getLink();
    }
    else if (dbVulnerability.getRefId().toLowerCase().startsWith("sonatype")) {
      String baseUrlValue;
      try {
        baseUrlValue = baseUrl.get();
      }
      catch (IllegalStateException e) {
        log.debug("Base URL not configured. Unable to generate external reference for vulnerability id {} in SBOM " +
            "version {}:{}", dbVulnerability.getRefId(), exportParams.sbomMetadata.getApplicationId(),
            exportParams.sbomMetadata.getSbomVersion());
        return null;
      }
      locator = baseUrlValue + "ui/links/vln/" + dbVulnerability.getRefId();
    }
    else {
      return null;
    }
    return document.createExternalRef(ReferenceCategory.SECURITY, new ReferenceType("advisory"), locator, comment);
  }

  private void copyComponents(
      final SpdxDocument originalDocument,
      final SpdxDocument newDocument,
      final SpdxPackage originalRootPkg)
      throws InvalidSPDXAnalysisException
  {
    SpdxPackage newRootPkg = null;
    List<SpdxPackage> potentialDirects = new ArrayList<>();
    List<String> transitives = new ArrayList<>();
    Map<String, ExtractedLicenseInfo> extractedLicenses = originalDocument.getExtractedLicenseInfos()
        .stream().collect(Collectors.toMap(l -> StringUtils.lowerCase(l.getLicenseId()), l -> l));

    for (SpdxPackage pkg : SbomSpdxUtils.getAllPackages(originalDocument)) {
      // a valid existing package will always have a name. It is unlikely to have this "unknown package" to get called
      String pkgName = pkg.getName().orElse("UNKNOWN PACKAGE");

      SpdxPackage.SpdxPackageBuilder pkgBuilder = newDocument
          .createPackage(pkg.getId(), pkgName, pkg.getLicenseConcluded(), pkg.getCopyrightText(),
              pkg.getLicenseDeclared())
          .setFilesAnalyzed(false);
      pkg.getDownloadLocation().ifPresent(pkgBuilder::setDownloadLocation);

      ThirdPartyFileCoordinate matchingDbComponent = getMatchingDbComponent(pkg);
      Collection<ExternalRef> externalRefs = pkg.getExternalRefs();

      Collection<AnyLicenseInfo> licenseInfoFromDb = null;
      if (matchingDbComponent != null) {
        licenseInfoFromDb = getAllLicenseInfoFromDb(matchingDbComponent, extractedLicenses);
        addVulnerabilityDiffsFromDatabase(newDocument, pkgBuilder, externalRefs, matchingDbComponent);
      }
      externalRefs.forEach(pkgBuilder::addExternalRef);

      pkg.getVersionInfo().ifPresent(pkgBuilder::setVersionInfo);
      pkg.getChecksums().forEach(pkgBuilder::addChecksum);
      pkg.getDescription().ifPresent(pkgBuilder::setDescription);
      pkg.getHomepage().ifPresent(pkgBuilder::setHomepage);
      pkg.getOriginator().ifPresent(pkgBuilder::setOriginator);
      pkg.getPackageFileName().ifPresent(pkgBuilder::setPackageFileName);
      pkg.getPackageVerificationCode().ifPresent(pkgBuilder::setPackageVerificationCode);
      pkg.getPrimaryPurpose().ifPresent(pkgBuilder::setPrimaryPurpose);
      pkg.getReleaseDate().ifPresent(pkgBuilder::setReleaseDate);
      pkg.getSourceInfo().ifPresent(pkgBuilder::setSourceInfo);
      pkg.getSupplier().ifPresent(pkgBuilder::setSupplier);
      pkg.getValidUntilDate().ifPresent(pkgBuilder::setValidUntilDate);
      pkg.getComment().ifPresent(pkgBuilder::setComment);
      pkg.getFiles().forEach(pkgBuilder::addFile);
      pkg.getAnnotations().forEach(pkgBuilder::addAnnotation);

      SpdxPackage newPkg = buildPackage(pkgBuilder);
      if (CollectionUtils.isNotEmpty(licenseInfoFromDb)) {
        AnyLicenseInfo spdxLicenseEvidence = newPkg.createConjunctiveLicenseSet(licenseInfoFromDb);
        if (Objects.nonNull(spdxLicenseEvidence)) {
          newPkg.getAttributionText().add("Evidence license text for: " + spdxLicenseEvidence);
        }
      }
      //pre-process relationships
      if (originalRootPkg != null && StringUtils.equals(originalRootPkg.getId(), pkg.getId())) {
        newRootPkg = newPkg;
      }
      else {
        Collection<Relationship> relationships = pkg.getRelationships();
        if (CollectionUtils.isNotEmpty(relationships)) {
          for (Relationship relationship : relationships) {
            pkgBuilder.addRelationship(relationship);
          }
          potentialDirects.add(newPkg);
        }
        else {
          transitives.add(newPkg.getId());
        }
      }
    }

    //any non-SPDX licenses added from DB (extracted) needs to be referenced at document level too.
    // Otherwise the document is considered invalid.
    newDocument.getExtractedLicenseInfos().addAll(extractedLicenses.values());
    //For some (weird) reason, copying the original dependency graph as is to the new document does not generate the
    // same dependency graph by the SDK. Hence, the creation of the graph manually.
    addDependencyRelationships(newDocument, newRootPkg, potentialDirects, transitives);
  }

  private static void addDependencyRelationships(
      final SpdxDocument newDocument,
      final SpdxPackage newRootPkg,
      final List<SpdxPackage> potentialDirects,
      final List<String> transitives) throws InvalidSPDXAnalysisException
  {
    if (newRootPkg != null) {
      newDocument.getDocumentDescribes().add(newRootPkg);
      for (SpdxPackage potentialDirect : potentialDirects) {
        if (!transitives.contains(potentialDirect.getId())) {
          //true direct
          Relationship relationship =
              newDocument.createRelationship(potentialDirect, RelationshipType.DEPENDS_ON, null);
          newRootPkg.addRelationship(relationship);
        }
      }
    }
  }

  private SpdxPackage buildPackage(final SpdxPackage.SpdxPackageBuilder pkgBuilder)
      throws InvalidSPDXAnalysisException
  {
    return pkgBuilder.build();
  }

  private void addVulnerabilityDiffsFromDatabase(
      final SpdxDocument document,
      final SpdxPackage.SpdxPackageBuilder pkgBuilder,
      final Collection<ExternalRef> externalRefs,
      final ThirdPartyFileCoordinate dbComponent) throws InvalidSPDXAnalysisException
  {
    List<ThirdPartyCoordinateSecurity> dbVulnerabilities =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(dbComponent.getId());
    for (ThirdPartyCoordinateSecurity dbVulnerability : dbVulnerabilities) {
      if (vulnerabilityAlreadyExists(externalRefs, dbVulnerability.getRefId())) {
        continue;
      }
      //new vulnerability not in original sbom. adding it
      ExternalRef externalRef = newVulnerabilityRefFor(document, dbVulnerability);
      if (externalRef != null) {
        pkgBuilder.addExternalRef(externalRef);
      }
    }
  }

  private boolean vulnerabilityAlreadyExists(final Collection<ExternalRef> externalRefs, final String refId)
      throws InvalidSPDXAnalysisException
  {
    for (ExternalRef externalRef : externalRefs) {
      if (StringUtils.equals(SbomSpdxUtils.getRefIdForVulnerability(externalRef), refId)) {
        return true;
      }
    }
    return false;
  }

  private Collection<AnyLicenseInfo> getAllLicenseInfoFromDb(
      final ThirdPartyFileCoordinate dbComponent,
      final Map<String, ExtractedLicenseInfo> extractedLicenses)
  {
    List<ThirdPartyCoordinateLicense> licenses =
        thirdPartyCoordinateLicenseDAO.getByFileCoordinateId(dbComponent.getId());
    Map<String, AnyLicenseInfo> collect = new HashMap<>();
    for (ThirdPartyCoordinateLicense license : licenses) {
      AnyLicenseInfo licenseObject = createLicenseObject(license.getLicenseId(), extractedLicenses);
      collect.put(StringUtils.lowerCase(licenseObject.getId()), licenseObject);
    }
    return collect.values();
  }

  private ThirdPartyFileCoordinate getMatchingDbComponent(final SpdxPackage pkg) {
    try {
      String purl = SbomSpdxUtils.getPurl(pkg);
      return thirdPartyFileCoordinateDAO.getByThirdPartyFileIdAndPackageUrl(
          exportParams.sbomMetadata.getThirdPartyFileId(), purl);
    }
    catch (InvalidSPDXAnalysisException e) {
      throw new SbomExportException("error determining purl for pkg", e);
    }
  }

  private AnyLicenseInfo createLicenseObject(
      String licenseId,
      final Map<String, ExtractedLicenseInfo> extractedLicenses)
  {
    AnyLicenseInfo anyLicenseInfo = null;
    try {
      anyLicenseInfo = LicenseInfoFactory.parseSPDXLicenseString(licenseId);
    }
    catch (InvalidLicenseStringException e) {
      log.debug("Failed to parse spdx license string: {}.", licenseId);
    }
    if (anyLicenseInfo == null) {
      try {
        if (ListedLicenses.getListedLicenses().isSpdxListedLicenseId(licenseId)) {
          return ListedLicenses.getListedLicenses().getListedLicenseById(licenseId);
        }

        if (!licenseId.startsWith(NON_STD_LICENSE_ID_PRENUM)) {
          licenseId = NON_STD_LICENSE_ID_PRENUM + licenseId.replaceAll(INVALID_REF_REGEX, "-");
        }
        String lowerLicenseId = StringUtils.lowerCase(licenseId, Locale.ROOT);
        if (extractedLicenses.containsKey(lowerLicenseId)) {
          return extractedLicenses.get(lowerLicenseId);
        }
        else {
          ExtractedLicenseInfo extractedLicenseInfo = new ExtractedLicenseInfo(licenseId, licenseId);
          extractedLicenses.put(StringUtils.lowerCase(extractedLicenseInfo.getLicenseId(), Locale.ROOT),
              extractedLicenseInfo);
          return extractedLicenseInfo;
        }
      }
      catch (InvalidSPDXAnalysisException e) {
        throw new SbomExportException("Internal error extracting license information for " + licenseId, e);
      }
    }
    return anyLicenseInfo;
  }
}
