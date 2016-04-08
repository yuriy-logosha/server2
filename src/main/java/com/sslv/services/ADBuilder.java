package com.sslv.services;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import com.sslv.common.Constants;
import com.sslv.model.AD;

public class ADBuilder implements Runnable {
	
	Logger logger = Logger.getLogger(ADBuilder.class);

	private static final String ID = "id";
	private static final String DOMAIN = "www.ss.lv";
	private static final String REPOSITORY = "http://127.0.0.1:9200/sslv/" + Constants.SEARCH_TYPE_SELL + "/%s";

	private final Element node;

	public ADBuilder(final Element node) {
		this.node = node;
	}
	
	@Override
	public void run() {
		AD ad = build();
		if(System.getProperty("debug") == null)
			Helper.save(String.format(REPOSITORY, ad.getId()), ad);
	}

	private AD build() {
		Element tmpElement = null;
		Elements infoTable = null;
		Node tmpNode = null;
		String tmpString = null;
		String id = node.attr(ID).substring(3);
		try {
			AD ad = new AD();
			
			ad.setId(Long.parseLong(id));
			
			tmpNode = node.select("a.am").first();
			ad.setName(text(eval(tmpNode)));
			
			ad.setUrl(DOMAIN + tmpNode.attr(Constants.HREF));
			
			infoTable = node.select("td[class=msga2-o pp6]");
			
			//1. Cost
			tmpElement = infoTable.last();
			tmpString = concatenateNodes(tmpElement.childNodes());
			CostParser cp = new CostParser();
			ad.setCost(cp.parse(tmpString));
			
			//2. Measure
			ad.setMeasure(StringEscapeUtils.unescapeHtml(tmpString.substring(tmpString.length() - 1)));
			
			//3. Location
			tmpNode = eval(infoTable.get(0));
			ad.setLocation(StringEscapeUtils.unescapeHtml(text((tmpNode instanceof TextNode)?tmpNode:eval(tmpNode))));

			//4. Rooms
			try {
				tmpString = concatenateNodes(extract(infoTable.get(1)));
				if(!"-".equals(tmpString)){
					ad.setRooms(Integer.parseInt(tmpString));
				}
			} catch (Exception e) {
				logger.error("Can't parse number of rooms.", e);
			}
			
			//5. Square
			try {
				tmpString = concatenateNodes(extract(infoTable.get(2)));
				ad.setSquare(Double.parseDouble(tmpString));
			} catch (ClassCastException e) {
				logger.error("Can't parse square.", e);
			}

			//6. Floor
			try {
				tmpString = concatenateNodes(extract(infoTable.get(3)));
				ad.setFloor(tmpString);
			} catch (ClassCastException e) {
				logger.error("Can't parse number of floors.", e);
			}
			
			
			tmpElement = null;
			infoTable = null;
			tmpNode = null;
			tmpString = null;
			
			if(logger.isInfoEnabled()){
				logger.info(String.format("%s < GET %s", ad.getId(), ad.getUrl()));
			}

			Document messagePage = Helper.getPage(ad.getUrl());
			if(messagePage != null){
			
				List<String> photos = new ArrayList<>();
				for (Element element : messagePage.select("div.pic_dv_thumbnail > a")) {
					photos.add(element.attr("href"));
				}
				ad.setPhotos(photos);
				tmpElement = messagePage.select("div#msg_div_msg").first();
				ad.setMessage(StringEscapeUtils.unescapeHtml(concatenateNodes(tmpElement.childNodes()).replace("\r", "").replace("\n", "")));
				ad.setSeries(tmpElement.select("td#tdo_6").text());
				ad.setBuildingType(tmpElement.select("td#tdo_2").text());
				ad.setCity(tmpElement.select("td#tdo_20").text());
				ad.setArea(tmpElement.select("td#tdo_856").text());
				tmpString = tmpElement.select("td#tdo_11").text();
				ad.setAddr(tmpString.substring(0, tmpString.length()-8));

				// Map
				try {
					Elements select = tmpElement.select("td#tdo_11 > span.td15 > a.ads_opt_link_map");
					if(select != null && !select.isEmpty()){
						tmpString = select.attr("onclick").split(";")[0];
						ad.setMap(DOMAIN + tmpString.substring(15, tmpString.length()-2));
//						StreetAddress street = getGoogleMapInfo(ad.getMap().split("=")[3].split(","));
//						ad.setAddress(street);
						ad.setCoordinates(ad.getMap().split("=")[3].split(","));
					}
				} catch (Exception e) {
					logger.error("", e);
				} finally {
					tmpString = null;
				}
			
				//Creation date
				try {
					Element date_element = messagePage.select("table#page_main > tbody > tr:eq(1) > td > table > tbody > tr:eq(1) > td:eq(1)").first();
					tmpString = text(eval(date_element)).substring(6);
					ad.setCreated((new SimpleDateFormat("dd.MM.yyyy HH:mm")).parse(tmpString));
				} catch (ParseException | NumberFormatException e) {
					logger.error(String.format("%s Error parsing creation date %s.", id, tmpString), e);
				} finally {
					tmpString = null;
				}
				ad.setSeries(text(eval(tmpElement.select("td[class=ads_opt]").get(6))));
				return ad;
			}
			
		} catch (Exception e) {
			logger.error(String.format("Exception while parsing post %s.", id), e);
		}
		return null;		
	}
	
