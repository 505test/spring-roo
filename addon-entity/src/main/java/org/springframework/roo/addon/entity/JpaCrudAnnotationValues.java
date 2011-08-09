package org.springframework.roo.addon.entity;

import static org.springframework.roo.addon.entity.RooEntity.CLEAR_METHOD_DEFAULT;
import static org.springframework.roo.addon.entity.RooEntity.COUNT_METHOD_DEFAULT;
import static org.springframework.roo.addon.entity.RooEntity.FIND_ALL_METHOD_DEFAULT;
import static org.springframework.roo.addon.entity.RooEntity.FIND_ENTRIES_METHOD_DEFAULT;
import static org.springframework.roo.addon.entity.RooEntity.FIND_METHOD_DEFAULT;
import static org.springframework.roo.addon.entity.RooEntity.FLUSH_METHOD_DEFAULT;
import static org.springframework.roo.addon.entity.RooEntity.MERGE_METHOD_DEFAULT;
import static org.springframework.roo.addon.entity.RooEntity.PERSIST_METHOD_DEFAULT;
import static org.springframework.roo.addon.entity.RooEntity.REMOVE_METHOD_DEFAULT;

import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.classpath.itd.MemberHoldingTypeDetailsMetadataItem;
import org.springframework.roo.support.util.StringUtils;

/**
 * The purely CRUD-related values of a parsed {@link RooEntity} annotation.
 * 
 * @author Andrew Swan
 * @since 1.2
 */
public class JpaCrudAnnotationValues extends AbstractAnnotationValues {

	// Fields (for each @RooEntity attribute)
	@AutoPopulate private String clearMethod = CLEAR_METHOD_DEFAULT;
	@AutoPopulate private String countMethod = COUNT_METHOD_DEFAULT;
	@AutoPopulate private String findAllMethod = FIND_ALL_METHOD_DEFAULT;
	@AutoPopulate private String findEntriesMethod = FIND_ENTRIES_METHOD_DEFAULT;
	@AutoPopulate private String findMethod = FIND_METHOD_DEFAULT;
	@AutoPopulate private String flushMethod = FLUSH_METHOD_DEFAULT;
	@AutoPopulate private String mergeMethod = MERGE_METHOD_DEFAULT;
	@AutoPopulate private String persistenceUnit = "";	
	@AutoPopulate private String persistMethod = PERSIST_METHOD_DEFAULT;
	@AutoPopulate private String removeMethod = REMOVE_METHOD_DEFAULT;
	@AutoPopulate private String transactionManager = "";

	@AutoPopulate private String[] finders;
	
	/**
	 * Constructor
	 *
	 * @param annotatedType
	 */
	public JpaCrudAnnotationValues(final MemberHoldingTypeDetailsMetadataItem<?> annotatedType) {
		super(annotatedType, RooEntity.class);
		AutoPopulationUtils.populate(this, annotationMetadata);
	}
	
	public String getClearMethod() {
		return StringUtils.hasText(clearMethod) ? clearMethod : CLEAR_METHOD_DEFAULT;
	}
	
	public String getCountMethod() {
		return StringUtils.hasText(countMethod) ? countMethod : COUNT_METHOD_DEFAULT;
	}

	public String getFindAllMethod() {
		return StringUtils.hasText(findAllMethod) ? findAllMethod : FIND_ALL_METHOD_DEFAULT;
	}
	
	/**
	 * Returns the prefix for the "find entries" method, e.g. the "find" part
	 * of "findFooEntries"
	 * 
	 * @return
	 */
	public String getFindEntriesMethod() {
		return findEntriesMethod;
	}
	
	/**
	 * Returns the custom finder names specified by the annotation
	 * 
	 * @return
	 */
	public String[] getFinders() {
		return finders;
	}
	
	public String getFindMethod() {
		return StringUtils.hasText(findMethod) ? findMethod : FIND_METHOD_DEFAULT;
	}
	
	public String getFlushMethod() {
		return StringUtils.hasText(flushMethod) ? flushMethod : FLUSH_METHOD_DEFAULT;
	}
	
	public String getMergeMethod() {
		return StringUtils.hasText(mergeMethod) ? mergeMethod : MERGE_METHOD_DEFAULT;
	}
	
	public String getPersistenceUnit() {
		return persistenceUnit;
	}

	public String getPersistMethod() {
		return StringUtils.hasText(persistMethod) ? persistMethod : PERSIST_METHOD_DEFAULT;
	}

	public String getRemoveMethod() {
		return StringUtils.hasText(removeMethod) ? removeMethod : REMOVE_METHOD_DEFAULT;
	}

	public String getTransactionManager() {
		return transactionManager;
	}
}
