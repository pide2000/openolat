/**
* OLAT - Online Learning and Training<br>
* http://www.olat.org
* <p>
* Licensed under the Apache License, Version 2.0 (the "License"); <br>
* you may not use this file except in compliance with the License.<br>
* You may obtain a copy of the License at
* <p>
* http://www.apache.org/licenses/LICENSE-2.0
* <p>
* Unless required by applicable law or agreed to in writing,<br>
* software distributed under the License is distributed on an "AS IS" BASIS, <br>
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
* See the License for the specific language governing permissions and <br>
* limitations under the License.
* <p>
* Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
* University of Zurich, Switzerland.
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.  
* <p>
*/ 
package org.olat.core.gui.components.form.flexible.elements;

import java.util.List;
import java.util.Set;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.impl.elements.table.ExtendedFlexiTableSearchController;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableComponent;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableComponentDelegate;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableRendererType;
import org.olat.core.gui.components.velocity.VelocityContainer;

/**
 * 
 */
public interface FlexiTableElement extends FormItem {

	public static final String ROM_SELECT_EVENT = "rSelect";
	
	@Override
	public FlexiTableComponent getComponent();
	
	
	public FlexiTableStateEntry getStateEntry();
	
	public void setStateEntry(UserRequest ureq, FlexiTableStateEntry state);
	
	/**
	 * @return the type of renderer used by  this table
	 */
	public FlexiTableRendererType getRendererType();
	
	/**
	 * Set the renderer for this table
	 * @param rendererType
	 */
	public void setRendererType(FlexiTableRendererType rendererType);
	
	/**
	 * Set the renderer available
	 * @param rendererType
	 */
	public void setAvailableRendererTypes(FlexiTableRendererType... rendererType);
	
	/**
	 * Set the row renderer used by the custom renderer type.
	 * @param renderer
	 * @param componentDelegate
	 */
	public void setRowRenderer(VelocityContainer renderer, FlexiTableComponentDelegate componentDelegate);
	
	/**
	 * Set the details renderer used by the classic renderer type.
	 * @param rowRenderer
	 * @param componentDelegate
	 */
	public void setDetailsRenderer(VelocityContainer rowRenderer, FlexiTableComponentDelegate componentDelegate);

	/**
	 * @return True if muli selection is enabled
	 */
	public boolean isMultiSelect();
	
	/**
	 * Enable multi-selection
	 */
	public void setMultiSelect(boolean enable);
	
	/**
	 * 
	 * @return true if the user can customize the columns of the table
	 */
	public boolean isCustomizeColumns();

	/**
	 * Enable customizing of columns
	 * @param customizeColumns
	 */
	public void setCustomizeColumns(boolean customizeColumns);
	
	/**
	 * Set the id of the preferences saved on the database.
	 * 
	 * @param ureq
	 * @param id
	 */
	public void setAndLoadPersistedPreferences(UserRequest ureq, String id);
	
	/**
	 * @return The CSS selector used to calculate the height of the table
	 * (datatables variant only)
	 */
	public String getWrapperSelector();
	
	/**
	 * Set a CSS selector for the datatables variant. It can
	 * calculate the height it can use.
	 * @param wrapperSelector
	 */
	public void setWrapperSelector(String wrapperSelector);
	
	/**
	 * 
	 * @return
	 */
	public int getColumnIndexForDragAndDropLabel();
	
	/**
	 * Show the num of rows, or not
	 * 
	 * @param enable
	 */
	public void setNumOfRowsEnabled(boolean enable);
	
	/**
	 * @return True if the choice in page size "All" is allowed.
	 */
	public boolean isShowAllRowsEnabled();
	
	/**
	 * Enable/disable the "All" choice for the page sizes.
	 * @param showAllRowsEnabled
	 */
	public void setShowAllRowsEnabled(boolean showAllRowsEnabled);

