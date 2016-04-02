package com.sslv.services;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import com.sslv.model.AD;

public class SSLVServices {
	private static final SimpleDateFormat SIMPLE_DATE_FORMAT2 = new SimpleDateFormat("dd.MM.yyyy");

	private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm");

	static Logger logger = Logger.getLogger(SSLVServices.class);

	public static final String SEARCH_TYPE_SELL = "sell";
	public static final String SEARCH_TYPE_RENT = "hand_over";
	public static final String SEARCH_TYPE_BUY  = "buy";
	
	private static final String PAGE = "page";
	private static final String HTML = ".html";
	private static final String ID = "id";
	private static final String HREF = "href";
	private static final String PAGE_MAIN = "page_main";
	private static final String DOMAIN = "www.ss.lv";
	
	private static final String SEARCH_PATH = "/ru/real-estate/flats/%s/%s/";
	private static final String SEARCH_QUERY_VAR_NAME = "q";
	
	
	private static final String DB_PROVIDER = "127.0.0.1:9200";
	private static final String REPOSITORY = "sslv";

	class MyThread extends Thread {
		
		int index;
		
		String type;
		
		public MyThread(String type, int index) {
			this.index = index;
			this.type = type;
		}
		
		@Override
		public void run(){
			getPage(type, getPageURI(getSearchPath() + type + "/" + PAGE + index + HTML));
		}
	}
	
	private String getSearchPath(){
		String property = System.getProperty("scope");
		String scope = (property == null)?"all":property; 
		property = System.getProperty("location");
		String location = (property == null)?"riga":property; 
		return String.format(SEARCH_PATH, location, scope);

	}
	
	public AD[] search(String type) {
		//Get first page
		Set<AD> adss = new HashSet<>();
		parseRootPage(type, getSearchPath() + type + "/page1.html");

		return adss.toArray(new AD[0]);
	}

	public void parseRootPage(String type, String url){
		Document first = getPage(getPageURI(url));
		int max = getMaxPageNumber(first.select("a[name=nav_id]"));
		if(logger.isInfoEnabled()){
			logger.info(String.format("Found %s page(s)", max));
		}

		String property = System.getProperty("threads");
		ExecutorService executor = Executors.newFixedThreadPool((property!= null)?Integer.valueOf(property):10);
		for(int i = 0; i < max; i++){
			MyThread t = new MyThread(type, i+1);
			executor.execute(t);
		}
		executor.shutdown();
		while (!executor.isTerminated()) {
//			System.out.println(executor.isTerminated());
        }
		
	}

	public void parsePage(String type, Document page){
		Element root = page.select("form#filter_frm > table[align=center] > tbody").first();
		Elements childNodes = page.select("tr[id~=^tr_\\d]");
		page = null;
		if(logger.isInfoEnabled()){
			logger.info(String.format("Found %s post(s)", childNodes.size()));
		}
		Element tmpElement = null;
		Elements infoTable = null;
		Node tmpNode = null;
		String tmpString = null;
		
		for (Element node : childNodes) {
			String name = node.attr("id");
			String id = node.attr("id").substring(3);
			try {
				AD ad = new AD(name, DOMAIN + node.select("td[class=msg2] > div > a").attr(HREF));
				
				infoTable = node.select("td[class=msga2-o pp6]");
				
				//1. Cost
				tmpElement = infoTable.last();
				tmpString = concatenateNodes(tmpElement.childNodes());
				ad.setCost(CostParser.parse(tmpString));
				
				//2. Measure
				ad.setMeasure(StringEscapeUtils.unescapeHtml(tmpString.substring(tmpString.length() - 1)));
				
				//3. Location
				tmpNode = eval(infoTable.get(0));
				ad.setLocation(StringEscapeUtils.unescapeHtml(text((tmpNode instanceof TextNode)?tmpNode:eval(tmpNode))));

				//4. Rooms
				try {
					tmpString = concatenateNodes(extract(infoTable.get(1)));
					ad.setRooms(Integer.parseInt(tmpString));
				} catch (Exception e) {
					logger.debug("Can't parse number of rooms.", e);
				}
				
				//5. Square
				try {
					tmpString = concatenateNodes(extract(infoTable.get(2)));
					ad.setSquare(Double.parseDouble(tmpString));
				} catch (ClassCastException e) {
					logger.debug("Can't parse square.", e);
				}

				//6. Floor
				try {
					tmpString = concatenateNodes(extract(infoTable.get(3)));
					ad.setFloor(tmpString);
				} catch (ClassCastException e) {
					logger.debug("Can't parse number of floors.", e);
				}
				
				
				tmpElement = null;
				infoTable = null;
				tmpNode = null;
				tmpString = null;
				
				if(logger.isInfoEnabled()){
					logger.info(String.format("%s GET info: %s", ad.getId(), ad.getUrl()));
				}

				Document messagePage = getPage(getPageURI2(ad.getUrl()));
				tmpElement = messagePage.select("div#msg_div_msg").first();
				ad.setMessage(StringEscapeUtils.unescapeHtml(concatenateNodes(tmpElement.childNodes()).replace("\r", "").replace("\n", "")));

//				String rooms = tmpElement.select("td#tdo_1").text();
//				if(!"-".equals(rooms)){
//					ad.setRooms(Integer.parseInt(rooms));
//				}
//				ad.setSquare(Double.parseDouble(tmpElement.select("td#tdo_3").text()));
					
//				ad.setFloor(tmpElement.select("td#tdo_4").text());
				ad.setSeries(tmpElement.select("td#tdo_6").text());
				ad.setBuildingType(tmpElement.select("td#tdo_2").text());
				ad.setCity(tmpElement.select("td#tdo_20").text());
				ad.setArea(tmpElement.select("td#tdo_856").text());
				ad.setAddr(tmpElement.select("td#tdo_11").text().replace("[Карта]", "").trim());
				
				tmpString = tmpElement.select("td#tdo_11 > span.td15 > a.ads_opt_link_map").attr("onclick").split(";")[0];
				ad.setMap(DOMAIN + tmpString.substring(15, tmpString.length()-2));
				tmpString = null;
				
				ad.setCoordinates(ad.getMap().split("=")[3].split(","));
				
				Element date_element = messagePage.select("table#page_main > tbody > tr:eq(1) > td > table > tbody > tr:eq(1) > td:eq(1)").first();

				try {
					Date createdDate = null;
					createdDate = SIMPLE_DATE_FORMAT.parse(text(eval(date_element)).substring(6));
					ad.setCreated(createdDate);
				} catch (ParseException | NumberFormatException e) {
					logger.error("Error parsing creation date.", e);
				}
				ad.setSeries(text(eval(tmpElement.select("td[class=ads_opt]").get(6))));

				if(logger.isInfoEnabled()){
					logger.info(String.format("%s save", ad.getId()));
				}
				if(System.getProperty("debug") == null){
					try {
						HTTPClient.post("http://" + DB_PROVIDER + "/" + REPOSITORY + "/"+type+"/" + ad.getId(), ad);
					} catch (IOException e) {
						logger.error("http://" + DB_PROVIDER + "/" + REPOSITORY + "/"+type+"/" + ad.getId(), e);
					}
				}				
			} catch (Exception e) {
				logger.error(String.format("Exception while parsing post %s.", id), e);
			}

		}
	}
	
