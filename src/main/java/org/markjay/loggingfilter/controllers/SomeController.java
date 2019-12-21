package org.markjay.loggingfilter.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.markjay.loggingfilter.model.SomeEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Controller
public class SomeController {

    @RequestMapping("/index")
    @ResponseBody
    public String greeting() {
        return "Hello World";
    }

    @PostMapping("/examples/post/by-param")
    @ResponseBody
    public SomeEntity getByRequestParam(
            @RequestParam String parameter1,
            @RequestHeader Map<String, String> headers,
            HttpServletRequest request
    ) {
        return new SomeEntity(String.format("body='parameter1=%s', headers='%s'", parameter1, headers.toString()));
    }

    @PostMapping(value = "/examples/post/by-json-body")
    @ResponseBody
    public SomeEntity get(
            @RequestBody SomeEntity someEntity,
            @RequestHeader Map<String, String> headers,
            HttpServletRequest request
    ) throws JsonProcessingException {
        return new SomeEntity(String.format("body='parameter1=%s', headers='%s'", new ObjectMapper().writeValueAsString(someEntity), headers.toString()));
    }
}