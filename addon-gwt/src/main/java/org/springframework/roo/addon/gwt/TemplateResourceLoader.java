package org.springframework.roo.addon.gwt;

import hapax.Template;
import hapax.TemplateException;
import hapax.TemplateLoader;
import hapax.parser.TemplateParser;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;

/**
 * Loads hapax templates from the classpath.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class TemplateResourceLoader implements TemplateLoader {

    // Constants
    private static final String TEMPLATE_DIR = "org/springframework/roo/addon/gwt/scaffold/templates/";
    private static final Map<String, Template> cache = new HashMap<String, Template>();

    // Fields
    protected final String baseDir;
    protected final TemplateParser parser;

    /**
     * Creates a TemplateLoader for CTemplate language using the default
     * template directory
     */
    public static TemplateLoader create() {
        return new TemplateResourceLoader(TEMPLATE_DIR);
    }

    /**
     * Creates a TemplateLoader for CTemplate language
     */
    public static TemplateLoader create(final String base_path) {
        return new TemplateResourceLoader(base_path);
    }

    /**
     * Creates a TemplateLoader using the argument parser.
     */
    public static TemplateLoader createForParser(final String base_path,
            final TemplateParser parser) {
        return new TemplateResourceLoader(base_path, parser);
    }

    public TemplateResourceLoader(final String baseDir) {
        this(baseDir, null);
    }

    public TemplateResourceLoader(final String baseDir,
            final TemplateParser parser) {
        this.baseDir = baseDir;
        this.parser = parser;
    }

    public String getTemplateDirectory() {
        return this.baseDir;
    }

    public Template getTemplate(final String resource) throws TemplateException {
        return getTemplate(new TemplateLoader.Context(this, baseDir), resource);
    }

    public Template getTemplate(final TemplateLoader context, String resource)
            throws TemplateException {
        if (!resource.endsWith(".xtm")) {
            resource += ".xtm";
        }

        String templatePath = baseDir + resource;
        if (cache.containsKey(templatePath)) {
            return cache.get(templatePath);
        }

        InputStream is = getClass().getClassLoader().getResourceAsStream(
                templatePath);
        Assert.notNull(is, "template path required");
        String contents;
        try {
            contents = FileCopyUtils.copyToString(new InputStreamReader(
                    new BufferedInputStream(is)));
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }

        Template template = parser == null ? new Template(contents, context)
                : new Template(parser, contents, context);

        synchronized (cache) {
            cache.put(templatePath, template);
        }

        return template;
    }
}
