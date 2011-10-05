package org.springframework.roo.addon.cloud.foundry.converter;

import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.cloud.foundry.CloudFoundrySession;
import org.springframework.roo.addon.cloud.foundry.model.CloudUri;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * Provides conversion to and from Cloud Foundry model classes.
 *
 * @author James Tyrrell
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component
@Service
public class CloudUriConverter implements Converter<CloudUri> {
	@Reference private CloudFoundrySession session;

	public CloudUri convertFromText(final String value, final Class<?> requiredType, final String optionContext) {
		if (value == null || "".equals(value)) {
			return null;
		}
		return new CloudUri(value);
	}

	public boolean supports(final Class<?> requiredType, final String optionContext) {
		return CloudUri.class.isAssignableFrom(requiredType);
	}

	public boolean getAllPossibleValues(final List<String> completions, final Class<?> requiredType, final String existingData, final String optionContext, final MethodTarget target) {
		final String appName = ConverterUtils.getOptionValue("appName", target.getRemainingBuffer());
		final List<String> uris = session.getBoundUrlMap().get(appName);
		if (uris != null) {
			completions.addAll(uris);
		}
		return false;
	}
}
