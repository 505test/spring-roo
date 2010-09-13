package org.springframework.roo.classpath.details;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Builder for {@link FieldMetadata}.
 * 
 * @author Ben Alex
 * @since 1.1
 *
 */
public final class FieldMetadataBuilder extends AbstractIdentifiableAnnotatedJavaStructureBuilder<FieldMetadata> {

	private String fieldInitializer;
	private JavaSymbolName fieldName;
	private JavaType fieldType;
	
	public FieldMetadataBuilder(String declaredbyMetadataId) {
		super(declaredbyMetadataId);
	}
	
	public FieldMetadataBuilder(FieldMetadata existing) {
		super(existing);
		this.fieldInitializer = existing.getFieldInitializer();
		this.fieldName = existing.getFieldName();
		this.fieldType = existing.getFieldType();
	}
	
	public FieldMetadata build() {
		return new DefaultFieldMetadata(getCustomData().build(), getDeclaredByMetadataId(), getModifier(), buildAnnotations(), getFieldName(), getFieldType(), getFieldInitializer());
	}

	public String getFieldInitializer() {
		return fieldInitializer;
	}

	public void setFieldInitializer(String fieldInitializer) {
		this.fieldInitializer = fieldInitializer;
	}

	public JavaSymbolName getFieldName() {
		return fieldName;
	}

	public void setFieldName(JavaSymbolName fieldName) {
		this.fieldName = fieldName;
	}

	public JavaType getFieldType() {
		return fieldType;
	}

	public void setFieldType(JavaType fieldType) {
		this.fieldType = fieldType;
	}
	
}
