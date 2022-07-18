

package io;

import actions.EditScript;
import actions.model.*;
import io.TreeIoUtils.AbstractSerializer;
import matchers.Mapping;
import matchers.MappingStore;
import tree.Tree;
import tree.TreeContext;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.Writer;

public final class ActionsIoUtils {

    private ActionsIoUtils() {
    }

    public static ActionSerializer toText(TreeContext sctx, EditScript actions,
                                          MappingStore mappings) throws IOException {
        return new ActionSerializer(sctx, mappings, actions) {

            @Override
            protected ActionFormatter newFormatter(TreeContext ctx, Writer writer) throws Exception {
                return new TextFormatter(ctx, writer);
            }
        };
    }

    public static ActionSerializer toXml(TreeContext sctx, EditScript actions,
                                         MappingStore mappings) throws IOException {
        return new ActionSerializer(sctx, mappings, actions) {

            @Override
            protected ActionFormatter newFormatter(TreeContext ctx, Writer writer) throws Exception {
                return new XmlFormatter(ctx, writer);
            }
        };
    }

    public abstract static class ActionSerializer extends AbstractSerializer {
        final TreeContext context;
        final MappingStore mappings;
        final EditScript actions;

        ActionSerializer(TreeContext context, MappingStore mappings, EditScript actions) {
            this.context = context;
            this.mappings = mappings;
            this.actions = actions;
        }

        protected abstract ActionFormatter newFormatter(TreeContext ctx, Writer writer) throws Exception;

        @Override
        public void writeTo(Writer writer) throws Exception {
            ActionFormatter fmt = newFormatter(context, writer);
            // Start the output
            fmt.startOutput();

            // Write the matches
            fmt.startMatches();
            for (Mapping m: mappings) {
                fmt.match(m.first, m.second);
            }
            fmt.endMatches();

            // Write the actions
            fmt.startActions();
            for (Action a : actions) {
                Tree src = a.getNode();
                if (a instanceof Move) {
                    Tree dst = mappings.getDstForSrc(src);
                    fmt.moveAction((Move) a, src, dst.getParent(), ((Move) a).getPosition());
                } else if (a instanceof Update) {
                    Tree dst = mappings.getDstForSrc(src);
                    fmt.updateAction((Update) a, src, dst);
                } else if (a instanceof Insert) {
                    Tree dst = a.getNode();
                    if (dst.isRoot())
                        fmt.insertRoot((Insert) a, src);
                    else
                        fmt.insertAction((Insert) a, src, dst.getParent(), dst.getParent().getChildPosition(dst));
                } else if (a instanceof Delete) {
                    fmt.deleteAction((Delete) a, src);
                } else if (a instanceof TreeInsert) {
                    Tree dst = a.getNode();
                    fmt.insertTreeAction((TreeInsert) a, src, dst.getParent(), dst.getParent().getChildPosition(dst));
                } else if (a instanceof  TreeDelete) {
                    fmt.deleteTreeAction((TreeDelete) a, src);
                }

            }
            fmt.endActions();

            // Finish up
            fmt.endOutput();
        }
    }

    interface ActionFormatter {
        void startOutput() throws Exception;

        void endOutput() throws Exception;

        void startMatches() throws Exception;

        void match(Tree srcNode, Tree destNode) throws Exception;

        void endMatches() throws Exception;

        void startActions() throws Exception;

        void insertRoot(Insert action, Tree node) throws Exception;

        void insertAction(Insert action, Tree node, Tree parent, int index) throws Exception;

        void insertTreeAction(TreeInsert action, Tree node, Tree parent, int index) throws Exception;

        void moveAction(Move action, Tree src, Tree dst, int index) throws Exception;

        void updateAction(Update action, Tree src, Tree dst) throws Exception;

        void deleteAction(Delete action, Tree node) throws Exception;

        void deleteTreeAction(TreeDelete action, Tree node) throws Exception;

        void endActions() throws Exception;
    }

    static class XmlFormatter implements ActionFormatter {
        final TreeContext context;
        final XMLStreamWriter writer;

        XmlFormatter(TreeContext context, Writer w) throws XMLStreamException {
            XMLOutputFactory f = XMLOutputFactory.newInstance();
            writer = new IndentingXMLStreamWriter(f.createXMLStreamWriter(w));
            this.context = context;
        }

