package org.reportbay.report.service.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.reportbay.common.dao.exception.BaseDAOException;
import org.reportbay.common.domain.SqlTypeEnum;
import org.reportbay.common.util.CommonUtils;
import org.reportbay.datasource.domain.ColumnMetadata;
import org.reportbay.datasource.domain.Datasource;
import org.reportbay.datasource.service.JdbcClient;
import org.reportbay.datasource.service.exception.JdbcClientException;
import org.reportbay.report.domain.AreaChartReport;
import org.reportbay.report.domain.BarChartReport;
import org.reportbay.report.domain.CartesianChartReport;
import org.reportbay.report.domain.ChartSeries;
import org.reportbay.report.domain.ColumnChartReport;
import org.reportbay.report.domain.CrossTabAttribute;
import org.reportbay.report.domain.CrossTabReport;
import org.reportbay.report.domain.LineChartReport;
import org.reportbay.report.domain.PieChartReport;
import org.reportbay.report.service.ReportGenerationService;
import org.reportbay.report.service.exception.ReportGenerationServiceException;
import org.reportbay.reporttemplate.dao.ReportQueryDAO;
import org.reportbay.reporttemplate.dao.ReportTemplateDAO;
import org.reportbay.reporttemplate.dao.exception.ReportQueryDAOException;
import org.reportbay.reporttemplate.dao.exception.ReportTemplateDAOException;
import org.reportbay.reporttemplate.domain.AreaChartTemplate;
import org.reportbay.reporttemplate.domain.BarChartTemplate;
import org.reportbay.reporttemplate.domain.BaseReportTemplate;
import org.reportbay.reporttemplate.domain.CartesianChartTemplate;
import org.reportbay.reporttemplate.domain.ColumnChartTemplate;
import org.reportbay.reporttemplate.domain.CrossTabTemplate;
import org.reportbay.reporttemplate.domain.CrossTabTemplateDetail;
import org.reportbay.reporttemplate.domain.LineChartTemplate;
import org.reportbay.reporttemplate.domain.PieChartTemplate;
import org.reportbay.reporttemplate.domain.ReportQuery;
import org.reportbay.reporttemplate.domain.ReportTemplateTypeEnum;
import org.reportbay.reporttemplate.domain.TemplateSeries;
import org.reportbay.reporttemplate.service.impl.CrossTabDetailsComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//stateless session bean
@Stateless
// container managed transaction manager
@TransactionManagement(TransactionManagementType.CONTAINER)
public class ReportGenerationServiceImpl implements ReportGenerationService {

	private final Logger LOG = LoggerFactory.getLogger(ReportGenerationServiceImpl.class);

	@Inject
	private ReportTemplateDAO reportTemplateDAO;

	@Inject
	private ReportQueryDAO reportQueryDAO;

