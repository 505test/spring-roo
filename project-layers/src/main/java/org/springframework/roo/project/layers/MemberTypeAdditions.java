package org.springframework.roo.project.layers;

import org.springframework.roo.classpath.details.AbstractMemberHoldingTypeDetailsBuilder;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * The required additions to a given type in order to invoke a given
 * application layer method, e.g. <code>findAll()</code>.
 * 
 * Instances are immutable.
 * 
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @since 1.2
 */
public class MemberTypeAdditions {
	
	// Fields
	private final ClassOrInterfaceTypeDetailsBuilder classOrInterfaceDetailsBuilder;
	private final String methodSignature;
	private final JavaSymbolName methodName;
	
	/**
	 * Constructor
	 *
	 * @param classOrInterfaceTypeDetailsBuilder (required)
	 * @param methodSignature the snippet of Java code that invokes the method
	 * @param methodName the name of the method
	 * in question, for example "<code>personService.findAll()</code>" (cannot
	 * be blank)
	 */
	public MemberTypeAdditions(ClassOrInterfaceTypeDetailsBuilder classOrInterfaceTypeDetailsBuilder, String methodSignature, JavaSymbolName methodName) {
		Assert.notNull(classOrInterfaceTypeDetailsBuilder, "Class or member details builder required");
		Assert.hasText(methodSignature, "Invalid method signature '" + methodSignature + "'");
		Assert.notNull(methodName, "Method name required");
		this.classOrInterfaceDetailsBuilder = classOrInterfaceTypeDetailsBuilder;
		this.methodSignature = methodSignature;
		this.methodName = methodName;
	}

	/**
	 * Returns the snippet of Java code that invokes the method in question,
	 * for example "<code>personService.findAll()</code>".
	 * 
	 * @return a non-blank String
	 */
	public String getMethodSignature() {
		return methodSignature;
	}
	
	/**
	 * Returns the method name;
	 * 
	 * @return the method name
	 */
	public JavaSymbolName getMethodName() {
		return methodName;
	}
	
	/**
	 * Copies this instance's additions into the given builder
	 * 
	 * @param targetBuilder the ITD builder to receive the additions (required)
	 */
	public void copyAdditionsTo(final AbstractMemberHoldingTypeDetailsBuilder<?> targetBuilder) {
		this.classOrInterfaceDetailsBuilder.copyTo(targetBuilder);
	}

	@Override
	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("memberHoldingTypeDetails", classOrInterfaceDetailsBuilder);
		tsc.append("methodSignature", methodSignature);
		return tsc.toString();
	}
}
