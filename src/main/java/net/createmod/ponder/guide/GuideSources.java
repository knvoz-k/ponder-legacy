package net.createmod.ponder.guide;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Supplier;
import net.createmod.ponder.Ponder;
import net.createmod.ponder.api.guide.Tutorial;
import net.minecraft.util.ResourceLocation;

/** Resource packs may override bundled Markdown; local authoring overrides are loaded last. */
public final class GuideSources {
    public interface ResourceProvider { InputStream open(ResourceLocation location) throws IOException; }
    private static ResourceProvider resources = location -> GuideSources.class.getResourceAsStream("/assets/"+location.getNamespace()+"/"+location.getPath());
    private static Supplier<String> language = () -> "en_us";
    private static Path root = Paths.get("scripts/ponder/guides").toAbsolutePath().normalize();
    private static final Map<String, Tutorial> lastGood = new LinkedHashMap<>();
    private GuideSources() {}
    public static void setRoot(File directory) { root = directory.toPath().toAbsolutePath().normalize(); lastGood.clear(); }
    public static Path root() { return root; }
    public static void setResourceProvider(ResourceProvider provider, Supplier<String> currentLanguage) {
        resources = provider; language = currentLanguage;
    }
    public static String read(ResourceLocation resource) {
        String path = "ponder/guides/"+resource.getPath()+".md";
        for (String candidate : List.of("ponder/guides/"+language.get().toLowerCase(Locale.ROOT)+"/"+resource.getPath()+".md",
            "ponder/guides/en_us/"+resource.getPath()+".md", path)) {
            try (InputStream stream=resources.open(new ResourceLocation(resource.getNamespace(),candidate))) {
                if (stream != null) return bounded(stream);
            } catch (FileNotFoundException missing) {
                // Try the source-language page next.
            } catch (IOException failure) { throw new IllegalArgumentException("Could not read "+candidate, failure); }
        }
        throw new IllegalArgumentException("Missing Markdown resource: "+resource);
    }
    public static List<ResourceLocation> readIndex(ResourceLocation resource) {
        ResourceLocation path = new ResourceLocation(resource.getNamespace(), "ponder/guides/"+resource.getPath()+".json");
        try (InputStream stream = resources.open(path)) {
            if (stream == null) throw new FileNotFoundException(path.toString());
            com.google.gson.JsonObject index = new com.google.gson.JsonParser().parse(bounded(stream)).getAsJsonObject();
            List<ResourceLocation> result = new ArrayList<>();
            for (var entry : index.getAsJsonArray("pages")) {
                String id = entry.getAsString();
                result.add(MarkdownTutorialCompiler.id(id.contains(":") ? id : resource.getNamespace()+":"+id));
                if (result.size()>1024) throw new IOException("Markdown index exceeds 1024 pages");
            }
            return List.copyOf(result);
        } catch (IOException | RuntimeException failure) {
            throw new IllegalArgumentException("Cannot read Markdown index "+path, failure);
        }
    }
    private static String bounded(InputStream stream) throws IOException {
        byte[] bytes=stream.readNBytes(MarkdownTutorialCompiler.MAX_CHARACTERS*4+1);
        if(bytes.length>MarkdownTutorialCompiler.MAX_CHARACTERS*4)throw new IOException("Markdown resource too large");
        return new String(bytes,StandardCharsets.UTF_8);
    }
    public static Path path(ResourceLocation id) throws IOException {
        MarkdownTutorialCompiler.id(id.toString());
        Files.createDirectories(root);
        Path path = root.resolve(id.getNamespace()).resolve(id.getPath()+".md").normalize();
        if (!path.startsWith(root)) throw new IOException("Markdown path escaped the guide directory");
        Path check=path;
        while(check!=null && check.startsWith(root)) {
            if(Files.isSymbolicLink(check))throw new IOException("Symlinks are not allowed in guide paths");
            check=check.getParent();
        }
        return path;
    }
    public static void save(ResourceLocation id,String markdown) throws IOException {
        MarkdownTutorialCompiler.compile(id,markdown,id+".md");
        Path target=path(id); Files.createDirectories(target.getParent());
        Path temp=Files.createTempFile(target.getParent(),".guide-",".tmp");
        try {
            Files.writeString(temp,markdown,StandardCharsets.UTF_8);
            try { Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch(AtomicMoveNotSupportedException e){Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);}
        } finally { Files.deleteIfExists(temp); }
    }
    static void loadLocal(Map<ResourceLocation,Tutorial> pages) {
        if(!Files.isDirectory(root))return;
        Map<String,Tutorial> selected=new LinkedHashMap<>();
        Set<ResourceLocation> seen = new HashSet<>();
        try(var paths=Files.walk(root)) {
            List<Path> files=paths.filter(p->p.toString().endsWith(".md")).sorted().toList();
            if(files.size()>1024)throw new IOException("Too many Markdown guide files");
            long total=0;
            for(Path file:files) {
                String source=root.relativize(file).toString().replace(File.separatorChar,'/');
                try {
                    total+=Files.size(file);if(total>16*1024*1024)throw new IOException("Markdown guides exceed 16 MiB");
                    if(Files.isSymbolicLink(file)||!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS))throw new IOException("Expected a regular guide file");
                    int split=source.indexOf('/');if(split<1)throw new IOException("Use guides/<namespace>/<page>.md");
                    ResourceLocation id=MarkdownTutorialCompiler.id(source.substring(0,split)+":"+source.substring(split+1,source.length()-3));
                    if(!path(id).equals(file.toAbsolutePath().normalize()))throw new IOException("Unexpected Markdown path");
                    if(Files.size(file)>MarkdownTutorialCompiler.MAX_CHARACTERS*4)throw new IOException("Markdown file too large");
                    Tutorial page=MarkdownTutorialCompiler.compile(id,Files.readString(file),source);
                    if (!seen.add(page.id())) throw new IllegalArgumentException("Duplicate local tutorial id: "+page.id());
                    selected.put(source,page);pages.put(page.id(),page);
                } catch(RuntimeException|IOException e) {
                    Ponder.LOGGER.error("Invalid Ponder Markdown guide {}: {}",source,e.getMessage());
                    Tutorial old=lastGood.get(source);if(old!=null&&seen.add(old.id())){selected.put(source,old);pages.put(old.id(),old);}
                }
            }
        }catch(IOException failure){throw new IllegalArgumentException("Cannot scan Markdown guides",failure);}
        lastGood.clear();lastGood.putAll(selected);
    }
}
