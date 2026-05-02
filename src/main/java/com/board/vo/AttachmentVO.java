package com.board.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttachmentVO {
    String attachmentId;
    String boardId;
    String originalName;
    String saveName;
    String filePath;
    String fileExt;
    long fileSize;
    String createDate;
    String modifyDate;
    boolean useYn;
}
