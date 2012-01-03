package org.springframework.roo.classpath;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.file.monitor.FileMonitorService;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.PhysicalPath;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.shell.NaturalOrderComparator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.StringUtils;

/**
 * Implementation of {@link TypeLocationService}.
 * <p>
 * For performance reasons automatically caches the queries. The cache is
 * invalidated on changes to the file system.
 * 
 * @author Alan Stewart
 * @author Ben Alex
 * @author Stefan Schmidt
 * @author James Tyrrell
 * @since 1.1
 */
@Component(immediate = true)
@Service
public class TypeLocationServiceImpl implements TypeLocationService {

    // Fields
    @Reference private FileManager fileManager;
    @Reference private FileMonitorService fileMonitorService;
    @Reference private MetadataService metadataService;
    @Reference private ProjectOperations projectOperations;
    @Reference private TypeCache typeCache;
    @Reference private TypeResolutionService typeResolutionService;

    private final Map<JavaType, Set<String>> annotationToMidMap = new HashMap<JavaType, Set<String>>();
    private final Map<Object, Set<String>> tagToMidMap = new HashMap<Object, Set<String>>();
    private final Map<String, Set<JavaType>> typeAnnotationMap = new HashMap<String, Set<JavaType>>();
    private final Map<String, Set<Object>> typeCustomDataMap = new HashMap<String, Set<Object>>();
    private final Map<String, Set<String>> changeMap = new HashMap<String, Set<String>>();
    private final Set<String> dirtyFiles = new HashSet<String>();
    private final Set<String> discoveredTypes = new HashSet<String>();

    public Set<ClassOrInterfaceTypeDetails> findClassesOrInterfaceDetailsWithAnnotation(
            final JavaType... annotationsToDetect) {
        final List<ClassOrInterfaceTypeDetails> types = new ArrayList<ClassOrInterfaceTypeDetails>();
        processTypesWithAnnotation(Arrays.asList(annotationsToDetect),
                new LocatedTypeCallback() {
                    public void process(
                            final ClassOrInterfaceTypeDetails located) {
                        if (located != null) {
                            types.add(located);
                        }
                    }
                });
        Collections.sort(types,
                new NaturalOrderComparator<ClassOrInterfaceTypeDetails>() {
                    @Override
                    protected String stringify(
                            final ClassOrInterfaceTypeDetails object) {
                        return object.getName().getSimpleTypeName();
                    }
                });

        return Collections
                .unmodifiableSet(new LinkedHashSet<ClassOrInterfaceTypeDetails>(
                        types));
    }

    public Set<ClassOrInterfaceTypeDetails> findClassesOrInterfaceDetailsWithTag(
            final Object tag) {
        Assert.notNull(tag, "Tag required");
        final Set<ClassOrInterfaceTypeDetails> types = new LinkedHashSet<ClassOrInterfaceTypeDetails>();
        processTypesWithTag(tag, new LocatedTypeCallback() {
            public void process(final ClassOrInterfaceTypeDetails located) {
                if (located != null) {
                    types.add(located);
                }
            }
        });
        return Collections.unmodifiableSet(types);
    }

    public Set<JavaType> findTypesWithAnnotation(
            final JavaType... annotationsToDetect) {
        return findTypesWithAnnotation(Arrays.asList(annotationsToDetect));
    }

    public Set<JavaType> findTypesWithAnnotation(
            final List<JavaType> annotationsToDetect) {
        Assert.notNull(annotationsToDetect, "Annotations to detect required");
        final Set<JavaType> types = new LinkedHashSet<JavaType>();
        processTypesWithAnnotation(annotationsToDetect,
                new LocatedTypeCallback() {
                    public void process(
                            final ClassOrInterfaceTypeDetails located) {
                        if (located != null) {
                            types.add(located.getName());
                        }
                    }
                });
        return Collections.unmodifiableSet(types);
    }

    public String getPhysicalTypeCanonicalPath(final JavaType javaType,
            final LogicalPath path) {
        return getPhysicalTypeCanonicalPath(PhysicalTypeIdentifier
                .createIdentifier(javaType, path));
    }

