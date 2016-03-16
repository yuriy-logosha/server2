package com.sslv.services;

import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;

import com.sslv.model.AD;

public class SSLVServicesTest {

	private static final String SEARCH_CRITERIA = "gazes";
	
	SSLVServices services = new SSLVServices();
	
//	@Test
//	@Ignore
//	public void testSearchRent() {
//		AD[] adList = services.search(SSLVServices.SEARCH_TYPE_RENT, SEARCH_CRITERIA);
//		int i = 0;
//		for (AD ad : adList) {
//			System.out.println(++i + " " + ad);
//		}
//		System.out.println("Total: " + adList.length);
//		assertNotNull(adList);
//
//	}

	@Test
	@Ignore
	public void testSearchSell() {
		AD[] adList = services.search(SSLVServices.SEARCH_TYPE_SELL, SEARCH_CRITERIA);
		assertNotNull(adList);

	}

}
