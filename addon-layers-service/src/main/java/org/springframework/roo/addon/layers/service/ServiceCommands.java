package org.springframework.roo.addon.layers.service;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
@Component
@Service
public class ServiceCommands implements CommandMarker {
	
	@Reference private ServiceOperations serviceOperations;
	
	@CliAvailabilityIndicator("service")
	public boolean isServiceCommandAvailable() {
		return serviceOperations.isServiceCommandAvailable();
	}
	
	@CliCommand(value = "service", help = "Adds @RooService annotation to target type") 
	public void service(@CliOption(key = "interface", mandatory = true, help = "The java interface to apply this annotation to") JavaType interfaceType,
			@CliOption(key = "class", mandatory = false, help = "Implementation class for the specified interface") JavaType classType,
			@CliOption(key = "domainType", unspecifiedDefaultValue = "*", optionContext = "update,project", mandatory = false, help = "The domain type this service should expose") JavaType domainType) {
		
		if (classType == null) {
			classType = new JavaType(interfaceType.getFullyQualifiedTypeName() + "Impl");
		}
		serviceOperations.setupService(interfaceType, classType, domainType);
	}

}
