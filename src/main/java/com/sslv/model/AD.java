package com.sslv.model;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class AD {
	private long id;
	private String name;
	private String message;
	private String url;
	private double cost;
	private double costPerM2;
	private String measure;
	private String location;
	private Date updated;
	private Date created;
	private String series;
	private String addr;
	private double square;
	private String floor;
	private String buildingType;
	private int rooms;
	private String city;
	private String area;
	private String map;
	private StreetAddress address;
	private String[] coordinates;
	private List<String> photos;
	
	
	
	public AD() {
		setUpdated(new Date());
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	@Override
	public String toString() {
		return "AD [id=" + id + ", url=" + url + ", cost=" + cost + ", location=" + location + ", map=" + map + "]";
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AD other = (AD) obj;
		if (id != other.id)
			return false;
		return true;
	}

	public double getCost() {
		return cost;
	}

	public void setCost(double cost) {
		this.cost = cost;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getMeasure() {
		return measure;
	}

	public void setMeasure(String measure) {
		this.measure = measure;
	}

	public Date getUpdated() {
		return updated;
	}

	public void setUpdated(Date timestamp) {
		this.updated = timestamp;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public String getSeries() {
		return series;
	}

	public void setSeries(String series) {
		this.series = series;
	}

	public double getCostPerM2() {
		return costPerM2;
	}

	public void setCostPerM2(double costPerM2) {
		this.costPerM2 = costPerM2;
	}

	public String getAddr() {
		return addr;
	}

	public void setAddr(String addr) {
		this.addr = addr;
	}

	public double getSquare() {
		return square;
	}

	public void setSquare(double square) {
		this.square = square;
	}

	public String getFloor() {
		return floor;
	}

	public void setFloor(String floor) {
		this.floor = floor;
	}

	public String getBuildingType() {
		return buildingType;
	}

	public void setBuildingType(String buildingType) {
		this.buildingType = buildingType;
	}

	public int getRooms() {
		return rooms;
	}

	public void setRooms(int rooms) {
		this.rooms = rooms;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getArea() {
		return area;
	}

	public void setArea(String area) {
		this.area = area;
	}

	public String getMap() {
		return map;
	}

	public void setMap(String map) {
		this.map = map;
	}

	public StreetAddress getAddress() {
		return address;
	}

	public void setAddress(StreetAddress address) {
		this.address = address;
	}

	public String[] getCoordinates() {
		return coordinates;
	}

	public void setCoordinates(String[] coordinates) {
		this.coordinates = coordinates;
	}

	public List<String> getPhotos() {
		return photos;
	}

	public void setPhotos(List<String> photos) {
		this.photos = photos;
	}

}
