package cpath.service.jpa;


import org.springframework.data.repository.CrudRepository;


/**
 * A spring-data repository (auto-instantiated) of Metadata entities 
 * (all methods here follow the spring-data naming and signature conventions,
 * and therefore do not require to be implemented by us; these will be auto-generated).
 * 
 * @author rodche
 */
public interface MetadataRepository extends CrudRepository<Metadata, Long> {

	/**
	 * Get data provider's Metadata by identifier.
	 * 
	 * @param identifier
	 * @return
	 */
	Metadata findByIdentifier(String identifier);

}
