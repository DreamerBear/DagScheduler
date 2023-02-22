package com.wts.dag.scheduler;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Console;

import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author xuchao
 */
public class ACTrie {

    private final Node root;                                  //根结点
    private Boolean failureStatesConstructed = false;   //是否建立了failure表


    public ACTrie() {
        this.root = new Node(true);
    }

    public static String test(String text) {
        StopWatch stopWatch = StopWatch.create("ACTrie耗时分析");
        stopWatch.start("ACTrie构建");
        ACTrie trie = new ACTrie();
        String queryNames = ResourceUtil.readStr("keyword.txt", Charset.defaultCharset());
        String[] split = queryNames.split("\n");
        for (int i = 0; i < split.length; i++) {
            trie.addKeyword(split[i].toLowerCase());
        }
        stopWatch.stop();

        stopWatch.start("ACTrie多模式查找");
        //匹配text，并返回匹配到的pattern
        Collection<Patten_String> PattenStrings = trie.parseText(text.toLowerCase());
        stopWatch.stop();
        Console.log(stopWatch.prettyPrint(TimeUnit.MILLISECONDS));

        return PattenStrings.stream().map(Patten_String::getKeyword).collect(Collectors.joining("\t"));
    }

    public static void main(String[] args) {
        String text = "机动车制动液DOT4/HZY4,GB12981-2012《机动车辆制动液》标准，符合基至超过美国运输部,DOT4标准,SAEJ1704标准和ISO4925.第4级,FMVSS$571.116,标准。,可以与相同规格的制动液混合使用,湿平衡回流沸点：≥160℃,全提供了值得信赖的技术保障,注意事项,水或其他物质的污染会引起制动液失效或增加车辆维修,动液清洁。,只许在原来的容器内存储制动液。保持容器的清洁和密封。不准在,费用,请严格遵守废弃物处理规定。请在指定的场所排放或者回收。,生产日期：,见包装瓶上喷码,保质期：五年,委,生产地址,（苏）XK18-001-00001,中国制造,BOSCH,Robert BoschGmbH,76227Karlsruhe,Germany,www.bosch-automotive.com,气车技术服务（中国）有限公司,东区润博路1号,www.bosch.com.cn,BF8999,yh2954";

        for (int i = 0; i < 1; i++) {
            System.out.println(SupplierCodeFragmentRecognizer.test(text));
            System.out.println(ACTrie.test(text));
        }
    }

    /**
     * 添加一个模式串(内部使用字典树构建)
     */
    public void addKeyword(String keyword) {
        if (keyword == null || keyword.length() == 0) {
            return;
        }
        Node currentState = this.root;
        for (Character character : keyword.toCharArray()) {
            //如果char已经在子节点里，返回这个节点的node；否则建一个node，并将map(char,node)加到子节点里去
            currentState = currentState.insert(character);
        }
        //在每一个尾节点处，将从root到尾节点的整个string添加到这个叶节点的PattenString里
        currentState.addPattenString(keyword);
    }


    /**
     * 用ac自动机做匹配，返回text里包含的pattern及其在text里的起始位置
     */
    public Collection<Patten_String> parseText(String text) {
        //首先构建 fail表，如已构建则跳过
        checkForConstructedFailureStates();

        Node currentState = this.root;
        List<Patten_String> collectedPattenStrings = new ArrayList<>();
        for (int position = 0; position < text.length(); position++) {
            Character character = text.charAt(position);
            //依次从子节点里找char，如果子节点没找到，就到子节点的fail node找，并返回最后找到的node；如果找不到就会返回root
            //这一步同时也在更新currentState，如果找到了就更新currentState为找到的node，没找到currentState就更新为root，相当于又从头开始找
            currentState = currentState.nextState(character);
            Collection<String> PattenStrings = currentState.PattenString();
            if (PattenStrings == null || PattenStrings.isEmpty()) {
                continue;
            }
            //如果找到的node的PattenString非空，说明有pattern被匹配到了
            for (String PattenString : PattenStrings) {
                collectedPattenStrings.add(new Patten_String(position - PattenString.length() + 1, position, PattenString));
            }
        }
        return collectedPattenStrings;//返回匹配到的所有pattern
    }


