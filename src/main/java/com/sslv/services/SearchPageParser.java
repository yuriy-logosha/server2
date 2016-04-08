package com.sslv.services;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.sslv.facade.Helper;

public class SearchPageParser implements Runnable {

	final static Logger logger = Logger.getLogger(SearchPageParser.class);

	private final String url;

	private final String searchType;

	public SearchPageParser(final String searchType, final String url) {
		this.searchType = searchType;
		this.url = url;
	}
	
	public void parsePage(final Document page){
		Elements childNodes = page.select("tr[id~=^tr_\\d]");
//		ExecutorService executor = Executors.newFixedThreadPool(childNodes.size());
		for (Element node : childNodes) {
			(new ADBuilder(searchType, node)).run();
//			Thread t = new Thread(new ADBuilder(searchType, node));
//			executor.execute(t);
		}
//		executor.shutdown();
//		while (!executor.isTerminated()) {
//        }
	}

	@Override
	public void run() {
		Document page = Helper.getPage(url);
		if(page != null){
			parsePage(page);
		}
	}
	
}
