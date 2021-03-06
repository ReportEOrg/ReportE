package org.reportbay.reporttemplate.domain;

import static org.reportbay.reporttemplate.domain.TemplateDiscriminatorConstants.AREA;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="cartesian_chart_template")
@DiscriminatorValue(AREA)
public class AreaChartTemplate extends CartesianChartTemplate{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
}