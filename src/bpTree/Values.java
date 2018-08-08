package bpTree;

import java.io.Serializable;

public class Values implements Serializable {
	private String str;
	private Integer num;

	public Values(String str1, String str2) {
		this.str = str1;
		this.num = Integer.parseInt(str2);
	}

	public String getStr() {
		return this.str;
	}

	public Integer getNum() {
		return this.num;
	}
}