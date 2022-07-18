

package view.webdiff;

import actions.ASTDiff;
import io.DirectoryComparator;
import utils.Pair;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;
import spark.Spark;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import ASTDiff.ProjectASTDiffer;

import static spark.Spark.*;

public class WebDiff  {
    public static final String JQUERY_JS_URL = "https://code.jquery.com/jquery-3.4.1.min.js";
    public static final String BOOTSTRAP_CSS_URL = "https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css";
    public static final String BOOTSTRAP_JS_URL = "https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js";
    public static final int port = 5678;

    public ProjectASTDiffer projectASTDiffer;


    public WebDiff(ProjectASTDiffer projectASTDiffer) {
        this.projectASTDiffer = projectASTDiffer;
    }

    public void run() {
        DirectoryComparator comparator = new DirectoryComparator(this.projectASTDiffer.getSrcPath(), this.projectASTDiffer.getDstPath());
        comparator.compare();
        configureSpark(comparator, this.port);
        Spark.awaitInitialization();
        System.out.println(String.format("Starting server: %s:%d.", "http://127.0.0.1", this.port));
    }

    public void configureSpark(final DirectoryComparator comparator, int port) {
        port(port);
        staticFiles.location("/web/");
        get("/", (request, response) -> {
            if (comparator.isDirMode())
                response.redirect("/list");
            else
                response.redirect("/monaco-diff/0");
            return "";
        });
        get("/list", (request, response) -> {
            Renderable view = new DirectoryDiffView(comparator);
            return render(view);
        });
        get("/vanilla-diff/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
            Pair<File, File> pair = comparator.getModifiedFiles().get(id);
            ASTDiff diff = projectASTDiffer.getASTDiffbyFileName(pair.first.getAbsolutePath());
            Renderable view = new VanillaDiffView(pair.first, pair.second, diff, false);
            return render(view);
        });
        get("/monaco-diff/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
            Pair<File, File> pair = comparator.getModifiedFiles().get(id);
            ASTDiff diff = projectASTDiffer.getASTDiffbyFileName(pair.first.getAbsolutePath());
            Renderable view = new MonacoDiffView(pair.first, pair.second, diff, id);
            return render(view);
        });
        get("/monaco-native-diff/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
            Pair<File, File> pair = comparator.getModifiedFiles().get(id);
            Renderable view = new MonacoNativeDiffView(pair.first, pair.second, id);
            return render(view);
        });
        get("/mergely-diff/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
            Renderable view = new MergelyDiffView(id);
            return render(view);
        });
//        get("/raw-diff/:id", (request, response) -> {
//            int id = Integer.parseInt(request.params(":id"));
//            Pair<File, File> pair = comparator.getModifiedFiles().get(id);
//            Diff diff = getDiff(pair.first.getAbsolutePath(), pair.second.getAbsolutePath());
//            Renderable view = new TextDiffView(pair.first, pair.second, diff);
//            return render(view);
//        });
        get("/left/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
            Pair<File, File> pair = comparator.getModifiedFiles().get(id);
            return readFile(pair.first.getAbsolutePath(), Charset.defaultCharset());
        });
        get("/right/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
            Pair<File, File> pair = comparator.getModifiedFiles().get(id);
            return readFile(pair.second.getAbsolutePath(), Charset.defaultCharset());
        });
        get("/quit", (request, response) -> {
            System.exit(0);
            return "";
        });
    }

    private static String render(Renderable r) throws IOException {
        HtmlCanvas c = new HtmlCanvas();
        r.renderOn(c);
        return c.toHtml();
    }

    private static String readFile(String path, Charset encoding)  throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
}
