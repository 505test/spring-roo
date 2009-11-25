package org.springframework.roo.addon.mvc.jsp;

import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * 
 * This is a helper class which creates JSP form artifacts 
 * used during view layer generation.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
public class JspUtils {

	public static Element getInputBox(Document document, JavaSymbolName field, Integer maxValue) {
		Element formInput = document.createElement("form:input");
		formInput.setAttribute("path", StringUtils.uncapitalize(field.getSymbolName()));
		formInput.setAttribute("id", "_" + field.getSymbolName() + "_id");
		formInput.setAttribute("size", "0");
		formInput.setAttribute("cssStyle", "width:250px");
		formInput.setAttribute("maxlength", maxValue.toString());
		return formInput;
	}
	
	public static Element getSelectBox(Document document, JavaSymbolName fieldName, String pluralName, FieldMetadata identifierField) {
		Element formSelect = document.createElement("form:select");
		formSelect.setAttribute("path", StringUtils.uncapitalize(fieldName.getSymbolName()));
		formSelect.setAttribute("cssStyle", "width:250px");						
		formSelect.setAttribute("id", "_" + fieldName + "_id");
		Element formOptions = document.createElement("form:options");
		formOptions.setAttribute("items", "${" + pluralName.toLowerCase() + "}");
		formOptions.setAttribute("itemValue", identifierField.getFieldName().getSymbolName());	
		formSelect.appendChild(formOptions);		
		return formSelect;
	}
	
	public static Element getEnumSelectBox(Document document, JavaType javaType, JavaSymbolName fieldName) {
		Element formSelect = document.createElement("form:select");
		formSelect.setAttribute("path", StringUtils.uncapitalize(fieldName.getSymbolName()));
		formSelect.setAttribute("cssStyle", "width:250px");						
		formSelect.setAttribute("id", "_" + fieldName.getSymbolName() + "_id");
		formSelect.setAttribute("items", "${" + javaType.getSimpleTypeName().toLowerCase() + "_enum}");		
		return formSelect;
	}
	
	public static Element getTextArea(Document document, JavaSymbolName fieldName, Integer maxValue) {
		Element textArea = document.createElement("form:textarea");
		textArea.setAttribute("path", StringUtils.uncapitalize(fieldName.getSymbolName()));
		textArea.setAttribute("id", "_" + fieldName.getSymbolName() + "_id");
		textArea.setAttribute("cssStyle", "width:250px");
		return textArea;
	}
	
	public static Element getCheckBox(Document document, JavaSymbolName fieldName) {
		Element formCheck = document.createElement("form:checkbox");
		formCheck.setAttribute("path", StringUtils.uncapitalize(fieldName.getSymbolName()));
		formCheck.setAttribute("id", "_" + fieldName.getSymbolName() + "_id");
		return formCheck;
	}
	
	public static Element getErrorsElement(Document document, JavaSymbolName field) {
		Element errors = document.createElement("form:errors");
		errors.setAttribute("path", StringUtils.uncapitalize(field.getSymbolName()));
		errors.setAttribute("id", "_" + field.getSymbolName() + "_error_id");
		errors.setAttribute("cssClass", "errors");
		return errors;
	}
}
