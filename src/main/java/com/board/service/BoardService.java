package com.board.service;

import com.board.exception.BusinessException;
import com.board.exception.NotFoundException;
import com.board.exception.ValidationException;
import com.board.mapper.BoardMapper;
import com.board.vo.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
public class BoardService {

    private final BoardMapper boardMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${board.upload-path}")
    private String uploadPath;

    private Set<String> allowedExtList;

    public BoardService(BoardMapper boardMapper
                        , PasswordEncoder passwordEncoder
                        , @Value("${board.allowed-ext}") String allowedExt) {  // 생성자 호출 후, @Value 주입됨
        // new 안쓰고 주입 => spring이 빈 찾아서 BoardService 빈으로 등록 (테스트, 결합도, 책임 분리, 싱글톤)
        this.boardMapper = boardMapper; // @Mapper로 빈 등록 (MyBatis)
        this.passwordEncoder = passwordEncoder; // @Bean으로 빈 등록
        allowedExtList = Arrays.stream(allowedExt.split(",")).collect(Collectors.toSet());
        // Set : 해시로 바로 찾아서 빠름, 중복 제거
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
        if(boardVO == null){
            throw new NotFoundException("존재하지 않는 게시글입니다.");
        }
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
    @Transactional
    public void registerBoard(BoardVO board, List<MultipartFile> attachmentList) throws IOException {

        // 유효성 : 작성자, 비밀번호, 제목, 내용
        boolean isInvalid =
                isBlank(board.getCreateUser()) ||
                        isBlank(board.getUserPassword()) ||
                        isBlank(board.getTitle()) ||
                        isBlank(board.getContent());

        if (isInvalid) {
            throw new ValidationException("필수 항목이 작성되지 않았습니다.");
        }

        if(!board.getUserPassword().equals(board.getPasswordConfirm())){
            throw new ValidationException("비밀번호가 일치하지 않습니다.");
        }

        // 비밀번호 해시
        board.setUserPassword(passwordEncoder.encode(board.getUserPassword()));

        // 게시글 insert (useGeneratedKeys로 boardId 자동 세팅)
        int insertBoardCnt = boardMapper.insertBoard(board);
        if(insertBoardCnt <= 0){
            throw new BusinessException("게시글 등록 중 오류가 발생하였습니다.");
        }
        saveAttachment(uploadPath, attachmentList, board, 0);
    }

    /**
     * 비밀번호 검증 후 삭제하기
     * @param deleteBoardVO
     * @return boolean
     */
    @Transactional
    public void deleteBoard(DeleteBoardVO deleteBoardVO) {
        String password = boardMapper.selectPasswordById(deleteBoardVO.getBoardId());

        if (password == null) { // 존재하지 않는 게시글
            throw new NotFoundException("존재하지 않는 게시글입니다.");
        }

        // 비밀번호 해시 비교
        if(!passwordEncoder.matches(deleteBoardVO.getPasswordInput(), password)){ // 비밀번호 틀렸을 경우
            //  matches(평문, 해시) => 해시 안 salt를 꺼내 사용해서 평문을 해싱 후 비교
            throw new ValidationException("비밀번호가 틀렸습니다.");
        }

        int successCount = boardMapper.deleteBoard(deleteBoardVO.getBoardId());

        if(successCount <= 0){
            throw new BusinessException("삭제 중 오류가 발생하였습니다.");
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
    public void modifyBoard(String passwordInput, BoardVO board,
                              List<String> deleteIds, List<MultipartFile> newFiles) throws IOException {

        boolean isInvalid =
                isBlank(board.getCreateUser()) ||
                        isBlank(passwordInput) ||
                        isBlank(board.getTitle()) ||
                        isBlank(board.getContent());

        if (isInvalid) {
            throw new ValidationException("필수 항목이 작성되지 않았습니다.");
        }


        String savedPassword = boardMapper.selectPasswordById(board.getBoardId());
        if (savedPassword == null) {
            throw new NotFoundException("존재하지 않는 게시글입니다.");
        }

        // 비밀번호 해시 비교
        if(!passwordEncoder.matches(passwordInput, savedPassword)){
            throw new ValidationException("비밀번호가 일치하지 않습니다.");
        }


        // 게시물 수정
        int updatedCount = boardMapper.updateBoard(board);

        if(updatedCount <= 0){
            throw new BusinessException("게시글 수정 중 오류가 발생하였습니다.");
        }

        // 기존 첨부파일 논리 삭제
        if (deleteIds != null) {
            for (String attachmentId : deleteIds) {
                boardMapper.deleteAttachment(attachmentId);
            }
        }

        // 새 첨부파일 업로드
        int existingCount = boardMapper.selectFileList(board.getBoardId()).size();
        saveAttachment(uploadPath, newFiles, board, existingCount);


    }

    public void registerReply(ReplyVO reply) {
        if (isBlank(reply.getContent())) {
            throw new ValidationException("필수 항목이 작성되지 않았습니다.");
        }
        int count = boardMapper.insertReply(reply);
        if(count <= 0){
            throw new BusinessException("댓글 등록 중 오류가 발생하였습니다.");
        }
    }

    public void saveAttachment(String uploadPath, List<MultipartFile> attachmentList, BoardVO board, int existingCount) throws IOException {
        if (attachmentList == null || attachmentList.isEmpty()) return;

        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) uploadDir.mkdirs();

        int count = 0;
        for (MultipartFile attachment : attachmentList) { // MultipartFile : wrapper (바이너리 아님)
            if (attachment == null || attachment.isEmpty()) continue;
            if (existingCount + count >= 3) break; // 최대 3개

            String originalName = attachment.getOriginalFilename();
            String ext = originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase()
                    : "";
            if(!allowedExtList.contains(ext)) throw new ValidationException("허용되지 않는 파일 형식입니다."); // 메서드 전달 인자 부적절
            String saveName = UUID.randomUUID() + "." + ext; // 중복 방지 + 난독화

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
    }

    /**
     * 유효성 검사 공통 사용 메소드
     * @param str
     * @return boolean
     */
    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}