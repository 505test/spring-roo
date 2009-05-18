package org.springframework.roo.shell;

import java.io.PrintWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.ExceptionUtils;
import org.springframework.roo.support.util.StringUtils;

public final class SimpleParser {
	private static final Logger logger = Logger.getLogger(SimpleParser.class.getName());

	private Set<Object> targets = new HashSet<Object>();
	private Set<Converter> converters = new HashSet<Converter>();
	private Map<String,MethodTarget> availabilityIndicators = new HashMap<String, MethodTarget>();
	
	public SimpleParser() {
		targets.add(this);
	}
	
	/**
	 * @param target an instance which is annotated with {@link CliCommand}, {@link CliOption} and {@link CliAvailabilityIndicator}
	 */
	public void addTarget(Object target) {
		Assert.notNull(target, "Target required");
		targets.add(target);
		
		for (Method m : target.getClass().getMethods()) {
			CliAvailabilityIndicator availability = m.getAnnotation(CliAvailabilityIndicator.class);
			if (availability != null) {
				Assert.isTrue(m.getParameterTypes().length == 0, "CliAvailabilityIndicator is only legal for 0 parameter methods (" + m.toGenericString() + ")");
				Assert.isTrue(m.getReturnType().equals(Boolean.TYPE), "CliAvailabilityIndicator is only legal for primitive boolean return types (" + m.toGenericString() + ")");
				for (String cmd : availability.value()) {
					Assert.isTrue(!availabilityIndicators.containsKey(cmd), "Cannot specify an availability indicator for '" + cmd + "' more than once");
					MethodTarget methodTarget = new MethodTarget();
					methodTarget.method = m;
					methodTarget.target = target;
					availabilityIndicators.put(cmd, methodTarget);
				}
			}
		}
		
	}
	
	public void removeTarget(Object target) {
		Assert.notNull(target, "Target required");
		targets.remove(target);
	}

	public void addConverter(Converter converter) {
		Assert.notNull(converter, "Converter required");
		converters.add(converter);
	}
	
