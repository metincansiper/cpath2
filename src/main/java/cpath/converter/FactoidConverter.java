package cpath.converter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;

class FactoidConverter extends BaseConverter {
	public void convert(InputStream is, OutputStream os) {
		try {
			String encoding = "UTF-8";
			String str = IOUtils.toString(is, encoding);
			os.write(str.getBytes(Charset.forName(encoding)));
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
	}
}