import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ParseResult;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

public class ASTToGraphViz {

    private static AtomicInteger nodeIdCounter = new AtomicInteger();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Eingabe des zu parsenden Java-Dateipfads
        System.out.println("Bitte den Pfad der zu parsenden Java-Datei eingeben:");
        String inputFilePath = scanner.nextLine();

        // Eingabe des Zielpfads für die PNG-Datei (inkl. Dateiname, z.B. "/pfad/zur/ast.png")
        System.out.println("Bitte den Pfad (inkl. Dateinamen) angeben, wohin die PNG-Datei geschrieben werden soll:");
        String outputPngPath = scanner.nextLine();

        File inputFile = new File(inputFilePath);
        if (!inputFile.exists()) {
            System.err.println("Die Datei existiert nicht: " + inputFile.getAbsolutePath());
            return;
        }

        try {
            // Java-Datei parsen
            JavaParser parser = new JavaParser();
            ParseResult<CompilationUnit> result = parser.parse(inputFile);
            if (!result.isSuccessful() || !result.getResult().isPresent()) {
                System.err.println("Parsing fehlgeschlagen. Probleme:");
                result.getProblems().forEach(problem -> System.err.println(problem));
                return;
            }
            CompilationUnit cu = result.getResult().get();

            // DOT-Code generieren
            StringBuilder dot = new StringBuilder();
            dot.append("digraph AST {\n");
            traverseAST(cu, dot, -1);
            dot.append("}");

            // DOT-Datei in das aktuelle Arbeitsverzeichnis schreiben
            String dotFileName = "ast.dot";
            try (FileWriter writer = new FileWriter(dotFileName)) {
                writer.write(dot.toString());
            }
            System.out.println("DOT-Datei wurde erzeugt: " + dotFileName);

            // GraphViz über den Konsolenbefehl aufrufen, um die PNG-Datei zu erzeugen
            ProcessBuilder pb = new ProcessBuilder("dot", "-Tpng", dotFileName, "-o", outputPngPath);
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("PNG-Datei wurde erfolgreich erzeugt: " + outputPngPath);
            } else {
                System.err.println("Fehler beim Erzeugen der PNG-Datei. Exit Code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Traversiert den AST rekursiv und fügt Knoten und Kanten in den DOT-Code ein.
     * @param node Aktueller Knoten
     * @param dot StringBuilder zum Aufbau der DOT-Datei
     * @param parentId ID des Elternknotens, -1 falls keiner vorhanden
     * @return Die eindeutige ID des aktuellen Knotens
     */
    private static int traverseAST(Node node, StringBuilder dot, int parentId) {
        int currentId = nodeIdCounter.incrementAndGet();
        // Der Klassenname des Knotens dient als Label
        String label = node.getClass().getSimpleName().replace("Impl", "");
        dot.append("  ").append(currentId).append(" [label=\"").append(label).append("\"];\n");

        if (parentId != -1) {
            dot.append("  ").append(parentId).append(" -> ").append(currentId).append(";\n");
        }

        for (Node child : node.getChildNodes()) {
            traverseAST(child, dot, currentId);
        }
        return currentId;
    }
}