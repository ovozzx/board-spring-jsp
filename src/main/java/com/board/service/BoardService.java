package com.board.service;

import com.board.mapper.BoardMapper;
import com.board.vo.*;
import org.springframework.http.codec.multipart.Part;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static com.board.constants.BoardConstants.UPLOAD_PATH;

@Service
public class BoardService {

    private final BoardMapper boardMapper;

    public BoardService(BoardMapper boardMapper) {
        this.boardMapper = boardMapper;
    }

    /**
     * 전체 카테고리 및 게시글 조회
     * @param searchVO
     * @return BoardListViewVO
     */
    public BoardListViewVO getBoardListView(SearchVO searchVO) {
        BoardListViewVO boardListViewVO = new BoardListViewVO();
        // boardMapper 조회, 저장만
        List<CategoryVO> categoryList = boardMapper.selectCategoryList();
        List<BoardVO> boardList = boardMapper.selectBoardList(searchVO);
        int boardListCount = boardMapper.selectBoardListCount(searchVO);

        boardListViewVO.setCategoryList(categoryList);
        boardListViewVO.setBoardList(boardList);
        boardListViewVO.setBoardListCount(boardListCount);

        return boardListViewVO;
    }

    /**
     * 상세 게시글 조회
     * @param boardId
     * @return BoardDetailViewVO
     */
    public BoardDetailViewVO getDetailBoardById(String boardId) {
        BoardDetailViewVO boardDetailViewVO = new BoardDetailViewVO();

        BoardVO boardVO = boardMapper.selectBoard(boardId);
        List<ReplyVO> replyList = boardMapper.selectReplyList(boardId);
        List<AttachmentVO> fileList = boardMapper.selectFileList(boardId);

        boardDetailViewVO.setBoard(boardVO);
        boardDetailViewVO.setReplyList(replyList);
        boardDetailViewVO.setFileList(fileList);

        boardMapper.updateViewCount(boardId);

        return boardDetailViewVO;
    }

    /**
     * 게시글 및 첨부파일 등록하기
     * @param board
     * @param attachmentList
     * @return boolean
     * @throws IOException
     */
    public boolean registerBoard(BoardVO board, List<MultipartFile> attachmentList) throws IOException {

        // 게시글 insert (useGeneratedKeys로 boardId 자동 세팅)
        int insertBoardCnt = boardMapper.insertBoard(board);

        if (attachmentList == null) return insertBoardCnt > 0;

        // 저장 경로 (프로젝트 외부 경로 권장)
        String uploadPath = UPLOAD_PATH; // TODO : 속성으로 관리. 배포 시스템마다 다름 (작업자마다 다름)
        // String uploadPath = request.getServletContext().getRealPath("/") + "uploads";
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) uploadDir.mkdirs();

        int count = 0;
        for (MultipartFile attachment : attachmentList) { // MultipartFile : wrapper (바이너리 아님)
            if (attachment == null || attachment.isEmpty()) continue;
            if (count >= 3) break; // 최대 3개

            String originalName = attachment.getOriginalFilename();
            String saveName = UUID.randomUUID() + "_" + originalName; // 중복 방지
            String ext = originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf("."))
                    : "";

            // 파일 저장 (실제 파일을 디스크에 저장)
            attachment.transferTo(new File(uploadPath + File.separator + saveName));

            // DB insert
            AttachmentVO attachmentVO = new AttachmentVO();
            attachmentVO.setBoardId(board.getBoardId());
            attachmentVO.setOriginalName(originalName);
            attachmentVO.setSaveName(saveName);
            attachmentVO.setFilePath(uploadPath);
            attachmentVO.setFileExt(ext);
            attachmentVO.setFileSize(attachment.getSize());

