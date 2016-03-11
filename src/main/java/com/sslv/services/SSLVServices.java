package com.sslv.services;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EncodingUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import com.sslv.model.AD;

public class SSLVServices {

	public static final String SEARCH_TYPE_ALL  = "";
	public static final String SEARCH_TYPE_SELL = "sell";
	public static final String SEARCH_TYPE_RENT = "hand_over";
	
	private static final String PAGE = "page";
	private static final String HTML = ".html";
	private static final String ID = "id";
	private static final String HREF = "href";
	private static final String PAGE_MAIN = "page_main";
	private static final String DOMAIN = "www.ss.lv";
	
	private static final String SEARCH_PATH = "/ru/real-estate/flats/riga/search-result/";
	private static final String SEARCH_QUERY_VAR_NAME = "q";
	
	
	private static final String DB_PROVIDER = "127.0.0.1:9200";
	private static final String REPOSITORY = "twitter";

	public boolean put(AD[] data){
		for (AD ad : data) {
			put(ad);
//			HTTPClient.post("http://127.0.0.1:9200/twitter/tweet/" + ad.getId(), ad);
			
		}
		return true;
	}
	
	public boolean put(AD ad){
		MyThread mytr = new MyThread(ad);
		mytr.start();
		return true;
	}
	
	class MyThread extends Thread {
		
		AD ad;
		public MyThread(AD ad) {
			this.ad = ad;
		}
		
		public void run(){
			try {
				HTTPClient.post("http://127.0.0.1:9200/twitter/tweet/" + ad.getId(), ad);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
	}
	
	public AD[] search(String type, String searchCriteria) {
    	URI uri = null;
		try {
			uri = HTTPClientProxy.getURIBuilder(DOMAIN).setPath(SEARCH_PATH + type + "/page1.html").setParameter(SEARCH_QUERY_VAR_NAME, searchCriteria).build();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		//Get first page
//		Document first = HTTPClient.get("http://"+DOMAIN+SEARCH_PATH + type + "/page1.html?"+SEARCH_QUERY_VAR_NAME+"="+searchCriteria);
		Document first = getPage(uri);
		Set<AD> adss = new HashSet<>();
		
		Date current = new Date(); 		
		AD[] adArr = getAD(first);

		int oldSize = adss.size();

		adss.addAll(Arrays.asList(adArr));
		
		System.out.println("Added " + (adss.size() - oldSize) + " item(s) " + adss.size() + " in " + ((new Date()).getTime() - current.getTime()) + " ms" );
		
		//Get rest pages
		String[] pagesURI;
		
		Element root = first.getElementById(PAGE_MAIN);
		Node tableBody = eval(root);
		List<Node> pagesNodes = extract(eval(eval(tableBody)).childNode(12));
		
		int max = getMaxPageNumber(pagesNodes);
		String[] arr = new String[max - 1];
		for(int i = 1; i < max; i++){
			arr[i - 1] = SEARCH_PATH + type + "/" + PAGE + (i+1) + HTML + "?" + SEARCH_QUERY_VAR_NAME +"="+searchCriteria;
		}
		pagesURI = arr;

		for (String str : pagesURI) {
			current = new Date();
//			Document page = HTTPClient.get("http://"+DOMAIN + str); 
			Document page = getPage(getPageURI(str));
			oldSize = adss.size();
			adArr = getAD(page);
			adss.addAll(Arrays.asList(adArr));
			System.out.println("Added " + (adss.size() - oldSize) + " item(s) " + adss.size() + " in " + ((new Date()).getTime() - current.getTime()) + " ms" );
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
	
	private static int getMaxPageNumber(List<Node> pagesNodes) {
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

	public AD[] getAD(Document d){
		AD[] list = new AD[30];
		Element root = d.getElementById(PAGE_MAIN);
		Node tableBody = eval(root);
		List<Node> childNodes = extract(eval(eval(tableBody)).childNode(4).childNode(1));
		int index = 0;
		for (Node node : childNodes) {
			if( node.getClass().equals(Element.class) && node.hasAttr(ID) && !node.attr(ID).equalsIgnoreCase("head_line")){
				AD ad = new AD();
				list[index++] = ad;
				Node adBody = eval(node.childNode(2).childNode(1));
//				String body = null;
//				if(eval(adBody) instanceof TextNode){
//					body = text(eval(adBody));
//				} else {
//					body = concatenateNodes(extract(eval(adBody)));
//				}
//				ad.setBody(body);
				ad.setUrl(adBody.attr(HREF));
				ad.setName(adBody.attr(ID));
				ad.setId(new Long(adBody.attr(ID).replace("dm_", "")));
				TextNode costNode = null;
				if(eval(node.childNode(8)) instanceof Element){
					costNode = (TextNode) eval(eval(node.childNode(8)));
				} else {
					costNode = (TextNode) eval(node.childNode(8));
				}
				String cost = text(costNode).split(" ")[0];
				ad.setCost(new Long(cost.replace(",", "")));
				
				
				String location = text(extract(findByClass(node, "ads_cat_names")).get(0));
				ad.setLocation(location);
				 
				Document page = getPage(getPageURI(adBody.attr(HREF)));
				Element body = page.select("div[id=msg_div_msg]").first();
				ad.setBody(concatenateNodes(body.childNodes()));
				put(ad);
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
