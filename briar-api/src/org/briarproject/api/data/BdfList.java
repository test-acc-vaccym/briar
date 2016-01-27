package org.briarproject.api.data;

import org.briarproject.api.FormatException;

import java.util.Vector;

public class BdfList extends Vector<Object> {

	public Boolean getBoolean(int index) throws FormatException {
		Object o = get(index);
		if (o instanceof Boolean) return (Boolean) o;
		throw new FormatException();
	}

	public Boolean getBoolean(int index, Boolean defaultValue) {
		Object o = get(index);
		if (o instanceof Boolean) return (Boolean) o;
		return defaultValue;
	}

	public Long getInteger(int index) throws FormatException {
		Object o = get(index);
		if (o instanceof Long) return (Long) o;
		throw new FormatException();
	}

	public Long getInteger(int index, Long defaultValue) {
		Object o = get(index);
		if (o instanceof Long) return (Long) o;
		return defaultValue;
	}

	public Double getFloat(int index) throws FormatException {
		Object o = get(index);
		if (o instanceof Double) return (Double) o;
		throw new FormatException();
	}

	public Double getFloat(int index, Double defaultValue) {
		Object o = get(index);
		if (o instanceof Double) return (Double) o;
		return defaultValue;
	}

	public String getString(int index) throws FormatException {
		Object o = get(index);
		if (o instanceof String) return (String) o;
		throw new FormatException();
	}

	public String getString(int index, String defaultValue) {
		Object o = get(index);
		if (o instanceof String) return (String) o;
		return defaultValue;
	}

	public byte[] getRaw(int index) throws FormatException {
		Object o = get(index);
		if (o instanceof byte[]) return (byte[]) o;
		throw new FormatException();
	}

	public byte[] getRaw(int index, byte[] defaultValue) {
		Object o = get(index);
		if (o instanceof byte[]) return (byte[]) o;
		return defaultValue;
	}

	public BdfList getList(int index) throws FormatException {
		Object o = get(index);
		if (o instanceof BdfList) return (BdfList) o;
		throw new FormatException();
	}

	public BdfList getList(int index, BdfList defaultValue) {
		Object o = get(index);
		if (o instanceof BdfList) return (BdfList) o;
		return defaultValue;
	}

	public BdfDictionary getDictionary(int index) throws FormatException {
		Object o = get(index);
		if (o instanceof BdfDictionary) return (BdfDictionary) o;
		throw new FormatException();
	}

	public BdfDictionary getDictionary(int index, BdfDictionary defaultValue) {
		Object o = get(index);
		if (o instanceof BdfDictionary) return (BdfDictionary) o;
		return defaultValue;
	}
}