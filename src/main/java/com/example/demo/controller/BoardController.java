package com.example.demo.controller;


import com.example.demo.domain.dto.BoardDto;
import com.example.demo.domain.dto.Criteria;
import com.example.demo.domain.dto.PageDto;
import com.example.demo.domain.entity.Board;
import com.example.demo.domain.repository.BoardRepository;
import com.example.demo.domain.service.BoardService;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.core.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/board")
@Slf4j
public class BoardController {

//    @Autowired
//    private BoardRepository boardRepository;



    @Autowired
    private BoardService boardService;

    public static String READ_DIRECTORY_PATH ;


    //-------------------
    //-------------------
    @GetMapping("/list")
    public String list(Integer pageNo,String type, String keyword, Model model, HttpServletResponse response)
    {
        log.info("GET /board/list... " + pageNo + " " + type +" " + keyword);

        //----------------
        //PageDto  Start
        //----------------
        Criteria criteria = null;
        if(pageNo==null) {
            //최초 /board/list 접근
            pageNo=1;
            criteria = new Criteria();  //pageno=1 , amount=10
        }
        else {
            criteria = new Criteria(pageNo,10); //페이지이동 요청 했을때
        }
        //--------------------
        //Search
        //--------------------
        criteria.setType(type);
        criteria.setKeyword(keyword);


        //서비스 실행
        Map<String,Object> map = boardService.GetBoardList(criteria);

        PageDto pageDto = (PageDto) map.get("pageDto");
        List<Board> list = (List<Board>) map.get("list");


        //Entity -> Dto
        List<BoardDto>  boardList =  list.stream().map(board -> BoardDto.Of(board)).collect(Collectors.toList());
        System.out.println(boardList);

        //View 전달
        model.addAttribute("boardList",boardList);
        model.addAttribute("pageNo",pageNo);
        model.addAttribute("pageDto",pageDto);

        //--------------------------------
        //COUNT UP - //쿠키 생성(/board/read.do 새로고침시 조회수 반복증가를 막기위한용도)
        //--------------------------------
        Cookie init = new Cookie("reading","true");
        response.addCookie(init);
        //--------------------------------

        return "board/list";
    }


    //-------------------
    // POST
    //-------------------
    @GetMapping("/post")
    public void get_addBoard(){
        log.info("GET /board/post");
    }

    @PostMapping("/post")
    public String post_addBoard(@Valid BoardDto dto, BindingResult bindingResult, Model model) throws IOException {
        log.info("POST /board/post " + dto + " " + dto);

        //유효성 검사
        if(bindingResult.hasFieldErrors()) {
            for( FieldError error  : bindingResult.getFieldErrors()) {
                log.info(error.getField()+ " : " + error.getDefaultMessage());
                model.addAttribute(error.getField(), error.getDefaultMessage());
            }
            return "/board/post";
        }

        //서비스 실행
        boolean isadd = boardService.addBoard(dto);

        if(isadd) {
            return "redirect:/board/list";
        }
        return "redirect:/board/post";


    }



    //-------------------
    // READ
    //-------------------

    @GetMapping("/read")
    public String read(Long no,Integer pageNo, Model model,HttpServletRequest request, HttpServletResponse response) {
        log.info("GET /board/read : " + no);

       //서비스 실행
       Board board =  boardService.getBoardOne(no);

       BoardDto dto = new BoardDto();
       dto.setNo(board.getNo());
       dto.setTitle(board.getTitle());
       dto.setContent(board.getContent());
       dto.setRegdate(board.getRegdate());
       dto.setUsername(board.getUsername());
       dto.setCount(board.getCount());

       System.out.println("FILENAMES : " + board.getFilename());
       System.out.println("FILESIZES : " + board.getFilesize());

       String filenames[] = null;
       String filesizes[] = null;
       if(board.getFilename()!=null){
           //첫문자열에 [ 제거
           filenames = board.getFilename().split(",");
           filenames[0] = filenames[0].substring(1, filenames[0].length());
           //마지막 문자열에 ] 제거
           int lastIdx = filenames.length-1;
           System.out.println("filenames[lastIdx] : " + filenames[lastIdx].substring(0,filenames[lastIdx].lastIndexOf("]")));
           filenames[lastIdx] = filenames[lastIdx].substring(0,filenames[lastIdx].lastIndexOf("]"));

           model.addAttribute("filenames", filenames);
       }
       if(board.getFilesize()!=null){
            //첫문자열에 [ 제거
           filesizes = board.getFilesize().split(",");
           filesizes[0] = filesizes[0].substring(1, filesizes[0].length());
           //마지막 문자열에 ] 제거
           int lastIdx = filesizes.length-1;
           System.out.println("filesizes[lastIdx] : " + filesizes[lastIdx].substring(0,filesizes[lastIdx].lastIndexOf("]")));
           filesizes[lastIdx] = filesizes[lastIdx].substring(0,filesizes[lastIdx].lastIndexOf("]"));



           model.addAttribute("filesizes", filesizes);
       }


       if(board.getDirpath()!=null){
           //model.addAttribute("dirpath",  board.getDirpath());
           //--------------------------------------------------------
           // FILEDOWNLOAD 추가
           //--------------------------------------------------------
           this.READ_DIRECTORY_PATH = board.getDirpath();
       }
       model.addAttribute("boardDto",dto);
       model.addAttribute("pageNo",pageNo);


        //-------------------
        // COUNTUP
        //-------------------

        //쿠키 확인 후  CountUp(/board/read.do 새로고침시 조회수 반복증가를 막기위한용도)
        Cookie[] cookies = request.getCookies();
        if(cookies!=null)
        {
            for(Cookie cookie:cookies)
            {
                if(cookie.getName().equals("reading"))
                {
                    if(cookie.getValue().equals("true"))
                    {
                        //CountUp
                        System.out.println("COOKIE READING TRUE | COUNT UP");
                        boardService.count(board.getNo());
                        //쿠키 value 변경
                        cookie.setValue("false");
                        response.addCookie(cookie);
                    }
                }
            }
        }
        return "board/read";

    }

