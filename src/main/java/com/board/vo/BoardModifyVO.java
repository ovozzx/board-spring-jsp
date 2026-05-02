package com.board.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BoardModifyVO {
    private BoardVO board;
    private List<AttachmentVO> fileList;
}
