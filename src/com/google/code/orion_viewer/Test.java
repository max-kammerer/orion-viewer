package com.google.code.orion_viewer;

import java.util.regex.Pattern;

/**
 * User: mike
 * Date: 25.01.12
 * Time: 13:39
 */
public class Test {
    public static void main(String[] args) {
        System.out.println(Pattern.compile("[0-3]?[0-9]{1}").matcher("33").matches());
    }
}
