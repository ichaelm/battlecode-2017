package robotcore;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by Michael on 1/10/2017.
 */
public class RangeListTest {

    @Test
    public void testEmpty() throws Exception {
        RangeList r = new RangeList(true);
        assertTrue("Empty RangeList should be empty", r.isEmpty());
        assertFalse("Empty RangeList contains should be false", r.contains(0f));
        assertEquals("Empty RangeList closest should be NaN", r.closest(0f), Float.NaN, 0.001f);
    }

    @Test
    public void testSingle() throws Exception {
        RangeList r = new RangeList(true);
        r.add(0,1);
        assertFalse("Single RangeList should not be empty", r.isEmpty());
        assertFalse("Single RangeList contains should be false", r.contains(-0.5f));
        assertTrue("Single RangeList contains should be true", r.contains(0f));
        assertTrue("Single RangeList contains should be true", r.contains(0.5f));
        assertTrue("Single RangeList contains should be true", r.contains(1f));
        assertFalse("Single RangeList contains should be false", r.contains(1.5f));
        assertEquals("Single RangeList closest should be 0", r.closest(-0.5f), 0f, 0.001f);
        assertEquals("Single RangeList closest should be 0", r.closest(0f), 0f, 0.001f);
        assertEquals("Single RangeList closest should be 0.5", r.closest(0.5f), 0.5f, 0.001f);
        assertEquals("Single RangeList closest should be 1", r.closest(1f), 1f, 0.001f);
        assertEquals("Single RangeList closest should be 1", r.closest(1.5f), 1f, 0.001f);
    }

    @Test
    public void testTwoDisjointIncreasing() throws Exception {
        RangeList r = new RangeList(true);
        r.add(0,1);
        r.add(2,3);
        assertFalse(r.isEmpty());
        assertFalse(r.contains(-0.5f));
        assertTrue(r.contains(0f));
        assertTrue(r.contains(0.5f));
        assertTrue(r.contains(1f));
        assertFalse(r.contains(1.5f));
        assertTrue(r.contains(2f));
        assertTrue(r.contains(2.5f));
        assertTrue(r.contains(3f));
        assertFalse(r.contains(3.5f));
        assertEquals(r.closest(-0.5f), 0f, 0.001f);
        assertEquals(r.closest(0f), 0f, 0.001f);
        assertEquals(r.closest(0.5f), 0.5f, 0.001f);
        assertEquals(r.closest(1f), 1f, 0.001f);
        assertEquals(r.closest(1.499f), 1f, 0.001f);
        assertEquals(r.closest(1.501f), 2f, 0.001f);
        assertEquals(r.closest(2f), 2f, 0.001f);
        assertEquals(r.closest(2.5f), 2.5f, 0.001f);
        assertEquals(r.closest(3f), 3f, 0.001f);
        assertEquals(r.closest(3.5f), 3f, 0.001f);
    }

    @Test
    public void testTwoDisjointDecreasing() throws Exception {
        RangeList r = new RangeList(true);
        r.add(2,3);
        r.add(0,1);
        assertFalse(r.isEmpty());
        assertFalse(r.contains(-0.5f));
        assertTrue(r.contains(0f));
        assertTrue(r.contains(0.5f));
        assertTrue(r.contains(1f));
        assertFalse(r.contains(1.5f));
        assertTrue(r.contains(2f));
        assertTrue(r.contains(2.5f));
        assertTrue(r.contains(3f));
        assertFalse(r.contains(3.5f));
        assertEquals(r.closest(-0.5f), 0f, 0.001f);
        assertEquals(r.closest(0f), 0f, 0.001f);
        assertEquals(r.closest(0.5f), 0.5f, 0.001f);
        assertEquals(r.closest(1f), 1f, 0.001f);
        assertEquals(r.closest(1.499f), 1f, 0.001f);
        assertEquals(r.closest(1.501f), 2f, 0.001f);
        assertEquals(r.closest(2f), 2f, 0.001f);
        assertEquals(r.closest(2.5f), 2.5f, 0.001f);
        assertEquals(r.closest(3f), 3f, 0.001f);
        assertEquals(r.closest(3.5f), 3f, 0.001f);
    }

