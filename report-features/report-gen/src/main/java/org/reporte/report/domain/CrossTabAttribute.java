package org.reporte.report.domain;

import org.reporte.common.domain.SqlTypeEnum;
import org.reporte.datasource.domain.ColumnMetadata;
import org.reporte.reporttemplate.domain.CrossTabFieldType;
import org.reporte.reporttemplate.domain.GroupOrAggregate;

public class CrossTabAttribute {
	
	private GroupOrAggregate groupOrAggregate;
	private CrossTabFieldType fieldType;
	private int attributeDisplaySequence;
	private SqlTypeEnum type;
	private ColumnMetadata metaData;
	
	public GroupOrAggregate getGroupOrAggregate() {
		return groupOrAggregate;
	}
	public void setGroupOrAggregate(GroupOrAggregate groupOrAggregate) {
		this.groupOrAggregate = groupOrAggregate;
	}
	public CrossTabFieldType getFieldType() {
		return fieldType;
	}
	public void setFieldType(CrossTabFieldType fieldType) {
		this.fieldType = fieldType;
	}
	public int getAttributeDisplaySequence() {
		return attributeDisplaySequence;
	}
	public void setAttributeDisplaySequence(int attributeDisplaySequence) {
		this.attributeDisplaySequence = attributeDisplaySequence;
	}
	public SqlTypeEnum getType() {
		return type;
	}
	public void setType(SqlTypeEnum type) {
		this.type = type;
	}
	public ColumnMetadata getMetaData() {
		return metaData;
	}
	public void setMetaData(ColumnMetadata metaData) {
		this.metaData = metaData;
	}
	@Override
	public String toString() {
		return "CrossTabAttribute [groupOrAggregate=" + groupOrAggregate
				+ ", fieldType=" + fieldType + ", attributeDisplaySequence="
				+ attributeDisplaySequence + ", type=" + type + ", metaData="
				+ metaData + "]";
	}
}
