package org.springframework.roo.metadata.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataNotificationListener;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.metadata.MetadataTimingStatistic;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;

/**
 * Default implementation of {@link MetadataDependencyRegistry}.
 * 
 * <p>
 * This implementation is not thread safe. It should only be accessed by a single thread at a time.
 * This is enforced by the process manager semantics, so we avoid the cost of re-synchronization here.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopment
public final class DefaultMetadataDependencyRegistry implements MetadataDependencyRegistry {

	private static final Logger logger = HandlerUtils.getLogger(DefaultMetadataDependencyRegistry.class);
	private int trace = 0;
	private int level = 0;
	private int notificationNumber = 0;
	private long started;
	private String responsibleClass;
	private Map<String,Long> timings = new HashMap<String, Long>();
	
	/** key: upstream dependency; value: list<downstream dependencies> */
	private Map<String, Set<String>> upstreamKeyed = new HashMap<String, Set<String>>();
	
	/** key: downstream dependency; value: list<upstream dependencies> */
	private Map<String, Set<String>> downstreamKeyed = new HashMap<String, Set<String>>();
	
	private MetadataService metadataService;
	
	private Set<MetadataNotificationListener> listeners = new HashSet<MetadataNotificationListener>();
	
	public void registerDependency(String upstreamDependency, String downstreamDependency) {
		Assert.isTrue(isValidDependency(upstreamDependency, downstreamDependency), "Invalid dependency between upstream '" + upstreamDependency + "' and downstream '" + downstreamDependency + "'");
		
		// Maintain the upstream-keyed map
		Set<String> downstream = upstreamKeyed.get(upstreamDependency);
		if (downstream == null) {
			downstream = new HashSet<String>();
			upstreamKeyed.put(upstreamDependency, downstream);
		}
		downstream.add(downstreamDependency);
		
		// Maintain the downstream-keyed map
		Set<String> upstream = downstreamKeyed.get(downstreamDependency);
		if (upstream == null) {
			upstream = new HashSet<String>();
			downstreamKeyed.put(downstreamDependency, upstream);
		}
		upstream.add(upstreamDependency);
		
	}

	public void deregisterDependencies(String downstreamDependency) {
		Assert.isTrue(MetadataIdentificationUtils.isValid(downstreamDependency), "Downstream dependency is an invalid metadata identification string ('" + downstreamDependency + "')");
		
		// Acquire the keys to delete
		Set<String> upstream = downstreamKeyed.get(downstreamDependency);
		if (upstream == null) {
			return;
		}
		
		Set<String> upstreamToDelete = new HashSet<String>(upstream);
		
		// Delete them normally
		for (String deleteUpstream : upstreamToDelete) {
			deregisterDependency(deleteUpstream, downstreamDependency);
		}
	}

	public void deregisterDependency(String upstreamDependency, String downstreamDependency) {
		Assert.isTrue(MetadataIdentificationUtils.isValid(upstreamDependency), "Upstream dependency is an invalid metadata identification string ('" + upstreamDependency + "')");
		Assert.isTrue(MetadataIdentificationUtils.isValid(downstreamDependency), "Downstream dependency is an invalid metadata identification string ('" + downstreamDependency + "')");
		
		// Maintain the upstream-keyed map, if it even exists
		Set<String> downstream = upstreamKeyed.get(upstreamDependency);
		if (downstream != null) {
			downstream.remove(downstreamDependency);
		}
		
		// Maintain the downstream-keyed map, if it even exists
		Set<String> upstream = downstreamKeyed.get(downstreamDependency);
		if (upstream != null) {
			upstream.remove(upstreamDependency);
		}
	}

	public Set<String> getDownstream(String upstreamDependency) {
		Assert.isTrue(MetadataIdentificationUtils.isValid(upstreamDependency), "Upstream dependency is an invalid metadata identification string ('" + upstreamDependency + "')");
		
		Set<String> downstream = upstreamKeyed.get(upstreamDependency);
		if (downstream == null) {
			return new HashSet<String>();
		}
		
		return Collections.unmodifiableSet(new CopyOnWriteArraySet<String>(downstream));
	}
	
	public Set<String> getUpstream(String downstreamDependency) {
		Assert.isTrue(MetadataIdentificationUtils.isValid(downstreamDependency), "Downstream dependency is an invalid metadata identification string ('" + downstreamDependency + "')");
		
		Set<String> upstream = downstreamKeyed.get(downstreamDependency);
		if (upstream == null) {
			return new HashSet<String>();
		}
		
		return Collections.unmodifiableSet(upstream);
	}

	public boolean isValidDependency(String upstreamDependency, String downstreamDependency) {
		Assert.isTrue(MetadataIdentificationUtils.isValid(upstreamDependency), "Upstream dependency is an invalid metadata identification string ('" + upstreamDependency + "')");
		Assert.isTrue(MetadataIdentificationUtils.isValid(downstreamDependency), "Downstream dependency is an invalid metadata identification string ('" + downstreamDependency + "')");
		Assert.isTrue(!upstreamDependency.equals(downstreamDependency), "Upstream dependency cannot be the same as the downstream dependency ('" + upstreamDependency + "')");
		
		// The simplest possible outcome is the relationship already exists, so quickly return in that case
		Set<String> downstream = upstreamKeyed.get(upstreamDependency);
		if (downstream != null && downstream.contains(downstreamDependency)) {
			return true;
		}
		// Don't need the variable anymore, as we don't care about the other downstream dependencies
		downstream = null;
		
		// Need to walk the upstream dependency's parent dependency graph, verifying no presence of the proposed downstream dependency
		
		// Need to build a set representing every eventual upstream dependency of the indicated upstream dependency
		Set<String> allUpstreams = new HashSet<String>();
		buildSetOfAllUpstreamDependencies(allUpstreams, upstreamDependency);
		
		// The dependency is valid if none of the upstreams depend on the proposed downstream
		return !allUpstreams.contains(downstreamDependency);
	}
	
	private void buildSetOfAllUpstreamDependencies(Set<String> results, String downstreamDependency) {
		Set<String> upstreams = downstreamKeyed.get(downstreamDependency);
		if (upstreams == null) {
			return;
		}
		
		for (String upstream : upstreams) {
			results.add(upstream);
			buildSetOfAllUpstreamDependencies(results, upstream);
		}
	}

	public void addNotificationListener(MetadataNotificationListener listener) {
		Assert.notNull(listener, "Metadata notification listener required");
		
		if (listener instanceof MetadataService) {
			Assert.isTrue(metadataService == null, "Cannot register more than one MetadataListener");
			this.metadataService = (MetadataService) listener;
			return;
		}
		
		this.listeners.add(listener);
	}

	public void removeNotificationListener(MetadataNotificationListener listener) {
		Assert.notNull(listener, "Metadata notification listener required");
		
		if (listener instanceof MetadataService && listener.equals(this.metadataService)) {
			this.metadataService = null;
			return;
		}
		
		this.listeners.remove(listener);
	}

	private void log(int currentNotification, String message) {
		if (trace == 0) {
			return;
		}
		StringBuilder sb = new StringBuilder("00000000");
		String hex = Integer.toHexString(currentNotification);
		sb.replace(8-hex.length(), 8, hex);
		for (int i = 0; i < level; i++) {
			sb.append(" ");
		}
		sb.append(message);
		logger.fine(sb.toString());
	}

	private void stopCounting(long duration) {
		Long existing = timings.get(responsibleClass);
		if (existing == null) {
			existing = duration;
		} else {
			existing = existing + duration;
		}
		timings.put(responsibleClass, existing);
	}
	
	public void notifyDownstream(String upstreamDependency) {
		try {
			notificationNumber++;
			
			long now = System.currentTimeMillis();

			if (level > 0) {
				long duration = now - started;
				stopCounting(duration);
			}
			
			started = now;
			level++;

			int currentNotification = notificationNumber;
			
			if (metadataService != null) {
				// First dispatch the fine-grained, instance-specific dependencies.
				Set<String> notifiedDownstreams = new HashSet<String>();
				for (String downstream : getDownstream(upstreamDependency)) {
					if (trace > 0) {
						log(currentNotification, upstreamDependency + " -> " + downstream);
					}
					// No need to ensure upstreamDependency is different from downstream, as that's taken care of in the isValidDependency() method
					responsibleClass = MetadataIdentificationUtils.getMetadataClass(downstream);
					metadataService.notify(upstreamDependency, downstream);
					notifiedDownstreams.add(downstream);
				}
				
				// Next dispatch the coarse-grained, class-specific dependencies.
				// We only do it if the upstream is not class specific, as otherwise we'd have handled class-specific dispatch in previous loop 
				if (!MetadataIdentificationUtils.isIdentifyingClass(upstreamDependency)) {
					String asClass = MetadataIdentificationUtils.create(MetadataIdentificationUtils.getMetadataClass(upstreamDependency));
					for (String downstream : getDownstream(asClass)) {
						// We don't notify a downstream if it had a direct instance-specific dependency and was already notified in previous loop
						// We also don't notify if upstream is the same as downstream, as it doesn't make sense to notify yourself of an event
						// (such a condition is only possible if an instance registered to receive class-specific notifications and that instance
						// caused an event to fire)
						if (!notifiedDownstreams.contains(downstream) && !upstreamDependency.equals(downstream)) {
							if (trace > 0) {
								log(currentNotification, upstreamDependency + " -> " + downstream + " [via class]");
							}
							responsibleClass = MetadataIdentificationUtils.getMetadataClass(downstream);
							metadataService.notify(upstreamDependency, downstream);
						}
					}
				}
				
				notifiedDownstreams = null;
			}
			
			// Finally dispatch the general-purpose additional listeners
			for (MetadataNotificationListener listener : listeners) {
				if (trace > 1) {
					log(currentNotification, upstreamDependency + " -> " + upstreamDependency + " [" + listener.getClass().getSimpleName() + "]");
				}
				responsibleClass = listener.getClass().getName();
				listener.notify(upstreamDependency, null);
			}
		} finally {
			level--;
			
			if (level == 0) {
				long now = System.currentTimeMillis();
				long duration = now - started;
				stopCounting(duration);
				started = 0;
			}
		}
	}

	public void setTrace(int trace) {
		this.trace = trace;
	}

	public SortedSet<MetadataTimingStatistic> getTimings() {
		SortedSet<MetadataTimingStatistic> result = new TreeSet<MetadataTimingStatistic>();
		for (String key : timings.keySet()) {
			result.add(new StandardMetadataTimingStatistic(key, timings.get(key)));
		}
		return result;
	}

}
