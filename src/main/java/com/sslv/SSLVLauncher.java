package com.sslv;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import com.sslv.services.SSLVPageParser;

import common.Constants;

public class SSLVLauncher {

	static Logger logger = Logger.getLogger(SSLVLauncher.class);
	private static final String DOMAIN = "www.ss.lv";
	private static final String SEARCH_PATH = "/ru/real-estate/flats/%s/%s/";
	
	public static void main(String[] args) {

		SSLVPageParser p = new SSLVPageParser("", "");
		String url = DOMAIN + getSearchPath() + Constants.SEARCH_TYPE_SELL + "/page%s.html";
		Document firstPage = p.getPage(String.format(url, 1));
		int max = getMaxPageNumber(firstPage.select("a[name=nav_id]"));
		if(logger.isInfoEnabled()){
			logger.info(String.format("Found %s page(s)", max));
		}
		
		String property = System.getProperty("threads");
		ExecutorService executor = Executors.newFixedThreadPool((property!= null)?Integer.valueOf(property):1);
		for(int i = 0; i < max; i++){
			Thread t = new Thread(new SSLVPageParser(Constants.SEARCH_TYPE_SELL, String.format(url, i+1)));
			executor.execute(t);
		}
		executor.shutdown();
		while (!executor.isTerminated()) {
        }
	
	}
	
	public static String getSearchPath(){
		return String.format(
				SEARCH_PATH, 
				(System.getProperty("location") == null)?"riga":System.getProperty("location"), 
						(System.getProperty("scope") == null)?"all":System.getProperty("scope"));
	}
	
	public static int getMaxPageNumber(final Elements pagesNodes) {
		for (Node node : pagesNodes) {
			if(node instanceof Element && ((Element) node).tagName().equalsIgnoreCase("a") && node.attr("name").equalsIgnoreCase("nav_id") && node.attr("rel").equalsIgnoreCase("prev")){
				try {
					String href = node.attr(Constants.HREF);
					String[] path = href.split("/");
					String lastWord = path[path.length-1];
					
					String result = lastWord.split(Constants.HTML)[0].replace(Constants.PAGE, "");
					int val = Integer.parseInt(result);
					return val;
				} catch (Exception e) {
					logger.error(e);
				}
			}
		}

		return 0;
	}
	
}
