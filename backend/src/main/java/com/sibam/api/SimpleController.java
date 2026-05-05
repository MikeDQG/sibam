package com.sibam.api;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/simple-api")
@CrossOrigin("*")
public class SimpleController {

    @GetMapping("/test")
    public String test(){
        return "test successfull";
    }
}
