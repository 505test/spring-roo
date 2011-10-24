package org.springframework.roo.classpath.converters;

import static org.springframework.roo.support.util.StringUtils.isBlank;
import static org.springframework.roo.support.util.StringUtils.removePrefix;
import static org.springframework.roo.support.util.StringUtils.removeSuffix;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.PomManagementService;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * A {@link Converter} for {@link JavaPackage}s, with support for using "~" to
 * denote the user's top-level package.
 *
 * @author Ben Alex
 * @since 1.0
 */
@Component
@Service
public class JavaPackageConverter implements Converter<JavaPackage> {

	/**
	 * The shell character that represents the current project or module's top
	 * level Java package.
	 * 
	 * TODO move (if appropriate) and reuse
	 */
	private static final String TOP_LEVEL_PACKAGE_SYMBOL = "~";
	
	// Fields
	@Reference FileManager fileManager;
	@Reference LastUsed lastUsed;
	@Reference PomManagementService pomManagementService;
	@Reference ProjectOperations projectOperations;
	@Reference TypeLocationService typeLocationService;

	public JavaPackage convertFromText(final String value, final Class<?> requiredType, final String optionContext) {
		if (isBlank(value)) {
			return null;
		}
		final JavaPackage result = new JavaPackage(convertToFullyQualifiedPackageName(value));
		if (optionContext != null && optionContext.contains("update")) {
			lastUsed.setPackage(result);
		}
		return result;
	}
	
	private String convertToFullyQualifiedPackageName(final String text) {
		final String normalisedText = removeSuffix(text, ".").toLowerCase();
		if (normalisedText.startsWith(TOP_LEVEL_PACKAGE_SYMBOL)) {
			return replaceTopLevelPackageSymbol(normalisedText);
		}
		return normalisedText;
	}
	
	/**
	 * Replaces the {@link #TOP_LEVEL_PACKAGE_SYMBOL} at the beginning of the
	 * given text with the current project/module's top-level package
	 * 
	 * @param text
	 * @return a well-formed Java package name (might have a trailing dot)
	 */
	private String replaceTopLevelPackageSymbol(final String text) {
		final String topLevelPackage = getTopLevelPackage();
		if (TOP_LEVEL_PACKAGE_SYMBOL.equals(text)) {
			return topLevelPackage;
		}
		final String textWithoutSymbol = removePrefix(text, TOP_LEVEL_PACKAGE_SYMBOL);
		return topLevelPackage + "." + removePrefix(textWithoutSymbol, ".");
	}
	
	private String getTopLevelPackage() {
		if (projectOperations.isFocusedProjectAvailable()) {
			return typeLocationService.getTopLevelPackageForModule(projectOperations.getFocusedModule());
		}
		return "";	// shouldn't happen if there's a project, i.e. most of the time
	}

	public boolean supports(final Class<?> requiredType, final String optionContext) {
		return JavaPackage.class.isAssignableFrom(requiredType);
	}

	public boolean getAllPossibleValues(final List<Completion> completions, final Class<?> requiredType, String existingData, final String optionContext, final MethodTarget target) {
		if (projectOperations.isFocusedProjectAvailable()) {
			completions.addAll(getCompletionsForAllKnownPackages());
		}
		return false;
	}

	private Collection<Completion> getCompletionsForAllKnownPackages() {
		final Collection<Completion> completions = new LinkedHashSet<Completion>();
		for (final Pom pom : pomManagementService.getPoms()) {
			for (final String type : typeLocationService.getTypesForModule(pom.getPath())) {
				completions.add(new Completion(type.substring(0, type.lastIndexOf('.'))));
			}
		}
		return completions;
	}
}