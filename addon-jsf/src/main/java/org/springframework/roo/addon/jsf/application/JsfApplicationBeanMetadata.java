package org.springframework.roo.addon.jsf.application;

import static org.springframework.roo.addon.jsf.JsfJavaType.APPLICATION;
import static org.springframework.roo.addon.jsf.JsfJavaType.APPLICATION_SCOPED;
import static org.springframework.roo.addon.jsf.JsfJavaType.DISPLAY_CREATE_DIALOG;
import static org.springframework.roo.addon.jsf.JsfJavaType.DISPLAY_LIST;
import static org.springframework.roo.addon.jsf.JsfJavaType.EL_CONTEXT;
import static org.springframework.roo.addon.jsf.JsfJavaType.EXPRESSION_FACTORY;
import static org.springframework.roo.addon.jsf.JsfJavaType.FACES_CONTEXT;
import static org.springframework.roo.addon.jsf.JsfJavaType.MANAGED_BEAN;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_DEFAULT_MENU_MODEL;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_MENU_ITEM;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_MENU_MODEL;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_SUB_MENU;
import static org.springframework.roo.addon.jsf.JsfJavaType.REQUEST_SCOPED;
import static org.springframework.roo.addon.jsf.JsfJavaType.SESSION_SCOPED;
import static org.springframework.roo.addon.jsf.JsfJavaType.VIEW_SCOPED;
import static org.springframework.roo.model.JdkJavaType.POST_CONSTRUCT;
import static org.springframework.roo.model.RooJavaType.ROO_JSF_MANAGED_BEAN;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Set;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Metadata for {@link RooJsfApplicationBean}.
 *
 * @author Alan Stewart
 * @since 1.2.0
 */
