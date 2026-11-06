package net.createmod.ponder.guide;

import java.nio.file.*;
import java.util.*;
import net.createmod.ponder.api.guide.Tutorial;
import net.createmod.ponder.api.guide.Tutorial.*;
import net.createmod.ponder.api.editor.MarkdownDocument;
import net.createmod.ponder.foundation.*;
import net.createmod.ponder.foundation.registration.*;
import net.minecraft.util.ResourceLocation;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class MarkdownTutorialTest {
    @Rule public TemporaryFolder tmp=new TemporaryFolder();
    private static final ResourceLocation ID=new ResourceLocation("test:network");
    private static final String HEADER="---\nid: test:network\ntitle: First network\nitem_ids: [minecraft:stone, minecraft:dirt]\nstructure: ponder:empty\nstep_ticks: 40\n---\n";
    private Tutorial compile(String markdown){return MarkdownTutorialCompiler.compile(ID,markdown,"test.md");}
    @Test public void oneMarkdownDrivesTwoPresentationsAndStableSnapshots(){
        Tutorial tutorial=compile(HEADER+"# Network\n\n## Power\n\nSupply power.\n\n```ponder\n[{\"op\":\"scene.idle\",\"ticks\":10}]\n```\n\n## Store\n\nInsert a cell.\n");
        assertEquals(2,tutorial.scenes().size());assertEquals(Presentation.BOTH,tutorial.presentation());
        assertEquals(ID,tutorial.scenes().get(0).getSceneId());
        assertEquals(List.of(60,100),tutorial.blocks().stream().filter(b->b.snapshot()!=null).map(b->b.snapshot().tick()).toList());
        var entry=new PonderStoryBoardEntry(tutorial.scenes().get(0).asStoryBoard(),"test",new ResourceLocation("ponder:empty"),new ResourceLocation("minecraft:stone"));
        var scene=PonderSceneRegistry.compileScene(new PonderLocalization(),entry,null);
        assertEquals(2,scene.getNarration().size());assertEquals("Supply power.",scene.getNarration().get(0).text().get());
        assertEquals(List.of(30,70),scene.getKeyframes());
    }
    @Test public void multipleSubjectsShareOneStoryboardIdentity() {
        var builder=new TutorialRegistry.Builder();builder.addMarkdown(ID,HEADER+"# Network\n\nShared step.\n");
        var scenes=new PonderSceneRegistry(new PonderLocalization());builder.build(scenes);
        var entries=new ArrayList<>(scenes.getRegisteredEntries());
        assertEquals(2,entries.size());assertSame(entries.get(0).getValue().getBoard(),entries.get(1).getValue().getBoard());
    }
    @Test public void dedicatedContentStaysInItsPresentation(){
        Tutorial page=compile(HEADER+"# Test\n\n<GuideOnly>\n\nGuide details.\n\n</GuideOnly>\n\n<AnimationOnly>\n\nAnimated narration.\n\n</AnimationOnly>\n\nShared narration.\n");
        String guide=page.blocks().toString();assertTrue(guide.contains("Guide details"));assertFalse(guide.contains("Animated narration"));
        String ir=page.scenes().get(0).serialize().toString();assertTrue(ir.contains("Animated narration"));assertFalse(ir.contains("Guide details"));
        assertEquals(1,page.blocks().stream().filter(b->b.snapshot()!=null).count());
    }
    @Test public void supportsNativeGuideMeWidgetsAndGfmTables(){
        Tutorial page=compile("---\npresentation: guide\n---\n# Static\n\n<GameScene interactive={true}>\n<Block id=\"minecraft:stone\" x={1} y={1} />\n<BlockAnnotation x={1} y={1} color=\"#00ff00\">Input</BlockAnnotation>\n</GameScene>\n\n<ItemLink id=\"minecraft:stone\" />\n\n<RecipeFor item=\"minecraft:stone\" />\n\n| A | B |\n| - | - |\n| In | Out |\n");
        assertTrue(page.scenes().isEmpty());assertTrue(page.blocks().stream().anyMatch(b->b.kind()==Kind.TABLE));
        assertNotNull(page.blocks().stream().filter(b->b.snapshot()!=null).findFirst().get().snapshot().definition());
    }
    @Test public void linksExistingAnimationWithoutReplacingIt(){
        Tutorial page=compile("---\nitem_ids: [minecraft:stone]\nscene: test:old_scene\n---\n# Native guide\n\n<GameScene scene=\"test:old_scene\" tick={240} />\n");
        assertTrue(page.scenes().isEmpty());assertEquals(new ResourceLocation("test:old_scene"),page.animation());
    }
    @Test public void actionsUseTheExistingStrictInstructionValidator(){
        assertThrows(IllegalArgumentException.class,()->compile(HEADER+"```ponder\n[{\"op\":\"world.unknown\"}]\n```"));
        assertThrows(IllegalArgumentException.class,()->compile(HEADER+"```ponder\n[{\"op\":\"scene.idle\",\"ticks\":-1}]\n```"));
    }
    @Test public void indexListLinksRemainInteractive() {
        Tutorial page=compile("# Index\n\n- [Target](target.md)\n");
        assertEquals("target.md",page.blocks().stream().filter(b->b.kind()==Kind.TEXT).findFirst().get().target());
    }
    @Test public void neverEvaluatesMdxJavaScript(){
        assertThrows(IllegalArgumentException.class,()->compile("# Unsafe\n\n<GameScene zoom={Runtime.exec()} />"));
        assertThrows(IllegalArgumentException.class,()->compile("# Unsafe\n\n<GameScene {...anything} />"));
    }
    @Test public void safeLocalOverrideRetainsLastGoodAndHonorsDeletion()throws Exception{
        GuideSources.setRoot(tmp.newFolder().getAbsoluteFile());
        GuideSources.save(ID,HEADER+"# Valid\n\nStored.\n");
        Map<ResourceLocation,Tutorial> pages=new LinkedHashMap<>();GuideSources.loadLocal(pages);assertEquals(1,pages.size());
        Path file=GuideSources.path(ID);Files.writeString(file,"---\nunknown_key: false\n---\n# Broken");
        pages.clear();GuideSources.loadLocal(pages);assertEquals("First network",pages.get(ID).title());
        Files.delete(file);pages.clear();GuideSources.loadLocal(pages);assertTrue(pages.isEmpty());
    }
    @Test public void pathTraversalAndSymlinkWritesAreRejected()throws Exception{
        assertThrows(IllegalArgumentException.class,()->MarkdownTutorialCompiler.id("test:../outside"));
        Path root=tmp.newFolder().toPath();GuideSources.setRoot(root.toFile());
        Files.createSymbolicLink(root.resolve("test"),tmp.newFolder().toPath());
        assertThrows(java.io.IOException.class,()->GuideSources.save(ID,HEADER+"# Safe"));
    }
    @Test public void multilineEditsUndoSelectionAndNormalizePaste(){
        MarkdownDocument doc=new MarkdownDocument("one\ntwo");doc.select(doc.text().length(),false);doc.insert("\r\nthree");assertEquals("one\ntwo\nthree",doc.text());
        doc.moveVertical(-1,false);assertEquals(7,doc.cursor());doc.select(4,false);doc.select(7,true);doc.insert("second");
        assertEquals("one\nsecond\nthree",doc.text());doc.undo();assertEquals("one\ntwo\nthree",doc.text());doc.redo();assertEquals("one\nsecond\nthree",doc.text());
        assertTrue(doc.dirty());doc.saved();assertFalse(doc.dirty());
    }
}
