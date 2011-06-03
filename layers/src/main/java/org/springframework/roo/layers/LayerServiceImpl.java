package org.springframework.roo.layers;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 * 
 */
@Component(immediate=true)
@Service
@Reference(name = "layerProvider", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC, referenceInterface = LayerProvider.class, cardinality = ReferenceCardinality.MANDATORY_MULTIPLE) 
public class LayerServiceImpl implements LayerService {
	private Object mutex = this;
	private Set<LayerProvider> providers = new TreeSet<LayerProvider>(new PersistenceProviderComparator());
	
	public MemberTypeAdditions getPersistMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType, LayerType layerType) {
		for (LayerProvider provider : Collections.unmodifiableSet(providers)) {
			if (provider.getLayerType().getOrder() <= layerType.getOrder()) {
				continue;
			}
			MemberTypeAdditions additions = provider.getPersistMethod(declaredByMetadataId, entityVariableName, entityType, layerType);
			if (additions != null) {
				return additions;
			}
		}
		return null;
	}

	public MemberTypeAdditions getUpdateMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType, LayerType layerType) {
		for (LayerProvider provider : Collections.unmodifiableSet(providers)) {
			if (provider.getLayerType().getOrder() <= layerType.getOrder()) {
				continue;
			}
			MemberTypeAdditions additions = provider.getUpdateMethod(declaredByMetadataId, entityVariableName, entityType, layerType);
			if (additions != null) {
				return additions;
			}
		}
		return null;
	}
	
	public MemberTypeAdditions getDeleteMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType, LayerType layerType) {
		for (LayerProvider provider : Collections.unmodifiableSet(providers)) {
			if (provider.getLayerType().getOrder() <= layerType.getOrder()) {
				continue;
			}
			MemberTypeAdditions additions = provider.getDeleteMethod(declaredByMetadataId, entityVariableName, entityType, layerType);
			if (additions != null) {
				return additions;
			}
		}
		return null;
	}
	
	public MemberTypeAdditions getFindMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType, LayerType layerType) {
		for (LayerProvider provider : Collections.unmodifiableSet(providers)) {
			if (provider.getLayerType().getOrder() <= layerType.getOrder()) {
				continue;
			}
			MemberTypeAdditions additions = provider.getFindMethod(declaredByMetadataId, entityVariableName, entityType, layerType);
			if (additions != null) {
				return additions;
			}
		}
		return null;
	}

	public MemberTypeAdditions getFindAllMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType, LayerType layerType) {
		for (LayerProvider provider : Collections.unmodifiableSet(providers)) {
			if (provider.getLayerType().getOrder() <= layerType.getOrder()) {
				continue;
			}
			MemberTypeAdditions additions = provider.getFindAllMethod(declaredByMetadataId, entityVariableName, entityType, layerType);
			if (additions != null) {
				return additions;
			}
		}
		return null;
	}
	
	public Map<String, MemberTypeAdditions> getFinderMethods(String declaredByMetadataId, JavaType entityType, LayerType layerType, String ... finderNames) {
		// TODO: change this so multiple providers can contribute individual finders rather than all or nothing
		for (LayerProvider provider : Collections.unmodifiableSet(providers)) {
			if (provider.getLayerType().getOrder() <= layerType.getOrder()) {
				continue;
			}
			Map<String, MemberTypeAdditions> additions = provider.getFinderMethods(declaredByMetadataId, entityType, layerType, finderNames);
			if (additions != null) {
				return additions;
			}
		}
		return null;
	}
	
	public MemberTypeAdditions getFindEntriesMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType, LayerType layerType) {
		for (LayerProvider provider : Collections.unmodifiableSet(providers)) {
			if (provider.getLayerType().getOrder() <= layerType.getOrder()) {
				continue;
			}
			MemberTypeAdditions additions = provider.getFindEntriesMethod(declaredByMetadataId, entityVariableName, entityType, layerType);
			if (additions != null) {
				return additions;
			}
		}
		return null;
	}

	public MemberTypeAdditions getCountMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType, LayerType layerType) {
		for (LayerProvider provider : Collections.unmodifiableSet(providers)) {
			if (provider.getLayerType().getOrder() <= layerType.getOrder()) {
				continue;
			}
			MemberTypeAdditions additions = provider.getCountMethod(declaredByMetadataId, entityVariableName, entityType, layerType);
			if (additions != null) {
				return additions;
			}
		}
		return null;
	}

	public Map<CrudKey, MemberTypeAdditions> collectMemberTypeAdditions(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType, LayerType layerType) {
		Map<CrudKey, MemberTypeAdditions> additions = new HashMap<CrudKey, MemberTypeAdditions>();
		additions.put(CrudKey.PERSIST_METHOD, getPersistMethod(declaredByMetadataId, entityVariableName, entityType, layerType));
		additions.put(CrudKey.COUNT_METHOD, getCountMethod(declaredByMetadataId, entityVariableName, entityType, layerType));
		additions.put(CrudKey.DELETE_METHOD, getDeleteMethod(declaredByMetadataId, entityVariableName, entityType, layerType));
		additions.put(CrudKey.FIND_ALL_METHOD, getFindAllMethod(declaredByMetadataId, entityVariableName, entityType, layerType));
		additions.put(CrudKey.FIND_ENTRIES_METHOD, getFindEntriesMethod(declaredByMetadataId, entityVariableName, entityType, layerType));
		additions.put(CrudKey.FIND_METHOD, getFindMethod(declaredByMetadataId, entityVariableName, entityType, layerType));
		additions.put(CrudKey.UPDATE_METHOD, getUpdateMethod(declaredByMetadataId, entityVariableName, entityType, layerType));
		return Collections.unmodifiableMap(additions);
	}

	protected void bindLayerProvider(LayerProvider provider) {
		synchronized (mutex) {
			providers.add(provider);
		}
	}

	protected void unbindLayerProvider(LayerProvider provider) {
		synchronized (mutex) {
			if (providers.contains(provider)) {
				providers.remove(provider);
			}
		}
	}

	class PersistenceProviderComparator implements Comparator<LayerProvider>, Serializable {
		private static final long serialVersionUID = 1L;
		public int compare(LayerProvider provider1, LayerProvider provider2) {
			if (provider1.equals(provider2)) {
				return 0;
			}
			int difference = provider1.getLayerType().getOrder() - provider2.getLayerType().getOrder();
			if (difference == 0) {
				throw new IllegalStateException(provider1.getClass().getSimpleName() + " and " + provider2.getClass().getSimpleName() + " both have order = " + provider1.getLayerType().getOrder());
			}
			return difference;
		}
	}
}