    public String getPhysicalTypeCanonicalPath(final String physicalTypeId) {
        final LogicalPath logicalPath = PhysicalTypeIdentifier
                .getPath(physicalTypeId);
        final JavaType javaType = PhysicalTypeIdentifier
                .getJavaType(physicalTypeId);
        final Pom pom = projectOperations.getPomFromModuleName(logicalPath
                .getModule());
        final String canonicalFilePath = pom.getPathLocation(logicalPath
                .getPath()) + javaType.getRelativeFileName();
        if (fileManager.exists(canonicalFilePath)) {
            typeCache.cacheTypeAgainstModule(pom, javaType);
            typeCache.cacheFilePathAgainstTypeIdentifier(canonicalFilePath,
                    physicalTypeId);
        }
        return canonicalFilePath;
    }

    public String getPhysicalTypeIdentifier(final JavaType type) {
        final PhysicalPath containingPhysicalPath = getPhysicalPath(type);
        if (containingPhysicalPath == null) {
            return null;
        }
        // N.B. as a side-effect, we make the currently focused module depend on
        // the given type's module
        final LogicalPath logicalPath = containingPhysicalPath.getLogicalPath();
        projectOperations.addModuleDependency(logicalPath.getModule());
        return PhysicalTypeIdentifier.createIdentifier(type, logicalPath);
    }

    public String getPhysicalTypeIdentifier(final String fileCanonicalPath) {
        Assert.hasText(fileCanonicalPath, "File canonical path required");
        if (!doesPathIndicateJavaType(fileCanonicalPath)) {
            return null;
        }
        String physicalTypeIdentifier = typeCache
                .getTypeIdFromTypeFilePath(fileCanonicalPath);
        if (physicalTypeIdentifier != null) {
            return physicalTypeIdentifier;
        }
        final String typeDirectory = FileUtils
                .getFirstDirectory(fileCanonicalPath);
        final String simpleTypeName = StringUtils.replaceFirst(
                fileCanonicalPath, typeDirectory + File.separator, "").replace(
                ".java", "");
        final JavaPackage javaPackage = typeResolutionService
                .getPackage(fileCanonicalPath);
        if (javaPackage == null) {
            return null;
        }
        final JavaType javaType = new JavaType(
                javaPackage.getFullyQualifiedPackageName() + "."
                        + simpleTypeName);
        final Pom module = projectOperations
                .getModuleForFileIdentifier(fileCanonicalPath);
        Assert.notNull(module, "The module for the file '" + fileCanonicalPath
                + "' could not be located");
        typeCache.cacheTypeAgainstModule(module, javaType);

        String reducedPath = fileCanonicalPath.replace(
                javaType.getRelativeFileName(), "");
        reducedPath = FileUtils.removeTrailingSeparator(reducedPath);

        for (final PhysicalPath physicalPath : module.getPhysicalPaths()) {
            if (physicalPath.getLocationPath().startsWith(reducedPath)) {
                final LogicalPath path = physicalPath.getLogicalPath();
                physicalTypeIdentifier = MetadataIdentificationUtils.create(
                        PhysicalTypeIdentifier.class.getName(), path.getName()
                                + "?" + javaType.getFullyQualifiedTypeName());
                break;
            }
        }
        typeCache.cacheFilePathAgainstTypeIdentifier(fileCanonicalPath,
                physicalTypeIdentifier);

        return physicalTypeIdentifier;
    }

