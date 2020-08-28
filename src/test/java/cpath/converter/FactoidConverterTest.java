package cpath.converter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.Model;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.junit4.SpringRunner;

import cpath.service.CPathUtils;
import cpath.service.Settings;
import cpath.service.api.Converter;

@RunWith(SpringRunner.class)
@EnableConfigurationProperties(Settings.class)
public class FactoidConverterTest {

	
	@Autowired
	private Settings cpath;
	
	@Test
	public void testConvert() throws IOException {
		// convert test data
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		Converter converter = CPathUtils.newConverter("cpath.converter.FactoidConverter");
		converter.setXmlBase(cpath.getXmlBase());

		ZipFile zf = new ZipFile(getClass().getResource("/factoid.zip").getFile());
		ZipEntry ze = zf.entries().nextElement();

		converter.convert(zf.getInputStream(ze), bos);

		Model model = new SimpleIOHandler().convertFromOWL(new ByteArrayInputStream(bos.toByteArray()));
		assertNotNull(model);
		assertFalse(model.getObjects().isEmpty());
	}
}