	public ParseResult parse(String buffer, PrintWriter writer) {
		Assert.notNull(buffer, "Buffer required");
		Assert.notNull(writer, "PrintWriter required");
		
		// Locate the applicable targets which match this buffer
		Set<MethodTarget> matchingTargets = locateTargets(buffer, true);
		if (matchingTargets.size() == 0) {
			logger.warning("Command '" + buffer + "' not found (for assistance press TAB or type \"hint\" then hit ENTER)");
			return null;
		}
		if (matchingTargets.size() > 1) {
			logger.warning("Ambigious command '" + buffer + "' (for assistance press TAB or type \"hint\" then hit ENTER)");
			return null;
		}
		MethodTarget methodTarget = matchingTargets.iterator().next(); 

		// Argument conversion time
		Annotation[][] parameterAnnotations = methodTarget.method.getParameterAnnotations();
		if (parameterAnnotations.length == 0) {
			// No args
			return new ParseResult(methodTarget.method, methodTarget.target, null);
		}
		
		// Oh well, we need to convert some arguments
		List<Object> arguments = new ArrayList<Object>(methodTarget.method.getParameterTypes().length);

		// Attempt to parse
		Map<String,String> options= null;
		try {
			options = ParserUtils.tokenize(methodTarget.remainingBuffer);
		} catch (IllegalArgumentException ex) {
			logger.warning(ExceptionUtils.extractRootCause(ex).getMessage());
			return null;
		}
		
		for (Annotation[] annotations : parameterAnnotations) {
			CliOption cliOption = null;
			
			for (Annotation a : annotations) {
				if (a instanceof CliOption) {
					cliOption = (CliOption) a;
				}
			}
			Assert.notNull(cliOption, "CliOption not found for parameter '" + annotations + "'");
			
			Class<?> requiredType = methodTarget.method.getParameterTypes()[arguments.size()];

			if (cliOption.systemProvided()) {
				Object result;
				if (Writer.class.isAssignableFrom(requiredType)) {
					result = writer;
				} else if (SimpleParser.class.isAssignableFrom(requiredType)) {
					result = this;
				} else {
					logger.warning("Parameter type '" + requiredType + "' is not system provided");
					return null;
				}
				arguments.add(result);
				continue;
			}
			
			// Obtain the value the user specified, taking care to ensure they only specified it via a single alias
			String value = null;
			String sourcedFrom =  null;
			for (String possibleKey : cliOption.key()) {
				if (options.containsKey(possibleKey)) {
					if (sourcedFrom != null) {
						logger.warning("You cannot specify option '" + possibleKey + "' when you have also specified '" + sourcedFrom + "' in the same command");
						return null;
					}
					sourcedFrom = possibleKey;
					value = options.get(possibleKey);
				}
			}
			
			// Ensure the user specified a value if the value is mandatory
			if ((value == null || "".equals(value.trim())) && cliOption.mandatory()) {
				if ("".equals(cliOption.key()[0])) {
					StringBuilder message = new StringBuilder("You must specify a default option ");
					if (cliOption.key().length > 1) {
						message.append("(otherwise known as option '" + cliOption.key()[1] + "') ");
					}
					message.append("for this command");
					logger.warning(message.toString());
				} else {
					logger.warning("You must specify option '" + cliOption.key()[0] + "' for this command");
				}
				return null;
			}
			
			// Accept a default if the user specified the option, but didn't provide a value
			if ("".equals(value)) {
				value = cliOption.specifiedDefaultValue();
			}
			
			// Accept a default if the user didn't specify the option at all
			if (value == null) {
				value = cliOption.unspecifiedDefaultValue();
			}

			// Special token that denotes a null value is sought (useful for default values)
			if ("__NULL__".equals(value)) {
				if (requiredType.isPrimitive()) {
					logger.warning("Nulls cannot be presented to primitive type " + requiredType.getSimpleName() + " for option '" + StringUtils.arrayToCommaDelimitedString(cliOption.key()) + "'");
					return null;
				} else {
					arguments.add(null);
				}
				continue;
			}

			// Now we're ready to perform a conversion
			try {
				CliOptionContext.setOptionContext(cliOption.optionContext());
				CliSimpleParserContext.setSimpleParserContext(this);
				Object result;
				Converter c = null;
				for (Converter candidate : converters) {
					if (candidate.supports(requiredType, cliOption.optionContext())) {
						// found a usable converter
						c = candidate;
						break;
					}
				}
				if (c == null) {
					// fallback to a normal SimpleTypeConverter and attempt conversion
					// TODO: add simple type conversion
					throw new IllegalStateException("TODO: Add basic type conversion");
//					SimpleTypeConverter simpleTypeConverter = new SimpleTypeConverter();
	//				result = simpleTypeConverter.convertIfNecessary(value, requiredType, mp);
				} else {
					// use the converter
					result = c.convertFromText(value, requiredType, cliOption.optionContext());
				}
				arguments.add(result);
			} catch (RuntimeException ex) {
				logger.warning("Failed to convert '" + value + "' to type " + requiredType.getSimpleName() + " for option '" + StringUtils.arrayToCommaDelimitedString(cliOption.key()) + "'");
				return null;
			} finally {
				CliOptionContext.resetOptionContext();
				CliSimpleParserContext.resetSimpleParserContext();
			}
			
		}
		
		return new ParseResult(methodTarget.method, methodTarget.target, arguments.toArray());
	}
	
	private Set<MethodTarget> locateTargets(String buffer, boolean strictMatching) {
		Assert.notNull(buffer, "Buffer required");
		Set<MethodTarget> result = new HashSet<MethodTarget>();
		// The reflection could certainly be optimised, but it's good enough for now (and cached reflection
		// is unlikely to be noticeable to a human being using the CLI)
		for (Object o : targets) {
			Method[] methods = o.getClass().getMethods();
			for (Method m : methods) {
				CliCommand cmd = m.getAnnotation(CliCommand.class);
				if (cmd != null) {
					// We have a @CliCommand.

					// Decide if this @CliCommand is available at this moment
					Boolean available = null;
					for (String value : cmd.value()) {
						MethodTarget mt = this.availabilityIndicators.get(value);
						if (mt != null) {
							Assert.isNull(available, "More than one availability indicator is defined for '" + m.toGenericString() + "'");
							try {
								available = (Boolean) mt.method.invoke(mt.target, new Object[] {});
								// We should "break" here, but we loop over all to ensure no conflicting availability indicators are defined
							} catch (Exception e) {
								available = false;
							}
						}
					}
					
					// Skip this @CliCommand if it's not available
					if (available != null && !available) {
						continue;
					}
					
					for (String value : cmd.value()) {
						
						String remainingBuffer = isMatch(buffer, value, strictMatching);
						if (remainingBuffer != null) {
							MethodTarget mt = new MethodTarget();
							mt.method = m;
							mt.target = o;
							mt.remainingBuffer = remainingBuffer;
							mt.key = value;
							result.add(mt);
						}
					}
				}
			}
		}
		return result;
	}
	
