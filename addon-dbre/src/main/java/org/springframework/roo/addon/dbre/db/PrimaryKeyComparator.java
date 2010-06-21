package org.springframework.roo.addon.dbre.db;

import java.util.Comparator;

/**
 * {@link Comparator} for {@link PrimaryKey}.
 * 
 * <p>
 * Used to sort primary keys on the key sequence.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class PrimaryKeyComparator implements Comparator<PrimaryKey> {

	public int compare(PrimaryKey o1, PrimaryKey o2) {
		return o1.getKeySeq().compareTo(o2.getKeySeq());
	}
}
