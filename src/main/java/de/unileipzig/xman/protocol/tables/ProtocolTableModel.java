package de.unileipzig.xman.protocol.tables;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Locale;

import org.olat.core.gui.components.table.DefaultColumnDescriptor;
import org.olat.core.gui.components.table.DefaultTableDataModel;
import org.olat.core.gui.components.table.TableController;
import org.olat.core.id.UserConstants;
import org.olat.core.util.Formatter;
import org.olat.core.util.Util;

import de.unileipzig.xman.protocol.Protocol;
import de.unileipzig.xman.studyPath.StudyPath;
import de.unileipzig.xman.studyPath.StudyPathManager;

public class ProtocolTableModel extends DefaultTableDataModel<Protocol> {
	
	private List<Protocol> entries;
	private int COLUMN_COUNT;
	private Locale locale;
	private boolean showScores;
	private boolean showExamName;
	private boolean showEsfLink;
	private boolean showVCardLink;

	public static final String COMMAND_VCARD = "show.vcard";
	public static final String EXAM_LAUNCH = "launch.exam";
	public static final String ESF_OPEN = "launch.esf";

	/**
	 * default constructor for this table model
	 * 
	 * @param locale - the local of the user
	 * @param protocols - the list of protocols to display
	 * @param showScores - show the column grades
	 * @param showExamName - the the column examName
	 * @param showEsfLink - make institutional number open esf
	 * @param showVCardLink - make username open vcard
	 */
	public ProtocolTableModel(Locale locale, List<Protocol> protocols, boolean showScores, boolean showExamName, boolean showEsfLink, boolean showVCardLink) {
		super(protocols);
		
		this.locale = locale;
		this.showScores = showScores;
		this.showExamName = showExamName;
		this.showEsfLink = showEsfLink;
		this.showVCardLink = showVCardLink;
		this.entries = protocols; 
		this.COLUMN_COUNT = showScores ? 8 : 7;
		if ( showExamName ) this.COLUMN_COUNT++;
	}
	
	public int getColumnCount() {
		
		return COLUMN_COUNT;
	}

	public int getRowCount() {
		
		return entries.size();
	}

	/**
	 * 
	 * @param row
	 * @return
	 */
	public Protocol getEntryAt(int row){
		
		return entries.get(row);
	}
	
	/**
	 * @see org.olat.core.gui.components.table.TableDataModel#getValueAt(int, int)
	 */
	public Object getValueAt(int row, int col) {
		
		Protocol proto = this.getEntryAt(row);
		switch(col) {
			case 0: return proto.getIdentity().getName();
			
			case 1: return proto.getIdentity().getUser().getProperty(UserConstants.INSTITUTIONALUSERIDENTIFIER , null);
			
			case 2: return proto.getIdentity().getUser().getProperty(UserConstants.LASTNAME, null) 
							+ ", " + proto.getIdentity().getUser().getProperty(UserConstants.FIRSTNAME, null);
			
			case 3: return proto.getIdentity().getUser().getProperty(UserConstants.STUDYSUBJECT, locale);
			
			case 4: return proto.getAppointment().getDate();
			
			case 5: return proto.getAppointment().getPlace();
			
			case 6: return proto.getGrade();
			
			//s. CommentEntryTableModel
			case 7: return proto.getComments();
			
			case 8: return proto.getExam().getName();
			
			default: return "";
			
		}
	}
	
	/**
	 * sets the entries of the table
	 * @param entries - a list of protocols
	 */
	public void setEntries(List<Protocol> entries){
		
		this.entries = entries;
		this.objects=entries;
	}
	
	/**
	 * initializes the table
	 */
	public void setTable(TableController tableCtr) {
		
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("ProtocolTableModel.header.login", 0, showVCardLink ? COMMAND_VCARD : null, locale));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("ProtocolTableModel.header.matrikel", 1, showEsfLink ? ESF_OPEN : null, locale));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("ProtocolTableModel.header.name", 2, null, locale));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("ProtocolTableModel.header.studyPath", 3, null, locale));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("ProtocolTableModel.header.date", 4, null, locale));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("ProtocolTableModel.header.loc", 5, null, locale));
		if ( this.showScores ) tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("ProtocolTableModel.header.mark", 6, null, locale));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("ProtocolTableModel.header.comment", 7, null, locale));
		if ( this.showExamName ) tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("ProtocolTableModel.header.examName", 8, EXAM_LAUNCH, locale));
	}
	
	/**
	 * Return a list of protocols for this bitset
	 * @param objectMarkers
	 * @return
	 */
	public List<Protocol> getProtocols(BitSet objectMarkers) {
		
		List<Protocol> results = new ArrayList<Protocol>();
		for( int i = objectMarkers.nextSetBit(0) ; i >= 0; i = objectMarkers.nextSetBit( i + 1 ) ) {
			Object elem = (Object) getObject(i);
			results.add((Protocol)elem);
		}
		return results;
	}

}