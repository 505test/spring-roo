package org.springframework.roo.addon.web.flow;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * Provides Web Flow configuration operations.
 *
 * @author Stefan Schmidt
 * @since 1.0
 */
@ScopeDevelopment
public class WebFlowOperations {
	
	Logger logger = Logger.getLogger(WebFlowOperations.class.getName());
	
	private FileManager fileManager;
	private PathResolver pathResolver;
	private MetadataService metadataService;
	private ProjectOperations projectOperations;
	
	public WebFlowOperations(FileManager fileManager, PathResolver pathResolver, MetadataService metadataService, ProjectOperations projectOperations) {
		Assert.notNull(fileManager, "File manager required");
		Assert.notNull(pathResolver, "Path resolver required");
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(projectOperations, "Project operations required");
		this.fileManager = fileManager;
		this.pathResolver = pathResolver;
		this.metadataService = metadataService;
		this.projectOperations = projectOperations;
	}
	
	public boolean isInstallWebFlowAvailable() {		
		return getPathResolver() != null;
	}
	
	public boolean isManageWebFlowAvailable() {
		return fileManager.exists(getPathResolver().getIdentifier(Path.SRC_MAIN_RESOURCES, "applicationContext-webflow.xml"));
	}
	
	/**
	 * 
	 * @param flowName
	 */
	public void installWebFlow(String flowName) {
		//if this param is not defined we make 'sample-flow' default directory
		flowName = ((flowName != null && flowName.length() != 0) ? flowName : "sample-flow");
		
		//clean up leading '/' if required
		if(flowName.startsWith("/")) {
			flowName = flowName.substring(1);
		}
		
		//install Web flow config
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
			
		String flowContextPath = pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "applicationContext-webflow.xml");
		MutableFile webflowContextMutableFile = null;
		
