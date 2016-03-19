package com.sslv.services;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;

import java.net.URL;

import org.apache.http.Consts;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sslv.model.AD;

public class HTTPClient {
	static Logger logger = Logger.getLogger(HTTPClient.class);

	public static Document get(String url){
		Document doc = null;
		try {
            logger.debug("GET " + url);
			doc = Jsoup.connect(url).get();
		} catch (IOException e) {
			logger.error(e);
		}
		return doc;
	}
	

	public static void post(String url, AD ad){
		try {
			ObjectMapper mapper = new ObjectMapper();

//			Jsoup.connect(url).header("Accept", "application/json").data(URLEncoder.encode(mapper.writeValueAsString(ad)), "").post();
            
            String rawData = mapper.writeValueAsString(ad);
            logger.debug("POST " + url +" " + rawData);

            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            //add reuqest header
            con.setRequestMethod("POST");
            con.setRequestProperty("Accept", "application/json");
            con.setRequestProperty("Accept-Encoding", "gzip,deflate,sdch");
            con.setRequestProperty("Accept-Language", "es-ES,es;q=0.8");
            con.setRequestProperty("Connection", "keep-alive");
            con.setRequestProperty("X-Requested-With", "XMLHttpRequest");

            // Send post request
            con.setDoOutput(true);

            OutputStreamWriter w = new OutputStreamWriter(con.getOutputStream(), Consts.UTF_8);

            w.write(rawData);
            w.close();

            int responseCode = con.getResponseCode();

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            
            
            
		} catch (IOException e) {
			logger.error(e);
		}
	}

}
