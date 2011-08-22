package org.springframework.roo.addon.jsf;

import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.JavaType;

/**
 * Constants for JSF/PrimeFaces-specific {@link JavaType}s.
 * 
 * Use them in preference to creating new instances of these types.
 *
 * @author Alan Stewart
 * @since 1.2.0
 */
public class JsfUtils {
	
	public static final JavaType CONVERTER = new JavaType("javax.faces.convert.Converter");
	public static final JavaType EL_CONTEXT = new JavaType("javax.el.ELContext");
	public static final JavaType EXPRESSION_FACTORY = new JavaType("javax.el.ExpressionFactory");
	public static final JavaType FACES_CONTEXT = new JavaType("javax.faces.context.FacesContext");
	public static final JavaType HTML_OUTPUT_TEXT = new JavaType("javax.faces.component.html.HtmlOutputText");
	public static final JavaType HTML_PANEL_GRID = new JavaType("javax.faces.component.html.HtmlPanelGrid");
	public static final JavaType MANAGED_BEAN = new JavaType("javax.faces.bean.ManagedBean");
	public static final JavaType PRIMEFACES_CLOSE_EVENT = new JavaType("org.primefaces.event.CloseEvent");
	public static final JavaType PRIMEFACES_DEFAULT_MENU_MODEL = new JavaType("org.primefaces.model.DefaultMenuModel");
	public static final JavaType PRIMEFACES_CALENDAR = new JavaType("org.primefaces.component.calendar.Calendar");
	public static final JavaType PRIMEFACES_MENU_ITEM = new JavaType("org.primefaces.component.menuitem.MenuItem");
	public static final JavaType PRIMEFACES_MENU_MODEL = new JavaType("org.primefaces.model.MenuModel");
	public static final JavaType PRIMEFACES_SUB_MENU = new JavaType("org.primefaces.component.submenu.Submenu");
	public static final JavaType REQUEST_SCOPED = new JavaType("javax.faces.bean.RequestScoped");
	public static final JavaType SESSION_SCOPED = new JavaType("javax.faces.bean.SessionScoped");
	public static final JavaType UI_COMPONENT = new JavaType("javax.faces.component.UIComponent");
	public static final JavaType VIEW_SCOPED = new JavaType("javax.faces.bean.ViewScoped");

	/**
	 * Constructor is private to prevent instantiation
	 */
	private JsfUtils() {}

	public static void addCommonJsfFields(ImportRegistrationResolver imports, InvocableMemberBodyBuilder bodyBuilder) {
		imports.addImport(EL_CONTEXT);
		imports.addImport(EXPRESSION_FACTORY);
		imports.addImport(FACES_CONTEXT);

		bodyBuilder.appendFormalLine("FacesContext facesContext = FacesContext.getCurrentInstance();");
		bodyBuilder.appendFormalLine("ExpressionFactory expressionFactory = facesContext.getApplication().getExpressionFactory();");
		bodyBuilder.appendFormalLine("ELContext elContext = facesContext.getELContext();");
		bodyBuilder.appendFormalLine("");
	}
}
