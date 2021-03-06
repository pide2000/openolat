/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.course.nodes.cl.ui;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.olat.core.commons.persistence.SortKey;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.impl.elements.table.DefaultFlexiTableDataModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.ExportableFlexiTableDataModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FilterableFlexiTableModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableComponent;
import org.olat.core.gui.components.form.flexible.impl.elements.table.SortableFlexiTableDataModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.SortableFlexiTableModelDelegate;
import org.olat.core.gui.components.form.flexible.impl.elements.table.XlsFlexiTableExporter;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.gui.media.WorkbookMediaResource;
import org.olat.core.gui.render.EmptyURLBuilder;
import org.olat.core.gui.render.StringOutput;
import org.olat.core.gui.render.StringOutputPool;
import org.olat.core.gui.render.URLBuilder;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.StringHelper;
import org.olat.core.util.filter.FilterFactory;
import org.olat.course.assessment.AssessmentHelper;
import org.olat.course.nodes.cl.model.Checkbox;
import org.olat.course.nodes.cl.model.CheckboxList;

/**
 * 
 * Initial date: 14.02.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class CheckListAssessmentDataModel extends DefaultFlexiTableDataModel<CheckListAssessmentRow>
	implements FilterableFlexiTableModel, SortableFlexiTableDataModel<CheckListAssessmentRow>,
	    ExportableFlexiTableDataModel {
	
	public static final int USER_PROPS_OFFSET = 500;
	public static final int CHECKBOX_OFFSET = 5000;
	
	private final CheckboxList checkboxList;
	private List<CheckListAssessmentRow> backupRows;
	
	public CheckListAssessmentDataModel(CheckboxList checkboxList, List<CheckListAssessmentRow> datas,
			FlexiTableColumnModel columnModel) {
		super(datas, columnModel);
		backupRows = datas;
		this.checkboxList = checkboxList;
	}
	
	/**
	 * @return The list of rows, not filtered
	 */
	public List<CheckListAssessmentRow> getBackedUpRows() {
		return backupRows;
	}

	@Override
	public DefaultFlexiTableDataModel<CheckListAssessmentRow> createCopyWithEmptyList() {
		return new CheckListAssessmentDataModel(checkboxList, new ArrayList<CheckListAssessmentRow>(), getTableColumnModel());
	}
	
	@Override
	public void sort(SortKey orderBy) {
		SortableFlexiTableModelDelegate<CheckListAssessmentRow> sorter
			= new SortableFlexiTableModelDelegate<>(orderBy, this, null);
		List<CheckListAssessmentRow> views = sorter.sort();
		super.setObjects(views);
	}

	@Override
	public MediaResource export(FlexiTableComponent ftC) {
		List<CheckListAssessmentRow> currentRows = getObjects();
		setObjects(backupRows);
		
		FlexiTableColumnModel columnModel = getTableColumnModel();
		int numOfColumns = columnModel.getColumnCount();
		List<FlexiColumnModel> columns = new ArrayList<>(numOfColumns);
		for(int i=0; i<numOfColumns; i++) {
			FlexiColumnModel column = columnModel.getColumnModel(i);
			String headerKey = column.getHeaderKey();
			if(!"edit.checkbox".equals(headerKey)) {
				columns.add(column);
			}
		}
		
		CheckListXlsFlexiTableExporter exporter = new CheckListXlsFlexiTableExporter();
		MediaResource resource = exporter.export(ftC, this, columns, ftC.getTranslator());
		//replace the current perhaps filtered rows
		super.setObjects(currentRows);
		return resource;
	}

	/**
	 * The filter apply to the groups
	 * @param key
	 */
	@Override
	public void filter(String filter) {
		setObjects(backupRows);
		
		Long groupKey = extractGroupKey(filter);
		if(groupKey != null) {
			List<CheckListAssessmentRow> filteredViews = new ArrayList<>();
			int numOfRows = getRowCount();
			for(int i=0; i<numOfRows; i++) {
				CheckListAssessmentRow view = getObject(i);
				if(accept(view, groupKey)) {
					filteredViews.add(view);
				}
			}
			super.setObjects(filteredViews);
		}
	}
	
	private Long extractGroupKey(String filter) {
		Long key = null;
		if(StringHelper.isLong(filter)) {
			try {
				key = Long.parseLong(filter);
			} catch (NumberFormatException e) {
				//
			}
		}
		return key;
	}
	
	private boolean accept(CheckListAssessmentRow view, Long groupKey) {
		boolean accept = false;
		Long[] groupKeys = view.getGroupKeys();
		if(groupKeys != null) {
			for(Long key:groupKeys) {
				if(groupKey.equals(key)) {
					accept = true;
				}
			}
		}
		return accept;
	}

	@Override
	public void setObjects(List<CheckListAssessmentRow> objects) {
		backupRows = objects;
		super.setObjects(objects);
	}

	@Override
	public Object getValueAt(int row, int col) {
		CheckListAssessmentRow box = getObject(row);
		return getValueAt(box, col);
	}
		
	@Override
	public Object getValueAt(CheckListAssessmentRow row, int col) {
		if(col == Cols.username.ordinal()) {
			return row.getIdentityName();
		} else if(col == Cols.totalPoints.ordinal()) {
			return row.getTotalPoints();
		} else if(col >= USER_PROPS_OFFSET && col < CHECKBOX_OFFSET) {
			int propIndex = col - USER_PROPS_OFFSET;
			return row.getIdentityProp(propIndex);
		} else if(col >= CHECKBOX_OFFSET) {
			int propIndex = col - CHECKBOX_OFFSET;
			
			if(row.getCheckedEl() != null) {
				//edit mode
				MultipleSelectionElement[] checked = row.getCheckedEl();
				if(checked != null && propIndex >= 0 && propIndex < checked.length) {
					return checked[propIndex];
				}
			}
			
			Boolean[] checked = row.getChecked();
			if(checked != null && propIndex >= 0 && propIndex < checked.length
					&& checked[propIndex] != null && checked[propIndex].booleanValue()) {
				return checked[propIndex];
			}
			return null;
		}
		return row;
	}
	
	public enum Cols {
		username("username"),
		totalPoints("points");
		
		private final String i18nKey;
		
		private Cols(String i18nKey) {
			this.i18nKey = i18nKey;
		}
		
		public String i18nKey() {
			return i18nKey;
		}
	}
	
	private static class CheckListXlsFlexiTableExporter {
		private static final URLBuilder ubu = new EmptyURLBuilder();
		
		private CellStyle headerCellStyle;
		private CheckListAssessmentDataModel dataModel;

		public MediaResource export(FlexiTableComponent ftC, CheckListAssessmentDataModel dataModel,
				List<FlexiColumnModel> columns, Translator translator) {
			Workbook wb = new HSSFWorkbook();
			headerCellStyle = XlsFlexiTableExporter.getHeaderCellStyle(wb);
			this.dataModel = dataModel;
			
			Sheet exportSheet = wb.createSheet("Sheet 1");
			createHeader(columns, translator, exportSheet);
			createData(ftC, columns, translator, exportSheet);
			
			return new WorkbookMediaResource(wb);
		}

		private void createHeader(List<FlexiColumnModel> columns, Translator translator, Sheet sheet) {
			Row headerRow = sheet.createRow(0);
			int pos = 0;
			for (int c=0; c<columns.size(); c++) {
				FlexiColumnModel cd = columns.get(c);
				String headerVal = cd.getHeaderLabel() == null ?
						translator.translate(cd.getHeaderKey()) : cd.getHeaderLabel();
				
				Cell cell = headerRow.createCell(pos++);
				cell.setCellValue(headerVal);
				cell.setCellStyle(headerCellStyle);
				
				if(cd.getColumnIndex() >= CHECKBOX_OFFSET) {
					int propIndex = cd.getColumnIndex() - CHECKBOX_OFFSET;
					Checkbox box = dataModel.checkboxList.getList().get(propIndex);
					if(box.getPoints() != null && box.getPoints().floatValue() > 0f) {
						Cell cellPoints = headerRow.createCell(pos++);
						cellPoints.setCellValue("");
						cellPoints.setCellStyle(headerCellStyle);
					}
				}
			}
		}

		private void createData(FlexiTableComponent ftC, List<FlexiColumnModel> columns, Translator translator, Sheet sheet) {
			int numOfRow = dataModel.getRowCount();
			int numOfColumns = columns.size();
			
			for (int r=0; r<numOfRow; r++) {
				int pos = 0;
				Row dataRow = sheet.createRow(r+1);
				for (int c = 0; c<numOfColumns; c++) {
					FlexiColumnModel cd = columns.get(c);
					Cell cell = dataRow.createCell(pos++);
					Object value = dataModel.getValueAt(r, cd.getColumnIndex());
					
					if(cd.getColumnIndex() >= CHECKBOX_OFFSET) {
						int propIndex = cd.getColumnIndex() - CHECKBOX_OFFSET;
						Checkbox box = dataModel.checkboxList.getList().get(propIndex);
						
						boolean checked;
						if(value instanceof Boolean) {
							checked = ((Boolean)value).booleanValue();
						} else {
							checked = false;
						}
						String checkVal = checked ? "x" : "";
						cell.setCellValue(checkVal);
						
						if(box.getPoints() != null && box.getPoints().floatValue() > 0f) {
							Cell cellPoints = dataRow.createCell(pos++);
							CheckListAssessmentRow assessmentRow = dataModel.getObject(r);
							Float[] scores = assessmentRow.getScores();
							if(checked && scores != null && scores.length > 0 && propIndex < scores.length) {
								String val = AssessmentHelper.getRoundedScore(scores[propIndex]);
								cellPoints.setCellValue(val);
							}
						}
					} else {
						renderCell(cell, value, r, ftC, cd, translator);
					}
				}
			}
		}
		
		protected void renderCell(Cell cell,Object value, int row, FlexiTableComponent ftC, FlexiColumnModel cd, Translator translator) {
			if(value instanceof Boolean) {
				Boolean val = (Boolean)value;
				String cellValue = val.booleanValue() ? "x" : "";
				cell.setCellValue(cellValue);
			} else {
				StringOutput so = StringOutputPool.allocStringBuilder(1000);
				cd.getCellRenderer().render(null, so, value, row, ftC, ubu, translator);
	
				String cellValue = StringOutputPool.freePop(so);
				cellValue = StringHelper.stripLineBreaks(cellValue);
				cellValue = FilterFactory.getHtmlTagsFilter().filter(cellValue);
				if(StringHelper.containsNonWhitespace(cellValue)) {
					cellValue = StringEscapeUtils.unescapeHtml(cellValue);
				}
				cell.setCellValue(cellValue);
			}
		}
		
	}
}