            boardMapper.insertAttachment(attachmentVO);
            count++;
        }
        // TODO : 등록 -> false
        // 성공이면 void, 실패면 throw (실패 원인 알려주기)
        return insertBoardCnt > 0 && count > 0; // 게시글 & 첨부파일 성공
    }

    /**
     * 비밀번호 검증 후 삭제하기
     * @param deleteBoardVO
     * @return boolean
     */
    @Transactional
    public boolean deleteBoard(DeleteBoardVO deleteBoardVO) {
        String password = boardMapper.selectPasswordById(deleteBoardVO.getBoardId());

        if (password == null) { // 존재하지 않는 게시글
            throw new IllegalArgumentException("존재하지 않는 게시글입니다.");
        }

        if (!password.equals(deleteBoardVO.getPasswordInput())) { // 비밀번호 틀렸을 경우
            throw new IllegalArgumentException("비밀번호가 틀렸습니다.");
        }

        int successCnt = boardMapper.deleteBoard(deleteBoardVO.getBoardId());

        if(successCnt > 0){
            return successCnt > 0;
        }else{ // 삭제 중 오류
            throw new IllegalArgumentException("삭제 중 오류가 발생하였습니다.");
        }


    }

    /**
     * 카테고리 읽기
     * @return List<CategoryVO>
     */
    public List<CategoryVO> getCategoryList() {
        return boardMapper.selectCategoryList();
    }

    /**
     * 첨부파일 읽기
     * @param attachmentId
     * @return AttachmentVO
     */
    public AttachmentVO getAttachmentById(String attachmentId) {
        return boardMapper.selectAttachmentById(attachmentId);
    }

    /**
     * 수정화면 데이터 읽기
     * @param boardId
     * @return BoardModifyVO
     */
    public BoardModifyVO getModifyBoardById(String boardId) {
        BoardModifyVO boardModifyVO = new BoardModifyVO();

        boardModifyVO.setBoard(boardMapper.selectBoard(boardId));
        boardModifyVO.setFileList(boardMapper.selectFileList(boardId));

        return boardModifyVO;
    }

    /**
     * 비밀번호 검증 후 게시글 수정하기
     * @param passwordInput
     * @param board
     * @param deleteIds
     * @param newFiles
     * @return
     * @throws IOException
     */
    @Transactional
    public String modifyBoard(String passwordInput, BoardVO board,
                              List<String> deleteIds, List<MultipartFile> newFiles) throws IOException {

        if (passwordInput == null) {
            throw new IllegalArgumentException("비밀번호를 작성해 주세요.");
        }

        if (!passwordInput.equals(boardMapper.selectPasswordById(board.getBoardId()))) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 게시물 수정
        boardMapper.updateBoard(board);

        // 기존 첨부파일 논리 삭제
        if (deleteIds != null) {
            for (String attachmentId : deleteIds) {
                boardMapper.deleteAttachment(attachmentId);
            }
        }

        // 새 첨부파일 업로드
        if (newFiles != null) {
            int existingCount = boardMapper.selectFileList(board.getBoardId()).size();
            int count = 0;

            String uploadPath = UPLOAD_PATH;
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) uploadDir.mkdirs();

            for (MultipartFile file : newFiles) {
                if (file == null || file.isEmpty()) continue;
                if (existingCount + count >= 3) break;

                String originalName = file.getOriginalFilename();
                if (originalName == null) originalName = "unknown";
                String saveName = UUID.randomUUID() + "_" + originalName;
                String ext = originalName.contains(".")
                        ? originalName.substring(originalName.lastIndexOf(".")) : "";

                file.transferTo(new File(uploadPath + File.separator + saveName));

                AttachmentVO attachmentVO = new AttachmentVO();
                attachmentVO.setBoardId(board.getBoardId());
                attachmentVO.setOriginalName(originalName);
                attachmentVO.setSaveName(saveName);
                attachmentVO.setFilePath(uploadPath);
                attachmentVO.setFileExt(ext);
                attachmentVO.setFileSize(file.getSize());

                boardMapper.insertAttachment(attachmentVO);
                count++;
            }
        }

        return "수정 완료되었습니다.";
    }
}