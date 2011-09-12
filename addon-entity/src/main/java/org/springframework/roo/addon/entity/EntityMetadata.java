package org.springframework.roo.addon.entity;

import static org.springframework.roo.model.JavaType.INT_PRIMITIVE;
import static org.springframework.roo.model.SpringJavaType.PROPAGATION;
import static org.springframework.roo.model.SpringJavaType.TRANSACTIONAL;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Metadata for a type annotated with {@link RooEntity}.
 *  
 * @author Ben Alex
 * @author Stefan Schmidt
 * @author Alan Stewart
 * @since 1.0
 */
public class EntityMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
	
	// Constants
	private static final String ENTITY_MANAGER_METHOD_NAME = "entityManager";
	private static final String PROVIDES_TYPE_STRING = EntityMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	private static final JavaType COUNT_RETURN_TYPE = JavaType.LONG_PRIMITIVE;
	private static final JavaType ENTITY_MANAGER = new JavaType("javax.persistence.EntityManager");
	private static final JavaType PERSISTENCE_CONTEXT = new JavaType("javax.persistence.PersistenceContext");

	public static String getMetadataIdentifierType() {
		return PROVIDES_TYPE;
	}
	
	public static String createIdentifier(final JavaType javaType, final Path path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static JavaType getJavaType(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static Path getPath(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static boolean isValid(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	// Fields
	private boolean isDataNucleusEnabled;
	private boolean isGaeEnabled;
	private EntityMetadata parent;
	private FieldMetadata identifierField;
	private JpaCrudAnnotationValues crudAnnotationValues;
	private String entityName;
	private String plural;

	/**
	 * Constructor
	 *
	 * @param metadataId (required)
	 * @param aspectName (required)
	 * @param governorPhysicalTypeMetadata (required)
	 * @param parent can be <code>null</code>
	 * @param projectMetadata (required)
	 * @param crudAnnotationValues the CRUD-related annotation values (required)
	 * @param plural the plural form of the entity (required)
	 * @param identifierField the entity's identifier field (required)
	 * @param entityName the JPA entity name (required)
	 */
	public EntityMetadata(final String metadataId, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata, final EntityMetadata parent, final ProjectMetadata projectMetadata, final JpaCrudAnnotationValues crudAnnotationValues, final String plural, final FieldMetadata identifierField, final String entityName) {
		super(metadataId, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(metadataId), "Metadata identification string '" + metadataId + "' does not appear to be a valid");
		Assert.notNull(crudAnnotationValues, "CRUD-related annotation values required");
		Assert.notNull(identifierField, "Identifier required for '" + metadataId + "'");
		Assert.hasText(entityName, "Entity name required for '" + metadataId + "'");
		Assert.hasText(plural, "Plural required for '" + metadataId + "'");
		Assert.notNull(projectMetadata, "Project metadata required");
		
		if (!isValid()) {
			return;
		}
		
		this.crudAnnotationValues = crudAnnotationValues;
		this.entityName = entityName;
		this.identifierField = identifierField;
		this.isDataNucleusEnabled = projectMetadata.isDataNucleusEnabled();
		this.isGaeEnabled = projectMetadata.isGaeEnabled();
		this.parent = parent;
		this.plural = StringUtils.capitalize(plural);
		
		// Determine the entity's "entityManager" field, which is guaranteed to be accessible to the ITD.
		builder.addField(getEntityManagerField());
		
		// Add helper methods
		builder.addMethod(getPersistMethod());
		builder.addMethod(getRemoveMethod());
		builder.addMethod(getFlushMethod());
		builder.addMethod(getClearMethod());
		builder.addMethod(getMergeMethod());
		
		// Add static methods
		builder.addMethod(getEntityManagerMethod());
		builder.addMethod(getCountMethod());
		builder.addMethod(getFindAllMethod());
		builder.addMethod(getFindMethod());
		builder.addMethod(getFindEntriesMethod());
		
		builder.putCustomData(PersistenceCustomDataKeys.DYNAMIC_FINDER_NAMES, getDynamicFinders());

		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}

	/**
	 * Locates the entity manager field that should be used.
	 * 
	 * <p>
	 * If a parent is defined, it must provide the field.
	 * 
	 * <p>
	 * We generally expect the field to be named "entityManager" and be of type javax.persistence.EntityManager. We
	 * also require it to be public or protected, and annotated with @PersistenceContext. If there is an
	 * existing field which doesn't meet these latter requirements, we add an underscore prefix to the "entityManager" name
	 * and try again, until such time as we come up with a unique name that either meets the requirements or the
	 * name is not used and we will create it.
	 *  
	 * @return the entity manager field (never returns null)
	 */
	public FieldMetadata getEntityManagerField() {
		if (parent != null) {
			// The parent is required to guarantee this is available
			return parent.getEntityManagerField();
		}
		
		// Need to locate it ourself
		int index = -1;
		while (true) {
			// Compute the required field name
			index++;
			String fieldName = "";
			for (int i = 0; i < index; i++) {
				fieldName = fieldName + "_";
			}
			fieldName = fieldName + "entityManager";
			
			final JavaSymbolName fieldSymbolName = new JavaSymbolName(fieldName);
			final FieldMetadata candidate = MemberFindingUtils.getField(governorTypeDetails, fieldSymbolName);
			if (candidate != null) {
				// Verify if candidate is suitable
				
				if (!Modifier.isPublic(candidate.getModifier()) && !Modifier.isProtected(candidate.getModifier()) && (Modifier.TRANSIENT != candidate.getModifier())) {
					// Candidate is not public and not protected and not simply a transient field (in which case subclasses
					// will see the inherited field), so any subsequent subclasses won't be able to see it. Give up!
					continue;
				}
				
				if (!candidate.getFieldType().equals(ENTITY_MANAGER)) {
					// Candidate isn't an EntityManager, so give up
					continue;
				}
				
				if (MemberFindingUtils.getAnnotationOfType(candidate.getAnnotations(), PERSISTENCE_CONTEXT) == null) {
					// Candidate doesn't have a PersistenceContext annotation, so give up
					continue;
				}
				
				// If we got this far, we found a valid candidate
				return candidate;
			}
			
			// Candidate not found, so let's create one
			final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
			final AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(PERSISTENCE_CONTEXT);
			if (StringUtils.hasText(crudAnnotationValues.getPersistenceUnit())) {
				annotationBuilder.addStringAttribute("unitName", crudAnnotationValues.getPersistenceUnit());
			}
			annotations.add(annotationBuilder);
			
			final FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), Modifier.TRANSIENT, annotations, fieldSymbolName, ENTITY_MANAGER);
			return fieldBuilder.build();
		}
	}
	
	/**
	 * @return the persist method (may return null)
	 */
	private MethodMetadata getPersistMethod() {
		if (parent != null) {
			final MethodMetadata found = parent.getPersistMethod();
			if (found != null) {
				return found;
			}
		}
		if ("".equals(crudAnnotationValues.getPersistMethod())) {
			return null;
		}
		return getDelegateMethod(new JavaSymbolName(crudAnnotationValues.getPersistMethod()), "persist");
	}
	
	/**
	 * @return the remove method (may return null)
	 */
	private MethodMetadata getRemoveMethod() {
		if (parent != null) {
			final MethodMetadata found = parent.getRemoveMethod();
			if (found != null) {
				return found;
			}
		}
		if ("".equals(crudAnnotationValues.getRemoveMethod())) {
			return null;
		}
		return getDelegateMethod(new JavaSymbolName(crudAnnotationValues.getRemoveMethod()), "remove");
	}
	
	/**
	 * @return the flush method (never returns null)
	 */
	private MethodMetadata getFlushMethod() {
		if (parent != null) {
			final MethodMetadata found = parent.getFlushMethod();
			if (found != null) {
				return found;
			}
		}
		if ("".equals(crudAnnotationValues.getFlushMethod())) {
			return null;
		}
		return getDelegateMethod(new JavaSymbolName(crudAnnotationValues.getFlushMethod()), "flush");
	}
	
	/**
	 * @return the clear method (never returns null)
	 */
	private MethodMetadata getClearMethod() {
		if (parent != null) {
			final MethodMetadata found = parent.getClearMethod();
			if (found != null) {
				return found;
			}
		}
		if ("".equals(crudAnnotationValues.getClearMethod())) {
			return null;
		}
		return getDelegateMethod(new JavaSymbolName(crudAnnotationValues.getClearMethod()), "clear");
	}

	/**
	 * @return the merge method (never returns null)
	 */
	private MethodMetadata getMergeMethod() {
		if ("".equals(crudAnnotationValues.getMergeMethod())) {
			return null;
		}
		return getDelegateMethod(new JavaSymbolName(crudAnnotationValues.getMergeMethod()), "merge");
	}
	
	private MethodMetadata getDelegateMethod(final JavaSymbolName methodName, final String methodDelegateName) {
		// Method definition to find or build
		final JavaType[] parameterTypes = {};
		
		// Locate user-defined method
		final MethodMetadata userMethod = getGovernorMethod(methodName, parameterTypes);
		if (userMethod != null) {
			return userMethod; 
		}
		
		// Create the method
		final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>(); 

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		
		// Address non-injected entity manager field
		final MethodMetadata entityManagerMethod = getEntityManagerMethod();
		Assert.notNull(entityManagerMethod, "Entity manager method should not have returned null");
		
		// Use the getEntityManager() method to acquire an entity manager (the method will throw an exception if it cannot be acquired)
		final String entityManagerFieldName = getEntityManagerField().getFieldName().getSymbolName();
		bodyBuilder.appendFormalLine("if (this." + entityManagerFieldName + " == null) this." + entityManagerFieldName + " = " + entityManagerMethod.getMethodName().getSymbolName() + "();");
		
		JavaType returnType = JavaType.VOID_PRIMITIVE;
		if ("flush".equals(methodDelegateName)) {
			addTransactionalAnnotation(annotations);
			bodyBuilder.appendFormalLine("this." + entityManagerFieldName + ".flush();");
		} else if ("clear".equals(methodDelegateName)) {
			addTransactionalAnnotation(annotations);
			bodyBuilder.appendFormalLine("this." + entityManagerFieldName + ".clear();");
		} else if ("merge".equals(methodDelegateName)) {
			addTransactionalAnnotation(annotations);
			returnType = new JavaType(destination.getSimpleTypeName());
			bodyBuilder.appendFormalLine(destination.getSimpleTypeName() + " merged = this." + entityManagerFieldName + ".merge(this);");
			bodyBuilder.appendFormalLine("this." + entityManagerFieldName + ".flush();");
			bodyBuilder.appendFormalLine("return merged;");
		} else if ("remove".equals(methodDelegateName)) {
			addTransactionalAnnotation(annotations);
			bodyBuilder.appendFormalLine("if (this." + entityManagerFieldName + ".contains(this)) {");
			bodyBuilder.indent();
			bodyBuilder.appendFormalLine("this." + entityManagerFieldName + ".remove(this);");
			bodyBuilder.indentRemove();
			bodyBuilder.appendFormalLine("} else {");
			bodyBuilder.indent();
			bodyBuilder.appendFormalLine(destination.getSimpleTypeName() + " attached = " + destination.getSimpleTypeName() + "." + getFindMethod().getMethodName().getSymbolName() + "(this." + identifierField.getFieldName().getSymbolName() + ");");
			bodyBuilder.appendFormalLine("this." + entityManagerFieldName + ".remove(attached);");
			bodyBuilder.indentRemove();
			bodyBuilder.appendFormalLine("}");
		} else {
			// Persist
			addTransactionalAnnotation(annotations, true);
			bodyBuilder.appendFormalLine("this." + entityManagerFieldName + "." + methodDelegateName  + "(this);");
		}

		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), new ArrayList<JavaSymbolName>(), bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}
	
	private void addTransactionalAnnotation(final List<AnnotationMetadataBuilder> annotations, final boolean isPersistMethod) {
		final AnnotationMetadataBuilder transactionalBuilder = new AnnotationMetadataBuilder(TRANSACTIONAL);
		if (StringUtils.hasText(crudAnnotationValues.getTransactionManager())) {
			transactionalBuilder.addStringAttribute("value", crudAnnotationValues.getTransactionManager());
		}
		if (isGaeEnabled && isPersistMethod) {
			transactionalBuilder.addEnumAttribute("propagation", new EnumDetails(PROPAGATION, new JavaSymbolName("REQUIRES_NEW")));
		}
		annotations.add(transactionalBuilder);
	}
	
	private void addTransactionalAnnotation(final List<AnnotationMetadataBuilder> annotations) {
		addTransactionalAnnotation(annotations, false);
	}

	/**
	 * @return the static utility entityManager() method used by other methods to obtain
	 * entity manager and available as a utility for user code (never returns nulls)
	 */
	public MethodMetadata getEntityManagerMethod() {
		if (parent != null) {
			// The parent is required to guarantee this is available
			return parent.getEntityManagerMethod();
		}
		
		// Method definition to find or build
		final JavaSymbolName methodName = new JavaSymbolName(ENTITY_MANAGER_METHOD_NAME);
		final JavaType[] parameterTypes = {};
		final JavaType returnType = ENTITY_MANAGER;
		
		// Locate user-defined method
		final MethodMetadata userMethod = getGovernorMethod(methodName, parameterTypes);
		if (userMethod != null) {
			Assert.isTrue(userMethod.getReturnType().equals(returnType), "Method '" + methodName + "' on '" + destination + "' must return '" + returnType.getNameIncludingTypeParameters() + "'");
			return userMethod;
		}

		// Create method
		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

		if (Modifier.isAbstract(governorTypeDetails.getModifier())) {
			// Create an anonymous inner class that extends the abstract class (no-arg constructor is available as this is a JPA entity)
			bodyBuilder.appendFormalLine(ENTITY_MANAGER.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + " em = new " + destination.getSimpleTypeName() + "() {");
			// Handle any abstract methods in this class
			bodyBuilder.indent();
			for (final MethodMetadata method : MemberFindingUtils.getMethods(governorTypeDetails)) {
				if (Modifier.isAbstract(method.getModifier())) {
					final StringBuilder params = new StringBuilder();
					int i = -1;
					final List<AnnotatedJavaType> types = method.getParameterTypes();
					for (final JavaSymbolName name : method.getParameterNames()) {
						i++;
						if (i > 0) {
							params.append(", ");
						}
						final AnnotatedJavaType type = types.get(i);
						params.append(type.toString()).append(" ").append(name);
					}
					final int newModifier = method.getModifier() - Modifier.ABSTRACT;
					bodyBuilder.appendFormalLine(Modifier.toString(newModifier) + " " + method.getReturnType().getNameIncludingTypeParameters() + " " + method.getMethodName().getSymbolName() + "(" + params.toString() + ") {");
					bodyBuilder.indent();
					bodyBuilder.appendFormalLine("throw new UnsupportedOperationException();");
					bodyBuilder.indentRemove();
					bodyBuilder.appendFormalLine("}");
				}
			}
			bodyBuilder.indentRemove();
			bodyBuilder.appendFormalLine("}." + getEntityManagerField().getFieldName().getSymbolName() + ";");
		} else {
			// Instantiate using the no-argument constructor (we know this is available as the entity must comply with the JPA no-arg constructor requirement)
			bodyBuilder.appendFormalLine(ENTITY_MANAGER.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + " em = new " + destination.getSimpleTypeName() + "()." + getEntityManagerField().getFieldName().getSymbolName() + ";");
		}
		
		bodyBuilder.appendFormalLine("if (em == null) throw new IllegalStateException(\"Entity manager has not been injected (is the Spring Aspects JAR configured as an AJC/AJDT aspects library?)\");");
		bodyBuilder.appendFormalLine("return em;");
		final int modifier = Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL;
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), modifier, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}
	
	/**
	 * Finds (creating if necessary) the method that counts entities of this type
	 * 
	 * @return the count method (never null)
	 */
	private MethodMetadata getCountMethod() {
		// Method definition to find or build
		final JavaSymbolName methodName = new JavaSymbolName(crudAnnotationValues.getCountMethod() + plural);
		final JavaType[] parameterTypes = {};
		final List<JavaSymbolName> parameterNames = Collections.<JavaSymbolName> emptyList();
		
		// Locate user-defined method
		final MethodMetadata userMethod = getGovernorMethod(methodName, parameterTypes);
		if (userMethod != null) {
			Assert.isTrue(userMethod.getReturnType().equals(COUNT_RETURN_TYPE), "Method '" + methodName + "' on '" + destination + "' must return '" + COUNT_RETURN_TYPE.getNameIncludingTypeParameters() + "'");
			return userMethod;
		}
		
		// Create method
		final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		if (isGaeEnabled) {
			addTransactionalAnnotation(annotations);
		}
		
		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		if (isDataNucleusEnabled) {
			bodyBuilder.appendFormalLine("return ((Number) " + ENTITY_MANAGER_METHOD_NAME + "().createQuery(\"SELECT COUNT(o) FROM " + entityName + " o\").getSingleResult()).longValue();");
		} else {
			bodyBuilder.appendFormalLine("return " + ENTITY_MANAGER_METHOD_NAME + "().createQuery(\"SELECT COUNT(o) FROM " + entityName + " o\", Long.class).getSingleResult();");
		}
		final int modifier = Modifier.PUBLIC | Modifier.STATIC;
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), modifier, methodName, COUNT_RETURN_TYPE, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}
	
	/**
	 * @return the find all method (may return null)
	 */
	private MethodMetadata getFindAllMethod() {
		if ("".equals(crudAnnotationValues.getFindAllMethod())) {
			return null;
		}
		
		// Method definition to find or build
		final JavaSymbolName methodName = new JavaSymbolName(crudAnnotationValues.getFindAllMethod() + plural);
		final JavaType[] parameterTypes = {};
		final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
		final JavaType returnType = new JavaType("java.util.List", 0, DataType.TYPE, null, Arrays.asList(destination));
		
		// Locate user-defined method
		final MethodMetadata userMethod = getGovernorMethod(methodName, parameterTypes);
		if (userMethod != null) {
			Assert.isTrue(userMethod.getReturnType().equals(returnType), "Method '" + methodName + "' on '" + destination + "' must return '" + returnType.getNameIncludingTypeParameters() + "'");
			return userMethod;
		}
		
		// Create method
		final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		if (isDataNucleusEnabled) {
			addSuppressWarnings(annotations);
			bodyBuilder.appendFormalLine("return " + ENTITY_MANAGER_METHOD_NAME + "().createQuery(\"SELECT o FROM " + entityName + " o\").getResultList();");
		} else {
			bodyBuilder.appendFormalLine("return " + ENTITY_MANAGER_METHOD_NAME + "().createQuery(\"SELECT o FROM " + entityName + " o\", " + destination.getSimpleTypeName() + ".class).getResultList();");
		}
 		final int modifier = Modifier.PUBLIC | Modifier.STATIC;
		if (isGaeEnabled) {
			addTransactionalAnnotation(annotations);
		}
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), modifier, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	/**
	 * @return the find (by ID) method (may return null)
	 */
	public MethodMetadata getFindMethod() {
		if ("".equals(crudAnnotationValues.getFindMethod())) {
			return null;
		}
		
		// Method definition to find or build
		final String idFieldName = identifierField.getFieldName().getSymbolName();
		final JavaSymbolName methodName = new JavaSymbolName(crudAnnotationValues.getFindMethod() + destination.getSimpleTypeName());
		final JavaType parameterType = identifierField.getFieldType();
		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName(idFieldName));
		final JavaType returnType = destination;
		
		// Locate user-defined method
		final MethodMetadata userMethod = getGovernorMethod(methodName, parameterType);
		if (userMethod != null) {
			Assert.isTrue(userMethod.getReturnType().equals(returnType), "Method '" + methodName + "' on '" + returnType + "' must return '" + returnType.getNameIncludingTypeParameters() + "'");
			return userMethod;
		}
		
		// Create method
		final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		
		if (JavaType.STRING.equals(identifierField.getFieldType())) {
			bodyBuilder.appendFormalLine("if (" + idFieldName + " == null || " + idFieldName + ".length() == 0) return null;");
		} else if (!identifierField.getFieldType().isPrimitive()) {
			bodyBuilder.appendFormalLine("if (" + idFieldName + " == null) return null;");
		}
		
		if (isDataNucleusEnabled) {
			bodyBuilder.appendFormalLine("try {");
			bodyBuilder.indent();
			bodyBuilder.appendFormalLine("return (" + destination.getSimpleTypeName() + ") " + ENTITY_MANAGER_METHOD_NAME + "().createQuery(\"SELECT o FROM " + entityName + " o WHERE o." + idFieldName + " = :" + idFieldName + "\").setParameter(\"" + idFieldName + "\", " + idFieldName + ").getSingleResult();");
			bodyBuilder.indentRemove();
			// Catch the Spring exception thrown by JpaExceptionTranslatorAspect
			bodyBuilder.appendFormalLine("} catch (org.springframework.dao.EmptyResultDataAccessException e) {");
			bodyBuilder.indent();
			bodyBuilder.appendFormalLine("return null;");
			bodyBuilder.indentRemove();
			// ... and the original JPA exception in case the aspect doesn't trigger
			bodyBuilder.appendFormalLine("} catch (javax.persistence.NoResultException e) {");
			bodyBuilder.indent();
			bodyBuilder.appendFormalLine("return null;");
			bodyBuilder.indentRemove();
			bodyBuilder.appendFormalLine("}");
		} else {
			bodyBuilder.appendFormalLine("return " + ENTITY_MANAGER_METHOD_NAME + "().find(" + returnType.getSimpleTypeName() + ".class, " + idFieldName + ");");
		}
		
		if (isGaeEnabled) {
			addTransactionalAnnotation(annotations);
		}

		final int modifier = Modifier.PUBLIC | Modifier.STATIC;
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), modifier, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(parameterType), parameterNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	/**
	 * @return the find entries method (may return null)
	 */
	private MethodMetadata getFindEntriesMethod() {
		if ("".equals(crudAnnotationValues.getFindEntriesMethod())) {
			return null;
		}
		
		// Method definition to find or build
		final JavaSymbolName methodName = new JavaSymbolName(crudAnnotationValues.getFindEntriesMethod() + destination.getSimpleTypeName() + "Entries");
		final JavaType[] parameterTypes = {INT_PRIMITIVE, INT_PRIMITIVE};
		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("firstResult"), new JavaSymbolName("maxResults"));
		final JavaType returnType = new JavaType("java.util.List", 0, DataType.TYPE, null, Arrays.asList(destination));
		
		// Locate user-defined method
		final MethodMetadata userMethod = getGovernorMethod(methodName, parameterTypes);
		if (userMethod != null) {
			Assert.isTrue(userMethod.getReturnType().equals(returnType), "Method '" + methodName + "' on '" + destination + "' must return '" + returnType.getNameIncludingTypeParameters() + "'");
			return userMethod;
		}
		
		// Create method
		final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		if (isDataNucleusEnabled) {
			addSuppressWarnings(annotations);
			bodyBuilder.appendFormalLine("return " + ENTITY_MANAGER_METHOD_NAME + "().createQuery(\"SELECT o FROM " + entityName + " o\").setFirstResult(firstResult).setMaxResults(maxResults).getResultList();");
		} else {
			bodyBuilder.appendFormalLine("return " + ENTITY_MANAGER_METHOD_NAME + "().createQuery(\"SELECT o FROM " + entityName + " o\", " + destination.getSimpleTypeName() + ".class).setFirstResult(firstResult).setMaxResults(maxResults).getResultList();");
		}
 		final int modifier = Modifier.PUBLIC | Modifier.STATIC;
		if (isGaeEnabled) {
			addTransactionalAnnotation(annotations);
		}
		
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), modifier, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private void addSuppressWarnings(final List<AnnotationMetadataBuilder> annotations) {
		final List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new StringAttributeValue(new JavaSymbolName("value"), "unchecked"));
		annotations.add(new AnnotationMetadataBuilder(new JavaType("java.lang.SuppressWarnings"), attributes));
	}
	
	/**
	 * @return the dynamic, custom finders (never returns null, but may return an empty list)
	 */
	public List<String> getDynamicFinders() {
		if (crudAnnotationValues.getFinders() == null) {
			return Collections.emptyList();
		}
		return Arrays.asList(crudAnnotationValues.getFinders());
	}

	/**
	 * @return the pluralised name (never returns null or an empty string)
	 */
	public String getPlural() {
		return plural;
	}
	
	public String toString() {
		final ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("identifier", getId());
		tsc.append("valid", valid);
		tsc.append("aspectName", aspectName);
		tsc.append("destinationType", destination);
		tsc.append("finders", crudAnnotationValues.getFinders());
		tsc.append("governor", governorPhysicalTypeMetadata.getId());
		tsc.append("itdTypeDetails", itdTypeDetails);
		return tsc.toString();
	}

	/**
	 * Returns the JPA name of this entity.
	 * 
	 * @return a non-<code>null</code> name (might be empty)
	 */
	public String getEntityName() {
		return entityName;
	}
}
