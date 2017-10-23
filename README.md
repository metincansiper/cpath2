# cPath2

[![Build Status](https://travis-ci.org/PathwayCommons/cpath2.svg?branch=master)](https://travis-ci.org/PathwayCommons/cpath2)

[![codebeat badge](https://codebeat.co/badges/cb01acef-739d-4926-a98e-9d5b46e2e252)](https://codebeat.co/projects/github-com-pathwaycommons-cpath2-master)

A pathway data integration and query platform for [Pathway Commons](http://www.pathwaycommons.org) (PC). 

The long-term vision is to achieve a complete computable map of the cell across all species and conditions. We aim to provide for the efficient exchange of pathway data; aggregation and integration of pathways from major available sources into a shared public information store; distribution in a value-added, standardized form; availability to the end-user via advanced internet web service technology; and dissemination of the technology of biological knowledge representation and pathway analysis by collaboration and training.

This effort builds on the recent emergence of a diverse and active community of pathway databases, the development in this community of a common language for biological pathway data exchange ([BioPAX](http://www.biopax.org)) and work in the Sander and Bader groups since late 2002 in building infrastructure for managing, integrating, and analyzing pathway information.


## Credits ###
[![IntelliJ IDEA](http://imagej.net/_images/thumb/1/1b/Intellij-idea.png/97px-Intellij-idea.png)](http://www.jetbrains.com/idea)

**Thanks**: to many open source projects and groups, such as Apache, Eclipse, Spring, etc., and, of course, - to GitHub.

**Funding**: "Pathway Commons: Research Resource for Biological Pathways"; Chris Sander, Gary Bader; 1U41HG006623 (formerly NIH P41HG04118)

## Configuration

### Working directory

Directory 'work' is where the configuration, data files and indices are saved.  

The directory may contain: 
- cpath2.properties (to configure modes, physical location, admin credentials, instance name, version, description, etc.);
- metadata.conf (describes the bio data to be imported/integrated);
- data/ directory (where original and intermediate data are stored);
- index/ (Lucene index directory for the final BioPAX model);
- downloads/ (where blacklist.txt and output data archives are created);
- logback.xml (can be enabled by -Dlogback.configurationFile=logback.xml JVM option);

In order to create a new cpath2 instance, run 

`cpath2.sh console` 

to execute one of the data integration stages: import metadata, 
clean, convert, normalize data, build the warehouse, the main BioPAX model, Lucene index, and downloads.

Once the instance is configured and data processed, run the web service using the same 
script as follows:

    nohup sh cpath2.sh server 2>&1 &

(watch the nohup.out and cpath2.log)

### Metadata

A cPath2 metadata configuration file is a plain text file (the default is metadata.conf) 
having the following format:
 - one data source definition per line;
 - blank lines and lines that begin with "#" are ignored (remarks);
 - there are exactly 11 columns; values are separated with tab (so each line has exactly 10 '\t' chars; 
   empty values (\t\t) are sometimes ok, e.g., when there is no Converter/Cleaner class.
 
The metadata columns are, in order: 
 1. IDENTIFIER - unique, short (40), and simple; spaces or dashes are not allowed;
 2. NAME - can contain one (must) or multiple provider names, separated 
 by semicolon, i.e.: [displayName;]standardName[;name1;name2;..];
 BIOPAX, PSI_MI, PSI_MITAB metadata entries should have at least the standard 
 official name, if possible (it is used for filtering in search/graph queries and by the data exporter);
 3. DESCRIPTION - free text: organization name, release date, web site, comments;
 4. URL to the Data - can be any URI (file://, http://, ftp://, classpath:). 
 It's just a memo, because original data usually needs re-packing.
 The cpath2 data fetcher looks for the data/IDENTIFIER.zip 
 input files (multi-entry zip archives are ok). How the data are then processed depends 
 on its TYPE (see below) and cleaner/converter implementations (if specified).
 So, as described above, a cPath2 Data Manager (a person) should  
 download, re-package, and save the required archives to data/
 (following the IDENTIFIER.zip naming convention) in advance.
 5. URL to the Data Provider's Homepage (optional, good to have)
 6. IMAGE URL (optional) - can be pointing to a resource logo;
 7. TYPE - one of: BIOPAX, PSI_MI, PSI_MITAB, WAREHOUSE, MAPPING;
 8. CLEANER_CLASS - leave empty or use a specific cleaner class name (like cpath.cleaner.internal.UniProtCleanerImpl);
 9. CONVERTER_CLASS - leave empty or use a specific converter class, 
 e.g., cpath.converter.internal.UniprotConverterImpl, cpath.converter.internal.PsimiConverterImpl;
 10. PUBMED ID - PubMed record ID (only number) of the main publication
 11. AVAILABILITY - values: 'free', 'academic', 'purchase'

A Converter or Cleaner implementation is not required to be implemented in the main cpath2 project sources. 
It's also possible to configure (metadata.conf) and plug into --premerge stage external 
cleaner/converter classes after the cpath2-cli.jar is released:
simply include to the cpath2-cli.sh Java options like: "-cp /path-to/MyDataCleanerImpl.class;/path-to/MyDataConverterImpl.class" 

### Data

One (a data manager) has to find, download, re-pack (zip) and put original 
biological pathway data files to the data/ sub-directory.

#### Warehouse data

Download UniProt, ChEBI, id-mapping tables into the data/ directory, e.g.:
 - `wget ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/taxonomic_divisions/uniprot_sprot_human.dat.gz`
 - `wget ftp://ftp.ebi.ac.uk/pub/databases/chebi/ontology/chebi.obo`
 - etc...

Re-pack/rename the data, i.e., make uniprot_human.zip, chebi.zip, etc.  
Filenames must be the corresponding metadata identifier, as in metadata.conf; 
only ZIP archive (can contain multiple files) is currently supported by cPath2 data importer 
(not .zip or no extension means the data will be read as is).

#### Pathway data 

Note: BioPAX L1, L2 models will be auto-converted to the BioPAX Level3. 

Prepare original BioPAX and PSI-MI/PSI-MITAB data archives in the 'data' folder as follows:
 - download (wget) original files or archives from the pathway resource (e.g., `wget http://www.reactome.org/download/current/biopax3.zip`) 
 - extract what you need (e.g. some species data only)
 - create a new zip archive using name like "IDENTIFIER.zip" (datasource identifier, e.g., reactome_human.zip).

### Run 

Set "cpath2.admin.enabled=true" in the cpath2.properties (or via "-Dcpath2.admin.enabled=true" java option).
To see available data import/export commands and options, run: 

`cpath2.sh console` 

The following sequence of the cpath2 tasks is normally required to build a new cPath2 instance from scratch: 
 - -metadata (sh cpath2.sh -metadata)
 - -premerge `[--buildWarehouse]`  
 - -merge
 - -index
 - -export (w/o arguments - default - generates blacklist.txt, etc., and a script to be run separately)

Extras/other steps (optional):
 - -run-analysis (to execute a class that implements cpath.dao.Analysis interface, 
  e.g., to post-fix something in the merged biopax model/db or to produce some output; 
  if it does modify the model though, i.e. not a read-only analysis, 
  you are to run -dbindex and following steps again.)
 - -export (to get a sub-model, using absolute URIs, e.g., to upload to a Virtuoso SPARQL server)
 - -pack (TODO; creates a tarball containing all the data to be uploaded somewhere on a file server)
