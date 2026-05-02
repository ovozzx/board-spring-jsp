package com.board.vo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;

@Getter
@Setter
public class SearchVO {

	private String startDate;
	private String endDate;
	private String categoryId;
	private String keyword;
	private int page;
	private int pageSize;

	public int getOffset() {
		// 맵퍼에서 연산식 못 넣어서, 계산하고 넘겨야 함
		// http://localhost:8081/board/list?startDate=2025-02-13&endDate=2026-02-13&categoryId=0&keyword=ee
		if(page == 0) page = 1;
		return (page - 1) * pageSize;
	}
}
