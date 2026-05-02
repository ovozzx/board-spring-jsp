package com.board.controller;

import com.board.service.BoardService;
import com.board.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.List;



// TODO : 다음에 할 것 => rest api, react
/**
 * 롬복, map structure 사용 필수
 * swagger 문서 뽑기
 * react 컴포넌트 많이 만들지말기 일단 되게만 하기 => 나중에 분리 ** next로 **
 * dto => request / response (dto 폴더 안에) (ex) requestWrite (엔드포인트당 2개씩, 아닌 경우 나타나면 고민해보기)
 * entity 만들어보기. dto, 엔티티 어디까지 쓰이는지 : 컨트롤러에서 엔티티로 전달 (내부 외부 단절)
 * 컨트롤러 : dto, 엔티티 둘다 알고 > 서비스 : dto > rep : 엔티티
 * 전달 데이터가 바뀔 때 컨트롤러는 바뀌어야 함 (서비스, repo는 그대로)
 * 없어도 되는 건 없어야 됨 (소스) => 즉시 삭제 (나중에 보면 어렵)
 * mybatis mapper 이름 => - 케밥케이스 xml
 * css, js, 정적 html => 웹에 노출되는 건 다 케밥케이스
 * 레퍼런스 타입, 프리미티브 타입 => 공부해오기 설명 가능하도록(메모리 사용 영역)
 * 디버깅 요령 : 시나리오 (추정 가설) 여러개 => 검증 (재현이 가능해야함)
 *  - end to end : 영역별로 쪼개서 찾음
 */
// 페이지네이션 & 수정 첨부파일
@Controller
@RequestMapping("/board")
public class BoardController {
    //  @Value는 Spring이 관리하는 빈(@Component, @Service, @Controller 등) 안에서만 동작
    @Value("${board.page-size}")
    private int pageSize;

    @Value("${board.page-group-size}")
    private int pageGroupSize;

    @Autowired
    private BoardService service;


    /**
     * 게시글 목록 조회
     * @param searchVO : 검색 조건, 페이지
     * @param model
     * @return view
     * dto : Data Transfer Object => 래핑해서 넘기자 구체적인 용도 (vo가 더 큰 범위)
     * 용도별 vo 구성 달라지는 거라서 dto는 용도별로 다르게 생길수밖에 없음
     */
    @GetMapping("/list")
    public String getBoardList(
            SearchVO searchVO,
            Model model
    ) {
        // TODO : 비즈니스 로직 컨트롤러에 너무 많음..
        // 컨트롤러는 유효성 검증 정도만 >
        // 카테고리
        searchVO.setPageSize(pageSize);
        BoardListViewVO boardListViewVO = service.getBoardListView(searchVO);
        int allBoardCount = boardListViewVO.getBoardListCount();
        int pageCount = (int) Math.ceil((double) allBoardCount / pageSize);
        //if (pageCount == 0) pageCount = 1;

        // 검색 날짜 조건 default 설정
        LocalDate today = LocalDate.now();

        if (searchVO.getStartDate() == null || searchVO.getStartDate().isEmpty()) {
            searchVO.setStartDate(today.minusYears(1).toString());
        }

        if (searchVO.getEndDate() == null || searchVO.getEndDate().isEmpty()) {
            searchVO.setEndDate(today.toString());
        }

        // 페이지네이션 계산 => 서비스로 넘기기
        // TODO : 4개 정보 (필수 필요) => 데이터만 전달하고 화면에서 렌더링하는 게 맞음
        // mvc : model (데이터) view (화면 = jsp) controller (매핑)
        // request scope : model에 넣은 걸 화면에서 쓸 수 있는 이유
        // session : 장바구니, 쿠키 세션 key 보고 조회 (서버)
        // 쿠키 : key (클라이언트)
        // int currentPage = searchVO.getPage() < 1 ? 1 : searchVO.getPage();
        int currentPage = searchVO.getPage();
        //int startPage = ((currentPage - 1) / pageGroupSize) * pageGroupSize + 1; //  int / int는 결과가 즉시 int (소수점 버려짐)
        //int endPage = Math.min(startPage + pageGroupSize - 1, pageCount); // 남은 페이지가 그룹 크기보다 적을 때 포함

        model.addAttribute("searchVO", searchVO);
        model.addAttribute("categoryList", boardListViewVO.getCategoryList());
        model.addAttribute("boardList", boardListViewVO.getBoardList());
        model.addAttribute("boardListCount", boardListViewVO.getBoardListCount());
        // 페이지네이션 정보
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("pageCount", pageCount);
        model.addAttribute("pageGroupSize", pageGroupSize);
        //model.addAttribute("startPage", startPage);
        //model.addAttribute("endPage", endPage);

        return "board/list";
    }

    /**
     * 게시글 상세 보기
     * @param boardId
     * @param model
     * @return view
     */
    @GetMapping("/view")
    public String getBoardDetailById(@RequestParam(required = false) String boardId, Model model){

        BoardDetailViewVO boardDetailViewVO = service.getDetailBoardById(boardId);

        model.addAttribute("replyList", boardDetailViewVO.getReplyList());
        model.addAttribute("fileList", boardDetailViewVO.getFileList());
        model.addAttribute("board", boardDetailViewVO.getBoard());

        return "board/view";
    }