public class JsfApplicationBeanMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

	// Constants
	private static final String PROVIDES_TYPE_STRING = JsfApplicationBeanMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	private static final JavaSymbolName MENU_MODEL = new JavaSymbolName("menuModel");
	private static final String CREATE_ICON = "ui-icon ui-icon-document";
	private static final String LIST_ICON = "ui-icon ui-icon-folder-open";

	// Fields
	private Set<ClassOrInterfaceTypeDetails> managedBeans;

	public JsfApplicationBeanMetadata(final String identifier, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata, final Set<ClassOrInterfaceTypeDetails> managedBeans, final String projectName) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		Assert.notNull(managedBeans, "Managed beans required");
		Assert.isTrue(StringUtils.hasText(projectName), "Project name required");

		if (!isValid()) {
			return;
		}

		if (managedBeans.isEmpty()) {
			valid = false;
			return;
		}
		
		this.managedBeans = managedBeans;

		// Add @ManagedBean annotation if required
		builder.addAnnotation(getManagedBeanAnnotation());

		// Add @SessionScoped annotation if required
		builder.addAnnotation(getScopeAnnotation());

		// Add menu model field
		builder.addField(getField(MENU_MODEL, PRIMEFACES_MENU_MODEL));

		// Add init() method
		builder.addMethod(getInitMethod());

		// Add model field accessor method
		builder.addMethod(getAccessorMethod(MENU_MODEL, PRIMEFACES_MENU_MODEL));

		// Add application name accessor method
		builder.addMethod(getMethod(Modifier.PUBLIC, new JavaSymbolName("getAppName"), JavaType.STRING, null, null, InvocableMemberBodyBuilder.getInstance().appendFormalLine("return \"" + StringUtils.capitalize(projectName) + "\";")));

		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}

	private AnnotationMetadata getManagedBeanAnnotation() {
		return getTypeAnnotation(MANAGED_BEAN);
	}

	private AnnotationMetadata getScopeAnnotation() {
		if (hasScopeAnnotation()) {
			return null;
		}
		return new AnnotationMetadataBuilder(REQUEST_SCOPED).build();
	}

	private boolean hasScopeAnnotation() {
		return (governorTypeDetails.getAnnotation(SESSION_SCOPED) != null
			|| governorTypeDetails.getAnnotation(VIEW_SCOPED) != null
			|| governorTypeDetails.getAnnotation(REQUEST_SCOPED) != null
			|| governorTypeDetails.getAnnotation(APPLICATION_SCOPED) != null);
	}

	private MethodMetadataBuilder getInitMethod() {
		JavaSymbolName methodName = new JavaSymbolName("init");
		if (governorHasMethod(methodName)) {
			return null;
		}

		builder.getImportRegistrationResolver().addImports(EL_CONTEXT, APPLICATION, EXPRESSION_FACTORY, FACES_CONTEXT, PRIMEFACES_MENU_ITEM, PRIMEFACES_SUB_MENU, PRIMEFACES_DEFAULT_MENU_MODEL);

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

		bodyBuilder.appendFormalLine("FacesContext facesContext = FacesContext.getCurrentInstance();");
		bodyBuilder.appendFormalLine("Application application = facesContext.getApplication();");
		bodyBuilder.appendFormalLine("ExpressionFactory expressionFactory = application.getExpressionFactory();");
		bodyBuilder.appendFormalLine("ELContext elContext = facesContext.getELContext();");
		bodyBuilder.appendFormalLine("");

		bodyBuilder.appendFormalLine("menuModel = new DefaultMenuModel();");
		bodyBuilder.appendFormalLine("Submenu submenu;");
		bodyBuilder.appendFormalLine("MenuItem item;");

		for (ClassOrInterfaceTypeDetails managedBean : managedBeans) {
			AnnotationMetadata annotation = MemberFindingUtils.getAnnotationOfType(managedBean.getAnnotations(), ROO_JSF_MANAGED_BEAN);
			if (annotation == null) {
				continue;
			}

			AnnotationAttributeValue<?> includeOnMenuAttributeValue = annotation.getAttribute(new JavaSymbolName("includeOnMenu"));
			if (includeOnMenuAttributeValue != null && !((Boolean) includeOnMenuAttributeValue.getValue()).booleanValue()) {
				continue;
			}

			AnnotationAttributeValue<?> entityAttributeValue = annotation.getAttribute(new JavaSymbolName("entity"));
			JavaType entity = (JavaType) entityAttributeValue.getValue();
			String entityLabel = entity.getSimpleTypeName().length() > 26 ? entity.getSimpleTypeName().substring(0, 23) + "..." : entity.getSimpleTypeName();

			AnnotationAttributeValue<?> beanNameAttributeValue = annotation.getAttribute(new JavaSymbolName("beanName"));
			String beanName = (String) beanNameAttributeValue.getValue();

			bodyBuilder.appendFormalLine("");
			bodyBuilder.appendFormalLine("submenu = new Submenu();");
			bodyBuilder.appendFormalLine("submenu.setId(\"" + StringUtils.uncapitalize(entity.getSimpleTypeName()) + "Submenu\");");
			bodyBuilder.appendFormalLine("submenu.setLabel(\"" + entityLabel + "\");");

			bodyBuilder.appendFormalLine("item = new MenuItem();");
			bodyBuilder.appendFormalLine("item.setId(\"create" + entity.getSimpleTypeName() + "MenuItem\");");
			bodyBuilder.appendFormalLine("item.setValueExpression(\"value\", expressionFactory.createValueExpression(elContext, \"#{messages.label_create}\", String.class));");
			bodyBuilder.appendFormalLine("item.setActionExpression(expressionFactory.createMethodExpression(elContext, \"#{" + beanName + "." + DISPLAY_CREATE_DIALOG + "}\", String.class, new Class[0]));");
			bodyBuilder.appendFormalLine("item.setIcon(\"" + CREATE_ICON + "\");");
			bodyBuilder.appendFormalLine("item.setAjax(false);");
			bodyBuilder.appendFormalLine("item.setAsync(false);");
			bodyBuilder.appendFormalLine("item.setUpdate(\"data\");");
			bodyBuilder.appendFormalLine("submenu.getChildren().add(item);");

			bodyBuilder.appendFormalLine("item = new MenuItem();");
			bodyBuilder.appendFormalLine("item.setId(\"list" + entity.getSimpleTypeName() + "MenuItem\");");
			bodyBuilder.appendFormalLine("item.setValueExpression(\"value\", expressionFactory.createValueExpression(elContext, \"#{messages.label_list}\", String.class));");
			bodyBuilder.appendFormalLine("item.setActionExpression(expressionFactory.createMethodExpression(elContext, \"#{" + beanName + "." + DISPLAY_LIST + "}\", String.class, new Class[0]));");
			bodyBuilder.appendFormalLine("item.setIcon(\"" + LIST_ICON + "\");");
			bodyBuilder.appendFormalLine("item.setAjax(false);");
			bodyBuilder.appendFormalLine("item.setAsync(false);");
			bodyBuilder.appendFormalLine("item.setUpdate(\"data\");");
			bodyBuilder.appendFormalLine("submenu.getChildren().add(item);");

			bodyBuilder.appendFormalLine("menuModel.addSubmenu(submenu);");
		}

		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		methodBuilder.addAnnotation(new AnnotationMetadataBuilder(POST_CONSTRUCT));
		return methodBuilder;
	}

	@Override
	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("identifier", getId());
		tsc.append("valid", valid);
		tsc.append("aspectName", aspectName);
		tsc.append("destinationType", destination);
		tsc.append("governor", governorPhysicalTypeMetadata.getId());
		tsc.append("itdTypeDetails", itdTypeDetails);
		return tsc.toString();
	}

	public static String getMetadataIdentiferType() {
		return PROVIDES_TYPE;
	}

	public static String createIdentifier(final JavaType javaType, final LogicalPath path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static JavaType getJavaType(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static LogicalPath getPath(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static boolean isValid(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}
}