	static String isMatch(String buffer, String command, boolean strictMatching) {
		if ("".equals(buffer.trim())) {
			return "";
		}
		String[] commandWords = StringUtils.delimitedListToStringArray(command, " ");
		int lastCommandWordUsed = 0;
		Assert.notEmpty(commandWords, "Command required");
		
		String bufferToReturn = null;
		String lastWord = null;
		
		next_buffer_loop:
		for (int bufferIndex = 0; bufferIndex < buffer.length() ; bufferIndex++) {
			String bufferSoFarIncludingThis = buffer.substring(0, bufferIndex+1);
			String bufferRemaining = buffer.substring(bufferIndex+1);
			
			int bufferLastIndexOfWord = bufferSoFarIncludingThis.lastIndexOf(" ");
			String wordSoFarIncludingThis = bufferSoFarIncludingThis;
			if (bufferLastIndexOfWord != -1) {
				wordSoFarIncludingThis = bufferSoFarIncludingThis.substring(bufferLastIndexOfWord);
			}
			
			if (wordSoFarIncludingThis.equals(" ") || bufferIndex == buffer.length()-1) {
				
				if (bufferIndex == buffer.length()-1 && !"".equals(wordSoFarIncludingThis.trim())) {
					lastWord = wordSoFarIncludingThis.trim();
				}
				
				// At end of word or buffer. Let's see if a word matched or not
				
				for (int candidate = lastCommandWordUsed; candidate < commandWords.length; candidate++) {
					if (lastWord != null && lastWord.length() > 0 && commandWords[candidate].startsWith(lastWord)) {
						
						if (bufferToReturn != null) {
							// We already matched something earlier, so ensure we didn't skip any word
							if (candidate != lastCommandWordUsed + 1) {
								// User has skipped a word
								bufferToReturn = null;
								break next_buffer_loop;
							}
						}
						
						bufferToReturn = bufferRemaining;
						lastCommandWordUsed = candidate;
						if (candidate+1 == commandWords.length) {
							// This was a match for the final word in the command, so abort
							break next_buffer_loop;
						}
						// There are more words left to potentially match, so continue
						continue next_buffer_loop;
					}
				}
				
				// This word is unrecognised as part of a command, so abort
				bufferToReturn = null;
				break next_buffer_loop;
			}
			
			lastWord = wordSoFarIncludingThis.trim();
		}
		
		// We only consider it a match if ALL words were actually used
		if (bufferToReturn != null) {
			if (!strictMatching || lastCommandWordUsed+1 == commandWords.length) {
				return bufferToReturn;
			}
		}
		
		return null; // not a match
	}
	
