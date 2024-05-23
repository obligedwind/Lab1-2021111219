import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.layout.mxCircleLayout;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import com.mxgraph.view.mxGraph;
import org.jgrapht.ext.*;
import javax.swing.*;
import java.io.*;
import java.util.*;
import java.util.Random;
// 这里注释的修改用于测试git 分支修改影响
public class TextGraph {
    private HashMap<String, HashMap<String, Integer>>graph = new HashMap<>();
    private Random random = new Random();
    private volatile boolean stopRequested = false;
    public void createGraph(String filePath) throws IOException {
        File file = new File(filePath);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        String lastWord = null;

        while ((line = br.readLine()) != null) {
            String[] words = line.replaceAll("[^a-zA-Z ]", "").toLowerCase().split("\\s+");
            for (String word : words) {
                if (!word.isEmpty()) {
                    if (lastWord != null) {
                        addEdge(lastWord, word);
                    }
                    lastWord = word;
                }
            }
        }
        br.close();
    }

    private void addEdge(String word1, String word2) {
        graph.putIfAbsent(word1, new HashMap<>());
        HashMap<String, Integer> edges = graph.get(word1);
        edges.put(word2, edges.getOrDefault(word2, 0) + 1);
    }


    public void showDirectedGraph() {
        // 创建一个有向加权图
        DefaultDirectedWeightedGraph<String, WeightedEdge> directedGraph =
                new DefaultDirectedWeightedGraph<>(WeightedEdge.class);
        // 将哈希表中的数据添加到图中
        // 添加顶点和边，并设置权重
        for (String word : graph.keySet()) {
            directedGraph.addVertex(word);
            HashMap<String, Integer> edges = graph.get(word);
            for (String adjacentWord : edges.keySet()) {
                directedGraph.addVertex(adjacentWord);
                WeightedEdge edge = directedGraph.addEdge(word, adjacentWord);
                if (edge != null) { // 确保边确实被创建了
                    directedGraph.setEdgeWeight(edge, edges.get(adjacentWord));
                }
            }
        }
        // 将JGraphT图转换为JGraphX图
        JGraphXAdapter<String, WeightedEdge> graphAdapter = new JGraphXAdapter<>(directedGraph);

        // 创建一个新的JFrame来作为图形的容器
        JFrame frame = new JFrame("Directed Weighted Graph Visualization");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);

        // 创建mxGraphComponent用于显示图形
        mxGraphComponent graphComponent = new mxGraphComponent(graphAdapter);

        frame.add(graphComponent);

        // 调整布局
        mxGraph mxGraph = graphComponent.getGraph();
        new mxCircleLayout(mxGraph).execute(mxGraph.getDefaultParent());