    /***
     * 게시글 작성 화면
     * @param model
     * @return view
     */
    @GetMapping("/write")
    public String viewWritePage(Model model){
        List<CategoryVO> categoryList = service.getCategoryList();
        model.addAttribute("categoryList", categoryList);
        return "board/write";
    }

    /**
     * 게시글 수정 화면
     * @param boardId
     * @param model
     * @return view
     */
    @GetMapping("/modify")
    public String viewModifyPage(@RequestParam(required = false) String boardId, Model model){
        // 수정 시에는 조회수 미증가
        BoardModifyVO boardModifyVO = service.getModifyBoardById(boardId);

        model.addAttribute("fileList", boardModifyVO.getFileList());
        model.addAttribute("board", boardModifyVO.getBoard());

        return "board/modify";
    }

    /**
     * 첨부파일 다운로드
     * @param fileId
     * @return ResponseEntity
     * @throws IOException
     */
    @GetMapping("/download")
    public ResponseEntity<ResourceRegion> downloadFile(@RequestParam String fileId, @RequestHeader HttpHeaders headers) throws IOException {
        // Resource는 "파일 전체", ResourceRegion은 "그 파일의 일부분 구간"
        AttachmentVO attachment = service.getAttachmentById(fileId);

        if (attachment == null) {
            return ResponseEntity.notFound().build();
        }

        File file = new File(attachment.getFilePath() + File.separator + attachment.getSaveName());

        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file); // 디스크의 파일을 HTTP 응답 바디로 스트리밍할 수 있게 래핑

        // 파일 부분 다운로드
        long contentLength = resource.contentLength();

        final long CHUNK_SIZE = 1024 * 1024; // 1MB씩 (버퍼 크기)

        // Range 헤더 파싱
        List<HttpRange> ranges = headers.getRange();
        ResourceRegion region;

        if (ranges.isEmpty()) {
            // 클라이언트가 Range 안 보냈을 때: 처음부터 CHUNK_SIZE 만큼만
            long rangeLength = Math.min(CHUNK_SIZE, contentLength);
            region = new ResourceRegion(resource, 0, rangeLength);
        } else {
            // 예: "Range: bytes=1048576-" → 1MB 지점부터
            HttpRange range = ranges.get(0);
            long start = range.getRangeStart(contentLength);
            long end = range.getRangeEnd(contentLength);
            long rangeLength = Math.min(CHUNK_SIZE, end - start + 1);
            region = new ResourceRegion(resource, start, rangeLength);
        }

        String encodedName = URLEncoder.encode(attachment.getOriginalName(), "UTF-8")
                .replaceAll("\\+", "%20");

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT) // 206
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes") // ← 클라에게 "Range 지원함" 알림
                .body(region); // 실제 파일 데이터를 응답 바디에 실어서 전송
        // 이후에 실제 바이너리 전송 시작 (디스크 → 메모리(작은 버퍼) → 소켓 → 브라우저)
    }

    /**
     * 게시글 등록하기
     * @param board
     * @param attachmentList
     * @return view
     * @throws IOException
     */
    @PostMapping("/write")
    public String registerBoard(BoardVO board
    , @RequestParam(required = false) List<MultipartFile> attachmentList
                                ) throws IOException {

        service.registerBoard(board, attachmentList);
        return "redirect:/board/list";

    }

    /**
     * 게시글 수정하기
     * @param passwordInput
     * @param board
     * @param deleteIds
     * @param newFiles
     * @return view
     * @throws IOException
     */
    @PostMapping("/modify")
    public String modifyBoard(
            @RequestParam String passwordInput,
            BoardVO board,
            @RequestParam(required = false) List<String> deleteIds,
            @RequestParam(required = false) List<MultipartFile> newFiles) throws IOException {

        service.modifyBoard(passwordInput, board, deleteIds, newFiles);

        return "redirect:/board/view?boardId=" + board.getBoardId();
    }

    /**
     * 게시글 삭제하기
     * @param deleteBoardVO : passwordInput, boardId
     * @param model
     * @return view
     * @throws IOException
     */
    @PostMapping("/delete")
    public String deleteBoard(DeleteBoardVO deleteBoardVO, Model model) throws IOException {
        // 이 흐름이 맞음
        // 컨트롤러 말고 global 핸들러에서 하도록 (한곳에서 처리)
        // 첨부파일, 댓글이 있는 게시물 삭제! => 실제 바이너리 삭제는 어떻게 할지 (정책에 따라)
        // 삭제 시, 본문 글만 지우는 경우 등 여러 케이스 존재 (고민).. cascade

        service.deleteBoard(deleteBoardVO); // 결과 cnt 피하기
        model.addAttribute("alertMsg", "삭제가 완료되었습니다.");
        model.addAttribute("redirectUrl", "/board/list");
        return "board/alert";


    }

    @PostMapping("/reply/write")
    public String registerReply(ReplyVO reply) {
        service.registerReply(reply);
        return "redirect:/board/view?boardId=" + reply.getBoardId();
    }


}
