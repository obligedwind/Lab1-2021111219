import org.jgrapht.graph.DefaultWeightedEdge;

// 自定义边类
public class WeightedEdge extends DefaultWeightedEdge {
    @Override
    public String toString() {
        // 返回边的权重值的字符串表示
        return String.valueOf(getWeight());
    }
}
