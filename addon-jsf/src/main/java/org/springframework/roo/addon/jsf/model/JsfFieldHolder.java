package org.springframework.roo.addon.jsf.model;

import static org.springframework.roo.model.RooJavaType.ROO_UPLOADED_FILE;

import java.util.Map;

import org.springframework.roo.classpath.customdata.tagkeys.MethodMetadataCustomDataKey;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.CollectionUtils;

/**
 * Holder for JSF field parameters.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
public class JsfFieldHolder {

	// Fields
	private final FieldMetadata field;
	private final boolean enumerated;
	private final JavaType genericType;
	private final String genericTypePlural;
	private final String genericTypeBeanName;
	private final MemberDetails applicationTypeMemberDetails;
	private final Map<MethodMetadataCustomDataKey, MemberTypeAdditions> crudAdditions;

	public JsfFieldHolder(final FieldMetadata field, final boolean enumerated, final JavaType genericType, final String genericTypePlural, final String genericTypeBeanName, final MemberDetails applicationTypeMemberDetails, final Map<MethodMetadataCustomDataKey, MemberTypeAdditions> crudAdditions) {
		Assert.notNull(field, "Field required");
		this.field = field;
		this.enumerated = enumerated;
		this.genericType = genericType;
		this.genericTypePlural = genericTypePlural;
		this.genericTypeBeanName = genericTypeBeanName;
		this.applicationTypeMemberDetails = applicationTypeMemberDetails;
		this.crudAdditions = crudAdditions;
	}

	public FieldMetadata getField() {
		return field;
	}

	public boolean isEnumerated() {
		return enumerated;
	}

	public JavaType getGenericType() {
		return genericType;
	}

	public String getGenericTypePlural() {
		return genericTypePlural;
	}

	public String getGenericTypeBeanName() {
		return genericTypeBeanName;
	}

	public MemberDetails getApplicationTypeMemberDetails() {
		return applicationTypeMemberDetails;
	}

	public Map<MethodMetadataCustomDataKey, MemberTypeAdditions> getCrudAdditions() {
		return crudAdditions;
	}

	public boolean isGenericType() {
		return genericType != null && genericTypeBeanName != null;
	}
	
	public boolean isApplicationType() {
		return applicationTypeMemberDetails != null && !CollectionUtils.isEmpty(crudAdditions);
	}

	public boolean isUploadFileField() {
		return field.getAnnotation(ROO_UPLOADED_FILE) != null;
	}
}
