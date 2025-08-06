package CommonClass.MapClass;

import CommonClass.SharedClass.Node;

import java.io.Serializable;
import java.util.List;

public record Nodes(List<Node> nodes)implements Serializable {}
