package view.webdiff;

import actions.ASTDiff;
import utils.Pair;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;
import spark.Spark;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import ASTDiff.ProjectASTDiff;
import ASTDiff.DirComperator;

import static spark.Spark.*;

public class WebDiff  {
    public static final String JQUERY_JS_URL = "https://code.jquery.com/jquery-3.4.1.min.js";
    public static final String BOOTSTRAP_CSS_URL = "https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css";
    public static final String BOOTSTRAP_JS_URL = "https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js";
    public static final int port = 5678;

    public ProjectASTDiff projectASTDiff;


    public WebDiff(ProjectASTDiff projectASTDiff) {
        this.projectASTDiff = projectASTDiff;
    }

    public void run() {
        //TODO:
//        DirectoryComparator comparator = new DirectoryComparator();

        DirComperator comparator = new DirComperator(projectASTDiff.getProjectData());
        configureSpark(comparator, this.port);
        Spark.awaitInitialization();
        System.out.println(String.format("Starting server: %s:%d.", "http://127.0.0.1", this.port));
    }

    public void configureSpark(final DirComperator comparator, int port) {
        port(port);
        staticFiles.location("/web/");
        get("/", (request, response) -> {
//            if (comparator.isDirMode())
                response.redirect("/list");
//            else
//                response.redirect("/monaco-diff/0");
            return "";
        });
        get("/list", (request, response) -> {
            Renderable view = new DirectoryDiffView(comparator);
            return render(view);
        });
        get("/vanilla-diff/:id", (request, response) -> {
//            int id = Integer.parseInt(request.params(":id"));
            String id = (request.params(":id"));
            String _id = id.replace("*","/");
            Pair<String, String> pair = comparator.getFileContentsPair(_id);
            ASTDiff diff = projectASTDiff.astDiffByName(_id);
            Renderable view = new VanillaDiffView(_id,_id,pair.first, pair.second, diff, false);
            return render(view);
        });
//        get("/monaco-diff/:id", (request, response) -> {
//            int id = Integer.parseInt(request.params(":id"));
//            Pair<File, File> pair = comparator.getModifiedFiles().get(id);
//            ASTDiff diff = projectASTDiff.astDiffByName(pair.first.getAbsolutePath());
//            Renderable view = new MonacoDiffView(pair.first, pair.second, diff, id);
//            return render(view);
//        });
//        get("/raw-diff/:id", (request, response) -> {
//            int id = Integer.parseInt(request.params(":id"));
//            Pair<File, File> pair = comparator.getModifiedFiles().get(id);
//            ASTDiff diff = projectASTDiff.astDiffByName(pair.first.getAbsolutePath());
//            Renderable view = new TextDiffView(pair.first, pair.second, diff,  id);
//            return render(view);
//        });
        get("/left/:id", (request, response) -> {
//            int id = Integer.parseInt(request.params(":id"));
            String id = (request.params(":id"));
            String _id = id.replace("*","/");
            Pair<String, String> pair = comparator.getFileContentsPair(_id);
            return pair.first;
        });
        get("/right/:id", (request, response) -> {
//            int id = Integer.parseInt(request.params(":id"));
            String id = (request.params(":id"));
            String _id = id.replace("*","/");
            Pair<String, String> pair = comparator.getFileContentsPair(_id);
            return pair.second;
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