    public List<String> getPotentialTopLevelPackagesForModule(final Pom module) {
        Assert.notNull(module, "Module required");

        final Map<String, Set<String>> packageMap = new HashMap<String, Set<String>>();
        final Set<String> moduleTypes = getTypesForModule(module.getPath());
        final List<String> topLevelPackages = new ArrayList<String>();
        if (moduleTypes.isEmpty()) {
            topLevelPackages.add(module.getGroupId());
            return topLevelPackages;
        }
        for (final String typeName : moduleTypes) {
            final StringBuilder sb = new StringBuilder();
            final String type = typeName
                    .substring(0, typeName.lastIndexOf('.'));
            final String[] typeSegments = type.split("\\.");
            final Set<String> discoveredPackages = new HashSet<String>();
            for (int i = 0; i < typeSegments.length; i++) {
                final String typeSegment = typeSegments[i];
                if (i > 0) {
                    sb.append(".");
                }
                sb.append(typeSegment);
                discoveredPackages.add(sb.toString());
            }

            for (final String discoveredPackage : discoveredPackages) {
                if (!packageMap.containsKey(discoveredPackage)) {
                    packageMap.put(discoveredPackage, new HashSet<String>());
                }
                packageMap.get(discoveredPackage).add(typeName);
            }
        }

        int longestPackage = 0;
        for (final Map.Entry<String, Set<String>> entry : packageMap.entrySet()) {
            if (entry.getValue().size() == moduleTypes.size()) {
                topLevelPackages.add(entry.getKey());
                if (entry.getKey().length() > longestPackage) {
                    longestPackage = entry.getKey().length();
                }
            }
        }
        return topLevelPackages;
    }

    public String getTopLevelPackageForModule(final Pom module) {
        Assert.notNull(module, "Module required");

        final Map<String, Set<String>> packageMap = new HashMap<String, Set<String>>();
        final Set<String> moduleTypes = getTypesForModule(module.getPath());
        if (moduleTypes.isEmpty()) {
            return module.getGroupId();
        }
        final Set<String> uniqueTypePackages = new HashSet<String>();
        for (final String typeName : moduleTypes) {
            final StringBuilder sb = new StringBuilder();
            final String typePackage = typeName.substring(0,
                    typeName.lastIndexOf('.'));
            uniqueTypePackages.add(typePackage);
            final String[] typeSegments = typePackage.split("\\.");
            final Set<String> discoveredPackages = new HashSet<String>();
            for (int i = 0; i < typeSegments.length; i++) {
                final String typeSegment = typeSegments[i];
                if (i > 0) {
                    sb.append(".");
                }
                sb.append(typeSegment);
                discoveredPackages.add(sb.toString());
            }

            for (final String discoveredPackage : discoveredPackages) {
                if (!packageMap.containsKey(discoveredPackage)) {
                    packageMap.put(discoveredPackage, new HashSet<String>());
                }
                packageMap.get(discoveredPackage).add(typeName);
            }
        }

        if (uniqueTypePackages.size() == 1) {
            return module.getGroupId();
        }

        final List<String> packageList = new ArrayList<String>(
                packageMap.keySet());
        Collections.sort(packageList, new Comparator<String>() {
            public int compare(final String s1, final String s2) {
                return Integer.valueOf(s1.length()).compareTo(s2.length());
            }
        });
        final Map<String, Set<String>> sortedPackageMap = new LinkedHashMap<String, Set<String>>();
        for (final String discoveredPackage : packageList) {
            sortedPackageMap.put(discoveredPackage,
                    packageMap.get(discoveredPackage));
        }

        int longestPackage = 0;
        String topLevelPackage = module.getGroupId();
        for (final Map.Entry<String, Set<String>> entry : sortedPackageMap
                .entrySet()) {
            if (entry.getValue().size() == moduleTypes.size()) {
                if (entry.getKey().length() > longestPackage) {
                    longestPackage = entry.getKey().length();
                    topLevelPackage = entry.getKey();
                }
            }
        }
        return topLevelPackage;
    }

    public ClassOrInterfaceTypeDetails getTypeDetails(final JavaType type) {
        return getTypeDetails(getPhysicalTypeIdentifier(type));
    }

    public ClassOrInterfaceTypeDetails getTypeDetails(
            final String physicalTypeId) {
        if (StringUtils.isBlank(physicalTypeId)) {
            return null;
        }
        Assert.isTrue(PhysicalTypeIdentifier.isValid(physicalTypeId),
                "Metadata id '" + physicalTypeId
                        + "' is not a valid physical type id");
        updateTypeCache();
        final ClassOrInterfaceTypeDetails cachedDetails = typeCache
                .getTypeDetails(physicalTypeId);
        if (cachedDetails != null) {
            return cachedDetails;
        }
        final PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService
                .get(physicalTypeId);
        if (physicalTypeMetadata == null) {
            return null;
        }
        return physicalTypeMetadata.getMemberHoldingTypeDetails();
    }