	@Inject
	private JdbcClient jdbcClient;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AreaChartReport generateAreaChartReport(int reportTemplateId) throws ReportGenerationServiceException {
		return (AreaChartReport) generateCartesiantChartReport(reportTemplateId, new AreaChartReport());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AreaChartReport generateAreaChartReport(AreaChartTemplate reportTemplate) throws ReportGenerationServiceException {
		return (AreaChartReport) generateCartesiantChartReport(reportTemplate, new AreaChartReport());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BarChartReport generateBarChartReport(int reportTemplateId) throws ReportGenerationServiceException {
		return (BarChartReport) generateCartesiantChartReport(reportTemplateId, new BarChartReport());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BarChartReport generateBarChartReport(BarChartTemplate reportTemplate) throws ReportGenerationServiceException {
		return (BarChartReport) generateCartesiantChartReport(reportTemplate, new BarChartReport());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ColumnChartReport generateColumnChartReport(int reportTemplateId) throws ReportGenerationServiceException {
		return (ColumnChartReport) generateCartesiantChartReport(reportTemplateId, new ColumnChartReport());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ColumnChartReport generateColumnChartReport(ColumnChartTemplate reportTemplate) throws ReportGenerationServiceException {
		return (ColumnChartReport) generateCartesiantChartReport(reportTemplate, new ColumnChartReport());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public LineChartReport generateLineChartReport(int reportTemplateId) throws ReportGenerationServiceException {
		return (LineChartReport) generateCartesiantChartReport(reportTemplateId, new LineChartReport());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public LineChartReport generateLineChartReport(LineChartTemplate reportTemplate) throws ReportGenerationServiceException {
		return (LineChartReport) generateCartesiantChartReport(reportTemplate, new LineChartReport());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PieChartReport generatePieChartReport(int reportTemplateId) throws ReportGenerationServiceException {
		PieChartReport report = null;
		try {
			// 1. obtain the template
			PieChartTemplate pieChartTemplate = (PieChartTemplate) reportTemplateDAO.find(reportTemplateId);

			// 2. generate pie chart report based on template
			report = generatePieChartReport(pieChartTemplate);
		} catch (BaseDAOException bde) {
			throw new ReportGenerationServiceException("Failed to generate pie chart Report for " + reportTemplateId, bde);
		}

		return report;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PieChartReport generatePieChartReport(PieChartTemplate reportTemplate) throws ReportGenerationServiceException {
		PieChartReport report = null;
		try {
			// 1. populate report with template setting
			report = new PieChartReport();
			populatePieChartReport(report, reportTemplate);

			// 2. generate pie chart report
			populatePieChartReportCategoryResult(report, reportTemplate);
		} catch (JdbcClientException jce) {
			throw new ReportGenerationServiceException("Failed to generate pie char report for " + reportTemplate.getTemplateName(), jce);
		} catch (BaseDAOException bde) {
			throw new ReportGenerationServiceException("Failed to generate pie char report for " + reportTemplate.getTemplateName(), bde);
		}

		return report;
	}

	/**
	 * 
	 * @param report
	 * @param template
	 */
	private void populatePieChartReport(PieChartReport report, PieChartTemplate template) {
		// report name
		report.setReportName(template.getReportDisplayName());

		// chart title
		report.setTitle(template.getTitle());

		// show legend
		report.setShowLegend(template.isShowLegend());

		// show data label
		report.setShowDataLabel(template.isShowDataLabel());

		// data type for show data label case
		if (report.isShowDataLabel()) {
			report.setDataTypeFormat(template.getDataTypeFormat());
		}

		// initialize Category data map for later usage
		report.setCategoryData(new HashMap<String, Number>());

		report.setReportType(template.getReportTemplateType());
	}

	/**
	 * 
	 * @param report
	 * @param chartTemplate
	 * @throws ReportQueryDAOException
	 * @throws JdbcClientException
	 */
	private void populatePieChartReportCategoryResult(PieChartReport report, PieChartTemplate chartTemplate) throws ReportQueryDAOException, JdbcClientException {

		Map<String, Number> categoryDataMap = report.getCategoryData();

		ReportQuery reportQuery = chartTemplate.getReportQuery();

		if (reportQuery == null) {
			reportQuery = reportQueryDAO.find(chartTemplate.getId());
		}
		List<Map<ColumnMetadata, String>> resultList = jdbcClient.execute(reportQuery.getDatasource(), reportQuery.getQuery());

		String modelCategoryField = chartTemplate.getModelCategoryField();
		String modelDataField = chartTemplate.getModelDataField();

		// for each result row
		for (Map<ColumnMetadata, String> row : resultList) {
			String categoryName = null;
			Number categoryValue = null;

			// for each field in the row
			for (Map.Entry<ColumnMetadata, String> rowField : row.entrySet()) {
				String fieldLabel = rowField.getKey().getLabel();

				if (fieldLabel != null) {
					if (fieldLabel.equals(modelCategoryField)) {
						categoryName = rowField.getValue();
					} else if (fieldLabel.equals(modelDataField)) {
						categoryValue = convertToNumber(rowField.getKey().getTypeName(), rowField.getValue());
					}
				}
			}

			if (StringUtils.isNoneBlank(categoryName) && categoryValue != null) {
				categoryDataMap.put(categoryName, categoryValue);
			}
		}
	}

	/**
	 * 
	 * @param reportTemplateId
	 * @param report
	 * @return
	 * @throws ReportGenerationServiceException
	 */
	private CartesianChartReport generateCartesiantChartReport(int reportTemplateId, CartesianChartReport report) throws ReportGenerationServiceException {

		try {
			// 1. obtain the template
			CartesianChartTemplate chartTemplate = (CartesianChartTemplate) reportTemplateDAO.find(reportTemplateId);

			// 2. generate cartesian chart report based on template
			generateCartesiantChartReport(chartTemplate, report);
		} catch (BaseDAOException bde) {
			throw new ReportGenerationServiceException("Failed to generate cartesian chart report for " + reportTemplateId, bde);
		}

		return report;
	}

	/**
	 * 
	 * @param chartTemplate
	 * @param report
	 * @return
	 * @throws ReportGenerationServiceException
	 */
	private CartesianChartReport generateCartesiantChartReport(CartesianChartTemplate chartTemplate, CartesianChartReport report) throws ReportGenerationServiceException {

		try {
			// 1. populate report with template setting
			populateCartesianReport(report, chartTemplate);

			// 2. populate report with query result
			populateCartesianReportSeriesResult(report, chartTemplate);
		} catch (JdbcClientException jce) {
			throw new ReportGenerationServiceException("Failed to generate cartesian chart report for " + chartTemplate.getTemplateName(), jce);
		} catch (BaseDAOException bde) {
			throw new ReportGenerationServiceException("Failed to generate cartesian chart report for " + chartTemplate.getTemplateName(), bde);
		}

		return report;
	}

	/**
	 * 
	 * @param report
	 * @param chartTemplate
	 */
	private void populateCartesianReport(CartesianChartReport report, CartesianChartTemplate chartTemplate) {

		// Report name
		report.setReportName(chartTemplate.getReportDisplayName());

		// chart title
		report.setTitle(chartTemplate.getTitle());

		// X-Axis title
		report.setShowXAxis(chartTemplate.isShowXAxis());

		if (report.isShowXAxis()) {
			report.setXAxisTitle(chartTemplate.getXAxisTitle());
		}

		// Y-Axis title
		report.setShowYAxis(chartTemplate.isShowYAxis());

		if (report.isShowYAxis()) {
			report.setYAxisTitle(chartTemplate.getYAxisTitle());
		}

		// show data label
		report.setShowDataLabel(chartTemplate.isShowDataLabel());

		// show legend
		report.setShowLegend(chartTemplate.isShowLegend());

		// report type
		report.setReportType(chartTemplate.getReportTemplateType());
	}

	/**
	 * 
	 * @param report
	 * @param chartTemplate
	 * @throws JdbcClientException
	 * @throws ReportQueryDAOException
	 */
	private void populateCartesianReportSeriesResult(CartesianChartReport report, CartesianChartTemplate chartTemplate) throws JdbcClientException, ReportQueryDAOException {

		report.setChartDataSeries(new ArrayList<ChartSeries>());

		// for preview case, template will supply report query
		ReportQuery reportQuery = chartTemplate.getReportQuery();

		if (reportQuery == null) {
			reportQuery = reportQueryDAO.find(chartTemplate.getId());
		}

		List<Map<ColumnMetadata, String>> resultList = jdbcClient.execute(reportQuery.getDatasource(), reportQuery.getQuery());

		String modelDataLabelField = chartTemplate.getModelDataLabelField();
		String modelDataValueField = chartTemplate.getModelDataValueField();
		String modelSeriesGroupField = chartTemplate.getModelSeriesGroupField();

		Map<String, ChartSeries> seriesLookupMap = prepareSeriesLookupMap(report, chartTemplate);

		// for each result row
		for (Map<ColumnMetadata, String> row : resultList) {
			String seriesName = null;
			String dataLabel = null;
			Number dataValue = null;

			// for each field in the row
			for (Map.Entry<ColumnMetadata, String> rowField : row.entrySet()) {
				String fieldLabel = rowField.getKey().getLabel();

				if (fieldLabel != null) {
					// if the field belongs to dataLabel
					if (fieldLabel.equals(modelDataLabelField)) {
						dataLabel = rowField.getValue();
					} else if (fieldLabel.equals(modelDataValueField)) {
						dataValue = convertToNumber(rowField.getKey().getTypeName(), rowField.getValue());
					} else if (fieldLabel.equals(modelSeriesGroupField)) {
						seriesName = rowField.getValue();
					}
				}
			}

			// if all are valid
			if (StringUtils.isNoneBlank(seriesName) && StringUtils.isNoneBlank(dataLabel) && dataValue != null) {
				// lookup the series to store the value
				ChartSeries series = seriesLookupMap.get(seriesName);

				if (series != null) {
					series.getSeriesData().put(dataLabel, dataValue);
				}
			}
		}
	}

	/**
	 * convert the value according to type to java.lang.Number
	 * 
	 * @param valueType
	 * @param valueStr
	 * @return
	 */
	private Number convertToNumber(String valueType, String valueStr) {
		Number convertedNumber = null;

		SqlTypeEnum sqlType = SqlTypeEnum.fromString(valueType);

		if (sqlType != null && valueStr != null) {
			try {
				switch (sqlType) {
				case BIGINT:
					convertedNumber = new BigInteger(valueStr);
					break;
				case DECIMAL:
				case NUMERIC:
					convertedNumber = new BigDecimal(valueStr);
					break;
				case DOUBLE:
				case FLOAT:
					convertedNumber = Double.valueOf(valueStr);
					break;
				case INTEGER:
					convertedNumber = Integer.valueOf(valueStr);
					break;
				case REAL:
					convertedNumber = Float.valueOf(valueStr);
					break;
				case SMALLINT:
					convertedNumber = Short.valueOf(valueStr);
					break;
				case TINYINT:
					convertedNumber = Byte.valueOf(valueStr);
					break;
				default:
					break;
				}
			} catch (NumberFormatException nfe) {
				LOG.info("{} can't be converted to Number type {}" ,valueStr, valueType, nfe);
			}
		}

		return convertedNumber;
	}

	/**
	 * 
	 * @param report
	 * @param chartTemplate
	 * @return
	 */
	private Map<String, ChartSeries> prepareSeriesLookupMap(CartesianChartReport report, CartesianChartTemplate chartTemplate) {
		Map<String, ChartSeries> seriesLookupMap = new HashMap<String, ChartSeries>();

		// for each of the template defined series
		for (TemplateSeries series : chartTemplate.getDataSeries()) {
			// if not yet registered
			if (seriesLookupMap.get(series.getName()) == null) {
				// create a new entry and register
				ChartSeries chartSeries = new ChartSeries();
				// initialize with empty series data
				chartSeries.setSeriesData(new HashMap<String, Number>());
				chartSeries.setSeriesName(series.getName());
				seriesLookupMap.put(series.getModelSeriesValue(), chartSeries);

				report.getChartDataSeries().add(chartSeries);
			}
		}

		return seriesLookupMap;
	}

	@Override
	public List<String> getDataFieldValues(Datasource dataSource, String query) throws ReportGenerationServiceException {
		List<String> dataFieldValues = new ArrayList<String>();
		try {
			List<Map<ColumnMetadata, String>> resultList = jdbcClient.execute(dataSource, query);
			// for each result row
			for (Map<ColumnMetadata, String> row : resultList) {
				for (Map.Entry<ColumnMetadata, String> rowField : row.entrySet()) {
					if (rowField.getValue() != null && !"null".equals(rowField.getValue())) {
						dataFieldValues.add(rowField.getValue());
					}
				}
			}
		} catch (JdbcClientException e) {
			throw new ReportGenerationServiceException("Exception while getting data field values ", e);
		}
		return dataFieldValues;
	}

	@Override
	public Optional<CrossTabReport> generateCrossTabReport(int reportTemplateId) throws ReportGenerationServiceException {
		LOG.info("Generating CrossTab Report for the Template Id {} ",reportTemplateId);
		//Check the input for validity
		if (reportTemplateId<=0) {
			throw new IllegalArgumentException("Input ReportTemplate ID must be greater than 0");
		}
		try {
			BaseReportTemplate reportTemplate = reportTemplateDAO.find(reportTemplateId);
			if (reportTemplate instanceof CrossTabTemplate) {
				//TODO: to implement the return type
				Optional<CrossTabReport>  crossTabReport = generateCrossTabReport((CrossTabTemplate) reportTemplate);
				if (crossTabReport.isPresent()) {
					return crossTabReport;
				}else{
					//return empty object
					return Optional.empty();
				}
			}else {
				// throw the error when report template id fetch different template other then cross tab template
				throw new ReportGenerationServiceException("ReportTemplate Id must of be CrossTab Template");
			}
		} catch (ReportTemplateDAOException e) {
			throw new ReportGenerationServiceException("Error in generating CrossTab Report", e);
		} catch (Exception e) {
			throw new ReportGenerationServiceException("Error in generating CrossTab Report", e);
		}
	}

	@Override
	public Optional<CrossTabReport> generateCrossTabReport(CrossTabTemplate reportTemplate) throws ReportGenerationServiceException {
		LOG.info("Generating CrossTab Report for the Template {} ",reportTemplate);
		try {
			CommonUtils.checkForNull(reportTemplate, CrossTabTemplate.class.getSimpleName());
			CrossTabReport report = new CrossTabReport();
			report.setReportName(reportTemplate.getReportDisplayName());
			report.setReportType(ReportTemplateTypeEnum.SIMPLE);
			ReportQuery reportQuery = reportTemplate.getReportQuery();
			if (reportQuery==null) {
				reportQuery = reportQueryDAO.find(reportTemplate.getId());
			}
			if (reportQuery==null) {
				//Throw exception if report query is still null
				throw new NullPointerException("ReportQuery Object is Null");
			}
			//sort the crosstab according to user
			List<CrossTabTemplateDetail> templateDetails  = reportTemplate.getCrossTabDetail();
			Collections.sort(templateDetails,new CrossTabDetailsComparator());
			//Execute the query and get the result
			LOG.info("Executing the query {} ",reportQuery.getQuery());
			List<Map<ColumnMetadata, String>> resultList = jdbcClient.execute(reportQuery.getDatasource(), reportQuery.getQuery());
			if (CollectionUtils.isNotEmpty(resultList)) {
				//verify the column metadata with crosstab template name
				Map<ColumnMetadata, String> row= resultList.get(0);
//				List<ColumnMetadata> metaDatas = new ArrayList<ColumnMetadata>();
				for (CrossTabTemplateDetail detail : templateDetails) {
					boolean matchFound = false;
					//Add individual ColumnMetadata to the list
					for(Map.Entry<ColumnMetadata, String> map : row.entrySet()){
						ColumnMetadata metaData = map.getKey();
						if (detail.getModelAttributeName().equalsIgnoreCase(metaData.getLabel())) {
							matchFound=true;
							LOG.info("Column Label Matched {} ", metaData.getLabel());
							CrossTabAttribute attribute = new CrossTabAttribute();
							attribute.setAttributeDisplaySequence(detail.getAttributeDisplaySequence());
							attribute.setFieldType(detail.getFieldType());
							attribute.setGroupOrAggregate(detail.getGroupOrAggregate());
							attribute.setMetaData(metaData);
							attribute.setType(detail.getSqltype());
							report.getAttributes().add(attribute);
							//Break from the loop if match is found
							break;
						}
					}
					if (!matchFound) {
						LOG.error("Unable to Find Column Label against Attribute Mapping");
					}
				}
				for (Map<ColumnMetadata, String> list : resultList) {
					//Store the resultset from query in the order which can be access. Linked HashMap maintain the insertion order
					Map<String, String> rowSet = new LinkedHashMap<String, String>();
					for (Map.Entry<ColumnMetadata, String> map: list.entrySet()) {
						String columnName = map.getKey().getLabel();
						rowSet.put(columnName, map.getValue());
					}
					//Add each row set value to the list and the map will maintain insertion order
					report.getResultSet().add(rowSet);
				}
				return Optional.of(report);
			}else{
				LOG.warn("ResultSet is empty for the query {} ",reportQuery.getQuery());
				return Optional.empty();
			}
		} catch (ReportQueryDAOException e) {
			throw new ReportGenerationServiceException("Error in generating CrossTab Report",e);
		} catch (JdbcClientException e) {
			throw new ReportGenerationServiceException("Error in generating CrossTab Report",e);
		}
	}

}
