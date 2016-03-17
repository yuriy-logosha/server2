package com.sslv.services;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EncodingUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import com.sslv.model.AD;

public class SSLVServices {
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
	
	private static final String SEARCH_PATH = "/ru/real-estate/flats/riga/all/";
	private static final String SEARCH_QUERY_VAR_NAME = "q";
	
	
	private static final String DB_PROVIDER = "127.0.0.1:9200";
	private static final String REPOSITORY = "twitter";

	class MyThread extends Thread {
		
		int index;
		
		String type;
		
		public MyThread(String type, int index) {
			this.index = index;
			this.type = type;
			Thread.currentThread().setName("Page"+index);
		}
		
		@Override
		public void run(){
			try {
				getPage(type, getPageURI(SEARCH_PATH + type + "/" + PAGE + index + HTML));
				//HTTPClient.post("http://127.0.0.1:9200/sslv/" + type + "/" + ad.getId(), ad);
//wait(999999);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
	}
	
	public AD[] search(String type, String searchCriteria) {
		//Get first page
		Set<AD> adss = new HashSet<>();
		parseRootPage(type, SEARCH_PATH + type + "/page1.html");

		return adss.toArray(new AD[0]);
	}

	public void parseRootPage(String type, String url){
		Document first = getPage(getPageURI(url));
		int max = getMaxPageNumber(first.select("a[name=nav_id]"));
		MyThread[] arr = new MyThread[max];
		for(int i = 0; i < max; i++){
			MyThread t = new MyThread(type, i+1);
			arr[i] = t;
			t.start();
		}
		boolean asble = true;
		do{
		for(int i = 1; i <= max; i++){
			
			if(arr[i].isAlive()){
				asble = true;
				try {
					arr[i].join(3000);
				} catch (InterruptedException e) {
					logger.debug(e);
				}
			} else
				asble = false;
			
		}
		}while(asble);
		
	}

	public void parsePage(String type, Document page){
		Element root = page.select("form#filter_frm > table[align=center] > tbody").first();
		Elements childNodes = page.select("tr[id~=^tr_\\d]");

		for (Node node : childNodes) {
			if( node.getClass().equals(Element.class) && node.hasAttr(ID) && !node.attr(ID).equalsIgnoreCase("head_line")){
				AD ad = new AD();

				Element adBody = ((Element)node).select("td[class=msg2]").first();
				Elements select = adBody.select("div > a");
				ad.setUrl(select.attr(HREF));
				ad.setName(select.attr(ID));
				ad.setId(new Long(ad.getName().replace("dm_", "")));
				
				Element costEl = root.select("tr#tr_"+ad.getId()+">td:eq(8)").first();
				String costValue = "";
				String costMeasure = "";
				if(costEl.childNodes().size() > 1){
					costValue = text(eval(eval(costEl))).replace(",", "");
					costMeasure = text(costEl.childNode(1)).trim();
				} else {
					String[] costArr;
					costArr = text(eval(costEl)).replace(",", "").split(" ");
					costValue = costArr[0];
					costMeasure = costArr[costArr.length-1];
				}
				try {
					ad.setCost(Double.parseDouble(costValue));
				} catch (Exception e) {
					e.printStackTrace();
				}
				ad.setMeasure(StringEscapeUtils.unescapeHtml(costMeasure));
				
				Elements adInfo = ((Element)node).select("td[class=msga2-o pp6]");
				
				//1. Location
				Element locationNode = adInfo.get(0);
				String location = text((eval(locationNode) instanceof TextNode)?eval(locationNode):eval(eval(locationNode)));

				ad.setLocation(StringEscapeUtils.unescapeHtml(location));
				 
				Document messagePage = getPage(getPageURI(ad.getUrl()));
				Element message = messagePage.select("div#msg_div_msg").first();
				ad.setMessage(StringEscapeUtils.unescapeHtml(concatenateNodes(message.childNodes()).replace("\r", "").replace("\n", "")));
				Element date_element = messagePage.select("table#page_main > tbody > tr:eq(1) > td > table > tbody > tr:eq(1) > td:eq(1)").first();
				try {
					Date createdDate = (new SimpleDateFormat("dd.MM.yyyy HH:mm")).parse(text(eval(date_element)).substring(6));
					ad.setCreated(createdDate);
				} catch (ParseException e) {
					e.printStackTrace();
				}
				ad.setSeries(text(eval(message.select("td[class=ads_opt]").get(6))));
				HTTPClient.post("http://127.0.0.1:9200/sslv/"+type+"/" + ad.getId(), ad);
			}
		}
	}
	
	private List<Node> extract (Node node){
		return node.childNodes();
	}

	private Node eval (Node node){
		if(node == null || node.childNodeSize() == 0){
			return null;
		}
		return node.childNode(0);
	}
	
	private String text (Node node){
		return ((TextNode) node).getWholeText();
	}

	public String concatenateNodes(List<Node> nodes){
		StringBuilder sb = new StringBuilder();
		for (Node node : nodes) {
			if(node instanceof TextNode){
				sb.append(text(node));
			} else {
				sb.append(concatenateNodes(extract(node)));
			}
		}
		
		return sb.toString();
	}

	public AD getADbyID(String id) {
		
		return null;
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
					e.printStackTrace();
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
			e.printStackTrace();
		}
		return uri;
	}
	
	private Document getPage(String type, URI uri){
		Document page = null;
		page = getPage(uri);
		parsePage(type, page);
		return page;
	}

	private Document getPage(URI uri){
		Document page = null;
		try {
			page = HTTPClientProxy.execute(uri);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return page;
	}
	
	
}
