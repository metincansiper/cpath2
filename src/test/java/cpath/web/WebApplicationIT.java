package cpath.web;

import cpath.service.api.CPathService;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@ActiveProfiles({"web"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebApplicationIT {

  @Autowired
  private TestRestTemplate template;

  @Autowired
	private CPathService service;

  @Before
	public void init() {
  	if(service.getModel()==null)
  		service.init();
	}

	@Test
	public void testGetTypes() {
		String result = template.getForObject("/help/types", String.class);
		assertNotNull(result);
		assertTrue(result.contains("{\"id\":\"types\",\"title\":\"BioPAX classes\""));
	}


	@Test
	public void testSearchPathway() {
		String result;
		result = template.getForObject("/search?type={t}&q={q}", String.class, "Pathway", "Gly*");
		//note: pathway and Pathway both works (both converted to L3 Pathway class)
		assertNotNull(result);
		assertTrue(result.contains("Pathway50"));
	}

	//HTTP GET
	@Test
	public void testGetQueryById() {
		String result = template.getForObject("/get?uri={uri}",
				String.class, "http://identifiers.org/uniprot/P27797");
    assertNotNull(result);
		assertTrue(result.contains("<bp:ProteinReference rdf:about=\"http://identifiers.org/uniprot/P27797\""));
	}

	@Test
	public void testGetNetworkToSbgn()  {
		String result = template.getForObject("/get?uri={uri}&format=sbgn",
			String.class, "http://www.biopax.org/examples/myExample#Pathway50");
		assertNotNull(result);
		assertTrue(result.contains("<glyph class=\"process\""));
	}

	@Test //if POST isn't disabled
	public void testPostQueryById() {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("uri", "http://identifiers.org/uniprot/P27797");
		String result = template.postForObject("/get", map, String.class);
    assertNotNull(result);
    assertTrue(result.contains("<bp:ProteinReference rdf:about=\"http://identifiers.org/uniprot/P27797\""));
//		assertTrue(result.contains("Method Not Allowed"));
	}

}