	public List<Node> extract (Node node){
		return node.childNodes();
	}

	public Node eval (Node node){
		if(node == null || node.childNodeSize() == 0){
			return null;
		}
		return node.childNode(0);
	}
	
	public String text (Node node){
		try {
			if(node.childNodeSize() > 0){
				return ((TextNode) eval(node)).getWholeText();
			} else
				return ((TextNode) node).getWholeText();
		} catch (ClassCastException e) {
			logger.error(String.format("Exception occured while parsing %s", node.nodeName()), e);
		}
		return null;
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
	
	public class CostParser {
		public double parse(String value){
			return Double.parseDouble(value.trim().substring(0, value.length()-1).replace(",", "").trim());
		}
		
	}
	
//	public StreetAddress getGoogleMapInfo(String[] coords) {
//	JSONObject response = null;
//	try {
//		response = JsonReader.read("http://maps.googleapis.com/maps/api/geocode/json?latlng="+URLEncoder.encode(coords[0], "utf-8")+","+URLEncoder.encode(coords[1], "utf-8"));
//		while ("OVER_QUERY_LIMIT".equals(response.getString("status"))) {
//			try {
//				Thread.sleep(999);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			response = JsonReader.read("http://maps.googleapis.com/maps/api/geocode/json?latlng="+URLEncoder.encode(coords[0], "utf-8")+","+URLEncoder.encode(coords[1], "utf-8"));
//		}
//		JSONObject location = response.getJSONArray("results").getJSONObject(0);
//		location = location.getJSONObject("geometry");
//		location = location.getJSONObject("location");
//		final double lng = location.getDouble("lng");
//		final double lat = location.getDouble("lat");
//		StreetAddress sa = new StreetAddress();
//		sa.setPlaceId(response.getJSONArray("results").getJSONObject(0).getString("place_id"));
//		sa.setAddress(response.getJSONArray("results").getJSONObject(0).getString("formatted_address"));
//		sa.setLocation(new double[]{lat, lng});
//		return sa;
//	} catch (IOException | JSONException e) {
//		logger.error(response.toString(), e);
//	}
//	return null;
//}

//public static URI getPageURI(String path){
//	URI uri = null;
//	try {
//		uri = HTTPClientProxy.getURIBuilder(DOMAIN).setPath(path).build();
//	} catch (URISyntaxException e) {
//		logger.error(e);
//	}
//	return uri;
//}

//public static URI getPageURI2(String pathWithDomain){
//	URI uri = null;
//	try {
//		uri = HTTPClientProxy.getURIBuilder(pathWithDomain).build();
//	} catch (URISyntaxException e) {
//		logger.error(e);
//	}
//	return uri;
//}

//public static URL getPageURL(final String path){
//	URL url = null;
//	try {
//		url = new URL("http://" + DOMAIN + path);
//	} catch (MalformedURLException e) {
//		logger.error("", e);
//	}
//	return url;
//}	
}
