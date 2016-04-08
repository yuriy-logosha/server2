package com.sslv.services;

import java.io.IOException;
import java.net.URI;

import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;

import com.sslv.model.AD;

public class Helper {
	final static Logger logger = Logger.getLogger(ADBuilder.class);

	public static Document getPage(final String url){
		Document page = null;
		try {
			page = HTTPClientProxy.execute(new URI("http://" + url));
		} catch (Exception e) {
			logger.error(url, e);
		}
		return page;
	}

	public static void save(final String url, final AD ad){
		try {
			HTTPClient.post(url, ad);
		} catch (IOException e) {
			logger.error(String.format("%s saving error. %s", ad.getId(), ad), e);
		}
	}

}
