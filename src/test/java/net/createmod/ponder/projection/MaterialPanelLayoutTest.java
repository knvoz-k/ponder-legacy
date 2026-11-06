package net.createmod.ponder.projection;

import java.util.List;
import net.createmod.ponder.client.projection.MaterialPanelLayout;
import net.createmod.ponder.client.projection.MaterialPanelLayout.Rect;
import org.junit.Test;
import static org.junit.Assert.*;

public class MaterialPanelLayoutTest {
    @Test public void avoidsContainerAndRecipeBookEvenWhenDraggedOntoSlots(){
        var container=new Rect(250,70,176,166);var recipes=new Rect(96,70,154,166);
        var result=MaterialPanelLayout.place(640,360,146,195,270,90,List.of(container,recipes));
        assertEquals(195,result.height());assertFalse(result.intersects(container));assertFalse(result.intersects(recipes));
    }
    @Test public void narrowScreensCollapseToFreeTopStrip(){
        var container=new Rect(68,33,184,174);
        var result=MaterialPanelLayout.place(320,240,146,195,165,8,List.of(container));
        assertTrue(result.height()>=24);assertTrue(result.height()<50);assertFalse(result.intersects(container));
    }
    @Test public void offscreenSavedPositionsClampAfterWindowResize(){
        var result=MaterialPanelLayout.place(320,240,146,69,900,-400,List.of());
        assertEquals(170,result.x());assertEquals(4,result.y());assertTrue(result.right()<=316);
        assertEquals(0,MaterialPanelLayout.normalize(-10,320,146));assertEquals(10000,MaterialPanelLayout.normalize(900,320,146));
        assertEquals(174,MaterialPanelLayout.restore(10000,320,146));
    }
    @Test public void coveredScreensNeverPlaceTargetsOnAnOccupiedControl(){
        var result=MaterialPanelLayout.place(320,240,146,195,8,8,List.of(new Rect(0,0,320,240)));
        assertEquals(0,result.height());
    }
}
