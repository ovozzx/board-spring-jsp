package com.board.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BoardVO {
    private String boardId;
    private String categoryId;
    private String categoryName;
    private String title;
    private String content;
    private String createUser;
    private String userPassword;
    private String passwordConfirm;
    private int viewCount;
    private String createDate;
    private String modifyDate;
    private String useYn;
	private boolean hasAttachment;
}