        @Override
        public void startOutput() throws XMLStreamException {
            writer.writeStartDocument();
        }

        @Override
        public void endOutput() throws XMLStreamException {
            writer.writeEndDocument();
        }

        @Override
        public void startMatches() throws XMLStreamException {
            writer.writeStartElement("matches");
        }

        @Override
        public void match(Tree srcNode, Tree destNode) throws XMLStreamException {
            writer.writeEmptyElement("match");
            writer.writeAttribute("src", srcNode.toString());
            writer.writeAttribute("dest", destNode.toString());
        }

        @Override
        public void endMatches() throws XMLStreamException {
            writer.writeEndElement();
        }

        @Override
        public void startActions() throws XMLStreamException {
            writer.writeStartElement("actions");
        }

        @Override
        public void insertRoot(Insert action, Tree node) throws Exception {
            start(action, node);
            end(node);
        }

        @Override
        public void insertAction(Insert action, Tree node, Tree parent, int index) throws Exception {
            start(action, node);
            writer.writeAttribute("parent", parent.toString());
            writer.writeAttribute("at", Integer.toString(index));
            end(node);
        }

        @Override
        public void insertTreeAction(TreeInsert action, Tree node, Tree parent, int index) throws Exception {
            start(action, node);
            writer.writeAttribute("parent", parent.toString());
            writer.writeAttribute("at", Integer.toString(index));
            end(node);
        }

        @Override
        public void moveAction(Move action, Tree src, Tree dst, int index) throws XMLStreamException {
            start(action, src);
            writer.writeAttribute("parent", dst.toString());
            writer.writeAttribute("at", Integer.toString(index));
            end(src);
        }

        @Override
        public void updateAction(Update action, Tree src, Tree dst) throws XMLStreamException {
            start(action, src);
            writer.writeAttribute("label", dst.getLabel());
            end(src);
        }

        @Override
        public void deleteAction(Delete action, Tree node) throws Exception {
            start(action, node);
            end(node);
        }

        @Override
        public void deleteTreeAction(TreeDelete action, Tree node) throws Exception {
            start(action, node);
            end(node);
        }

        @Override
        public void endActions() throws XMLStreamException {
            writer.writeEndElement();
        }

        private void start(Action action, Tree src) throws XMLStreamException {
            writer.writeEmptyElement(action.getName());
            writer.writeAttribute("tree", src.toString());
        }

        private void end(Tree node) throws XMLStreamException {
//            writer.writeEndElement();
        }
    }

    static class TextFormatter implements ActionFormatter {
        final Writer writer;
        final TreeContext context;

        public TextFormatter(TreeContext ctx, Writer writer) {
            this.context = ctx;
            this.writer = writer;
        }

        @Override
        public void startOutput() throws Exception {
        }

        @Override
        public void endOutput() throws Exception {
        }

        @Override
        public void startMatches() throws Exception {
        }

        @Override
        public void match(Tree srcNode, Tree destNode) throws Exception {
            write(String.format("===\nmatch\n---\n%s\n%s", toS(srcNode), toS(destNode)));
        }

        @Override
        public void endMatches() throws Exception {
        }

        @Override
        public void startActions() throws Exception {
        }

        @Override
        public void insertRoot(Insert action, Tree node) throws Exception {
            write(action.toString());
        }

        @Override
        public void insertAction(Insert action, Tree node, Tree parent, int index) throws Exception {
            write(action.toString());
        }

        @Override
        public void insertTreeAction(TreeInsert action, Tree node, Tree parent, int index) throws Exception {
            write(action.toString());
        }

        @Override
        public void moveAction(Move action, Tree src, Tree dst, int position) throws Exception {
            write(action.toString());
        }

        @Override
        public void updateAction(Update action, Tree src, Tree dst) throws Exception {
            write(action.toString());
        }

        @Override
        public void deleteAction(Delete action, Tree node) throws Exception {
            write(action.toString());
        }

        @Override
        public void deleteTreeAction(TreeDelete action, Tree node) throws Exception {
            write(action.toString());
        }

        @Override
        public void endActions() throws Exception {
        }

        private void write(String fmt, Object... objs) throws IOException {
            writer.append(fmt);
            writer.append("\n");
        }

        private String toS(Tree node) {
            return String.format("%s", node.toString());
        }
    }
}
