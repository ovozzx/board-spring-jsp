package com.board.service;

import com.board.mapper.BoardMapper;
import com.board.mapper.ReplyMapper;
import com.board.vo.ReplyVO;
import org.springframework.stereotype.Service;

@Service
public class ReplyService {

	private final ReplyMapper replyMapper;

	public ReplyService(ReplyMapper replyMapper) {
		this.replyMapper = replyMapper;
	}

	/**
	 * 댓글 등록하기
	 * @param reply
	 * @return boolean
	 */
    public boolean registerReply(ReplyVO reply) {
		return replyMapper.insertReply(reply) > 0;
    }
}
