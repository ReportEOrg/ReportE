package org.reportbay.model.service;

import java.util.List;

import javax.ejb.Local;

import org.reportbay.model.domain.Model;
import org.reportbay.model.service.exception.ModelServiceException;

@Local
public interface ModelService {
	/**
	 * 
	 * @param model
	 * @return
	 * @throws ModelServiceException if there is any problem during the process.
	 * @throws IllegalArgumentException if <code>model</code> is <code>null</code>.
	 */
	Model save(Model model) throws ModelServiceException;
	/**
	 * 
	 * @param model
	 * @throws ModelServiceException if there is any problem during the process.
	 * @throws IllegalArgumentException if <code>model</code> is <code>null</code>.
	 */
	void update(Model model) throws ModelServiceException;
	/**
	 * 
	 * @param model
	 * @throws ModelServiceException if there is any problem during the process.
	 * @throws IllegalArgumentException if <code>model</code> is <code>null</code>.
	 */
	void delete(Model model) throws ModelServiceException;
	/**
	 * 
	 * @param id
	 * @return
	 * @throws ModelServiceException
	 */
	Model find(int id) throws ModelServiceException;
	/**
	 * 
	 * @return
	 * @throws ModelServiceException
	 */
	List<Model> findAll() throws ModelServiceException;
	
	/**
	 * 
	 * @return
	 * @throws ModelServiceException
	 */
	List<Model> findAllOrderByDatasourceName() throws ModelServiceException;

	/**
	 * 
	 * @param model
	 * @throws ModelServiceException
	 */
	void updateModelQueryFromJoinQuery(Model model) throws ModelServiceException;
	/**
	 * 
	 * @param model
	 * @throws ModelServiceException
	 */
	void updateModelQueryFromSimpleQuery(Model model) throws ModelServiceException;
	
	/**
	 * 
	 * @param model
	 * @param fieldName
	 * @return
	 * @throws ModelServiceException
	 */
	List<String> getModelFieldUniqueValue(Model model, String fieldName) throws ModelServiceException;
}
