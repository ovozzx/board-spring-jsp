package com.board.controller;

import com.board.service.BoardService;
import com.board.service.ReplyService;
import com.board.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.multipart.Part;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/reply")
public class ReplyController {

    @Autowired
    private ReplyService service;

    /**
     * 댓글 등록하기
     * @param reply
     * @param model
     * @return view
     */
    @PostMapping("/write")
    public String registerReply(ReplyVO reply, Model model) {
        boolean isRegistered = service.registerReply(reply);
        //System.out.println("=== 댓글 등록 : " + result);
        return "redirect:/board/view?boardId=" + reply.getBoardId();
    }

}