    public LogicalPath getTypePath(final JavaType javaType) {
        final String physicalTypeId = getPhysicalTypeIdentifier(javaType);
        if (StringUtils.isBlank(physicalTypeId)) {
            return null;
        }
        return PhysicalTypeIdentifier.getPath(physicalTypeId);
    }

    public Set<String> getTypesForModule(final String modulePath) {
        Assert.notNull(modulePath, "Module path required");
        return typeCache.getTypeNamesForModuleFilePath(modulePath);
    }

    public boolean hasTypeChanged(final String requestingClass,
            final JavaType javaType) {
        Assert.notNull(requestingClass, "Requesting class required");
        Assert.notNull(javaType, "Java type required");

        updateTypeCache();
        Set<String> changesSinceLastRequest = changeMap.get(requestingClass);
        if (changesSinceLastRequest == null) {
            changesSinceLastRequest = new LinkedHashSet<String>();
            for (final String typeIdentifier : typeCache
                    .getAllTypeIdentifiers()) {
                changesSinceLastRequest.add(typeCache
                        .getTypeDetails(typeIdentifier).getName()
                        .getFullyQualifiedTypeName());
            }
            changeMap.put(requestingClass, changesSinceLastRequest);
        }
        for (final String changedId : changesSinceLastRequest) {
            if (changedId.equals(javaType.getFullyQualifiedTypeName())) {
                changesSinceLastRequest.remove(changedId);
                return true;
            }
        }
        return false;
    }

    public boolean isInProject(final JavaType javaType) {
        return javaType != null && !javaType.isCoreType()
                && getPhysicalPath(javaType) != null;
    }

    public void processTypesWithAnnotation(
            final List<JavaType> annotationsToDetect,
            final LocatedTypeCallback callback) {
        Assert.notNull(annotationsToDetect, "Annotations to detect required");
        Assert.notNull(callback, "Callback required");
        // If the cache doesn't yet contain the annotation to be found it should
        // be added
        for (final JavaType annotationType : annotationsToDetect) {
            if (!annotationToMidMap.containsKey(annotationType)) {
                annotationToMidMap.put(annotationType, new HashSet<String>());
            }
        }

        // Before processing the call any changes to the project should be
        // processed and the cache updated accordingly
        updateTypeCache();

        for (final JavaType annotationType : annotationsToDetect) {
            for (final String locatedMid : annotationToMidMap
                    .get(annotationType)) {
                final ClassOrInterfaceTypeDetails located = typeCache
                        .getTypeDetails(locatedMid);
                callback.process(located);
            }
        }
    }

    // -------------------------- Private methods ------------------------------

    private void cacheType(final String fileCanonicalPath) {
        Assert.hasText(fileCanonicalPath, "File canonical path required");
        if (doesPathIndicateJavaType(fileCanonicalPath)) {
            final String id = getPhysicalTypeIdentifier(fileCanonicalPath);
            if (id != null && PhysicalTypeIdentifier.isValid(id)) {
                // Change to Java, so drop the cache
                final ClassOrInterfaceTypeDetails cid = lookupClassOrInterfaceTypeDetails(id);
                if (cid == null) {
                    if (!fileManager.exists(fileCanonicalPath)) {
                        typeCache.removeType(id);
                        final JavaType type = typeCache.getTypeDetails(id)
                                .getName();
                        updateChanges(type.getFullyQualifiedTypeName(), true);
                    }
                    return;
                }
                typeCache.cacheType(fileCanonicalPath, cid);
                updateAttributeCache(cid);
                updateChanges(cid.getName().getFullyQualifiedTypeName(), false);
            }
        }
    }

