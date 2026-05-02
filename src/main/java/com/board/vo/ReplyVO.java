package com.board.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReplyVO {

	private String replyId;        // REPLY_ID
    private String boardId;        // BOARD_ID
    private String parentReplyId;  // PARENT_REPLY_ID (null 가능)
    private String createUser;   // CREATE_USER
    private String content;      // CONTENT
    private String createDate; // CREATE_DATE
    private String modifyDate; // MODIFY_DATE
    private String useYn;        // USE_YN
    
}
