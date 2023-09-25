package com.example.demo.controller;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j

public class HomeController {

    @GetMapping("/")
    public String home(){
        log.info("/GET /");
        return "index";
    }
    @GetMapping("/templates")
    public void templates(){
        log.info("/GET /templates");
    }

}
