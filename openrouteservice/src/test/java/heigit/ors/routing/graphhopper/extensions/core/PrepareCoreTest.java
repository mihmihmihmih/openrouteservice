/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for 
 *  additional information regarding copyright ownership.
 * 
 *  GraphHopper GmbH licenses this file to you under the Apache License, 
 *  Version 2.0 (the "License"); you may not use this file except in 
 *  compliance with the License. You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package heigit.ors.routing.graphhopper.extensions.core;

import heigit.ors.routing.graphhopper.extensions.core.CoreTestEdgeFilter;

import com.carrotsearch.hppc.IntIndexedContainer;
import com.graphhopper.routing.*;
//import com.graphhopper.routing.ch.PrepareContractionHierarchies.Shortcut;
import com.graphhopper.routing.util.AllCHEdgesIterator;
import com.graphhopper.routing.util.BikeFlagEncoder;
import com.graphhopper.routing.util.CarFlagEncoder;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.weighting.ShortestWeighting;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.*;
import com.graphhopper.util.*;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static com.graphhopper.util.Parameters.Algorithms.DIJKSTRA_BI;
import static org.junit.Assert.*;

/**
 * @author Hendrik Leuschner
 */
public class PrepareCoreTest {
    private final CarFlagEncoder carEncoder = new CarFlagEncoder();
    private final EncodingManager encodingManager = new EncodingManager(carEncoder);
    private final Weighting weighting = new ShortestWeighting(carEncoder);
    private final TraversalMode tMode = TraversalMode.NODE_BASED;
    private Directory dir;

    @Before
    public void setUp() {
        dir = new GHDirectory("", DAType.RAM_INT);
    }

    public static Graph createSimpleGraph(Graph g) {
        //5-1-----2
        //   \ __/|
        //    0   |
        //   /    |
        //  4-----3
        //
        g.edge(0, 1, 1, true);
        g.edge(0, 2, 1, true);
        g.edge(0, 4, 3, true);
        g.edge(1, 2, 2, true);
        g.edge(2, 3, 1, true);
        g.edge(4, 3, 2, true);
        g.edge(5, 1, 2, true);
        return g;
    }

    public static Graph createMoreComplexGraph(Graph g) {
//        3--------4--5
//        |\       |  |
//        | \      6--7
//        2--0        |
//        | / \__   _/
//        |/     \ /
//        1-------8
        g.edge(0, 1, 1, true);
        g.edge(0, 2, 1, true);
        g.edge(0, 3, 5, true);
        g.edge(0, 8, 1, true);
        g.edge(1, 2, 1, true);
        g.edge(1, 8, 2, true);
        g.edge(2, 3, 2, true);
        g.edge(3, 4, 2, true);
        g.edge(4, 5, 1, true);
        g.edge(4, 6, 1, true);
        g.edge(5, 7, 1, true);
        g.edge(6, 7, 2, true);
        g.edge(7, 8, 3, true);
        return g;
    }

    // prepare-routing.svg
    public static Graph initShortcutsGraph(Graph g) {
        g.edge(0, 1, 1, true);
        g.edge(0, 2, 1, true);
        g.edge(1, 2, 1, true);
        g.edge(2, 3, 1.5, true);
        g.edge(1, 4, 1, true);
        g.edge(2, 9, 1, true);
        g.edge(9, 3, 1, true);
        g.edge(10, 3, 1, true);
        g.edge(4, 5, 1, true);
        g.edge(5, 6, 1, true);
        g.edge(6, 7, 1, true); //make this restricted; edge 10
        g.edge(7, 8, 1, true);
        g.edge(8, 9, 1, true);
        g.edge(4, 11, 1, true);
        g.edge(9, 14, 1, true);
        g.edge(10, 14, 1, true);
        g.edge(11, 12, 1, true);
        g.edge(12, 15, 1, true); //make this restricted; edge 17
        g.edge(12, 13, 1, true);
        g.edge(13, 16, 1, true);
        g.edge(15, 16, 2, true);
        g.edge(14, 16, 1, true);
        return g;
    }
    GraphHopperStorage createGHStorage() {
        return new GraphBuilder(encodingManager).setCHGraph(weighting).create();
    }


    @Test
    public void testSimpleUnrestrictedGraph() {
        GraphHopperStorage g = createGHStorage();
        CHGraph lg = g.getGraph(CHGraph.class);
        createSimpleGraph(lg);
        int oldCount = g.getAllEdges().getMaxId();
        CoreTestEdgeFilter restrictedEdges = new CoreTestEdgeFilter();
        PrepareCore prepare = new PrepareCore(dir, g, lg, weighting, tMode, restrictedEdges);
        prepare.doWork();
        for(int i = 0; i < lg.getNodes(); i++)
            System.out.println("nodeId " + i + " level: " + lg.getLevel(i));
        AllCHEdgesIterator iter = lg.getAllEdges();
        while(iter.next()){
            System.out.print(iter.getBaseNode() + " -> " + iter.getAdjNode() + " via edge " + iter.getEdge());
            if(iter.isShortcut()) System.out.println(" (shortcut)");
            else System.out.println(" ");
        }
        assertEquals(oldCount, g.getAllEdges().getMaxId());
        assertEquals(oldCount + 1, lg.getAllEdges().getMaxId());
    }

