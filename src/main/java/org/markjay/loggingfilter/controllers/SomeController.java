package org.markjay.loggingfilter.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SomeController {

    @RequestMapping("/")
    @ResponseBody
    public String greeting() {
        return "Hello World";
    }

}