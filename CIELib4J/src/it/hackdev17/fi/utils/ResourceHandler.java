package it.hackdev17.fi.utils;

import java.io.Closeable;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ResourceHandler {

	private static final Logger LOG = Logger.getLogger(ResourceHandler.class.getName());

	public static void safeClose(Closeable res) {
		if(res == null){
			return;
		}
		try {
			res.close();
		} catch (IOException e) {
			LOG.log(Level.SEVERE, e.getMessage(), e);
		}
	}

}