    /**
     * 建立Fail表(核心,BFS遍历)
     */
    private void constructFailureStates() {
        Queue<Node> queue = new LinkedList<>();

        //首先从把root的子节点的fail node全设为root
        //然后将root的所有子节点加到queue里面
        for (Node depthOneState : this.root.children()) {
            depthOneState.setFailure(this.root);
            queue.add(depthOneState);
        }
        this.failureStatesConstructed = true;

        while (!queue.isEmpty()) {
            Node parentNode = queue.poll();
            //下面给parentNode的所有子节点找fail node
            for (Character transition : parentNode.getTransitions()) {           //transition是父节点的子节点的char
                Node childNode = parentNode.find(transition);                    //childNode是子节点中对应上面char值的节点的Node值
                queue.add(childNode);                                            //将这个parentNode的所有子节点加入queue，在parentNode的所有兄弟节点都过了一遍之后，就会过这些再下一层的节点
                Node failNode = parentNode.getFailure().nextState(transition);   //利用父节点的fail node来构建子节点的fail node
                childNode.setFailure(failNode);

                //每个节点处的PattenString要加上它的fail node处的PattenString
                //因为能匹配到这个位置的话，那么fail node处的PattenString一定是匹配的pattern
                childNode.addPattenString(failNode.PattenString());
            }
        }
    }


    /**
     * 检查是否建立了Fail表(若没建立，则建立)
     */
    private void checkForConstructedFailureStates() {
        if (!this.failureStatesConstructed) {
            constructFailureStates();
        }
    }

    private static class Node {
        private final Map<Character, Node> map;   //用于放这个Node的所有子节点，储存形式是：Map(char, Node)
        private final List<String> PattenStrings; //该节点处包含的所有pattern string
        private Node failure;               //fail指针指向的node
        private Boolean isRoot = false;     //是否为根结点


        public Node() {
            map = new HashMap<>();
            PattenStrings = new ArrayList<>();
        }


        public Node(Boolean isRoot) {
            this();
            this.isRoot = isRoot;
        }


        //用于build trie，如果一个字符character存在于子节点中，不做任何操作，返回这个节点的node
        //否则，建一个node，并将map(char,node)添加到当前节点的子节点里，并返回这个node
        public Node insert(Character character) {
            Node node = this.map.get(character);
            if (node == null) {
                node = new Node();
                map.put(character, node);
            }
            return node;
        }


        public void addPattenString(String keyword) {
            PattenStrings.add(keyword);
        }


        public void addPattenString(Collection<String> keywords) {
            PattenStrings.addAll(keywords);
        }


        public Node find(Character character) {
            return map.get(character);
        }


        /**
         * 利用父节点fail node来寻找子节点的fail node
         * or
         * parseText时找下一个匹配的node
         */
        private Node nextState(Character transition) {
            //用于构建fail node时，这里的this是父节点的fail node
            //首先从父节点的fail node的子节点里找有没有值和当前失败节点的char值相同的
            Node state = this.find(transition);

            //如果找到了这样的节点，那么该节点就是当前失败位置节点的fail node
            if (state != null) {
                return state;
            }

            //如果没有找到这样的节点，而父节点的fail node又是root，那么返回root作为当前失败位置节点的fail node
            if (this.isRoot) {
                return this;
            }

            //如果上述两种情况都不满足，那么就对父节点的fail node的fail node再重复上述过程，直到找到为止
            //这个地方借鉴了KMP算法里面求解next列表的思想
            return this.failure.nextState(transition);
        }


        public Collection<Node> children() {
            return this.map.values();
        }

        public Node getFailure() {
            return failure;
        }

        public void setFailure(Node node) {
            failure = node;
        }

        //返回一个Node的所有子节点的键值，也就是这个子节点上储存的char
        public Set<Character> getTransitions() {
            return map.keySet();
        }


        public Collection<String> PattenString() {
            return this.PattenStrings == null ? Collections.emptyList() : this.PattenStrings;
        }
    }

    private static class Patten_String {
        private final String keyword;   //匹配到的模式串
        private final int start;        //起点
        private final int end;          //终点

        public Patten_String(final int start, final int end, final String keyword) {
            this.start = start;
            this.end = end;
            this.keyword = keyword;
        }

        public String getKeyword() {
            return this.keyword;
        }

        @Override
        public String toString() {
            return super.toString() + "=" + this.keyword;
        }
    }
}

