package org.restsql.core;

public class SqlStruct {

	private StringBuffer clause;
	private int limit = -1;
	private StringBuffer main;
	private int offset = -1;
	private StringBuffer orderByClause;

	public SqlStruct(String mainStr, String clauseStr) {
		main = new StringBuffer();
		main.append(mainStr.trim());

		clause = new StringBuffer();
		if (clauseStr != null)
			clause.append(clauseStr.trim());

	}

	public StringBuffer getOrderByClause() {
		return orderByClause;
	}

	public void setOrderByClause(String orderByStr) {
		this.orderByClause = new StringBuffer();
		if (null != orderByStr && orderByStr.trim().length() > 0)
			this.orderByClause.append(" ORDER BY ").append(orderByStr);

	}

	public StringBuffer getClause() {
		return clause;
	}

	public int getLimit() {
		return limit;
	}

	public StringBuffer getMain() {
		return main;
	}

	public int getOffset() {
		return offset;
	}

	public boolean isClauseEmpty() {
		return clause.length() == 0;
	}

	public void setLimit(final int limit) {
		this.limit = limit;
	}

	public void setOffset(final int offset) {
		this.offset = offset;
	}

	public String getStructSql() {

		StringBuffer struct = new StringBuffer();
		struct.append(main.toString());
		if (this.clause.length() > 0) {
			if (struct.indexOf("where ") > 0 || struct.indexOf("WHERE ") > 0) {
				String clauseString = this.clause.toString().replaceFirst(
						"where ", "");
				clauseString.replaceFirst("WHERE ", "");
				struct.append(" AND ").append(clauseString);
			} else if (this.clause.indexOf("where ") > 0
					|| this.clause.indexOf("WHERE ") > 0) {
				struct.append(" ").append(this.clause.toString().trim());
			} else {
				struct.append(" WHERE ").append(this.clause.toString().trim());
			}
		}
		if (this.orderByClause.length() > 0) {
			struct.append(" ").append(this.orderByClause.toString());
		}
		if (this.limit > 0) {

			struct.append(" LIMIT ");
			struct.append(this.limit);

		}
		if (this.offset >= 0) {
			struct.append(" OFFSET ");
			struct.append(this.offset);
		}

		return struct.toString().trim();
	}
}