    @Test
    public void testTwoOverlappingIncreasing() throws Exception {
        RangeList r = new RangeList(true);
        r.add(0,2);
        r.add(1,3);
        assertFalse(r.isEmpty());
        assertFalse(r.contains(-0.5f));
        assertTrue(r.contains(0f));
        assertTrue(r.contains(0.5f));
        assertTrue(r.contains(1f));
        assertTrue(r.contains(1.5f));
        assertTrue(r.contains(2f));
        assertTrue(r.contains(2.5f));
        assertTrue(r.contains(3f));
        assertFalse(r.contains(3.5f));
        assertEquals(r.closest(-0.5f), 0f, 0.001f);
        assertEquals(r.closest(0f), 0f, 0.001f);
        assertEquals(r.closest(0.5f), 0.5f, 0.001f);
        assertEquals(r.closest(1f), 1f, 0.001f);
        assertEquals(r.closest(1.4f), 1.4f, 0.001f);
        assertEquals(r.closest(1.5f), 1.5f, 0.001f);
        assertEquals(r.closest(1.6f), 1.6f, 0.001f);
        assertEquals(r.closest(2f), 2f, 0.001f);
        assertEquals(r.closest(2.5f), 2.5f, 0.001f);
        assertEquals(r.closest(3f), 3f, 0.001f);
        assertEquals(r.closest(3.5f), 3f, 0.001f);
    }

    @Test
    public void testTwoOverlappingDecreasing() throws Exception {
        RangeList r = new RangeList(true);
        r.add(1,3);
        r.add(0,2);
        assertFalse(r.isEmpty());
        assertFalse(r.contains(-0.5f));
        assertTrue(r.contains(0f));
        assertTrue(r.contains(0.5f));
        assertTrue(r.contains(1f));
        assertTrue(r.contains(1.5f));
        assertTrue(r.contains(2f));
        assertTrue(r.contains(2.5f));
        assertTrue(r.contains(3f));
        assertFalse(r.contains(3.5f));
        assertEquals(r.closest(-0.5f), 0f, 0.001f);
        assertEquals(r.closest(0f), 0f, 0.001f);
        assertEquals(r.closest(0.5f), 0.5f, 0.001f);
        assertEquals(r.closest(1f), 1f, 0.001f);
        assertEquals(r.closest(1.4f), 1.4f, 0.001f);
        assertEquals(r.closest(1.5f), 1.5f, 0.001f);
        assertEquals(r.closest(1.6f), 1.6f, 0.001f);
        assertEquals(r.closest(2f), 2f, 0.001f);
        assertEquals(r.closest(2.5f), 2.5f, 0.001f);
        assertEquals(r.closest(3f), 3f, 0.001f);
        assertEquals(r.closest(3.5f), 3f, 0.001f);
    }

    @Test
    public void testTwoInsideOut() throws Exception {
        RangeList r = new RangeList(true);
        r.add(1,2);
        r.add(0,3);
        assertFalse(r.isEmpty());
        assertFalse(r.contains(-0.5f));
        assertTrue(r.contains(0f));
        assertTrue(r.contains(0.5f));
        assertTrue(r.contains(1f));
        assertTrue(r.contains(1.5f));
        assertTrue(r.contains(2f));
        assertTrue(r.contains(2.5f));
        assertTrue(r.contains(3f));
        assertFalse(r.contains(3.5f));
        assertEquals(r.closest(-0.5f), 0f, 0.001f);
        assertEquals(r.closest(0f), 0f, 0.001f);
        assertEquals(r.closest(0.5f), 0.5f, 0.001f);
        assertEquals(r.closest(1f), 1f, 0.001f);
        assertEquals(r.closest(1.4f), 1.4f, 0.001f);
        assertEquals(r.closest(1.5f), 1.5f, 0.001f);
        assertEquals(r.closest(1.6f), 1.6f, 0.001f);
        assertEquals(r.closest(2f), 2f, 0.001f);
        assertEquals(r.closest(2.5f), 2.5f, 0.001f);
        assertEquals(r.closest(3f), 3f, 0.001f);
        assertEquals(r.closest(3.5f), 3f, 0.001f);
    }

    @Test
    public void testTwoOutsideIn() throws Exception {
        RangeList r = new RangeList(true);
        r.add(0,3);
        r.add(1,2);
        assertFalse(r.isEmpty());
        assertFalse(r.contains(-0.5f));
        assertTrue(r.contains(0f));
        assertTrue(r.contains(0.5f));
        assertTrue(r.contains(1f));
        assertTrue(r.contains(1.5f));
        assertTrue(r.contains(2f));
        assertTrue(r.contains(2.5f));
        assertTrue(r.contains(3f));
        assertFalse(r.contains(3.5f));
        assertEquals(r.closest(-0.5f), 0f, 0.001f);
        assertEquals(r.closest(0f), 0f, 0.001f);
        assertEquals(r.closest(0.5f), 0.5f, 0.001f);
        assertEquals(r.closest(1f), 1f, 0.001f);
        assertEquals(r.closest(1.4f), 1.4f, 0.001f);
        assertEquals(r.closest(1.5f), 1.5f, 0.001f);
        assertEquals(r.closest(1.6f), 1.6f, 0.001f);
        assertEquals(r.closest(2f), 2f, 0.001f);
        assertEquals(r.closest(2.5f), 2.5f, 0.001f);
        assertEquals(r.closest(3f), 3f, 0.001f);
        assertEquals(r.closest(3.5f), 3f, 0.001f);
    }

}