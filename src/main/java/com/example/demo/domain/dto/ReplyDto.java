package com.example.demo.domain.dto;


import com.example.demo.domain.entity.Board;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReplyDto {

    private Long rno;
    private Long bno;
    private String username;
    private String content;
    private Long likecount;       //좋아요 Count
    private Long unlikecount;     //싫어요 Count
    private LocalDateTime regdate;  // 등록날자
}
