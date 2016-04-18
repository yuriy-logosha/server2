package com.sslv.facade;

import java.io.IOException;
import java.net.URI;

import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sslv.model.AD;
import com.sslv.services.ADBuilder;

public class Helper {
	final static Logger logger = Logger.getLogger(ADBuilder.class);

	public static Document getPage(final String url) {
		Document page = null;
		try {
			page = HTTPClientProxy.get(new URI(url));
		} catch (Exception e) {
			logger.error(String.format("%s Get page error.", url), e);
		}
		return page;
	}

	public static void save(final String url, final AD ad) {
		try {
			HTTPClientProxy.put(new URI(url), ad);
		} catch (Exception e) {
			logger.error(String.format("%s saving error. %s %s", ad.getId(), url, ad), e);
		}

	}

}
