package space.caoshd.genetest.service;


import space.caoshd.genetest.repository.ParseRepository;

import java.util.List;


public class ParseService {

    public String name;


    private ParseRepository parseRepository;

    public int age;

    public void selectUserName(String username) {
    }

    private String condition1(String name, Integer age, List<String> children) {
        if (name == null) {
            // case_1
            if ("str".equals("hello")) {
                // case_1_1
                return "hello1";
            } else {
                // case_1_2
                return "";
            }
        } else {
            if (name.isEmpty()) {
                // case_2
                return name;
            } else {
                // case_3
                if (name.equals("hello")) {
                    // case_3_1
                    return "hello1";
                } else {
                    // case_3_2
                    return "";
                }
            }
        }
    }

    public String condition2(Integer name) {
        return name == null ? "" : name.toString();
    }


    public String condition3(Integer name) {
        try {
            if ("str".equals("hello")) {
                // case_1_1
                return "hello1";
            } else {
                // case_1_2
                return "";
            }
        } catch (Exception e) {
            return "1";
        } finally {
            return "0";
        }
    }


}