    @GetMapping("/update")
    public void update(Long no,Model model){
        log.info("GET /board/update no " + no);


        //서비스 실행
        Board board =  boardService.getBoardOne(no);

        BoardDto dto = new BoardDto();
        dto.setNo(board.getNo());
        dto.setTitle(board.getTitle());
        dto.setContent(board.getContent());
        dto.setRegdate(board.getRegdate());
        dto.setUsername(board.getUsername());
        dto.setCount(board.getCount());


        System.out.println("FILENAMES : " + board.getFilename());
        System.out.println("FILESIZES : " + board.getFilesize());

        String filenames[] = null;
        String filesizes[] = null;
        if(board.getFilename()!=null){
            //첫문자열에 [ 제거
            filenames = board.getFilename().split(",");
            filenames[0] = filenames[0].substring(1, filenames[0].length());
            //마지막 문자열에 ] 제거
            int lastIdx = filenames.length-1;
            System.out.println("filenames[lastIdx] : " + filenames[lastIdx].substring(0,filenames[lastIdx].lastIndexOf("]")));
            filenames[lastIdx] = filenames[lastIdx].substring(0,filenames[lastIdx].lastIndexOf("]"));

            model.addAttribute("filenames", filenames);
        }
        if(board.getFilesize()!=null){
            //첫문자열에 [ 제거
            filesizes = board.getFilesize().split(",");
            filesizes[0] = filesizes[0].substring(1, filesizes[0].length());
            //마지막 문자열에 ] 제거
            int lastIdx = filesizes.length-1;
            System.out.println("filesizes[lastIdx] : " + filesizes[lastIdx].substring(0,filesizes[lastIdx].lastIndexOf("]")));
            filesizes[lastIdx] = filesizes[lastIdx].substring(0,filesizes[lastIdx].lastIndexOf("]"));



            model.addAttribute("filesizes", filesizes);
        }


        if(board.getDirpath()!=null){
            //model.addAttribute("dirpath",  board.getDirpath());
            //--------------------------------------------------------
            // FILEDOWNLOAD 추가
            //--------------------------------------------------------
            this.READ_DIRECTORY_PATH = board.getDirpath();
        }

        model.addAttribute("boardDto",dto);

    }

    @PostMapping("/update")
    public String Post_update(@Valid BoardDto dto, BindingResult bindingResult, Model model) throws IOException {
        log.info("POST /board/update dto " + dto);

        if(bindingResult.hasFieldErrors()) {
            for( FieldError error  : bindingResult.getFieldErrors()) {
                log.info(error.getField()+ " : " + error.getDefaultMessage());
                model.addAttribute(error.getField(), error.getDefaultMessage());
            }
            return "board/read";
        }

        //서비스 실행
        boolean isadd = boardService.updateBoard(dto);

        if(isadd) {
            return "redirect:/board/read?no="+dto.getNo();
        }
        return "redirect:/board/update?no="+dto.getNo();

    }



    //--------------------------------
    // /Board/reply/delete
    //--------------------------------
    @GetMapping("/reply/delete/{bno}/{rno}")
    public String delete(@PathVariable Long bno, @PathVariable Long rno){
        log.info("GET /board/reply/delete bno,rno " + rno + " " + rno);

        boardService.deleteReply(rno);

        return "redirect:/board/read?no="+bno;
    }

    //--------------------------------
    // /board/reply/thumbsup
    //--------------------------------
    @GetMapping("/reply/thumbsup")
    public String thumbsup(Long bno, Long rno)
    {

        boardService.thumbsUp(rno);
        return "redirect:/board/read?no="+bno;
    }
    //--------------------------------
    // /board/reply/thumbsdown
    //--------------------------------
    @GetMapping("/reply/thumbsdown")
    public String thumbsudown(Long bno, Long rno)
    {
        boardService.thumbsDown(rno);
        return "redirect:/board/read?no="+bno;
    }

}
