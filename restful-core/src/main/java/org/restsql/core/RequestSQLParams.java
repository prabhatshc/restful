package org.restsql.core;

public class RequestSQLParams {

	private String filter, orderby;
	private Integer limit = -1, offset = -1;

	public RequestSQLParams(String filter, String orderby, Integer limit,
			Integer offset) {

		this.filter = filter.trim();
		this.orderby = orderby.trim();
		if (null != limit)
			this.limit = limit;
		if (null != offset)
			this.offset = offset;
	}

	public String getFilter() {
		return filter;
	}

	public String getOrderby() {
		return orderby;
	}

	public Integer getLimit() {
		return limit;
	}

	public Integer getOffset() {
		return offset;
	}

	@Override
	public String toString() {
		return "RequestSQLParams [filter=" + filter + ", orderby=" + orderby
				+ ", limit=" + limit + ", offset=" + offset + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((filter == null) ? 0 : filter.hashCode());
		result = prime * result + ((limit == null) ? 0 : limit.hashCode());
		result = prime * result + ((offset == null) ? 0 : offset.hashCode());
		result = prime * result + ((orderby == null) ? 0 : orderby.hashCode());
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
		RequestSQLParams other = (RequestSQLParams) obj;
		if (filter == null) {
			if (other.filter != null)
				return false;
		} else if (!filter.equals(other.filter))
			return false;
		if (limit == null) {
			if (other.limit != null)
				return false;
		} else if (!limit.equals(other.limit))
			return false;
		if (offset == null) {
			if (other.offset != null)
				return false;
		} else if (!offset.equals(other.offset))
			return false;
		if (orderby == null) {
			if (other.orderby != null)
				return false;
		} else if (!orderby.equals(other.orderby))
			return false;
		return true;
	}

}
