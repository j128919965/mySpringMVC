package lzr.demo.service;

import lzr.annotation.Service;

@Service
public class DemoService {

    public String get(String name){
        return "My name is "+name;
    }

}