		Document appCtx;
		try {
			if (fileManager.exists(flowContextPath)) {
				webflowContextMutableFile = fileManager.updateFile(flowContextPath);
				appCtx = XmlUtils.getDocumentBuilder().parse(webflowContextMutableFile.getInputStream());
			} else {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "applicationContext-webflow-template.xml"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "applicationContext-webflow.xml")).getOutputStream());
				webflowContextMutableFile = fileManager.updateFile(flowContextPath);
				appCtx = XmlUtils.getDocumentBuilder().parse(webflowContextMutableFile.getInputStream());
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		} 	
		
		//register custom directory if needed
		if(!"sample-flow".equals(flowName)) {	
			Element root = (Element) appCtx.getFirstChild();	
			Element flowViewResolver = XmlUtils.findRequiredElement("/beans/bean[@class='org.springframework.web.servlet.view.InternalResourceViewResolver']", root);
			flowViewResolver.removeAttribute("p:prefix");
			flowViewResolver.setAttribute("p:prefix", "/WEB-INF/jsp/" + flowName);
			XmlUtils.writeXml(webflowContextMutableFile.getOutputStream(), appCtx);
		}
		
		//adjust MVC servlet config to accommodate Spring Web Flow
		String mvcContextPath = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/" + projectMetadata.getProjectName().toLowerCase() + "-servlet.xml");
		MutableFile mvcContextMutableFile = null;
		
		Document mvcAppCtx;
		try {
			if (fileManager.exists(mvcContextPath)) {
				mvcContextMutableFile = fileManager.updateFile(mvcContextPath);
				mvcAppCtx = XmlUtils.getDocumentBuilder().parse(mvcContextMutableFile.getInputStream());
			} else {
				throw new IllegalStateException("Could not find MVC servlet configuration file at " + mvcContextPath);
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		} 				
		
		Element root = (Element) mvcAppCtx.getFirstChild();	
		boolean writeXml = false;

		if (null == XmlUtils.findFirstElement("/beans/bean[@class='org.springframework.webflow.mvc.servlet.FlowHandlerMapping']", root)) {
			root.appendChild(mvcAppCtx.createComment("Maps request paths to flows in the flowRegistry"));
			Element bean = mvcAppCtx.createElement("bean");
			bean.setAttribute("class", "org.springframework.webflow.mvc.servlet.FlowHandlerMapping");
			bean.appendChild(getValueProperty("order", "0", mvcAppCtx));
			bean.appendChild(getRefProperty("flowRegistry", "flowRegistry", mvcAppCtx));
			root.appendChild(bean);
			writeXml = true;
		}
		
		if (null == XmlUtils.findFirstElement("/beans/bean[@class='org.springframework.web.servlet.mvc.annotation.DefaultAnnotationHandlerMapping']", root)) {
			root.appendChild(mvcAppCtx.createComment("Maps request paths to @Controller classes"));
			Element bean = mvcAppCtx.createElement("bean");
			bean.setAttribute("class", "org.springframework.web.servlet.mvc.annotation.DefaultAnnotationHandlerMapping");
			bean.appendChild(getValueProperty("order", "1", mvcAppCtx));
			root.appendChild(bean);
			writeXml = true;
		}
		
		if (null == XmlUtils.findFirstElement("/beans/bean[@class='org.springframework.webflow.mvc.servlet.FlowHandlerAdapter']", root)) {
			root.appendChild(mvcAppCtx.createComment("Dispatches requests mapped to flows to FlowHandler implementations"));
			Element bean = mvcAppCtx.createElement("bean");
			bean.setAttribute("class", "org.springframework.webflow.mvc.servlet.FlowHandlerAdapter");
			bean.appendChild(getRefProperty("flowExecutor", "flowExecutor", mvcAppCtx));
			root.appendChild(bean);
			writeXml = true;
		}
		
		if (null == XmlUtils.findFirstElement("/beans/bean[@class='org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter']", root)) {
			root.appendChild(mvcAppCtx.createComment("Dispatches requests mapped to POJO @Controllers implementations"));
			Element bean = mvcAppCtx.createElement("bean");
			bean.setAttribute("class", "org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter");
			root.appendChild(bean);
			writeXml = true;
		}
		
		if (writeXml) {
			XmlUtils.writeXml(mvcContextMutableFile.getOutputStream(), mvcAppCtx);	
		}
		
		String flowDirectory = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/jsp/" + flowName);
		
		//install flow directory
		if (!fileManager.exists(flowDirectory)) {			
			fileManager.createDirectory(flowDirectory);
		}
		
		String flowConfig = flowDirectory.concat("/" + flowName + "-flow.xml");
		if (!fileManager.exists(flowConfig)) {			
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "flow-template.xml"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/jsp/" + flowName + "/" + flowName + "-flow.xml")).getOutputStream());
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
		
		updateDependencies();
	}	
	
	private void updateDependencies() {	
		InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "dependencies.xml");
		Assert.notNull(templateInputStream, "Could not acquire dependencies.xml file");
		Document dependencyDoc;
		try {
			dependencyDoc = XmlUtils.getDocumentBuilder().parse(templateInputStream);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		Element dependenciesElement = (Element) dependencyDoc.getFirstChild();
		
		List<Element> springDependencies = XmlUtils.findElements("/dependencies/springWebFlow/dependency", dependenciesElement);
		for(Element dependency : springDependencies) {
			projectOperations.dependencyUpdate(new Dependency(dependency));
		}
	}	
	
	private Element getValueProperty(String name, String value, Document doc) {
		Element property = doc.createElement("property");
		property.setAttribute("name", name);
		property.setAttribute("value", value);
		return property;
	}
	
	private Element getRefProperty(String name, String ref, Document doc) {
		Element property = doc.createElement("property");
		property.setAttribute("name", name);
		property.setAttribute("ref", ref);
		return property;
	}
	
	private PathResolver getPathResolver() {
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		if (projectMetadata == null) {
			return null;
		}
		return projectMetadata.getPathResolver();
	}
}