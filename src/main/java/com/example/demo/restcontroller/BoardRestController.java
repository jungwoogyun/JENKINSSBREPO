package com.example.demo.restcontroller;


import com.example.demo.controller.BoardController;
import com.example.demo.domain.dto.ReplyDto;
import com.example.demo.domain.entity.Board;
import com.example.demo.domain.entity.Reply;
import com.example.demo.domain.service.BoardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.List;

@RestController
@RequestMapping("/board")
@Slf4j
public class BoardRestController {

    @Autowired
    private BoardService boardService;

    //------------------
    //FILEDOWNLOAD
    //------------------

    @GetMapping(value="/download" ,produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> Download(String filename) throws UnsupportedEncodingException
    {
        filename = filename.trim();
        System.out.println("GET /board/download filename : " + filename);
        String path  = BoardController.READ_DIRECTORY_PATH + File.separator + filename;
        log.info("RestController_03Download's Download Call.." + path);
        System.out.println("GET /board/download path : " + path);
        //FileSystemResource : 파일시스템의 특정 파일로부터 정보를 가져오는데 사용
        Resource resource = new FileSystemResource(path);
        //파일명 추출
        filename = resource.getFilename();
        //헤더 정보 추가
        HttpHeaders headers = new HttpHeaders();
        //ISO-8859-1 : 라틴어(특수문자등 깨짐 방지)
        headers.add("Content-Disposition","attachment; filename="+new String(filename.getBytes("UTF-8"),"ISO-8859-1"));
        //리소스,파일정보가 포함된 헤더,상태정보를 전달
        return new ResponseEntity<Resource>(resource,headers, HttpStatus.OK);

    }



    //-------------------
    // 수정하기
    //-------------------
    @PutMapping("/put/{no}/{filename}")
    public String put(@PathVariable String no, @PathVariable String filename)
    {
        log.info("PUT /board/put " + no + " " + filename);
        boolean isremoved = boardService.removeFile(no,filename);
        return "success";
    }


    //-------------------
    // 삭제하기
    //-------------------
    @DeleteMapping("/delete")
    public String delete(Long no){
        log.info("DELETE /board/delete no " + no);

        boolean isremoved =  boardService.removeBoard(no);
        if(isremoved)
            return "success";
        else
            return "failed";

    }



    
    //-------------------
    //댓글추가
    //-------------------
    @GetMapping("/reply/add")
    public void addReply(Long bno,String contents , String username){
        log.info("GET /board/reply/add " + bno + " " + contents + " " + username);
        boardService.addReply(bno,contents, username);
    }
    //-------------------
    //댓글 조회
    //-------------------
    @GetMapping("/reply/list")
    public List<ReplyDto> getListReply(Long bno){
        log.info("GET /board/reply/list " + bno);
        List<ReplyDto> list =  boardService.getReplyList(bno);
        return list;
    }
    //-------------------
    //댓글 카운트
    //-------------------
    @GetMapping("/reply/count")
    public Long getCount(Long bno){
        log.info("GET /board/reply/count " + bno);
        Long cnt = boardService.getReplyCount(bno);

        return cnt;
    }




    
    //-------------------
    //댓글삭제
    //-------------------

    //-------------------
    //댓글수정
    //-------------------






}
