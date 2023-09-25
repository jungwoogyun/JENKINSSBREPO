package com.example.demo.domain.service;


import com.example.demo.controller.BoardController;
import com.example.demo.domain.dto.BoardDto;
import com.example.demo.domain.dto.Criteria;
import com.example.demo.domain.dto.PageDto;
import com.example.demo.domain.dto.ReplyDto;
import com.example.demo.domain.entity.Board;
import com.example.demo.domain.entity.Reply;
import com.example.demo.domain.repository.BoardRepository;
import com.example.demo.domain.repository.ReplyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.util.ArrayUtils;
import org.thymeleaf.util.StringUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.swing.text.html.Option;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class BoardService {
    private String uploadDir = "c:\\upload";

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private ReplyRepository replyRepository;


    //모든 게시물 가져오기
    @Transactional(rollbackFor = Exception.class)
    public Map<String,Object> GetBoardList(Criteria criteria) {



        Map<String,Object> returns = new HashMap<String,Object>();


        //전체게시물 건수 받기(type,Keyword가 적용된 count로 변경
        //int totalcount = (int)boardRepository.count();
        //--------------------------------------------------------
        //SEARCH
        //--------------------------------------------------------
        int totalcount=0;
        if(criteria!=null&& criteria.getType()!=null) {
            if (criteria.getType().equals("title"))
                totalcount = boardRepository.countWhereTitleKeyword(criteria.getKeyword());
            else if (criteria.getType().equals("username"))
                totalcount = boardRepository.countWhereUsernameKeyword(criteria.getKeyword());
            else if (criteria.getType().equals("content"))
                totalcount = boardRepository.countWhereContentKeyword(criteria.getKeyword());
        }
        else
            totalcount = (int)boardRepository.count();


        System.out.println("COUNT  :" + totalcount);

        //PageDto 만들기
        PageDto pagedto = new PageDto(totalcount,criteria);

        //시작 게시물 번호 구하기(수정) - OFFSET
        int offset =(criteria.getPageno()-1) * criteria.getAmount();    //1page = 0, 2page = 10

        //--------------------------------------------------------
        //SEARCH
        //--------------------------------------------------------
        List<Board> list = null;
        if(criteria!=null&& criteria.getType()!=null) {
            if (criteria.getType().equals("title")) {
                list = boardRepository.findBoardTitleAmountStart(criteria.getKeyword(), pagedto.getCriteria().getAmount(), offset);
                System.out.println("TITLE SEARCH!");
                System.out.println(list);
            } else if (criteria.getType().equals("username"))
                list = boardRepository.findBoardUsernameAmountStart(criteria.getKeyword(), pagedto.getCriteria().getAmount(), offset);
            else if (criteria.getType().equals("content"))
                list = boardRepository.findBoardContentsAmountStart(criteria.getKeyword(), pagedto.getCriteria().getAmount(), offset);
            else if (criteria.getType().equals("none"))
                list = boardRepository.findBoardAmountStart(pagedto.getCriteria().getAmount(), offset);
        }
        else
            list  =  boardRepository.findBoardAmountStart(pagedto.getCriteria().getAmount(),offset);


        returns.put("list",list);
        returns.put("pageDto",pagedto);

        return returns;


    }


    @Transactional(rollbackFor = Exception.class)
    public boolean addBoard(BoardDto dto) throws IOException {

        //--------------------------------
        System.out.println("upload File Count : " +dto.getFiles().length);


        Board board = new Board();
        board.setTitle(dto.getTitle());
        board.setContent(dto.getContent());
        board.setRegdate(LocalDateTime.now());
        board.setUsername(dto.getUsername());
        board.setCount(0L);

        MultipartFile [] files = dto.getFiles();
//        System.out.println("FILES's LENGTH : " +files.length);
//        System.out.println("FILES's SIZE : "  + files[0].getSize());

        List<String> filenames = new ArrayList<String>();
        List<String> filesizes = new ArrayList<String>();



        if(dto.getFiles().length >= 1 && dto.getFiles()[0].getSize()!=0L)
        {
            //Upload Dir 미존재시 생성
            String path = uploadDir+ File.separator+dto.getUsername()+File.separator+ UUID.randomUUID();
            File dir = new File(path);
            if(!dir.exists()) {
                dir.mkdirs();
            }
            //board에 경로 추가
            board.setDirpath(dir.toString());


            for(MultipartFile file  : dto.getFiles())
            {
                System.out.println("--------------------");
                System.out.println("FILE NAME : " + file.getOriginalFilename());
                System.out.println("FILE SIZE : " + file.getSize() + " Byte");
                System.out.println("--------------------");

                //파일명 추출
                String filename = file.getOriginalFilename();
                //파일객체 생성
                File fileobj = new File(path,filename);
                //업로드
                file.transferTo(fileobj);

                //filenames 저장
                filenames.add(filename);
                filesizes.add(file.getSize()+"");
            }
            board.setFilename(filenames.toString());
            board.setFilesize(filesizes.toString());
        }





        board =    boardRepository.save(board);

        boolean issaved =  boardRepository.existsById(board.getNo());

        return issaved;
    }


    @Transactional(rollbackFor = Exception.class)

    public Board getBoardOne(Long no) {

        Optional<Board> board =    boardRepository.findById(no);
        if(board.isEmpty())
            return null;
        else
            return board.get();
    }



    //----------------------------------------------------------------
    //수정하기코드!
    //----------------------------------------------------------------
    @Transactional(rollbackFor = SQLException.class)
    public boolean updateBoard(BoardDto dto) throws IOException {

        //--------------------------------
        System.out.println("upload File Count : " +dto.getFiles().length);
        System.out.println("BoardController.READ_DIRECTORY_PATH : " +BoardController.READ_DIRECTORY_PATH);


        Board board = new Board();
        board.setTitle(dto.getTitle());
        board.setContent(dto.getContent());
        board.setRegdate(LocalDateTime.now());
        board.setUsername(dto.getUsername());
        board.setCount(dto.getCount());

        MultipartFile [] files = dto.getFiles();
//        System.out.println("FILES's LENGTH : " +files.length);
//        System.out.println("FILES's SIZE : "  + files[0].getSize());

        List<String> filenames = new ArrayList<String>();
        List<String> filesizes = new ArrayList<String>();

        //기존 정보 가져오기
        Board oldBoard =  boardRepository.findById(dto.getNo()).get();

        if(dto.getFiles().length >= 1 && dto.getFiles()[0].getSize()!=0L)
        {
            //--------------------------------
            //Upload Dir 미존재시 생성[추가]
            //--------------------------------
            if(BoardController.READ_DIRECTORY_PATH==null){
                String path = uploadDir+ File.separator+dto.getUsername()+File.separator+ UUID.randomUUID();
                File dir = new File(path);
                if(!dir.exists()) {
                    dir.mkdirs();
                }
                BoardController.READ_DIRECTORY_PATH = path;
            }
            board.setDirpath(BoardController.READ_DIRECTORY_PATH.toString());
            //--------------------------------


            for(MultipartFile file  : dto.getFiles())
            {
                System.out.println("--------------------");
                System.out.println("FILE NAME : " + file.getOriginalFilename());
                System.out.println("FILE SIZE : " + file.getSize() + " Byte");
                System.out.println("--------------------");

                //파일명 추출
                String filename = file.getOriginalFilename();
                //파일객체 생성
                File fileobj = new File(BoardController.READ_DIRECTORY_PATH,filename);
                //업로드
                file.transferTo(fileobj);

                //filenames 저장
                filenames.add(filename);
                filesizes.add(file.getSize()+"");
            }

            //--------------------
            //기존 파일 정보와 병합
            //--------------------
            //----------------------------------------------------------------
            //코드 수정
            //----------------------------------------------------------------
            if(oldBoard.getFilename()!=null) {
                String oldFilenames = oldBoard.getFilename();
                String oldFilesizes = oldBoard.getFilesize();
                String[] old_fn_arr = oldFilenames.split(",");
                String[] old_fs_arr = oldFilesizes.split(",");

                //첫문자열에 '[' 마지막 ']' 제거
                old_fn_arr[0] = old_fn_arr[0].substring(1, old_fn_arr[0].length());
                int lastIdx = old_fn_arr.length - 1;
                old_fn_arr[lastIdx] = old_fn_arr[lastIdx].substring(0, old_fn_arr[lastIdx].lastIndexOf("]"));

                old_fs_arr[0] = old_fs_arr[0].substring(1, old_fs_arr[0].length());
                int lastIdx2 = old_fs_arr.length - 1;
                old_fs_arr[lastIdx2] = old_fs_arr[lastIdx2].substring(0, old_fs_arr[lastIdx2].lastIndexOf("]"));


                String newFilenames[] = Stream.concat(Arrays.stream(old_fn_arr), Arrays.stream(filenames.toArray())).toArray(String[]::new);
                String newFilesizes[] = Stream.concat(Arrays.stream(old_fs_arr), Arrays.stream(filesizes.toArray())).toArray(String[]::new);
                System.out.println("newFilenames : " + (Arrays.toString(newFilenames)));
                System.out.println("newFilesizes : " + (Arrays.toString(newFilesizes)));

                board.setFilename(Arrays.toString(newFilenames));
                board.setFilesize(Arrays.toString(newFilesizes));
            }
            else {
                board.setFilename(filenames.toString());
                board.setFilesize(filesizes.toString());
            }


        }
        else {
            board.setFilename(oldBoard.getFilename());
            board.setFilesize(oldBoard.getFilesize());
        }
        board.setNo(oldBoard.getNo());

        board =  boardRepository.save(board);


        return board!=null;

    }


    @Transactional(rollbackFor = SQLException.class)
   public boolean removeFile(String no,String filename){

        Long num = Long.parseLong(no);
        filename = filename.trim();
       //------------------------------------------------
        //파일삭제도 하고
       //------------------------------------------------
        File file = new File(BoardController.READ_DIRECTORY_PATH,filename);
        if(!file.exists()){
            System.out.println("없음.. " + BoardController.READ_DIRECTORY_PATH);
            return false;
        }

        //------------------------------------------------
        //DB도 지우고
       //------------------------------------------------
        Board readBoard = boardRepository.findById(num).get();
        String fn []=  readBoard.getFilename().split(",");
        String fs []=  readBoard.getFilesize().split(",");

       //첫문자열에 '[' 마지막 ']' 제거
       fn[0] = fn[0].substring(1, fn[0].length());
       int lastIdx = fn.length-1;
       fn[lastIdx] = fn[lastIdx].substring(0,fn[lastIdx].lastIndexOf("]"));

       fs[0] = fs[0].substring(1, fs[0].length());
       int lastIdx2 = fs.length-1;
       fs[lastIdx2] = fs[lastIdx2].substring(0,fs[lastIdx2].lastIndexOf("]"));

        List<String> newFn  = new ArrayList();
        List<String> newFs  = new ArrayList();

        for(int i=0;i<fn.length;i++)
        {
            if(!StringUtils.contains(fn[i],filename)){
                System.out.println("보존 FILENAME : " + fn[i]);
                newFn.add(fn[i]);
                newFs.add(fs[i]);
            }


        }

       readBoard.setFilename(newFn.toString());
       readBoard.setFilesize(newFn.toString());
       readBoard = boardRepository.save(readBoard);


        return file.delete();


    }


    @Transactional(rollbackFor = SQLException.class)
    public boolean removeBoard(Long no) {

        Board board = boardRepository.findById(no).get();
        String removePath =BoardController.READ_DIRECTORY_PATH;

        //파일 있으면삭제
        File dir = new File(removePath);
        if(dir.exists()){
            File files[] = dir.listFiles();
            for(File file : files){
                file.delete();
            }
            dir.delete();
        }
        //DB 삭제
        boardRepository.delete(board);

        return boardRepository.existsById(no);
    }


    //----------------------------------------------------------------
    // COUNT
    //----------------------------------------------------------------
    @Transactional(rollbackFor = SQLException.class)
    public void count(Long no) {
        Board board =  boardRepository.findById(no).get();
        board.setCount(board.getCount()+1);
        boardRepository.save(board);
    }


    //----------------------------------------------------------------
    // REPLY ADD
    //----------------------------------------------------------------
    public void addReply(Long bno,String contents, String username) {
        Reply reply = new Reply();
        Board board = new Board();
        board.setNo(bno);

        reply.setBoard(board);
        reply.setContent(contents);
        reply.setUsername(username);
        reply.setRegdate(LocalDateTime.now());
        reply.setLikecount(0L);
        reply.setUnlikecount(0L);

        replyRepository.save(reply);
    }

    //----------------------------------------------------------------
    // REPLY LIST
    //----------------------------------------------------------------
    public List<ReplyDto> getReplyList(Long bno) {
        List<Reply> replyList =  replyRepository.GetReplyByBnoDesc(bno);

        List<ReplyDto> returnReply  = new ArrayList();
        ReplyDto dto = null;

        if(!replyList.isEmpty()) {
            for(int i=0;i<replyList.size();i++) {

                dto = new ReplyDto();
                dto.setBno(replyList.get(i).getBoard().getNo());
                dto.setRno(replyList.get(i).getRno());
                dto.setUsername(replyList.get(i).getUsername());
                dto.setContent(replyList.get(i).getContent());
                dto.setLikecount(replyList.get(i).getLikecount());
                dto.setUnlikecount(replyList.get(i).getUnlikecount());
                dto.setRegdate(replyList.get(i).getRegdate());

                returnReply.add(dto);

            }
            return returnReply;
        }

        return null;

    }


    //----------------------------------------------------------------
    // REPLY COUNT By BNO
    //----------------------------------------------------------------

    public Long getReplyCount(Long bno) {
        return replyRepository.GetReplyCountByBnoDesc(bno);

    }


    public void deleteReply(Long rno) {
            replyRepository.deleteById(rno);
    }

    public void thumbsUp(Long rno) {
        Reply reply =  replyRepository.findById(rno).get();
        reply.setLikecount(reply.getLikecount()+1L);
        replyRepository.save(reply);
    }

    public void thumbsDown(Long rno) {
        Reply reply =  replyRepository.findById(rno).get();
        reply.setUnlikecount(reply.getUnlikecount()+1L);
        replyRepository.save(reply);
    }
}
