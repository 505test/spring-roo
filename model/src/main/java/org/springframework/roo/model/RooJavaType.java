package org.springframework.roo.model;

/**
 * Constants for Roo-specific {@link JavaType}s.
 * 
 * Use them in preference to creating new instances of these types.
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public final class RooJavaType {

	// Currently these are all annotations, but there's no reason why other
	// Roo-specific types couldn't be added here if necessary.
	
	// Literal class names are used to avoid dependencies upon the relevant addons.
	
	public static final JavaType ROO_CONFIGURABLE = new JavaType("org.springframework.roo.addon.configurable.RooConfigurable");
	public static final JavaType ROO_CONVERSION_SERVICE = new JavaType("org.springframework.roo.addon.web.mvc.controller.RooConversionService");
	public static final JavaType ROO_DATA_ON_DEMAND = new JavaType("org.springframework.roo.addon.dod.RooDataOnDemand");
	public static final JavaType ROO_DB_MANAGED = new JavaType("org.springframework.roo.addon.dbre.RooDbManaged");
	public static final JavaType ROO_EDITOR = new JavaType("org.springframework.roo.addon.property.editor.RooEditor");
	public static final JavaType ROO_ENTITY = new JavaType("org.springframework.roo.addon.entity.RooEntity");
	public static final JavaType ROO_GWT_MIRRORED_FROM = new JavaType("org.springframework.roo.addon.gwt.RooGwtMirroredFrom");
	public static final JavaType ROO_IDENTIFIER = new JavaType("org.springframework.roo.addon.entity.RooIdentifier");
	public static final JavaType ROO_INTEGRATION_TEST = new JavaType("org.springframework.roo.addon.test.RooIntegrationTest");
	public static final JavaType ROO_JAVA_BEAN = new JavaType("org.springframework.roo.addon.javabean.RooJavaBean");
	public static final JavaType ROO_JPA_ENTITY = new JavaType("org.springframework.roo.addon.entity.RooJpaEntity");
	public static final JavaType ROO_JSF_APPLICATION_BEAN = new JavaType("org.springframework.roo.addon.jsf.RooJsfApplicationBean");
	public static final JavaType ROO_JSF_MANAGED_BEAN = new JavaType("org.springframework.roo.addon.jsf.RooJsfManagedBean");
	public static final JavaType ROO_JSON = new JavaType("org.springframework.roo.addon.json.RooJson");
	public static final JavaType ROO_OP4J = new JavaType("org.springframework.roo.addon.op4j.RooOp4j");
	public static final JavaType ROO_PLURAL = new JavaType("org.springframework.roo.addon.plural.RooPlural");
	public static final JavaType ROO_REPOSITORY_JPA = new JavaType("org.springframework.roo.addon.layers.repository.RooRepositoryJpa");
	public static final JavaType ROO_SERIALIZABLE = new JavaType("org.springframework.roo.addon.serializable.RooSerializable");
	public static final JavaType ROO_SERVICE = new JavaType("org.springframework.roo.addon.layers.service.RooService");
	public static final JavaType ROO_SOLR_SEARCHABLE = new JavaType("org.springframework.roo.addon.solr.RooSolrSearchable");
	public static final JavaType ROO_SOLR_WEB_SEARCHABLE = new JavaType("org.springframework.roo.addon.solr.RooSolrWebSearchable");
	public static final JavaType ROO_TO_STRING = new JavaType("org.springframework.roo.addon.tostring.RooToString");
	public static final JavaType ROO_WEB_SCAFFOLD = new JavaType("org.springframework.roo.addon.web.mvc.controller.RooWebScaffold");
	
	/**
	 * Constructor is private to prevent instantiation
	 */
	private RooJavaType() {}
}