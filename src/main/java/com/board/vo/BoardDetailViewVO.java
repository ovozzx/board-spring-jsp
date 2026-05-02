package com.board.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BoardDetailViewVO {
    private BoardVO board;
    private List<ReplyVO> replyList;
    private List<AttachmentVO> fileList;
}
