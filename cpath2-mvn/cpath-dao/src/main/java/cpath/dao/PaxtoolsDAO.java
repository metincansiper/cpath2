// $Id$
//------------------------------------------------------------------------------
/** Copyright (c) 2010 Memorial Sloan-Kettering Cancer Center.
 **
 ** This library is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** any later version.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** Memorial Sloan-Kettering Cancer Center
 ** has no obligations to provide maintenance, support,
 ** updates, enhancements or modifications.  In no event shall
 ** Memorial Sloan-Kettering Cancer Center
 ** be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** Memorial Sloan-Kettering Cancer Center
 ** has been advised of the possibility of such damage.  See
 ** the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this library; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 **/
package cpath.dao;

import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;

import cpath.dao.filters.SearchFilter;

import java.util.Collection;
import java.util.List;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;

/**
 * BioPAX data access (both model and repository).
 */
public interface PaxtoolsDAO extends Model, Reindexable {	
	
	/**
	 * Persists the given model to the db.
	 *
	 * @param biopaxFile File
	 * @param createIndex boolean
	 * @throws FileNoteFoundException
	 */
	void importModel(File biopaxFile) throws FileNotFoundException;

    
	 /**
	 * Returns the set of IDs 
	 * of the BioPAX elements that match the query 
	 * and satisfy the filters if any defined.
	 * 
     * @param query String
	 * @param filterByTypes - class filters for the full-text search (actual effect may depend on the concrete implementation!)
	 * @param extraFilters custom filters (implies AND in between) that apply after the full-text query has returned;
	 * 		  these can be used, e.g., for post-search filtering by organism or data source, anything...
	 * @return ordered by the element's relevance list of rdfIds
     */
    List<String> find(String query, Class<? extends BioPAXElement>[] filterByTypes,
    		SearchFilter<? extends BioPAXElement,?>... extraFilters);
    
    
	 /**
	 * Returns the count 
	 * of the BioPAX elements of given class
	 * that match the full-text query string.
	 * 
     * @param query String
	 * @param filterBy class to be used as a filter.
     * @return count
     */
    @Deprecated
    Integer count(String query, Class<? extends BioPAXElement> filterBy);
    
    
    /**
     * Exports the entire model (if no IDs are given) 
     * or a sub-model as BioPAX (OWL)
     * 
     * @param outputStream
     * @param ids (optional) build a sub-model from these IDs and export it
     */
    void exportModel(OutputStream outputStream, String... ids);   
    
    
    /**
     * Creates a "valid" sub-model from the BioPAX elements
     * 
     * @param ids a set of valid RDFId
     * @return
     */
    Model getValidSubModel(Collection<String> ids);
 
    
    /**
     * Gets deeply initialized BioPAX element, i.e., 
     * with all the properties' properties set for sure
     * (this matters when an implementation uses 
     * caching, transactions, etc.)
     * 
     * @param id
     * @return
     */
    @Deprecated
    BioPAXElement getByIdInitialized(String id);
 
    
    /**
     * Initializes the properties and inverse properties, 
     * including collections!
     * 
     */
    void initialize(Object element);

    
    /**
     * Merges (updates or adds) a BioPAX element to the model.
     * 
     * @param bpe
     */
    void merge(BioPAXElement bpe);

    
    /**
     * Executes custom algorithms (or plugins)
     * 
     * @param analysis defines a job/query to perform
     * @param args - optional parameters for the algorithm
     * @return
     */
    Model runAnalysis(Analysis analysis, Object... args);
    
}