    private Set<String> discoverTypes() {
        // Retrieve a list of paths that have been discovered or modified since
        // the last invocation by this class
        for (final String change : fileMonitorService
                .getDirtyFiles(TypeLocationServiceImpl.class.getName())) {
            if (doesPathIndicateJavaType(change)) {
                discoveredTypes.add(change);
                dirtyFiles.add(change);
            }
        }
        return discoveredTypes;
    }

    private boolean doesPathIndicateJavaType(final String fileCanonicalPath) {
        Assert.hasText(fileCanonicalPath, "File canonical path required");
        return fileCanonicalPath.endsWith(".java")
                && !fileCanonicalPath.endsWith("package-info.java")
                && JavaSymbolName
                        .isLegalJavaName(getProposedJavaType(fileCanonicalPath));
    }

    private String getParentPath(final JavaType javaType) {
        final String relativePath = javaType.getRelativeFileName();
        for (final String typePath : discoverTypes()) {
            if (typePath.endsWith(relativePath)) {
                return StringUtils.removeSuffix(typePath, relativePath);
            }
        }
        return null;
    }

    private PhysicalPath getPhysicalPath(final JavaType javaType) {
        Assert.notNull(javaType, "Java type required");
        final String parentPath = getParentPath(javaType);
        if (parentPath == null) {
            return null;
        }
        for (final Pom pom : projectOperations.getPoms()) {
            for (final PhysicalPath physicalPath : pom.getPhysicalPaths()) {
                if (physicalPath.isSource()) {
                    final String pathLocation = FileUtils
                            .ensureTrailingSeparator(physicalPath
                                    .getLocationPath());
                    if (pathLocation.startsWith(parentPath)) {
                        typeCache.cacheTypeAgainstModule(pom, javaType);
                        return physicalPath;
                    }
                }
            }
        }
        return null;
    }

    private String getProposedJavaType(final String fileCanonicalPath) {
        Assert.hasText(fileCanonicalPath, "File canonical path required");
        // Determine the JavaType for this file
        String relativePath = "";
        final Pom moduleForFileIdentifier = projectOperations
                .getModuleForFileIdentifier(fileCanonicalPath);
        if (moduleForFileIdentifier == null) {
            return relativePath;
        }

        for (final PhysicalPath physicalPath : moduleForFileIdentifier
                .getPhysicalPaths()) {
            final String moduleCanonicalPath = FileUtils
                    .ensureTrailingSeparator(FileUtils
                            .getCanonicalPath(physicalPath.getLocation()));
            if (fileCanonicalPath.startsWith(moduleCanonicalPath)) {
                relativePath = File.separator
                        + StringUtils.replaceFirst(fileCanonicalPath,
                                moduleCanonicalPath, "");
                break;
            }
        }
        Assert.hasText(relativePath,
                "Could not determine compilation unit name for file '"
                        + fileCanonicalPath + "'");
        Assert.isTrue(relativePath.startsWith(File.separator),
                "Relative path unexpectedly dropped the '" + File.separator
                        + "' prefix (received '" + relativePath + "' from '"
                        + fileCanonicalPath + "'");
        relativePath = relativePath.substring(1);
        Assert.isTrue(relativePath.endsWith(".java"),
                "The relative path unexpectedly dropped the .java extension for file '"
                        + fileCanonicalPath + "'");
        relativePath = relativePath.substring(0,
                relativePath.lastIndexOf(".java"));
        return relativePath.replace(File.separatorChar, '.');
    }

    private void initTypeMap() {
        for (final Pom pom : projectOperations.getPoms()) {
            for (final PhysicalPath path : pom.getPhysicalPaths()) {
                if (path.isSource()) {
                    final String allJavaFiles = FileUtils
                            .ensureTrailingSeparator(path.getLocationPath())
                            + "**" + File.separatorChar + "*.java";
                    for (final FileDetails file : fileManager
                            .findMatchingAntPath(allJavaFiles)) {
                        cacheType(file.getCanonicalPath());
                    }
                }
            }
        }
    }

