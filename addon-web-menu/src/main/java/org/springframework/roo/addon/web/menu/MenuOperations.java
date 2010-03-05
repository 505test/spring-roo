package org.springframework.roo.addon.web.menu;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlRoundTripUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Generates the jsp menu and allows for management of menu items.
 *
 * @author Stefan Schmidt
 * @since 1.0
 */
@ScopeDevelopment
public class MenuOperations {
	
	private FileManager fileManager;
	
	private PathResolver pathResolver;
	
	private String menuFile;
	
	public static final String DEFAULT_MENU_ITEM_PREFIX = "i:";
	public static final String FINDER_MENU_ITEM_PREFIX = "fi:";
	
	public MenuOperations(FileManager fileManager, PathResolver pathResolver){
		Assert.notNull(fileManager, "File manager required");
		Assert.notNull(pathResolver, "Path resolver required");

		this.fileManager = fileManager;
		this.pathResolver = pathResolver;
		
		menuFile = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/menu.jspx");
	}

	/**
	 * Allows for the addition of menu categories and menu items. If a category or menu item with the
	 * given identifier exists then it will <b>not</b> be overwritten or replaced.
	 * <p>
	 * Addons can determine their own category and menu item identifiers so that there are no clashes 
	 * with other addons. 
	 * <p>
	 * The recommended category identifier naming convention is <i>addon-name_intention_category</i> where 
	 * intention represents a further identifier to diffentiate between different categories provided
	 * by the same addon. Similarly, the recommended menu item identifier naming convention is
	 * <i>addon-name_intention_menu_item</i>.
	 *  
	 * 
	 * @param menuCategoryId the identifier for the menu category (required)
	 * @param menuCategoryLabel the category label (required)
	 * @param menuItemId the menu item identifier (required)
	 * @param menuItemLabel the menu item label (required)
	 * @param link the menu item link (required)
	 * @param idPrefix the prefix to be used for this menu item (optional, MenuOperations.DEFAULT_MENU_ITEM_PREFIX is default)
	 */
	public void addMenuItem(JavaSymbolName menuCategoryName, JavaSymbolName menuItemName, String globalMessageCode, String link, String idPrefix) {
		Assert.notNull(menuCategoryName, "Menu category name required");
		Assert.notNull(menuItemName, "Menu item name required");
		Assert.hasText(link, "Link required");
		
		if (idPrefix == null || idPrefix.length() == 0) {
			idPrefix = DEFAULT_MENU_ITEM_PREFIX;
		}
		
		Document document;
		try {			
			document = XmlUtils.getDocumentBuilder().parse(getMenuFile());
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to parse menu.jspx" + (e.getMessage() == null || "".equals(e.getMessage()) ? "" : " (" + e.getMessage() + ")"));
		}
		
		//make the root element of the menu the one with the menu identifier allowing for different decorations of menu
		Element rootElement = XmlUtils.findFirstElement("//*[@id='_menu']", (Element) document.getFirstChild());
		if (rootElement == null) {
			Element rootMenu = new XmlElementBuilder("menu:menu", document).addAttribute("id", "_menu").build();
			rootMenu.setAttribute("z", XmlRoundTripUtils.calculateUniqueKeyFor(rootMenu));
			rootElement = (Element) document.getDocumentElement().appendChild(rootMenu);
		}
		
		//check for existence of menu category by looking for the indentifier provided
		Element category = XmlUtils.findFirstElement("//*[@id='c:" + menuCategoryName.getSymbolName().toLowerCase() + "']", rootElement);
		
		//if not exists, create new one
		if(category == null) {
			category = (Element) rootElement.appendChild(new XmlElementBuilder("menu:category", document)
															.addAttribute("id", "c:" + menuCategoryName.getSymbolName().toLowerCase())
															.addAttribute("name", menuCategoryName.getSymbolName())
														.build());
			category.setAttribute("z", XmlRoundTripUtils.calculateUniqueKeyFor(category));
			setProperty(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/application.properties", "menu.category." + menuCategoryName.getSymbolName().toLowerCase() + ".label", menuCategoryName.getReadableSymbolName());
		} 
		
		//check for existence of menu item by looking for the indentifier provided
		Element menuItem = XmlUtils.findFirstElement("//*[@id='" + menuCategoryName.getSymbolName().toLowerCase() + "_" + menuItemName.getSymbolName().toLowerCase() + "']", rootElement);
		
		if (menuItem == null) {
			menuItem = new XmlElementBuilder("menu:item", document)
							.addAttribute("id", idPrefix + menuCategoryName.getSymbolName().toLowerCase() + "_" + menuItemName.getSymbolName().toLowerCase())
							.addAttribute("name", menuItemName.getSymbolName())
							.addAttribute("messageCode", globalMessageCode)
							.addAttribute("url", link)
						.build();
			menuItem.setAttribute("z", XmlRoundTripUtils.calculateUniqueKeyFor(menuItem));
			category.appendChild(menuItem);	
			setProperty(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/application.properties", "menu.item." + menuItemName.getSymbolName().toLowerCase() + ".label", menuItemName.getReadableSymbolName());
		}
		writeToDiskIfNecessary(document);
	}
	
	public void cleanUpFinderMenuItems(JavaSymbolName menuCategoryName, List<String> allowedFinderMenuIds) {
		Assert.notNull(menuCategoryName, "Menu category identifier required");
		Assert.notNull(allowedFinderMenuIds, "List of allowed menu items required");
		
		Document document;
		try {
			document = XmlUtils.getDocumentBuilder().parse(getMenuFile());
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to parse menu.jspx", e);
		}
		
		//find any menu items under this category which have an id that starts with the menuItemIdPrefix
		List<Element> elements = XmlUtils.findElements("//category[@id='c:" +  menuCategoryName.getSymbolName().toLowerCase() + "']//item[starts-with(@id, '" + FINDER_MENU_ITEM_PREFIX + "')]", document.getDocumentElement());
		if(elements.size() == 0) {
			return;
		}
		for (Element element: elements) {
			if (!allowedFinderMenuIds.contains(element.getAttribute("id")) && ("?".equals(element.getAttribute("z")) || XmlRoundTripUtils.calculateUniqueKeyFor(element).equals(element.getAttribute("z")))) {
				element.getParentNode().removeChild(element);
			}
		}
		writeToDiskIfNecessary(document);
	}
	
	/**
	 * Attempts to locate a menu item and remove it. 
	 * 
	 * @param menuCategoryId the identifier for the menu category (required)
	 * @param menuItemId the menu item identifier (required)
	 * @param idPrefix the prefix to be used for this menu item (optional, MenuOperations.DEFAULT_MENU_ITEM_PREFIX is default)
	 */
	public void cleanUpMenuItem(JavaSymbolName menuCategoryName, JavaSymbolName menuItemName, String idPrefix) {
		Assert.notNull(menuCategoryName, "Menu category identifier required");
		Assert.notNull(menuItemName, "Menu item id required");
		
		if (idPrefix == null || idPrefix.length() == 0) {
			idPrefix = DEFAULT_MENU_ITEM_PREFIX;
		}
		
		Document document;
		try {
			document = XmlUtils.getDocumentBuilder().parse(getMenuFile());
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to parse menu.jsp", e);
		}
		
		//find menu item under this category if exists 
		Element element = XmlUtils.findFirstElement("//category[@id='c:" + menuCategoryName.getSymbolName().toLowerCase() + "']//item[@id='" + idPrefix + menuCategoryName.getSymbolName().toLowerCase() + "_" + menuItemName.getSymbolName().toLowerCase() + "']", document.getDocumentElement());
		if(element==null) {
			return;
		}
		if ("?".equals(element.getAttribute("z")) || XmlRoundTripUtils.calculateUniqueKeyFor(element).equals(element.getAttribute("z"))) {
			element.getParentNode().removeChild(element);
		}
		
		writeToDiskIfNecessary(document);
	}
	
	private InputStream getMenuFile() {			
		if (!fileManager.exists(menuFile)) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "menu.jspx"), fileManager.createFile(menuFile).getOutputStream());
			} catch (Exception e) {
				new IllegalStateException("Encountered an error during copying of resources for MVC Menu addon.", e);
			}			
		}
		
		if (!fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/tags/menu/menu.tagx"))) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "menu.tagx"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/tags/menu/menu.tagx")).getOutputStream());
			} catch (Exception e) {
				new IllegalStateException("Encountered an error during copying of resources for MVC Menu addon.", e);
			}			
		}
		
		if (!fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/tags/menu/item.tagx"))) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "item.tagx"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/tags/menu/item.tagx")).getOutputStream());
			} catch (Exception e) {
				new IllegalStateException("Encountered an error during copying of resources for MVC Menu addon.", e);
			}			
		}
		
		if (!fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/tags/menu/category.tagx"))) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "category.tagx"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/tags/menu/category.tagx")).getOutputStream());
			} catch (Exception e) {
				new IllegalStateException("Encountered an error during copying of resources for MVC Menu addon.", e);
			}			
		}
		
		// We know the menu file already exists, as the logic earlier copied it from a template
		InputStream existingMenu;
		try {
			existingMenu = new FileInputStream(new File(menuFile));
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}		
		return existingMenu;
	}
	
	/** return indicates if disk was changed (ie updated or created) */
	private boolean writeToDiskIfNecessary(Document proposed) {
		Document original = null;
		
		// If mutableFile becomes non-null, it means we need to use it to write out the contents of jspContent to the file
		MutableFile mutableFile = null;
		if (fileManager.exists(menuFile)) {	
			try {
				original = XmlUtils.getDocumentBuilder().parse(getMenuFile());
			} catch (Exception e) {
				new IllegalStateException("Could not parse file: " + menuFile);
			} 
			Assert.notNull(original, "Unable to parse " + menuFile);
			if (XmlRoundTripUtils.compareDocuments(original, proposed)) {
				mutableFile = fileManager.updateFile(menuFile);
			}
		} else {
			original = proposed;
			mutableFile = fileManager.createFile(menuFile);
			Assert.notNull(mutableFile, "Could not create JSP file '" + menuFile + "'");
		}
		
		try {
			if (mutableFile != null) {
				// Build a string representation of the JSP
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				XmlUtils.writeXml(XmlUtils.createIndentingTransformer(), byteArrayOutputStream, original);
				String jspContent = byteArrayOutputStream.toString();

				// We need to write the file out (it's a new file, or the existing file has different contents)
				FileCopyUtils.copy(jspContent, new OutputStreamWriter(mutableFile.getOutputStream()));
				// Return and indicate we wrote out the file
				return true;
			}
		} catch (IOException ioe) {
			throw new IllegalStateException("Could not output '" + mutableFile.getCanonicalPath() + "'", ioe);
		}
		
		// A file existed, but it contained the same content, so we return false
		return false;
	}
	
	 /**
     * Changes the specified property, throwing an exception if the file does not exist.
     * 
     * @param propertyFilePath the location of the property file (required)
     * @param propertyFilename the name of the property file within the specified path (required)
     * @param key the property key to update (required)
     * @param value the property value to set into the property key (required)
     */
    private void setProperty(Path propertyFilePath, String propertyFilename, String key, String value) {
	    Assert.notNull(propertyFilePath, "Property file path required");
	    Assert.hasText(propertyFilename, "Property filename required");
	    Assert.hasText(key, "Key required");
	    Assert.hasText(value, "Value required");

	    String filePath = pathResolver.getIdentifier(propertyFilePath, propertyFilename);

	    Properties readProps = new Properties();
	    try {
            if (fileManager.exists(filePath)) {
            	
            	readProps.load(fileManager.getInputStream(filePath));
            } else {
            	throw new IllegalStateException("Properties file not found");
            }
	    } catch (IOException ioe) {
	    	throw new IllegalStateException(ioe);
	    }
	    if (null == readProps.getProperty(key)) {
	    	MutableFile mutableFile = fileManager.updateFile(filePath);
		    Properties props = new Properties() {
				//override the keys() method to order the keys alphabetically
		        @Override 
		        @SuppressWarnings("unchecked")
		        public synchronized Enumeration keys() {
		        	final Object[] keys = keySet().toArray();
		        	Arrays.sort(keys);
		        	return new Enumeration() {
			        	int i = 0;
			        	public boolean hasMoreElements() { return i < keys.length; }
			        		public Object nextElement() { return keys[i++]; }
			        	};
		        	}
		    	};
		    try {
		    	props.load(mutableFile.getInputStream());	
				props.setProperty(key, value);   
		    	props.store(mutableFile.getOutputStream() , "Updated " + new Date());
		    } catch (IOException ioe) {
		    	throw new IllegalStateException(ioe);
		    }
	    }
    }
}

