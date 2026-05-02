package com.board.vo;

import com.board.mapper.BoardMapper;
import jdk.jfr.Category;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BoardListViewVO {
    List<CategoryVO> categoryList;
    List<BoardVO> boardList;
    int boardListCount;
}
