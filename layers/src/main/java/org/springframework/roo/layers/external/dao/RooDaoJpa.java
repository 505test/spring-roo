package org.springframework.roo.layers.external.dao;

public @interface RooDaoJpa {
	
	public static final String REMOVE_METHOD = "remove";
	public static final String DOMAIN_TYPES = "domainTypes";
	
	Class<?>[] domainTypes();
	
	String removeMethod() default REMOVE_METHOD;

}
