package com.budget.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {
    @GetMapping(value = {"/", "/dashboard", "/import"})
    public String index() {
        return "forward:/index.html";
    }
}
