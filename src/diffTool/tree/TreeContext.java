

package diffTool.tree;

import diffTool.io.TreeIoUtils;
import gr.uom.java.xmi.UMLComment;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * The tree context class contains an AST together with its context (key - value metadata).
 *
 * @see Tree
 */
public class TreeContext {
    private String filename;
    private List<UMLComment> umlCommentList;
    private final Map<String, Object> metadata = new HashMap<>();

    private final MetadataSerializers serializers = new MetadataSerializers();

    private Tree root;

    @Override
    public String toString() {
        return TreeIoUtils.toText(this).toString();
    }

    /**
     * Set the AST of the TreeContext.
     */
    public void setRoot(Tree root) {
        this.root = root;
    }

    /**
     * Return the AST of the TreeContext.
     */
    public Tree getRoot() {
        return root;
    }

    /**
     * Utility method to create a default tree with a given type and label.
     * @see DefaultTree#DefaultTree(Type, String)
     */
    public Tree createTree(Type type, String label) {
        return new DefaultTree(type, label);
    }

    /**
     * Utility method to create a default tree with a given type.
     * @see DefaultTree#DefaultTree(Type)
     */
    public Tree createTree(Type type) {
        return new DefaultTree(type);
    }

    /**
     * Utility method to create a fake tree with the given children.
     * @see FakeTree#FakeTree(Tree...)
     */
    public Tree createFakeTree(Tree... trees) {
        return new FakeTree(trees);
    }

    /**
     * Get the AST metadata with the given key.
     * There is no way to know if the metadata is really null or does not exists.
     *
     * @return the metadata or null if not found
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    /**
     * Store an AST metadata with the given key and value.
     *
     * @return the previous value of metadata if existing or null
     */
    public Object setMetadata(String key, Object value) {
        return metadata.put(key, value);
    }

    public void setUmlCommentList(List<UMLComment> umlCommentList) {
        this.umlCommentList = umlCommentList;
    }

    public List<UMLComment> getUmlCommentList() {
        return umlCommentList;
    }


    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }

    /**
     * Get an iterator on the AST metadata.
     */


    public Iterator<Entry<String, Object>> getMetadata() {
        return metadata.entrySet().iterator();
    }

    /**
     * Get the metadata serializers for this tree context.
     */
    public MetadataSerializers getSerializers() {
        return serializers;
    }

    public TreeContext export(MetadataSerializers s) {
        serializers.addAll(s);
        return this;
    }

    public TreeContext export(String key, TreeIoUtils.MetadataSerializer s) {
        serializers.add(key, s);
        return this;
    }

    public TreeContext export(String... name) {
        for (String n : name)
            serializers.add(n, x -> x.toString());
        return this;
    }

    public static class Marshallers<E> {
        Map<String, E> serializers = new HashMap<>();

        public static final Pattern valid_id = Pattern.compile("[a-zA-Z0-9_]*");

        public void addAll(Marshallers<E> other) {
            addAll(other.serializers);
        }

        public void addAll(Map<String, E> serializers) {
            serializers.forEach((k, s) -> add(k, s));
        }

        public void add(String name, E serializer) {
            if (!valid_id.matcher(name).matches()) // TODO I definitely don't like this rule, we should think twice
                throw new RuntimeException("Invalid key for serialization");
            serializers.put(name, serializer);
        }

        public void remove(String key) {
            serializers.remove(key);
        }

        public Set<String> exports() {
            return serializers.keySet();
        }
    }

    public static class MetadataSerializers extends Marshallers<TreeIoUtils.MetadataSerializer> {
        public void serialize(TreeIoUtils.TreeFormatter formatter, String key, Object value) throws Exception {
            TreeIoUtils.MetadataSerializer s = serializers.get(key);
            if (s != null)
                formatter.serializeAttribute(key, s.toString(value));
        }
    }

    public static class MetadataUnserializers extends Marshallers<TreeIoUtils.MetadataUnserializer> {
        public void load(Tree tree, String key, String value) throws Exception {
            TreeIoUtils.MetadataUnserializer s = serializers.get(key);
            if (s != null) {
                if (key.equals("pos"))
                    tree.setPos(Integer.parseInt(value));
                else if (key.equals("length"))
                    tree.setLength(Integer.parseInt(value));
                else
                    tree.setMetadata(key, s.fromString(value));
            }
        }
    }
}
