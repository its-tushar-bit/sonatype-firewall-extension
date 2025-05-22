/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.sbom.export;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.sonatype.insight.SbomIdentityUtils;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
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
import org.apache.commons.collections4.MapUtils;
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
import org.spdx.library.model.license.AnyLicenseInfo;
import org.spdx.library.model.license.ConjunctiveLicenseSet;
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

  protected Map<String, String> spdxIdsToComponentRefs = new HashMap<>();

  protected boolean hasComponentRefs;

  protected AbstractSpdxExporter(
      final InsightWork insightWork,
      final ThirdPartyFileDAO thirdPartyFileDAO,
      final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO,
      final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO,
      final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO,
      final ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDAO,
      final BaseUrl baseUrl,
      final IdUtils idUtils,
      final VersionService versionService)
  {
    super(insightWork, thirdPartyFileDAO, thirdPartyFileCoordinateDAO, thirdPartyCoordinateSecurityDAO,
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
    copyComponents(originalDocument, newDocument);
    newDocument.setExternalDocumentRefs(originalDocument.getExternalDocumentRefs());
    finalizeExtractedLicensingInfo(newDocument);
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
      final SpdxDocument newDocument)
      throws InvalidSPDXAnalysisException
  {
    SpdxPackage originalRootPkg = SbomSpdxUtils.getRootPackage(originalDocument);
    List<SpdxPackage> originalDocPackages = SbomSpdxUtils.getAllPackages(originalDocument);

    for (SpdxPackage pkg : originalDocPackages) {
      // A valid existing package will always have a name. It is unlikely to have this "unknown package" to get called
      String pkgName = pkg.getName().orElse("UNKNOWN PACKAGE");

      SpdxPackage.SpdxPackageBuilder pkgBuilder = newDocument
          .createPackage(pkg.getId(), pkgName, pkg.getLicenseConcluded(), pkg.getCopyrightText(),
              pkg.getLicenseDeclared())
          .setFilesAnalyzed(pkg.isFilesAnalyzed());
      pkg.getDownloadLocation().ifPresent(pkgBuilder::setDownloadLocation);

      ThirdPartyFileCoordinate matchingDbComponent;
      if (hasComponentRefs) {
        matchingDbComponent = getDbComponentByComponentRef(pkg);
      }
      else {
        matchingDbComponent = thirdPartyFileCoordinateDAO.getByThirdPartyFileIdAndPackageUrl(
            exportParams.sbomMetadata.getThirdPartyFileId(), SbomSpdxUtils.getPurl(pkg));
      }

      Collection<ExternalRef> externalRefs = pkg.getExternalRefs();

      Collection<AnyLicenseInfo> licenseInfoFromDb = null;
      if (matchingDbComponent != null) {
        licenseInfoFromDb = getAllLicenseInfoFromDb(matchingDbComponent, newDocument);
        addVulnerabilityDiffsFromDatabase(newDocument, pkgBuilder, externalRefs, matchingDbComponent);
      }

      externalRefs.forEach(pkgBuilder::addExternalRef);

      pkg.getAttributionText().forEach(pkgBuilder::addAttributionText);

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
        ConjunctiveLicenseSet licenseSetForSpdxPackage = newPkg.createConjunctiveLicenseSet(licenseInfoFromDb);
        if (Objects.nonNull(licenseSetForSpdxPackage)) {
          newPkg.getAttributionText().add("Evidence license text for: " + licenseSetForSpdxPackage);
        }
      }
      if (originalRootPkg != null && StringUtils.equals(originalRootPkg.getId(), pkg.getId())) {
        newDocument.getDocumentDescribes().add(newPkg);
      }
    }
    copyDependencyRelationships(originalDocument, newDocument);
  }

  private void finalizeExtractedLicensingInfo(SpdxDocument spdxDocument) {
    try {
      List<SpdxPackage> spdxPackages = SbomSpdxUtils.getAllPackages(spdxDocument);
      for (SpdxPackage spdxPackage : spdxPackages) {
        AnyLicenseInfo licenseDeclared = spdxPackage.getLicenseDeclared();
        checkForExtractedLicenses(spdxDocument, licenseDeclared);
        AnyLicenseInfo licenseConcluded = spdxPackage.getLicenseConcluded();
        checkForExtractedLicenses(spdxDocument, licenseConcluded);
      }
    }
    catch (InvalidSPDXAnalysisException e) {
      log.debug("Failed to finalize extracted licensing info", e);
    }
  }

  private void checkForExtractedLicenses(SpdxDocument spdxDocument,
                                         AnyLicenseInfo licenseInfo) throws InvalidSPDXAnalysisException
  {
    if (licenseInfo instanceof ConjunctiveLicenseSet conjunctiveLicenseSet) {
      List<AnyLicenseInfo> licenseMembers = conjunctiveLicenseSet.getFlattenedMembers();
      for (AnyLicenseInfo license : licenseMembers ) {
        conditionallyAddLicenseToExtractedLicensingInfos(spdxDocument, license.getId());
      }
    }
    else if (licenseInfo instanceof ExtractedLicenseInfo extractedLicenseInfo) {
      conditionallyAddLicenseToExtractedLicensingInfos(spdxDocument, extractedLicenseInfo.getId());
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
      final SpdxDocument newDocument) throws InvalidSPDXAnalysisException
  {
    List<ThirdPartyCoordinateLicense> licenses =
        thirdPartyCoordinateLicenseDAO.getByFileCoordinateId(dbComponent.getId());
    Map<String, AnyLicenseInfo> collect = new HashMap<>();
    for (ThirdPartyCoordinateLicense license : licenses) {
      try {
        AnyLicenseInfo licenseObject = LicenseInfoFactory.parseSPDXLicenseString(license.getLicenseId(),
            newDocument.getModelStore(), newDocument.getDocumentUri(), newDocument.getCopyManager());
        collect.put(StringUtils.lowerCase(licenseObject.getId()), licenseObject);
      }
      catch (InvalidLicenseStringException e) {
        log.debug("Attempting to parse non-listed SPDX license string {} as a extracted license", license.getId());
        ExtractedLicenseInfo extractedLicenseInfo =
            conditionallyAddLicenseToExtractedLicensingInfos(newDocument, license.getId());
        if (extractedLicenseInfo != null) {
          collect.put(extractedLicenseInfo.getLicenseId(), extractedLicenseInfo);
        }
      }
    }
    return collect.values();
  }

  private ThirdPartyFileCoordinate getDbComponentByComponentRef(SpdxPackage pkg) {
    String componentRef;
    if (MapUtils.isNotEmpty(spdxIdsToComponentRefs) && spdxIdsToComponentRefs.containsKey(pkg.getId())) {
      componentRef = spdxIdsToComponentRefs.get(pkg.getId());
    }
    else {
      componentRef = SbomIdentityUtils.getComponentRef(pkg);
    }
    return componentRef != null ?
        thirdPartyFileCoordinateDAO.getByComponentRef(componentRef, exportParams.sbomMetadata.getThirdPartyFileId())
        : null;
  }

  private void copyDependencyRelationships(
      final SpdxDocument originalDocument,
      final SpdxDocument newDocument)
      throws InvalidSPDXAnalysisException
  {
    for (SpdxPackage pkg : SbomSpdxUtils.getAllPackages(originalDocument)) {
      SpdxPackage newPackage = SbomSpdxUtils.getPackageById(newDocument, pkg.getId());
      if (newPackage != null) {
        for (Relationship relationship : pkg.getRelationships()) {
          if (relationship.getRelatedSpdxElement().isPresent()) {
            SpdxPackage relatedPackage = SbomSpdxUtils.getPackageById(newDocument,
                relationship.getRelatedSpdxElement().get().getId());
            if (relatedPackage != null) {
              Relationship newDocumentRelationship = newDocument.createRelationship(relatedPackage,
                  relationship.getRelationshipType(), relationship.getComment().orElse(null));
              newPackage.addRelationship(newDocumentRelationship);
            }
          }
        }
      }
    }
  }

  private String generateSpdxValidExternalLicenseInfoId(String licenseId) {
    return (!licenseId.startsWith(NON_STD_LICENSE_ID_PRENUM)) ?
        NON_STD_LICENSE_ID_PRENUM + licenseId.replaceAll(INVALID_REF_REGEX, "-") : licenseId;
  }

  private ExtractedLicenseInfo conditionallyAddLicenseToExtractedLicensingInfos(final SpdxDocument spdxDocument,
                                                                final String originalLicense)
      throws InvalidSPDXAnalysisException
  {
    if (licenseCanBeAddedToExtractedLicensingInfos(spdxDocument, originalLicense)) {
      ExtractedLicenseInfo extractedLicenseInfo =
          spdxDocument.createExtractedLicense(generateSpdxValidExternalLicenseInfoId(originalLicense),
          "Extracted license created by Sonatype SBOM Manager");
      spdxDocument.addExtractedLicenseInfos(extractedLicenseInfo);
      return extractedLicenseInfo;
    }
    return null;
  }

  private boolean licenseCanBeAddedToExtractedLicensingInfos(final SpdxDocument spdxDocument,
                                                             final String originalLicense)
      throws InvalidSPDXAnalysisException
  {
    return !ListedLicenses.getListedLicenses().isSpdxListedLicenseId(originalLicense) &&
        spdxDocument.getExtractedLicenseInfos().stream().noneMatch(license ->
                license.getId().equalsIgnoreCase(generateSpdxValidExternalLicenseInfoId(originalLicense)));
  }
}