    /**
     * Obtains the a fresh copy of the {@link ClassOrInterfaceTypeDetails} for
     * the given physical type.
     * 
     * @param physicalTypeIdentifier to lookup (required)
     * @return the requested details (or <code>null</code> if unavailable)
     */
    private ClassOrInterfaceTypeDetails lookupClassOrInterfaceTypeDetails(
            final String physicalTypeIdentifier) {
        final PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService
                .evictAndGet(physicalTypeIdentifier);
        if (physicalTypeMetadata == null) {
            return null;
        }
        return physicalTypeMetadata.getMemberHoldingTypeDetails();
    }

    private void processTypesWithTag(final Object tag,
            final LocatedTypeCallback callback) {
        Assert.notNull(tag, "Tag required");
        Assert.notNull(callback, "Callback required");
        // If the cache doesn't yet contain the tag it should be added
        if (!tagToMidMap.containsKey(tag)) {
            tagToMidMap.put(tag, new HashSet<String>());
        }

        // Before processing the call any changes to the project should be
        // processed and the cache updated accordingly
        updateTypeCache();

        for (final String locatedMid : tagToMidMap.get(tag)) {
            final ClassOrInterfaceTypeDetails located = typeCache
                    .getTypeDetails(locatedMid);
            callback.process(located);
        }
    }

    private void updateAttributeCache(final MemberHoldingTypeDetails cid) {
        Assert.notNull(cid, "Member holding type details required");
        if (!typeAnnotationMap.containsKey(cid.getDeclaredByMetadataId())) {
            typeAnnotationMap.put(cid.getDeclaredByMetadataId(),
                    new HashSet<JavaType>());
        }
        if (!typeCustomDataMap.containsKey(cid.getDeclaredByMetadataId())) {
            typeCustomDataMap.put(cid.getDeclaredByMetadataId(),
                    new HashSet<Object>());
        }
        final Set<JavaType> previousAnnotations = typeAnnotationMap.get(cid
                .getDeclaredByMetadataId());
        for (final JavaType previousAnnotation : previousAnnotations) {
            final Set<String> midSet = annotationToMidMap
                    .get(previousAnnotation);
            if (midSet != null) {
                midSet.remove(cid.getDeclaredByMetadataId());
            }
        }
        previousAnnotations.clear();
        for (final AnnotationMetadata annotationMetadata : cid.getAnnotations()) {
            if (!annotationToMidMap.containsKey(annotationMetadata
                    .getAnnotationType())) {
                annotationToMidMap.put(annotationMetadata.getAnnotationType(),
                        new HashSet<String>());
            }
            previousAnnotations.add(annotationMetadata.getAnnotationType());
            annotationToMidMap.get(annotationMetadata.getAnnotationType()).add(
                    cid.getDeclaredByMetadataId());
        }
        final Set<Object> previousCustomDataSet = typeCustomDataMap.get(cid
                .getDeclaredByMetadataId());
        for (final Object previousCustomData : previousCustomDataSet) {
            final Set<String> midSet = tagToMidMap.get(previousCustomData);
            if (midSet != null) {
                midSet.remove(cid.getDeclaredByMetadataId());
            }
        }
        previousCustomDataSet.clear();
        for (final Object customData : cid.getCustomData().keySet()) {
            if (!tagToMidMap.containsKey(customData)) {
                tagToMidMap.put(customData, new HashSet<String>());
            }
            previousCustomDataSet.add(customData);
            tagToMidMap.get(customData).add(cid.getDeclaredByMetadataId());
        }
    }

    private void updateChanges(final String typeName, final boolean remove) {
        Assert.notNull(typeName, "Type name required");
        for (final String requestingClass : changeMap.keySet()) {
            if (remove) {
                changeMap.get(requestingClass).remove(typeName);
            }
            else {
                changeMap.get(requestingClass).add(typeName);
            }
        }
    }

    private void updateTypeCache() {
        if (typeCache.getAllTypeIdentifiers().isEmpty()) {
            initTypeMap();
        }
        discoverTypes();
        // Update the type cache
        for (final String change : dirtyFiles) {
            cacheType(change);
        }
        dirtyFiles.clear();
    }
}
