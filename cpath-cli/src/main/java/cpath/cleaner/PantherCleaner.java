package cpath.cleaner;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import cpath.service.CPathUtils;
import org.biopax.paxtools.controller.Cloner;
import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.controller.SimpleEditorMap;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.model.level3.Process;
import org.biopax.paxtools.util.ClassFilterSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpath.service.Cleaner;

/**
 * Cleaner for the PANTHER Pathway 3.4.1 BioPAX model
 * 
 * Remove non-yeast entity references. Add "dangling" Controls to Pathways.
 */
final class PantherCleaner implements Cleaner {
	
    private static Logger log = LoggerFactory.getLogger(PantherCleaner.class);

    
    public void clean(InputStream data, OutputStream cleanedData)
	{	
		SimpleIOHandler simpleReader = new SimpleIOHandler(BioPAXLevel.L3);
		Model originalModel = simpleReader.convertFromOWL(data);

		//get yeast BioSource (559292); it's already there normalized - uses Identifiers.org URI
		BioSource bioSource = (BioSource) originalModel.getByID("559292");
		if(bioSource == null) {
			//fail shortly (importer must skip this dataFile)
			throw new RuntimeException("Yeast data (BioSource 559292) not found.");
		}

		// fix the taxonomy xref.db standard name
		bioSource.getXref().iterator().next().setDb("Taxonomy");
		
		//Remove/replace non-yeast BioSources, SequenceEntityReferences
		//Pathways all have organism=null; let's set 'yeast' for all
		//Use parallel execution
		ExecutorService exec = Executors.newFixedThreadPool(3);
		//make a copy of all model's objects set to avoid concurrent modification exceptions.
		final Set<BioPAXElement> objects = new HashSet<>(originalModel.getObjects());
		final Model model = originalModel;
		exec.execute(() -> {
			for(BioPAXElement o : objects) {
				if((o instanceof BioSource) && !bioSource.equals(o)) {
					model.remove(o);
				}
			}
		});
		exec.execute(() -> {
			for(SequenceEntityReference er : new ClassFilterSet<>(objects, SequenceEntityReference.class)) {
				if(er.getOrganism() != null && !er.getOrganism().equals(bioSource)) {
					model.remove(er);
					for(SimplePhysicalEntity spe : new HashSet<>(er.getEntityReferenceOf()))
						spe.setEntityReference(null);
					for(EntityReference generic : new HashSet<>(er.getMemberEntityReferenceOf()))
						generic.removeMemberEntityReference(er);
				}
			}
		});
		exec.execute(() -> {
			for(Pathway p : new ClassFilterSet<>(objects, Pathway.class)) {
				if(p.getUri().startsWith("http://identifiers.org/panther.pathway/")
						|| !p.getPathwayComponent().isEmpty()
						|| !p.getPathwayOrder().isEmpty()) { //- seems they don't use pathwayOrder property, anyway
					p.setOrganism(bioSource);
				} else //black box and no-components pathways
					p.setOrganism(null); //clear in case it's set to other org.
			}
		});
		exec.execute(() -> {
			// remove ALL "PANTHER Pathway" and "PANTHER Pathway Component" xrefs
			// (they collide with UniProt IDs like P01234 and confuse id-mapping, full-text search)
			for(BioPAXElement o : objects) {
				if((o instanceof Xref) && CPathUtils
						.startsWithAnyIgnoreCase(String.valueOf(((Xref)o).getDb()),"panther pathway"))
				{
					model.remove(o);
				}
			}
		});
		exec.shutdown(); //accept no more tasks
		try {//wait
			exec.awaitTermination(120, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted!", e);
		}
		
		//clone the model (to actually get rid of removed objects in all object properties)
		final Model cleanModel = (new Cloner(SimpleEditorMap.L3, BioPAXLevel.L3.getDefaultFactory()))
				.clone(originalModel, originalModel.getObjects());		
		log.info((objects.size()-cleanModel.getObjects().size())
				+ " non-yeast objects (and all corresponding properties) were cleared.");

		if(cleanModel.getObjects(BioSource.class).size() != 1)
			throw new IllegalStateException("There are still several BioSource objects");
		
		//replace generic ERs that have only one non-generic memberER with that member
		//(should not be nested generics; if so, won't replace those)
		for(SequenceEntityReference generic : new HashSet<>(
				cleanModel.getObjects(SequenceEntityReference.class)))
		{
			if(generic.getMemberEntityReference().size() != 1)
				continue; //non-generic or non-trivial
			
			// check for nested generic ERs
			if(!generic.getMemberEntityReferenceOf().isEmpty()) {
				log.warn("Found nested generic " + generic + "; skip it.");
				continue;
			}
			
			// Use that single member ER instead of this generic ER;
			// copy names and xrefs
			EntityReference member = generic.getMemberEntityReference().iterator().next();
			
			//PANTHER contains DnaRegionReferences having ProteinRefrence members (wrong BioPAX model)
			if(generic.getModelInterface().isInstance(member)) { //correct classes, then -
//				member.getComment().addAll(generic.getComment()); //skip misleading/old comments
				member.getComment().add("REPLACED generic " + generic.getUri());
				member.getXref().addAll(generic.getXref());
				member.getName().addAll(generic.getName());
				if(member.getDisplayName() == null)
					member.setDisplayName(generic.getDisplayName());
				if(member.getStandardName() == null)
					member.setStandardName(generic.getStandardName());
				// replace
				for(SimplePhysicalEntity spe : new HashSet<>(generic.getEntityReferenceOf()))
					spe.setEntityReference(member);
				
				generic.removeMemberEntityReference(member);				
				cleanModel.remove(generic);
			} else if(generic instanceof DnaReference || generic instanceof RnaReference
					|| generic instanceof SmallMoleculeReference || generic instanceof ProteinReference)
			{// it's in fact allowed, except for these constraints:
			// R:RnaReference=RnaReference R:SmallMoleculeReference=SmallMoleculeReference
			// R:DnaReference=DnaReference R:ProteinReference=ProteinReference
				log.error(generic.getModelInterface().getSimpleName() + ", uri:"
					+ generic.getUri() + ", has a member ER of incompatible type: "
					+ member.getModelInterface().getSimpleName());
			}
		}
				
		//Add "dangling" Catalyses/Controls to related Pathways (to pathwayComponent) where controlled reaction belong
		for(Control c : cleanModel.getObjects(Control.class)) {
			for(Process process : c.getControlled()) {
				for(Pathway pathway : process.getPathwayComponentOf()) {
					pathway.addPathwayComponent(c);
				}
			}
		}
		
		//cleanup (remove above replaced generics, etc.)
		ModelUtils.removeObjectsIfDangling(cleanModel, UtilityClass.class);
		
		// convert model back to OutputStream for return
		try {
			simpleReader.convertToOWL(cleanModel, cleanedData);
		} catch (Exception e) {
			throw new RuntimeException("Failed writing cleaned PANTHER BioPAX model to RDF/XML", e);
		}
	}

}
