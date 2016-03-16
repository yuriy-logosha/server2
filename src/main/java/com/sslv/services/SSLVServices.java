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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import com.sslv.model.AD;

public class SSLVServices {

	private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm");
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

	public boolean put(String type, AD[] data){
		for (AD ad : data) {
			put(type, ad);
			
		}
		return true;
	}
	
	public boolean put(String type, AD ad){
//		Thread mytr = new MyThread(ad);
//		mytr.start();
		HTTPClient.post("http://127.0.0.1:9200/sslv/"+type+"/" + ad.getId(), ad);

		return true;
	}
	
	class MyThread extends Thread {
		
		AD ad;
		public MyThread(AD ad) {
			this.ad = ad;
		}
		
		@Override
		public void run(){
			try {
				HTTPClient.post("http://127.0.0.1:9200/sslv/" + ad.getLocation().trim() + "/" + ad.getId(), ad);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
	}
	
	public AD[] search(String type, String searchCriteria) {
    	URI uri = null;
		try {
			uri = HTTPClientProxy.getURIBuilder(DOMAIN).setPath(SEARCH_PATH + type + "/page1.html").build();
			//.setParameter(SEARCH_QUERY_VAR_NAME, searchCriteria)
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		//Get first page
//		Document first = HTTPClient.get("http://"+DOMAIN+SEARCH_PATH + type + "/page1.html?"+SEARCH_QUERY_VAR_NAME+"="+searchCriteria);
		Document first = getPage(uri);
		Set<AD> adss = new HashSet<>();
		
		Date current = new Date(); 		
		AD[] adArr = parsePage(first);
		put(type, adArr);

		int oldSize = adss.size();

		adss.addAll(Arrays.asList(adArr));
		
		System.out.println("Added " + (adss.size() - oldSize) + " item(s) " + adss.size() + " in " + ((new Date()).getTime() - current.getTime()) + " ms " + SEARCH_PATH + type + "/page1.html" );
		
		//Get rest pages
		String[] pagesURI;
		
		Element root = first.getElementById(PAGE_MAIN);
		Node tableBody = eval(root);
		Elements pagesNodes = root.select("a[name=nav_id]");
				//extract(eval(eval(tableBody)).childNode(12));
		
		int max = getMaxPageNumber(pagesNodes);
		String[] arr = new String[max - 1];
		for(int i = 1; i < max; i++){
			arr[i - 1] = SEARCH_PATH + type + "/" + PAGE + (i+1) + HTML;
					//+ "?" + SEARCH_QUERY_VAR_NAME +"="+searchCriteria;
		}
		pagesURI = arr;

		for (String str : pagesURI) {
			current = new Date();
//			Document page = HTTPClient.get("http://"+DOMAIN + str);
			Document page = getPage(getPageURI(str));
			oldSize = adss.size();
			adArr = parsePage(page);
			adss.addAll(Arrays.asList(adArr));
			put(type, adArr);
			System.out.println("Added " + adArr.length + " item(s) " + adss.size() + " in " + ((new Date()).getTime() - current.getTime()) + " ms " + str );
		}
		
		return adss.toArray(new AD[0]);
	}

	public URI getPageURI(String str){
    	URI uri = null;
		try {
			uri = HTTPClientProxy.getURIBuilder(DOMAIN).setPath(str).build();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return uri;

	}
	
	private Document getPage(URI uri){
		Document response = null;
		try {
			response = HTTPClientProxy.execute(uri);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;
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

	public AD[] parsePage(Document d){
		AD[] list = new AD[30];
		Element root = d.select("form#filter_frm > table[align=center] > tbody").first();
		Elements childNodes = d.select("tr[id~=^tr_\\d]");
		int index = 0;
		for (Node node : childNodes) {
			if( node.getClass().equals(Element.class) && node.hasAttr(ID) && !node.attr(ID).equalsIgnoreCase("head_line")){
				AD ad = new AD();
				list[index++] = ad;
				Element adBody = ((Element)node).select("td[class=msg2]").first();
				Elements select = adBody.select("div > a");
				String url = select.attr(HREF);
				ad.setUrl(url);
				String name = select.attr(ID);
				ad.setName(name);
				Long id = new Long(name.replace("dm_", ""));
				ad.setId(id);
				
				Element costEl = root.select("tr#tr_"+id+">td:eq(8)").first();
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
				 
				Document page = getPage(getPageURI(url));
				Element message = page.select("div#msg_div_msg").first();
				ad.setMessage(StringEscapeUtils.unescapeHtml(concatenateNodes(message.childNodes()).replace("\r", "").replace("\n", "")));
				Element date_element = page.select("table#page_main > tbody > tr:eq(1) > td > table > tbody > tr:eq(1) > td:eq(1)").first();
				Date createdDate = null;
				try {
					createdDate = SIMPLE_DATE_FORMAT.parse(text(eval(date_element)).substring(6));
				} catch (ParseException e) {
					e.printStackTrace();
				}
				ad.setCreated(createdDate);
				ad.setSeries(text(eval(message.select("td[class=ads_opt]").get(6))));
			}
		}
		AD[] arr = new AD[index];
		System.arraycopy(list, 0, arr, 0, index);
		
		return arr;
	}
	
	private Node findByClass (Node node, String sclass){
		for (Node n : extract(node)) {
			if(sclass.equals(n.attr("class"))){
				return n;
			}
			Node findByClass = findByClass(n, sclass);
			if(findByClass != null){
				return findByClass;
			}
		}
		return null;
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
	
	

}
