import javax.tools.*;
import com.sun.source.util.JavacTask;
import java.nio.file.*;
import java.util.*;
import java.io.*;

/** Parsing real do compilador Java; não substitui build/tipagem contra o Android SDK. */
class CheckJavaSyntax {
    public static void main(String[] args)throws Exception {
        JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();List<File> files=new ArrayList<>();
        try(java.util.stream.Stream<Path> walk=Files.walk(Paths.get("app/src"))){walk.filter(p->p.toString().endsWith(".java")).forEach(p->files.add(p.toFile()));}
        DiagnosticCollector<JavaFileObject> diagnostics=new DiagnosticCollector<>();
        try(StandardJavaFileManager manager=compiler.getStandardFileManager(diagnostics,Locale.ROOT,java.nio.charset.StandardCharsets.UTF_8)){
            JavacTask task=(JavacTask)compiler.getTask(null,manager,diagnostics,Arrays.asList("--release","11","-proc:none"),null,manager.getJavaFileObjectsFromFiles(files));task.parse();
        }
        int errors=0;for(Diagnostic<?> d:diagnostics.getDiagnostics())if(d.getKind()==Diagnostic.Kind.ERROR){System.out.println(d);errors++;}
        if(errors>0)throw new AssertionError(errors+" erros de sintaxe.");
        System.out.println("PASS: sintaxe Java de "+files.size()+" arquivos. Tipagem Android ainda requer SDK/dependências.");
    }
}