    @Test
    public void testSimpleRestrictedGraph() {
        GraphHopperStorage g = createGHStorage();
        CHGraph lg = g.getGraph(CHGraph.class);
        createSimpleGraph(lg);
        int oldCount = g.getAllEdges().getMaxId();
        CoreTestEdgeFilter restrictedEdges = new CoreTestEdgeFilter();
        restrictedEdges.add(5);
        restrictedEdges.add(2);
        PrepareCore prepare = new PrepareCore(dir, g, lg, weighting, tMode, restrictedEdges);
        prepare.doWork();
        for(int i = 0; i < lg.getNodes(); i++)
            System.out.println("nodeId " + i + " level: " + lg.getLevel(i));
        AllCHEdgesIterator iter = lg.getAllEdges();
        while(iter.next()){
            System.out.print(iter.getBaseNode() + " -> " + iter.getAdjNode() + " via edge " + iter.getEdge());
            if(iter.isShortcut()) System.out.println(" (shortcut)");
            else System.out.println(" ");
        }
        assertEquals(oldCount, g.getAllEdges().getMaxId());
        assertEquals(oldCount + 1, lg.getAllEdges().getMaxId());

        Integer coreNodeIds[] = {0, 3, 4};
        assertCore(lg, new HashSet<>(Arrays.asList(coreNodeIds)));
    }

    private void assertCore(CHGraph g, Set<Integer> coreNodes) {
        int nodes = g.getNodes();
        int maxLevel = nodes + 1;
        for (int node = 0; node < nodes; node++) {
            int level = g.getLevel(node);
            if (coreNodes.contains(node)) {
                assertEquals(maxLevel, level);
            } else {
                assertTrue(level < nodes);
            }
        }
    }

    @Test
    public void testLargeUnrestrictedGraph() {
        GraphHopperStorage g = createGHStorage();
        CHGraph lg = g.getGraph(CHGraph.class);
        initShortcutsGraph(lg);
        int oldCount = g.getAllEdges().getMaxId();
        CoreTestEdgeFilter restrictedEdges = new CoreTestEdgeFilter();
        PrepareCore prepare = new PrepareCore(dir, g, lg, weighting, tMode, restrictedEdges);
        prepare.doWork();
        for(int i = 0; i < lg.getNodes(); i++)
            System.out.println("nodeId " + i + " level: " + lg.getLevel(i));
        AllCHEdgesIterator iter = lg.getAllEdges();
        while(iter.next()){
            System.out.print(iter.getBaseNode() + " -> " + iter.getAdjNode() + " via edge " + iter.getEdge());
            if(iter.isShortcut()) System.out.println(" (shortcut)");
            else System.out.println(" ");
        }
        assertEquals(oldCount, g.getAllEdges().getMaxId());
        assertEquals(oldCount + 7, lg.getAllEdges().getMaxId());
    }

    @Test
    public void testLargeRestrictedGraph() {
        GraphHopperStorage g = createGHStorage();
        CHGraph lg = g.getGraph(CHGraph.class);
        initShortcutsGraph(lg);
        int oldCount = g.getAllEdges().getMaxId();
        CoreTestEdgeFilter restrictedEdges = new CoreTestEdgeFilter();
        restrictedEdges.add(10);
        restrictedEdges.add(17);
        PrepareCore prepare = new PrepareCore(dir, g, lg, weighting, tMode, restrictedEdges);
        prepare.doWork();
        for(int i = 0; i < lg.getNodes(); i++)
            System.out.println("nodeId " + i + " level: " + lg.getLevel(i));
        AllCHEdgesIterator iter = lg.getAllEdges();
        while(iter.next()){
            System.out.print(iter.getBaseNode() + " -> " + iter.getAdjNode() + " via edge " + iter.getEdge());
            if(iter.isShortcut()) System.out.println(" (shortcut)");
            else System.out.println(" ");
        }
        assertEquals(oldCount, g.getAllEdges().getMaxId());
        assertEquals(oldCount + 10, lg.getAllEdges().getMaxId());
    }

    @Test
    public void testMoreComplexUnrestrictedGraph() {
        GraphHopperStorage g = createGHStorage();
        CHGraph lg = g.getGraph(CHGraph.class);
        createMoreComplexGraph(lg);
        int oldCount = g.getAllEdges().getMaxId();
        CoreTestEdgeFilter restrictedEdges = new CoreTestEdgeFilter();
        PrepareCore prepare = new PrepareCore(dir, g, lg, weighting, tMode, restrictedEdges);
        prepare.doWork();
        for(int i = 0; i < lg.getNodes(); i++)
            System.out.println("nodeId " + i + " level: " + lg.getLevel(i));
        AllCHEdgesIterator iter = lg.getAllEdges();
        while(iter.next()){
            System.out.print(iter.getBaseNode() + " -> " + iter.getAdjNode() + " via edge " + iter.getEdge());
            if(iter.isShortcut()) System.out.println(" (shortcut from  " + iter.getSkippedEdge1() + " and " + iter.getSkippedEdge2() + ")");
            else System.out.println(" ");
        }



//        assertEquals(oldCount, g.getAllEdges().getMaxId());
//        assertEquals(oldCount + 7, lg.getAllEdges().getMaxId());
    }

