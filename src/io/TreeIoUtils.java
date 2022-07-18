

package io;
import matchers.Mapping;
import matchers.MappingStore;
import tree.*;
import tree.TreeContext.MetadataSerializers;
import tree.TreeContext.MetadataUnserializers;

import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import gen.TreeGenerator;



/**
 * Class providing static utility IO methods.
 * This class is not designed to be instantiated.
 */
public final class TreeIoUtils {
    private TreeIoUtils() {
    }

    public static TreeSerializer toXml(TreeContext ctx) {
        return toXml(ctx, ctx.getRoot());
    }

    public static TreeSerializer toXml(TreeContext ctx, Tree root) {
        return new TreeSerializer(ctx, root) {
            @Override
            protected TreeFormatter newFormatter(TreeContext ctx, MetadataSerializers serializers, Writer writer)
                    throws XMLStreamException {
                return new XmlFormatter(writer, ctx);
            }
        };
    }

    public static TreeSerializer toAnnotatedXml(TreeContext ctx, boolean isSrc, MappingStore m) {
        return toAnnotatedXml(ctx, ctx.getRoot(), isSrc, m);
    }

    public static TreeSerializer toAnnotatedXml(TreeContext ctx, Tree root, boolean isSrc, MappingStore m) {
        return new TreeSerializer(ctx, root) {
            @Override
            protected TreeFormatter newFormatter(TreeContext ctx, MetadataSerializers serializers, Writer writer)
                    throws XMLStreamException {
                return new XmlAnnotatedFormatter(writer, ctx, isSrc, m);
            }
        };
    }

    public static TreeSerializer toCompactXml(TreeContext ctx) {
        return toCompactXml(ctx, ctx.getRoot());
    }

    public static TreeSerializer toCompactXml(TreeContext ctx, Tree root) {
        return new TreeSerializer(ctx, root) {
            @Override
            protected TreeFormatter newFormatter(TreeContext ctx, MetadataSerializers serializers, Writer writer)
                    throws Exception {
                return new XmlCompactFormatter(writer, ctx);
            }
        };
    }



    public static TreeSerializer toLisp(TreeContext ctx) {
        return toLisp(ctx, ctx.getRoot());
    }

    public static TreeSerializer toLisp(TreeContext ctx, Tree tree) {
        return new TreeSerializer(ctx, tree) {
            @Override
            protected TreeFormatter newFormatter(TreeContext ctx, MetadataSerializers serializers, Writer writer) {
                return new LispFormatter(writer, ctx);
            }
        };
    }

    public static TreeSerializer toDot(TreeContext ctx) {
        return toDot(ctx, ctx.getRoot());
    }

    public static TreeSerializer toDot(TreeContext ctx, Tree root) {
        return new TreeSerializer(ctx, root) {
            @Override
            protected TreeFormatter newFormatter(TreeContext ctx, MetadataSerializers serializer, Writer writer) {
                return new DotFormatter(writer, ctx);
            }
        };
    }

    public static TreeSerializer toText(TreeContext ctx) {
        return toText(ctx, ctx.getRoot());
    }

    public static TreeSerializer toText(TreeContext ctx, Tree root) {
        return new TreeSerializer(ctx, root) {
            @Override
            protected TreeFormatter newFormatter(TreeContext ctx, MetadataSerializers serializer, Writer writer) {
                return new TextFormatter(writer, ctx);
            }
        };
    }

    public static TreeSerializer toShortText(Tree root) {
        return new TreeSerializer(null, root) {
            @Override
            protected TreeFormatter newFormatter(TreeContext ctx, MetadataSerializers serializer, Writer writer) {
                return new ShortTextFormatter(writer, ctx);
            }
        };
    }

    public abstract static class AbstractSerializer {

        public abstract void writeTo(Writer writer) throws Exception;

        public void writeTo(OutputStream writer) throws Exception {
            // FIXME Since the stream is already open, we should not close it, however due to semantic issue
            // it should stay like this
            try (OutputStreamWriter os = new OutputStreamWriter(writer, StandardCharsets.UTF_8)) {
                writeTo(os);
            }
        }