	/**
	 * Setting a value enable the drag and drop on this table. Drag and drop
	 * is only implemented for the classic voew.
	 * 
	 * @param columnLabelForDragAndDrop
	 */
	public void setColumnIndexForDragAndDropLabel(int columnLabelForDragAndDrop);
	
	/**
	 * @return true if the links select all / unselect all are enabled
	 */
	public boolean isSelectAllEnable();
	
	/**
	 * Enable the select all /unselect all links
	 * @param enable
	 */
	public void setSelectAllEnable(boolean enable);
	
	/**
	 * Return all selected rows
	 * @return
	 */
	public Set<Integer> getMultiSelectedIndex();
	
	/**
	 * Set a list of selected index (don't sort after this point)
	 * @param set
	 */
	public void setMultiSelectedIndex(Set<Integer> set);
	
	/**
	 * 
	 * @param index
	 * @return true if the row is selected
	 */
	public boolean isMultiSelectedIndex(int index);
	
	/**
	 * Is a search field enabled
	 * @return
	 */
	public boolean isSearchEnabled();
	
	/**
	 * Enable the search field
	 * @param enable
	 */
	public void setSearchEnabled(boolean enable);
	
	/**
	 * Is the filer enabled?
	 * @return
	 */
	public boolean isFilterEnabled();
	
	/**
	 * @return The selected key by the filter, or null if no item is selected
	 */
	public String getSelectedFilterKey();
	
	/**
	 * @return The selected value by the filter, or null if no item is selected
	 */
	public String getSelectedFilterValue();
	
	/**
	 * Set the values for the filter and it will enable it.
	 * @param keys
	 * @param values
	 */
	public void setFilters(String label, List<FlexiTableFilter> filters);
	
	/**
	 * 
	 * @param label
	 * @param sorts
	 */
	public void setSortSettings(FlexiTableSortOptions options);
	
	/**
	 * Enable export
	 * @return True if export is enabled
	 */
	public boolean isExportEnabled();
	
	public void setExportEnabled(boolean enabled);
	
	/**
	 *
	 * @return True if the table is in editing mode
	 */
	public boolean isEditMode();

	/**
	 * Set a visual change but do not change anything on the model
	 * @param editMode
	 */
	public void setEditMode(boolean editMode);
	
	public boolean isColumnModelVisible(FlexiColumnModel col);
	
	public void setColumnModelVisible(FlexiColumnModel col, boolean visible);
	
	/**
	 * 
	 * @param callout
	 */
	public void setExtendedSearch(ExtendedFlexiTableSearchController controller);
	
	
	public boolean isExtendedSearchExpanded();
	
	/**
	 * Open the extended search
	 */
	public void expandExtendedSearch(UserRequest ureq);
	
	/**
	 * Close the extended search callout if open
	 */
	public void collapseExtendedSearch();
	
	/**
	 * Is the details view visible for this particular row?
	 */
	public boolean isDetailsExpended(int row);
	
	/**
	 * 
	 */
	public void expandDetails(int row);
	
	public void collapseDetails(int row);
	
	public void collapseAllDetails();
	
	/**
	 * Return the page size
	 * @return
	 */
	public int getPageSize();
	
	public void setPageSize(int pageSize);
	/**
	 * Return the default page size which cannot be changed
	 * by users.
	 * 
	 * @return
	 */
	public int getDefaultPageSize();
	
	public int getPage();
	
	public void setPage(int page);
	
	public void quickSearch(UserRequest ureq, String search);
	
	public void sort(String sortKey, boolean asc);
	
	public void reloadData();
	
	public void deselectAll();

	/**
	 * Set the message displayed when the table is empty and the table header
	 * and table options such as search, sort etc are hidden. If null (default)
	 * the empty table is shown.
	 * 
	 * @param i18key
	 */
	public void setEmtpyTableMessageKey(String i18key);
	/**
	 * @return The i18n key for the message to be displayed when the table is empty or NULL when no message should be displayed.
	 */
	public String getEmtpyTableMessageKey();
}