	private Node eval2(Element element) {
		return element.select(":last-child").first();
	}

//	private Node eval2(Element select) {
//		return select.select(":last-child");
//	}

	private List<Node> extract (Node node){
		return node.childNodes();
	}

	private static Node eval (Node node){
		if(node == null || node.childNodeSize() == 0){
			return null;
		}
		return node.childNode(0);
	}
	
	private static String text (Node node){
		return ((TextNode) node).getWholeText();
	}

	public String concatenateNodes(List<Node> nodes){
		StringBuffer sb = new StringBuffer();
		for (Node node : nodes) {
			if(node instanceof TextNode){
				sb.append(text(node));
			} else {
				sb.append(concatenateNodes(extract(node)));
			}
		}
		
		return sb.toString();
	}

	private static int getMaxPageNumber(Elements pagesNodes) {
		for (Node node : pagesNodes) {
			if(node instanceof Element && ((Element) node).tagName().equalsIgnoreCase("a") && node.attr("name").equalsIgnoreCase("nav_id") && node.attr("rel").equalsIgnoreCase("prev")){
				try {
					String href = node.attr(HREF);
					String[] path = href.split("/");
					String lastWord = path[path.length-1];
					
					String result = lastWord.split(HTML)[0].replace(PAGE, "");
					int val = Integer.parseInt(result);
					return val;
				} catch (Exception e) {
					logger.error(e);
				}
			}
		}

		return 1;
	}

	public URI getPageURI(String path){
    	URI uri = null;
		try {
			uri = HTTPClientProxy.getURIBuilder(DOMAIN).setPath(path).build();
		} catch (URISyntaxException e) {
			logger.error(e);
		}
		return uri;
	}

	public URI getPageURI2(String pathWithDomain){
    	URI uri = null;
		try {
			uri = HTTPClientProxy.getURIBuilder(pathWithDomain).build();
		} catch (URISyntaxException e) {
			logger.error(e);
		}
		return uri;
	}
	
	
	private Document getPage(String type, URI uri){
		Document page = null;
		if(logger.isInfoEnabled()){
			logger.info(String.format("Requesting page %s", uri.getRawPath()));
		}
		page = getPage(uri);
		if(logger.isInfoEnabled()){
			logger.info(String.format("Page received. Parsing %s", uri.getRawPath()));
		}
		parsePage(type, page);
		return page;
	}

	private Document getPage(URI uri){
		Document page = null;
		try {
			page = HTTPClientProxy.execute(uri);
		} catch (Exception e) {
			logger.error(e);
		}
		return page;
	}
	
	static class CostParser {
		public static double parse(String value){
			return Double.parseDouble(value.trim().substring(0, value.length()-1).replace(",", "").trim());
		}
		
	}
	
}