        // 显示窗口
        frame.setVisible(true);
    }
    public String queryBridgeWords(String word1, String word2) {
        if (!graph.containsKey(word1) || !graph.containsKey(word2)) {
            return "No " + word1 + " or " + word2 + " in the graph!";
        }

        List<String> bridgeWords = new ArrayList<>();
        HashMap<String, Integer> edges = graph.get(word1);
        for (String word3 : edges.keySet()) {
            if (graph.containsKey(word3) && graph.get(word3).containsKey(word2)) {
                bridgeWords.add(word3);
            }
        }

        if (bridgeWords.isEmpty()) {
            return "No bridge words from " + word1 + " to " + word2 + "!";
        } else {
            return "The bridge words from " + word1 + " to " + word2 + " are: " + String.join(", ", bridgeWords) ;
        }
    }

    public String generateNewText(String inputText) {
        String[] words = inputText.split("\\s+");
        StringBuilder newText = new StringBuilder();

        for (int i = 0; i < words.length - 1; i++) {
            newText.append(words[i]).append(" ");
            String bridgeWords = queryBridgeWords(words[i], words[i + 1]);
            if (bridgeWords.startsWith("The bridge words from")) {
                String[] bridges = bridgeWords.substring(bridgeWords.indexOf(':') + 2).split(", ");
                // 随机选择一个桥接词插入
                String selectedBridgeWord = bridges[random.nextInt(bridges.length)];
                newText.append(selectedBridgeWord).append(" ");
            }
        }
        newText.append(words[words.length - 1]);
        return newText.toString();
    }

    public String calcShortestPath(String word1, String word2) {
        if (!graph.containsKey(word1) || !graph.containsKey(word2)) {
            return "One or both words are not present in the graph.";
        }

        // 使用优先级队列实现 Dijkstra 算法
        PriorityQueue<Pair> pq = new PriorityQueue<>(Comparator.comparingInt(pair -> pair.distance));
        // 使用表来存储两词最短路径
        Map<String, Integer> distances = new HashMap<>();
        // 使用表来存每个节点的上个节点
        Map<String, String> predecessors = new HashMap<>();

        // 初始化距离
        for (String node : graph.keySet()) {
            distances.put(node, Integer.MAX_VALUE);
        }
        distances.put(word1, 0);
        pq.add(new Pair(word1, 0));

        // Dijkstra's algorithm
        while (!pq.isEmpty()) {
            Pair currentPair = pq.poll();
            String currentNode = currentPair.node;
            int currentDistance = currentPair.distance;

            if (currentDistance > distances.get(currentNode)) continue;

            // 访问邻居节点
            for (Map.Entry<String, Integer> entry : graph.get(currentNode).entrySet()) {
                String neighbor = entry.getKey();
                int weight = entry.getValue();
                int distanceThroughCurrent = currentDistance + weight;

                if (distanceThroughCurrent < distances.get(neighbor)) {
                    distances.put(neighbor, distanceThroughCurrent);
                    predecessors.put(neighbor, currentNode);
                    pq.add(new Pair(neighbor, distanceThroughCurrent));
                }
            }
        }

        if (distances.get(word2) == Integer.MAX_VALUE) {
            return "The words are not connected.";
        }

        // 建立最短路径
        List<String> path = new ArrayList<>();
        for (String at = word2; at != null; at = predecessors.get(at)) {
            path.add(at);
        }
        Collections.reverse(path);

        // 生成结果
        StringBuilder output = new StringBuilder();
        output.append("Shortest path: ").append(String.join(" -> ", path)).append("\n");
        output.append("Path length: ").append(distances.get(word2)).append("\n");

        return output.toString();
    }
    public String randomWalk() throws IOException {
        if (graph.isEmpty()) {
            return "The graph is empty.";
        }

        // 启动后台进程监听用户是否手动暂停
        Thread inputThread = new Thread(this::listenForStopCommand);
        inputThread.start();

        List<String> nodes = new ArrayList<>(graph.keySet());
        String currentNode = nodes.get(random.nextInt(nodes.size()));
        Set<String> visitedEdges = new HashSet<>();
        List<String> path = new ArrayList<>();

        path.add(currentNode);

        while (true) {
            if (stopRequested) {
                System.out.println("Traversal stopped by user.");
                break;
            }

            HashMap<String, Integer> edges = graph.get(currentNode);
            if (edges == null || edges.isEmpty()) {
                break;
            }

            List<String> neighbors = new ArrayList<>(edges.keySet());
            String nextNode = neighbors.get(random.nextInt(neighbors.size()));
            String edge = currentNode + "->" + nextNode;

            if (visitedEdges.contains(edge)) {
                break;
            }

            visitedEdges.add(edge);
            path.add(nextNode);
            currentNode = nextNode;
        }

        // 停止输入线程
        inputThread.interrupt();

        // 将结果写入文件
        String filePath = "E:\\project\\software_project_lab\\out\\random_walk_output.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (String node : path) {
                writer.write(node);
                writer.newLine();
            }
        }

        return "Random walk complete. Path: " + String.join(" -> ", path) + ". Output written to " + filePath;
    }

    private void listenForStopCommand() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Type 'stop' to stop the traversal:");
        try {
            while (!Thread.currentThread().isInterrupted()) {
                if (scanner.nextLine().trim().equalsIgnoreCase("stop")) {
                    stopRequested = true;
                    break;
                }
            }
        } catch (NoSuchElementException e) {
            // 只是防止这段用户没输入stop而导致无输入报错
        }
    }
    private static class Pair {
        String node;
        int distance;

        public Pair(String node, int distance) {
            this.node = node;
            this.distance = distance;
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("请输入文本文件的路径和文件名：");
        String filePath = scanner.nextLine();
        System.out.println("请输入一个单词：");
        String word1 = scanner.nextLine();
        System.out.println("请输入下一个单词：");
        String word2 = scanner.nextLine();
        // System.out.println("生成文本输入");
        // String input = scanner.nextLine();
        scanner.close();

        TextGraph textGraph = new TextGraph();
        try {
            textGraph.createGraph(filePath);
            //textGraph.showDirectedGraph();
            //System.out.println(textGraph.queryBridgeWords(word1,word2));
            // System.out.println(textGraph.generateNewText(input));
            String result = textGraph.calcShortestPath(word1, word2);
            System.out.println(result);
            String randomWalkResult = textGraph.randomWalk();
            System.out.println(randomWalkResult);
        } catch (IOException e) {
            System.out.println("读取文件时出错: " + e.getMessage());
        }
    }
}
