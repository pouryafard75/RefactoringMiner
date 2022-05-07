package tree;


import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;

public class TreeIoUtils {

    public static TreeSerializer toShortText(Tree root) {
        return new TreeSerializer(null, root) {
            @Override
            protected TreeFormatter newFormatter(TreeContext ctx, MetadataSerializers serializer, Writer writer) {
                return new ShortTextFormatter(writer, ctx);
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
        public TreeSerializer(TreeContext ctx, Tree root) {
            context = ctx;
            this.root = root;
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


        protected void writeAttributes(TreeFormatter formatter, Iterator<Map.Entry<String, Object>> it) throws Exception {
            while (it.hasNext()) {
                Map.Entry<String, Object> entry = it.next();
            }
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

}
