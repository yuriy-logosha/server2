package com.sslv.facade;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import com.sslv.model.AD;
import com.sslv.services.SSLVServices;

@Path("/online/ad")
public class ONLINEServices {

	SSLVServices services = new SSLVServices();
	
	@GET
	public Response getList() {
		AD[] adList = services.search(SSLVServices.SEARCH_TYPE_SELL);
		return Response.status(200).entity(adList).build();
	}
	
//	@GET
//	@PathParam("{id}")
//	public Response getAdByID(String id) {
//		AD ad = services.getADbyID(id);
//		return Response.status(200).entity(ad).build();
//	}
	
}