    private CHGraph testMoreComplexRestrictedGraph(CoreTestEdgeFilter restrictedEdges) {
        GraphHopperStorage g = createGHStorage();
        CHGraph lg = g.getGraph(CHGraph.class);
        createMoreComplexGraph(lg);
        int oldCount = g.getAllEdges().getMaxId();
        PrepareCore prepare = new PrepareCore(dir, g, lg, weighting, tMode, restrictedEdges);
        prepare.doWork();
        for(int i = 0; i < lg.getNodes(); i++)
            System.out.println("nodeId " + i + " level: " + lg.getLevel(i));
        AllCHEdgesIterator iter = lg.getAllEdges();
        while(iter.next()){
            System.out.print(iter.getBaseNode() + " -> " + iter.getAdjNode() + " via edge " + iter.getEdge());
            if(iter.isShortcut()) System.out.println(" (shortcut)");
            else System.out.println(" ");
        }
        return lg;

    }

    //With a restriction on {0->1}
    @Test
    public void testRestrictedGraph1(){
        CoreTestEdgeFilter restrictedEdges = new CoreTestEdgeFilter();
        restrictedEdges.add(0);
        CHGraph lg = testMoreComplexRestrictedGraph(restrictedEdges);
        AllCHEdgesIterator iter = lg.getAllEdges();

        while(iter.next()){
            if(iter.isShortcut()){
                if(iter.getEdge() == 13){
                    assertEquals(iter.getBaseNode(), 4);
                    assertEquals(iter.getAdjNode(), 7);
                    continue;

                }
                if(iter.getEdge()==14){
                    assertEquals(iter.getBaseNode(), 1);
                    assertEquals(iter.getAdjNode(), 3);
                    continue;

                }
                if(iter.getEdge()==15){
                    assertEquals(iter.getBaseNode(), 0);
                    assertEquals(iter.getAdjNode(), 3);
                    continue;

                }
                if(iter.getEdge()==16){
                    assertEquals(iter.getBaseNode(), 1);
                    assertEquals(iter.getAdjNode(), 4);
                    continue;

                }
                if(iter.getEdge()==17){
                    assertEquals(iter.getBaseNode(), 0);
                    assertEquals(iter.getAdjNode(), 4);

                }
            }
        }
        assertEquals(10,lg.getLevel(0));
        assertEquals(10,lg.getLevel(1));
    }

    //restrictions on edges:{0->1,2->3}
    @Test
    public void testRestrictedGraph2(){
        CoreTestEdgeFilter restrictedEdges = new CoreTestEdgeFilter();
        restrictedEdges.add(0);
        restrictedEdges.add(6);
        CHGraph lg = testMoreComplexRestrictedGraph(restrictedEdges);
        AllCHEdgesIterator iter = lg.getAllEdges();

        while(iter.next()){
            if(iter.isShortcut()){
                if(iter.getEdge() == 13){
                    assertEquals(iter.getBaseNode(), 4);
                    assertEquals(iter.getAdjNode(), 7);
                    continue;

                }
                if(iter.getEdge() == 14){
                    assertEquals(iter.getBaseNode(), 3);
                    assertEquals(iter.getAdjNode(), 7);


                }


            }
        }
        assertEquals(10,lg.getLevel(0));
        assertEquals(10,lg.getLevel(1));
        assertEquals(10,lg.getLevel(2));
        assertEquals(10,lg.getLevel(3));



    }
    //Restrictions on edges{2->3,7->8}
    @Test
    public void testRestrictedGraph3(){
        CoreTestEdgeFilter restrictedEdges = new CoreTestEdgeFilter();
        restrictedEdges.add(12);
        restrictedEdges.add(6);
        CHGraph lg = testMoreComplexRestrictedGraph(restrictedEdges);
        AllCHEdgesIterator iter = lg.getAllEdges();

        while(iter.next()){
            if(iter.isShortcut()){
                if(iter.getEdge() == 13){
                    assertEquals(iter.getBaseNode(), 4);
                    assertEquals(iter.getAdjNode(), 7);
                    continue;

                }
                if(iter.getEdge() == 14){
                    assertEquals(iter.getBaseNode(), 3);
                    assertEquals(iter.getAdjNode(), 7);
                    continue;

                }
                if(iter.getEdge() == 15){
                    assertEquals(iter.getBaseNode(), 3);
                    assertEquals(iter.getAdjNode(), 8);
                    continue;

                }
                if(iter.getEdge() == 16){
                    assertEquals(iter.getBaseNode(), 2);
                    assertEquals(iter.getAdjNode(), 8);


                }
            }
        }
        assertEquals(10,lg.getLevel(2));
        assertEquals(10,lg.getLevel(3));
        assertEquals(10,lg.getLevel(7));
        assertEquals(10,lg.getLevel(8));



    }

}
