package com.mypopsy.slidinguppanelayout.demo.utils;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class LoremPixel {
    private static final String[] CATEGORIES = new String[]{"abstract","animals","business","cats","city","food","nightlife","fashion","people","nature","sports","technics","transport"};
    private static final Random sRandom = new Random();

    public static String random(int width, int height) {
        return "http://lorempixel.com/"+width+"/"+height+"/"+CATEGORIES[sRandom.nextInt(CATEGORIES.length)]+"/"+(1+sRandom.nextInt(10));
    }

    public static Set<String> random(int count, int width, int height) {
        count = Math.min(count, CATEGORIES.length*10);
        Set<String> set = new HashSet<>();
        while(set.size() != count)
            set.add(random(width, height));
        return set;
    }
}
