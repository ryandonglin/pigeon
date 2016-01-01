package payne.framework.pigeon.core.formatting;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import payne.framework.pigeon.core.Header;
import payne.framework.pigeon.core.Invocation;
import payne.framework.pigeon.core.exception.FormatterException;
import payne.framework.pigeon.core.toolkit.IOToolkit;

public class XMLInvocationFormatter implements InvocationFormatter {
	private final JAXBContext context;

	public XMLInvocationFormatter() throws IOException, ClassNotFoundException, JAXBException {
		this("pigeon-jaxb.index");
	}

	public XMLInvocationFormatter(String path) throws IOException, ClassNotFoundException, JAXBException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		InputStream index = classLoader.getResourceAsStream(path);
		if (index == null) {
			throw new FileNotFoundException("file " + path + " not found in classpath");
		}
		InputStreamReader isr = null;
		BufferedReader br = null;
		try {
			Set<Class<?>> classes = new HashSet<Class<?>>();
			classes.add(Invocation.class);
			isr = new InputStreamReader(index);
			br = new BufferedReader(isr);
			String line = null;
			while ((line = br.readLine()) != null) {
				// 忽略空行和注释行
				if (line.trim().equals("") || line.trim().startsWith("#")) {
					continue;
				}
				Class<?> clazz = classLoader.loadClass(line.trim());
				classes.add(clazz);
			}
			this.context = JAXBContext.newInstance(classes.toArray(new Class<?>[classes.size()]));
		} finally {
			IOToolkit.close(br);
			IOToolkit.close(isr);
			IOToolkit.close(index);
		}
	}

	public XMLInvocationFormatter(JAXBContext context) {
		this.context = context;
	}

	public String algorithm() {
		return "application/xml";
	}

	public void serialize(Header header, Invocation data, OutputStream out, String charset) throws FormatterException {
		try {
			Marshaller marshaller = context.createMarshaller();
			marshaller.marshal(data, out);
		} catch (Exception e) {
			throw new FormatterException(e, this, data, null);
		}
	}

	public Invocation deserialize(Header header, InputStream in, String charset, Method method) throws FormatterException {
		try {
			Unmarshaller unmarshaller = context.createUnmarshaller();
			return (Invocation) unmarshaller.unmarshal(in);
		} catch (Exception e) {
			throw new FormatterException(e, this, in, method);
		}
	}

}