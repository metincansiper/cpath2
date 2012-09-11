package cpath.webservice;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import javax.imageio.ImageIO;

//import cpath.service.jaxb.*;
import cpath.warehouse.MetadataDAO;
import cpath.warehouse.beans.Metadata;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.validator.result.ValidatorResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


/**
 * 
 * @author rodche
 */
@Controller
public class MetadataController extends BasicController 
{
    
	private static final Log log = LogFactory.getLog(MetadataController.class);    
	
    private MetadataDAO service; // main PC db access
	
    public MetadataController(MetadataDAO service) {
		this.service = service;
	}
    
    @RequestMapping("/validation/{metadataId}/files.html") //a JSP view
    public String queryForValidations(@PathVariable String metadataId, Model model) 
    {
    	if (log.isInfoEnabled())
			log.info("Query - all validations (separate files) by datasource: " + metadataId);
    	
    	Map<Integer,String> result = service.getPathwayDataInfo(metadataId);
    	
    	model.addAttribute("identifier", metadataId);
    	if(result != null)
    		model.addAttribute("results", result.entrySet());
    	
    	return "validations";
    }
    
    
// replaced 
//    @RequestMapping("/validation/file/{pk}.html") // now view; the HTML is written to the response stream
//    public void queryForValidationByPkHtml(@PathVariable Integer pk, Writer writer, HttpServletResponse response) throws IOException 
//    {	
//    	ValidatorResponse resp = queryForValidationByPk(pk);
//    	//the xslt stylesheet exists in the biopax-validator-core module
//		Source xsl = new StreamSource((new DefaultResourceLoader())
//			.getResource("classpath:html-result.xsl").getInputStream());
//		response.setContentType("text/html");
//		BiopaxValidatorUtils.write(resp, writer, xsl); 
//    }
//     
//    @RequestMapping("/validation/file/{pk}") //XML report
//    public @ResponseBody ValidatorResponse queryForValidationByPk(@PathVariable Integer pk) 
//    {
//    	ValidatorResponse body = service.getValidationReport(pk);
//    	
//    	return body; //XML output (marshaled automatically)
//    }

    
    @RequestMapping("/validation/{key}")
    public @ResponseBody ValidatorResponse queryForValidation(@PathVariable String key) 
    {
    	if (log.isInfoEnabled())
			log.info("Getting a validation report for: " + key);
    	
    	//distinguish between a metadata and pathwayData primary key cases:
    	try {
    		Integer pk = Integer.parseInt(key);
    		return service.getValidationReport(pk);
    	} catch (NumberFormatException e) {}
    	
    	return service.getValidationReport(key);
    }

    
    @RequestMapping("/validation/{key}.html") //a JSP view
    public String queryForValidation(@PathVariable String key, Model model) 
    {
    	if (log.isInfoEnabled())
			log.info("Getting a validation report, as html, for:" + key);

    	ValidatorResponse body = queryForValidation(key);
		model.addAttribute("response", body);
		
		return "validationSummary";
    }
	
    
    @RequestMapping(value = "/logo/{identifier}")
    public  @ResponseBody byte[] queryForLogo(@PathVariable String identifier) throws IOException {
    	// try to get the metadata record by id:
    	Metadata ds = service.getMetadataByIdentifier(identifier);
    	byte[] bytes = null;
    	
    	if(ds != null) {
    		bytes = ds.getIcon();
    	} else {
    		for(Metadata m : service.getAllMetadata())
    			if(m.getUri().equalsIgnoreCase(identifier)) {
    				bytes = m.getIcon();
    				break;
    			}
    	}
    	
		if (bytes != null) {
			BufferedImage bufferedImage = ImageIO
					.read(new ByteArrayInputStream(bytes));
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			ImageIO.write(bufferedImage, "gif", byteArrayOutputStream);
			bytes = byteArrayOutputStream.toByteArray();
		}
        
        return bytes;
    }
       
    
    @RequestMapping(value = "/metadata")
    public  @ResponseBody Collection<Metadata> queryPathwayMetadata() {
    	if (log.isInfoEnabled())
			log.info("Getting pathway type Metadata.");
    	
    	return service.getAllMetadata();
    }

}