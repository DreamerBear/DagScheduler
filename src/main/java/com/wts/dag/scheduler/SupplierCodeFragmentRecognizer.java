package com.wts.dag.scheduler;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Console;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 供应商编码片段识别
 *
 * @author: xuchao（xuchao.xxc@ncarzone.com）
 * @date: 2022/8/11 下午2:05
 */
public class SupplierCodeFragmentRecognizer {

    private final Pattern joinerPattern = Pattern.compile("[^a-zA-Z0-9]", Pattern.CASE_INSENSITIVE);
    private String supplierCodeJoiner;
    private Pattern supplierCodeFragmentPattern;
    private Set<String> supplierCodeFragmentSet;

    public static String test(String text) {
        StopWatch stopWatch = StopWatch.create("正则耗时分析");
        stopWatch.start("正则构建");
        SupplierCodeFragmentRecognizer supplierCodeFragmentRecognizer = new SupplierCodeFragmentRecognizer();
        supplierCodeFragmentRecognizer.supplierCodeJoiner = "\\.—_\\-\\|";
        supplierCodeFragmentRecognizer.init();
        stopWatch.stop();

        stopWatch.start("正则匹配");
        List<String> supplierCodeFragmentList = supplierCodeFragmentRecognizer.fetchSupplierCodeFragmentListFromText(text, true);
        stopWatch.stop();
        Console.log(stopWatch.prettyPrint(TimeUnit.MILLISECONDS));

        return String.join("\t", supplierCodeFragmentList);
    }


    public void init() {
        supplierCodeFragmentPattern = Pattern.compile(String.format("(\\b|[^a-zA-Z0-9%s])([a-zA-Z0-9%s]+)(\\b|[^a-zA-Z0-9%s])", supplierCodeJoiner, supplierCodeJoiner, supplierCodeJoiner), Pattern.CASE_INSENSITIVE);
        supplierCodeFragmentSet = new HashSet<>(80000);
        String queryNames = ResourceUtil.readStr("keyword.txt", Charset.defaultCharset());
        String[] split = queryNames.split("\n");
        for (int i = 0; i < split.length; i++) {
            supplierCodeFragmentSet.add(split[i].toLowerCase());
        }
    }


    private Set<String> getSupplierCodeFragmentSet() {
        return supplierCodeFragmentSet;
    }

    private List<String> fetchSupplierCodeFragmentListFromText(String text, boolean needFullMatch) {
        //1.正则切分,去除非数字英文
        List<String> supplierCodeFragmentList = new ArrayList<>();
        if (text == null || text.length() < 3) {
            return supplierCodeFragmentList;
        }
        Matcher matcher = supplierCodeFragmentPattern.matcher(text);
        while (matcher.find()) {
            String supplierCodeFragment = joinerPattern.matcher(matcher.group(2)).replaceAll("");
            if (StringUtils.isNotBlank(supplierCodeFragment) && supplierCodeFragment.length() >= 3) {
                supplierCodeFragmentList.add(supplierCodeFragment.toLowerCase());
            }
        }
        if (CollectionUtils.isEmpty(supplierCodeFragmentList) || !needFullMatch) {
            return supplierCodeFragmentList;
        }
        //2.词库全匹配
        Set<String> supplierCodeFragmentSet = getSupplierCodeFragmentSet();
        return new ArrayList<>(CollectionUtils.intersection(supplierCodeFragmentList, supplierCodeFragmentSet));
    }

}