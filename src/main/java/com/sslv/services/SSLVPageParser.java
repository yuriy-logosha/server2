package com.sslv.services;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SSLVPageParser implements Runnable {

	final static Logger logger = Logger.getLogger(SSLVPageParser.class);

	private final String url;

	public SSLVPageParser(final String url) {
		this.url = url;
	}
	
	public void parsePage(final Document page){
		Elements childNodes = page.select("tr[id~=^tr_\\d]");
		ExecutorService executor = Executors.newFixedThreadPool(childNodes.size());
		for (Element node : childNodes) {
			Thread t = new Thread(new ADBuilder(node));
			executor.execute(t);
		}
		executor.shutdown();
		while (!executor.isTerminated()) {
        }
	}

	@Override
	public void run() {
		Document page = Helper.getPage(url);
		if(page != null){
			parsePage(page);
		}
	}
	
}
