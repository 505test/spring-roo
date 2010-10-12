package org.springframework.roo.addon.gwt;

/**
 * Represents mirror types classes. There are one of these for each entity mirrored by Roo.
 * 
 * <p>
 * A mirror type has its .java source code produced by the {@link GwtMetadataProvider}. 
 * 
 * <p>
 * This enum provides a convenient way to ensure filenames are composed correctly for each mirror type
 * to be generated, and also ensures the mirror type is placed into the correct package. The correct
 * package is resolved via the {@link GwtPath} enum.
 * 
 * @author Ben Alex
 * @author Alan Stewart
 * @author Amit Manjhi 
 * @since 1.1
 */
public enum MirrorType {
	PROXY(GwtPath.MANAGED_REQUEST, "Proxy", "proxy", null), 
	REQUEST(GwtPath.MANAGED_REQUEST, "Request", "request", null), 
	ACTIVITIES_MAPPER(GwtPath.MANAGED_ACTIVITY, "ActivitiesMapper", "activitiesMapper", "ActivitiesMapper"), 
	DETAIL_ACTIVITY(GwtPath.MANAGED_ACTIVITY, "DetailsActivity", "detailsActivity", "DetailsActivity"), 
	EDIT_ACTIVITY_WRAPPER(GwtPath.MANAGED_ACTIVITY, "EditActivityWrapper", "editActivityWrapper", "EditActivityWrapper"), 
	LIST_ACTIVITY(GwtPath.MANAGED_ACTIVITY, "ListActivity", "listActivity", "ListActivity"), 
	LIST_VIEW(GwtPath.MANAGED_UI, "ListView", "listView", "ListView"), 
	LIST_VIEW_BINDER(GwtPath.MANAGED_UI, "ListViewBinder", "listViewBinder", null), 
	MOBILE_LIST_VIEW(GwtPath.MANAGED_UI, "MobileListView", "mobileListView", "MobileListView"),
	DETAILS_VIEW_BINDER(GwtPath.MANAGED_UI, "DetailsViewBinder", "detailsViewBinder", null), 
	DETAILS_VIEW(GwtPath.MANAGED_UI, "DetailsView", "detailsView", "DetailsView"),
	MOBILE_DETAILS_VIEW(GwtPath.MANAGED_UI, "MobileDetailsView", "mobileDetailsView", "MobileDetailsView"),
	EDIT_VIEW_BINDER(GwtPath.MANAGED_UI, "EditViewBinder", "editViewBinder", null), 
	EDIT_VIEW(GwtPath.MANAGED_UI, "EditView", "editView", "EditView"),
	MOBILE_EDIT_VIEW(GwtPath.MANAGED_UI, "MobileEditView", "mobileEditView", "MobileEditView"),
	EDIT_RENDERER(GwtPath.MANAGED_UI, "ProxyRenderer", "renderer", "EditRenderer"),
        SET_EDITOR(GwtPath.MANAGED_UI, "SetEditor", "setEditor", "SetEditor"),
        LIST_EDITOR(GwtPath.MANAGED_UI, "ListEditor", "listEditor", "ListEditor");

	private GwtPath path;
	private String suffix;
	private String name;
	private String template;

	private MirrorType(GwtPath path, String suffix, String name, String template) {
		this.path = path;
		this.suffix = suffix;
		this.name = name;
		this.template = template;
	}

	public GwtPath getPath() {
		return path;
	}

	public String getSuffix() {
		return suffix;
	}

	public String getName() {
		return name;
	}

	public String getTemplate() {
		return template;
	}

        public boolean isList() {
                return this == LIST_ACTIVITY || this == LIST_VIEW || this == LIST_VIEW_BINDER;
        }

        public boolean isUI() {
          return getPath() != GwtPath.MANAGED_REQUEST;
        }
}