	public int complete(String buffer, int cursor, List<String> candidates) {
		Assert.notNull(buffer, "Buffer required");
		Assert.notNull(candidates, "Candidates list required");
		
		// Begin by only including the portion of the buffer represented to the present cursor position
		String translated = buffer.substring(0, cursor);
		
		// Start by locating a method that matches
		Set<MethodTarget> targets = locateTargets(translated, false);
		SortedSet<String> results = new TreeSet<String>();

//		logger.info("RESULTS: '" + translated + "' " + StringUtils.collectionToCommaDelimitedString(targets));
		
		if (targets.size() == 0) {
			// Nothing matches the buffer they've presented
			return cursor;
		}
		if (targets.size() > 1) {
			// Assist them locate a particular target
			for (MethodTarget target : targets) {
				// Only add the first word of each target, if they've typed nothing on the CLI so far
				if ("".equals(translated) && target.key.contains(" ")) {
					results.add(target.key.substring(0, target.key.indexOf(" ")));
					
				} else {
					// Only add the commands which start with whatever they've typed.
					// This is needed so they don't get overwhelmed by too many options appearing
					if (target.key.startsWith(translated)) {
						results.add(target.key);
					}
				}
			}
			candidates.addAll(results);
			return 0;
		}
		
		// There is a single target of this method, so provide completion services for it
		MethodTarget methodTarget = targets.iterator().next();
		
		// Identify the command we're working with
		CliCommand cmd = methodTarget.method.getAnnotation(CliCommand.class);
		Assert.notNull(cmd, "CliCommand unavailable for '" + methodTarget.method.toGenericString() + "'");

		// Make a reasonable attempt at parsing the remainingBuffer
		Map<String,String> options= null;
		try {
			options = ParserUtils.tokenize(methodTarget.remainingBuffer);
		} catch (IllegalArgumentException ex) {
			// Assume any IllegalArgumentException is due to a quotation mark mismatch
			candidates.add(translated + "\"");
			return 0;
		}

		// Lookup arguments for this target
		Annotation[][] parameterAnnotations = methodTarget.method.getParameterAnnotations();

		// If there aren't any parameters for the method, at least ensure they have typed the command properly
		if (parameterAnnotations.length == 0) {
			for (String value : cmd.value()) {
				if (buffer.startsWith(value) || value.startsWith(buffer)) {
					results.add(value);  // no space at the end, as there's no need to continue the command further
				}
			}
			candidates.addAll(results);
			return 0;
		}

		// If they haven't specified any parameters yet, at least verify the command name is fully completed
		if (options.size() == 0) {
			for (String value : cmd.value()) {
				if (value.startsWith(buffer)) {
					// They are potentially trying to type this command
					// We only need provide completion, though, if they failed to specify it fully
					if (!buffer.startsWith(value)) {
						// They failed to specify the command fully
						results.add(value + " ");
					}
				}
			}
			
			// Only quit right now if they have to finish specifying the command name
			if (results.size() > 0) {
				candidates.addAll(results);
				return 0;
			}
		}
		
		// To get this far, we know there are arguments required for this CliCommand, and they specified a valid command name
		
		// Record all the CliOptions applicable to this command
		List<CliOption> cliOptions = new ArrayList<CliOption>();
		for (Annotation[] annotations : parameterAnnotations) {
			CliOption cliOption = null;
			
			for (Annotation a : annotations) {
				if (a instanceof CliOption) {
					cliOption = (CliOption) a;
				}
			}
			Assert.notNull(cliOption, "CliOption not found for parameter '" + annotations + "'");
			cliOptions.add(cliOption);
		}
		
		// Make a list of all CliOptions they've already included or are system-provided
		List<CliOption> alreadySpecified = new ArrayList<CliOption>();
		for (CliOption option : cliOptions) {
			for (String value : option.key()) {
				if (options.containsKey(value)) {
					alreadySpecified.add(option);
					break;
				}
			}
			if (option.systemProvided()) {
				alreadySpecified.add(option);
			}
		}

		// Make a list of all CliOptions they have not provided
		List<CliOption> unspecified = new ArrayList<CliOption>(cliOptions);
		unspecified.removeAll(alreadySpecified);

		// Determine whether they're presently editing an option key or an option value
		// (and if possible, the full or partial name of the said option key being edited)
		String lastOptionKey = null;
		String lastOptionValue = null;

		// The last item in the options map is *always* the option key they're editing (will never be null)
		if (options.size() > 0) {
			lastOptionKey = new ArrayList<String>(options.keySet()).get(options.keySet().size()-1);
			lastOptionValue = options.get(lastOptionKey);
		}

		// Handle if they are trying to find out the available option keys; always present option keys in order
		// of their declaration on the method signature, thus we can stop when mandatory options are filled in
		if (methodTarget.remainingBuffer.endsWith("-")) {

			boolean showAllRemaining = true;
			for (CliOption include : unspecified) {
				if (include.mandatory()) {
					showAllRemaining = false;
					break;
				}
			}
			
			for (CliOption include : unspecified) {
				for (String value : include.key()) {
					results.add(translated + value + " ");
				}
				if (!showAllRemaining) {
					break;
				}
			}
			
			candidates.addAll(results);
			return 0;
		}
		
		// Handle suggesting an option key if they haven't got one presently specified (or they've completed a full option key/value pair)
		if (lastOptionKey == null || (!"".equals(lastOptionKey) && !"".equals(lastOptionValue) && translated.endsWith(" "))) {
			
			// We have either NEVER specified an option key/value pair
			// OR we have specified a full option key/value pair
			
			// Let's list some other options the user might want to try (naturally skip the "" option, as that's the default)
			for (CliOption include : unspecified) {
				for (String value : include.key()) {
					if (!"".equals(value) && include.mandatory()) {
						if (translated.endsWith(" ")) {
							results.add(translated + "-" + value + " ");
						} else {
							results.add(translated + " -" + value + " ");
						}
					}
				}
			}
			
			// Only abort at this point if we have some suggestions; otherwise we might want to try to complete the "" option
			if (results.size() > 0) {
				candidates.addAll(results);
				return 0;
			}
		}

		// Handle completing the option key they're presently typing
		if ((lastOptionValue == null || "".equals(lastOptionValue)) && !translated.endsWith(" ")) {
			// Given we haven't got an option value of any form, and there's no space at the buffer end, we must still be typing an option key
			
			// TODO: only include the option key itself
			
			for (CliOption option : cliOptions) {
				for (String value : option.key()) {
					if (value != null && lastOptionKey != null && value.startsWith(lastOptionKey)) {
						String remainder = value.substring(lastOptionKey.length());
						results.add(translated + remainder + " ");
					}
				}
			}
			
			candidates.addAll(results);
			return 0;
		}
		
		// To be here, we are NOT typing an option key (or we might be, and there are no further option keys left)
		if (lastOptionKey != null && !"".equals(lastOptionKey)) {
			// Lookup the relevant CliOption that applies to this lastOptionKey
			// We do this via the parameter type
			Class<?>[] paramTypes = methodTarget.method.getParameterTypes();
			for (int i = 0; i < paramTypes.length; i++) {
				CliOption option = cliOptions.get(i);
				Class<?> paramType = paramTypes[i];

				for (String value : option.key()) {
					if (value.equals(lastOptionKey)) {
						
						List<String> allValues = new ArrayList<String>();

						String suffix = " ";

						// Let's use a Converter if one is available
						for (Converter candidate : converters) {
							if (candidate.supports(paramType, option.optionContext())) {
								// found a usable converter
								boolean addSpace = candidate.getAllPossibleValues(allValues, paramType, lastOptionValue, option.optionContext(), methodTarget);
								if (!addSpace) {
									suffix = "";
								}
								break;
							}
						}
						
						if (allValues.size() == 0) {
							// Doesn't appear to be a custom Converter, so let's go and provide defaults for simple types
							
							// Provide some simple options for common types
							if (Boolean.class.isAssignableFrom(paramType) || Boolean.TYPE.isAssignableFrom(paramType)) {
								allValues.add("true");
								allValues.add("false");
							}
							
							if (Number.class.isAssignableFrom(paramType)) {
								allValues.add("0");
								allValues.add("1");
								allValues.add("2");
								allValues.add("3");
								allValues.add("4");
								allValues.add("5");
								allValues.add("6");
								allValues.add("7");
								allValues.add("8");
								allValues.add("9");
							}
							
						}
						
						String prefix = "";
						if (!translated.endsWith(" ")) {
							prefix = " ";
						}

						// Only include in the candidates those results which are compatible with the present buffer
						for (String currentValue : allValues) {
							// We only provide a suggestion if the lastOptionValue == ""
							if (lastOptionValue == null || "".equals(lastOptionValue)) {
								// We should add the result, as they haven't typed anything yet
								results.add(prefix + currentValue + suffix);
							} else {
								// Only add the result **if** what they've typed is compatible *AND* they haven't already typed it in full 
								if (lastOptionValue.startsWith(currentValue) || currentValue.startsWith(lastOptionValue)) {
									if (!lastOptionValue.equals(currentValue)) {
										results.add(prefix + currentValue + suffix);
									}
								}
							}
						}
						
						if (results.size() == 1) {
							String suggestion = results.iterator().next().trim();
							if (suggestion.equals(lastOptionValue)) {
								// They have pressed TAB in the default value, and the default value has already been provided as an explicit option
								return 0;
							}
						}
						
						if (results.size() > 0) {
							candidates.addAll(results);
							// values presented from the last space onwards
							if (translated.endsWith(" ")) {
								return translated.lastIndexOf(" ")+1;
							} else {
								return translated.trim().lastIndexOf(" ");
							}
						}

						return 0;
					}
				}

			}

		}
		
		return 0;
	}

