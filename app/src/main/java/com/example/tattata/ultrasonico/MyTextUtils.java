package com.example.tattata.ultrasonico;


import java.util.List;

/**
 * Created by tattata on 2018/01/21.
 */

public class MyTextUtils {
    public MyTextUtils() {
    }
    public int[] textToDigital(String msg) {
        int[] result = new int[msg.length() * 5];
        for(int i = 0; i < msg.length(); i++) {
            char c = msg.charAt(i);
            int num = c - 'a';
            for(int j = 4; j >= 0; j--) {
                result[i*5 + j] = num % 2;
                num = num / 2;
            }
        }
        return result;
    }
    public String DigitalToText(List<Integer> digitals) {
        String result = "";
        int base = 16;
        int c = 0;
        for(int i = 0; i < digitals.size(); i++) {
            c += base * digitals.get(i);
            base /= 2;
            if(base == 0) {
                base = 16;
                result += (char)('a' + c);
                c = 0;
            }
        }
        return result;
    }
}
