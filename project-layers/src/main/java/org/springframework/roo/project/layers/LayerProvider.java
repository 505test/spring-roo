package org.springframework.roo.project.layers;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Pair;

/**
 * Provides persistence-related methods at a given layer of the application.
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
public interface LayerProvider {
	
	/**
	 * The priority of the core layers.
	 */
	int CORE_LAYER_PRIORITY = 0;
	
	/**
	 * A layer provider should determine if it can provide {@link MemberTypeAdditions} for a given 
	 * target entity and construct it accordingly. If it can not provide the requested functionality
	 * it should simply return null;
	 * 
	 * @param metadataId Id of calling metadata provider
	 * @param methodIdentifier specifies the method which is being requested
	 * @param targetEntity specifies the target entity
	 * @param methodParameters parameters which are passed in to the method
	 * @return {@link MemberTypeAdditions} if a layer provider can offer this functionality, null otherwise
	 */
	MemberTypeAdditions getMemberTypeAdditions(String metadataId, String methodIdentifier, JavaType targetEntity, Pair<JavaType, JavaSymbolName>... methodParameters);
	
	/**
	 * Returns the position of this layer relative to others. 
	 * 
	 * @return a large number for higher-level layers, a smaller number for lower-level layers
	 */
	int getLayerPosition();
	
	/**
	 * Returns the priority of this layer relative to other implementations with the same
	 * position.
	 * 
	 * @return a value greater than {@link #CORE_LAYER_PRIORITY} in order to take
	 * precedence over the core {@link LayerProvider}s
	 * @see #getLayerPosition()
	 */
	int getPriority();
}
