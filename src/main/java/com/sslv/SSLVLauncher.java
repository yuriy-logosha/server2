package com.sslv;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import com.sslv.common.Constants;
import com.sslv.facade.Helper;
import com.sslv.services.SearchPageParser;

public class SSLVLauncher {

	static Logger logger = Logger.getLogger(SSLVLauncher.class);
	private static final String DOMAIN = "www.ss.lv";
	private static final String SEARCH_PATH = "/ru/real-estate/flats/%s/%s/";
	
	public static void main(String[] args) {
		
		String searchType = args[0];

		String url = DOMAIN + getSearchPath() + searchType + "/page%s.html";
		SearchPageParser p = new SearchPageParser(searchType, String.format(url, 1));
		Document firstPage = Helper.getPage(String.format(url, 1));
		int max = getMaxPageNumber(firstPage.select("a[name=nav_id]"));
		if(logger.isInfoEnabled()){
			logger.info(String.format("Found %s page(s)", max));
		}
		p.parsePage(firstPage);
		
		String property = System.getProperty("threads");
		ExecutorService executor = Executors.newFixedThreadPool((property!= null)?Integer.valueOf(property):1);
		for(int i = 1; i < max; i++){
			Thread t = new Thread(new SearchPageParser(searchType, String.format(url, i+1)));
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
