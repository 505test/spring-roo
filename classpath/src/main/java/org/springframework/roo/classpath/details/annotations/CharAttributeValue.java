package org.springframework.roo.classpath.details.annotations;

import org.springframework.roo.model.JavaSymbolName;

/**
 * Represents a char annotation attribute value.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class CharAttributeValue extends AbstractAnnotationAttributeValue<Character> {
	
	// Fields
	private final char value;
	
	/**
	 * Constructor
	 *
	 * @param name
	 * @param value
	 */
	public CharAttributeValue(JavaSymbolName name, char value) {
		super(name);
		this.value = value;
	}

	public Character getValue() {
		return value;
	}

	public String toString() {
		return getName() + " -> " + value;
	}
}