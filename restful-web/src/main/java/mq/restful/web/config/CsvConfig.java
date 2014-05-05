package mq.restful.web.config;

public class CsvConfig {
	private String csvLineSeparator = "\n";

	private String csvColumnSeparator = ",";

	public CsvConfig() {

	}

	public String getCsvLineSeparator() {
		return csvLineSeparator;
	}

	public void setCsvLineSeparator(String csvLineSeparator) {
		this.csvLineSeparator = csvLineSeparator;
	}

	public String getCsvColumnSeparator() {
		return csvColumnSeparator;
	}

	public void setCsvColumnSeparator(String csvColumnSeparator) {
		this.csvColumnSeparator = csvColumnSeparator;
	}
}
