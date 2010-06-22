package org.springframework.roo.addon.dbre;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.beaninfo.BeanInfoMetadataProvider;
import org.springframework.roo.addon.configurable.ConfigurableMetadataProvider;
import org.springframework.roo.addon.dbre.db.DbModel;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.addon.plural.PluralMetadataProvider;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdProviderRole;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;

/**
 * Provides {@link DbreMetadata}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Component
@Service
public class DbreMetadataProviderImpl extends AbstractItdMetadataProvider implements DbreMetadataProvider {
	//@Reference private EntityMetadataProvider entityMetadataProvider;
	@Reference private ConfigurableMetadataProvider configurableMetadataProvider;
	@Reference private PluralMetadataProvider pluralMetadataProvider;
	@Reference private BeanInfoMetadataProvider beanInfoMetadataProvider;
	@Reference private TableModelService tableModelService;
	@Reference private DbModel dbModel;

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		metadataDependencyRegistry.registerDependency(ProjectMetadata.getProjectIdentifier(), getProvidesType());
	//	entityMetadataProvider.addMetadataTrigger(new JavaType(RooDbManaged.class.getName()));
		configurableMetadataProvider.addMetadataTrigger(new JavaType(RooDbManaged.class.getName()));
		pluralMetadataProvider.addMetadataTrigger(new JavaType(RooDbManaged.class.getName()));
		addProviderRole(ItdProviderRole.ACCESSOR_MUTATOR);
		addMetadataTrigger(new JavaType(RooDbManaged.class.getName()));
	}

	protected void deactivate(ComponentContext context) {
	//	entityMetadataProvider.removeMetadataTrigger(new JavaType(RooDbManaged.class.getName()));
		configurableMetadataProvider.removeMetadataTrigger(new JavaType(RooDbManaged.class.getName()));
		pluralMetadataProvider.removeMetadataTrigger(new JavaType(RooDbManaged.class.getName()));
		beanInfoMetadataProvider.removeMetadataTrigger(new JavaType(RooDbManaged.class.getName()));
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return DbreMetadata.createIdentifier(javaType, path);
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = DbreMetadata.getJavaType(metadataIdentificationString);
		Path path = DbreMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		dbModel.deserialize();
		
		// We need to lookup the metadata we depend on
		JavaType javaType = governorPhysicalTypeMetadata.getPhysicalTypeDetails().getName();
		String entityMetadataKey = EntityMetadata.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
		EntityMetadata entityMetadata = (EntityMetadata) metadataService.get(entityMetadataKey);
 		
		// We need to abort if we couldn't find dependent metadata
		if (entityMetadata == null || !entityMetadata.isValid()) {
			return null;
		}

		return new DbreMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, entityMetadata, tableModelService, dbModel);
	}

	public String getItdUniquenessFilenameSuffix() {
		return "DbManaged";
	}

	public String getProvidesType() {
		return DbreMetadata.getMetadataIdentiferType();
	}
}