        @Override
        public String toString() {
            try (StringWriter s = new StringWriter()) {
                writeTo(s);
                return s.toString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public void writeTo(String file) throws Exception {
            try (Writer w = Files.newBufferedWriter(Paths.get(file), Charset.forName("UTF-8"))) {
                writeTo(w);
            }
        }

        public void writeTo(File file) throws Exception {
            try (Writer w = Files.newBufferedWriter(file.toPath(), Charset.forName("UTF-8"))) {
                writeTo(w);
            }
        }
    }

    public abstract static class TreeSerializer extends AbstractSerializer {
        private final TreeContext context;
        private final Tree root;
        final MetadataSerializers serializers = new MetadataSerializers();

        public TreeSerializer(TreeContext ctx, Tree root) {
            context = ctx;
            this.root = root;
            if (ctx != null)
                serializers.addAll(ctx.getSerializers());
        }

        protected abstract TreeFormatter newFormatter(TreeContext ctx, MetadataSerializers serializers, Writer writer)
                throws Exception;

        public void writeTo(Writer writer) throws Exception {
            TreeFormatter formatter = newFormatter(context, serializers, writer);
            try {
                writeTree(formatter, root);
            } finally {
                formatter.close();
            }
        }

        private void forwardException(Exception e) {
            throw new FormatException(e);
        }

        protected void writeTree(TreeFormatter formatter, Tree root) throws Exception {
            formatter.startSerialization();
            if (context != null)
                writeAttributes(formatter, context.getMetadata());
            formatter.endProlog();
            try {
                TreeVisitor.visitTree(root, new TreeVisitor() {

                    @Override
                    public void startTree(Tree tree) {
                        try {
                            assert tree != null;
                            formatter.startTree(tree);
                            writeAttributes(formatter, tree.getMetadata());
                            formatter.endTreeProlog(tree);
                        } catch (Exception e) {
                            forwardException(e);
                        }
                    }

                    @Override
                    public void endTree(Tree tree) {
                        try {
                            formatter.endTree(tree);
                        } catch (Exception e) {
                            forwardException(e);
                        }
                    }
                });
            } catch (FormatException e) {
                throw e.getCause();
            }
            formatter.stopSerialization();
        }

        protected void writeAttributes(TreeFormatter formatter, Iterator<Entry<String, Object>> it) throws Exception {
            while (it.hasNext()) {
                Entry<String, Object> entry = it.next();
                serializers.serialize(formatter, entry.getKey(), entry.getValue());
            }
        }

        public TreeSerializer export(String name, MetadataSerializer serializer) {
            serializers.add(name, serializer);
            return this;
        }

        public TreeSerializer export(String... name) {
            for (String n : name)
                serializers.add(n, Object::toString);
            return this;
        }
    }

    public interface TreeFormatter {
        void startSerialization() throws Exception;

        void endProlog() throws Exception;

        void stopSerialization() throws Exception;

        void startTree(Tree tree) throws Exception;

        void endTreeProlog(Tree tree) throws Exception;

        void endTree(Tree tree) throws Exception;

        void close() throws Exception;

        void serializeAttribute(String name, String value) throws Exception;
    }

    @FunctionalInterface
    public interface MetadataSerializer {
        String toString(Object object);
    }

    @FunctionalInterface
    public interface MetadataUnserializer {
        Object fromString(String value);
    }

    static class FormatException extends RuntimeException {
        private static final long serialVersionUID = 593766540545763066L;
        Exception cause;

        public FormatException(Exception cause) {
            super(cause);
            this.cause = cause;
        }

        @Override
        public synchronized Exception getCause() {
            return cause;
        }
    }

    static class TreeFormatterAdapter implements TreeFormatter {
        protected final TreeContext context;

        protected TreeFormatterAdapter(TreeContext ctx) {
            context = ctx;
        }

        @Override
        public void startSerialization() throws Exception {
        }

        @Override
        public void endProlog() throws Exception {
        }

        @Override
        public void startTree(Tree tree) throws Exception {
        }

        @Override
        public void endTreeProlog(Tree tree) throws Exception {
        }

        @Override
        public void endTree(Tree tree) throws Exception {
        }

        @Override
        public void stopSerialization() throws Exception {
        }

        @Override
        public void close() throws Exception {
        }

        @Override
        public void serializeAttribute(String name, String value) throws Exception {
        }
    }

    abstract static class AbsXmlFormatter extends TreeFormatterAdapter {
        protected final XMLStreamWriter writer;

        protected AbsXmlFormatter(Writer w, TreeContext ctx) throws XMLStreamException {
            super(ctx);
            XMLOutputFactory f = XMLOutputFactory.newInstance();
            writer = new IndentingXMLStreamWriter(f.createXMLStreamWriter(w));
        }

        @Override
        public void startSerialization() throws XMLStreamException {
            writer.writeStartDocument();
        }

        @Override
        public void stopSerialization() throws XMLStreamException {
            writer.writeEndDocument();
        }

        @Override
        public void close() throws XMLStreamException {
            writer.close();
        }
    }

    static class XmlFormatter extends AbsXmlFormatter {
        public XmlFormatter(Writer w, TreeContext ctx) throws XMLStreamException {
            super(w, ctx);
        }

        @Override
        public void startSerialization() throws XMLStreamException {
            super.startSerialization();
            writer.writeStartElement("root");
            writer.writeStartElement("context");
        }

        @Override
        public void endProlog() throws XMLStreamException {
            writer.writeEndElement();
        }

        @Override
        public void stopSerialization() throws XMLStreamException {
            writer.writeEndElement();
            super.stopSerialization();
        }

        @Override
        public void serializeAttribute(String name, String value) throws XMLStreamException {
            writer.writeStartElement(name);
            writer.writeCharacters(value);
            writer.writeEndElement();
        }

        @Override
        public void startTree(Tree tree) throws XMLStreamException {
            writer.writeStartElement("tree");
            writer.writeAttribute("type", tree.getType().toString());
            if (tree.hasLabel()) writer.writeAttribute("label", tree.getLabel());
            if (Tree.NO_POS != tree.getPos()) {
                writer.writeAttribute("pos", Integer.toString(tree.getPos()));
                writer.writeAttribute("length", Integer.toString(tree.getLength()));
            }
        }

        @Override
        public void endTree(Tree tree) throws XMLStreamException {
            writer.writeEndElement();
        }
    }

    static class XmlAnnotatedFormatter extends XmlFormatter {
        final SearchOther searchOther;

        public XmlAnnotatedFormatter(Writer w, TreeContext ctx, boolean isSrc,
                                     MappingStore m) throws XMLStreamException {
            super(w, ctx);

            if (isSrc)
                searchOther = (tree) -> m.isSrcMapped(tree) ? m.getDstForSrc(tree) : null;
            else
                searchOther = (tree) -> m.isDstMapped(tree) ? m.getSrcForDst(tree) : null;
        }

        interface SearchOther {
            Tree lookup(Tree tree);
        }

        @Override
        public void startTree(Tree tree) throws XMLStreamException {
            super.startTree(tree);
            Tree o = searchOther.lookup(tree);

            if (o != null) {
                if (Tree.NO_POS != o.getPos()) {
                    writer.writeAttribute("other_pos", Integer.toString(o.getPos()));
                    writer.writeAttribute("other_length", Integer.toString(o.getLength()));
                }
            }
        }
    }

    static class XmlCompactFormatter extends AbsXmlFormatter {
        public XmlCompactFormatter(Writer w, TreeContext ctx) throws XMLStreamException {
            super(w, ctx);
        }

        @Override
        public void startSerialization() throws XMLStreamException {
            super.startSerialization();
            writer.writeStartElement("root");
        }

        @Override
        public void stopSerialization() throws XMLStreamException {
            writer.writeEndElement();
            super.stopSerialization();
        }

        @Override
        public void serializeAttribute(String name, String value) throws XMLStreamException {
            writer.writeAttribute(name, value);
        }

        @Override
        public void startTree(Tree tree) throws XMLStreamException {
            if (tree.getChildren().size() == 0)
                writer.writeEmptyElement(tree.getType().toString());
            else
                writer.writeStartElement(tree.getType().toString());
            if (tree.hasLabel())
                writer.writeAttribute("label", tree.getLabel());
        }

        @Override
        public void endTree(Tree tree) throws XMLStreamException {
            if (tree.getChildren().size() > 0)
                writer.writeEndElement();
        }
    }

    static class LispFormatter extends TreeFormatterAdapter {
        protected final Writer writer;
        protected final Pattern protectChars = Pattern.compile("[ ,\"]");
        protected final Pattern escapeChars = Pattern.compile("[\\\\\"]");
        int level = 0;

        protected LispFormatter(Writer w, TreeContext ctx) {
            super(ctx);
            writer = w;
        }

        @Override
        public void startSerialization() throws IOException {
            writer.write("((");
        }

        @Override
        public void startTree(Tree tree) throws IOException {
            if (!tree.isRoot())
                writer.write("\n");
            for (int i = 0; i < level; i++)
                writer.write("    ");
            level++;

            String pos = (Tree.NO_POS == tree.getPos() ? "" : String.format("(%d %d)",
                    tree.getPos(), tree.getLength()));

            writer.write(String.format("(%s %s (%s",
                    protect(tree.getType().toString()), protect(tree.getLabel()), pos));
        }

        @Override
        public void endProlog() throws Exception {
            writer.append(") ");
        }

        @Override
        public void endTreeProlog(Tree tree) throws Exception {
            writer.append(") (");
        }

        @Override
        public void serializeAttribute(String name, String value) throws Exception {
            writer.append(String.format("(:%s %s) ", name, protect(value)));
        }

        protected String protect(String val) {
            String text = escapeChars.matcher(val).replaceAll("\\\\$0");
            if (protectChars.matcher(text).find() || val.isEmpty())
                return String.format("\"%s\"", text);
            else
                return text;
        }

        @Override
        public void endTree(Tree tree) throws IOException {
            writer.write(")");
            level--;
        }

        @Override
        public void stopSerialization() throws IOException {
            writer.write(")");
        }
    }

    static class DotFormatter extends TreeFormatterAdapter {
        protected final Writer writer;

        private static AtomicLong idCounter = new AtomicLong();

        private Map<Tree, String> idForTrees = new HashMap<>();

        protected DotFormatter(Writer w, TreeContext ctx) {
            super(ctx);
            writer = w;
        }

        @Override
        public void startSerialization() throws Exception {
            writer.write("digraph G {\n");
        }

        @Override
        public void startTree(Tree tree) throws Exception {
            String label = getCleanLabel(tree);
            writer.write(String.format("\t%s [label=\"%s\"];\n", id(tree), label));
            if (tree.getParent() != null)
                writer.write(String.format("\t%s -> %s;\n", id(tree.getParent()), id(tree)));
        }

        @Override
        public void stopSerialization() throws Exception {
            writer.write("}");
        }

        private String getCleanLabel(Tree tree) {
            String label = tree.toString();
            if (label.contains("\"") || label.contains("\\s"))
                label = label
                        .replaceAll("\"", "")
                        .replaceAll("\\s", "")
                        .replaceAll("\\\\", "");
            if (label.length() > 30)
                label = label.substring(0, 30);
            return label;
        }

        private String id(Tree t) {
            if (idForTrees.containsKey(t))
                return idForTrees.get(t);
            else {
                String id = generateId();
                idForTrees.put(t, id);
                return id;
            }
        }

        private static String generateId() {
            return "id_" + idCounter.getAndIncrement();
        }
    }


    public abstract static class AbstractTextFormatter extends TreeFormatterAdapter {
        protected final Writer writer;
        int level = 0;

        public AbstractTextFormatter(Writer w, TreeContext ctx) {
            super(ctx);
            writer = w;
        }

        protected void indent(int level, String prefix) throws IOException {
            for (int i = 0; i < level; i++)
                writer.write(prefix);
        }

        @Override
        public void startTree(Tree tree) throws IOException {
            if (level != 0) writer.write("\n");
            indent(level, "    ");
            level++;

            writeTree(tree);
        }

        protected abstract void writeTree(Tree tree) throws IOException;

        @Override
        public void endTree(Tree tree) throws IOException {
            level--;
        }
    }

    public static class TextFormatter extends AbstractTextFormatter {

        public TextFormatter(Writer w, TreeContext ctx) {
            super(w, ctx);
        }

        @Override
        public void writeTree(Tree tree) throws IOException {
            writer.write(tree.toString());
        }
    }

    public static class ShortTextFormatter extends AbstractTextFormatter {

        public ShortTextFormatter(Writer w, TreeContext ctx) {
            super(w, ctx);
        }

        @Override
        public void writeTree(Tree tree) throws IOException {
            writer.write(tree.toString());
        }
    }
}
