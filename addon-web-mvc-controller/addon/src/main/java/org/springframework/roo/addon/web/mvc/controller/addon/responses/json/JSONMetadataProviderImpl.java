package org.springframework.roo.addon.web.mvc.controller.addon.responses.json;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.layers.service.addon.ServiceMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecoratorTracker;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Implementation of {@link JSONMetadataProvider}.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class JSONMetadataProviderImpl extends AbstractMemberDiscoveringItdMetadataProvider
    implements JSONMetadataProvider {

  protected final static Logger LOGGER = HandlerUtils.getLogger(JSONMetadataProviderImpl.class);

  private final Map<JavaType, String> domainTypeToServiceMidMap =
      new LinkedHashMap<JavaType, String>();

  protected MetadataDependencyRegistryTracker registryTracker = null;
  protected CustomDataKeyDecoratorTracker keyDecoratorTracker = null;

  private boolean readOnly;
  private JavaType entity;
  private JavaType service;
  private String path;
  private String metadataIdentificationString;

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Create and open the {@link CustomDataKeyDecoratorTracker}.</li>
   * <li>Registers {@link RooJavaType#ROO_JSON} as additional 
   * JavaType that will trigger metadata registration.</li>
   * <li>Set ensure the governor type details represent a class.</li>
   * </ul>
   */
  @Override
  @SuppressWarnings("unchecked")
  protected void activate(final ComponentContext cContext) {
    context = cContext.getBundleContext();
    super.setDependsOnGovernorBeingAClass(false);
    this.registryTracker =
        new MetadataDependencyRegistryTracker(context, this,
            PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
    this.registryTracker.open();

    addMetadataTrigger(RooJavaType.ROO_JSON);
  }

  /**
   * This service is being deactivated so unregister upstream-downstream 
   * dependencies, triggers, matchers and listeners.
   * 
   * @param context
   */
  protected void deactivate(final ComponentContext context) {
    MetadataDependencyRegistry registry = this.registryTracker.getService();
    registry.removeNotificationListener(this);
    registry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(),
        getProvidesType());
    this.registryTracker.close();

    removeMetadataTrigger(RooJavaType.ROO_JSON);

    CustomDataKeyDecorator keyDecorator = this.keyDecoratorTracker.getService();
    keyDecorator.unregisterMatchers(getClass());
    this.keyDecoratorTracker.close();
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return JSONMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = JSONMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = JSONMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "Response_Type_JSON";
  }

  @Override
  protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
    // Determine the governor for this ITD, and whether any metadata is even
    // hoping to hear about changes to that JavaType and its ITDs
    final JavaType governor = itdTypeDetails.getName();
    final String localMid = domainTypeToServiceMidMap.get(governor);
    if (localMid != null) {
      return localMid;
    }

    final MemberHoldingTypeDetails memberHoldingTypeDetails =
        getTypeLocationService().getTypeDetails(governor);
    if (memberHoldingTypeDetails != null) {
      for (final JavaType type : memberHoldingTypeDetails.getLayerEntities()) {
        final String localMidType = domainTypeToServiceMidMap.get(type);
        if (localMidType != null) {
          return localMidType;
        }
      }
    }
    return null;
  }

  @Override
  protected ItdTypeDetailsProvidingMetadataItem getMetadata(
      final String metadataIdentificationString, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {

    this.metadataIdentificationString = metadataIdentificationString;

    AnnotationMetadata controllerAnnotation =
        governorPhysicalTypeMetadata.getMemberHoldingTypeDetails().getAnnotation(
            RooJavaType.ROO_CONTROLLER);

    // Getting entity and check if is a readOnly entity or not
    this.entity = (JavaType) controllerAnnotation.getAttribute("entity").getValue();
    AnnotationMetadata entityAnnotation =
        getTypeLocationService().getTypeDetails(this.entity).getAnnotation(
            RooJavaType.ROO_JPA_ENTITY);

    Validate.notNull(entityAnnotation, "ERROR: Entity should be annotated with @RooJpaEntity");

    this.readOnly = false;
    if (entityAnnotation.getAttribute("readOnly") != null) {
      this.readOnly = (Boolean) entityAnnotation.getAttribute("readOnly").getValue();
    }

    // Getting service and its metadata
    this.service = (JavaType) controllerAnnotation.getAttribute("service").getValue();
    ClassOrInterfaceTypeDetails serviceDetails =
        getTypeLocationService().getTypeDetails(this.service);

    List<MethodMetadata> finders = new ArrayList<MethodMetadata>();
    final LogicalPath logicalPath =
        PhysicalTypeIdentifier.getPath(serviceDetails.getDeclaredByMetadataId());
    final String serviceMetadataKey =
        ServiceMetadata.createIdentifier(serviceDetails.getType(), logicalPath);
    final ServiceMetadata serviceMetadata =
        (ServiceMetadata) getMetadataService().get(serviceMetadataKey);

    // Getting path
    this.path = (String) controllerAnnotation.getAttribute("path").getValue();

    // Getting save method from related service
    MethodMetadata serviceSaveMethod = serviceMetadata.getSaveMethod();

    return new JSONMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata,
        getCreateMethod(serviceSaveMethod), this.readOnly);
  }

  /**
   * This method provides the "create" method  using JSON 
   * response type
   * 
   * @param serviceSaveMethod
   * 
   * @return MethodMetadata
   */
  private MethodMetadata getCreateMethod(MethodMetadata serviceSaveMethod) {

    // If provided entity is readOnly, create method is not
    // available
    if (this.readOnly) {
      return null;
    }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("create");


    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(this.entity, new AnnotationMetadataBuilder(
        SpringJavaType.REQUEST_BODY).build()));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(getEntityField().getFieldName());

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getRequestMappingAnnotation("POST", "", null, "", "application/json",
        "application/json", ""));

    // Adding @RequestBody annotation
    AnnotationMetadataBuilder requestBodyAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.RESPONSE_BODY);
    annotations.add(requestBodyAnnotation);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // return entityService.SAVE_METHOD(entityField);
    bodyBuilder.appendFormalLine(String.format("return %s.%s(%s);", getServiceField()
        .getFieldName(), serviceSaveMethod.getMethodName(), getEntityField().getFieldName()));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            this.entity, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method generates a valid @RequestMapping annotation with provided parameters
   * 
   * @param method
   * @param path
   * @param params
   * @param accept
   * @param consumes
   * @param produces
   * @param headers
   * 
   * @return
   */
  private AnnotationMetadataBuilder getRequestMappingAnnotation(String method, String path,
      List<String> params, String accept, String consumes, String produces, String headers) {

    List<AnnotationAttributeValue<?>> requestMappingAttributes =
        new ArrayList<AnnotationAttributeValue<?>>();

    // Adding method attribute. Force GET method if empty
    if (StringUtils.isNotBlank(method)) {
      requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"),
          new EnumDetails(SpringJavaType.REQUEST_METHOD, new JavaSymbolName(method))));
    } else {
      requestMappingAttributes.add(new EnumAttributeValue(new JavaSymbolName("method"),
          new EnumDetails(SpringJavaType.REQUEST_METHOD, new JavaSymbolName("GET"))));
    }

    // Adding path attribute
    if (StringUtils.isNotBlank(path)) {
      requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), path));
    }

    // TODO: Adding params attribute

    // Adding accept attribute
    if (StringUtils.isNotBlank(accept)) {
      requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("accept"), accept));
    }

    // Adding consumes attribute
    if (StringUtils.isNotBlank(consumes)) {
      requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("consumes"),
          consumes));
    }

    // Adding produces attribute
    if (StringUtils.isNotBlank(produces)) {
      requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("produces"),
          produces));
    }

    // Adding headers attribute
    if (StringUtils.isNotBlank(headers)) {
      requestMappingAttributes
          .add(new StringAttributeValue(new JavaSymbolName("headers"), headers));
    }

    return new AnnotationMetadataBuilder(SpringJavaType.REQUEST_MAPPING, requestMappingAttributes);
  }

  /**
   * This method returns entity field included on controller
   * 
   * @return
   */
  private FieldMetadata getEntityField() {

    // Generating service field name
    String fieldName =
        new JavaSymbolName(this.entity.getSimpleTypeName()).getSymbolNameUnCapitalisedFirstLetter();

    return new FieldMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC,
        new ArrayList<AnnotationMetadataBuilder>(), new JavaSymbolName(fieldName), this.service)
        .build();
  }

  /**
   * This method returns service field included on controller
   * 
   * @return
   */
  private FieldMetadata getServiceField() {

    // Generating service field name
    String fieldName =
        new JavaSymbolName(this.service.getSimpleTypeName())
            .getSymbolNameUnCapitalisedFirstLetter();

    return new FieldMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC,
        new ArrayList<AnnotationMetadataBuilder>(), new JavaSymbolName(fieldName), this.service)
        .build();
  }

  private void registerDependency(final String upstreamDependency, final String downStreamDependency) {

    if (getMetadataDependencyRegistry() != null
        && StringUtils.isNotBlank(upstreamDependency)
        && StringUtils.isNotBlank(downStreamDependency)
        && !upstreamDependency.equals(downStreamDependency)
        && !MetadataIdentificationUtils.getMetadataClass(downStreamDependency).equals(
            MetadataIdentificationUtils.getMetadataClass(upstreamDependency))) {
      getMetadataDependencyRegistry().registerDependency(upstreamDependency, downStreamDependency);
    }
  }

  public String getProvidesType() {
    return JSONMetadata.getMetadataIdentiferType();
  }
}
