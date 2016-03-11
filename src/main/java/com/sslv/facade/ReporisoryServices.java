package com.sslv.facade;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import com.sslv.model.AD;

@Path("/data/flat")
public class ReporisoryServices {

	@GET
	public Response getList() {
		AD[] adList;
		return Response.status(200).build();
	}
}
