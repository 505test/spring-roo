package org.springframework.roo.project.packaging;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.support.util.Assert;

/**
 * The {@link PackagingProviderRegistry} implementation.
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component
@Service
@Reference(name = "packagingProvider", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC, referenceInterface = PackagingProvider.class, cardinality = ReferenceCardinality.MANDATORY_MULTIPLE)
public class PackagingProviderRegistryImpl implements PackagingProviderRegistry {

	// Fields
	private final Object mutex = new Object();
	// Using a map avoids each PackagingProvider having to implement equals() properly (when removing)
	private final Map<String, PackagingProvider> packagingProviders = new HashMap<String, PackagingProvider>();
	
	// ------------------------- OSGi callback methods -------------------------

	protected void bindPackagingProvider(final PackagingProvider packagingProvider) {
		synchronized (mutex) {
			final PackagingProvider previousPackagingProvider = packagingProviders.put(packagingProvider.getId(), packagingProvider);
			Assert.isNull(previousPackagingProvider, "More than one PackagingProvider with ID = '" + packagingProvider.getId() + "'");
		}
	}

	protected void unbindPackagingProvider(final PackagingProvider packagingProvider) {
		synchronized (mutex) {
			packagingProviders.remove(packagingProvider.getId());
		}
	}
	
	// ------------------ PackagingProviderRegistry methods --------------------
	
	public Collection<PackagingProvider> getAllPackagingProviders() {
		return new ArrayList<PackagingProvider>(packagingProviders.values());
	}

	public PackagingProvider getDefaultPackagingProvider() {
		PackagingProvider defaultCoreProvider = null;
		for (final PackagingProvider packagingProvider : packagingProviders.values()) {
			if (packagingProvider.isDefault()) {
				if (packagingProvider instanceof CorePackagingProvider) {
					defaultCoreProvider = packagingProvider;
				} else {
					return packagingProvider;
				}
			}
		}
		Assert.state(defaultCoreProvider != null, "Should have found a default core PackagingProvider");
		return defaultCoreProvider;
	}

	public PackagingProvider getPackagingProvider(String id) {
		for (final PackagingProvider packagingProvider : packagingProviders.values()) {
			if (packagingProvider.getId().equalsIgnoreCase(id)) {
				return packagingProvider;
			}
		}
		return null;
	}
}