	@CliCommand(value="help", help="Shows system help")
	public void obtainHelp(@CliOption(key={"","command"}, optionContext="availableCommands") String buffer) {
		if (buffer == null) {
			buffer = "";
		}
		
		StringBuilder sb = new StringBuilder();

		// Figure out if there's a single command we can offer help for
		Set<MethodTarget> matchingTargets = locateTargets(buffer, false);
		if (matchingTargets.size() == 1) {
			// Single command help
			MethodTarget methodTarget = matchingTargets.iterator().next(); 

			// Argument conversion time
			Annotation[][] parameterAnnotations = methodTarget.method.getParameterAnnotations();
			if (parameterAnnotations.length > 0) {
				// offer specified help
				
				CliCommand cmd = methodTarget.method.getAnnotation(CliCommand.class);
				Assert.notNull(cmd, "CliCommand not found");
				
				for (String value : cmd.value()) {
					sb.append("Keyword:                   " + value).append(System.getProperty("line.separator"));
				}
				
				sb.append("Description:               " + cmd.help()).append(System.getProperty("line.separator"));
				
				for (Annotation[] annotations : parameterAnnotations) {
					CliOption cliOption = null;
					
					for (Annotation a : annotations) {
						if (a instanceof CliOption) {
							cliOption = (CliOption) a;
						}
						
						for (String key : cliOption.key()) {
							if ("".equals(key)) {
								key = "** default **";
							}
							sb.append(" Keyword:                  " + key).append(System.getProperty("line.separator"));
						}
						
						sb.append("   Help:                   " + cliOption.help()).append(System.getProperty("line.separator"));
						sb.append("   Mandatory:              " + cliOption.mandatory()).append(System.getProperty("line.separator"));
						sb.append("   Default if specified:   '" + cliOption.specifiedDefaultValue() + "'").append(System.getProperty("line.separator"));
						sb.append("   Default if unspecified: '" + cliOption.unspecifiedDefaultValue() + "'").append(System.getProperty("line.separator"));
						sb.append(System.getProperty("line.separator"));
						
					}
					Assert.notNull(cliOption, "CliOption not found for parameter '" + annotations + "'");
					
					
				}
				
				logger.info(sb.toString());
			}
			
			// only a single argument, so default to the normal help operation
		}

		SortedSet<String> result = new TreeSet<String>();
		for (MethodTarget mt : matchingTargets) {
			CliCommand cmd = mt.method.getAnnotation(CliCommand.class);
			if (cmd != null) {
				for (String value : cmd.value()) {
					if ("".equals(cmd.help())) {
						result.add("* " + value);
					} else {
						result.add("* " + value + " - " + cmd.help());
					}
				}
			}
		}
		
		for (String s : result) {
			sb.append(s).append(System.getProperty("line.separator"));
		}
		
		logger.info(sb.toString());
		
		logger.warning("** Type 'hint' (without the quotes) and hit ENTER for step-by-step guidance **" + System.getProperty("line.separator"));
	}

	public Set<String> getEveryCommand() {
		SortedSet<String> result = new TreeSet<String>();
		for (Object o : targets) {
			Method[] methods = o.getClass().getMethods();
			for (Method m : methods) {
				CliCommand cmd = m.getAnnotation(CliCommand.class);
				if (cmd != null) {
					for (String value : cmd.value()) {
						result.add(value);
					}
				}
			}
		}
		return result;
	}
	
}
