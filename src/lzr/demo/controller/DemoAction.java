package lzr.demo.controller;

import lzr.annotation.AutoWired;
import lzr.annotation.Controller;
import lzr.annotation.RequestMapping;
import lzr.annotation.RequestParam;
import lzr.demo.service.DemoService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
@RequestMapping("/demo")
public class DemoAction {

    @AutoWired
    private DemoService demoService;

    @RequestMapping("/query")
    public void query(HttpServletRequest request, HttpServletResponse response,@RequestParam("name") String name){
        System.out.println(demoService);
        String result = demoService.get(name);
        try {
            response.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequestMapping("/add")
    public void add(HttpServletRequest request, HttpServletResponse response,@RequestParam("a") Integer a,@RequestParam("b") Integer b){
        try {
            response.getWriter().write(a+"+"+b+"="+(a+b));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
