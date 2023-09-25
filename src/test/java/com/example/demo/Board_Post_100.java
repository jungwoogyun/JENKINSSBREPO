package com.example.demo;


import com.example.demo.domain.entity.Board;
import com.example.demo.domain.repository.BoardRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
public class Board_Post_100 {

    @Autowired
    private BoardRepository boardRepository;;

    @Test
    public void Post_100() throws Exception {
        for(Long i = 1L; i<=1000; i++){

            Board board = Board.builder()
                    .no(i)
                    .count(0L)
                    .content("내용"+i)
                    .dirpath(null)
                    .filename(null)
                    .filesize(null)
                    .regdate(LocalDateTime.now())
                    .title("제목"+i)
                    .username("user"+i+"@naver.com")
                    .build();
            boardRepository.save(board);
        }

    }

    @Test
    public void test2(){

        List<Board> list =  boardRepository.findBoardAmountStart(10,10);
        System.out.println(list);

    }


}
