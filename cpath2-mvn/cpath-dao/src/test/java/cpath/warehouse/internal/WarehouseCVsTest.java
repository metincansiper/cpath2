/**
 ** Copyright (c) 2009 Memorial Sloan-Kettering Cancer Center (MSKCC)
 ** and University of Toronto (UofT).
 **
 ** This is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** any later version.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** both UofT and MSKCC have no obligations to provide maintenance, 
 ** support, updates, enhancements or modifications.  In no event shall
 ** UofT or MSKCC be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** UofT or MSKCC have been advised of the possibility of such damage.  
 ** See the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this software; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA;
 ** or find it at http://www.fsf.org/ or http://www.gnu.org.
 **/

package cpath.warehouse.internal;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.biopax.paxtools.model.level3.*;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import psidev.ontology_manager.Ontology;
import psidev.ontology_manager.OntologyTermI;
import psidev.ontology_manager.impl.OntologyImpl;
import psidev.ontology_manager.impl.OntologyTermImpl;

/**
 * This tests are for CVs only (not using DAO);
 * other tests are in the cpath-importer module.
 * 
 * @author rodche
 *
 */
public class WarehouseCVsTest {

	static OntologyManagerCvRepository warehouse; // implements cpath.dao.WarehouseDAO
		
	static {
		Resource ont = new ClassPathResource("ontologies.xml");
		warehouse = new OntologyManagerCvRepository(ont, null);
	}
	
	@Test
	public void ontologyLoading() {
		Collection<String> ontologyIDs = warehouse.getOntologyIDs();
		Assert.assertTrue(ontologyIDs.contains("GO"));
		Assert.assertTrue(ontologyIDs.contains("SO"));
		Assert.assertTrue(ontologyIDs.contains("MI"));

		Ontology oa2 = warehouse.getOntology("GO");
		Assert.assertNotNull(oa2);
		Assert.assertTrue(oa2 instanceof OntologyImpl);

		oa2 = warehouse.getOntology("SO");
		Assert.assertNotNull(oa2);
		Assert.assertTrue(oa2 instanceof OntologyImpl);

		oa2 = warehouse.getOntology("MI");
		Assert.assertNotNull(oa2);
		Assert.assertTrue(oa2 instanceof OntologyImpl);
	}

	/**
	 * Test method for
	 * {@link cpath.warehouse.internal.OntologyManagerCvRepository#ontologyTermsToUrns(java.util.Collection)}
	 * .
	 */
	@Test
	public final void testOntologyTermsToUrns() {
		OntologyTermI term = new OntologyTermImpl("GO", "GO:0005654",
				"nucleoplasm");
		Set<OntologyTermI> terms = new HashSet<OntologyTermI>();
		terms.add(term);
		Set<String> urns = warehouse.ontologyTermsToUrns(terms);
		assertFalse(urns.isEmpty());
		assertTrue(urns.size() == 1);
		String urn = urns.iterator().next();
		assertEquals("urn:miriam:obo.go:GO%3A0005654", urn);
	}

	/**
	 * Test method for
	 * {@link cpath.warehouse.internal.OntologyManagerCvRepository#ontologyTermsToUrns(java.util.Collection)}
	 * .
	 */
	@Test
	public final void testSearchForTermByAccession() {
		OntologyTermI term = warehouse.findTermByAccession("GO:0005654");
		assertNotNull(term);
		assertEquals("nucleoplasm", term.getPreferredName());
	}

	@Test
	public final void testGetDirectChildren() {
		Set<String> dc = warehouse.getDirectChildren("urn:miriam:obo.go:GO%3A0005654");
		assertFalse(dc.isEmpty());
		assertTrue(dc.contains("urn:miriam:obo.go:GO%3A0044451"));
		System.out.println("DirectChildren:\n" + dc.toString() + " " +
			warehouse.getObject("urn:miriam:obo.go:GO%3A0044451", ControlledVocabulary.class));
	}

	@Test
	public final void testGetDirectAllChildren() {
		Set<String> dc = warehouse.getAllChildren("urn:miriam:obo.go:GO%3A0005654");
		assertFalse(dc.isEmpty());
		assertTrue(dc.contains("urn:miriam:obo.go:GO%3A0044451"));
		assertTrue(dc.contains("urn:miriam:obo.go:GO%3A0042555"));
		assertTrue(dc.contains("urn:miriam:obo.go:GO%3A0070847"));
		//System.out.println("AllChildren:\n" +dc.toString() + "; e.g., " +
		//		warehouse.getObject("urn:miriam:obo.go:GO%3A0042555", ControlledVocabulary.class));
	}

	@Test
	public final void testGetDirectParents() {
		Set<String> dc = warehouse.getDirectParents("urn:miriam:obo.go:GO%3A0005654");
		assertFalse(dc.isEmpty());
		assertTrue(dc.contains("urn:miriam:obo.go:GO%3A0031981"));
		System.out.println("DirectParents:\n" +dc.toString() + "; e.g., " +
			warehouse.getObject("urn:miriam:obo.go:GO%3A0031981", ControlledVocabulary.class));
	}

	@Test
	public final void testGetAllParents() {
		Set<String> dc = warehouse.getAllParents("urn:miriam:obo.go:GO%3A0005654");
		assertFalse(dc.isEmpty());
		assertTrue(dc.contains("urn:miriam:obo.go:GO%3A0031981"));
		assertTrue(dc.contains("urn:miriam:obo.go:GO%3A0044428"));
		assertTrue(dc.contains("urn:miriam:obo.go:GO%3A0044422"));
		//System.out.println("AllParents:\n" +dc.toString());
	}


	@Test // using correct ID
	public final void testGetObject() {
		CellularLocationVocabulary cv = warehouse.getObject(
				"urn:miriam:obo.go:GO%3A0005737",CellularLocationVocabulary.class);
		assertNotNull(cv);
	}
	
	@Test // using bad ID
	public final void testGetObject2() {
		CellularLocationVocabulary cv = warehouse.getObject(
				"urn:miriam:obo.go:GO%3A0005737X",CellularLocationVocabulary.class);
		assertNull(cv);
	}

}
