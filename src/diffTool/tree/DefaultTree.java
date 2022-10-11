

package diffTool.tree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class DefaultTree extends AbstractTree implements Tree {
    private Type type;

    private String label;

    private int pos;
    private int length;

    private AssociationMap metadata;

    /**
     * Constructs a new node with an empty label.
     * @param type the type of the node
     * @see TreeContext#createTree(Type, String)
     */
    public DefaultTree(Type type) {
        this(type, NO_LABEL);
    }

    /**
     * Constructs a new node with a given label.
     * @param type the type of the node
     * @param label the label. If null, it will be replaced by an empty string.
     *              Note that the label will be interned.
     * @see TreeContext#createTree(Type, String)
     * @see String#intern()
     */
    public DefaultTree(Type type, String label) {
        this.type = type;
        this.label = (label == null) ? NO_LABEL : label.intern();
        this.children = new ArrayList<>();
    }

    /**
     * Construct a node using a given node as the model. It copies only
     * the local attributes of the given node, and not its parent and children.
     * @param other the model node, must be not null.
     */
    public DefaultTree(Tree other) {
        this.type = other.getType();
        this.label = other.getLabel();
        this.pos = other.getPos();
        this.length = other.getLength();
        this.children = new ArrayList<>();
    }

    @Override
    public Tree deepCopy() {
        Tree copy = new DefaultTree(this);
        for (Tree child : getChildren())
            copy.addChild(child.deepCopy());
        return copy;
    }

    @Override
    public Tree deepCopyWithMapPruning(Map<Tree,Tree> cpyMap) {
        if (this.type.name.equals("Block")) return null;
        Tree copy = new DefaultTree(this);
        cpyMap.put(copy,this);
        if (this.type.name.equals("AnonymousClassDeclaration")) return copy;
        for (Tree child : this.getChildren()) {
            Tree childCopy = child.deepCopyWithMapPruning(cpyMap);
            if (childCopy != null)
                copy.addChild(childCopy);
        }
        return copy;
    }

    @Override
    public Tree deepCustomCopy() {
        Tree copy = new DefaultTree(this);
        for (Tree child : getChildren()) {
            if (
                    !child.getType().name.equals("Block")
//                    && !child.getType().name.equals("LambdaExpression")
            )
                copy.addChild(child.deepCustomCopy());
        }
        return copy;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public int getPos() {
        return pos;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public void setLabel(String label) {
        this.label = (label == null) ? NO_LABEL : label.intern();
    }

    @Override
    public void setLength(int length) {
        this.length = length;
    }

    @Override
    public void setPos(int pos) {
        this.pos = pos;
    }

    @Override
    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public Object getMetadata(String key) {
        if (metadata == null)
            return null;
        return metadata.get(key);
    }

    @Override
    public Object setMetadata(String key, Object value) {
        if (value == null) {
            if (metadata == null)
                return null;
            else
                return metadata.remove(key);
        }
        if (metadata == null)
            metadata = new AssociationMap();
        return metadata.set(key, value);
    }

    @Override
    public Iterator<Entry<String, Object>> getMetadata() {
        if (metadata == null)
            return new EmptyEntryIterator();
        return metadata.iterator();
    }
}
